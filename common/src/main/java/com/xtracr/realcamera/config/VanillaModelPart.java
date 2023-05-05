package com.xtracr.realcamera.config;

import java.util.function.Function;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

public enum VanillaModelPart {
    head(model -> model.head),
    body(model -> model.body),
    leftArm(model -> model.leftArm),
    rightArm(model -> model.rightArm),
    leftLeg(model -> model.leftLeg),
    rightLeg(model -> model.rightLeg);

    private final Function<PlayerEntityModel<AbstractClientPlayerEntity>, ModelPart> function;

    private VanillaModelPart(Function<PlayerEntityModel<AbstractClientPlayerEntity>, ModelPart> function) {
        this.function = function;
    }

    public ModelPart get(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
        return this.function.apply(playerModel);
    }

}
