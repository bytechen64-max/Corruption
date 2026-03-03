package corruption.init;

import corruption.CorruptionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CorruptionMod.MODID);

    public static final RegistryObject<SoundEvent> LONG_ARM_BEAST_CALL_1 = registerSoundEvent("long_arm_beast_call_1");
    public static final RegistryObject<SoundEvent> LONG_ARM_BEAST_CALL_2 = registerSoundEvent("long_arm_beast_call_2");



    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CorruptionMod.MODID, name)));
    }
}
