package net.tkg.radiantrefractions.server.item;

import com.ibm.icu.impl.Pair;
import foundry.veil.api.client.render.light.data.AreaLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.tkg.radiantrefractions.client.lights.FlashlightLights;
import net.tkg.radiantrefractions.client.renderer.FlashlightItemRenderer;
import net.tkg.radiantrefractions.server.registry.ConfigRegistryRR;
import net.tkg.radiantrefractions.server.registry.DataComponentsRegistryRR;
import net.tkg.radiantrefractions.server.registry.SoundRegistryRR;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.Consumer;

public class FlashlightItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public FlashlightItem(Properties properties) {
        super(properties.component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    private static boolean isBattery(ItemStack stack) {
        return stack.getItem() instanceof BatteryItem;
    }

    private static BundleContents getContents(ItemStack flashlight) {
        return flashlight.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
    }

    private ItemStack peekBattery(ItemStack flashlight) {
        BundleContents contents = flashlight.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        for (ItemStack stack : contents.items()) {
            if (stack.getItem() instanceof BatteryItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasBattery(ItemStack flashlight) {
        return !getContents(flashlight).isEmpty();
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack flashlight, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;

        BundleContents contents = getContents(flashlight);

        if (other.isEmpty()) {
            if (contents.isEmpty()) return false;

            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
            ItemStack removed = mutable.removeOne();
            if (removed == null || removed.isEmpty()) return false;

            access.set(removed);
            flashlight.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());

            if (flashlight.getOrDefault(DataComponentsRegistryRR.LIGHT, false)) {
                flashlight.set(DataComponentsRegistryRR.LIGHT, false);
            }
            return true;
        }

        if (!isBattery(other)) return false;
        if (!contents.isEmpty()) return false;

        ItemStack one = other.split(1);

        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        int added = mutable.tryInsert(one);
        if (added <= 0) {
            other.grow(1);
            return false;
        }

        flashlight.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        access.set(other);
        return true;
    }


    @Override
    public boolean overrideStackedOnOther(ItemStack flashlight, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;

        if (!hasBattery(flashlight)) {
            ItemStack inSlot = slot.getItem();
            BundleContents.Mutable mutable = new BundleContents.Mutable(getContents(flashlight));
            if (inSlot.isEmpty()) {
                mutable.removeOne();
            }
            if (!inSlot.isEmpty() && isBattery(inSlot)) {
                ItemStack taken = slot.safeTake(1, 1, player);
                if (taken.isEmpty()) return false;

                int added = mutable.tryInsert(taken);
                if (added == 1) {
                    flashlight.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
                    return true;
                } else {
                    ItemStack remainder = slot.safeInsert(taken);
                    if (!remainder.isEmpty()) {
                        player.addItem(remainder);
                    }
                    return false;
                }
            }
        }

        if (slot.getItem().isEmpty() && hasBattery(flashlight)) {
            BundleContents.Mutable mutable = new BundleContents.Mutable(getContents(flashlight));
            ItemStack removed = mutable.removeOne();
            if (removed != null && !removed.isEmpty()) {
                ItemStack rem = slot.safeInsert(removed);
                if (rem.isEmpty()) {
                    flashlight.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
                    if (flashlight.getOrDefault(DataComponentsRegistryRR.LIGHT, false)) {
                        flashlight.set(DataComponentsRegistryRR.LIGHT, false);
                    }
                    return true;
                } else {
                    mutable.tryInsert(removed);
                }
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        player.getCooldowns().addCooldown(this, 25);

        if (!level.isClientSide) {
            ensureUUID(stack);

            boolean currentState = stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false);
            if (!currentState) {
                ItemStack battery = peekBattery(stack);
                boolean canTurnOn = !battery.isEmpty() && BatteryItem.hasCharge(battery);
                if (!canTurnOn) {
                    return InteractionResultHolder.fail(stack);
                }
            }
            stack.set(DataComponentsRegistryRR.LIGHT, !currentState);
        }

        if (level.isClientSide) {
            ensureUUID(stack);
            stack.set(DataComponentsRegistryRR.PLAY_USE_ANIM, true);
        }

        level.playSeededSound(player, player.getX(), player.getY(), player.getZ(), SoundRegistryRR.FLASHLIGHT_CLICK, SoundSource.PLAYERS, 1.0f, 1.0f, 0);

        return super.use(level, player, usedHand);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return !stack.has(DataComponents.HIDE_TOOLTIP) && !stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP) ? Optional.ofNullable(stack.get(DataComponents.BUNDLE_CONTENTS)).map(BundleTooltip::new) : Optional.empty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        boolean isLightOn = stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false);
        tooltipComponents.add(Component.translatable(isLightOn ? "tooltip.item.on" : "tooltip.item.off").withStyle(isLightOn ? ChatFormatting.GREEN : ChatFormatting.RED));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private static final Map<UUID, FlashlightLights> activeLights = new HashMap<>();
    private static final Map<Pair<UUID, InteractionHand>, FlashlightLights> otherPlayerActiveLights = new HashMap<>();

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false)) {
            if ((level.getGameTime() % ConfigRegistryRR.battery_timer) == 0) {
                BundleContents contents = getContents(stack);
                if (!contents.isEmpty()) {
                    BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
                    ItemStack battery = mutable.removeOne();
                    if (battery != null && !battery.isEmpty() && isBattery(battery)) {
                        boolean contributed = BatteryItem.drainCharge(battery, 1);
                        mutable.tryInsert(battery);
                        stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());

                        if (!contributed || !BatteryItem.hasCharge(battery)) {
                            stack.set(DataComponentsRegistryRR.LIGHT, false);
                        }
                    } else {
                        stack.set(DataComponentsRegistryRR.LIGHT, false);
                    }
                } else {
                    stack.set(DataComponentsRegistryRR.LIGHT, false);
                }
            }
        }

