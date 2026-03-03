package corruption.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class Bloody extends TextureSheetParticle {
    // 配置参数
    private static final int DEFAULT_LIFETIME = 50;
    private static final float DEFAULT_GRAVITY = 1.0F;
    private static final float DEFAULT_SIZE = 0.4F;
    private static final float ROLL_SPEED = 0.1F;
    private static final float SIZE_DECAY = 0.98F;
    private static final float GROUND_FRICTION = 0.7F;

    private final SpriteSet sprites;
    private boolean isOnGround = false;
    private float baseSize;
    private int groundTime = 0;

    protected Bloody(ClientLevel level, double x, double y, double z,
                     double xSpeed, double ySpeed, double zSpeed,
                     SpriteSet sprites, float size, int lifetime, float gravity) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;
        this.baseSize = size;

        this.setSize(size, size);
        this.lifetime = lifetime;
        this.gravity = gravity;
        this.hasPhysics = true;

        // 设置初始精灵
        if (this.sprites != null) {
            this.setSpriteFromAge(this.sprites);
        }

        // 随机初始旋转
        this.roll = (float) (random.nextDouble() * Math.PI * 2);
        this.oRoll = this.roll;

        // 随机颜色偏移（模拟血液颜色变化）
        float colorVar = 0.8F + random.nextFloat() * 0.2F;
        this.rCol *= colorVar;
        this.gCol *= colorVar * 0.8F; // 绿色通道减少，更红
        this.bCol *= colorVar * 0.6F; // 蓝色通道更少
    }

    public Bloody(ClientLevel level, double x, double y, double z,
                  double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        this(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites,
                DEFAULT_SIZE, DEFAULT_LIFETIME, DEFAULT_GRAVITY);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // 应用重力
        this.yd -= 0.04D * (double) this.gravity;
        this.move(this.xd, this.yd, this.zd);

        // 地面检测和处理
        if (this.onGround && !this.isOnGround) {
            this.isOnGround = true;
            this.onHitGround();
        }

        // 旋转处理
        this.oRoll = this.roll;
        if (!this.isOnGround) {
            this.roll += ROLL_SPEED * (1.0F + this.age * 0.02F); // 随时间加速旋转
        }

        // 大小衰减
        this.quadSize = this.baseSize * (float) Math.pow(SIZE_DECAY, this.age);

        // 更新精灵
        if (this.sprites != null) {
            this.setSpriteFromAge(this.sprites);
        }

        // 地面摩擦力
        if (this.onGround) {
            this.xd *= GROUND_FRICTION;
            this.zd *= GROUND_FRICTION;
            this.groundTime++;

            // 地面上的颜色变化
            if (groundTime > 10) {
                float fade = 1.0F - (groundTime - 10) / 30.0F;
                this.alpha = Mth.clamp(fade, 0.0F, 1.0F);
            }
        }

        // 速度衰减（空气阻力）
        this.xd *= 0.98D;
        this.yd *= 0.98D;
        this.zd *= 0.98D;
    }

    protected void onHitGround() {
        // 落地时的小反弹
        if (Math.abs(this.yd) > 0.1) {
            this.yd = -this.yd * 0.3;
        }
    }

    // =============== 构建器模式 ===============
    public static class Builder {
        private float size = DEFAULT_SIZE;
        private int lifetime = DEFAULT_LIFETIME;
        private float gravity = DEFAULT_GRAVITY;
        private float scaleOverTime = 1.0F;
        private boolean hasBounce = true;

        public Builder size(float size) {
            this.size = size;
            return this;
        }

        public Builder lifetime(int lifetime) {
            this.lifetime = lifetime;
            return this;
        }

        public Builder gravity(float gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder scaleOverTime(float scale) {
            this.scaleOverTime = scale;
            return this;
        }

        public Builder bounce(boolean bounce) {
            this.hasBounce = bounce;
            return this;
        }

        public Bloody create(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed,
                             SpriteSet sprites) {
            Bloody particle = new Bloody(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    sprites, size, lifetime, gravity);
            // 应用额外配置
            return particle;
        }
    }

    // =============== 提供者 ===============
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        @Nullable private final ParticleConfig config;

        public Provider(SpriteSet sprites) {
            this(sprites, null);
        }

        public Provider(SpriteSet sprites, @Nullable ParticleConfig config) {
            this.sprites = sprites;
            this.config = config;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            if (this.sprites == null) {
                return null;
            }

            Bloody particle;
            if (config != null) {
                particle = config.configureBloody(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
            } else {
                particle = new Bloody(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
            }

            // 随机速度变化
            if (level.random.nextFloat() < 0.3F) {
                particle.xd += (level.random.nextDouble() - 0.5) * 0.05;
                particle.yd += (level.random.nextDouble() - 0.5) * 0.05;
                particle.zd += (level.random.nextDouble() - 0.5) * 0.05;
            }

            return particle;
        }
    }

    // =============== 配置类 ===============
    public static class ParticleConfig {
        private final float size;
        private final int lifetime;
        private final float gravity;

        public ParticleConfig(float size, int lifetime, float gravity) {
            this.size = size;
            this.lifetime = lifetime;
            this.gravity = gravity;
        }

        public Bloody configureBloody(ClientLevel level, double x, double y, double z,
                                      double xSpeed, double ySpeed, double zSpeed,
                                      SpriteSet sprites) {
            return new Bloody(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    sprites, size, lifetime, gravity);
        }

        public static ParticleConfig create() {
            return new ParticleConfig(DEFAULT_SIZE, DEFAULT_LIFETIME, DEFAULT_GRAVITY);
        }

        public static ParticleConfig small() {
            return new ParticleConfig(0.2F, 40, 1.2F);
        }

        public static ParticleConfig heavy() {
            return new ParticleConfig(0.6F, 60, 1.5F);
        }

        public static ParticleConfig spray() {
            return new ParticleConfig(0.3F, 30, 0.8F);
        }
    }
}