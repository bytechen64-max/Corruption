package corruption.entity.custom.host;

import corruption.entity.ai.SmartMoveGoal;
import corruption.entity.ai.SmartTargetGoal;
import corruption.entity.custom.baseEntity.Host;
import corruption.init.ModEffects;
import corruption.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.level.block.state.BlockState;

public class InfectionCow extends Host {
    public static final String registryName="infection_cow";
    public InfectionCow(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setDoHurtTime(20);
        this.setDoHurtDistance(2);
        this.setDieRandom(40);
        this.setDieTime(31);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.FOLLOW_RANGE, 1.0D)
                .add(Attributes.ARMOR, 2.0);
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4D, false));
        this.goalSelector.addGoal(2, new SmartMoveGoal.Builder(this)
                .speed(2.4f)
                .maxJumpHeight(1)      // 可跳跃高度
                .maxJumpWidth(2)       // 可跨越宽度
                .avoidDangerousFluids(true) // 避开岩浆
                .debug(false)           // 开启粒子调试
                .build());

        this.targetSelector.addGoal(1, new SmartTargetGoal.Builder(this)
                .range(16)
                .throughWall(false)
                .attackOtherBaseMobs(false)
                .build());
    }
    @Override
    public boolean doHurtGoal(LivingEntity target) {
        target.hurt(target.damageSources().mobAttack(this), 0+this.getDifficultNumber());
        if(this.randomPercentage(50))
        {
            target.addEffect(new MobEffectInstance(ModEffects.CORRUPTION.get(), 100, 1));
        }
        return true;
    }

    // 声音部分（原版牛的音效）
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.COW_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(SoundEvents.COW_STEP, 0.15F, 1.0F);
    }
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);
        this.spawnAtLocation(new ItemStack(ModItems.INFECTION_LEATHER.get()));
        this.spawnAtLocation(new ItemStack(ModItems.INFECTION_MEAT.get()));
    }
}