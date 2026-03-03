package corruption.entity.renderer;

import corruption.entity.custom.host.InfectionVillager;
import corruption.entity.model.InfectionVillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class InfectionVillagerRenderer extends GeoEntityRenderer<InfectionVillager> {
    public InfectionVillagerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfectionVillagerModel());
    }
}
