package corruption.util.effect;


import corruption.init.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public class DeadEffectUtil {
    // 默认配置
    public static class EffectConfig {
        public int particleCount = 30;
        public float baseSpeed = 0.5f;
        public float spread = 1.0f;
        public float upwardBias = 0.0f;
        @Nullable public SoundEvent sound = null;
        public float soundVolume = 0.5f;
        public float soundPitch = 1.0f;

        public static EffectConfig defaults() {
            return new EffectConfig();
        }

        public EffectConfig withParticleCount(int count) {
            this.particleCount = count;
            return this;
        }

        public EffectConfig withSpeed(float speed) {
            this.baseSpeed = speed;
            return this;
        }

        public EffectConfig withSpread(float spread) {
            this.spread = spread;
            return this;
        }

        public EffectConfig withUpwardBias(float bias) {
            this.upwardBias = bias;
            return this;
        }

        public EffectConfig withSound(SoundEvent sound, float volume, float pitch) {
            this.sound = sound;
            this.soundVolume = volume;
            this.soundPitch = pitch;
            return this;
        }
    }

    // =============== 核心方法 ===============

    /**
     * 生成粒子效果（通用方法）
     */
    public static void spawnEffect(Level level, LivingEntity entity,
                                   ParticleOptions particleType, EffectConfig config) {
        Vec3 position = getEffectPosition(entity, config.upwardBias > 0);

        // 播放音效
        if (config.sound != null && !level.isClientSide()) {
            level.playSound(null, entity.blockPosition(), config.sound,
                    SoundSource.HOSTILE, config.soundVolume, config.soundPitch);
        }

        if (level.isClientSide()) {
            spawnParticles((ClientLevel) level, position, particleType, config);
        } else {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
            spawnParticlesOnServer(serverLevel, position, particleType, config);
        }
    }

    /**
     * 生成粒子效果（带位置偏移）
     */
    public static void spawnEffectAt(Level level, Vec3 position,
                                     ParticleOptions particleType, EffectConfig config) {
        if (level.isClientSide()) {
            spawnParticles((ClientLevel) level, position, particleType, config);
        } else {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
            spawnParticlesOnServer(serverLevel, position, particleType, config);
        }
    }

    // =============== 便捷方法 ===============

    /**
     * 肉块爆裂效果
     */
    public static void spawnMeatBurst(Level level, LivingEntity entity) {
        spawnEffect(level, entity, ModParticles.MEAT_PARTICLE.get(),
                EffectConfig.defaults()
                        .withParticleCount(40)
                        .withSpeed(0.6f)
                        .withSpread(1.2f)
                        .withSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 0.8f));
    }

    /**
     * 大量肉块爆裂效果
     */
    public static void spawnMeatExplosion(Level level, LivingEntity entity) {
        spawnEffect(level, entity, ModParticles.MEAT_PARTICLE.get(),
                EffectConfig.defaults()
                        .withParticleCount(80)
                        .withSpeed(0.8f)
                        .withSpread(2.0f)
                        .withUpwardBias(0.3f)
                        .withSound(SoundEvents.GENERIC_EXPLODE, 0.5f, 1.2f));
    }

    /**
     * 血液喷溅效果
     */
    public static void spawnBloodSpray(Level level, LivingEntity entity) {
        spawnEffect(level, entity, ModParticles.BLOODY_PARTICLE.get(),
                EffectConfig.defaults()
                        .withParticleCount(25)
                        .withSpeed(0.4f)
                        .withSpread(0.8f)
                        .withSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.4f, 1.5f));
    }

    /**
     * 大量血液喷溅效果
     */
    public static void spawnBloodFountain(Level level, LivingEntity entity) {
        spawnEffect(level, entity, ModParticles.BLOODY_PARTICLE.get(),
                EffectConfig.defaults()
                        .withParticleCount(50)
                        .withSpeed(0.7f)
                        .withSpread(1.5f)
                        .withUpwardBias(0.7f)
                        .withSound(SoundEvents.BUCKET_FILL, 0.6f, 0.6f));
    }

    /**
     * 方向性喷溅效果 - 修复版本
     */
    public static void spawnDirectionalSpray(Level level, LivingEntity entity,
                                             Vec3 direction, float intensity) {
        Vec3 position = getEffectPosition(entity, false);

        // 确保方向向量不为零且已归一化
        if (direction.lengthSqr() < 0.001) {
            direction = new Vec3(0, 1, 0); // 默认向上
        } else {
            direction = direction.normalize();
        }

        EffectConfig config = EffectConfig.defaults()
                .withParticleCount((int)(20 * intensity))
                .withSpeed(0.3f * intensity)
                .withSpread(0.3f);

        if (level.isClientSide()) {
            spawnDirectionalParticles((ClientLevel) level, position,
                    ModParticles.BLOODY_PARTICLE.get(),
                    direction, config);
        } else {
            // 服务器端实现
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
            spawnDirectionalParticlesOnServer(serverLevel, position,
                    ModParticles.BLOODY_PARTICLE.get(),
                    direction, config);
        }
    }

    // =============== 工具方法 ===============

    private static Vec3 getEffectPosition(LivingEntity entity, boolean upwardBias) {
        double x = entity.getX();
        double y = entity.getY() + (upwardBias ? entity.getBbHeight() * 0.3 : entity.getBbHeight() * 0.5);
        double z = entity.getZ();
        return new Vec3(x, y, z);
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnParticles(ClientLevel level, Vec3 position,
                                       ParticleOptions particleType, EffectConfig config) {
        RandomSource random = level.random;

        for (int i = 0; i < config.particleCount; i++) {
            // 计算带有向上偏置的方向
            double dirX = (random.nextDouble() - 0.5) * 2.0 * (1 - config.upwardBias);
            double dirY = random.nextDouble() * config.upwardBias + (1 - config.upwardBias) * 0.5;
            double dirZ = (random.nextDouble() - 0.5) * 2.0 * (1 - config.upwardBias);

            Vec3 direction = new Vec3(dirX, dirY, dirZ).normalize();

            // 速度变化
            double speedVariation = config.baseSpeed * (0.7 + random.nextDouble() * 0.6);
            double particleSpeedX = direction.x * speedVariation;
            double particleSpeedY = direction.y * speedVariation;
            double particleSpeedZ = direction.z * speedVariation;

            // 位置变化
            double particleX = position.x + (random.nextDouble() - 0.5) * config.spread;
            double particleY = position.y + (random.nextDouble() - 0.5) * config.spread * 0.5;
            double particleZ = position.z + (random.nextDouble() - 0.5) * config.spread;

            // 添加随机扰动
            particleSpeedX += (random.nextDouble() - 0.5) * 0.1;
            particleSpeedZ += (random.nextDouble() - 0.5) * 0.1;

            level.addParticle(particleType,
                    particleX, particleY, particleZ,
                    particleSpeedX, particleSpeedY, particleSpeedZ);
        }
    }

    private static void spawnParticlesOnServer(net.minecraft.server.level.ServerLevel level, Vec3 position,
                                               ParticleOptions particleType, EffectConfig config) {
        level.sendParticles(
                particleType,
                position.x, position.y, position.z,
                config.particleCount,
                config.spread, config.spread * 0.5, config.spread,
                config.baseSpeed
        );
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnDirectionalParticles(ClientLevel level, Vec3 position,
                                                  ParticleOptions particleType,
                                                  Vec3 direction, EffectConfig config) {
        RandomSource random = level.random;

        for (int i = 0; i < config.particleCount; i++) {
            // 在方向周围添加随机散布
            double spreadX = (random.nextDouble() - 0.5) * 0.3;
            double spreadY = (random.nextDouble() - 0.5) * 0.3;
            double spreadZ = (random.nextDouble() - 0.5) * 0.3;

            Vec3 particleDir = new Vec3(
                    direction.x + spreadX,
                    direction.y + spreadY,
                    direction.z + spreadZ
            ).normalize();

            // 速度变化
            double speed = config.baseSpeed * (0.8 + random.nextDouble() * 0.4);

            // 位置（在方向上有偏移）
            double offset = random.nextDouble() * 0.2;
            double particleX = position.x + particleDir.x * offset;
            double particleY = position.y + particleDir.y * offset;
            double particleZ = position.z + particleDir.z * offset;

            level.addParticle(particleType,
                    particleX, particleY, particleZ,
                    particleDir.x * speed, particleDir.y * speed, particleDir.z * speed);
        }
    }

    private static void spawnDirectionalParticlesOnServer(net.minecraft.server.level.ServerLevel level, Vec3 position,
                                                          ParticleOptions particleType,
                                                          Vec3 direction, EffectConfig config) {
        // 服务器端发送简化版本 - 使用正确的参数顺序
        level.sendParticles(
                particleType,
                position.x, position.y, position.z,
                config.particleCount,
                config.spread, config.spread * 0.5, config.spread,
                config.baseSpeed
        );
    }


    /**
     * 客户端生成方向性粒子
     */
    @OnlyIn(Dist.CLIENT)
    private static void spawnDirectionalParticlesClient(Level level, LivingEntity entity,
                                                        Vec3 direction, float intensity) {
        if (!(level instanceof ClientLevel clientLevel)) return;

        Vec3 position = getEffectPosition(entity, false);
        RandomSource random = clientLevel.random;

        // 确保方向向量不为零且已归一化
        if (direction.lengthSqr() < 0.001) {
            direction = new Vec3(0, 1, 0);
        } else {
            direction = direction.normalize();
        }

        int particleCount = (int)(15 * intensity);
        float baseSpeed = 0.3f * intensity;

        for (int i = 0; i < particleCount; i++) {
            // 在方向周围添加随机散布
            double spreadX = (random.nextDouble() - 0.5) * 0.5;
            double spreadY = (random.nextDouble() - 0.5) * 0.3;
            double spreadZ = (random.nextDouble() - 0.5) * 0.5;

            Vec3 particleDir = new Vec3(
                    direction.x + spreadX,
                    Math.max(0.1, direction.y + spreadY), // 确保Y轴至少有点向上
                    direction.z + spreadZ
            ).normalize();

            // 速度变化
            double speed = baseSpeed * (0.7 + random.nextDouble() * 0.6);

            // 位置（从实体中心出发）
            double offset = random.nextDouble() * 0.3;
            double particleX = position.x + particleDir.x * offset;
            double particleY = position.y + particleDir.y * offset + 0.1; // 稍微抬高一点
            double particleZ = position.z + particleDir.z * offset;

            // 添加粒子
            clientLevel.addParticle(
                    ModParticles.BLOODY_PARTICLE.get(),
                    particleX, particleY, particleZ,
                    particleDir.x * speed,
                    particleDir.y * speed * 0.5, // Y轴速度减半，避免飞太高
                    particleDir.z * speed
            );
        }
    }

    /**
     * 服务器端：向所有客户端发送粒子效果
     */
    private static void spawnDirectionalSprayForAllClients(Level level, LivingEntity entity,
                                                           Vec3 direction, float intensity) {
        if (level.isClientSide()) return;

        // 使用网络包发送给所有客户端
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;

        // 创建一个简单的位置和方向数据包
        // 注意：这里需要实际的网络包实现，我们先简化处理
        // 临时方案：直接调用客户端方法（仅用于测试）
        // 实际应该使用网络包：PacketDistributor.TRACKING_ENTITY.with(() -> entity)

        // 播放音效
        if (!level.isClientSide()) {
            level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                    SoundSource.HOSTILE, 0.3f, 1.5f);
        }
    }
    // =============== 调试方法 ===============

    /**
     * 测试方向性喷溅 - 使用固定方向
     */
    public static void testDirectionalSpray(Level level, LivingEntity entity) {
        // 测试向上喷溅
        spawnDirectionalSpray(level, entity, new Vec3(0, 1, 0), 1.0f);
    }

    /**
     * 测试从前面喷溅
     */
    public static void testForwardSpray(Level level, LivingEntity entity) {
        // 使用实体的前方方向
        Vec3 forward = entity.getLookAngle();
        spawnDirectionalSpray(level, entity, forward, 1.0f);
    }
}