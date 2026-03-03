package corruption.init;

import corruption.CorruptionMod;
import corruption.entity.custom.host.*;
import corruption.entity.custom.refactored.LongArmBeast;
import corruption.item.custom.InfectionHeart;
import corruption.item.custom.InfectionLeather;
import corruption.item.custom.InfectionMeat;
import corruption.item.custom.LongArmBeastTooth;
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
    public static final RegistryObject<Item> INFECTION_VILLAGER_SPAWN_EGG = ITEMS.register(
            InfectionVillager.registryName+"_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.INFECTION_VILLAGER, -1, -1, new Item.Properties()
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
    public static final RegistryObject<Item> INFECTION_SPIDER_SPAWN_EGG = ITEMS.register(
            InfectionSpider.registryName+"_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.INFECTION_SPIDER, -1, -1, new Item.Properties()
                    .stacksTo(64)));
    public static final RegistryObject<Item> LONG_ARM_BEAST_SPAWN_EGG = ITEMS.register(
            LongArmBeast.registryName+"_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LONG_ARM_BEAST, -1, -1, new Item.Properties()
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
    public static final RegistryObject<Item> INFECTION_HEART = ITEMS.register(
            "infection_heart",
            () -> new InfectionHeart(
                    new Item.Properties()
                            .stacksTo(64)
            ));
    public static final RegistryObject<Item> LONG_ARM_BEAST_TOOTH = ITEMS.register(
            "long_arm_beast_tooth",
            () -> new LongArmBeastTooth(
                    new Item.Properties()
                            .stacksTo(64)
            ));
}
