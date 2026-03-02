package corruption.entity.event;

import corruption.CorruptionMod;
import corruption.entity.custom.baseEntity.BaseMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = CorruptionMod.MODID)

public class OnEntitySpawn {
    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if(entity instanceof BaseMob baseMob)
        {
            AttributeInstance maxHealthAttr = baseMob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(baseMob.getHealth()+ baseMob.getDifficultNumber());
            }
        }
    }
}
