package net.tkg.veillights.item.custom;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tkg.veillights.component.ModDataComponents;
import org.jetbrains.annotations.NotNull;

public class LighterItem extends Item {
    private final PointLight lighter_light = new PointLight().setColor(1f, 0.5f, 0f).setBrightness(1f).setRadius(20f); // Light instance for the item
    private boolean lightAdded = false; // Flag to track if the light has been added

    public LighterItem(Properties properties) {
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
        if (level.isClientSide && isSelected) { // Client-side only, while the item is in hand
            boolean isLightOn = stack.getOrDefault(ModDataComponents.LIGHT, false);

            if (isLightOn && !lightAdded) {
                // Add light to renderer if it hasn't been added yet
                VeilRenderSystem.renderer().getLightRenderer().addLight(lighter_light);
                lightAdded = true; // Mark the light as added
            } else if (!isLightOn && lightAdded) {
                // Remove light if it's off and was previously added
                VeilRenderSystem.renderer().getLightRenderer().removeLight(lighter_light);
                lightAdded = false; // Mark the light as removed
            }

            if (isLightOn) {
                lighter_light.setPosition(entity.getX(), entity.getY() + 1, entity.getZ());
            }
        } else if (level.isClientSide && !isSelected) {
            VeilRenderSystem.renderer().getLightRenderer().removeLight(lighter_light);
            lightAdded = false; // Mark the light as removed
        }
    }
}
