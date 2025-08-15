package net.tkg.radiantrefractions.client.renderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.tkg.radiantrefractions.client.model.FlashlightThirdPersonModel;
import net.tkg.radiantrefractions.server.item.FlashlightItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class FlashlightThirdPersonRenderer extends GeoItemRenderer<FlashlightItem> {
    public FlashlightThirdPersonRenderer() {
        super(new FlashlightThirdPersonModel());
    }

    @Override
    public RenderType getRenderType(FlashlightItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}

