package corruption.init;

import corruption.CorruptionMod;
import corruption.entity.custom.host.InfectionZombie;
import corruption.entity.renderer.InfectionZombieRenderer;
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

    public static void registerAttributesUnit(EntityAttributeCreationEvent event) {
        event.put(ModEntities.INFECTION_ZOMBIE.get(), InfectionZombie.createAttributes().build());
    }
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.INFECTION_ZOMBIE.get(), InfectionZombieRenderer::new);

    }
}
