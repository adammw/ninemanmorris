package com.github.adammw.ninemanmorris;

/**
 * Factory class for building various player types
 */
public class PlayerFactory {
    public static Player build(GameController controller, PlayerType type, String name) {
        switch (type) {
            case HUMAN_PLAYER:
                return new HumanPlayer(controller, name);
            default:
                return null;
        }
    }
}
