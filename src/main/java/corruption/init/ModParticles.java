package corruption.init;

import corruption.CorruptionMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles
{
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, CorruptionMod.MODID);

    public static final RegistryObject<SimpleParticleType> MEAT_PARTICLE =
            PARTICLE_TYPES.register("meat",
                    () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOODY_PARTICLE =
            PARTICLE_TYPES.register("bloody",
                    () -> new SimpleParticleType(false));
}
