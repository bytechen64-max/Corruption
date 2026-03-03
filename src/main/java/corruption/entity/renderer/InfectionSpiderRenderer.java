package corruption.entity.renderer;

import corruption.entity.custom.host.InfectionSpider;
import corruption.entity.model.InfectionSpiderModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class InfectionSpiderRenderer extends GeoEntityRenderer<InfectionSpider> {
    public InfectionSpiderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfectionSpiderModel());
    }
}
