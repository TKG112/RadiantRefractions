package net.tkg.radiantrefractions.server.item;

import com.ibm.icu.impl.Pair;
import net.minecraft.ChatFormatting;
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
import net.tkg.radiantrefractions.client.lights.LighterLights;
import net.tkg.radiantrefractions.client.renderer.LighterItemRenderer;
import net.tkg.radiantrefractions.server.registry.DataComponentsRegistryRR;
import net.tkg.radiantrefractions.server.registry.SoundRegistryRR;
import org.joml.Vector3f;
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

    public static ItemDisplayContext transformType;

    public LighterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        player.getCooldowns().addCooldown(this, 25);

        boolean currentState = stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false);

        if (!level.isClientSide) {
            ensureUUID(stack);
            stack.set(DataComponentsRegistryRR.LIGHT, !currentState);

            level.playSeededSound(null, player.getX(), player.getY(), player.getZ(), currentState ? SoundRegistryRR.LIGHTER_CLOSE : SoundRegistryRR.LIGHTER_OPEN, SoundSource.PLAYERS, 1.0f, 1.0f, 0);
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
        tooltipComponents.add(Component.translatable(isLightOn ? "tooltip.item.on" : "tooltip.item.off").withStyle(isLightOn ? ChatFormatting.GREEN : ChatFormatting.RED));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LighterItem::onRenderTick);
    }

    private static final Map<Pair<UUID, InteractionHand>, LighterLights> otherPlayerActiveLights = new HashMap<>();
    private static final Map<UUID, LighterLights> activeLights = new HashMap<>();

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player) || !level.isClientSide) return;

        ensureUUID(stack);
        UUID lighterUUID = getUUID(stack);
        boolean isLightOn = stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false);
        LighterLights light = activeLights.get(lighterUUID);

        if ((isSelected || isHoldingInEitherHand(player, stack)) && isLightOn) {
            if (light == null) {
                light = new LighterLights(player.getPosition(1));
                activeLights.put(lighterUUID, light);
            }
        } else if (light != null) {
            light.remove();
            activeLights.remove(lighterUUID);
        }
    }

    private static void onRenderTick(RenderLevelStageEvent event) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player localPlayer = mc.player;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        // Local player
        activeLights.forEach((uuid, light) -> {
            ItemStack stack = findLighterInHand(localPlayer);
            if (stack == null) {
                light.remove();
                return;
            }

            Vec3 view = localPlayer.getViewVector(partial);
            double ox = 0.4;
            Vector3f pos = new Vector3f(
                    (float) (localPlayer.getX() + view.x * ox),
                    (float) (localPlayer.getEyeY() + view.y * ox),
                    (float) (localPlayer.getZ() + view.z * ox)
            );
            light.update(pos);
        });

        // Other players
        Map<Pair<UUID, InteractionHand>, LighterLights> currentFrame = new HashMap<>();

        for (Player p : mc.level.players()) {
            if (p == localPlayer) continue;

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = p.getItemInHand(hand);
                if (stack.getItem() instanceof LighterItem &&
                        stack.getOrDefault(DataComponentsRegistryRR.LIGHT, false)) {

                    var key = com.ibm.icu.impl.Pair.of(p.getUUID(), hand);
                    var light = otherPlayerActiveLights.computeIfAbsent(key, k -> new LighterLights(localPlayer.getPosition(partial)));

                    float xRot = p.getViewXRot(partial);
                    float yRot = p.getViewYRot(partial);
                    net.minecraft.world.phys.Vec3 view = net.minecraft.world.phys.Vec3.directionFromRotation(xRot, yRot);

                    double ox = 0.4;
                    org.joml.Vector3f pos = new org.joml.Vector3f(
                            (float) (p.getX() + view.x * ox),
                            (float) (p.getEyeY() + view.y * ox),
                            (float) (p.getZ() + view.z * ox)
                    );

                    light.update(pos);
                    currentFrame.put(key, light);
                }
            }
        }

        otherPlayerActiveLights.entrySet().removeIf(e -> {
            if (!currentFrame.containsKey(e.getKey())) {
                e.getValue().remove();
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
        return stack.getOrDefault(DataComponentsRegistryRR.UUID, UUID.randomUUID());
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
                .triggerableAnim("use", RawAnimation.begin()));
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
