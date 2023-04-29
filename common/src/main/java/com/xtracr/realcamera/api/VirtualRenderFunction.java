package com.xtracr.realcamera.api;

import net.minecraft.client.util.math.MatrixStack;

/**
 * 
 * This method's code should include as much as possible all parts related to {@code matrixStack} in the code that renders the player model, 
 * to ensure that the result of {@code matrixStack} after processing is identical to the actual rendering.
 * <p>{@link com.xtracr.realcamera.api.CompatExample#virtualRender See example here}
 * 
 * @param tickDelta   or particalTick(s) (official mapping)
 * @param matrixStack or poseStack (official mapping)
 * @return {@code boolean} skip rendering if true
 * @throws Exception
 * 
 */
@FunctionalInterface
public interface VirtualRenderFunction {
    boolean virtualRender(float tickDelta, MatrixStack matrixStack) throws Exception;
}
