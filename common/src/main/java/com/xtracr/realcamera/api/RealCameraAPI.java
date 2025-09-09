package com.xtracr.realcamera.api;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class RealCameraAPI {
    private static final List<BiFunction<Minecraft, Float, BindResult>> FUNCTIONS = new ArrayList<>();

    public static void registerFunction(BiFunction<Minecraft, Float, BindResult> function) {
        FUNCTIONS.add(function);
    }

    public static BindResult computeBindResult(Minecraft client, float deltaTick) {
        for (BiFunction<Minecraft, Float, BindResult> function : FUNCTIONS) {
            BindResult result = function.apply(client, deltaTick);
            if (result.available()) return result;
        }
        return BindResult.EMPTY;
    }
}
