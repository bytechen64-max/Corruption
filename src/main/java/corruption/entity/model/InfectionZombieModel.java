package corruption.entity.model;

import corruption.CorruptionMod;
import corruption.entity.custom.host.InfectionZombie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class InfectionZombieModel extends GeoModel<InfectionZombie> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            CorruptionMod.MODID, "geo/entity/" + InfectionZombie.registryName + ".geo.json"
    );
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            CorruptionMod.MODID, "textures/entity/"+InfectionZombie.registryName+".png"
    );
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            CorruptionMod.MODID, "animations/entity/"+InfectionZombie.registryName+".animation.json"
    );
    @Override
    public ResourceLocation getModelResource(InfectionZombie animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(InfectionZombie animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(InfectionZombie animatable) {
        return ANIMATION;
    }
}