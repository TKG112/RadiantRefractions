package net.tkg.radiantrefractions.server.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.radiantrefractions.RadiantRefractionsMod;

import java.util.UUID;
import java.util.function.UnaryOperator;

public class DataComponentsRegistryRR {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(RadiantRefractionsMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LIGHT = register("light",
            builder -> builder.persistent(Codec.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> UUID = register("uuid",
            builder -> builder.persistent(UUIDUtil.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PLAY_USE_ANIM = register("play_use_anim",
            builder -> builder.persistent(Codec.BOOL));

    public static final DataComponentType<ItemStack> BATTERY = DataComponentType.<ItemStack>builder().persistent(ItemStack.CODEC).build();


    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name,
                                                                                          UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
