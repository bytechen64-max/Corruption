package corruption.entity.renderer;

import corruption.entity.custom.host.InfectionCow;
import corruption.entity.model.InfectionCowModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class InfectionCowRenderer extends GeoEntityRenderer<InfectionCow> {
    public InfectionCowRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfectionCowModel());
    }
}
