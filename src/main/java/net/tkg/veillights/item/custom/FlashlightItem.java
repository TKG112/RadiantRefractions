package net.tkg.veillights.item.custom;

import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.AreaLight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
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

public class FlashlightItem extends Item {
    private static final Map<UUID, AreaLight> activeLights = new HashMap<>(); // Map of active lights

    public FlashlightItem(Properties properties) {
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

        ensureUUID(stack); // Ensure the flashlight has a UUID
        UUID flashlightUUID = getUUID(stack);
        boolean isLightOn = stack.getOrDefault(ModDataComponents.LIGHT, false);
        AreaLight light = activeLights.get(flashlightUUID);

        if (isSelected && isLightOn) {
            if (light == null) {
                // Create a new light instance for this flashlight
                light = new AreaLight().setColor(1f, 1f, 1f).setBrightness(1.5f).setDistance(200).setSize(0, 0);
                activeLights.put(flashlightUUID, light);
                VeilRenderSystem.renderer().getLightRenderer().addLight(light);
            }
        } else {
            if (light != null) {
                // Remove the light when not selected or turned off
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
        return stack.getOrDefault(ModDataComponents.UUID, UUID.randomUUID());
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FlashlightItem::onRenderTick);
    }

    private static void onRenderTick(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;

        // Loop through all active flashlight lights
        activeLights.forEach((uuid, light) -> {
            ItemStack stack = findFlashlightInHand(player);
            if (stack == null) {
                // Remove light if the flashlight is no longer in hand
                VeilRenderSystem.renderer().getLightRenderer().removeLight(light);
                return;
            }

            // Update light position & orientation
            light.setPosition(player.getX(), player.getEyeY(), player.getZ());

            float partialTick = Minecraft.getInstance().getFrameTimeNs();
            Vec3 viewVector = player.getViewVector(partialTick);
            Vector3f direction = new Vector3f((float) -viewVector.x(), (float) -viewVector.y(), (float) -viewVector.z());
            Quaternionf viewRotation = new Quaternionf().lookAlong(direction, new Vector3f(0, 1, 0));

            light.setOrientation(viewRotation);
        });

        // Remove any lights that are no longer active
        activeLights.entrySet().removeIf(entry -> !VeilRenderSystem.renderer().getLightRenderer().getLights(LightTypeRegistry.AREA.get()).contains(entry.getValue()));
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
}
