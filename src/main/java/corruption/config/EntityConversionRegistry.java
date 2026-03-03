package corruption.config;

import corruption.init.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EntityConversionRegistry {

    // TICK 转换信息：包含目标实体提供者 + 时间配置项
    public static class TickConversion {
        public final Supplier<? extends EntityType<?>> target;
        private final ForgeConfigSpec.IntValue ticksConfig;

        public TickConversion(Supplier<? extends EntityType<?>> target, ForgeConfigSpec.IntValue ticksConfig) {
            this.target = target;
            this.ticksConfig = ticksConfig;
        }

        /** 动态获取当前配置的 ticks 值 */
        public int getTicks() {
            return ticksConfig.get();
        }
    }

    // TICK 转换映射：源实体 -> TickConversion（所有实体共用同一个配置项）
    private static final Map<EntityType<?>, TickConversion> TICK_CONVERSIONS = new HashMap<>();

    // EFFECT 转换映射：源实体 -> 目标提供者（无需 ticks）
    private static final Map<EntityType<?>, Supplier<? extends EntityType<?>>> EFFECT_CONVERSIONS = new HashMap<>();

    static {
        // 注册 TICK 转换（全部使用全局配置项 BasicWorldConfig.GLOBAL_CONVERSION_TICKS）
        TICK_CONVERSIONS.put(EntityType.ZOMBIE,
                new TickConversion(() -> ModEntities.INFECTION_ZOMBIE.get(), BasicWorldConfig.GLOBAL_CONVERSION_TICKS));
        // 可继续添加更多 TICK 转换
        // TICK_CONVERSIONS.put(EntityType.SKELETON,
        //         new TickConversion(() -> ModEntities.INFECTION_SKELETON.get(), BasicWorldConfig.GLOBAL_CONVERSION_TICKS));

        // 注册 EFFECT 转换（无 ticks）
        EFFECT_CONVERSIONS.put(EntityType.SHEEP, () -> ModEntities.INFECTION_SHEEP.get());
        EFFECT_CONVERSIONS.put(EntityType.PIG, () -> ModEntities.INFECTION_PIG.get());
        EFFECT_CONVERSIONS.put(EntityType.COW, () -> ModEntities.INFECTION_COW.get());
        EFFECT_CONVERSIONS.put(EntityType.VILLAGER, () -> ModEntities.INFECTION_VILLAGER.get());
        EFFECT_CONVERSIONS.put(EntityType.SPIDER, () -> ModEntities.INFECTION_SPIDER.get());
        // 更多 EFFECT 转换...
    }

    public static Map<EntityType<?>, TickConversion> getTickConversions() {
        return Collections.unmodifiableMap(TICK_CONVERSIONS);
    }

    public static Map<EntityType<?>, Supplier<? extends EntityType<?>>> getEffectConversions() {
        return Collections.unmodifiableMap(EFFECT_CONVERSIONS);
    }

    /** 获取指定实体的 TICK 转换信息（如果存在） */
    public static TickConversion getTickConversion(EntityType<?> source) {
        return TICK_CONVERSIONS.get(source);
    }

    /** 获取指定实体的 EFFECT 转换目标（如果存在） */
    public static Supplier<? extends EntityType<?>> getEffectTarget(EntityType<?> source) {
        return EFFECT_CONVERSIONS.get(source);
    }

    public static boolean hasTickConversion(EntityType<?> source) {
        return TICK_CONVERSIONS.containsKey(source);
    }

    public static boolean hasEffectConversion(EntityType<?> source) {
        return EFFECT_CONVERSIONS.containsKey(source);
    }
}