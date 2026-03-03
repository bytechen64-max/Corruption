package corruption.init;

import corruption.CorruptionMod;
import corruption.entity.custom.host.*;
import corruption.entity.custom.refactored.LongArmBeast;
import corruption.entity.renderer.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CorruptionMod.MODID);

    public static final RegistryObject<EntityType<InfectionZombie>> INFECTION_ZOMBIE =
            ENTITIES.register(InfectionZombie.registryName,
                    () -> EntityType.Builder.of(InfectionZombie::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.8f)
                            .build(InfectionZombie.registryName));
    public static final RegistryObject<EntityType<InfectionVillager>> INFECTION_VILLAGER =
            ENTITIES.register(InfectionVillager.registryName,
                    () -> EntityType.Builder.of(InfectionVillager::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.8f)
                            .build(InfectionVillager.registryName));
    public static final RegistryObject<EntityType<InfectionSheep>> INFECTION_SHEEP =
            ENTITIES.register(InfectionSheep.registryName,
                    () -> EntityType.Builder.of(InfectionSheep::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.2f)
                            .build(InfectionSheep.registryName));
    public static final RegistryObject<EntityType<InfectionPig>> INFECTION_PIG =
            ENTITIES.register(InfectionPig.registryName,
                    () -> EntityType.Builder.of(InfectionPig::new, MobCategory.MONSTER)
                            .sized(0.8f, 1f)
                            .build(InfectionPig.registryName));
    public static final RegistryObject<EntityType<InfectionCow>> INFECTION_COW =
            ENTITIES.register(InfectionCow.registryName,
                    () -> EntityType.Builder.of(InfectionCow::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.3f)
                            .build(InfectionCow.registryName));
    public static final RegistryObject<EntityType<InfectionSpider>> INFECTION_SPIDER =
            ENTITIES.register(InfectionSpider.registryName,
                    () -> EntityType.Builder.of(InfectionSpider::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.2f)
                            .build(InfectionSpider.registryName));
    public static final RegistryObject<EntityType<LongArmBeast>> LONG_ARM_BEAST =
            ENTITIES.register(LongArmBeast.registryName,
                    () -> EntityType.Builder.of(LongArmBeast::new, MobCategory.MONSTER)
                            .sized(1.3f, 3.5f)
                            .build(LongArmBeast.registryName));

    public static void registerAttributesUnit(EntityAttributeCreationEvent event) {
        event.put(ModEntities.INFECTION_ZOMBIE.get(), InfectionZombie.createAttributes().build());
        event.put(ModEntities.INFECTION_VILLAGER.get(), InfectionVillager.createAttributes().build());
        event.put(ModEntities.INFECTION_SHEEP.get(), InfectionSheep.createAttributes().build());
        event.put(ModEntities.INFECTION_PIG.get(), InfectionPig.createAttributes().build());
        event.put(ModEntities.INFECTION_COW.get(), InfectionCow.createAttributes().build());
        event.put(ModEntities.INFECTION_SPIDER.get(), InfectionSpider.createAttributes().build());
        event.put(ModEntities.LONG_ARM_BEAST.get(), LongArmBeast.createAttributes().build());
    }
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.INFECTION_ZOMBIE.get(), InfectionZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.INFECTION_VILLAGER.get(), InfectionVillagerRenderer::new);
        event.registerEntityRenderer(ModEntities.INFECTION_SHEEP.get(), InfectionSheepRenderer::new);
        event.registerEntityRenderer(ModEntities.INFECTION_PIG.get(), InfectionPigRenderer::new);
        event.registerEntityRenderer(ModEntities.INFECTION_COW.get(), InfectionCowRenderer::new);
        event.registerEntityRenderer(ModEntities.INFECTION_SPIDER.get(), InfectionSpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.LONG_ARM_BEAST.get(), LongArmBeastRenderer::new);

    }
}
