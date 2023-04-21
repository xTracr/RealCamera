package com.xtracr.realcamera.utils;

import net.minecraft.client.util.math.MatrixStack;

@FunctionalInterface
public interface VirtualRenderFunction {
    void virtualRender(float tickDelta, MatrixStack matrixStack) throws Exception;
}
