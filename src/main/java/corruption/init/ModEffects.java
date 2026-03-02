package corruption.init;

import corruption.CorruptionMod;
import corruption.effects.CorruptionEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, CorruptionMod.MODID);
    public static final RegistryObject<MobEffect> CORRUPTION=
            EFFECTS.register("corruption",CorruptionEffect::new);
}
