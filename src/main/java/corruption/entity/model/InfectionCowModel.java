package corruption.entity.model;

import corruption.CorruptionMod;
import corruption.entity.custom.host.InfectionCow;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class InfectionCowModel extends GeoModel<InfectionCow> {
    private static String registerName=InfectionCow.registryName;
    private static final ResourceLocation MODEL = new ResourceLocation(
            CorruptionMod.MODID, "geo/entity/" + registerName + ".geo.json"
    );
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            CorruptionMod.MODID, "textures/entity/"+registerName+".png"
    );
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            CorruptionMod.MODID, "animations/entity/"+registerName+".animation.json"
    );
    @Override
    public ResourceLocation getModelResource(InfectionCow animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(InfectionCow animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(InfectionCow animatable) {
        return ANIMATION;
    }
}