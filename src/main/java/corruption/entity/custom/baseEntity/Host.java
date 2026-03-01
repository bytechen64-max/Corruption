package corruption.entity.custom.baseEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class Host extends BaseMob{
    public Host(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setKind("host");
    }
}
