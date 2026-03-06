package corruption.events;

import corruption.CorruptionMod;
import corruption.init.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Random;

@Mod.EventBusSubscriber(modid = CorruptionMod.MODID)
public class CommonEventHandler {

    private static final Random RANDOM = new Random();

    /**
     * 效果结束时：
     * 1. 在服务端生成大量红色尘埃粒子（DustParticleOptions，RGB 1,0,0）
     * 2. 使用魔法伤害造成 100 点伤害
     */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effectInstance = event.getEffectInstance();

        if (effectInstance == null) return;
        if (!effectInstance.getEffect().equals(ModEffects.INSTANT_DEATH.get())) return;

        if (entity.level() instanceof ServerLevel serverLevel) {

            // 红色尘埃粒子（大小 2.0，鲜红色）
            DustParticleOptions redDust = new DustParticleOptions(
                    new Vector3f(1.0f, 0.0f, 0.0f), 2.0f);

            double cx = entity.getX();
            double cy = entity.getY() + entity.getBbHeight() * 0.5;
            double cz = entity.getZ();

            // 60 个粒子从实体中心向外爆散
            for (int i = 0; i < 60; i++) {
                double vx = (RANDOM.nextDouble() - 0.5) * 3.0;
                double vy = (RANDOM.nextDouble()) * 3.0;
                double vz = (RANDOM.nextDouble() - 0.5) * 3.0;
                serverLevel.sendParticles(redDust,
                        cx + vx * 0.3,
                        cy + vy * 0.3,
                        cz + vz * 0.3,
                        1,       // count=1，单独使用速度参数
                        vx * 0.2, vy * 0.2, vz * 0.2,
                        0.05);
            }

            // 额外：沿 Y 轴的环形粒子
            for (int i = 0; i < 30; i++) {
                double angle = (i / 30.0) * Math.PI * 2.0;
                double radius = 0.8 + RANDOM.nextDouble() * 0.5;
                serverLevel.sendParticles(redDust,
                        cx + Math.cos(angle) * radius,
                        cy,
                        cz + Math.sin(angle) * radius,
                        1,
                        0.0, 0.5 + RANDOM.nextDouble(), 0.0,
                        0.1);
            }

            // 造成 100 点魔法伤害（绕过护甲）
            entity.hurt(entity.damageSources().magic(), 100.0f);
        }
    }
}