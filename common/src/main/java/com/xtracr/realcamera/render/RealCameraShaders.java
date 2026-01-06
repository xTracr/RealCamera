package com.xtracr.realcamera.render;

import net.minecraft.client.renderer.ShaderInstance;

public final class RealCameraShaders {
    private static ShaderInstance entitySolidShader;
    private static ShaderInstance entityCutoutShader;
    private static ShaderInstance entityTranslucentShader;

    private RealCameraShaders() {
    }

    public static ShaderInstance entitySolid() {
        return entitySolidShader;
    }

    public static ShaderInstance entityCutout() {
        return entityCutoutShader;
    }

    public static ShaderInstance entityTranslucent() {
        return entityTranslucentShader;
    }

    public static void setEntitySolid(ShaderInstance shader) {
        entitySolidShader = shader;
    }

    public static void setEntityCutout(ShaderInstance shader) {
        entityCutoutShader = shader;
    }

    public static void setEntityTranslucent(ShaderInstance shader) {
        entityTranslucentShader = shader;
    }

    public static void setDisableDepth(ShaderInstance shader, float depth) {
        if (shader == null) return;
        shader.safeGetUniform("DisableDepth").set(depth);
        shader.markDirty();
    }
}
