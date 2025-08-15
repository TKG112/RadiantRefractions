package net.tkg.radiantrefractions.server.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.radiantrefractions.RadiantRefractionsMod;
import net.tkg.radiantrefractions.server.item.BatteryItem;
import net.tkg.radiantrefractions.server.item.FlashlightItem;
import net.tkg.radiantrefractions.server.item.LighterItem;

public class ItemRegistryRR {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RadiantRefractionsMod.MODID);

    public static final DeferredItem<Item> FLASHLIGHT = ITEMS.register("flashlight",
            () -> new FlashlightItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> LIGHTER = ITEMS.register("lighter",
            () -> new LighterItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BATTERY = ITEMS.register("battery",
            () -> new BatteryItem(new Item.Properties().stacksTo(64)));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
