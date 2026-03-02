package corruption.entity.custom.baseEntity;

import corruption.api.IBaseEntity;
import corruption.world.data.DifficultyData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class BaseMob extends PathfinderMob implements GeoEntity, IBaseEntity {
    //=============== 数据同步 ===============
    private static final EntityDataAccessor<Integer> KILL_COUNT = SynchedEntityData.defineId(BaseMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_FAKE_DYING = SynchedEntityData.defineId(BaseMob.class, EntityDataSerializers.BOOLEAN);

    //=============== 配置字段 ===============
    protected String kind = "unknown";
    protected int doHurtTime = 0;
    protected int doHurtCooldown = 0;
    protected float doHurtDistance = 0;
    protected int dieRandom = 50;
    protected int dieTime = 0;
    protected int fakeDeathTimer = 30;
    protected boolean attackSameKind = false; // 新增：是否攻击同种类生物

    //=============== 状态标志 ===============
    protected boolean isTargetingDisabled = false;

    //=============== 伤害相关 ===============
    protected float lastHealth = 0.0f;
    @Nullable protected DamageSource lastDamageSource = null;

    //=============== 回调函数 ===============
    @Nullable protected BiPredicate<BaseMob, LivingEntity> customTargetValidator = null;
    @Nullable protected Consumer<LivingEntity> onTargetFoundCallback = null;
    @Nullable protected Consumer<LivingEntity> onDoHurtCallback = null;
    @Nullable protected Consumer<LivingEntity> onKillTargetCallback = null;
    @Nullable protected Runnable onDieAnimStartCallback = null;
    @Nullable protected Runnable onDieAnimEndCallback = null;
    @Nullable protected Consumer<Float> onSetHealthCallback = null;

    //=============== 性能优化字段 ===============
    @Nullable
    private AABB cachedBoundingBox = null;
    private long lastBoundingBoxUpdateTick = 0;
    private static final int BOUNDING_BOX_CACHE_DURATION = 5;
    private static final double MAX_ENTITY_RADIUS = 1.0;
    private static final double MAX_ENTITY_HEIGHT = 2.0;

    private float cachedAttackRangeSqr = 0.0f;
    private boolean attackRangeCacheValid = false;

    //=============== 构造与数据 ===============
    public BaseMob(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.entityData.define(KILL_COUNT, 0);
        this.entityData.define(IS_FAKE_DYING, false);
        this.lastHealth = this.getHealth();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kill_count", this.getKillCount());
        tag.putBoolean("IsFakeDying", this.isFakeDying());
        tag.putFloat("LastHealth", this.lastHealth);
        tag.putString("Kind", this.kind);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Kill_count")) {
            this.setKillCount(tag.getInt("Kill_count"));
        }
        if (tag.contains("IsFakeDying")) {
            this.setFakeDying(tag.getBoolean("IsFakeDying"));
        }
        if (tag.contains("LastHealth")) {
            this.lastHealth = tag.getFloat("LastHealth");
        }
        if (tag.contains("Kind")) {
            this.kind = tag.getString("Kind");
        }
    }

    //=============== 性能优化方法 ===============

    /**
     * 获取缓存的自身碰撞箱，减少getBoundingBox()调用
     */
    private AABB getCachedBoundingBox() {
        long currentTick = this.level().getGameTime();
        if (cachedBoundingBox == null || currentTick - lastBoundingBoxUpdateTick > BOUNDING_BOX_CACHE_DURATION) {
            cachedBoundingBox = this.getBoundingBox();
            lastBoundingBoxUpdateTick = currentTick;
        }
        return cachedBoundingBox;
    }

    /**
     * 获取攻击范围平方（缓存）
     */
    private float getAttackRangeSqr() {
        if (!attackRangeCacheValid) {
            cachedAttackRangeSqr = this.doHurtDistance * this.doHurtDistance;
            attackRangeCacheValid = true;
        }
        return cachedAttackRangeSqr;
    }

    /**
     * 当攻击距离改变时，更新缓存
     */
    public void setDoHurtDistance(float distance) {
        this.doHurtDistance = distance;
        this.attackRangeCacheValid = false;
    }

    //=============== 优化的距离计算方法 ===============

    /**
     * 计算到实体的水平距离平方（忽略Y轴）- 快速版本
     */
    public float horizontalDistanceToSqr(Entity entity) {
        double dx = this.getX() - entity.getX();
        double dz = this.getZ() - entity.getZ();
        return (float) (dx * dx + dz * dz);
    }

    /**
     * 计算到实体的完整距离平方 - 快速版本
     */
    public double distanceToSqr(Entity entity) {
        double dx = this.getX() - entity.getX();
        double dy = this.getY() - entity.getY();
        double dz = this.getZ() - entity.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * 计算两个实体碰撞箱边缘到边缘的最小距离（精确计算）
     * 只有在中心点距离很近时才使用
     */
    public double getBoundingBoxDistanceSqrExact(Entity entity) {
        AABB thisAABB = getCachedBoundingBox();
        AABB targetAABB = entity.getBoundingBox();

        return calculateAABBDistanceSqr(thisAABB, targetAABB);
    }

    /**
     * 静态方法计算AABB距离，避免创建临时对象
     */
    private static double calculateAABBDistanceSqr(AABB a, AABB b) {
        double dx = 0;
        if (a.maxX < b.minX) {
            dx = b.minX - a.maxX;
        } else if (b.maxX < a.minX) {
            dx = a.minX - b.maxX;
        }

        double dy = 0;
        if (a.maxY < b.minY) {
            dy = b.minY - a.maxY;
        } else if (b.maxY < a.minY) {
            dy = a.minY - b.maxY;
        }

        double dz = 0;
        if (a.maxZ < b.minZ) {
            dz = b.minZ - a.maxZ;
        } else if (b.maxZ < a.minZ) {
            dz = a.minZ - b.maxZ;
        }

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * 优化的碰撞箱边缘到边缘距离计算（带快速过滤）
     * 用于普通攻击判断
     */
    public double getBoundingBoxHorizontalDistanceSqr(Entity entity) {
        // 1. 快速检查：中心点水平距离
        double dxCenter = this.getX() - entity.getX();
        double dzCenter = this.getZ() - entity.getZ();
        double centerDistanceSqr = dxCenter * dxCenter + dzCenter * dzCenter;

        // 2. 快速过滤：如果中心点距离已经很大，直接返回保守估计值
        // 保守估计：中心距离 - 4 * 最大半径平方（因为两边各有一个半径）
        double quickMaxDistanceSqr = centerDistanceSqr - (MAX_ENTITY_RADIUS * MAX_ENTITY_RADIUS * 4);
        if (quickMaxDistanceSqr > 0) {
            return Math.max(quickMaxDistanceSqr, centerDistanceSqr);
        }

        // 3. 中心点距离很近，需要精确计算
        return getBoundingBoxHorizontalDistanceSqrExact(entity);
    }

    /**
     * 精确计算水平碰撞箱边缘距离
     */
    private double getBoundingBoxHorizontalDistanceSqrExact(Entity entity) {
        AABB thisAABB = getCachedBoundingBox();
        AABB targetAABB = entity.getBoundingBox();

        double dx = 0;
        if (thisAABB.maxX < targetAABB.minX) {
            dx = targetAABB.minX - thisAABB.maxX;
        } else if (targetAABB.maxX < thisAABB.minX) {
            dx = thisAABB.minX - targetAABB.maxX;
        }

        double dz = 0;
        if (thisAABB.maxZ < targetAABB.minZ) {
            dz = targetAABB.minZ - thisAABB.maxZ;
        } else if (targetAABB.maxZ < thisAABB.minX) {
            dz = thisAABB.minZ - targetAABB.maxZ;
        }

        return dx * dx + dz * dz;
    }

    /**
     * 优化的方向距离计算
     */
    public double getDistanceInDirection(Vec3 direction, Entity target) {
        // 快速检查：方向向量是否有效
        double dirLengthSqr = direction.lengthSqr();
        if (dirLengthSqr < 1e-6) {
            return Double.MAX_VALUE;
        }

        // 如果方向向量没有归一化，进行归一化（只计算一次）
        Vec3 normalizedDir;
        if (Math.abs(dirLengthSqr - 1.0) > 1e-6) {
            normalizedDir = direction.normalize();
        } else {
            normalizedDir = direction;
        }

        AABB thisAABB = getCachedBoundingBox();
        AABB targetAABB = target.getBoundingBox();

        // 计算边缘点
        double thisEdgeX = normalizedDir.x > 0 ? thisAABB.maxX : thisAABB.minX;
        double thisEdgeY = normalizedDir.y > 0 ? thisAABB.maxY : thisAABB.minY;
        double thisEdgeZ = normalizedDir.z > 0 ? thisAABB.maxZ : thisAABB.minZ;

        double targetEdgeX = normalizedDir.x > 0 ? targetAABB.minX : targetAABB.maxX;
        double targetEdgeY = normalizedDir.y > 0 ? targetAABB.minY : targetAABB.maxY;
        double targetEdgeZ = normalizedDir.z > 0 ? targetAABB.minZ : targetAABB.maxZ;

        // 计算向量差和点积
        double dx = targetEdgeX - thisEdgeX;
        double dy = targetEdgeY - thisEdgeY;
        double dz = targetEdgeZ - thisEdgeZ;

        return dx * normalizedDir.x + dy * normalizedDir.y + dz * normalizedDir.z;
    }

    /**
     * 快速方向距离计算（用于近战攻击快速判断）
     * 使用简化计算，只考虑水平方向
     */
    public double getHorizontalDistanceInDirection(Vec3 direction, Entity target) {
        // 只计算水平方向
        double dirLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (dirLength < 1e-6) {
            return Double.MAX_VALUE;
        }

        // 归一化水平方向
        double invDirLength = 1.0 / dirLength;
        double normX = direction.x * invDirLength;
        double normZ = direction.z * invDirLength;

        AABB thisAABB = getCachedBoundingBox();
        AABB targetAABB = target.getBoundingBox();

        // 只计算水平边缘
        double thisEdgeX = normX > 0 ? thisAABB.maxX : thisAABB.minX;
        double thisEdgeZ = normZ > 0 ? thisAABB.maxZ : thisAABB.minZ;

        double targetEdgeX = normX > 0 ? targetAABB.minX : targetAABB.maxX;
        double targetEdgeZ = normZ > 0 ? targetAABB.minZ : targetAABB.maxZ;

        // 水平距离点积
        double dx = targetEdgeX - thisEdgeX;
        double dz = targetEdgeZ - thisEdgeZ;

        return dx * normX + dz * normZ;
    }

    /**
     * 概率判断方法（百分比）
     */
    public boolean randomPercentage(int percentage) {
        return this.getRandom().nextInt(100) < percentage;
    }

    /**
     * 判断是否能看到目标
     */
    public boolean canSeeTarget(LivingEntity target) {
        return this.hasLineOfSight(target);
    }

    /**
     * 获取最后造成伤害的来源位置
     */
    @Nullable
    public Vec3 getLastDamageSourcePosition() {
        if (lastDamageSource == null || lastDamageSource.getEntity() == null) {
            return null;
        }

        Entity sourceEntity = lastDamageSource.getEntity();
        return new Vec3(
                sourceEntity.getX(),
                sourceEntity.getY() + sourceEntity.getEyeHeight() / 2.0,
                sourceEntity.getZ()
        );
    }

    /**
     * 获取从伤害来源指向实体的方向
     */
    @Nullable
    public Vec3 getDirectionFromDamageSource() {
        Vec3 sourcePos = getLastDamageSourcePosition();
        if (sourcePos == null) {
            return null;
        }

        Vec3 thisPos = new Vec3(
                this.getX(),
                this.getY() + this.getEyeHeight() / 2.0,
                this.getZ()
        );

        return thisPos.subtract(sourcePos).normalize();
    }

    //=============== 优化的目标检查方法 ===============

    /**
     * 优化的目标检查方法，减少性能开销
     */
    public boolean canAttackTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        // 基本检查
        if (!this.canAttack(target)) {
            return false;
        }

        // 检查同种类
        if (!this.attackSameKind && target instanceof BaseMob other) {
            String thisKind = this.getKind();
            String otherKind = other.getKind();
            if (thisKind != null && otherKind != null && thisKind.equals(otherKind)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 优化的距离检查
     */
    public boolean isTargetInRange(LivingEntity target, double range) {
        if (target == null) return false;

        // 使用水平距离检查，减少计算开销
        double dx = this.getX() - target.getX();
        double dz = this.getZ() - target.getZ();
        double distanceSqr = dx * dx + dz * dz;

        return distanceSqr <= (range * range);
    }

    //=============== 配置方法 ===============
    public void setDoHurtTime(int cooldown) {
        this.doHurtTime = cooldown;
    }

    public void setDieTime(int time) {
        this.dieTime = time;
    }

    public void setDieRandom(int random) {
        this.dieRandom = random;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public int getDifficultNumber() {
        Level level = this.level();
        // 仅在服务端且 level 是 ServerLevel 时获取真实难度
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            return DifficultyData.get(serverLevel).getDifficulty().getValue();
        }
        // 客户端返回默认值（例如 0 或和平难度 0）
        return 0;
    }

    public String getKind() {
        return kind;
    }

    public int getKillCount() {
        return entityData.get(KILL_COUNT);
    }

    public void setKillCount(int count) {
        entityData.set(KILL_COUNT, count);
    }

    public boolean isFakeDying() {
        return entityData.get(IS_FAKE_DYING);
    }

    public void setFakeDying(boolean fakeDying) {
        entityData.set(IS_FAKE_DYING, fakeDying);
    }

    public void disableTargeting() {
        this.isTargetingDisabled = true;
        this.setTarget(null);
    }

    public void enableTargeting() {
        this.isTargetingDisabled = false;
    }

    public boolean isTargetingEnabled() {
        return !isTargetingDisabled;
    }

    public void setAttackSameKind(boolean attackSameKind) {
        this.attackSameKind = attackSameKind;
    }

    public boolean shouldAttackSameKind() {
        return attackSameKind;
    }

    //=============== 回调设置方法 ===============
    public void setCustomTargetValidator(BiPredicate<BaseMob, LivingEntity> validator) {
        this.customTargetValidator = validator;
    }

    public void setOnTargetFoundCallback(Consumer<LivingEntity> callback) {
        this.onTargetFoundCallback = callback;
    }

    public void setOnDoHurtCallback(Consumer<LivingEntity> callback) {
        this.onDoHurtCallback = callback;
    }

    public void setOnKillTargetCallback(Consumer<LivingEntity> callback) {
        this.onKillTargetCallback = callback;
    }

    public void setOnDieAnimStartCallback(Runnable callback) {
        this.onDieAnimStartCallback = callback;
    }

    public void setOnDieAnimEndCallback(Runnable callback) {
        this.onDieAnimEndCallback = callback;
    }

    public void setOnSetHealthCallback(Consumer<Float> callback) {
        this.onSetHealthCallback = callback;
    }

    //=============== 死亡动画处理 ===============
    protected void startDieAnimation() {
        if (isFakeDying()) return;

        this.setDeltaMovement(Vec3.ZERO);
        this.setFakeDying(true);
        this.fakeDeathTimer = this.dieTime;
        this.triggerAnim("die", "die");
        this.setHealth(this.getMaxHealth());

        if (onDieAnimStartCallback != null) {
            onDieAnimStartCallback.run();
        }
    }

    //=============== 伤害处理相关方法 ===============
    /**
     * 当实体受到伤害并减少血量时调用
     */
    protected void beHurtByHealth(DamageSource damageSource, float damageAmount, float healthReduced) {
        // 默认实现为空，子类可以重写
    }

    /**
     * 检查血量是否减少并调用相应的回调
     */
    private void checkHealthReduction(DamageSource damageSource, float damageAmount, float newHealth) {
        float oldHealth = this.lastHealth;
        float healthReduced = oldHealth - newHealth;

        if (healthReduced > 0) {
            this.lastDamageSource = damageSource;
            this.beHurtByHealth(damageSource, damageAmount, healthReduced);
            this.beHurtForSetHealth(healthReduced);
        }

        this.lastHealth = newHealth;
    }

    //=============== 优化的逻辑处理 ===============
    @Override
    public void tick() {
        super.tick();

        // 更新攻击冷却
        if (doHurtCooldown > 0) {
            doHurtCooldown--;
            return;
        }

        // 处理假死亡状态
        if (isFakeDying()) {
            if (fakeDeathTimer > 0) {
                fakeDeathTimer--;
            } else {
                this.remove(RemovalReason.KILLED);
                this.onDieAnimEnd();
            }
            return;
        }

        // 攻击判断
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            // 简化距离检查，减少计算
            double distanceSqr = this.distanceToSqr(target);
            float attackRangeSqr = this.doHurtDistance * this.doHurtDistance;

            // 增加一点容差，避免浮点误差
            if (distanceSqr <= attackRangeSqr * 1.1) {
                // 简单视线检查（攻击时需要）
                if (this.hasLineOfSight(target)) {
                    if (tryDoHurtGoal(target)) {
                        doHurtCooldown = doHurtTime;

                        if (!target.isAlive()) {
                            onKillTarget(target);
                            this.setKillCount(this.getKillCount() + 1);
                        }
                    }
                }
            }
        }
    }

    /**
     * 尝试攻击目标
     */
    protected boolean tryDoHurtGoal(LivingEntity target) {
        boolean result = doHurtGoal(target);
        if (result && onDoHurtCallback != null) {
            try {
                onDoHurtCallback.accept(target);
            } catch (Exception e) {
            }
        }
        return result;
    }

    //=============== 虚方法（供子类重写）===============
    /**
     * 攻击目标时的逻辑，返回是否成功攻击
     */
    public boolean doHurtGoal(LivingEntity target) {
        // 简单距离检查
        double distance = this.distanceToSqr(target);
        return distance <= (this.doHurtDistance * this.doHurtDistance);
    }

    /**
     * 死亡动画结束时的逻辑
     */
    public void onDieAnimEnd() {
        if (onDieAnimEndCallback != null) {
            try {
                onDieAnimEndCallback.run();
            } catch (Exception e) {

            }
        }
    }

    /**
     * 实体真实死亡时的逻辑
     */
    public void onEntityDie() {
    }

    /**
     * 击杀目标时的逻辑
     */
    public void onKillTarget(LivingEntity target) {
        if (onKillTargetCallback != null) {
            try {
                onKillTargetCallback.accept(target);
            } catch (Exception e) {

            }
        }
    }

    /**
     * 受到伤害时的额外逻辑
     */
    public void beHurtForSetHealth(float damage) {
        // 默认实现为空，子类可以重写
    }

    /**
     * 发现目标时的回调，用于子类自定义目标验证
     * 修复了回调异常处理和返回值逻辑
     */
    public boolean foundGoal(LivingEntity entity) {
        if (onTargetFoundCallback != null) {
            try {
                onTargetFoundCallback.accept(entity);
            } catch (Exception e) {
            }
        }

        if (customTargetValidator != null) {
            try {
                return customTargetValidator.test(this, entity);
            } catch (Exception e) {

                return true; // 出错时默认返回true，允许攻击
            }
        }
        return true; // 默认返回true，允许攻击
    }

    //=============== 动画控制 ===============
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    protected final RawAnimation REST = RawAnimation.begin().thenLoop("idle");
    protected final RawAnimation RUN = RawAnimation.begin().thenLoop("walk");
    protected final RawAnimation DIE = RawAnimation.begin().thenLoop("die");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::handleMainAnimations));
        controllers.add(new AnimationController<>(this, "die", 5, this::handleDieAnimations)
                .triggerableAnim("die", DIE));
    }

    protected <E extends BaseMob> PlayState handleMainAnimations(AnimationState<E> state) {
        if (isFakeDying()) {
            return PlayState.STOP;
        }

        if (state.isMoving()) {
            return state.setAndContinue(RUN);
        }
        return state.setAndContinue(REST);
    }

    protected <E extends BaseMob> PlayState handleDieAnimations(AnimationState<E> state) {
        return isFakeDying() ? state.setAndContinue(DIE) : PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    //=============== 重写方法 ===============
    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void setHealth(float newHealth) {
        float oldHealth = this.getHealth();
        super.setHealth(newHealth);

        if (oldHealth > newHealth) {
            float damage = oldHealth - newHealth;
            this.beHurtForSetHealth(damage);

            if (onSetHealthCallback != null) {
                try {
                    onSetHealthCallback.accept(damage);
                } catch (Exception e) {
                }
            }
        }

        this.lastHealth = newHealth;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        if (isFakeDying()) {
            return false;
        }

        float currentHealth = this.getHealth();
        boolean result = super.hurt(damageSource, amount);

        if (result) {
            float newHealth = this.getHealth();
            this.checkHealthReduction(damageSource, amount, newHealth);
        }

        return result;
    }

    @Override
    public void die(DamageSource damageSource) {
        if (this.randomPercentage(dieRandom)) {
            startDieAnimation();
        } else {
            super.die(damageSource);
            this.onEntityDie();
        }
    }

    // =============== 修复：假死时禁止AI、移动和推动 ===============
    @Override
    public boolean isEffectiveAi() {
        // 假死状态下不执行任何AI逻辑（包括移动、目标选择、头部旋转等）
        if (this.isFakeDying()) {
            return false;
        }
        return super.isEffectiveAi();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isFakeDying()) {
            // 假死时禁止移动，速度强制归零
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public boolean isPushable() {
        // 假死时禁止被其他实体推动
        if (this.isFakeDying()) {
            return false;
        }
        return super.isPushable();
    }

    //=============== 性能监控方法（可选）===============
    /**
     * 重置所有性能缓存（在实体尺寸变化时调用）
     */
    public void resetPerformanceCache() {
        this.cachedBoundingBox = null;
        this.attackRangeCacheValid = false;
        this.lastBoundingBoxUpdateTick = 0;
    }

    /**
     * 获取攻击距离
     */
    public float getDoHurtDistance() {
        return doHurtDistance;
    }

    /**
     * 获取攻击冷却时间
     */
    public int getDoHurtTime() {
        return doHurtTime;
    }

    /**
     * 获取攻击冷却剩余时间
     */
    public int getDoHurtCooldown() {
        return doHurtCooldown;
    }
}