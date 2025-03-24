package com.xtracr.realcamera.api;

import com.xtracr.realcamera.util.IVertexRecorder;
import net.minecraft.client.renderer.RenderType;

public interface IVertexCatcher {
    RenderType renderType();

    IVertexRecorder.Vertex[] collectVertices();
}
