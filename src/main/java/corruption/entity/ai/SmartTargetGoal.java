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
 * 优化：增加垂直差过大过滤，避免锁定无法到达的目标
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
        if (!mob.isTargetingEnabled()) return false;

        ticksSinceLastSearch++;
        if (ticksSinceLastSearch < config.checkInterval) return false;
        ticksSinceLastSearch = 0;

        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive() && mob.isTargetInRange(currentTarget, config.range)) {
            return false;
        }

        pendingTarget = findTarget();
        return pendingTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!mob.isTargetingEnabled()) return false;

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
        if (entity == mob) return false;
        if (!entity.isAlive()) return false;

        // 垂直差过大时暂时不作为目标（让寻路先计算）
        int dy = Math.abs((int) (entity.getY() - mob.getY()));
        if (dy > 4 && !mob.isInWater()) {
            return false;
        }

        if (entity instanceof BaseMob other) {
            if (!config.attackSameKind) {
                String myKind = mob.getKind();
                String otherKind = other.getKind();
                if (myKind != null && !myKind.isEmpty() && myKind.equals(otherKind)) {
                    return false;
                }
            }
            if (!config.attackOtherBaseMobs) {
                return false;
            }
        }

        if (entity instanceof Bat) return false;
        if (entity instanceof Creeper) return false;

        for (EntityType<?> excludedType : config.excludedEntityTypes) {
            if (entity.getType() == excludedType) return false;
        }

        if (config.extraFilter != null && !config.extraFilter.test(entity)) return false;

        if (config.playersOnly && !(entity instanceof net.minecraft.world.entity.player.Player)) return false;

        double distSqr = mob.distanceToSqr(entity);
        if (distSqr > config.range * config.range) return false;

        if (!config.throughWall && !mob.hasLineOfSight(entity)) return false;

        if (!mob.canAttackTarget(entity)) return false;
        if (!mob.foundGoal(entity)) return false;

        return true;
    }

    // ===================== 配置数据类 =====================

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

    public static final class Builder {

        private final BaseMob mob;

        private double range = 16.0;
        private int checkInterval = 10;
        private boolean throughWall = false;
        private boolean mustSee = true;
        private boolean mustReach = false;
        private boolean attackSameKind = false;
        private boolean attackOtherBaseMobs = false;
        private boolean playersOnly = false;
        private double trackingRangeMultiplier = 1.5;
        private final List<EntityType<?>> excludedEntityTypes = new ArrayList<>();
        @Nullable private Predicate<LivingEntity> extraFilter = null;

        public Builder(BaseMob mob) { this.mob = mob; }

        public Builder range(double range) { this.range = range; return this; }
        public Builder checkInterval(int ticks) { this.checkInterval = Math.max(1, ticks); return this; }
        public Builder throughWall(boolean throughWall) { this.throughWall = throughWall; if (throughWall) this.mustSee = false; return this; }
        public Builder mustSee(boolean mustSee) { this.mustSee = mustSee; return this; }
        public Builder mustReach(boolean mustReach) { this.mustReach = mustReach; return this; }
        public Builder attackSameKind(boolean attackSameKind) { this.attackSameKind = attackSameKind; return this; }
        public Builder attackOtherBaseMobs(boolean attackOtherBaseMobs) { this.attackOtherBaseMobs = attackOtherBaseMobs; return this; }
        public Builder playersOnly(boolean playersOnly) { this.playersOnly = playersOnly; return this; }
        public Builder trackingRangeMultiplier(double multiplier) { this.trackingRangeMultiplier = Math.max(1.0, multiplier); return this; }
        public Builder exclude(EntityType<?> entityType) { this.excludedEntityTypes.add(entityType); return this; }
        public Builder excludeAll(List<EntityType<?>> types) { this.excludedEntityTypes.addAll(types); return this; }
        public Builder extraFilter(Predicate<LivingEntity> filter) { this.extraFilter = filter; return this; }

        public SmartTargetGoal build() {
            return new SmartTargetGoal(mob, new Config(this));
        }
    }
}