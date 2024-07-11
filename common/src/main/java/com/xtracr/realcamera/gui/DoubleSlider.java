package com.xtracr.realcamera.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Function;

public class DoubleSlider extends AbstractSliderButton {
    private final Function<Double, Component> textFactory;
    private final double min, max;

    public DoubleSlider(int width, int height, double value, double min, double max, Function<Double, Component> textFactory) {
        super(0, 0, width, height, textFactory.apply(value), Mth.clamp(0, (value - min) / (max - min), 1));
        this.textFactory = textFactory;
        this.min = min;
        this.max = max;
    }

    public double getValue() {
        return min + (max - min) * value;
    }

    public void setValue(double value) {
        this.value = Mth.clamp(0, (value - min) / (max - min), 1);
        applyValue();
    }

    @Override
    protected void updateMessage() {
        setMessage(textFactory.apply(getValue()));
    }

    @Override
    protected void applyValue() {
        updateMessage();
    }
}
