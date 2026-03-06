package corruption.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class InstantDeathEffect extends MobEffect {

    public InstantDeathEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }
}