package corruption.entity.custom.baseEntity;

import corruption.init.ModItems;
import corruption.util.effect.DeadEffectUtil;
import corruption.util.effect.RipBodyUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Host extends BaseMob{
    public Host(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setKind("host");
    }
    @Override
    public void onDieAnimEnd() {
        super.onDieAnimEnd();
       // this.playSound(soundEventRegistry.RIP_BODY.get(), 0.8F, 1.0F);
        RipBodyUtil.hostRip(this, 4, 10);
        DeadEffectUtil.spawnMeatExplosion(this.level(),this);
    }

    /**
     * @param damage
     */
    @Override
    public void beHurtForSetHealth(float damage) {
        DeadEffectUtil.spawnBloodSpray(this.level(),this);
        super.beHurtForSetHealth(damage);
    }
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);
        if(this.randomPercentage(50))
        {
            this.spawnAtLocation(new ItemStack(ModItems.INFECTION_MEAT.get()));
            this.spawnAtLocation(new ItemStack(ModItems.INFECTION_HEART.get()));
        }

    }
}
