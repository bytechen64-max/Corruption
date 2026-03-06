package corruption.entity.renderer;

import corruption.entity.custom.refactored.Shocker;
import corruption.entity.model.ShockerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ShockerRenderer extends GeoEntityRenderer<Shocker> {
    public ShockerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ShockerModel());
    }
}
