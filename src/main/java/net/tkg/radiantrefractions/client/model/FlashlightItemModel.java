package net.tkg.radiantrefractions.client.model;

import net.minecraft.resources.ResourceLocation;
import net.tkg.radiantrefractions.RadiantRefractionsMod;
import net.tkg.radiantrefractions.server.item.FlashlightItem;
import software.bernie.geckolib.model.GeoModel;

public class FlashlightItemModel extends GeoModel<FlashlightItem> {

    @Override
    public ResourceLocation getModelResource(FlashlightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "geo/flashlight.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FlashlightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "textures/item/flashlight.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FlashlightItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "animations/flashlight.animation.json");
    }

}