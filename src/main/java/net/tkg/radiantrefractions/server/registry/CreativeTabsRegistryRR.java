package net.tkg.radiantrefractions.server.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.radiantrefractions.RadiantRefractionsMod;

import java.util.function.Supplier;

public class CreativeTabsRegistryRR {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RadiantRefractionsMod.MODID);

    public static final Supplier<CreativeModeTab> ITEMS_TAB = CREATIVE_MODE_TAB.register("items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ItemRegistryRR.LIGHTER.get()))
                    .title(Component.translatable("creativetab.radiantrefractions.items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ItemRegistryRR.LIGHTER);
                        output.accept(ItemRegistryRR.FLASHLIGHT);
                        output.accept(ItemRegistryRR.BATTERY);

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
