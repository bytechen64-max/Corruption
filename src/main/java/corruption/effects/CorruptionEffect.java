package corruption.effects;

import corruption.CorruptionMod;
import corruption.config.EntityConversionRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = CorruptionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CorruptionEffect extends MobEffect {

    public CorruptionEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, amplifier));
        if (entity.getHealth() <= 0) {

            EntityType<?> type =entity.getType();
            Supplier<? extends  EntityType<?>> targetSupplier = EntityConversionRegistry.getEffectTarget(type);
            if (targetSupplier == null) return;
            if(entity instanceof Player) return;
            entity.remove(Entity.RemovalReason.KILLED);
            if (entity.getPersistentData().getBoolean("CorruptionConverted")) return;
            entity.getPersistentData().putBoolean("CorruptionConverted", true);
            convertEntity(entity, targetSupplier);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

//    @SubscribeEvent
//    public static void onEffectExpired(MobEffectEvent.Expired event) {
//        MobEffectInstance effectInstance = event.getEffectInstance();
//        LivingEntity entity = event.getEntity();
//        Level level = entity.level();
//        if (level.isClientSide) return;
//
//        if (effectInstance.getEffect() != ModEffects.CORRUPTION.get()) return;
//
//        EntityType<?> type = entity.getType();
//        // 从注册表获取 EFFECT 类型转换目标
//        Supplier<? extends EntityType<?>> targetSupplier = EntityConversionRegistry.getEffectTarget(type);
//        if (targetSupplier == null) return;
//
//        // 防止重复转换
//        if (entity.getPersistentData().getBoolean("CorruptionConverted")) return;
//        entity.getPersistentData().putBoolean("CorruptionConverted", true);
//
//        convertEntity(entity, targetSupplier);
//    }

    private static void convertEntity(LivingEntity oldEntity, Supplier<? extends EntityType<?>> targetSupplier) {
        Level level = oldEntity.level();
        if (level.isClientSide) return;

        EntityType<?> targetType = targetSupplier.get();
        Entity newEntity = targetType.create(level);
        if (newEntity != null) {
            newEntity.moveTo(oldEntity.getX(), oldEntity.getY(), oldEntity.getZ(),
                    oldEntity.getYRot(), oldEntity.getXRot());
            level.addFreshEntity(newEntity);
            oldEntity.discard();
        }
    }
}