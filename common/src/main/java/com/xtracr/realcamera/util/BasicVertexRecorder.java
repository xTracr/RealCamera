package com.xtracr.realcamera.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.ConfigFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class BasicVertexRecorder implements VertexRecorder {
    protected final List<BuiltRecord> records = new ArrayList<>();
    protected MultiVertexCatcher catcher;

    @Override
    public List<BuiltRecord> records() {
        return records;
    }

    @Override
    public void setCatcher(MultiVertexCatcher catcher) {
        this.catcher = catcher;
    }

    @Override
    public void updateModel(Minecraft client, Entity entity, float deltaTick, PoseStack poseStack) {
        if (catcher == null) setCatcher(MultiVertexCatcher.defaultImpl());
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        dispatcher.render(entity, 0, 0, 0, Mth.lerp(deltaTick, entity.yRotO, entity.getYRot()), deltaTick, poseStack, catcher, dispatcher.getPackedLightCoords(entity, deltaTick));
        records.clear();
        catcher.sendVertices(this);
    }

    @Override
    public BindResult computeBindResult() {
        for (BindTarget target : ConfigFile.config().getBindTargetList()) {
            for (BuiltRecord record : records) {
                BindResult result = new BindResult(target, false);
                if (!record.textureId().contains(result.target.textureId())) continue;
                BindTarget.TargetConfig config = result.target.targetConfig();
                VertexData[] primitive = record.findPrimitiveInCache(config.posU(), config.posV());
                if (primitive == null) primitive = record.findPrimitive(config.posU(), config.posV());
                if (primitive != null) result.setPosition(VertexData.position(primitive, config.posU(), config.posV()));
                primitive = record.findPrimitiveInCache(config.forwardU(), config.forwardV());
                if (primitive == null) primitive = record.findPrimitive(config.forwardU(), config.forwardV());
                if (primitive != null) result.setForward(VertexData.normal(primitive));
                primitive = record.findPrimitiveInCache(config.upwardU(), config.upwardV());
                if (primitive == null) primitive = record.findPrimitive(config.upwardU(), config.upwardV());
                if (primitive != null) result.setUpward(VertexData.normal(primitive));
                if (result.available()) return result;
            }
        }
        return BindResult.EMPTY;
    }
}
