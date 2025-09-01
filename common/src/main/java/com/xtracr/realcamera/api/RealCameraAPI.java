package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.BindTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.BindResult;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RealCameraAPI {
    private static final Map<String, Consumer<Object>> poseHandlerConsumers = new HashMap<>();

    public static void registerPoseHandlerConsumer(String id, Consumer<Object> consumer) {
        poseHandlerConsumers.put(id, consumer);
    }

    public static BindResult computeBindResult(Minecraft client, float deltaTick) {
        for (Map.Entry<String, Consumer<Object>> entry : poseHandlerConsumers.entrySet()) {
            BindTarget target = ConfigFile.config().getOrCreateFixedTarget(entry.getKey());
            BindResult result = new BindResult(target, client, deltaTick, false);
            entry.getValue().accept(result);
            if (result.available()) return result;
        }
        return BindResult.EMPTY;
    }
}
