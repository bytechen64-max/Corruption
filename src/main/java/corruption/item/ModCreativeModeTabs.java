package corruption.item;

import corruption.CorruptionMod;
import corruption.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATEMOBSTABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CorruptionMod.MODID);
    public static final RegistryObject<CreativeModeTab> TECH = CREATEMOBSTABS.register("corruption_tech",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.INFECTION_ZOMBIE_SPAWN_EGG.get()))
                    .title(Component.translatable("itemGroup."+CorruptionMod.MODID+".corruption_tech"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.INFECTION_ZOMBIE_SPAWN_EGG.get());
                        output.accept(ModItems.INFECTION_SHEEP_SPAWN_EGG.get());
                        output.accept(ModItems.INFECTION_PIG_SPAWN_EGG.get());
                        output.accept(ModItems.INFECTION_COW_SPAWN_EGG.get());
                        output.accept(ModItems.INFECTION_LEATHER.get());
                        output.accept(ModItems.INFECTION_MEAT.get());
                    })
                    .build());
    public static void register(IEventBus eventBus) {
        CREATEMOBSTABS.register(eventBus);
}
}