package net.tkg.radiantrefractions.server.item;

import com.ibm.icu.impl.Pair;
import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.tkg.radiantrefractions.client.renderer.LighterItemRenderer;
import net.tkg.radiantrefractions.server.registry.DataComponentsRegistryRR;
import net.tkg.radiantrefractions.server.registry.SoundRegistryRR;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class LighterItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final Map<UUID, PointLight> activeLights = new HashMap<>();

    public static ItemDisplayContext transformType;

    public LighterItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        // Cooldown check
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        player.getCooldowns().addCooldown(this, 25);

        // Get the current light state BEFORE toggling
        boolean currentState = stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false);

        if (!level.isClientSide) {
            ensureUUID(stack);
            stack.set(DataComponentsRegistryRR.LIGHT, !currentState);

            // Choose sound based on the PREVIOUS state
            level.playSeededSound(
                    null, // null means all nearby players hear it
                    player.getX(), player.getY(), player.getZ(),
                    currentState ? SoundRegistryRR.LIGHTER_CLOSE : SoundRegistryRR.LIGHTER_OPEN,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f,
                    0
            );
        }

        if (level.isClientSide) {
            ensureUUID(stack);
            stack.set(DataComponentsRegistryRR.PLAY_USE_ANIM, true);
        }

        return super.use(level, player, usedHand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {

        boolean isLightOn = stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false);
        tooltipComponents.add(Component.translatable(isLightOn ? "tooltip.item.on" : "tooltip.item.off")
                .withStyle(isLightOn ? ChatFormatting.GREEN : ChatFormatting.RED));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player) || !level.isClientSide) return;

        ensureUUID(stack);
        UUID lighterUUID = getUUID(stack);
        boolean isLightOn = stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false);
        PointLight light = activeLights.get(lighterUUID);



        if ((isSelected || isHoldingInEitherHand(player, stack)) && isLightOn) {
            if (light == null) {
                float frameTime = (float) Minecraft.getInstance().getFrameTimeNs();
                Vec3 viewVector = player.getViewVector(frameTime);
                double forwardOffset = 0.4;
                double verticalOffset = 0.0;
                double targetX = player.getX() + (viewVector.x * forwardOffset);
                double targetY = player.getEyeY() + (viewVector.y * forwardOffset) + verticalOffset;
                double targetZ = player.getZ() + (viewVector.z * forwardOffset);

                light = new PointLight()
                        .setColor(1f, 0.5f, 0f)
                        .setBrightness(1f)
                        .setRadius(20f)
                        .setPosition(targetX, targetY, targetZ);

                activeLights.put(lighterUUID, light);
                VeilRenderSystem.renderer().getLightRenderer().addLight(light);
            }
        } else {
            if (light != null) {
                VeilRenderSystem.renderer().getLightRenderer().removeLight(light);
                activeLights.remove(lighterUUID);
            }
        }
    }

    private void ensureUUID(ItemStack stack) {
        if (!stack.has(DataComponentsRegistryRR.UUID)) {
            stack.set(DataComponentsRegistryRR.UUID, UUID.randomUUID());
        }
    }

    private UUID getUUID(ItemStack stack) {
        return stack.getOrDefault(DataComponentsRegistryRR.UUID, UUID.randomUUID());
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LighterItem::onRenderTick);
    }

    private static final Map<Pair<UUID, InteractionHand>, PointLight> otherPlayerActiveLights = new HashMap<>();

    private static void onRenderTick(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player localPlayer = mc.player;
        float frameTime = (float) Minecraft.getInstance().getFrameTimeNs() / 1000000000;

        // Handle LOCAL PLAYER lights
        activeLights.forEach((uuid, light) -> {
            ItemStack stack = findLighterInHand(localPlayer);
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
        });

        // Handle OTHER PLAYERS lights
        Map<Pair<UUID, InteractionHand>, PointLight> currentFrameLights = new HashMap<>();

        for (Player player : mc.level.players()) {
            if (player == localPlayer) continue;

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof LighterItem && stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false)) {
                    Pair<UUID, InteractionHand> key = Pair.of(player.getUUID(), hand);

                    PointLight light = otherPlayerActiveLights.computeIfAbsent(key, k -> {
                        PointLight newLight = new PointLight()
                                .setColor(1f, 0.5f, 0f)
                                .setBrightness(1f)
                                .setRadius(20)
                                .setPosition(player.getX(), player.getEyeY(), player.getZ());
                        VeilRenderSystem.renderer().getLightRenderer().addLight(newLight);
                        return newLight;
                    });

                    float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);

                    float xRot = player.xRotO + (player.getXRot() - player.xRotO) * partialTicks;
                    float yRot = player.yRotO + (player.getYRot() - player.yRotO) * partialTicks;

                    Vec3 viewVector = Vec3.directionFromRotation(xRot, yRot);

                    double forwardOffset = 0.4;
                    double verticalOffset = 0.0;

                    double targetX = player.getX() + (viewVector.x * forwardOffset);
                    double targetY = player.getEyeY() + (viewVector.y * forwardOffset) + verticalOffset;
                    double targetZ = player.getZ() + (viewVector.z * forwardOffset);

                    light.setPosition(targetX, targetY, targetZ);
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
                !VeilRenderSystem.renderer().getLightRenderer().getLights(LightTypeRegistry.POINT.get()).contains(entry.getValue())
        );
    }

    private static ItemStack findLighterInHand(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof LighterItem) {
                return stack;
            }
        }
        return null;
    }

    private static boolean isHoldingInEitherHand(Player player, ItemStack stack) {
        return player.getItemInHand(InteractionHand.MAIN_HAND) == stack || player.getItemInHand(InteractionHand.OFF_HAND) == stack;
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate)
                .triggerableAnim("use", RawAnimation.begin())); // optional, can be removed
    }

    private static final RawAnimation ANIM_OPEN = RawAnimation.begin().thenPlay("use_open").thenLoop("idle_open");
    private static final RawAnimation ANIM_CLOSE = RawAnimation.begin().thenPlay("use_close").thenLoop("idle_close");

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        ItemStack stack = state.getData(DataTickets.ITEMSTACK);
        ItemDisplayContext context = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);

        if (context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return PlayState.STOP;
        }

        boolean isLightOn = stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false);


        if (isLightOn && !state.isCurrentAnimation(ANIM_OPEN)) {
            state.setAnimation(ANIM_OPEN);
        } else if (!isLightOn && !state.isCurrentAnimation(ANIM_CLOSE)) {
            state.setAnimation(ANIM_CLOSE);
        }

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
            private LighterItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new LighterItemRenderer();
                return this.renderer;
            }
        });
    }

    public void getTransformType(ItemDisplayContext type) {
        this.transformType = type;
    }

}
