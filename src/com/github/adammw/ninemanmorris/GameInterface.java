package com.github.adammw.ninemanmorris;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Interfaces with the user to provide a graphical representation of the game and solicit input from the user
 */
public class GameInterface {
    public class GameParams {
        PlayerType player1Type;
        PlayerType player2Type;

        public GameParams(PlayerType player1Type, PlayerType player2Type) {
            this.player1Type = player1Type;
            this.player2Type = player1Type;
        }
    }

    public GameInterface() {
        System.out.println("Nine Man's Morris");
        System.out.println("=================");
    }

    public GameParams getParams() throws Exception {
        return new GameParams(readPlayerType(1), readPlayerType(2));
    }

    private PlayerType readPlayerType(int id) throws IOException {
        PlayerType playerType = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        do {
            String possibleOptions = Stream.of(PlayerType.values()).map(pt -> pt.toString().replace("_PLAYER", "")).collect(Collectors.joining("/"));
            System.out.println("Enter Player " + id + " Type (" + possibleOptions + ") :");
            String playerTypeString = in.readLine().toUpperCase();
            try {
                playerType = PlayerType.valueOf(playerTypeString + "_PLAYER");
            } catch(IllegalArgumentException ex) {
                System.err.println("Invalid player type");
            }
        } while(playerType == null);

        return playerType;
    }
}
