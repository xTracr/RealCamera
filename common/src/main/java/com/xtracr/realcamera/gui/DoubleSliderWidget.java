package com.xtracr.realcamera.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.DoubleConsumer;
import java.util.function.Function;

public class DoubleSliderWidget extends SliderWidget {
    private final double min, max;
    private final Function<Double, Text> textFunction;
    private final DoubleConsumer setter;

    public DoubleSliderWidget(int width, int height, double value, double min, double max, Function<Double, Text> textFunction, DoubleConsumer setter) {
        super(0, 0, width, height, textFunction.apply(min + (max - min) * value), value);
        this.min = min;
        this.max = max;
        this.textFunction = textFunction;
        this.setter = setter;
        this.setter.accept(min + (max - min) * value);
    }

    protected void setValue(double value) {
        this.value = MathHelper.clamp(0, (value - min) / (max - min), 1);
        applyValue();
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(textFunction.apply(min + (max - min) * value));
    }

    @Override
    protected void applyValue() {
        setter.accept(min + (max - min) * value);
    }
}
