package corruption.entity.renderer;

import corruption.entity.custom.host.InfectionPig;
import corruption.entity.model.InfectionPigModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class InfectionPigRenderer extends GeoEntityRenderer<InfectionPig> {
    public InfectionPigRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfectionPigModel());
    }
}
