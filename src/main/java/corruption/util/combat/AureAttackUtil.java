package corruption.util.combat;

import corruption.entity.custom.baseEntity.BaseMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 现代范围攻击工具类，支持圆柱扇形攻击、距离衰减伤害、自定义伤害回调等功能
 */
public class AureAttackUtil {

    /**
     * 圆柱扇形攻击配置类
     */
    public static class CylindricalSectorConfig {
        private final LivingEntity source;
        private final float horizontalRange;
        private final float verticalRange;
        private final float angle;
        private final float setHealthDamage;
        private final float minDamage;
        private final float maxDamage;
        private final int damageLevel;
        private final Vec3 knockback;
        private final boolean excludeSameKind;
        private final boolean includePlayers;
        private final boolean damageAttenuation;
        private final float attenuationFactor;
        private final boolean applyKnockback;
        private final boolean destroyBlocks;
        private final float blockBreakHardness;
        private final boolean includeLiquids;
        private final boolean hurtTick;
        private final boolean protectGround; // 新增：保护脚下的地面
        private final float groundProtectionRadius; // 新增：地面保护半径
        private final boolean onlyBreakFrontBlocks; // 新增：是否只破坏前方的方块
        @Nullable
        private final BiFunction<LivingEntity, LivingEntity, Boolean> customTargetFilter;

        private CylindricalSectorConfig(Builder builder) {
            this.source = builder.source;
            this.horizontalRange = builder.horizontalRange;
            this.verticalRange = builder.verticalRange;
            this.angle = builder.angle;
            this.setHealthDamage = builder.setHealthDamage;
            this.minDamage = builder.minDamage;
            this.maxDamage = builder.maxDamage;
            this.damageLevel = builder.damageLevel;
            this.knockback = builder.knockback;
            this.excludeSameKind = builder.excludeSameKind;
            this.includePlayers = builder.includePlayers;
            this.damageAttenuation = builder.damageAttenuation;
            this.attenuationFactor = builder.attenuationFactor;
            this.applyKnockback = builder.applyKnockback;
            this.destroyBlocks = builder.destroyBlocks;
            this.blockBreakHardness = builder.blockBreakHardness;
            this.includeLiquids = builder.includeLiquids;
            this.hurtTick = builder.hurtTick;
            this.protectGround = builder.protectGround;
            this.groundProtectionRadius = builder.groundProtectionRadius;
            this.onlyBreakFrontBlocks = builder.onlyBreakFrontBlocks;
            this.customTargetFilter = builder.customTargetFilter;
        }

        public static Builder builder(LivingEntity source) {
            return new Builder(source);
        }

        /**
         * 建造者模式
         */
        public static class Builder {
            // 必需参数
            private final LivingEntity source;

            // 默认参数
            private float horizontalRange = 5.0f;
            private float verticalRange = 2.0f;
            private float angle = 90.0f;
            private float setHealthDamage = 1.0f;
            private float minDamage = 0.0f;
            private float maxDamage = 10.0f;
            private int damageLevel = 2;
            private Vec3 knockback = new Vec3(0.5, 0.2, 0.5);
            private boolean excludeSameKind = true;
            private boolean includePlayers = true;
            private boolean damageAttenuation = false;
            private float attenuationFactor = 1.0f;
            private boolean applyKnockback = true;
            private boolean destroyBlocks = false;
            private float blockBreakHardness = 50.0f;
            private boolean includeLiquids = false;
            private boolean hurtTick = false;
            private boolean protectGround = true; // 默认保护地面
            private float groundProtectionRadius = 1.5f; // 默认保护半径
            private boolean onlyBreakFrontBlocks = true; // 默认只破坏前方的方块
            @Nullable
            private BiFunction<LivingEntity, LivingEntity, Boolean> customTargetFilter = null;

            public Builder(LivingEntity source) {
                this.source = source;
            }
            public Builder setHealthDamage(float damage) {
                this.setHealthDamage = damage;
                return this;
            }

            public Builder horizontalRange(float range) {
                this.horizontalRange = range;
                return this;
            }

            public Builder verticalRange(float range) {
                this.verticalRange = range;
                return this;
            }

            public Builder angle(float angle) {
                this.angle = Math.min(Math.max(angle, 0.0f), 180.0f);
                return this;
            }

            public Builder damage(float min, float max) {
                this.minDamage = min;
                this.maxDamage = max;
                return this;
            }

