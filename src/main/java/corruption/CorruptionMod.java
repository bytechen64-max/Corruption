package corruption;

import corruption.init.ModEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import software.bernie.geckolib.GeckoLib;

@Mod("corruption")
public class CorruptionMod {
    public static final String MODID = "corruption";
    public static final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    public CorruptionMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


        GeckoLib.initialize();
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        ModEntities.ENTITIES.register(modEventBus);
        Mixins.addConfiguration("corruption.mixins.json");
        if (MixinEnvironment.getDefaultEnvironment().getObfuscationContext() == null) {
            MixinEnvironment.getDefaultEnvironment().setObfuscationContext("named:intermediary");
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
    private void onRegisterCommands(final RegisterCommandsEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event)
        {
            ModEntities.registerAttributesUnit(event);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            ModEntities.registerRenderers(event);
        }
    }

}