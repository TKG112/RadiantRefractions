package net.tkg.radiantrefractions.client.lights;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import org.joml.Vector3f;

public final class LighterLights {

    private final LightRenderHandle<PointLightData> handle;

    public LighterLights() {
        LightRenderer lightRenderer = VeilRenderSystem.renderer().getLightRenderer();
        this.handle = lightRenderer.addLight(new PointLightData());
        PointLightData light = handle.getLightData();
        light.setBrightness(1f);
        light.setRadius(20f);
        light.setColor(1.0f, 0.5f, 0f);
        light.setPosition(0.0, -1000.0, 0.0);
    }

    public void update(Vector3f pos) {
        PointLightData light = handle.getLightData();
        light.setPosition(pos.x, pos.y, pos.z);
    }

    public LightRenderHandle<PointLightData> getHandle() {
        return handle;
    }

    public void remove() {
        handle.free();
    }
}
