package corruption.block;

import corruption.CorruptionMod;
import corruption.block.custom.blocks.HostRemainsLarge;
import corruption.block.custom.blocks.HostRemainsMedium;
import corruption.block.custom.blocks.HostRemainsSmall;
import corruption.init.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CorruptionMod.MODID);

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block, Item.Properties itemProperties) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(toReturn.get(), itemProperties));
        return toReturn;
    }

    public static final RegistryObject<Block> HOST_REMAINS_SMALL = registerBlock("host_remains_small",
            () -> new HostRemainsSmall(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)),
            new Item.Properties());
    public static final RegistryObject<Block> HOST_REMAINS_MEDIUM = registerBlock("host_remains_medium",
            () -> new HostRemainsMedium(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)),
            new Item.Properties());
    public static final RegistryObject<Block> HOST_REMAINS_LARGE = registerBlock("host_remains_large",
            () -> new HostRemainsLarge(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)),
            new Item.Properties());
}