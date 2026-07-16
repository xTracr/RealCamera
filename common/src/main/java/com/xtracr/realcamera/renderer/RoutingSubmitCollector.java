package com.xtracr.realcamera.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public final class RoutingSubmitCollector implements SubmitNodeCollector {
    private static final BiFunction<RenderType, CustomGeometryRenderer, CustomGeometryRenderer> IDENTITY_DECORATOR = (_, renderer) -> renderer;
    private final SubmitNodeCollector defaultCollector;
    private final SubmitNodeCollector modelCollector;
    private final BiFunction<RenderType, CustomGeometryRenderer, CustomGeometryRenderer> customGeometryDecorator;

    public RoutingSubmitCollector(SubmitNodeCollector defaultCollector, SubmitNodeCollector modelCollector) {
        this(defaultCollector, modelCollector, IDENTITY_DECORATOR);
    }

    public RoutingSubmitCollector(
            SubmitNodeCollector defaultCollector,
            SubmitNodeCollector modelCollector,
            BiFunction<RenderType, CustomGeometryRenderer, CustomGeometryRenderer> customGeometryDecorator) {
        this.defaultCollector = defaultCollector;
        this.modelCollector = modelCollector;
        this.customGeometryDecorator = customGeometryDecorator;
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
    public void submitBlockModel(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull List<BlockStateModelPart> parts, int @NonNull [] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        defaultCollector.submitBlockModel(poseStack, renderType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void submitBreakingBlockModel(@NonNull PoseStack poseStack, @NonNull BlockStateModel model, long seed, int progress) {
        defaultCollector.submitBreakingBlockModel(poseStack, model, seed, progress);
    }

    @Override
    public void submitItem(@NonNull PoseStack poseStack, @NonNull ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int @NonNull [] tintLayers, @NonNull List<BakedQuad> quads, ItemStackRenderState.@NonNull FoilType foilType) {
        defaultCollector.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
    }

    @Override
    public void submitCustomGeometry(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull CustomGeometryRenderer customGeometryRenderer) {
        modelCollector.submitCustomGeometry(poseStack, renderType, customGeometryDecorator.apply(renderType, customGeometryRenderer));
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
        public void submitBlockModel(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull List<BlockStateModelPart> parts, int @NonNull [] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
            defaultDelegate.submitBlockModel(poseStack, renderType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
        }

        @Override
        public void submitBreakingBlockModel(@NonNull PoseStack poseStack, @NonNull BlockStateModel model, long seed, int progress) {
            defaultDelegate.submitBreakingBlockModel(poseStack, model, seed, progress);
        }

        @Override
        public void submitItem(@NonNull PoseStack poseStack, @NonNull ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int @NonNull [] tintLayers, @NonNull List<BakedQuad> quads, ItemStackRenderState.@NonNull FoilType foilType) {
            defaultDelegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
        }

        @Override
        public void submitCustomGeometry(@NonNull PoseStack poseStack, @NonNull RenderType renderType, @NonNull CustomGeometryRenderer customGeometryRenderer) {
            modelDelegate.submitCustomGeometry(poseStack, renderType, customGeometryDecorator.apply(renderType, customGeometryRenderer));
        }

        @Override
        public void submitParticleGroup(@NonNull ParticleGroupRenderer particleGroupRenderer) {
            defaultDelegate.submitParticleGroup(particleGroupRenderer);
        }
    }
}
