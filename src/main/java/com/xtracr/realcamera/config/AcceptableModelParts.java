package com.xtracr.realcamera.config;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

public enum AcceptableModelParts {
    HEAD {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.head;
        }
    },
    BODY {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.body;
        }
    },
    LEFT_ARM {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.leftArm;
        }
    },
    RIGHT_ARM {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.rightArm;
        }
    },
    LEFT_LEG {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.leftLeg;
        }
    },
    RIGHT_LEG {
        public ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel) {
            return playerModel.rightLeg;
        }
    };

    private static final AcceptableModelParts[] VALUES = values();

    private AcceptableModelParts() {
    }

    public AcceptableModelParts cycle() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public abstract ModelPart getTarget(PlayerModel<AbstractClientPlayer> playerModel);
}
