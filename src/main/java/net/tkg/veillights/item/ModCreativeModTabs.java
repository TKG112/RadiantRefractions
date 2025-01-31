package net.tkg.veillights.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.veillights.VeilLightsMod;

import java.util.function.Supplier;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VeilLightsMod.MODID);

    public static final Supplier<CreativeModeTab> ITEMS_TAB = CREATIVE_MODE_TAB.register("items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.LIGHTER.get()))
                    .title(Component.translatable("creativetab.veillights.items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.LIGHTER);
                        output.accept(ModItems.FLASHLIGHT);

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