            public Builder damageLevel(int level) {
                this.damageLevel = Math.max(1, Math.min(level, 4));
                return this;
            }

            public Builder knockback(Vec3 knockback) {
                this.knockback = knockback;
                return this;
            }

            public Builder excludeSameKind(boolean exclude) {
                this.excludeSameKind = exclude;
                return this;
            }

            public Builder includePlayers(boolean include) {
                this.includePlayers = include;
                return this;
            }

            public Builder damageAttenuation(boolean attenuation) {
                this.damageAttenuation = attenuation;
                return this;
            }

            public Builder attenuationFactor(float factor) {
                this.attenuationFactor = factor;
                return this;
            }

            public Builder applyKnockback(boolean apply) {
                this.applyKnockback = apply;
                return this;
            }

            public Builder destroyBlocks(boolean destroy) {
                this.destroyBlocks = destroy;
                return this;
            }

            public Builder blockBreakHardness(float hardness) {
                this.blockBreakHardness = hardness;
                return this;
            }

            public Builder includeLiquids(boolean include) {
                this.includeLiquids = include;
                return this;
            }

            public Builder hurtTick(boolean hurtTick) {
                this.hurtTick = hurtTick;
                return this;
            }

            public Builder protectGround(boolean protect) {
                this.protectGround = protect;
                return this;
            }

            public Builder groundProtectionRadius(float radius) {
                this.groundProtectionRadius = radius;
                return this;
            }

            public Builder onlyBreakFrontBlocks(boolean onlyFront) {
                this.onlyBreakFrontBlocks = onlyFront;
                return this;
            }

            public Builder customTargetFilter(@Nullable BiFunction<LivingEntity, LivingEntity, Boolean> filter) {
                this.customTargetFilter = filter;
                return this;
            }

            public CylindricalSectorConfig build() {
                return new CylindricalSectorConfig(this);
            }
        }
    }

    /**
     * 执行圆柱扇形攻击（前方）
     */
    public static void executeFrontCylindricalSectorAttack(CylindricalSectorConfig config) {
        executeCylindricalSectorAttack(config, true);
    }

    /**
     * 执行圆柱扇形攻击（后方）
     */
    public static void executeBackCylindricalSectorAttack(CylindricalSectorConfig config) {
        executeCylindricalSectorAttack(config, false);
    }

