package corruption.entity.model;

import corruption.CorruptionMod;
import corruption.entity.custom.refactored.LongArmBeast;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class LongArmBeastModel extends GeoModel<LongArmBeast> {
    private static String registerName=LongArmBeast.registryName;
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
    public ResourceLocation getModelResource(LongArmBeast animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(LongArmBeast animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(LongArmBeast animatable) {
        return ANIMATION;
    }
    @Override
    public void setCustomAnimations(LongArmBeast entity, long instanceId,
                                    AnimationState<LongArmBeast> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head == null) return;

        float relativeYaw = Mth.clamp(
                Mth.wrapDegrees(entity.getYHeadRot() - entity.yBodyRot),
                -50f, 50f
        );
        head.setRotY(-relativeYaw * Mth.DEG_TO_RAD);
    }
}