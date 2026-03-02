package corruption.entity.model;

import corruption.CorruptionMod;
import corruption.entity.custom.host.InfectionSheep;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class InfectionSheepModel extends GeoModel<InfectionSheep> {
    private static String registerName=InfectionSheep.registryName;
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
    public ResourceLocation getModelResource(InfectionSheep animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(InfectionSheep animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(InfectionSheep animatable) {
        return ANIMATION;
    }
}