    /**
     * 执行圆柱扇形攻击
     */
    private static void executeCylindricalSectorAttack(CylindricalSectorConfig config, boolean front) {
        LivingEntity source = config.source;
        Level level = source.level();
        Vec3 center = source.position();

        // 获取前方或后方的方向向量
        Vec3 lookVec = source.getLookAngle();
        Vec3 horizontalLookVec = new Vec3(lookVec.x, 0, lookVec.z).normalize();
        if (!front) {
            horizontalLookVec = horizontalLookVec.reverse();
        }

        // 计算角度余弦值
        double cosAngle = Math.cos(Math.toRadians(config.angle / 2));

        // 获取范围内的实体
        List<Entity> entities = level.getEntitiesOfClass(Entity.class,
                new AABB(
                        center.x - config.horizontalRange,
                        center.y - config.verticalRange,
                        center.z - config.horizontalRange,
                        center.x + config.horizontalRange,
                        center.y + config.verticalRange,
                        center.z + config.horizontalRange
                )
        );

        for (Entity entity : entities) {
            if (!shouldAffectEntity(source, entity, config)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;

            // 检查是否在圆柱扇形内
            if (!isInCylindricalSector(source, target, config, front)) {
                continue;
            }

            // 计算距离衰减伤害
            float finalDamage = calculateDamageWithAttenuation(source, target, config);

            // 应用伤害
            boolean damageApplied = HurtTargetUtil.TrueHurt(
                    source,
                    target,
                    config.setHealthDamage, // 最小伤害（无视无敌）
                    finalDamage, // 普通伤害（如果需要可调整）
                    config.damageLevel,
                    config.hurtTick
            );

            // 应用击退
            if (damageApplied && config.applyKnockback) {
                applyKnockback(source, target, config);
            }
        }

        // 破坏方块
        if (config.destroyBlocks) {
            breakBlocksInCylindricalSector(source, config, front);
        }
    }

    /**
     * 检查实体是否应该被影响
     */
    private static boolean shouldAffectEntity(LivingEntity source, Entity entity, CylindricalSectorConfig config) {
        // 跳过自身
        if (entity == source) {
            return false;
        }

        // 排除创造模式和旁观模式玩家
        if (entity instanceof Player player) {
            if (player.getAbilities().instabuild || player.isSpectator()) {
                return false;
            }
            if (!config.includePlayers) {
                return false;
            }
        }

        // 只处理LivingEntity
        if (!(entity instanceof LivingEntity target)) {
            return false;
        }

        // 排除同种类实体
        if (config.excludeSameKind && source instanceof BaseMob sourceBase &&
                target instanceof BaseMob targetBase) {

            return false;
        }

        // 自定义过滤器
        if (config.customTargetFilter != null) {
            return config.customTargetFilter.apply(source, target);
        }

        return true;
    }

    /**
     * 检查目标是否在圆柱扇形内
     */
    private static boolean isInCylindricalSector(LivingEntity source, LivingEntity target,
                                                 CylindricalSectorConfig config, boolean front) {
        Vec3 sourcePos = source.position();
        Vec3 targetPos = target.position();

        // 检查垂直范围
        double verticalDiff = Math.abs(targetPos.y - sourcePos.y);
        if (verticalDiff > config.verticalRange) {
            return false;
        }

        // 计算水平方向向量
        Vec3 horizontalToTarget = new Vec3(
                targetPos.x - sourcePos.x,
                0,
                targetPos.z - sourcePos.z
        );

        // 检查水平距离
        double horizontalDistance = horizontalToTarget.length();
        if (horizontalDistance > config.horizontalRange || horizontalDistance == 0) {
            return false;
        }

        // 获取前方或后方的方向向量
        Vec3 lookVec = source.getLookAngle();
        Vec3 horizontalLookVec = new Vec3(lookVec.x, 0, lookVec.z).normalize();
        if (!front) {
            horizontalLookVec = horizontalLookVec.reverse();
        }

        // 计算点积
        Vec3 normalizedDirection = horizontalToTarget.normalize();
        double dotProduct = horizontalLookVec.dot(normalizedDirection);

        // 计算角度余弦值
        double cosAngle = Math.cos(Math.toRadians(config.angle / 2));

        return dotProduct >= cosAngle;
    }

    /**
     * 计算带距离衰减的伤害
     */
    private static float calculateDamageWithAttenuation(LivingEntity source, LivingEntity target,
                                                        CylindricalSectorConfig config) {
        if (!config.damageAttenuation) {
            return config.maxDamage;
        }

        double distance = source.distanceTo(target);
        double maxDistance = config.horizontalRange;

        if (distance >= maxDistance) {
            return config.minDamage;
        }

        // 线性衰减公式
        double attenuation = 1.0 - (distance / maxDistance) * config.attenuationFactor;
        attenuation = Math.max(0.0, Math.min(1.0, attenuation));

        float baseDamage = config.maxDamage - config.minDamage;
        return config.minDamage + baseDamage * (float)attenuation;
    }

    /**
     * 应用击退效果
     */
    private static void applyKnockback(LivingEntity source, LivingEntity target, CylindricalSectorConfig config) {
        Vec3 direction = target.position().subtract(source.position()).normalize();
        Vec3 actualKnockback = new Vec3(
                direction.x * config.knockback.x,
                direction.y * config.knockback.y + 0.2, // 添加轻微上浮
                direction.z * config.knockback.z
        );

        target.setDeltaMovement(target.getDeltaMovement().add(actualKnockback));
        target.hurtMarked = true;
    }

    /**
     * 破坏圆柱扇形内的方块
     */
    private static void breakBlocksInCylindricalSector(LivingEntity source, CylindricalSectorConfig config, boolean front) {
        Level level = source.level();
        if (level.isClientSide()) {
            return;
        }

        Vec3 center = source.position();
        Vec3 lookVec = source.getLookAngle();
        Vec3 horizontalLookVec = new Vec3(lookVec.x, 0, lookVec.z).normalize();
        if (!front) {
            horizontalLookVec = horizontalLookVec.reverse();
        }

        double cosAngle = Math.cos(Math.toRadians(config.angle / 2));

        // 计算更精确的破坏范围
        int minX = (int) (center.x - config.horizontalRange);
        int minY = calculateMinY(source, config);
        int minZ = (int) (center.z - config.horizontalRange);
        int maxX = (int) (center.x + config.horizontalRange);
        int maxY = (int) (center.y + config.verticalRange);
        int maxZ = (int) (center.z + config.horizontalRange);

        List<BlockPos> blocksToBreak = new ArrayList<>();

        // 第一步：收集所有需要破坏的方块
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    // 检查是否在圆柱扇形内
                    if (!isBlockInCylindricalSector(center, horizontalLookVec, pos,
                            config.horizontalRange, config.verticalRange, cosAngle)) {
                        continue;
                    }

                    // 检查是否需要保护脚下的地面
                    if (config.protectGround && isNearFeet(source, pos, config.groundProtectionRadius)) {
                        continue;
                    }

                    // 检查是否只破坏前方的方块
                    if (config.onlyBreakFrontBlocks && isBehindSource(source, pos, front)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);

                    // 跳过不可破坏的方块
                    if (shouldSkipBlock(state)) {
                        continue;
                    }

                    // 检查液体
                    if (!config.includeLiquids && !state.getFluidState().isEmpty()) {
                        continue;
                    }

                    // 检查方块硬度
                    float hardness = state.getDestroySpeed(level, pos);
                    if (hardness >= 0 && hardness <= config.blockBreakHardness) {
                        blocksToBreak.add(pos);
                    }
                }
            }
        }

        // 第二步：按从远到近的顺序破坏方块（避免连锁反应导致意外）
        blocksToBreak.sort((pos1, pos2) -> {
            double dist1 = center.distanceToSqr(pos1.getX() + 0.5, pos1.getY() + 0.5, pos1.getZ() + 0.5);
            double dist2 = center.distanceToSqr(pos2.getX() + 0.5, pos2.getY() + 0.5, pos2.getZ() + 0.5);
            return Double.compare(dist2, dist1); // 从远到近
        });

        // 第三步：执行破坏
        for (BlockPos pos : blocksToBreak) {
            level.destroyBlock(pos, false, source);
        }
    }

    /**
     * 计算破坏的最小Y坐标，避免破坏脚下的方块
     */
    private static int calculateMinY(LivingEntity source, CylindricalSectorConfig config) {
        // 保护脚下的地面，不从地面开始破坏
        int sourceY = (int) Math.floor(source.getY());
        int eyeHeight = (int) source.getEyeHeight();

        // 如果保护地面，则从源实体的腰部位置开始破坏
        if (config.protectGround) {
            return sourceY + eyeHeight / 3; // 从腰部高度开始
        }

        return (int) (source.getY() - config.verticalRange);
    }

    /**
     * 检查方块是否在角色附近（保护脚下地面）
     */
    private static boolean isNearFeet(LivingEntity source, BlockPos pos, float radius) {
        double dx = pos.getX() + 0.5 - source.getX();
        double dz = pos.getZ() + 0.5 - source.getZ();
        double dy = pos.getY() + 0.5 - source.getY();

        // 检查水平距离
        double horizontalDistanceSqr = dx * dx + dz * dz;
        if (horizontalDistanceSqr > radius * radius) {
            return false;
        }

        // 检查垂直距离（只保护脚下的方块）
        return dy <= 1.0 && dy >= -0.5; // 保护脚下0.5格到头上1格的方块
    }

    /**
     * 检查方块是否在源实体的后方
     */
    private static boolean isBehindSource(LivingEntity source, BlockPos pos, boolean front) {
        Vec3 sourcePos = source.position();
        Vec3 toBlock = new Vec3(
                pos.getX() + 0.5 - sourcePos.x,
                0,
                pos.getZ() + 0.5 - sourcePos.z
        );

        Vec3 lookVec = source.getLookAngle();
        Vec3 horizontalLookVec = new Vec3(lookVec.x, 0, lookVec.z).normalize();

        if (!front) {
            horizontalLookVec = horizontalLookVec.reverse();
        }

        double dotProduct = horizontalLookVec.dot(toBlock.normalize());
        return dotProduct < 0; // 点积为负表示在后方
    }

    /**
     * 检查方块是否在圆柱扇形内
     */
    private static boolean isBlockInCylindricalSector(Vec3 center, Vec3 direction, BlockPos pos,
                                                      float horizontalRange, float verticalRange, double cosAngle) {
        // 计算方块中心坐标
        Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        // 检查垂直范围
        double verticalDiff = Math.abs(blockCenter.y - center.y);
        if (verticalDiff > verticalRange) {
            return false;
        }

        // 计算水平方向向量
        Vec3 toBlock = new Vec3(
                blockCenter.x - center.x,
                0,
                blockCenter.z - center.z
        );

        // 检查水平距离
        double horizontalDistance = toBlock.horizontalDistance();
        if (horizontalDistance > horizontalRange || horizontalDistance == 0) {
            return false;
        }

        // 检查角度
        Vec3 normalizedDirection = toBlock.normalize();
        double dotProduct = direction.dot(normalizedDirection);

        return dotProduct >= cosAngle;
    }

    /**
     * 跳过某些特殊方块
     */
    private static boolean shouldSkipBlock(BlockState state) {
        // 跳过基岩
        if (state.getBlock() == Blocks.BEDROCK) {
            return true;
        }

        // 跳过空气
        if (state.isAir()) {
            return true;
        }

        // 跳过屏障
        if (state.getBlock() == Blocks.BARRIER) {
            return true;
        }

        // 跳过末地传送门框架
        if (state.getBlock() == Blocks.END_PORTAL_FRAME) {
            return true;
        }

        return false;
    }

    // ============== 新增的专门方法 ==============

    /**
     * 执行标准泥土破坏攻击（前方扇形）
     * 修复了破坏脚下地面的问题
     */
    public static void executeStandardDirtBreakAttack(LivingEntity source) {
        CylindricalSectorConfig config = CylindricalSectorConfig.builder(source)
                .horizontalRange(3.1f)
                .verticalRange(2.2f)
                .angle(90.0f) // 90度扇形
                .damage(4.0f, 20.0f) // 最小伤害4点，最大伤害20点
                .damageLevel(2) // 使用虚空伤害，可破无敌帧
                .knockback(Vec3.ZERO) // 不击退
                .excludeSameKind(true) // 排除同类
                .includePlayers(true) // 包含玩家
                .damageAttenuation(false) // 不随距离衰减
                .applyKnockback(false) // 不应用击退
                .destroyBlocks(true) // 破坏方块
                .blockBreakHardness(0.6f) // 泥土硬度为0.6，可以破坏硬度<=0.6的方块
                .includeLiquids(false) // 不破坏液体
                .hurtTick(true) // 可破无敌帧
                .protectGround(true) // 保护脚下地面
                .groundProtectionRadius(1.5f) // 保护半径1.5格
                .onlyBreakFrontBlocks(true) // 只破坏前方的方块
                .build();

        executeFrontCylindricalSectorAttack(config);
    }

    /**
     * 执行安全的泥土破坏攻击（不会破坏脚下的方块）
     */
    public static void executeSafeDirtBreakAttack(LivingEntity source) {
        CylindricalSectorConfig config = CylindricalSectorConfig.builder(source)
                .horizontalRange(3.1f)
                .verticalRange(2.0f) // 降低垂直范围，避免破坏过高或过低
                .angle(90.0f)
                .damage(4.0f, 20.0f)
                .damageLevel(2)
                .knockback(Vec3.ZERO)
                .excludeSameKind(true)
                .includePlayers(true)
                .damageAttenuation(false)
                .applyKnockback(false)
                .destroyBlocks(true)
                .blockBreakHardness(0.6f)
                .includeLiquids(false)
                .hurtTick(true)
                .protectGround(true)
                .groundProtectionRadius(2.0f) // 更大的保护半径
                .onlyBreakFrontBlocks(true)
                .build();

        executeFrontCylindricalSectorAttack(config);
    }

    /**
     * 执行带阶梯效果的泥土破坏攻击（破坏从腰部向前延伸的方块）
     */
    public static void executeSteppedDirtBreakAttack(LivingEntity source) {
        CylindricalSectorConfig config = CylindricalSectorConfig.builder(source)
                .horizontalRange(3.1f)
                .verticalRange(1.5f) // 较小的垂直范围
                .angle(90.0f)
                .damage(4.0f, 20.0f)
                .damageLevel(2)
                .knockback(Vec3.ZERO)
                .excludeSameKind(true)
                .includePlayers(true)
                .damageAttenuation(false)
                .applyKnockback(false)
                .destroyBlocks(true)
                .blockBreakHardness(0.6f)
                .includeLiquids(false)
                .hurtTick(true)
                .protectGround(true)
                .groundProtectionRadius(1.0f)
                .onlyBreakFrontBlocks(true)
                .build();

        executeFrontCylindricalSectorAttack(config);
    }

    /**
     * 执行标准泥土破坏攻击（带自定义角度和方向）
     */
    public static void executeCustomDirtBreakAttack(LivingEntity source, float angle, boolean front) {
        CylindricalSectorConfig config = CylindricalSectorConfig.builder(source)
                .horizontalRange(3.1f)
                .verticalRange(2.2f)
                .angle(angle)
                .damage(4.0f, 20.0f)
                .damageLevel(2)
                .knockback(Vec3.ZERO)
                .excludeSameKind(true)
                .includePlayers(true)
                .damageAttenuation(false)
                .applyKnockback(false)
                .destroyBlocks(true)
                .blockBreakHardness(0.6f)
                .includeLiquids(false)
                .hurtTick(true)
                .protectGround(true)
                .groundProtectionRadius(1.5f)
                .onlyBreakFrontBlocks(true)
                .build();

        if (front) {
            executeFrontCylindricalSectorAttack(config);
        } else {
            executeBackCylindricalSectorAttack(config);
        }
    }

    /**
     * 执行标准泥土破坏攻击（完全自定义）
     */
    public static void executeCustomDirtBreakAttack(CylindricalSectorConfig config) {
        executeFrontCylindricalSectorAttack(config);
    }

    /**
     * 简单的球形范围攻击（不使用圆柱扇形）
     */
    public static void executeSphericalAttack(LivingEntity source, float radius, float minDamage,
                                              float maxDamage, int damageLevel, boolean excludeSameKind,
                                              boolean includePlayers, boolean damageAttenuation) {
        CylindricalSectorConfig config = CylindricalSectorConfig.builder(source)
                .horizontalRange(radius)
                .verticalRange(radius)
                .angle(360.0f) // 全方向
                .damage(minDamage, maxDamage)
                .damageLevel(damageLevel)
                .excludeSameKind(excludeSameKind)
                .includePlayers(includePlayers)
                .damageAttenuation(damageAttenuation)
                .protectGround(false) // 球形攻击不保护地面
                .onlyBreakFrontBlocks(false) // 球形攻击破坏所有方向
                .build();

        // 使用前方180度和后方180度组合实现球形
        executeFrontCylindricalSectorAttack(config);
        executeBackCylindricalSectorAttack(config);
    }

    /**
     * 快速前方扇形攻击（简化的方法）
     */
    public static void quickFrontSectorAttack(LivingEntity source, float range, float angle, float damage) {
        CylindricalSectorConfig config = CylindricalSectorConfig.builder(source)
                .horizontalRange(range)
                .verticalRange(2.0f)
                .angle(angle)
                .damage(0.0f, damage)
                .damageLevel(2)
                .knockback(new Vec3(0.3, 0.1, 0.3))
                .hurtTick(true)
                .protectGround(true)
                .groundProtectionRadius(1.0f)
                .onlyBreakFrontBlocks(true)
                .build();

        executeFrontCylindricalSectorAttack(config);
    }

    /**
     * 创建冲击波效果（带方块破坏）
     */
    public static void createShockwave(LivingEntity source, float range, float damage, boolean breakBlocks) {
        CylindricalSectorConfig config = CylindricalSectorConfig.builder(source)
                .horizontalRange(range)
                .verticalRange(1.0f)
                .angle(360.0f)
                .damage(damage * 0.5f, damage)
                .damageLevel(3)
                .knockback(new Vec3(0.5, 0.3, 0.5))
                .damageAttenuation(true)
                .attenuationFactor(0.8f)
                .destroyBlocks(breakBlocks)
                .blockBreakHardness(30.0f)
                .hurtTick(true)
                .protectGround(true) // 冲击波也保护地面
                .groundProtectionRadius(1.0f)
                .onlyBreakFrontBlocks(false) // 冲击波向所有方向扩散
                .build();

        executeFrontCylindricalSectorAttack(config);
    }

    /**
     * 破坏面前的地面攻击（向下挖掘效果）
     */
    public static void executeGroundBreakAttack(LivingEntity source, float range, float depth) {
        CylindricalSectorConfig config = CylindricalSectorConfig.builder(source)
                .horizontalRange(range)
                .verticalRange(depth)
                .angle(90.0f)
                .damage(0.0f, 0.0f) // 只破坏方块，不造成伤害
                .damageLevel(0)
                .knockback(Vec3.ZERO)
                .excludeSameKind(false)
                .includePlayers(false)
                .damageAttenuation(false)
                .applyKnockback(false)
                .destroyBlocks(true)
                .blockBreakHardness(50.0f)
                .includeLiquids(false)
                .hurtTick(false)
                .protectGround(false) // 不保护地面，专门破坏地面
                .onlyBreakFrontBlocks(true)
                .build();

        executeFrontCylindricalSectorAttack(config);
    }
}