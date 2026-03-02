package corruption.config;

import corruption.CorruptionMod;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = CorruptionMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityExchangeRuleConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;

    // 规则列表，每个条目格式：source=target[:trigger]
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CONVERSION_RULES;

    // 触发类型枚举
    public enum TriggerType {
        TICK,   // 基于时间（原有逻辑）
        EFFECT  // 基于腐化效果到期
    }

    // 规则信息类
    public static class RuleInfo {
        public final Supplier<EntityType<?>> target;
        public final TriggerType trigger;

        public RuleInfo(Supplier<EntityType<?>> target, TriggerType trigger) {
            this.target = target;
            this.trigger = trigger;
        }
    }

    static {
        BUILDER.push("entity_conversion_rules");
        CONVERSION_RULES = BUILDER
                .comment("实体转换规则列表，每个条目的格式为 '源实体注册名=目标实体注册名[:触发类型]'",
                        "触发类型可选：tick（基于存活时间）、effect（基于腐化效果）",
                        "例如: 'minecraft:zombie=corruption:infection_zombie' 或 'minecraft:skeleton=corruption:infection_skeleton:effect'")
                .defineList("rules",
                        List.of("minecraft:zombie=corruption:infection_zombie","minecraft:sheep=corruption:infection_sheep:effect"),
                        o -> o instanceof String && ((String) o).contains("="));
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    // 缓存：源实体类型 -> 规则信息
    private static Map<EntityType<?>, RuleInfo> cachedRules = new HashMap<>();

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "corruption-exchange-rules.toml");
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            refreshCache();
        }
    }

    private static void refreshCache() {
        Map<EntityType<?>, RuleInfo> newMap = new HashMap<>();
        List<? extends String> rules = CONVERSION_RULES.get();
        for (String rule : rules) {
            // 拆分源和目标部分
            String[] mainParts = rule.split("=", 2);
            if (mainParts.length != 2) continue;
            String sourceKey = mainParts[0].trim();
            String remainder = mainParts[1].trim();

            // 解析剩余部分：可能包含触发类型
            String targetKey;
            TriggerType trigger = TriggerType.TICK; // 默认
            if (remainder.contains(":")) {
                String[] subParts = remainder.split(":", 2);
                targetKey = subParts[0].trim();
                try {
                    trigger = TriggerType.valueOf(subParts[1].trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            } else {
                targetKey = remainder;
            }

            try {
                ResourceLocation sourceId = new ResourceLocation(sourceKey);
                ResourceLocation targetId = new ResourceLocation(targetKey);
                EntityType<?> sourceType = ForgeRegistries.ENTITY_TYPES.getValue(sourceId);
                EntityType<?> targetType = ForgeRegistries.ENTITY_TYPES.getValue(targetId);
                if (sourceType != null && targetType != null) {
                    newMap.put(sourceType, new RuleInfo(() -> targetType, trigger));
                } else {
                    // 可添加日志警告
                }
            } catch (ResourceLocationException ignored) {}
        }
        cachedRules = newMap;
    }

    public static Map<EntityType<?>, RuleInfo> getRules() {
        if (cachedRules.isEmpty() && !CONVERSION_RULES.get().isEmpty()) {
            refreshCache();
        }
        return Collections.unmodifiableMap(cachedRules);
    }
}