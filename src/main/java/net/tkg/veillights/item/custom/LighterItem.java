package net.tkg.veillights.item.custom;

import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.tkg.veillights.component.ModDataComponents;
import org.jetbrains.annotations.NotNull;

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

        if (isSelected && isLightOn) {
            if (light == null) {
                // Create a new light instance for this lighter
                light = new PointLight().setColor(1f, 0.5f, 0f).setBrightness(1f).setRadius(20f);
                activeLights.put(lighterUUID, light);
                VeilRenderSystem.renderer().getLightRenderer().addLight(light);
            }
        } else {
            if (light != null) {
                // Remove the light when not selected or turned off
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

    private static void onRenderTick(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;

        // Loop through all active lighter lights
        activeLights.forEach((uuid, light) -> {
            ItemStack stack = findLighterInHand(player);
            if (stack == null) {
                // Remove light if the lighter is no longer in hand
                VeilRenderSystem.renderer().getLightRenderer().removeLight(light);
                return;
            }

            // Update light position
            light.setPosition(player.getX(), player.getY() + 1, player.getZ());
        });

        // Remove any lights that are no longer active
        activeLights.entrySet().removeIf(entry -> !VeilRenderSystem.renderer().getLightRenderer().getLights(LightTypeRegistry.POINT.get()).contains(entry.getValue()));
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
}
