package net.tkg.radiantrefractions.server.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.tkg.radiantrefractions.client.renderer.BatteryRenderer;
import net.tkg.radiantrefractions.server.registry.ConfigRegistryRR;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class BatteryItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BatteryItem(Properties properties) {
        super(properties);
    }

    public static boolean hasCharge(ItemStack battery) {
        int max = battery.getMaxDamage();
        if (max <= 0) {
            return true;
        }
        return battery.getDamageValue() < max;
    }


    public static boolean drainCharge(ItemStack battery, int amount) {
        if (!(battery.getItem() instanceof BatteryItem) || amount <= 0) return false;
        int max = battery.getMaxDamage();
        if (max <= 0) return false;
        int dmg = battery.getDamageValue();
        if (dmg >= max) return false;

        int newDmg = Math.min(max, dmg + amount);
        battery.setDamageValue(newDmg);
        return true;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ConfigRegistryRR.battery_durability;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getDamageValue() != 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int current = getMaxDamage(stack) - stack.getDamageValue();
        int max = getMaxDamage(stack);
        ChatFormatting color;

        if(current == 0) {
            color = ChatFormatting.RED;
        } else {
            color = ChatFormatting.GREEN;
        }

        tooltipComponents.add(Component.translatable("tooltip.item.energy", current, max).withStyle(color));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BatteryRenderer.BatteryItemRenderer renderer = null;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new BatteryRenderer.BatteryItemRenderer();

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
