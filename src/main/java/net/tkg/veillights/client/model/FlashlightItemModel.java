package net.tkg.veillights.client.model;

import net.minecraft.resources.ResourceLocation;
import net.tkg.veillights.VeilLightsMod;
import net.tkg.veillights.server.item.custom.FlashlightItem;
import software.bernie.geckolib.model.GeoModel;

public class FlashlightItemModel extends GeoModel<FlashlightItem> {

    @Override
    public ResourceLocation getModelResource(FlashlightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(VeilLightsMod.MODID, "geo/flashlight.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FlashlightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(VeilLightsMod.MODID, "textures/item/flashlight.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FlashlightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(VeilLightsMod.MODID, "animations/flashlight.animation.json");
    }

}