package com.xtracr.realcamera.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;


public class FloatFieldWidget extends TextFieldWidget {

    public FloatFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable FloatFieldWidget copyFrom, Text text) {
        super(textRenderer, x, y, width, height, text);
        setMaxLength(8);
        setValue(0);
        if (copyFrom != null) setValue(copyFrom.getValue());
    }

    protected float getValue() {
        try {
            String text = getText();
            if (text.startsWith(".")) setText("0" + text);
            return Float.parseFloat(getText());
        } catch (Exception exception) {
            return 0f;
        }
    }

    protected void setValue(float value) {
        setText(String.valueOf(value));
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr != '.' && (chr < '0' || chr > '9')) return false;
        if (chr == '.' && getText().contains(".")) return false;
        return super.charTyped(chr, modifiers);
    }
}
