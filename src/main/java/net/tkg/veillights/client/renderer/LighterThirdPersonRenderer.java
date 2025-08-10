package net.tkg.veillights.client.renderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.tkg.veillights.client.model.LighterThirdPersonModel;
import net.tkg.veillights.server.item.custom.LighterItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class LighterThirdPersonRenderer extends GeoItemRenderer<LighterItem> {
    public LighterThirdPersonRenderer() {
        super(new LighterThirdPersonModel());
    }

    @Override
    public RenderType getRenderType(LighterItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
