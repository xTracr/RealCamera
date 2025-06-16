package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.config.ConfigFile;
import com.xtracr.realcamera.util.BindingContext;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RealCameraAPI {
    private static final List<Consumer<Object>> poseHandlerConsumers = new ArrayList<>();

    public static void registerPoseHandlerConsumer(Consumer<Object> consumer) {
        poseHandlerConsumers.add(consumer);
    }

    public static BindingContext genBindingContext(Minecraft client, float deltaTick) {
        BindingTarget target = ConfigFile.config().findFixedTarget(BindingTarget.API_ONLY);
        for (Consumer<Object> consumer : poseHandlerConsumers) {
            BindingContext context = new BindingContext(target, client, deltaTick, false);
            consumer.accept(context);
            if (context.available()) return context;
        }
        return BindingContext.EMPTY;
    }
}
