package corruption.init;


import corruption.CorruptionMod;
import corruption.particle.custom.Bloody;
import corruption.particle.custom.Meat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CorruptionMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents
{
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {

        event.registerSpriteSet(ModParticles.MEAT_PARTICLE.get(), spriteSet -> {
                Meat.Provider provider = new Meat.Provider(spriteSet);
                return provider;

        });
        event.registerSpriteSet(ModParticles.BLOODY_PARTICLE.get(), spriteSet -> {
            Bloody.Provider provider = new Bloody.Provider(spriteSet);
            return provider;

        });
    }
}
