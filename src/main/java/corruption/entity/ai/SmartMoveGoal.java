package corruption.entity.custom.baseEntity.goal;

import corruption.entity.custom.baseEntity.BaseMob;
import corruption.entity.custom.baseEntity.pathfinding.JumpPointPathfinder;
import corruption.entity.custom.baseEntity.pathfinding.JumpPointPathfinder.JumpType;
import corruption.entity.custom.baseEntity.pathfinding.JumpPointPathfinder.State;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * SmartMoveGoal v2
 *
 * 关键修复：
 *  - 处理 JumpType.GAP_1 / GAP_2：在"坑边"提前起跳，而非到达坑内节点才跳
 *  - detectGapAhead(): 主动扫描前方 1-3 格，发现坑口立即起跳
 *  - UP 跳跃：在接近高台阶节点时更早触发，确保有足够跳跃高度
 */
public class SmartMoveGoal extends Goal {

    // ===================== 配置 =====================

    public static final class Config {
        final float   moveSpeed;
        final float   waypointReachDist;
        final int     pathfindMaxNodes;
        final int     partialKeepFromEnd;
        final int     targetUpdateInterval;
        final float   targetMoveThreshold;
        final boolean debug;

        private Config(Builder b) {
            this.moveSpeed            = b.moveSpeed;
            this.waypointReachDist    = b.waypointReachDist;
            this.pathfindMaxNodes     = b.pathfindMaxNodes;
            this.partialKeepFromEnd   = b.partialKeepFromEnd;
            this.targetUpdateInterval = b.targetUpdateInterval;
            this.targetMoveThreshold  = b.targetMoveThreshold;
            this.debug                = b.debug;
        }
    }

    // ===================== 字段 =====================

    private final BaseMob mob;
    private final Config  cfg;
    private final JumpPointPathfinder pathfinder;

    private LivingEntity currentTarget   = null;
    private BlockPos      lastTargetPos  = null;
    private int           waypointIndex  = 0;
    private int           targetUpdateCd = 0;
    private boolean       directChase    = false;

    /** 跳跃冷却（tick），防止连续起跳导致飘飞 */
    private int jumpCooldown = 0;
    private static final int JUMP_CD = 10;

    /**
     * 跨坑跳跃状态机：
     *  NONE        = 未处理
     *  APPROACHING = 正在接近坑口，准备起跳
     *  JUMPING     = 已起跳，正在飞越
     */
    private enum GapState { NONE, APPROACHING, JUMPING }
    private GapState gapState      = GapState.NONE;
    private BlockPos gapEdgeTarget = null; // 坑口处的移动目标（站在坑边发力点）
    private BlockPos gapLandTarget = null; // 落点

    // ===================== 构造 =====================

    private SmartMoveGoal(BaseMob mob, Config cfg) {
        this.mob        = mob;
        this.cfg        = cfg;
        this.pathfinder = new JumpPointPathfinder(
                mob.level(), mob.getBbHeight(),
                cfg.pathfindMaxNodes, cfg.partialKeepFromEnd);
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
        waypointIndex  = 0;
        directChase    = false;
        gapState       = GapState.NONE;
        lastTargetPos  = currentTarget.blockPosition();
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

        // ---- 冷却递减 ----
        if (jumpCooldown > 0) jumpCooldown--;

        // ---- 目标位置更新 ----
        if (--targetUpdateCd <= 0) {
            targetUpdateCd = cfg.targetUpdateInterval;
            BlockPos ntp = target.blockPosition();
            if (lastTargetPos == null || blockDist(lastTargetPos, ntp) > cfg.targetMoveThreshold) {
                lastTargetPos = ntp;
                pathfinder.updateTarget(ntp, mob.blockPosition());
            }
        }

        // ---- 每 tick 推进寻路器 ----
        State pfState = pathfinder.tickSearch();

        // ---- 根据状态移动 ----
        if (pfState == State.DIRECT_CHASE || directChase) {
            doDirectChase(target);
        } else if (pfState == State.PATH_FOUND) {
            followPath(target);
        } else {
            // 寻路中 / 失败 → 先直行
            doDirectChase(target);
        }

        // ---- 调试粒子 ----
        if (cfg.debug) spawnParticles();
    }

    // ===================== 路径跟随 =====================

