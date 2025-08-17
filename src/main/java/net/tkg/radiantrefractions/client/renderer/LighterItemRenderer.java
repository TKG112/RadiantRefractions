package net.tkg.radiantrefractions.client.renderer;

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
import net.tkg.radiantrefractions.RadiantRefractionsMod;
import net.tkg.radiantrefractions.client.model.LighterItemModel;
import net.tkg.radiantrefractions.server.item.FlashlightItem;
import net.tkg.radiantrefractions.server.item.LighterItem;
import net.tkg.radiantrefractions.server.util.AnimUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashSet;
import java.util.Set;

public class LighterItemRenderer extends GeoItemRenderer<LighterItem> {
    public LighterItemRenderer() {
        super(new LighterItemModel());
    }

    private static final ResourceLocation LIGHTER_TEXTURE = ResourceLocation.fromNamespaceAndPath(RadiantRefractionsMod.MODID, "textures/item/lighter.png");

    @Override
    public RenderType getRenderType(LighterItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;
    public ItemDisplayContext transformType;
    protected LighterItem animatable;
    private final Set<String> hiddenBones = new HashSet<>();

    private static final LighterThirdPersonRenderer thirdPersonRenderer = new LighterThirdPersonRenderer();

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        this.transformType = transformType;

        // Mirror model if in first-person left hand
        if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            // Flip X axis
            poseStack.scale(-1.0F, 1.0F, 1.0F);
            // Shift it back after mirroring so it stays in place
            poseStack.translate(-1.0F, 0.0F, 0.0F);
        }

        if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
                transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            if (this.animatable != null)
                this.animatable.getTransformType(transformType);

            super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            thirdPersonRenderer.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    @Override
    public void actuallyRender(PoseStack poseStack, LighterItem animatable, BakedGeoModel model, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        this.currentBuffer = bufferSource;
        this.renderType = renderType;
        this.animatable = animatable;
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void renderRecursively(PoseStack poseStack,
                                  LighterItem animatable,
                                  GeoBone bone,
                                  RenderType renderType,
                                  MultiBufferSource bufferSource,
                                  VertexConsumer buffer,
                                  boolean isReRender,
                                  float partialTick,
                                  int packedLight,
                                  int packedOverlay,
                                  int colour) {
        Minecraft mc = Minecraft.getInstance();
        String name = bone.getName();
        boolean renderingArms = false;

        if (name.equals("lefthand_pos") || name.equals("righthand_pos")) {
            bone.setHidden(true);
            renderingArms = true;
        } else {
            bone.setHidden(this.hiddenBones.contains(name));
        }

        if (renderingArms) {
            AbstractClientPlayer player = mc.player;
            float armsAlpha = player.isInvisible() ? 0.15f : 1.0f;
            PlayerRenderer playerRenderer =
                    (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
            PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();

            poseStack.pushPose();
            RenderUtil.translateMatrixToBone(poseStack, bone);
            RenderUtil.translateToPivotPoint(poseStack, bone);
            RenderUtil.rotateMatrixAroundBone(poseStack, bone);
            RenderUtil.scaleMatrixForBone(poseStack, bone);
            RenderUtil.translateAwayFromPivotPoint(poseStack, bone);

            // 🔹 Re-added: arm + sleeve builders
            ResourceLocation loc = player.getSkin().texture();
            VertexConsumer armBuilder =
                    this.currentBuffer.getBuffer(RenderType.entitySolid(loc));
            VertexConsumer sleeveBuilder =
                    this.currentBuffer.getBuffer(RenderType.entityTranslucent(loc));

            if (name.equals("lefthand_pos")) {
                AnimUtils.renderPartOverBone(model.leftArm, bone, poseStack,
                        armBuilder, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                AnimUtils.renderPartOverBone(model.leftSleeve, bone, poseStack,
                        sleeveBuilder, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
            } else if (name.equals("righthand_pos")) {
                AnimUtils.renderPartOverBone(model.rightArm, bone, poseStack,
                        armBuilder, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                AnimUtils.renderPartOverBone(model.rightSleeve, bone, poseStack,
                        sleeveBuilder, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
            }

            this.currentBuffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(this.animatable)));
            poseStack.popPose();
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource,
                buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public ResourceLocation getTextureLocation(LighterItem animatable) {
        return LIGHTER_TEXTURE;
    }
}

