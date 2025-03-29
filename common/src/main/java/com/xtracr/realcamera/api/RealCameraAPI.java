package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.util.BindingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RealCameraAPI {
    private static final List<Consumer<Object>> poseSetterConsumers = new ArrayList<>();

    public static void registerPoseSetterConsumer(Consumer<Object> function) {
        poseSetterConsumers.add(function);
    }

    public static BindingContext genBindingContext(BindingTarget target) {
        for (Consumer<Object> poseSetterConsumer : poseSetterConsumers) {
            BindingContext context = new BindingContext(target);
            poseSetterConsumer.accept(context);
            if (context.available()) return context;
        }
        return BindingContext.EMPTY;
    }
}