    private void followPath(LivingEntity target) {
        List<BlockPos> path      = pathfinder.getFinalPath();
        List<JumpType> jumpTypes = pathfinder.getJumpTypes();

        if (path.isEmpty()) { doDirectChase(target); return; }
        if (waypointIndex >= path.size()) { doDirectChase(target); return; }

        // 进入直接追击范围
        if (mob.distanceToSqr(target) <= JumpPointPathfinder.DIRECT_CHASE_DIST_SQ) {
            directChase = true;
            doDirectChase(target);
            return;
        }

        BlockPos wp = path.get(waypointIndex);
        JumpType jt = jumpTypes.size() > waypointIndex ? jumpTypes.get(waypointIndex) : JumpType.NONE;

        // 到达当前路径点
        if (mob.blockPosition().distSqr(wp) <= cfg.waypointReachDist * cfg.waypointReachDist) {
            waypointIndex++;
            gapState = GapState.NONE; // 到达节点，重置跨坑状态
            if (waypointIndex >= path.size()) { doDirectChase(target); return; }
            wp = path.get(waypointIndex);
            jt = jumpTypes.size() > waypointIndex ? jumpTypes.get(waypointIndex) : JumpType.NONE;
        }

        // ---- 分类处理跳跃 ----
        switch (jt) {
            case UP     -> handleUpJump(wp);
            case GAP_1  -> handleGapJump(wp, false);
            case GAP_2  -> handleGapJump(wp, true);
            default     -> {
                // 主动扫描前方，防止因寻路节点跨度大而错过坑口
                if (!detectAndHandleGapAhead(wp)) {
                    moveTo(wp);
                }
            }
        }
    }

    // ===================== 跳跃处理 =====================

    /**
     * 上坡跳跃：目标节点比自身高 1-2 格时，提前触发跳跃
     */
    private void handleUpJump(BlockPos wp) {
        int dy = wp.getY() - mob.blockPosition().getY();
        double distSq = mob.blockPosition().distSqr(wp);

        // 当距离节点 3 格以内且需要跳跃时触发
        if (dy >= 1 && distSq < 9 && jumpCooldown <= 0) {
            mob.getJumpControl().jump();
            jumpCooldown = JUMP_CD;
        }
        moveTo(wp);
    }

    /**
     * 跨坑跳跃状态机：
     *  1. 计算坑口边缘位置（waypoint 后退一格，即坑的起跳点）
     *  2. 先移动到坑口
     *  3. 到达坑口后起跳，同时将移动目标切换到落点
     */
    private void handleGapJump(BlockPos landPos, boolean wideGap) {
        if (gapState == GapState.NONE) {
            // 计算坑口：在当前位置到 landPos 方向上，后退1格的地方
            gapEdgeTarget = computeGapEdge(mob.blockPosition(), landPos);
            gapLandTarget = landPos;
            gapState = GapState.APPROACHING;
        }

        if (gapState == GapState.APPROACHING) {
            double distToEdge = mob.blockPosition().distSqr(gapEdgeTarget);

            if (distToEdge <= (wideGap ? 2.25 : 1.44)) {
                // 到达坑口，起跳
                if (jumpCooldown <= 0) {
                    mob.getJumpControl().jump();
                    jumpCooldown = JUMP_CD;
                    gapState = GapState.JUMPING;
                }
            }
            // 朝坑口移动（快速加速以确保跳跃动力）
            moveToFast(gapEdgeTarget);
        }

        if (gapState == GapState.JUMPING) {
            // 已起跳，持续朝落点移动
            moveToFast(gapLandTarget);
            // 如果落地（onGround），重置状态
            if (mob.onGround() && mob.blockPosition().distSqr(gapLandTarget) < 4) {
                gapState = GapState.NONE;
            }
        }
    }

    /**
     * 主动扫描前方 1-3 格是否有坑口，若有则提前起跳。
     * 这是最关键的修复：即使路径节点跨度大（如 16 格），也能在坑边及时跳跃。
     *
     * @return 是否已处理（接管了移动控制）
     */
    private boolean detectAndHandleGapAhead(BlockPos wp) {
        if (gapState != GapState.NONE) {
            // 已在跨坑状态，继续处理
            if (gapLandTarget != null) {
                handleGapJump(gapLandTarget, gapState == GapState.JUMPING);
            }
            return true;
        }

        Vec3  myPos = mob.position();
        BlockPos myBlock = mob.blockPosition();

        // 计算朝向 wp 的单位方向（水平）
        double dx = wp.getX() + 0.5 - myPos.x;
        double dz = wp.getZ() + 0.5 - myPos.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.1) return false;

        int sdx = (int) Math.signum(dx);
        int sdz = (int) Math.signum(dz);

