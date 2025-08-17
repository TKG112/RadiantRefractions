package net.tkg.radiantrefractions.client.lights;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.AreaLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class FlashlightLights {
    private final LightRenderHandle<AreaLightData> handle;

    public FlashlightLights(Vec3 pos) {
        LightRenderer lightRenderer = VeilRenderSystem.renderer().getLightRenderer();
        this.handle = lightRenderer.addLight(new AreaLightData());
        AreaLightData light = handle.getLightData();
        light.setBrightness(1.5f);
        light.setDistance(200.0f);
        light.setSize(0, 0);
        light.getPosition().set(pos.x, pos.y, pos.z);
    }

    public void update(Vector3f pos, float rotX, float rotY) {
        AreaLightData light = handle.getLightData();
        light.getPosition().set(pos.x, pos.y, pos.z);
        light.getOrientation().set(new Quaternionf().rotateXYZ((float) Math.toRadians(-rotX), (float) Math.toRadians(rotY), 0f));
    }

    public LightRenderHandle<AreaLightData> getHandle() {
        return handle;
    }

    public void remove() {
        handle.free();
    }
}
