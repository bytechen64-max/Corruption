package corruption.entity.model;

import corruption.CorruptionMod;
import corruption.entity.custom.host.InfectionSpider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class InfectionSpiderModel extends GeoModel<InfectionSpider> {
    private static String registerName=InfectionSpider.registryName;
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
    public ResourceLocation getModelResource(InfectionSpider animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(InfectionSpider animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(InfectionSpider animatable) {
        return ANIMATION;
    }
    @Override
    public void setCustomAnimations(InfectionSpider entity, long instanceId,
                                    AnimationState<InfectionSpider> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head == null) return;

        float relativeYaw = Mth.clamp(
                Mth.wrapDegrees(entity.getYHeadRot() - entity.yBodyRot),
                -50f, 50f
        );
        head.setRotY(-relativeYaw * Mth.DEG_TO_RAD);


        float pitch = Mth.clamp(entity.getXRot(), -30f, 30f);
        head.setRotX(-pitch * Mth.DEG_TO_RAD);
    }
}