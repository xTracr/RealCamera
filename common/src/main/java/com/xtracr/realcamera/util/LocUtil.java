package com.xtracr.realcamera.util;

import com.xtracr.realcamera.RealCamera;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class LocUtil {
    public static final String KEY_MOD_NAME = "general." + RealCamera.FULL_ID + ".modName";

    public static MutableComponent MOD_NAME() {
        return Component.translatable(KEY_MOD_NAME);
    }

    public static MutableComponent CONFIG_CATEGORY(String key) {
        return Component.translatable("config.category." + RealCamera.FULL_ID + "." + key);
    }

    public static MutableComponent CONFIG_OPTION(String key, Object... args) {
        return Component.translatable("config.option." + RealCamera.FULL_ID + "." + key, args);
    }

    public static MutableComponent CONFIG_TOOLTIP(String key) {
        return Component.translatable("config.tooltip." + RealCamera.FULL_ID + "." + key);
    }

    public static MutableComponent MESSAGE(String key, Object... args) {
        return Component.translatable("message." + RealCamera.FULL_ID + "." + key, args);
    }

    public static MutableComponent MODEL_VIEW_TITLE() {
        return Component.translatable("screen." + RealCamera.FULL_ID + ".modelView_title");
    }

    public static MutableComponent MODEL_VIEW_WIDGET(String key, Object... args) {
        return Component.translatable("screen.widget." + RealCamera.FULL_ID + ".modelView_" + key, args);
    }

    public static Tooltip MODEL_VIEW_TOOLTIP(String key, Object... args) {
        return Tooltip.create(Component.translatable("screen.tooltip." + RealCamera.FULL_ID + ".modelView_" + key, args));
    }

    public static MutableComponent literal(String string) {
        return Component.literal(string);
    }
}
