package corruption.config;

import corruption.world.data.DifficultyLevel;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class BasicWorldConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 全局腐化转换时间（所有 TICK 触发的实体共用）
    public static final ForgeConfigSpec.IntValue GLOBAL_CONVERSION_TICKS;

    // 难度倍率值（与之前相同）
    public static final ForgeConfigSpec.IntValue DIFFICULTY_EASY_VALUE;
    public static final ForgeConfigSpec.IntValue DIFFICULTY_NORMAL_VALUE;
    public static final ForgeConfigSpec.IntValue DIFFICULTY_HARD_VALUE;
    public static final ForgeConfigSpec.IntValue DIFFICULTY_IMPOSSIBLE_VALUE;

    static {
        // ── 转换时间（全局）─────────────────────────────────────────────────
        BUILDER.push("entity_conversion");
        BUILDER.comment("实体腐化转换相关配置（单位：tick，20 tick = 1 秒，默认 1200 = 1 分钟）");

        GLOBAL_CONVERSION_TICKS = BUILDER
                .comment("腐化的转换时间（tick），所有 TICK 类型转换共用此值")
                .defineInRange("conversion_ticks", 20 * 60, 1, Integer.MAX_VALUE);

        BUILDER.pop();

        // ── 难度倍率值 ────────────────────────────────────────────────────────
        BUILDER.push("difficulty");
        BUILDER.comment("各难度等级对应的数值倍率（影响伤害、刷怪速率等逻辑）");

        DIFFICULTY_EASY_VALUE = BUILDER
                .comment("EASY 难度的倍率值（默认 " + DifficultyLevel.EASY.getValue() + "）")
                .defineInRange("easy_value", DifficultyLevel.EASY.getValue(), 1, 1024);

        DIFFICULTY_NORMAL_VALUE = BUILDER
                .comment("NORMAL 难度的倍率值（默认 " + DifficultyLevel.NORMAL.getValue() + "）")
                .defineInRange("normal_value", DifficultyLevel.NORMAL.getValue(), 1, 1024);

        DIFFICULTY_HARD_VALUE = BUILDER
                .comment("HARD 难度的倍率值（默认 " + DifficultyLevel.HARD.getValue() + "）")
                .defineInRange("hard_value", DifficultyLevel.HARD.getValue(), 1, 1024);

        DIFFICULTY_IMPOSSIBLE_VALUE = BUILDER
                .comment("IMPOSSIBLE 难度的倍率值（默认 " + DifficultyLevel.IMPOSSIBLE.getValue() + "）")
                .defineInRange("impossible_value", DifficultyLevel.IMPOSSIBLE.getValue(), 1, 1024);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "corruption-world.toml");
    }
}