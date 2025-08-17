package net.tkg.radiantrefractions.server.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import software.bernie.geckolib.cache.object.GeoBone;

public class AnimUtils {
    public static void renderPartOverBone(ModelPart model, GeoBone bone, PoseStack stack,
                                          VertexConsumer buffer, int packedLightIn,
                                          int packedOverlayIn, float alpha) {
        if (model == null) return; // <-- prevent crash
        renderPartOverBone(model, bone, stack, buffer, packedLightIn, packedOverlayIn, 1.0f, 1.0f, 1.0f, alpha);
    }

    public static void renderPartOverBone(ModelPart model, GeoBone bone, PoseStack stack,
                                          VertexConsumer buffer, int packedLightIn,
                                          int packedOverlayIn,
                                          float r, float g, float b, float a) {
        if (model == null) return; // <-- prevent crash
        setupModelFromBone(model, bone);
        model.render(stack, buffer, packedLightIn, packedOverlayIn);
    }

    public static void setupModelFromBone(ModelPart model, GeoBone bone) {
        if (model == null || bone == null) return; // <-- guard
        model.setPos(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
        model.xRot = 0.0f;
        model.yRot = 0.0f;
        model.zRot = 0.0f;
    }
}

