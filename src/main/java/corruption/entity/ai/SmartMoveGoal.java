package corruption.entity.ai;

import corruption.entity.custom.baseEntity.BaseMob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * SmartMoveGoal v3
 *
 * 优化：
 * - 支持最大跳跃高度、宽度配置
 * - 跳跃前验证可行性，跳不过则重新寻路
 * - 改进跳跃时机（接近坑口时起跳）
 * - 直接追击时避免走入岩浆
 * - 卡死时重新寻路
 */
public class SmartMoveGoal extends Goal {

    // ===================== 配置 =====================

    public static final class Config {
        final float moveSpeed;
        final float waypointReachDist;
        final int pathfindMaxNodes;
        final int partialKeepFromEnd;
        final int targetUpdateInterval;
        final float targetMoveThreshold;
        final int maxJumpHeight;
        final int maxJumpWidth;
        final boolean avoidDangerousFluids;
        final boolean debug;

        private Config(Builder b) {
            this.moveSpeed = b.moveSpeed;
            this.waypointReachDist = b.waypointReachDist;
            this.pathfindMaxNodes = b.pathfindMaxNodes;
            this.partialKeepFromEnd = b.partialKeepFromEnd;
            this.targetUpdateInterval = b.targetUpdateInterval;
            this.targetMoveThreshold = b.targetMoveThreshold;
            this.maxJumpHeight = b.maxJumpHeight;
            this.maxJumpWidth = b.maxJumpWidth;
            this.avoidDangerousFluids = b.avoidDangerousFluids;
            this.debug = b.debug;
        }
    }

    // ===================== 字段 =====================

    private final BaseMob mob;
    private final Config cfg;
    private final JumpPointPathfinder pathfinder;

    private LivingEntity currentTarget = null;
    private BlockPos lastTargetPos = null;
    private int waypointIndex = 0;
    private int targetUpdateCd = 0;
    private boolean directChase = false;

    /** 跳跃冷却（tick） */
    private int jumpCooldown = 0;
    private static final int JUMP_CD = 10;

    // 卡死检测
    private int stuckTicks = 0;
    private BlockPos lastWp = null;

    /**
     * 跨坑跳跃状态机
     */
    private enum GapState { NONE, APPROACHING, JUMPING }
    private GapState gapState = GapState.NONE;
    private BlockPos gapEdgeTarget = null; // 坑口位置
    private BlockPos gapLandTarget = null; // 落点

    // ===================== 构造 =====================

