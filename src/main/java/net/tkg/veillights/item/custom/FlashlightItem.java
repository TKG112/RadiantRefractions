package net.tkg.veillights.item.custom;

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
import net.tkg.veillights.component.ModDataComponents;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FlashlightItem extends Item {
    private final AreaLight flashlight_light = new AreaLight().setColor(1f, 1f, 1f).setBrightness(1.5f).setDistance(200).setSize(0, 0); // Light instance for the item
    private boolean lightAdded = false; // Flag to track if the light has been added

    public FlashlightItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide) { // Ensure this runs only on the server
            ItemStack stack = player.getItemInHand(usedHand);

            // Get the current "light" state (default to false if missing)
            boolean currentState = stack.getOrDefault(ModDataComponents.LIGHT, false);

            // Toggle the "light" state
            stack.set(ModDataComponents.LIGHT, !currentState);
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        boolean isLightOn = stack.getOrDefault(ModDataComponents.LIGHT, false);

        if (level.isClientSide && isSelected) { // Client-side only, while the item is in hand
            if (isLightOn && !lightAdded) {
                // Add light to renderer if it hasn't been added yet
                VeilRenderSystem.renderer().getLightRenderer().addLight(flashlight_light);
                lightAdded = true; // Mark the light as added
            } else if (!isLightOn && lightAdded) {
                // Remove light if it's off and was previously added
                VeilRenderSystem.renderer().getLightRenderer().removeLight(flashlight_light);
                lightAdded = false; // Mark the light as removed
            }

            if (isLightOn) {

                flashlight_light.setPosition(entity.getX(), entity.getY() + 1, entity.getZ());

                // Get the partial tick time (used for smooth rotation)
                float partialTick = Minecraft.getInstance().getFrameTimeNs();

                Vec3 viewVector = entity.getViewVector(partialTick);
                Vector3f direction = new Vector3f((float) -viewVector.x(), (float) -viewVector.y(), (float) -viewVector.z());

                // Create a quaternion from the view vector directly (this is an alternative to using yaw and pitch)
                Quaternionf viewRotation = new Quaternionf().lookAlong(direction, new Vector3f(0, 1, 0));

                // Set the light's orientation to match the player's view
                flashlight_light.setOrientation(viewRotation);

            }
        } else if (level.isClientSide && !isSelected) {
            VeilRenderSystem.renderer().getLightRenderer().removeLight(flashlight_light);
            lightAdded = false; // Mark the light as removed
        }
    }
}