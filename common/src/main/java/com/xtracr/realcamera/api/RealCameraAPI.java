package com.xtracr.realcamera.api;

import com.xtracr.realcamera.config.BindingTarget;
import com.xtracr.realcamera.util.BindingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RealCameraAPI {
    private static final List<Consumer<Object>> vectorsSupplier = new ArrayList<>();

    public static void registerVectorSupplier(Consumer<Object> function) {
        vectorsSupplier.add(function);
    }

    public static BindingContext genBindingContext(BindingTarget target) {
        for (Consumer<Object> bindingFunction : vectorsSupplier) {
            BindingContext context = new BindingContext(target);
            bindingFunction.accept(context);
            if (context.available()) return context;
        }
        return BindingContext.EMPTY;
    }
}
