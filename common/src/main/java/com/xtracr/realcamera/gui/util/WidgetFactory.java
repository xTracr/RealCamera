package com.xtracr.realcamera.gui.util;

import com.xtracr.realcamera.config.UVRectangle;
import com.xtracr.realcamera.gui.components.DoubleSlider;
import com.xtracr.realcamera.gui.components.NumberField;
import com.xtracr.realcamera.gui.components.UVRectangleWidget;
import com.xtracr.realcamera.util.LocUtil;
import com.xtracr.realcamera.util.MathUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Map;

public final class WidgetFactory {

    public static Tooltip tooltip(String key, Object... args) {
        return Tooltip.create(LocUtil.MODEL_VIEW_TOOLTIP(key, args));
    }

    public static Button button(Component message, int width, int height, Button.OnPress onPress) {
        return Button.builder(message, onPress).size(width, height).build();
    }
    
    public static UVRectangleWidget rectWidget(UVRectangle rect) {
        return new UVRectangleWidget(rect.uMin(), rect.vMin(), rect.uMax(), rect.vMax());
    }

    public static <T> CycleButton.Builder<T> cyclingButton(Map<T, Component> messages, T defaultValue) {
        return new CycleButton.Builder<>(messages::get, () -> defaultValue).withValues(messages.keySet());
    }

    public static DoubleSlider slider(String key, int width, int height, double min, double max) {
        return new DoubleSlider(width, height, 0, min, max, d -> LocUtil.MODEL_VIEW_WIDGET(key, MathUtil.round(d, 2)));
    }

    public static NumberField<Float> floatField(Font font, int width, int height, float defaultValue) {
        return NumberField.ofFloat(font, width - 2, height - 2, defaultValue, null).setMax(1.0f).setMin(0f);
    }

    public static EditBox textField(Font font, int width, int height, int maxLength) {
        EditBox editBox = new EditBox(font, 0, 0, width - 2, height - 2, CommonComponents.EMPTY);
        editBox.setMaxLength(maxLength);
        return editBox;
    }
}

