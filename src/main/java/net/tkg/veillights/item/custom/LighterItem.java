package net.tkg.veillights.item.custom;

import com.ibm.icu.impl.Pair;
import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.AreaLight;
import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.tkg.veillights.component.ModDataComponents;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LighterItem extends Item {
    private static final Map<UUID, PointLight> activeLights = new HashMap<>(); // Map of active lights

    public LighterItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide) { // Ensure this runs only on the server
            ItemStack stack = player.getItemInHand(usedHand);
            ensureUUID(stack);

            // Toggle the "light" state
            boolean currentState = stack.getOrDefault(ModDataComponents.LIGHT, false);
            stack.set(ModDataComponents.LIGHT, !currentState);
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player) || !level.isClientSide) return;

        ensureUUID(stack); // Ensure the lighter has a UUID
        UUID lighterUUID = getUUID(stack);
        boolean isLightOn = stack.getOrDefault(ModDataComponents.LIGHT, false);
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

                // Create a new light instance for this lighter
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
        if (!stack.has(ModDataComponents.UUID)) {
            stack.set(ModDataComponents.UUID, UUID.randomUUID());
        }
    }

    private UUID getUUID(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.UUID, UUID.randomUUID());
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

        // Handle LOCAL PLAYER's lights (existing logic)
        activeLights.forEach((uuid, light) -> {
            ItemStack stack = findLighterInHand(localPlayer);
            if (stack == null) {
                VeilRenderSystem.renderer().getLightRenderer().removeLight(light);
                return;
            }

            Vec3 viewVector = localPlayer.getViewVector(frameTime);
            double forwardOffset = 0.4;
            double verticalOffset = 0.0;

            // Calculate target position with smoothing
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

        // Handle OTHER PLAYERS' lights (new multiplayer logic)
        Map<Pair<UUID, InteractionHand>, PointLight> currentFrameLights = new HashMap<>();

        for (Player player : mc.level.players()) {
            if (player == localPlayer) continue; // Skip local player

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof LighterItem && stack.getOrDefault(ModDataComponents.LIGHT, false)) {
                    Pair<UUID, InteractionHand> key = Pair.of(player.getUUID(), hand);

                    // Get or create light
                    PointLight light = otherPlayerActiveLights.computeIfAbsent(key, k -> {
                        PointLight newLight = new PointLight()
                                .setColor(1f, 0.5f, 0f)
                                .setBrightness(1f)
                                .setRadius(20)
                                .setPosition(player.getX(), player.getEyeY(), player.getZ());
                        VeilRenderSystem.renderer().getLightRenderer().addLight(newLight);
                        return newLight;
                    });

                    // Update position directly (no smoothing for other players)
                    Vec3 viewVector = player.getViewVector(mc.getFrameTimeNs());
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

        // Cleanup other players' lights that are no longer active
        otherPlayerActiveLights.entrySet().removeIf(entry -> {
            if (!currentFrameLights.containsKey(entry.getKey())) {
                VeilRenderSystem.renderer().getLightRenderer().removeLight(entry.getValue());
                return true;
            }
            return false;
        });

        // Existing cleanup for local player's lights
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
}
