package com.xtracr.betterfpcam.config;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

public enum AcceptableModelParts {
    HEAD("head") {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.head;
        }
    },
    BODY("body") {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.body;
        }
    },
    LEFT_ARM("leftArm") {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.leftArm;
        }
    },
    RIGHT_ARM("rightArm") {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.rightArm;
        }
    },
    LEFT_LEG("leftLeg") {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.leftLeg;
        }
    },
    RIGHT_LEG("rightLeg") {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.rightLeg;
        }
    };

    private static final AcceptableModelParts[] VALUES = values();
    public final String name;

    private AcceptableModelParts(String name) {
        this.name = name;
    }

    public AcceptableModelParts cycle() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public abstract ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel);
}
