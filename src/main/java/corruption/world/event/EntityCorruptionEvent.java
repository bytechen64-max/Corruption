package corruption.world.event;

import corruption.CorruptionMod;
import corruption.config.EntityConversionRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = CorruptionMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityCorruptionEvent {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide) return;

        EntityType<?> type = entity.getType();
        // 检查是否有 TICK 转换规则
        if (EntityConversionRegistry.hasTickConversion(type)) {
            CompoundTag data = entity.getPersistentData();
            if (!data.contains("CorruptionSpawnTime")) {
                data.putLong("CorruptionSpawnTime", level.getGameTime());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.contains("CorruptionSpawnTime")) return;

        long spawnTime = data.getLong("CorruptionSpawnTime");
        long currentTime = level.getGameTime();
        EntityType<?> type = entity.getType();

        // 获取 TICK 转换信息
        EntityConversionRegistry.TickConversion conv = EntityConversionRegistry.getTickConversion(type);
        if (conv == null) return; // 没有转换规则

        // 从配置中读取转换所需 ticks（动态获取最新值）
        int requiredTicks = conv.getTicks();

        if (currentTime - spawnTime >= requiredTicks) {
            convertEntity(entity, conv.target);
        }
    }

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