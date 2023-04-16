package com.xtracr.realcamera.config;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

public enum VanillaModelPart {
    head {
        public ModelPart get(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.head;
        }
    },
    body {
        public ModelPart get(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.body;
        }
    },
    leftArm {
        public ModelPart get(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.leftArm;
        }
    },
    rightArm {
        public ModelPart get(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.rightArm;
        }
    },
    leftLeg {
        public ModelPart get(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.leftLeg;
        }
    },
    rightLeg {
        public ModelPart get(PlayerEntityModel<AbstractClientPlayerEntity> playerModel) {
            return playerModel.rightLeg;
        }
    };

    private static final VanillaModelPart[] VALUES = values();

    private VanillaModelPart() {
    }
    
    public VanillaModelPart cycle() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public abstract ModelPart get(PlayerEntityModel<AbstractClientPlayerEntity> playerModel);
}
