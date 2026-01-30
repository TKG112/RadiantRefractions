package net.tkg.radiantrefractions.server.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.tkg.radiantrefractions.RadiantRefractionsMod;

@EventBusSubscriber(modid = RadiantRefractionsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ConfigRegistryRR
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue BATTERY_DURABILITY = BUILDER
            .comment("Battery durability, set to 0 for no durability")
            .defineInRange("battery_durability", 300, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue BATTERY_TIMER = BUILDER
            .comment("How many ticks it must pass to do 1 damage to the battery durability (20 ticks = 1 second)")
            .defineInRange("battery_timer", 20, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue FLASHLIGHT_LIGHT_DISTANCE = BUILDER
            .comment("Flashlight light distance")
            .defineInRange("flashlight_light_distance", 200.0, 0.0, 1000.0);

    private static final ModConfigSpec.IntValue FLASHLIGHT_LIGHT_ANGLE = BUILDER
            .comment("Flashlight light angle in degrees")
            .defineInRange("flashlight_light_angle", 45, 0, 180);

    private static final ModConfigSpec.DoubleValue LIGHTER_LIGHT_RADIUS = BUILDER
            .comment("Lighter light radius")
            .defineInRange("lighter_light_radius", 20.0, 0.0, 100.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int battery_durability;
    public static int battery_timer;
    public static double flashlight_light_distance;
    public static int flashlight_light_angle;
    public static double lighter_light_radius;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        battery_durability = BATTERY_DURABILITY.get();
        battery_timer = BATTERY_TIMER.get();
        flashlight_light_distance = FLASHLIGHT_LIGHT_DISTANCE.get();
        flashlight_light_angle = FLASHLIGHT_LIGHT_ANGLE.get();
        lighter_light_radius = LIGHTER_LIGHT_RADIUS.get();
    }
}