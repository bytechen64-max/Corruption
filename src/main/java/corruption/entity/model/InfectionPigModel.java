package corruption.entity.model;

import corruption.CorruptionMod;
import corruption.entity.custom.host.InfectionPig;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class InfectionPigModel extends GeoModel<InfectionPig> {
    private static String registerName=InfectionPig.registryName;
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
    public ResourceLocation getModelResource(InfectionPig animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(InfectionPig animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(InfectionPig animatable) {
        return ANIMATION;
    }
}