package corruption.entity.custom.refactored;

import corruption.entity.ai.SmartMoveGoal;
import corruption.entity.ai.SmartTargetGoal;
import corruption.entity.custom.baseEntity.Host;
import corruption.init.ModItems;
import corruption.init.ModSounds;
import corruption.util.combat.AureAttackUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;

public class Shocker extends Host implements GeoEntity {

    public static final String registryName = "shocker";
    protected int attackCooldown = 30;

    // 新增：碰撞攻击冷却（单位：tick）
    private int collisionAttackCooldown = 0;
    // 新增：记录上一tick的水平/垂直碰撞状态，用于检测瞬间碰撞
    private boolean wasHorizCollided = false;
    private boolean wasVertCollided = false;

    public Shocker(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setDoHurtTime(20);
        this.setDoHurtDistance(3.2f);
        this.setDieRandom(40);
        this.setDieTime(17);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.FOLLOW_RANGE, 1.0D)
                .add(Attributes.ARMOR, 5.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4D, false));
        this.goalSelector.addGoal(2, new SmartMoveGoal.Builder(this)
                .speed(4f)
                .maxJumpHeight(2)
                .maxJumpWidth(6)
                .avoidDangerousFluids(true)
                .debug(false)
                .build());

        this.targetSelector.addGoal(1, new SmartTargetGoal.Builder(this)
                .range(35)
                .throughWall(false)
                .attackOtherBaseMobs(false)
                .build());
    }

    // ========== 攻击逻辑 ==========

    public void fullyCustomAttack() {
        AureAttackUtil.CylindricalSectorConfig config = AureAttackUtil.CylindricalSectorConfig.builder(this)
                .horizontalRange(3.2f)
                .verticalRange(3.2f)
                .angle(120.0f)
                .damage(6.0f, 10.0f)
                .damageLevel(1)
                .setHealthDamage(1)
                .knockback(new Vec3(0.5, 0.3, 0.5))
                .excludeSameKind(true)
                .includePlayers(true)
                .damageAttenuation(true)
                .attenuationFactor(0.8f)
                .applyKnockback(true)
                .destroyBlocks(true)
                .includeLiquids(true)
                .hurtTick(true)
                .protectGround(true)
                .build();

        AureAttackUtil.executeCustomDirtBreakAttack(config);
    }

    @Override
    public void tick() {
        super.tick();

        // 原有目标攻击冷却
        if (this.getTarget() != null && this.attackCooldown > 0) {
            attackCooldown--;
            if (attackCooldown <= 0) {
                fullyCustomAttack();
            }
        }

        // 新增：碰撞攻击冷却递减（确保不小于0）
        if (collisionAttackCooldown > 0) {
            collisionAttackCooldown--;
        }

        // 仅在服务端处理碰撞攻击
        if (!this.level().isClientSide) {
            // 检测方块碰撞：当前tick的碰撞状态
            boolean horizCollision = this.horizontalCollision;
            boolean vertCollision = this.verticalCollision;

            // 如果冷却结束，且刚刚发生了水平或垂直碰撞（从无到有），则触发攻击
            if (collisionAttackCooldown <= 0) {
                if ((horizCollision && !wasHorizCollided) || (vertCollision && !wasVertCollided)) {
                    fullyCustomAttack();
                    collisionAttackCooldown = 5; // 设置最小间隔5刻
                }
            }

            // 更新上一tick的碰撞状态
            wasHorizCollided = horizCollision;
            wasVertCollided = vertCollision;
        }
    }

    @Override
    public boolean doHurtGoal(LivingEntity target) {
        fullyCustomAttack();
        //target.hurt(target.damageSources().mobAttack(this), 1 + this.getDifficultNumber());
        return true;
    }

    // ========== 新增：实体碰撞触发攻击 ==========
    @Override
    public void push(Entity entity) {
        super.push(entity);
        // 仅在服务端、冷却结束且不是与自己碰撞时触发
        if (!this.level().isClientSide && collisionAttackCooldown <= 0 && entity != this) {
            fullyCustomAttack();
            collisionAttackCooldown = 5; // 设置最小间隔5刻
        }
    }

    // ========== 声音 ==========

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.LONG_ARM_BEAST_CALL_2.get(); }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) { return null; }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.LONG_ARM_BEAST_CALL_1.get(); }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);
        this.spawnAtLocation(new ItemStack(ModItems.LONG_ARM_BEAST_TOOTH.get()));
        this.spawnAtLocation(new ItemStack(ModItems.INFECTION_MEAT.get()));
    }
}