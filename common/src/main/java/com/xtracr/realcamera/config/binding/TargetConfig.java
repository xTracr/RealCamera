package com.xtracr.realcamera.config.binding;

import com.xtracr.realcamera.renderer.state.VertexData.UV;

import java.util.Objects;

/**
 * Describes which UV coordinates of a texture define a target. A target without a definition uses
 * {@link #EMPTY}, whose texture id is blank.
 */
public record TargetConfig(String textureId,
                           float forwardU, float forwardV,
                           float upwardU, float upwardV,
                           float posU, float posV) {
    public static final TargetConfig EMPTY = new TargetConfig("", 0, 0, 0, 0, 0, 0);

    public TargetConfig {
        textureId = Objects.requireNonNullElse(textureId, "");
    }

    public boolean isEmpty() {
        return textureId.isEmpty();
    }

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
