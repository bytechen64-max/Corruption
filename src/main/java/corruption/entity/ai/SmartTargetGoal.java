package corruption.entity.ai;

import corruption.entity.custom.baseEntity.BaseMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.ambient.Bat;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * SmartTargetGoal —— BaseMob 高度可配置索敌 AI
 *
 * 特性：
 *  - 排除 BaseMob 及其子类（不互相攻击，除非配置允许）
 *  - 排除蝙蝠、苦力怕（可扩展排除列表）
 *  - 可选：是否索敌同类（kind 相同）
 *  - 可选：是否穿墙（无视视线）
 *  - 可配置：索敌距离、视野角度、索敌间隔（tick）
 *  - 可配置：额外排除 EntityType 列表
 *  - 可配置：额外过滤谓词
 *  - 可配置：是否只索敌玩家
 *  - 可配置：是否追踪上次伤害来源
 *
 * 用法示例（在具体 Mob 的 registerGoals() 中）：
 * <pre>{@code
 * this.targetSelector.addGoal(1,
 *     new SmartTargetGoal.Builder(this)
 *         .range(16.0)
 *         .checkInterval(10)
 *         .throughWall(false)
 *         .attackSameKind(false)
 *         .build()
 * );
 * }</pre>
 */
public class SmartTargetGoal extends TargetGoal {

    // ===================== 核心字段 =====================

    private final BaseMob mob;
    private final Config config;

    @Nullable
    private LivingEntity pendingTarget = null;
    private int ticksSinceLastSearch = 0;

    // ===================== 构造 =====================

    private SmartTargetGoal(BaseMob mob, Config config) {
        super(mob, config.mustSee, config.mustReach);
        this.mob = mob;
        this.config = config;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    // ===================== 目标条件构建 =====================

    private TargetingConditions buildConditions() {
        TargetingConditions conditions = TargetingConditions.forCombat()
                .range(config.range);

        if (config.throughWall) {
            conditions = conditions.ignoreLineOfSight();
        }

        return conditions;
    }

    // ===================== GoalSelector 接口 =====================

    @Override
    public boolean canUse() {
        // 如果索敌被禁用，直接返回 false
        if (!mob.isTargetingEnabled()) return false;

        // 控制索敌检测间隔（减少性能开销）
        ticksSinceLastSearch++;
        if (ticksSinceLastSearch < config.checkInterval) return false;
        ticksSinceLastSearch = 0;

        // 如果已有存活目标，暂时不重新搜索
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive() && mob.isTargetInRange(currentTarget, config.range)) {
            return false;
        }

        // 开始搜索
        pendingTarget = findTarget();
        return pendingTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!mob.isTargetingEnabled()) return false;

