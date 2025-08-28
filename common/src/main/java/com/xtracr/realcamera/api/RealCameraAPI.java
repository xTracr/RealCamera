package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.BindingContext;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RealCameraAPI {
    private static final Map<String, Consumer<Object>> poseHandlerConsumers = new HashMap<>();

    public static void registerPoseHandlerConsumer(String id, Consumer<Object> consumer) {
        poseHandlerConsumers.put(id, consumer);
    }

    public static BindingContext genBindingContext(Minecraft client, float deltaTick) {
        for (Map.Entry<String, Consumer<Object>> entry : poseHandlerConsumers.entrySet()) {
            BindingTarget target = ConfigFile.config().getOrCreateFixedTarget(entry.getKey());
            BindingContext context = new BindingContext(target, client, deltaTick, false);
            entry.getValue().accept(context);
            if (context.available()) return context;
        }
        return BindingContext.EMPTY;
    }
}
