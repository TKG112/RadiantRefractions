package net.tkg.radiantrefractions.server.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.radiantrefractions.RadiantRefractionsMod;

public class SoundRegistryRR {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, RadiantRefractionsMod.MODID);

    public static final Holder<SoundEvent> FLASHLIGHT_CLICK = SOUND_EVENTS.register("flashlight_click", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LIGHTER_OPEN = SOUND_EVENTS.register("lighter_open", SoundEvent::createVariableRangeEvent);
    public static final Holder<SoundEvent> LIGHTER_CLOSE = SOUND_EVENTS.register("lighter_close", SoundEvent::createVariableRangeEvent);

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
