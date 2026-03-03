package corruption.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class Meat extends TextureSheetParticle {
    // 配置参数
    private static final int DEFAULT_LIFETIME = 30;
    private static final float DEFAULT_GRAVITY = 1.0F;
    private static final float DEFAULT_SIZE = 1F;
    private static final float ROLL_SPEED = 0.15F;
    private static final float SIZE_DECAY = 0.98F;
    private static final float GROUND_FRICTION = 0.6F;

    private final SpriteSet sprites;
    private boolean isOnGround = false;
    private float baseSize;
    private float bounceFactor = 0.3F;
    private int bounceCount = 0;
    private final int maxBounces = 3;

    protected Meat(ClientLevel level, double x, double y, double z,
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

        // 随机肉块颜色变化
        float colorVar = 0.7F + random.nextFloat() * 0.3F;
        this.rCol = Mth.clamp(0.8F * colorVar, 0.6F, 0.9F);
        this.gCol = Mth.clamp(0.4F * colorVar, 0.3F, 0.5F);
        this.bCol = Mth.clamp(0.2F * colorVar, 0.1F, 0.3F);
    }

    public Meat(ClientLevel level, double x, double y, double z,
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
            // 空中旋转速度与速度相关
            float speed = (float) Math.sqrt(xd * xd + yd * yd + zd * zd);
            this.roll += ROLL_SPEED * speed;
        } else {
            // 地面上缓慢旋转
            this.roll += ROLL_SPEED * 0.1F;
        }

        // 大小衰减
        float ageFactor = 1.0F - (float) age / lifetime;
        this.quadSize = this.baseSize * ageFactor;

        // 更新精灵
        if (this.sprites != null) {
            this.setSpriteFromAge(this.sprites);
        }

        // 地面摩擦力
        if (this.onGround) {
            this.xd *= GROUND_FRICTION;
            this.zd *= GROUND_FRICTION;

            // 地面上的缓慢下沉效果
            if (this.bounceCount >= maxBounces) {
                this.yd = 0;
                this.y += 0.001; // 轻微下沉
            }
        }

        // 空气阻力
        this.xd *= 0.99D;
        this.zd *= 0.99D;
    }

    protected void onHitGround() {
        if (bounceCount < maxBounces && Math.abs(this.yd) > 0.05) {
            // 反弹效果
            this.yd = -this.yd * bounceFactor;
            this.bounceCount++;

            // 每次反弹减弱
            bounceFactor *= 0.7F;

            // 反弹时的小水平偏移
            if (random.nextFloat() < 0.5F) {
                this.xd += (random.nextDouble() - 0.5) * 0.05;
                this.zd += (random.nextDouble() - 0.5) * 0.05;
            }
        } else {
            this.yd = 0;
        }
    }

    // =============== 构建器模式 ===============
    public static class Builder {
        private float size = DEFAULT_SIZE;
        private int lifetime = DEFAULT_LIFETIME;
        private float gravity = DEFAULT_GRAVITY;
        private float bounceFactor = 0.3F;
        private int maxBounces = 3;

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

        public Builder bounce(float bounceFactor, int maxBounces) {
            this.bounceFactor = bounceFactor;
            this.maxBounces = maxBounces;
            return this;
        }

        public Meat create(ClientLevel level, double x, double y, double z,
                           double xSpeed, double ySpeed, double zSpeed,
                           SpriteSet sprites) {
            Meat particle = new Meat(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    sprites, size, lifetime, gravity);
            // 应用反弹配置
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

            Meat particle;
            if (config != null) {
                particle = config.configureMeat(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
            } else {
                particle = new Meat(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
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

        public Meat configureMeat(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  SpriteSet sprites) {
            return new Meat(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    sprites, size, lifetime, gravity);
        }

        public static ParticleConfig create() {
            return new ParticleConfig(DEFAULT_SIZE, DEFAULT_LIFETIME, DEFAULT_GRAVITY);
        }

        public static ParticleConfig chunk() {
            return new ParticleConfig(0.15F, 40, 1.2F);
        }

        public static ParticleConfig piece() {
            return new ParticleConfig(0.08F, 25, 0.9F);
        }

        public static ParticleConfig bigChunk() {
            return new ParticleConfig(0.2F, 50, 1.5F);
        }
    }
}