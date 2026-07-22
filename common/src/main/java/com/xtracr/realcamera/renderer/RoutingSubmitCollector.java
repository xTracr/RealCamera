package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class RoutingSubmitCollector implements SubmitNodeCollector {
    private final SubmitNodeCollector defaultCollector;
    private final SubmitNodeCollector modelCollector;

    public RoutingSubmitCollector(SubmitNodeCollector defaultCollector, SubmitNodeCollector modelCollector) {
        this.defaultCollector = defaultCollector;
        this.modelCollector = modelCollector;
    }

    @Override
    public @NonNull OrderedSubmitNodeCollector order(int order) {
        return new RoutingOrderedCollector(order);
    }

    @Override
    public void submitShadow(@NonNull PoseStack poseStack, float radius, @NonNull List<EntityRenderState.ShadowPiece> pieces) {
        defaultCollector.submitShadow(poseStack, radius, pieces);
    }

    @Override
    public void submitNameTag(@NonNull PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, @NonNull Component name, boolean seeThrough, int lightCoords, double distanceToCameraSq, @NonNull CameraRenderState camera) {
        defaultCollector.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, distanceToCameraSq, camera);
    }

    @Override
    public void submitText(@NonNull PoseStack poseStack, float x, float y, @NonNull FormattedCharSequence string, boolean dropShadow, Font.@NonNull DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        defaultCollector.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
    }

    @Override
    public void submitFlame(@NonNull PoseStack poseStack, @NonNull EntityRenderState renderState, @NonNull Quaternionf rotation) {
        defaultCollector.submitFlame(poseStack, renderState, rotation);
    }

    @Override
    public void submitLeash(@NonNull PoseStack poseStack, EntityRenderState.@NonNull LeashState leashState) {
        defaultCollector.submitLeash(poseStack, leashState);
    }

    @Override
    public <S> void submitModel(@NonNull Model<? super S> model, @NonNull S state, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        modelCollector.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    }

    @Override
    public void submitModelPart(@NonNull ModelPart modelPart, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor) {
        modelCollector.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, sheeted, hasFoil, tintedColor, crumblingOverlay, outlineColor);
    }

    @Override
    public void submitMovingBlock(@NonNull PoseStack poseStack, @NonNull MovingBlockRenderState movingBlockRenderState) {
        defaultCollector.submitMovingBlock(poseStack, movingBlockRenderState);
    }

    @Override
    public void submitBlock(@NonNull PoseStack poseStack, @NonNull BlockState blockState, int lightCoords, int overlayCoords, int outlineColor) {
        defaultCollector.submitBlock(poseStack, blockState, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void submitBlockModel(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull BlockStateModel model, float red, float green, float blue, int lightCoords, int overlayCoords, int outlineColor) {
        defaultCollector.submitBlockModel(poseStack, renderType, model, red, green, blue, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void submitItem(@NonNull PoseStack poseStack, @NonNull ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int @NonNull [] tintLayers, @NonNull List<BakedQuad> quads, @NonNull RenderType renderType, ItemStackRenderState.@NonNull FoilType foilType) {
        defaultCollector.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, renderType, foilType);
    }

    @Override
    public void submitCustomGeometry(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull CustomGeometryRenderer customGeometryRenderer) {
        modelCollector.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
    }

    @Override
    public void submitParticleGroup(@NonNull ParticleGroupRenderer particleGroupRenderer) {
        defaultCollector.submitParticleGroup(particleGroupRenderer);
    }

    private final class RoutingOrderedCollector implements OrderedSubmitNodeCollector {
        private final OrderedSubmitNodeCollector defaultDelegate;
        private final OrderedSubmitNodeCollector modelDelegate;

        RoutingOrderedCollector(int order) {
            this.defaultDelegate = defaultCollector.order(order);
            this.modelDelegate = modelCollector.order(order);
        }

        @Override
        public void submitShadow(@NonNull PoseStack poseStack, float radius, @NonNull List<EntityRenderState.ShadowPiece> pieces) {
            defaultDelegate.submitShadow(poseStack, radius, pieces);
        }

        @Override
        public void submitNameTag(@NonNull PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, @NonNull Component name, boolean seeThrough, int lightCoords, double distanceToCameraSq, @NonNull CameraRenderState camera) {
            defaultDelegate.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, distanceToCameraSq, camera);
        }

        @Override
        public void submitText(@NonNull PoseStack poseStack, float x, float y, @NonNull FormattedCharSequence string, boolean dropShadow, Font.@NonNull DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
            defaultDelegate.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
        }

        @Override
        public void submitFlame(@NonNull PoseStack poseStack, @NonNull EntityRenderState renderState, @NonNull Quaternionf rotation) {
            defaultDelegate.submitFlame(poseStack, renderState, rotation);
        }

        @Override
        public void submitLeash(@NonNull PoseStack poseStack, EntityRenderState.@NonNull LeashState leashState) {
            defaultDelegate.submitLeash(poseStack, leashState);
        }

        @Override
        public <S> void submitModel(@NonNull Model<? super S> model, @NonNull S state, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
            modelDelegate.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
        }

        @Override
        public void submitModelPart(@NonNull ModelPart modelPart, @NonNull PoseStack poseStack, @NonNull RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor) {
            modelDelegate.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, sheeted, hasFoil, tintedColor, crumblingOverlay, outlineColor);
        }

        @Override
        public void submitMovingBlock(@NonNull PoseStack poseStack, @NonNull MovingBlockRenderState movingBlockRenderState) {
            defaultDelegate.submitMovingBlock(poseStack, movingBlockRenderState);
        }

        @Override
        public void submitBlock(@NonNull PoseStack poseStack, @NonNull BlockState blockState, int lightCoords, int overlayCoords, int outlineColor) {
            defaultDelegate.submitBlock(poseStack, blockState, lightCoords, overlayCoords, outlineColor);
        }

        @Override
        public void submitBlockModel(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull BlockStateModel model, float red, float green, float blue, int lightCoords, int overlayCoords, int outlineColor) {
            defaultDelegate.submitBlockModel(poseStack, renderType, model, red, green, blue, lightCoords, overlayCoords, outlineColor);
        }

        @Override
        public void submitItem(@NonNull PoseStack poseStack, @NonNull ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int @NonNull [] tintLayers, @NonNull List<BakedQuad> quads, @NonNull RenderType renderType, ItemStackRenderState.@NonNull FoilType foilType) {
            defaultDelegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, renderType, foilType);
        }

        @Override
        public void submitCustomGeometry(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull CustomGeometryRenderer customGeometryRenderer) {
            modelDelegate.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
        }

        @Override
        public void submitParticleGroup(@NonNull ParticleGroupRenderer particleGroupRenderer) {
            defaultDelegate.submitParticleGroup(particleGroupRenderer);
        }
    }
}
