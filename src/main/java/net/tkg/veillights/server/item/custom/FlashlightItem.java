package net.tkg.veillights.server.item.custom;

import com.ibm.icu.impl.Pair;
import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.AreaLight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.tkg.veillights.client.renderer.FlashlightItemRenderer;
import net.tkg.veillights.server.component.ModDataComponents;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class FlashlightItem extends Item implements GeoItem {
    private static final Map<UUID, AreaLight> activeLights = new HashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static ItemDisplayContext transformType;


    public FlashlightItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide) {
            ItemStack stack = player.getItemInHand(usedHand);
            ensureUUID(stack);

            boolean currentState = stack.getOrDefault(ModDataComponents.LIGHT, false);
            stack.set(ModDataComponents.LIGHT, !currentState);
        }
        // Trigger animation client-side
        if (level.isClientSide) {
            ItemStack stack = player.getItemInHand(usedHand);
            ensureUUID(stack);
            this.triggerAnim(player, GeoItem.getId(stack), "controller", "use");
        }

        return super.use(level, player, usedHand);
    }


    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player) || !level.isClientSide) return;

        ensureUUID(stack);
        UUID flashlightUUID = getUUID(stack);
        boolean isLightOn = stack.getOrDefault(ModDataComponents.LIGHT, false);
        AreaLight light = activeLights.get(flashlightUUID);

        if ((isSelected || isHoldingInEitherHand(player, stack)) && isLightOn) {
            if (light == null) {
                float partialTick = Minecraft.getInstance().getFrameTimeNs();
                Vec3 viewVector = player.getViewVector(partialTick);
                Vector3f direction = new Vector3f((float) -viewVector.x(), (float) -viewVector.y(), (float) -viewVector.z());
                Quaternionf viewRotation = new Quaternionf().lookAlong(direction, new Vector3f(0, 1, 0));
                light = new AreaLight()
                        .setColor(1f, 1f, 1f)
                        .setBrightness(1.5f).setDistance(200)
                        .setSize(0, 0)
                        .setPosition(entity.getX(), entity.getEyeY(), entity.getZ())
                        .setOrientation(viewRotation);

                activeLights.put(flashlightUUID, light);
                VeilRenderSystem.renderer().getLightRenderer().addLight(light);
            }
        } else {
            if (light != null) {
                VeilRenderSystem.renderer().getLightRenderer().removeLight(light);
                activeLights.remove(flashlightUUID);
            }
        }
    }

    private void ensureUUID(ItemStack stack) {
        if (!stack.has(ModDataComponents.UUID)) {
            stack.set(ModDataComponents.UUID, UUID.randomUUID());
        }
    }

    private UUID getUUID(ItemStack stack) {
        return stack.get(ModDataComponents.UUID); // Don't return a random UUID
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FlashlightItem::onRenderTick);
    }

    private static final Map<Pair<UUID, InteractionHand>, AreaLight> otherPlayerActiveLights = new HashMap<>();

    private static void onRenderTick(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player localPlayer = mc.player;
        float frameTime = (float) Minecraft.getInstance().getFrameTimeNs() / 1000000000;

        // Handle local player lights
        activeLights.forEach((uuid, light) -> {
            ItemStack stack = findFlashlightInHand(localPlayer);
            if (stack == null) {
                VeilRenderSystem.renderer().getLightRenderer().removeLight(light);
                return;
            }

            Vec3 viewVector = localPlayer.getViewVector(frameTime);
            double forwardOffset = 0.4;
            double verticalOffset = 0.0;

            double targetX = localPlayer.getX() + (viewVector.x * forwardOffset);
            double targetY = localPlayer.getEyeY() + (viewVector.y * forwardOffset) + verticalOffset;
            double targetZ = localPlayer.getZ() + (viewVector.z * forwardOffset);

            Vec3 currentPos = new Vec3(light.getPosition().x(), light.getPosition().y(), light.getPosition().z());
            double smoothFactor = 2.25;
            double newX = currentPos.x() + (targetX - currentPos.x()) * smoothFactor * frameTime;
            double newY = currentPos.y() + (targetY - currentPos.y()) * smoothFactor * frameTime;
            double newZ = currentPos.z() + (targetZ - currentPos.z()) * smoothFactor * frameTime;

            light.setPosition(newX, newY, newZ);

            Vector3f direction = new Vector3f((float) -viewVector.x(), (float) -viewVector.y(), (float) -viewVector.z());
            Quaternionf viewRotation = new Quaternionf().lookAlong(direction, new Vector3f(0, 1, 0));
            light.setOrientation(viewRotation);
        });

        // Handle other players lights
        Map<Pair<UUID, InteractionHand>, AreaLight> currentFrameLights = new HashMap<>();

        for (Player player : mc.level.players()) {
            if (player == localPlayer) continue;

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof FlashlightItem && stack.getOrDefault(ModDataComponents.LIGHT, false)) {
                    Pair<UUID, InteractionHand> key = Pair.of(player.getUUID(), hand);

                    float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);
                    float xRot = player.xRotO + (player.getXRot() - player.xRotO) * partialTicks;
                    float yRot = player.yRotO + (player.getYRot() - player.yRotO) * partialTicks;

                    Vec3 viewVector = Vec3.directionFromRotation(xRot, yRot);

                    Vector3f direction = new Vector3f((float) -viewVector.x(), (float) -viewVector.y(), (float) -viewVector.z());
                    Quaternionf viewRotation = new Quaternionf().lookAlong(direction, new Vector3f(0, 1, 0));

                    AreaLight light = otherPlayerActiveLights.computeIfAbsent(key, k -> {
                        AreaLight newLight = new AreaLight()
                                .setColor(1f, 1f, 1f)
                                .setBrightness(1.5f)
                                .setDistance(200)
                                .setSize(0, 0)
                                .setPosition(player.getX(), player.getEyeY(), player.getZ())
                                .setOrientation(viewRotation);
                        VeilRenderSystem.renderer().getLightRenderer().addLight(newLight);
                        return newLight;
                    });

                    double forwardOffset = 0.4;
                    double verticalOffset = 0.0;

                    double targetX = player.getX() + (viewVector.x * forwardOffset);
                    double targetY = player.getEyeY() + (viewVector.y * forwardOffset) + verticalOffset;
                    double targetZ = player.getZ() + (viewVector.z * forwardOffset);

                    light.setPosition(targetX, targetY, targetZ);

                    light.setOrientation(viewRotation);

                    currentFrameLights.put(key, light);
                }
            }
        }

        otherPlayerActiveLights.entrySet().removeIf(entry -> {
            if (!currentFrameLights.containsKey(entry.getKey())) {
                VeilRenderSystem.renderer().getLightRenderer().removeLight(entry.getValue());
                return true;
            }
            return false;
        });

        activeLights.entrySet().removeIf(entry ->
                !VeilRenderSystem.renderer().getLightRenderer().getLights(LightTypeRegistry.AREA.get()).contains(entry.getValue())
        );
    }

    private static ItemStack findFlashlightInHand(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof FlashlightItem) {
                return stack;
            }
        }
        return null;
    }


    private static boolean isHoldingInEitherHand(Player player, ItemStack stack) {
        return player.getItemInHand(InteractionHand.MAIN_HAND) == stack || player.getItemInHand(InteractionHand.OFF_HAND) == stack;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate)
                .triggerableAnim("use", RawAnimation.begin().thenPlay("use")));
    }


    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        RawAnimation use = RawAnimation.begin().thenPlay("use");

        if (state.isCurrentAnimation(use)) {
            return PlayState.CONTINUE;
        }
        state.setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private FlashlightItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new FlashlightItemRenderer();
                return this.renderer;
            }
        });
    }

    public void getTransformType(ItemDisplayContext type) {
        this.transformType = type;
    }


}
