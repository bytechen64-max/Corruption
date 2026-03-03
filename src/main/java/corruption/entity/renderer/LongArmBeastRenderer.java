package corruption.entity.renderer;

import corruption.entity.custom.refactored.LongArmBeast;
import corruption.entity.model.LongArmBeastModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LongArmBeastRenderer extends GeoEntityRenderer<LongArmBeast> {
    public LongArmBeastRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LongArmBeastModel());
    }
}
