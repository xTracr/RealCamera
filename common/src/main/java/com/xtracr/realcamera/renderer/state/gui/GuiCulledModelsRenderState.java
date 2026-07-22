package com.xtracr.realcamera.renderer.state.gui;

import com.xtracr.realcamera.renderer.state.BuiltModelRecord;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record GuiCulledModelsRenderState(
        List<BuiltModelRecord> records,
        Matrix4f transform,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiCulledModelsRenderState(
            List<BuiltModelRecord> records,
            Matrix4f transform,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(records, transform, x0, y0, x1, y1, scale, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
