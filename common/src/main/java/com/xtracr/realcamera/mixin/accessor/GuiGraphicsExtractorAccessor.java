package com.xtracr.realcamera.mixin.accessor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
    @Accessor
    GuiGraphicsExtractor.ScissorStack getScissorStack();

    @Accessor
    GuiRenderState getGuiRenderState();
}
