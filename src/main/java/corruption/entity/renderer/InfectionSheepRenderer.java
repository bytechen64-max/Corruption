package corruption.entity.renderer;

import corruption.entity.custom.host.InfectionSheep;
import corruption.entity.model.InfectionSheepModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class InfectionSheepRenderer extends GeoEntityRenderer<InfectionSheep> {
    public InfectionSheepRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfectionSheepModel());
    }
}
