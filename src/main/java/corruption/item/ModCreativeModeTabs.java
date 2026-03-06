package corruption.item;

import corruption.CorruptionMod;
import corruption.block.ModBlocks;
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
                        output.accept(ModItems.INFECTION_VILLAGER_SPAWN_EGG.get());
                        output.accept(ModItems.INFECTION_SHEEP_SPAWN_EGG.get());
                        output.accept(ModItems.INFECTION_PIG_SPAWN_EGG.get());
                        output.accept(ModItems.INFECTION_COW_SPAWN_EGG.get());
                        output.accept(ModItems.INFECTION_SPIDER_SPAWN_EGG.get());
                        output.accept(ModItems.LONG_ARM_BEAST_SPAWN_EGG.get());
                        output.accept(ModItems.SHOCKER_SPAWN_EGG.get());
                        output.accept(ModItems.INFECTION_LEATHER.get());
                        output.accept(ModItems.INFECTION_MEAT.get());
                        output.accept(ModItems.INFECTION_HEART.get());
                        output.accept(ModItems.LONG_ARM_BEAST_TOOTH.get());
                        output.accept(ModBlocks.HOST_REMAINS_SMALL.get());
                        output.accept(ModBlocks.HOST_REMAINS_MEDIUM.get());
                        output.accept(ModBlocks.HOST_REMAINS_LARGE.get());
                        output.accept(ModBlocks.INFECTION_COAL_ORE.get());
                        output.accept(ModBlocks.INFECTION_COPPER_ORE.get());
                        output.accept(ModBlocks.INFECTION_IRON_ORE.get());
                        output.accept(ModBlocks.INFECTION_REDSTONE_ORE.get());
                        output.accept(ModBlocks.INFECTION_EMERALD_ORE.get());
                        output.accept(ModBlocks.INFECTION_LAPIS_ORE.get());
                        output.accept(ModBlocks.INFECTION_GOLD_ORE.get());
                        output.accept(ModBlocks.INFECTION_DIAMOND_ORE.get());
                        output.accept(ModItems.VICE_TEMPLATE.get());
                    })
                    .build());
    public static void register(IEventBus eventBus) {
        CREATEMOBSTABS.register(eventBus);
}
}