package com.github.adammw.ninemanmorris;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Interfaces with the user to provide a graphical representation of the game and solicit input from the user
 */
public class GameInterface {
    private static final boolean USE_EMOJI = false;
    public static final String[][] BOARD_LINES = {
            {"◦─","──","──","◦─","──", "──", "◦"},
            {"│ ","  ","  ","│ ","  ", "  ", "│ "},
            {"│ ","◦─","──","◦─","──", "◦ ", "│ "},
            {"│ ","│ ","  ","│ ","  ", "│ ", "│ "},
            {"│ ","│ ","◦─","◦─","◦ ", "│ ", "│ "},
            {"│ ","│ ","│ ","  ","│ ", "│ ", "│ "},
            {"◦─","◦─","◦ ","  ","◦─", "◦─", "◦"},
            {"│ ","│ ","│ ","  ","│ ", "│ ", "│ "},
            {"│ ","│ ","◦─","◦─","◦ ", "│ ", "│ "},
            {"│ ","│ ","  ","│ ","  ", "│ ", "│ "},
            {"│ ","◦─","──","◦─","──", "◦ ", "│ "},
            {"│ ","  ","  ","│ ","  ", "  ", "│ "},
            {"◦─","──","──","◦─","──", "──", "◦"},
    };

    public class GameParams {
        PlayerType playerTypes[];

        public GameParams(PlayerType... playerTypes) {
            assert(playerTypes.length == 2);
            this.playerTypes = playerTypes;
        }
    }

    public GameInterface() {
        System.out.println("Nine Man's Morris");
        System.out.println("=================");
    }

    public GameParams getParams() throws Exception {
        return new GameParams(readPlayerType(1), readPlayerType(2));
    }

    public Move getMoveFromUser(Board board) throws Exception {
        displayGameState(board);

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        BoardLocation fromPosition = null;
        BoardLocation toPosition = null;

        try {
            switch (board.getStage()) {
                case PLACING:
                    System.out.println("Where do you want to place your piece? (a1 - g7)");
                    toPosition = new BoardLocation(in.readLine());
                    break;
                case MOVING:
                case FLYING:
                    System.out.println("Move piece");
                    System.out.print("FROM: ");
                    fromPosition = new BoardLocation(in.readLine());
                    System.out.print("TO: ");
                    toPosition = new BoardLocation(in.readLine());
                    break;
            }
        } catch(BoardLocation.InvalidLocationException ex) {
            System.err.println("Invalid location");
        }

        return new Move(fromPosition, toPosition);
    }

    /**
     * Prints a representation of the game board to the console
     * @param board the board model to get the game state from
     */
    private void displayGameState(Board board) {
        int boardSize = Board.VALID_LOCATIONS.length; // assumes a square board
        for (int y = 0; y < boardSize; y++) {
            System.out.print((y+1) + " ");
            for( int x = 0; x < boardSize; x++) {
                if (Board.VALID_LOCATIONS[y][x]) {
                    Piece piece = board.getPieceAt(x, y);
                    System.out.print(displayPiece(piece, board));
                    System.out.print((!USE_EMOJI || piece == null) ? BOARD_LINES[2*y][x].substring(1) : " ");
                } else {
                    System.out.print(BOARD_LINES[2 * y][x]);
                }
            }
            System.out.print("\n  ");
            if (y + 1 != boardSize) {
                for (int x = 0; x < boardSize; x++) {
                    System.out.print(BOARD_LINES[2 * y + 1][x]);
                }
                System.out.print("\n");
            }
        }
        for( int x = 0; x < boardSize; x++) {
            System.out.print(((char) ('a' + x)) + " ");
        }
        System.out.print("\n");
    }

    private String displayPiece(Piece piece, Board board) {
        if (piece == null) {
            return "◦";
        } else {
            if (piece.getOwner() == board.getPlayer(0)) {
                return USE_EMOJI ?  "⚫" : "●";
            } else {
                return USE_EMOJI ? "⚪" : "○";
            }
        }
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

    public void notifyCurrentPlayer(Player player, int id) {
        System.out.println("Current player is Player " + (id + 1));
    }

    public void displayError(Exception ex) {
        System.err.println(ex);
    }
}
