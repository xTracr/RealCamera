package com.xtracr.realcamera.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.xtracr.realcamera.api.BindResult;
import com.xtracr.realcamera.api.RealCameraAPI;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.renderer.BuiltIterableBuffer;
import com.xtracr.realcamera.renderer.MultiVertexCatcher;
import com.xtracr.realcamera.renderer.state.VertexData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public class YSMCompat {
    private static final Map<BindTarget, BindResult> resultMap = new HashMap<>();
    private static final Map<BindTarget, Integer> resultPassMasks = new HashMap<>();
    private static final ProbePass[] probePasses = new ProbePass[TetrahedralProbeSet.PASS_COUNT];
    private static BindResult bindResult = BindResult.EMPTY;
    private static int preferredPassMask;

    static {
        for (int pass = 0; pass < probePasses.length; pass++) {
            probePasses[pass] = new ProbePass(pass);
        }
    }

    public static void register() {
        RealCameraAPI.registerFunction(-100, YSMCompat::computeBindResult);
    }

    private static BindResult createBindResult(BindTarget target) {
        return new BindResult(target);
    }

    private static BindResult computeBindResult(Minecraft client, float deltaTick) {
        resultMap.clear();
        resultPassMasks.clear();
        bindResult = BindResult.EMPTY;
        Entity entity = client.getCameraEntity();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        PoseStack poseStack = new PoseStack();
        float yaw = Mth.lerp(deltaTick, entity.yRotO, entity.getYRot());
        int light = dispatcher.getPackedLightCoords(entity, deltaTick);
        Matrix4f originalProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting originalSorting = RenderSystem.getVertexSorting();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        int[] passOrder = TetrahedralProbeSet.prioritizedOrder(preferredPassMask);
        modelViewStack.pushPose();
        try {
            modelViewStack.setIdentity();
            RenderSystem.applyModelViewMatrix();
            for (int passIndex : passOrder) {
                ProbePass probePass = probePasses[passIndex];
                probePass.vertexCatcher.clear();
                RenderSystem.setProjectionMatrix(TetrahedralProbeSet.projection(passIndex), VertexSorting.ORTHOGRAPHIC_Z);
                poseStack.pushPose();
                try {
                    dispatcher.render(entity, 0, 0, 0, yaw, deltaTick, poseStack, probePass.vertexCatcher, light);
                    probePass.vertexCatcher.forEachBuffer(probePass::computeBindResultInCache);
                } finally {
                    poseStack.popPose();
                }
                if (bindResult.available()) return rememberPreferredPasses();
            }
            for (int passIndex : passOrder) {
                ProbePass probePass = probePasses[passIndex];
                probePass.vertexCatcher.forEachBuffer(probePass::computeBindResult);
                if (bindResult.available()) return rememberPreferredPasses();
            }
            return BindResult.EMPTY;
        } finally {
            try {
                for (ProbePass probePass : probePasses) {
                    probePass.vertexCatcher.clear();
                }
            } finally {
                try {
                    modelViewStack.popPose();
                    RenderSystem.applyModelViewMatrix();
                } finally {
                    RenderSystem.setProjectionMatrix(originalProjection, originalSorting);
                }
            }
        }
    }

    private static BindResult rememberPreferredPasses() {
        preferredPassMask = resultPassMasks.getOrDefault(bindResult.target, 0);
        return bindResult;
    }

    private static final class ProbePass {
        private final int passMask;
        private final MultiVertexCatcher vertexCatcher = MultiVertexCatcher.create();

        private ProbePass(int pass) {
            passMask = 1 << pass;
        }

        public void computeBindResultInCache(BuiltIterableBuffer builtBuffer) {
            if (bindResult.available()) return;
            for (BindTarget target : ConfigFile.config().getBindTargetList(builtBuffer.textureId())) {
                BindResult result = resultMap.computeIfAbsent(target, YSMCompat::createBindResult);
                BindTarget.TargetConfig config = target.targetConfig();
                VertexData.UV posUV = new VertexData.UV(config.posU(), config.posV());
                VertexData.UV forwardUV = new VertexData.UV(config.forwardU(), config.forwardV());
                VertexData.UV upwardUV = new VertexData.UV(config.upwardU(), config.upwardV());
                if (result.getPosition() != Vec3.ZERO) posUV = null;
                if (result.getForward() != Vec3.ZERO) forwardUV = null;
                if (result.getUpward() != Vec3.ZERO) upwardUV = null;
                VertexData[][] primitives = builtBuffer.findPrimitivesInCache(new VertexData.UV[]{posUV, forwardUV, upwardUV});
                applyPrimitives(target, result, config, primitives);
                if (!result.available()) continue;
                bindResult = result;
                return;
            }
        }

        public void computeBindResult(BuiltIterableBuffer builtBuffer) {
            if (bindResult.available()) return;
            for (BindTarget target : ConfigFile.config().getBindTargetList(builtBuffer.textureId())) {
                BindResult result = resultMap.computeIfAbsent(target, YSMCompat::createBindResult);
                BindTarget.TargetConfig config = target.targetConfig();
                VertexData.UV posUV = result.getPosition() == Vec3.ZERO ? new VertexData.UV(config.posU(), config.posV()) : null;
                VertexData.UV forwardUV = result.getForward() == Vec3.ZERO ? new VertexData.UV(config.forwardU(), config.forwardV()) : null;
                VertexData.UV upwardUV = result.getUpward() == Vec3.ZERO ? new VertexData.UV(config.upwardU(), config.upwardV()) : null;
                VertexData[][] primitives = builtBuffer.findPrimitives(new VertexData.UV[]{posUV, forwardUV, upwardUV});
                applyPrimitives(target, result, config, primitives);
                if (!result.available()) continue;
                bindResult = result;
                return;
            }
        }

        private void applyPrimitives(BindTarget target, BindResult result, BindTarget.TargetConfig config, VertexData[][] primitives) {
            boolean contributed = false;
            if (primitives[0] != null) {
                result.setPosition(VertexData.position(primitives[0], config.posU(), config.posV()));
                contributed = true;
            }
            if (primitives[1] != null) {
                result.setForward(VertexData.normal(primitives[1]));
                contributed = true;
            }
            if (primitives[2] != null) {
                result.setUpward(VertexData.normal(primitives[2]));
                contributed = true;
            }
            if (contributed) resultPassMasks.merge(target, passMask, (left, right) -> left | right);
        }
    }
}
