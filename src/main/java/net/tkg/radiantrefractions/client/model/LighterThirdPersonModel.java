package net.tkg.radiantrefractions.client.model;

import net.minecraft.resources.ResourceLocation;
import net.tkg.radiantrefractions.RadiantRefractionsMod;
import net.tkg.radiantrefractions.server.item.LighterItem;
import software.bernie.geckolib.model.GeoModel;

public class LighterThirdPersonModel extends GeoModel<LighterItem> {

    @Override
    public ResourceLocation getModelResource(LighterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "geo/thirdperson_lighter.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LighterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "textures/item/lighter.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LighterItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "animations/lighter.animation.json");
    }
}
