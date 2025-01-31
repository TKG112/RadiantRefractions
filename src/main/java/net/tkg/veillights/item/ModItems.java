package net.tkg.veillights.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.veillights.VeilLightsMod;
import net.tkg.veillights.item.custom.FlashlightItem;
import net.tkg.veillights.item.custom.LighterItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VeilLightsMod.MODID);

    public static final DeferredItem<Item> FLASHLIGHT = ITEMS.register("flashlight",
            () -> new FlashlightItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> LIGHTER = ITEMS.register("lighter",
            () -> new LighterItem(new Item.Properties().stacksTo(1)));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
