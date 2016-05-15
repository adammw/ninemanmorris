package com.github.adammw.ninemanmorris;

/**
 * Controls the game and mediates the flow of data between the View and the Model
 */
public class GameController {
    private GameInterface view = new GameInterface();
    private Player[] players = {null, null};

    public GameController() {
        try {
            GameInterface.GameParams gameParams = view.getParams();
            players[0] = PlayerFactory.build(this, gameParams.player1Type);
            players[1] = PlayerFactory.build(this, gameParams.player2Type);
        } catch(Exception ex) {
            System.err.println(ex);
            System.exit(1);
        }
    }
}
