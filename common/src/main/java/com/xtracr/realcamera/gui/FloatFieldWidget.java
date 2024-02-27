package com.xtracr.realcamera.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;


public class FloatFieldWidget extends NumberFieldWidget<Float> {
    public FloatFieldWidget(TextRenderer textRenderer, int width, int height, @Nullable NumberFieldWidget<Float> copyFrom, Text text) {
        super(textRenderer, 0, 0, width, height, copyFrom, text, 0f, Float.MAX_VALUE, -Float.MAX_VALUE);
        setMaxLength(8);
    }

    @Override
    protected Float getValueInternal() {
        return Float.parseFloat(getText());
    }
}