        // 超出最大追踪距离则放弃
        double maxTracking = config.range * config.trackingRangeMultiplier;
        return mob.isTargetInRange(target, maxTracking);
    }

    @Override
    public void start() {
        mob.setTarget(pendingTarget);
        super.start();
    }

    @Override
    public void stop() {
        mob.setTarget(null);
        pendingTarget = null;
        super.stop();
    }

    // ===================== 核心搜索逻辑 =====================

    @Nullable
    private LivingEntity findTarget() {
        TargetingConditions conditions = buildConditions();

        List<LivingEntity> candidates = mob.level().getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(config.range),
                entity -> isValidTarget(entity, conditions)
        );

        if (candidates.isEmpty()) return null;

        // 选择最近的目标
        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double dist = mob.distanceToSqr(candidate);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = candidate;
            }
        }

        return nearest;
    }

    /**
     * 判断某个实体是否是合法攻击目标
     */
    private boolean isValidTarget(LivingEntity entity, TargetingConditions conditions) {
        // 排除自身
        if (entity == mob) return false;

        // 排除非存活
        if (!entity.isAlive()) return false;

        // 核心排除：BaseMob 及其子类（同阵营）
        if (entity instanceof BaseMob other) {
            // 如果不允许攻击同类，且 kind 相同，排除
            if (!config.attackSameKind) {
                String myKind = mob.getKind();
                String otherKind = other.getKind();
                if (myKind != null && !myKind.isEmpty() && myKind.equals(otherKind)) {
                    return false;
                }
            }
            // 如果不允许攻击任何 BaseMob 子类（默认行为）
            if (!config.attackOtherBaseMobs) {
                return false;
            }
        }

        // 默认排除：蝙蝠
        if (entity instanceof Bat) return false;

        // 默认排除：苦力怕
        if (entity instanceof Creeper) return false;

        // 检查额外排除的 EntityType 列表
        for (EntityType<?> excludedType : config.excludedEntityTypes) {
            if (entity.getType() == excludedType) return false;
        }

        // 检查额外过滤谓词（可自定义黑名单/白名单逻辑）
        if (config.extraFilter != null && !config.extraFilter.test(entity)) return false;

        // 只索敌玩家模式
        if (config.playersOnly && !(entity instanceof net.minecraft.world.entity.player.Player)) return false;

        // 距离检查
        double distSqr = mob.distanceToSqr(entity);
        if (distSqr > config.range * config.range) return false;

        // 视线检查（非穿墙模式）
        if (!config.throughWall && !mob.hasLineOfSight(entity)) return false;

        // 调用 BaseMob 的目标验证（canAttackTarget + foundGoal）
        if (!mob.canAttackTarget(entity)) return false;

        // 调用自定义回调（foundGoal 包含 customTargetValidator）
        if (!mob.foundGoal(entity)) return false;

        return true;
    }

    // ===================== 配置数据类 =====================

    /**
     * 不可变配置对象，由 Builder 构建
     */
    public static final class Config {
        final double range;
        final int checkInterval;
        final boolean throughWall;
        final boolean mustSee;
        final boolean mustReach;
        final boolean attackSameKind;
        final boolean attackOtherBaseMobs;
        final boolean playersOnly;
        final double trackingRangeMultiplier;
        final List<EntityType<?>> excludedEntityTypes;
        @Nullable final Predicate<LivingEntity> extraFilter;

        private Config(Builder b) {
            this.range = b.range;
            this.checkInterval = b.checkInterval;
            this.throughWall = b.throughWall;
            this.mustSee = b.mustSee;
            this.mustReach = b.mustReach;
            this.attackSameKind = b.attackSameKind;
            this.attackOtherBaseMobs = b.attackOtherBaseMobs;
            this.playersOnly = b.playersOnly;
            this.trackingRangeMultiplier = b.trackingRangeMultiplier;
            this.excludedEntityTypes = List.copyOf(b.excludedEntityTypes);
            this.extraFilter = b.extraFilter;
        }
    }

    // ===================== Builder =====================

    /**
     * 流式 Builder，所有参数均有合理默认值
     */
    public static final class Builder {

        private final BaseMob mob;

        // 默认参数
        private double range = 16.0;
        private int checkInterval = 10;          // 每 10 tick 搜索一次
        private boolean throughWall = false;     // 默认不穿墙
        private boolean mustSee = true;
        private boolean mustReach = false;
        private boolean attackSameKind = false;  // 默认不攻击同类
        private boolean attackOtherBaseMobs = false; // 默认不攻击其他 BaseMob 子类
        private boolean playersOnly = false;
        private double trackingRangeMultiplier = 1.5; // 追踪距离 = range * 1.5
        private final List<EntityType<?>> excludedEntityTypes = new ArrayList<>();
        @Nullable private Predicate<LivingEntity> extraFilter = null;

        public Builder(BaseMob mob) {
            this.mob = mob;
        }

        /** 索敌（搜索）距离，默认 16 */
        public Builder range(double range) {
            this.range = range;
            return this;
        }

        /** 每隔多少 tick 执行一次搜索，默认 10 */
        public Builder checkInterval(int ticks) {
            this.checkInterval = Math.max(1, ticks);
            return this;
        }

        /** 是否穿墙索敌（无视视线），默认 false */
        public Builder throughWall(boolean throughWall) {
            this.throughWall = throughWall;
            if (throughWall) {
                this.mustSee = false;
            }
            return this;
        }

        /** 是否需要有视线才能持续追踪，默认 true */
        public Builder mustSee(boolean mustSee) {
            this.mustSee = mustSee;
            return this;
        }

        /** 是否需要可到达才能持续追踪，默认 false */
        public Builder mustReach(boolean mustReach) {
            this.mustReach = mustReach;
            return this;
        }

        /** 是否攻击 kind 相同的同类 BaseMob，默认 false */
        public Builder attackSameKind(boolean attackSameKind) {
            this.attackSameKind = attackSameKind;
            return this;
        }

        /**
         * 是否允许攻击其他 BaseMob 子类（不同 kind），默认 false
         * 注意：attackSameKind 在此基础上进一步控制
         */
        public Builder attackOtherBaseMobs(boolean attackOtherBaseMobs) {
            this.attackOtherBaseMobs = attackOtherBaseMobs;
            return this;
        }

        /** 是否仅索敌玩家，默认 false */
        public Builder playersOnly(boolean playersOnly) {
            this.playersOnly = playersOnly;
            return this;
        }

        /**
         * 追踪距离倍率，追踪距离 = range × multiplier，默认 1.5
         * 超出此距离后放弃目标
         */
        public Builder trackingRangeMultiplier(double multiplier) {
            this.trackingRangeMultiplier = Math.max(1.0, multiplier);
            return this;
        }

        /** 添加额外需要排除的 EntityType（可多次调用） */
        public Builder exclude(EntityType<?> entityType) {
            this.excludedEntityTypes.add(entityType);
            return this;
        }

        /** 批量添加需要排除的 EntityType */
        public Builder excludeAll(List<EntityType<?>> types) {
            this.excludedEntityTypes.addAll(types);
            return this;
        }

        /**
         * 设置额外的过滤谓词，返回 true 表示目标合法。
         * 可用于实现白名单、自定义条件等。
         * 示例：只攻击血量低于 10 的目标
         * <pre>{@code .extraFilter(e -> e.getHealth() < 10) }</pre>
         */
        public Builder extraFilter(Predicate<LivingEntity> filter) {
            this.extraFilter = filter;
            return this;
        }

        public SmartTargetGoal build() {
            return new SmartTargetGoal(mob, new Config(this));
        }
    }
}