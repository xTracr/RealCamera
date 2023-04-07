package com.xtracr.realcamera.config;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

public enum AcceptableModelParts {
    HEAD {
        public ModelPart getTarget(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.head;
        }
    },
    BODY {
        public ModelPart getTarget(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.body;
        }
    },
    LEFT_ARM {
        public ModelPart getTarget(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.leftArm;
        }
    },
    RIGHT_ARM {
        public ModelPart getTarget(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.rightArm;
        }
    },
    LEFT_LEG {
        public ModelPart getTarget(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.leftLeg;
        }
    },
    RIGHT_LEG {
        public ModelPart getTarget(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.rightLeg;
        }
    };

    private static final AcceptableModelParts[] VALUES = values();

    private AcceptableModelParts() {
    }
    public AcceptableModelParts cycle() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public abstract ModelPart getTarget(PlayerEntityModel<AbstractClientPlayerEntity> playerModel);
}
