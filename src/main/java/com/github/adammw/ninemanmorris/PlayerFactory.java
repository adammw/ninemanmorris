package com.github.adammw.ninemanmorris;

/**
 * Factory class for building various player types
 */
public class PlayerFactory {
    public static Player build(GameController controller, PlayerType type) {
        switch (type) {
            case HUMAN_PLAYER:
                return new HumanPlayer(controller);
            default:
                return null;
        }
    }
}
