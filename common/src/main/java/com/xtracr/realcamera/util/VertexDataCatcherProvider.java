package com.xtracr.realcamera.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class VertexDataCatcherProvider implements VertexConsumerProvider {
    protected final Map<RenderLayer, VertexDataCatcher> catchers = new LinkedHashMap<>();

    public VertexDataCatcher getUnion(VertexDataCatcher union) {
        catchers.values().forEach(catcher -> union.vertices.addAll(catcher.vertices));
        return union;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        catchers.putIfAbsent(layer, new VertexDataCatcher());
        return catchers.get(layer);
    }
}
