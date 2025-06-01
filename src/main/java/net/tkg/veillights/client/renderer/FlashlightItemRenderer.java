package net.tkg.veillights.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.tkg.veillights.client.model.FlashlightItemModel;
import net.tkg.veillights.server.item.custom.FlashlightItem;
import net.tkg.veillights.server.util.AnimUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashSet;
import java.util.Set;

public class FlashlightItemRenderer extends GeoItemRenderer<FlashlightItem> {
    private final Set<String> hiddenBones = new HashSet<>();
    private MultiBufferSource currentBuffer;
    private RenderType renderType;
    private ItemDisplayContext transformType;
    private FlashlightItem animatable;

    public FlashlightItemRenderer() {
        super(new FlashlightItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        this.transformType = transformType;
        this.animatable = (FlashlightItem) stack.getItem();
        super.renderByItem(stack, transformType, poseStack, buffer, light, overlay);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, FlashlightItem animatable, BakedGeoModel model, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        this.currentBuffer = bufferSource;
        this.renderType = renderType;
        this.animatable = animatable;
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, FlashlightItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        String name = bone.getName();
        Minecraft mc = Minecraft.getInstance();

        if (name.equals("Left_arm") || name.equals("Right_arm")) {
            bone.setHidden(true);

            if (transformType != null && transformType.firstPerson()) {
                AbstractClientPlayer player = mc.player;
                if (player == null) return;

                float armsAlpha = player.isInvisible() ? 0.15f : 1.0f;
                PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
                PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();

                poseStack.pushPose();
                RenderUtil.translateMatrixToBone(poseStack, bone);
                RenderUtil.translateToPivotPoint(poseStack, bone);
                RenderUtil.rotateMatrixAroundBone(poseStack, bone);
                RenderUtil.scaleMatrixForBone(poseStack, bone);
                RenderUtil.translateAwayFromPivotPoint(poseStack, bone);

                ResourceLocation skin = player.getSkin().texture();
                VertexConsumer armConsumer = currentBuffer.getBuffer(RenderType.entitySolid(skin));
                VertexConsumer sleeveConsumer = currentBuffer.getBuffer(RenderType.entityTranslucent(skin));

                if (name.equals("Left_arm")) {
                    poseStack.translate(0f, 0f, 0f);
                    AnimUtils.renderPartOverBone(model.leftArm, bone, poseStack, armConsumer, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                    AnimUtils.renderPartOverBone(model.leftSleeve, bone, poseStack, sleeveConsumer, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                } else {
                    poseStack.translate(0f, 0f, 0f);
                    AnimUtils.renderPartOverBone(model.rightArm, bone, poseStack, armConsumer, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                    AnimUtils.renderPartOverBone(model.rightSleeve, bone, poseStack, sleeveConsumer, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                }

                poseStack.popPose();
            }

        } else {
            bone.setHidden(hiddenBones.contains(name));
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
