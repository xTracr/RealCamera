package com.xtracr.realcamera.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public class IntFieldWidget extends TextFieldWidget {
    IntConsumer consumer;

    public IntFieldWidget(TextRenderer textRenderer, int width, int height, Text text, int value, IntConsumer consumer) {
        super(textRenderer, width, height, text);
        this.consumer = consumer;
        setMaxLength(9);
        setValue(value);
    }

    protected int getValue() {
        return Integer.parseInt(getText());
    }

    protected void setValue(int value) {
        setText(String.valueOf(value));
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr < '0' || chr > '9') {
            return false;
        }
        boolean ret = super.charTyped(chr, modifiers);
        int value = getValue();
        setValue(value);
        consumer.accept(value);
        return ret;
    }
}
