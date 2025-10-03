package com.xtracr.realcamera.api;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class RealCameraAPI {
    private static final List<BiFunction<Minecraft, Float, BindResult>> FUNCTIONS = new ArrayList<>();
    private static final Map<BiFunction<Minecraft, Float, BindResult>, Integer> PRIORITIES = new Object2IntOpenHashMap<>();

    public static void registerFunction(BiFunction<Minecraft, Float, BindResult> function) {
        registerFunction(0, function);
    }

    public static void registerFunction(int priority, BiFunction<Minecraft, Float, BindResult> function) {
        FUNCTIONS.add(function);
        PRIORITIES.put(function, priority);
        FUNCTIONS.sort((a, b) -> PRIORITIES.get(b) - PRIORITIES.get(a));
    }

    public static BindResult computeBindResult(Minecraft client, float deltaTick) {
        for (BiFunction<Minecraft, Float, BindResult> function : FUNCTIONS) {
            BindResult result = function.apply(client, deltaTick);
            if (result.available()) return result;
        }
        return BindResult.EMPTY;
    }
}
