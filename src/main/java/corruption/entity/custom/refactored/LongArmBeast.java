package corruption.entity.custom.refactored;

import corruption.entity.ai.SmartMoveGoal;
import corruption.entity.ai.SmartTargetGoal;
import corruption.entity.custom.baseEntity.BaseMob;
import corruption.entity.custom.baseEntity.Host;
import corruption.init.ModItems;
import corruption.init.ModSounds;
import corruption.util.combat.AureAttackUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LongArmBeast extends Host implements GeoEntity {

    public static final String registryName="long_arm_beast";

    public LongArmBeast(EntityType<? extends PathfinderMob> entityType, Level level) {
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
                .speed(3f)
                .maxJumpHeight(1)
                .maxJumpWidth(5)
                .avoidDangerousFluids(true)
                .debug(false)
                .build());

        this.targetSelector.addGoal(1, new SmartTargetGoal.Builder(this)
                .range(25)
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
                .damage(12.0f, 22.0f)
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
    public boolean doHurtGoal(LivingEntity target) {
        triggerAnim("attack","attack");
        fullyCustomAttack();
        //target.hurt(target.damageSources().mobAttack(this), 1 + this.getDifficultNumber());
        return true;
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
    //
    private final AnimatableInstanceCache a = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation REST = RawAnimation.begin().thenLoop("idle");
    private final RawAnimation RUN = RawAnimation.begin().thenLoop("walk");
    private final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 10, this::predicate));
        controllers.add(new AnimationController<>(this, "attack", 3, this::attack)
                .triggerableAnim("attack", ATTACK));
    }

    //动画控制器
    private <E extends BaseMob> PlayState predicate(AnimationState<E> state) {
        if (state.isMoving()) {
            return state.setAndContinue(RUN);
        }
        return state.setAndContinue(REST);
    }

    private <E extends BaseMob> PlayState attack(AnimationState<E> state) {
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return a;
    }
}