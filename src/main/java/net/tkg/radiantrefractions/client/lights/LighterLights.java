package net.tkg.radiantrefractions.client.lights;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import net.minecraft.world.phys.Vec3;
import net.tkg.radiantrefractions.server.registry.ConfigRegistryRR;
import org.joml.Vector3f;

public final class LighterLights {

    private final LightRenderHandle<PointLightData> handle;

    public LighterLights(Vec3 pos) {
        LightRenderer lightRenderer = VeilRenderSystem.renderer().getLightRenderer();
        this.handle = lightRenderer.addLight(new PointLightData());
        PointLightData light = handle.getLightData();
        light.setBrightness(1f);
        light.setRadius((float) ConfigRegistryRR.lighter_light_radius);
        light.setColor(1.0f, 0.5f, 0f);
        light.setPosition(pos.x, pos.y, pos.z);
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
