package corruption.init;

import corruption.CorruptionMod;
import corruption.entity.custom.host.InfectionCow;
import corruption.entity.custom.host.InfectionPig;
import corruption.entity.custom.host.InfectionSheep;
import corruption.entity.custom.host.InfectionZombie;
import corruption.item.mod_items.InfectionLeather;
import corruption.item.mod_items.InfectionMeat;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CorruptionMod.MODID);
    public static final RegistryObject<Item> INFECTION_ZOMBIE_SPAWN_EGG = ITEMS.register(
            InfectionZombie.registryName+"_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.INFECTION_ZOMBIE, -1, -1, new Item.Properties()
                    .stacksTo(64)));
    public static final RegistryObject<Item> INFECTION_SHEEP_SPAWN_EGG = ITEMS.register(
            InfectionSheep.registryName+"_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.INFECTION_SHEEP, -1, -1, new Item.Properties()
                    .stacksTo(64)));
    public static final RegistryObject<Item> INFECTION_PIG_SPAWN_EGG = ITEMS.register(
            InfectionPig.registryName+"_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.INFECTION_PIG, -1, -1, new Item.Properties()
                    .stacksTo(64)));
    public static final RegistryObject<Item> INFECTION_COW_SPAWN_EGG = ITEMS.register(
            InfectionCow.registryName+"_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.INFECTION_COW, -1, -1, new Item.Properties()
                    .stacksTo(64)));
    public static final RegistryObject<Item> INFECTION_LEATHER = ITEMS.register(
            "infection_leather",
            () -> new InfectionLeather(
                    new Item.Properties()
                            .stacksTo(64)
            ));
    public static final RegistryObject<Item> INFECTION_MEAT = ITEMS.register(
            "infection_meat",
            () -> new InfectionMeat(
                    new Item.Properties()
                            .stacksTo(64)
            ));
}