        // 向前扫描 1-3 格，寻找坑口
        for (int dist = 1; dist <= 3; dist++) {
            int scanX = myBlock.getX() + sdx * dist;
            int scanZ = myBlock.getZ() + sdz * dist;
            int scanY = myBlock.getY();

            BlockPos scanPos = new BlockPos(scanX, scanY, scanZ);

            // 检查是否是间隙（脚下空旷）
            if (!mob.level().getBlockState(scanPos.below()).isSolid()
                    && !mob.level().getBlockState(scanPos).isSolid()) {

                // 确认下方是空气（坑而非台阶）
                boolean isRealGap = false;
                for (int d = 1; d <= 3; d++) {
                    if (!mob.level().getBlockState(scanPos.below(d)).isSolid()) {
                        isRealGap = true;
                        break;
                    }
                }

                if (isRealGap) {
                    // 检查对面是否可落脚（1格或2格跨越）
                    BlockPos land1 = new BlockPos(scanX + sdx, scanY, scanZ + sdz);
                    BlockPos land2 = new BlockPos(scanX + sdx*2, scanY, scanZ + sdz*2);

                    BlockPos landTarget = null;
                    if (canStandAt(land1)) {
                        landTarget = land1;
                    } else if (!mob.level().getBlockState(land1).isSolid()
                            && canStandAt(land2)) {
                        landTarget = land2;
                    }

                    if (landTarget != null) {
                        // 发现可跳过的坑，进入 GAP 处理
                        gapEdgeTarget = new BlockPos(scanX - sdx, scanY, scanZ - sdz);
                        gapLandTarget = landTarget;
                        gapState = GapState.APPROACHING;
                        handleGapJump(landTarget, landTarget.equals(land2));
                        return true;
                    }
                    // 跳不过去 → 让寻路器找绕路（不接管移动）
                }
            }
        }
        return false;
    }

    // ===================== 直接追击 =====================

    private void doDirectChase(LivingEntity target) {
        mob.getMoveControl().setWantedPosition(
                target.getX(), target.getY(), target.getZ(), cfg.moveSpeed);
        mob.getLookControl().setLookAt(target, 30f, 30f);

        // 直接追击时也主动扫描前方坑口
        if (!detectAndHandleGapAhead(target.blockPosition())) {
            handleWallJump(target.blockPosition());
        }

        // 目标变远则恢复寻路
        if (mob.distanceToSqr(target) > JumpPointPathfinder.DIRECT_CHASE_DIST_SQ * 4 && directChase) {
            directChase = false;
            pathfinder.startSearch(mob.blockPosition(), target.blockPosition());
            waypointIndex = 0;
        }
    }

    /**
     * 处理前方固体障碍的跳跃（上台阶 / 翻越 1-2 格高墙）
     */
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

    /** 跨坑时用较高速度确保跳跃动力充足 */
    private void moveToFast(BlockPos pos) {
        mob.getMoveControl().setWantedPosition(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                cfg.moveSpeed * 1.15);
        mob.getLookControl().setLookAt(
                pos.getX() + 0.5, pos.getY() + mob.getEyeHeight(), pos.getZ() + 0.5);
    }

    /**
     * 计算坑口起跳点：从自身位置朝 landPos 方向退一格
     */
    private BlockPos computeGapEdge(BlockPos from, BlockPos land) {
        int sdx = (int) Math.signum(land.getX() - from.getX());
        int sdz = (int) Math.signum(land.getZ() - from.getZ());
        // 坑口起跳点 = 坑的前一格（即 from 朝 land 方向走一步）
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

    private static float blockDist(BlockPos a, BlockPos b) {
        int dx = a.getX()-b.getX(), dy = a.getY()-b.getY(), dz = a.getZ()-b.getZ();
        return (float) Math.sqrt(dx*dx+dy*dy+dz*dz);
    }

    // ===================== 调试粒子 =====================

    private void spawnParticles() {
        if (!(mob.level() instanceof ServerLevel sl)) return;
        List<BlockPos> path = pathfinder.getFinalPath();
        List<JumpType> jts  = pathfinder.getJumpTypes();

        int start = Math.max(0, waypointIndex - 1);
        int end   = Math.min(path.size(), waypointIndex + 12);

        for (int i = start; i < end; i++) {
            BlockPos n  = path.get(i);
            JumpType jt = (i < jts.size()) ? jts.get(i) : JumpType.NONE;
            var particle = switch (jt) {
                case UP    -> ParticleTypes.FIREWORK;
                case GAP_1 -> ParticleTypes.FLAME;
                case GAP_2 -> ParticleTypes.SOUL_FIRE_FLAME;
                default    -> ParticleTypes.COMPOSTER;
            };
            sl.sendParticles(particle, n.getX()+0.5, n.getY()+1.2, n.getZ()+0.5, 2, 0.1,0.1,0.1, 0);
        }

        // 坑口/落点标记
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
        private float   moveSpeed            = 1.2f;
        private float   waypointReachDist    = 1.2f;
        private int     pathfindMaxNodes     = 512;
        private int     partialKeepFromEnd   = 5;
        private int     targetUpdateInterval = 5;
        private float   targetMoveThreshold  = 2.0f;
        private boolean debug                = false;

        public Builder(BaseMob mob)                     { this.mob = mob; }
        public Builder speed(float v)                   { moveSpeed = v; return this; }
        public Builder waypointReachDist(float v)       { waypointReachDist = v; return this; }
        public Builder maxNodes(int v)                  { pathfindMaxNodes = v; return this; }
        public Builder partialKeepFromEnd(int v)        { partialKeepFromEnd = v; return this; }
        public Builder targetUpdateInterval(int v)      { targetUpdateInterval = Math.max(1,v); return this; }
        public Builder targetMoveThreshold(float v)     { targetMoveThreshold = v; return this; }
        public Builder debug(boolean v)                 { debug = v; return this; }
        public SmartMoveGoal build()                    { return new SmartMoveGoal(mob, new Config(this)); }
    }
}