package com.xtracr.realcamera.config.binding;

import com.xtracr.realcamera.renderer.state.VertexData.UV;

public record TargetConfig(String textureId,
                           float forwardU, float forwardV,
                           float upwardU, float upwardV,
                           float posU, float posV) {
    public UV positionUV() {
        return new UV(posU, posV);
    }

    public UV forwardUV() {
        return new UV(forwardU, forwardV);
    }

    public UV upwardUV() {
        return new UV(upwardU, upwardV);
    }
}
