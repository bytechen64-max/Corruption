package corruption.entity.custom.host;

import corruption.entity.ai.SmartTargetGoal;
import corruption.entity.custom.baseEntity.Host;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public class InfectionZombie extends Host {
    public static final String registryName="infection_zombie";
    public InfectionZombie(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setDoHurtTime(20);
        this.setDoHurtDistance(2);
        this.setDieRandom(0);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.FOLLOW_RANGE, 1.0D)
                .add(Attributes.ARMOR, 2.0);
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4D, false));
        this.goalSelector.addGoal(2,
                new corruption.entity.custom.baseEntity.goal.SmartMoveGoal.Builder(this)
                        .speed(2.3f)
                        .maxNodes(512)
                        .targetUpdateInterval(5)
                        .targetMoveThreshold(2.0f)
                        .partialKeepFromEnd(5)
                        .debug(false) // 开发时可改 true
                        .build()
        );

        // 索敌 goal（优先级 1）
        this.targetSelector.addGoal(1,
                new SmartTargetGoal.Builder(this)
                        .range(24.0)
                        .checkInterval(1)
                        .throughWall(false)
                        .build()
        );
  }
}
