package net.tkg.radiantrefractions.client.model;

import net.minecraft.resources.ResourceLocation;
import net.tkg.radiantrefractions.RadiantRefractionsMod;
import net.tkg.radiantrefractions.server.item.BatteryItem;
import software.bernie.geckolib.model.GeoModel;

public class BatteryItemModel extends GeoModel<BatteryItem> {

    @Override
    public ResourceLocation getModelResource(BatteryItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "geo/battery.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BatteryItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "textures/item/battery.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BatteryItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "animations/empty.animation.json");
    }

}
