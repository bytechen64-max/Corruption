package corruption.entity.renderer;

import corruption.entity.custom.host.InfectionZombie;
import corruption.entity.model.InfectionZombieModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class InfectionZombieRenderer extends GeoEntityRenderer<InfectionZombie> {
    public InfectionZombieRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new InfectionZombieModel());
    }
}
