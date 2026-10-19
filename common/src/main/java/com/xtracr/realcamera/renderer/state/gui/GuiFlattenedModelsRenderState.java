package com.xtracr.realcamera.renderer.state.gui;

import com.xtracr.realcamera.renderer.state.ModelSnapshot;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record GuiFlattenedModelsRenderState(
        List<ModelSnapshot> snapshots,
        Vector3f translation,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiFlattenedModelsRenderState(
            List<ModelSnapshot> snapshots,
            Vector3f translation,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(snapshots, translation, x0, y0, x1, y1, scale, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
