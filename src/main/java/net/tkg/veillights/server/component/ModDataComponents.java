package net.tkg.veillights.server.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.veillights.VeilLightsMod;

import java.util.UUID;
import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(VeilLightsMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LIGHT = register("light",
            builder -> builder.persistent(Codec.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> UUID = register("uuid",
            builder -> builder.persistent(UUIDUtil.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PLAY_USE_ANIM = register("play_use_anim",
            builder -> builder.persistent(Codec.BOOL));


    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name,
                                                                                          UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
