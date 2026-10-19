package com.xtracr.realcamera.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtracr.realcamera.config.ConfigFile;

public record LayoutConstants(int xSize, int ySize, int x, int y, int middleWidth, int widgetWidth, int widgetHeight, int wideWidgetWidth, int compactWidgetWidth, InputConstants.Key modifierKey) {
    public static LayoutConstants create(int width, int height) {
        int xSize = Math.clamp(width - 10, 10, 450);
        int ySize = Math.clamp(height - 10, 10, 206);
        int middleWidth = Math.max(10, xSize - 200);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        int widgetWidth = 42;
        int widgetHeight = 18;
        int wideWidgetWidth = widgetWidth * 2 + 4;
        int compactWidgetWidth = widgetWidth * 2 - 18;
        InputConstants.Key modifierKey = InputConstants.getKey(ConfigFile.config().binding.screenModifierKey);
        return new LayoutConstants(xSize, ySize, x, y, middleWidth, widgetWidth, widgetHeight, wideWidgetWidth, compactWidgetWidth, modifierKey);
    }
}
