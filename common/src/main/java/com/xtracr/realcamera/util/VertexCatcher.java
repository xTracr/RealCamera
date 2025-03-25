package com.xtracr.realcamera.util;

import net.minecraft.client.renderer.RenderType;

public interface VertexCatcher {
    RenderType renderType();

    VertexRecorder.Vertex[] collectVertices();
}