        if (level.isClientSide && entity instanceof Player player) {
            ensureUUID(stack);
            UUID flashlightUUID = getUUID(stack);
            boolean isLightOn = stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false);
            FlashlightLights light = activeLights.get(flashlightUUID);

            if ((isSelected || isHoldingInEitherHand(player, stack)) && isLightOn) {
                if (light == null) {
                    light = new FlashlightLights();
                    activeLights.put(flashlightUUID, light);
                }
            } else {
                if (light != null) {
                    light.remove();
                    activeLights.remove(flashlightUUID);
                }
            }
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FlashlightItem::onRenderTick);
    }

    private static void onRenderTick(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player localPlayer = mc.player;
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        // Local player
        activeLights.forEach((uuid, light) -> {
            ItemStack stack = findFlashlightInHand(localPlayer);
            if (stack == null) {
                light.remove();
                return;
            }

            Vec3 viewVector = localPlayer.getViewVector(partialTicks);
            double forwardOffset = 0.4;
            Vector3f targetPos = new Vector3f(
                    (float) (localPlayer.getX() + (viewVector.x * forwardOffset)),
                    (float) (localPlayer.getEyeY() + (viewVector.y * forwardOffset)),
                    (float) (localPlayer.getZ() + (viewVector.z * forwardOffset))
            );

            float smoothing = 0.025f;
            LightRenderHandle<AreaLightData> handle = light.getHandle();
            AreaLightData areaLightData = handle.getLightData();
            double currentPosX = areaLightData.getPosition().x;
            double currentPosY = areaLightData.getPosition().y;
            double currentPosZ = areaLightData.getPosition().z;
            Vector3f smoothedPos = new Vector3f(
                    (float) (currentPosX + (targetPos.x - currentPosX) * smoothing),
                    (float) (currentPosY + (targetPos.y - currentPosY) * smoothing),
                    (float) (currentPosZ + (targetPos.z - currentPosZ) * smoothing)
            );

            light.update(smoothedPos, localPlayer.getXRot(), localPlayer.getYRot());
        });

        // Other players
        Map<Pair<UUID, InteractionHand>, FlashlightLights> currentFrameLights = new HashMap<>();

        for (Player player : mc.level.players()) {
            if (player == localPlayer) continue;

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof FlashlightItem &&
                        stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false)) {

                    Pair<UUID, InteractionHand> key = Pair.of(player.getUUID(), hand);

                    FlashlightLights light = otherPlayerActiveLights.computeIfAbsent(key, k -> new FlashlightLights());

                    float xRot = player.getViewXRot(partialTicks);
                    float yRot = player.getViewYRot(partialTicks);
                    Vec3 viewVector = Vec3.directionFromRotation(xRot, yRot);

                    double forwardOffset = 0.4;
                    Vector3f pos = new Vector3f(
                            (float) (player.getX() + (viewVector.x * forwardOffset)),
                            (float) (player.getEyeY() + (viewVector.y * forwardOffset)),
                            (float) (player.getZ() + (viewVector.z * forwardOffset))
                    );

                    light.update(pos, xRot, yRot);
                    currentFrameLights.put(key, light);
                }
            }
        }

        otherPlayerActiveLights.entrySet().removeIf(entry -> {
            if (!currentFrameLights.containsKey(entry.getKey())) {
                entry.getValue().remove();
                return true;
            }
            return false;
        });
    }

    private void ensureUUID(ItemStack stack) {
        if (!stack.has(DataComponentsRegistryRR.UUID)) {
            stack.set(DataComponentsRegistryRR.UUID, UUID.randomUUID());
        }
    }

    private UUID getUUID(ItemStack stack) {
        return stack.get(DataComponentsRegistryRR.UUID);
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
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate)
                .triggerableAnim("use", RawAnimation.begin().thenPlay("use")));
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private static final RawAnimation USE = RawAnimation.begin().thenPlay("use");

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        AnimationController<?> controller = state.getController();
        ItemDisplayContext context = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        ItemStack stack = state.getData(DataTickets.ITEMSTACK);

        if (context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return PlayState.STOP;
        }

        if (stack != null && stack.getOrDefault(DataComponentsRegistryRR.PLAY_USE_ANIM, false)) {
            controller.setAnimation(USE);
            stack.set(DataComponentsRegistryRR.PLAY_USE_ANIM, false);
            return PlayState.CONTINUE;
        }

        if (controller.getCurrentAnimation() == null || controller.hasAnimationFinished()) {
            controller.setAnimation(IDLE);
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