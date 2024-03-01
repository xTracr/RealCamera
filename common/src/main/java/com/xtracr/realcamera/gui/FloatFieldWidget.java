package com.xtracr.realcamera.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;


public class FloatFieldWidget extends NumberFieldWidget<Float> {
    public FloatFieldWidget(TextRenderer textRenderer, int width, int height, float defaultValue, @Nullable NumberFieldWidget<Float> copyFrom, Text text) {
        super(textRenderer, 0, 0, width, height, defaultValue, Float.MAX_VALUE, -Float.MAX_VALUE, copyFrom, text);
        setMaxLength(8);
    }

    @Override
    protected Float getValueInternal() {
        return Float.parseFloat(getText());
    }
}