    private SmartMoveGoal(BaseMob mob, Config cfg) {
        this.mob = mob;
        this.cfg = cfg;
        this.pathfinder = new JumpPointPathfinder(
                mob.level(), mob.getBbHeight(),
                cfg.pathfindMaxNodes, cfg.partialKeepFromEnd,
                cfg.maxJumpHeight, cfg.maxJumpWidth);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    // ===================== Goal 接口 =====================

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive()) return false;
        return mob.distanceToSqr(t) < 2048;
    }

    @Override
    public void start() {
        currentTarget = mob.getTarget();
        if (currentTarget == null) return;
        waypointIndex = 0;
        directChase = false;
        gapState = GapState.NONE;
        stuckTicks = 0;
        lastWp = null;
        lastTargetPos = currentTarget.blockPosition();
        pathfinder.startSearch(mob.blockPosition(), lastTargetPos);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        pathfinder.reset();
        currentTarget = null; lastTargetPos = null;
        directChase = false; gapState = GapState.NONE;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        // 冷却递减
        if (jumpCooldown > 0) jumpCooldown--;

        // 目标位置更新
        if (--targetUpdateCd <= 0) {
            targetUpdateCd = cfg.targetUpdateInterval;
            BlockPos ntp = target.blockPosition();
            if (lastTargetPos == null || blockDist(lastTargetPos, ntp) > cfg.targetMoveThreshold) {
                lastTargetPos = ntp;
                pathfinder.updateTarget(ntp, mob.blockPosition());
            }
        }

        // 推进寻路器
        JumpPointPathfinder.State pfState = pathfinder.tickSearch();

        // 根据状态移动
        if (pfState == JumpPointPathfinder.State.DIRECT_CHASE || directChase) {
            doDirectChase(target);
        } else if (pfState == JumpPointPathfinder.State.PATH_FOUND) {
            followPath(target);
        } else {
            // 寻路中/失败 → 尝试直接追击
            doDirectChase(target);
        }

        // 调试粒子
        if (cfg.debug) spawnParticles();
    }

    // ===================== 路径跟随 =====================

    private void followPath(LivingEntity target) {
        // 近距离直接追击
        double horizontalDistSq = mob.distanceToSqr(target.getX(), mob.getY(), target.getZ());
        int dy = Math.abs(target.blockPosition().getY() - mob.blockPosition().getY());
        if (horizontalDistSq <= 16 && dy <= 3) {
            directChase = true;
            doDirectChase(target);
            return;
        }

        // 目标可见且距离近
        if (mob.hasLineOfSight(target) && mob.distanceToSqr(target) <= 36) {
            directChase = true;
            doDirectChase(target);
            return;
        }

        List<BlockPos> path = pathfinder.getFinalPath();
        List<JumpPointPathfinder.JumpType> jumpTypes = pathfinder.getJumpTypes();

        if (path.isEmpty()) { doDirectChase(target); return; }
        if (waypointIndex >= path.size()) { doDirectChase(target); return; }

        // 进入直接追击范围
        if (mob.distanceToSqr(target) <= JumpPointPathfinder.DIRECT_CHASE_DIST_SQ) {
            directChase = true;
            doDirectChase(target);
            return;
        }

        BlockPos wp = path.get(waypointIndex);
        JumpPointPathfinder.JumpType jt = jumpTypes.size() > waypointIndex ? jumpTypes.get(waypointIndex) : JumpPointPathfinder.JumpType.NONE;

        // 到达当前路径点
        if (mob.blockPosition().distSqr(wp) <= cfg.waypointReachDist * cfg.waypointReachDist) {
            waypointIndex++;
            gapState = GapState.NONE;
            stuckTicks = 0;
            lastWp = null;
            if (waypointIndex >= path.size()) { doDirectChase(target); return; }
            wp = path.get(waypointIndex);
            jt = jumpTypes.size() > waypointIndex ? jumpTypes.get(waypointIndex) : JumpPointPathfinder.JumpType.NONE;
        }

        // 卡死检测：长时间未到达当前路径点 → 重新寻路
        if (wp.equals(lastWp)) {
            if (mob.blockPosition().distSqr(wp) > cfg.waypointReachDist * cfg.waypointReachDist) {
                stuckTicks++;
                if (stuckTicks > 40) {
                    // 重新寻路
                    pathfinder.startSearch(mob.blockPosition(), target.blockPosition());
                    waypointIndex = 0;
                    stuckTicks = 0;
                    lastWp = null;
                    return;
                }
            } else {
                stuckTicks = 0;
            }
        } else {
            lastWp = wp;
            stuckTicks = 0;
        }

        // 分类处理跳跃
        switch (jt) {
            case UP -> handleUpJump(wp);
            case GAP_1 -> handleGapJump(wp, false);
            case GAP_2 -> handleGapJump(wp, true);
            default -> {
                if (!detectAndHandleGapAhead(wp)) {
                    moveTo(wp);
                }
            }
        }
    }

    // ===================== 跳跃处理 =====================

    private void handleUpJump(BlockPos wp) {
        int dy = wp.getY() - mob.blockPosition().getY();
        double distSq = mob.blockPosition().distSqr(wp);

        if (dy >= 1 && distSq < 9 && jumpCooldown <= 0) {
            mob.getJumpControl().jump();
            jumpCooldown = JUMP_CD;
        }
        moveTo(wp);
    }

    private void handleGapJump(BlockPos landPos, boolean wideGap) {
        if (gapState == GapState.NONE) {
            gapEdgeTarget = computeGapEdge(mob.blockPosition(), landPos);
            gapLandTarget = landPos;

            // 验证跳跃能力
            double dx = gapLandTarget.getX() - mob.blockPosition().getX();
            double dz = gapLandTarget.getZ() - mob.blockPosition().getZ();
            double horizontalDist = Math.sqrt(dx*dx + dz*dz);
            int verticalDiff = gapLandTarget.getY() - mob.blockPosition().getY();
            if (horizontalDist > cfg.maxJumpWidth + 0.5 || Math.abs(verticalDiff) > cfg.maxJumpHeight) {
                // 跳不过，放弃并重新寻路
                gapState = GapState.NONE;
                pathfinder.startSearch(mob.blockPosition(), currentTarget.blockPosition());
                return;
            }
            gapState = GapState.APPROACHING;
        }

        if (gapState == GapState.APPROACHING) {
            double distToEdge = Math.sqrt(mob.blockPosition().distSqr(gapEdgeTarget));
            if (distToEdge <= 1.0) { // 距离坑口 1 格内起跳
                if (jumpCooldown <= 0) {
                    mob.getJumpControl().jump();
                    jumpCooldown = JUMP_CD;
                    gapState = GapState.JUMPING;
                }
            }
            moveToFast(gapEdgeTarget);
        }

        if (gapState == GapState.JUMPING) {
            moveToFast(gapLandTarget);
            if (mob.onGround() && mob.blockPosition().distSqr(gapLandTarget) < 4) {
                gapState = GapState.NONE;
            }
        }
    }

    /**
     * 主动扫描前方坑口并处理
     */
    private boolean detectAndHandleGapAhead(BlockPos wp) {
        if (gapState != GapState.NONE) {
            if (gapLandTarget != null) {
                handleGapJump(gapLandTarget, gapState == GapState.JUMPING);
            }
            return true;
        }

        Vec3 myPos = mob.position();
        BlockPos myBlock = mob.blockPosition();

        double dx = wp.getX() + 0.5 - myPos.x;
        double dz = wp.getZ() + 0.5 - myPos.z;
        double len = Math.sqrt(dx*dx + dz*dz);
        if (len < 0.1) return false;

        int sdx = (int) Math.signum(dx);
        int sdz = (int) Math.signum(dz);

        for (int dist = 1; dist <= 3; dist++) {
            int scanX = myBlock.getX() + sdx * dist;
            int scanZ = myBlock.getZ() + sdz * dist;
            int scanY = myBlock.getY();
            BlockPos scanPos = new BlockPos(scanX, scanY, scanZ);

            if (!mob.level().getBlockState(scanPos.below()).isSolid()
                    && !mob.level().getBlockState(scanPos).isSolid()) {

                boolean isRealGap = false;
                for (int d = 1; d <= 3; d++) {
                    if (!mob.level().getBlockState(scanPos.below(d)).isSolid()) {
                        isRealGap = true;
                        break;
                    }
                }

                if (isRealGap) {
                    BlockPos land1 = new BlockPos(scanX + sdx, scanY, scanZ + sdz);
                    BlockPos land2 = new BlockPos(scanX + sdx*2, scanY, scanZ + sdz*2);
                    BlockPos landTarget = null;
                    if (canStandAt(land1)) {
                        landTarget = land1;
                    } else if (!mob.level().getBlockState(land1).isSolid() && canStandAt(land2)) {
                        landTarget = land2;
                    }

                    if (landTarget != null) {
                        // 验证跳跃能力
                        double jumpDx = landTarget.getX() - mob.blockPosition().getX();
                        double jumpDz = landTarget.getZ() - mob.blockPosition().getZ();
                        double jumpHoriz = Math.sqrt(jumpDx*jumpDx + jumpDz*jumpDz);
                        int jumpDy = landTarget.getY() - mob.blockPosition().getY();
                        if (jumpHoriz <= cfg.maxJumpWidth + 0.5 && Math.abs(jumpDy) <= cfg.maxJumpHeight) {
                            gapEdgeTarget = new BlockPos(scanX - sdx, scanY, scanZ - sdz);
                            gapLandTarget = landTarget;
                            gapState = GapState.APPROACHING;
                            handleGapJump(landTarget, landTarget.equals(land2));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ===================== 直接追击 =====================

    private void doDirectChase(LivingEntity target) {
        Vec3 targetPos = target.position();
        BlockPos targetBlock = target.blockPosition();

        // 避免走入岩浆（如果配置开启）
        if (cfg.avoidDangerousFluids) {
            BlockPos ground = targetBlock.below();
            if (isDangerousFluid(ground)) {
                // 目标站在危险液体上，不直接追击，重新寻路
                directChase = false;
                pathfinder.startSearch(mob.blockPosition(), targetBlock);
                waypointIndex = 0;
                return;
            }
        }

        // 垂直跳跃处理
        if (targetPos.y > mob.getY() + 0.5) {
            int sdx = (int) Math.signum(targetPos.x - mob.getX());
            int sdz = (int) Math.signum(targetPos.z - mob.getZ());
            BlockPos front = mob.blockPosition().offset(sdx, 0, sdz);
            if (mob.level().getBlockState(front).isSolid() &&
                    !mob.level().getBlockState(front.above()).isSolid()) {
                if (jumpCooldown <= 0) {
                    mob.getJumpControl().jump();
                    jumpCooldown = JUMP_CD;
                }
            }
        }

        mob.getMoveControl().setWantedPosition(
                targetPos.x, targetPos.y, targetPos.z, cfg.moveSpeed);
        mob.getLookControl().setLookAt(target, 30f, 30f);

        if (!detectAndHandleGapAhead(target.blockPosition())) {
            handleWallJump(target.blockPosition());
        }

        // 目标变远则恢复寻路
        if (mob.distanceToSqr(target) > 16 * 4 && directChase) {
            directChase = false;
            pathfinder.startSearch(mob.blockPosition(), target.blockPosition());
            waypointIndex = 0;
            stuckTicks = 0;
            lastWp = null;
        }
    }

    private void handleWallJump(BlockPos towards) {
        if (jumpCooldown > 0) return;
        BlockPos myBlock = mob.blockPosition();
        int sdx = (int) Math.signum(towards.getX() - myBlock.getX());
        int sdz = (int) Math.signum(towards.getZ() - myBlock.getZ());

        BlockPos front = myBlock.offset(sdx, 0, sdz);
        if (mob.level().getBlockState(front).isSolid()) {
            BlockPos front2 = front.above();
            if (!mob.level().getBlockState(front2).isSolid()) {
                mob.getJumpControl().jump();
                jumpCooldown = JUMP_CD;
            } else if (!mob.level().getBlockState(front2.above()).isSolid()) {
                mob.getJumpControl().jump();
                jumpCooldown = JUMP_CD;
            }
        }
    }

    // ===================== 工具 =====================

    private void moveTo(BlockPos pos) {
        mob.getMoveControl().setWantedPosition(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, cfg.moveSpeed);
        mob.getLookControl().setLookAt(
                pos.getX() + 0.5, pos.getY() + mob.getEyeHeight(), pos.getZ() + 0.5);
    }

    private void moveToFast(BlockPos pos) {
        mob.getMoveControl().setWantedPosition(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                cfg.moveSpeed * 1.15);
        mob.getLookControl().setLookAt(
                pos.getX() + 0.5, pos.getY() + mob.getEyeHeight(), pos.getZ() + 0.5);
    }

    private BlockPos computeGapEdge(BlockPos from, BlockPos land) {
        int sdx = (int) Math.signum(land.getX() - from.getX());
        int sdz = (int) Math.signum(land.getZ() - from.getZ());
        return new BlockPos(from.getX() + sdx, from.getY(), from.getZ() + sdz);
    }

    private boolean canStandAt(BlockPos pos) {
        if (!mob.level().getBlockState(pos.below()).isSolid()) return false;
        int h = (int) Math.ceil(mob.getBbHeight());
        for (int i = 0; i < h; i++) {
            if (mob.level().getBlockState(pos.above(i)).isSolid()) return false;
        }
        return true;
    }

    private boolean isDangerousFluid(BlockPos pos) {
        FluidState fs = mob.level().getFluidState(pos);
        return fs.getType() == net.minecraft.world.level.material.Fluids.LAVA
                || fs.getType() == net.minecraft.world.level.material.Fluids.FLOWING_LAVA;
    }

    private static float blockDist(BlockPos a, BlockPos b) {
        int dx = a.getX()-b.getX(), dy = a.getY()-b.getY(), dz = a.getZ()-b.getZ();
        return (float) Math.sqrt(dx*dx+dy*dy+dz*dz);
    }

    // ===================== 调试粒子 =====================

    private void spawnParticles() {
        if (!(mob.level() instanceof ServerLevel sl)) return;
        List<BlockPos> path = pathfinder.getFinalPath();
        List<JumpPointPathfinder.JumpType> jts = pathfinder.getJumpTypes();

        int start = Math.max(0, waypointIndex - 1);
        int end = Math.min(path.size(), waypointIndex + 12);

        for (int i = start; i < end; i++) {
            BlockPos n = path.get(i);
            JumpPointPathfinder.JumpType jt = (i < jts.size()) ? jts.get(i) : JumpPointPathfinder.JumpType.NONE;
            var particle = switch (jt) {
                case UP -> ParticleTypes.FIREWORK;
                case GAP_1 -> ParticleTypes.FLAME;
                case GAP_2 -> ParticleTypes.SOUL_FIRE_FLAME;
                default -> ParticleTypes.COMPOSTER;
            };
            sl.sendParticles(particle, n.getX()+0.5, n.getY()+1.2, n.getZ()+0.5, 2, 0.1,0.1,0.1, 0);
        }

        if (gapEdgeTarget != null && gapState != GapState.NONE) {
            sl.sendParticles(ParticleTypes.CRIT,
                    gapEdgeTarget.getX()+0.5, gapEdgeTarget.getY()+1.5, gapEdgeTarget.getZ()+0.5,
                    4, 0.1,0.1,0.1, 0);
        }
        if (gapLandTarget != null && gapState != GapState.NONE) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    gapLandTarget.getX()+0.5, gapLandTarget.getY()+1.5, gapLandTarget.getZ()+0.5,
                    4, 0.2,0.2,0.2, 0);
        }
    }

    // ===================== Builder =====================

    public static final class Builder {
        private final BaseMob mob;
        private float moveSpeed = 1.2f;
        private float waypointReachDist = 1.2f;
        private int pathfindMaxNodes = 512;
        private int partialKeepFromEnd = 5;
        private int targetUpdateInterval = 5;
        private float targetMoveThreshold = 2.0f;
        private int maxJumpHeight = 2;
        private int maxJumpWidth = 2;
        private boolean avoidDangerousFluids = true;
        private boolean debug = false;

        public Builder(BaseMob mob) { this.mob = mob; }
        public Builder speed(float v) { moveSpeed = v; return this; }
        public Builder waypointReachDist(float v) { waypointReachDist = v; return this; }
        public Builder maxNodes(int v) { pathfindMaxNodes = v; return this; }
        public Builder partialKeepFromEnd(int v) { partialKeepFromEnd = v; return this; }
        public Builder targetUpdateInterval(int v) { targetUpdateInterval = Math.max(1,v); return this; }
        public Builder targetMoveThreshold(float v) { targetMoveThreshold = v; return this; }
        public Builder maxJumpHeight(int v) { maxJumpHeight = v; return this; }
        public Builder maxJumpWidth(int v) { maxJumpWidth = v; return this; }
        public Builder avoidDangerousFluids(boolean v) { avoidDangerousFluids = v; return this; }
        public Builder debug(boolean v) { debug = v; return this; }
        public SmartMoveGoal build() { return new SmartMoveGoal(mob, new Config(this)); }
    }
}