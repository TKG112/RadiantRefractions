package net.tkg.veillights.client.model;

import net.minecraft.resources.ResourceLocation;
import net.tkg.veillights.VeilLightsMod;
import net.tkg.veillights.server.item.custom.LighterItem;
import software.bernie.geckolib.model.GeoModel;

public class LighterItemModel extends GeoModel<LighterItem> {

    @Override
    public ResourceLocation getModelResource(LighterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(VeilLightsMod.MODID, "geo/lighter.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LighterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(VeilLightsMod.MODID, "textures/item/lighter.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LighterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(VeilLightsMod.MODID, "animations/lighter.animation.json");
    }

}
