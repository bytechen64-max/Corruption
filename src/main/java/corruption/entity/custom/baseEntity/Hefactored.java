package corruption.entity.custom.baseEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class Hefactored extends BaseMob{
    public Hefactored(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setKind("refactored");
    }
}
