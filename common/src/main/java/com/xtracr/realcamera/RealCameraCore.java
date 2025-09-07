package com.xtracr.realcamera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.compat.DisableHelper;
import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.config.ExcludedRegion;
import com.xtracr.realcamera.util.BindingContext;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.PrimitiveUtils;
import com.xtracr.realcamera.util.VertexData;
import com.xtracr.realcamera.util.VertexRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealCameraCore {
    private static final VertexRecorder defaultRecorder = new VertexRecorder();
    private static VertexRecorder activeRecorder = defaultRecorder;
    private static BindingContext bindingContext = BindingContext.EMPTY;
    private static Vec3 cameraPos = Vec3.ZERO, entityPos = Vec3.ZERO;
    private static boolean active = false, rendering = false, readyToSendMessage = true;
    
    // Note: Avoid per-frame caches keyed by BuiltRecord; records are rebuilt each frame,
    // so cross-frame caches won’t hit and only add overhead. Compute light-weight
    // features inline per primitive instead (UV center, area, vertexCount).

    public static void setActiveRecorder(VertexRecorder recorder) {
        activeRecorder = recorder;
    }

    public static BindingTarget currentTarget() {
        return bindingContext.target;
    }

    public static float getPitch(float f) {
        if (currentTarget().isBindRotation()) return (float) bindingContext.getEulerAngle().x();
        return f;
    }

    public static float getYaw(float f) {
        if (currentTarget().isBindRotation()) return (float) -bindingContext.getEulerAngle().y();
        return f;
    }

    public static float getRoll(float f) {
        if (ConfigFile.config().isClassic()) return f + ConfigFile.config().getClassicRoll();
        if (currentTarget().isBindRotation()) return (float) bindingContext.getEulerAngle().z();
        return f;
    }

    public static Vec3 getRawPos(Vec3 vec) {
        Vec3 rawPos = bindingContext.getPosition().add(entityPos);
        return new Vec3(currentTarget().isBindX() ? rawPos.x() : vec.x(), currentTarget().isBindY() ? rawPos.y() : vec.y(), currentTarget().isBindZ() ? rawPos.z() : vec.z());
    }

    public static Vec3 getCameraPos(Vec3 vec) {
        return new Vec3(currentTarget().isBindX() ? cameraPos.x() : vec.x(), currentTarget().isBindY() ? cameraPos.y() : vec.y(), currentTarget().isBindZ() ? cameraPos.z() : vec.z());
    }

    public static void setCameraPos(Vec3 vec) {
        cameraPos = vec;
    }

    public static void initialize(Minecraft client) {
        Entity entity = client.getCameraEntity();
        active = ConfigFile.config().enabled() && client.options.getCameraType().isFirstPerson() && entity != null && !DisableHelper.MAIN_FEATURE.disabled(entity);
        rendering = active && ConfigFile.config().renderModel() && !DisableHelper.RENDER_MODEL.disabled(entity);
        activeRecorder.records().clear();
        
        // No explicit clear needed for WeakHashMap; cached entries drop with records
    }

    public static void readyToSendMessage() {
        readyToSendMessage = ConfigFile.config().enabled();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isRendering() {
        return isActive() && rendering;
    }

    public static void computeCamera(Minecraft client, float deltaTick) {
        Entity entity = client.getCameraEntity();
        if (entity.tickCount == 0) {
            entity.xOld = entity.getX();
            entity.yOld = entity.getY();
            entity.zOld = entity.getZ();
        }
        entityPos = new Vec3(Mth.lerp(deltaTick, entity.xOld, entity.getX()), Mth.lerp(deltaTick, entity.yOld, entity.getY()), Mth.lerp(deltaTick, entity.zOld, entity.getZ()));

        BindingContext apiContext = RealCameraAPI.genBindingContext(client, deltaTick);
        if (apiContext.available()) bindingContext = apiContext;
        else {
            activeRecorder.updateModel(client, client.getCameraEntity(), deltaTick, new PoseStack());
            bindingContext = activeRecorder.genContext();
        }
        if (activeRecorder.records().isEmpty()) bindingContext.skipRendering = false;
        if (!bindingContext.available()) {
            Entity player = client.player;
            if (readyToSendMessage && player != null) player.sendSystemMessage(LocUtil.MESSAGE("bindingFailed", LocUtil.MOD_NAME(), LocUtil.MODEL_VIEW_TITLE()));
            active = readyToSendMessage = false;
            return;
        }
        readyToSendMessage = true;
        bindingContext.init();
    }

    public static void renderCameraEntity(Minecraft client, float deltaTick, MultiBufferSource bufferSource, Matrix4f cameraPose) {
        Vec3 eulerAngle = bindingContext.getEulerAngle();
        Matrix4f invertedCameraPose = new Matrix4f()
                .rotateZ((float) Math.toRadians(eulerAngle.z()))
                .rotateX((float) Math.toRadians(eulerAngle.x()))
                .rotateY((float) Math.toRadians(180.0f - eulerAngle.y()))
                .transpose()
                .invert()
                .translate(Vec3.ZERO.subtract(bindingContext.getPosition()).toVector3f());
        PoseStack poseStack = new PoseStack();
        // Precompute inverse of camera pose once per frame
        Matrix4f invCameraPose = cameraPose.invert(new Matrix4f());
        if (!bindingContext.skipRendering || ConfigFile.config().rerenderModel()) {
            poseStack.mulPose(new Matrix4f(invertedCameraPose).mulLocal(invCameraPose));
            activeRecorder.updateModel(client, client.getCameraEntity(), deltaTick, poseStack);
        }
        // If poseStack hasn't been modified above, its last pose is identity; avoid a needless invert
        Matrix4f lastPose = poseStack.last().pose();
        Matrix4f positionMatrix = (lastPose.m00() == 1f && lastPose.m11() == 1f && lastPose.m22() == 1f && lastPose.m33() == 1f
                && lastPose.m01() == 0f && lastPose.m02() == 0f && lastPose.m03() == 0f && lastPose.m10() == 0f
                && lastPose.m12() == 0f && lastPose.m13() == 0f && lastPose.m20() == 0f && lastPose.m21() == 0f
                && lastPose.m23() == 0f && lastPose.m30() == 0f && lastPose.m31() == 0f && lastPose.m32() == 0f)
                ? new Matrix4f(invertedCameraPose)
                : new Matrix4f(invertedCameraPose).mul(lastPose.invert(new Matrix4f()));
        final double m02 = positionMatrix.m02(), m12 = positionMatrix.m12(), m22 = positionMatrix.m22(), m32 = positionMatrix.m32();
        positionMatrix.mulLocal(invCameraPose);
        Matrix3f normalMatrix = new Matrix3f(positionMatrix);
        List<VertexRecorder.BuiltRecord> recs = activeRecorder.records();
        for (int r = 0, rLen = recs.size(); r < rLen; r++) {
            VertexRecorder.BuiltRecord record = recs.get(r);
            // Skip entire record if its texture is disabled for the current target
            boolean skipRecord = false;
            for (String disabled : currentTarget().getDisabledTextureIds()) {
                if (record.textureId().contains(disabled)) {
                    skipRecord = true;
                    break;
                }
            }
            if (skipRecord) continue;
            VertexConsumer buffer = bufferSource.getBuffer(record.renderType());
            if (!record.renderType().canConsolidateConsecutiveGeometry()) {
                VertexData.renderVertices(record.vertices(), buffer);
                continue;
            }
            final double depth = currentTarget().getDisablingDepth();
            List<ExcludedRegion> excludedRegions = currentTarget().getExcludedRegions();
            
            VertexData[][] primitives = record.primitives();
            for (int i = 0; i < primitives.length; i++) {
                VertexData[] primitive = primitives[i];
                
                // Check depth culling
                boolean passedDepthTest = false;
                for (VertexData vertex : primitive) {
                    if (Math.fma(m02, vertex.x(), Math.fma(m12, vertex.y(), Math.fma(m22, vertex.z(), m32))) <= -depth) {
                        passedDepthTest = true;
                        break;
                    }
                }
                if (!passedDepthTest) continue;
                
                // Check if primitive should be excluded based on UV signatures
                if (shouldExcludePrimitive(record, primitive, excludedRegions)) {
                    continue;
                }
                
                VertexData.renderVertices(primitive, buffer, positionMatrix, normalMatrix);
            }
        }
    }

    private static boolean shouldExcludePrimitive(VertexRecorder.BuiltRecord record,
                                                  VertexData[] primitive,
                                                  List<ExcludedRegion> excludedRegions) {
        if (excludedRegions == null || excludedRegions.isEmpty()) {
            return false;
        }
        // Compute lightweight, rotation-invariant features once per primitive
        Vec2 uvCenter = PrimitiveUtils.getUVCenter(primitive);
        float area = PrimitiveUtils.calculateUVArea(primitive);
        int vertexCount = primitive.length;

        // Perform matching using the computed features
        for (ExcludedRegion excluded : excludedRegions) {
            // Quick texture ID check
            if (!record.textureId().contains(excluded.getTextureId())) {
                continue;
            }
            
            // Use cached features for matching (passing Vec3.ZERO for normal - ignored in rotation-invariant matching)
            // Hash is set to 0 since we don't calculate neighborhoods at runtime for performance
            if (excluded.matchesUV(uvCenter, Vec3.ZERO, 0, vertexCount, area)) {
                return true; // This primitive should be excluded
            }
        }
        
        return false;
    }
}
