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
    private static final String[][] BOARD_LINES = {
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
    private int playerIdx = 0;

    public class GameParams {
        PlayerType playerTypes[];

        public GameParams(PlayerType... playerTypes) {
            assert(playerTypes.length == 2);
            this.playerTypes = playerTypes;
        }
    }

    /**
     * Create a new console-based game view
     */
    public GameInterface() {
        System.out.println("Nine Man's Morris");
        System.out.println("=================");
    }

    /**
     * Prompt the user for the game parameters
     * @return an object
     * @throws IOException if an IO error occurs reading from stdin
     */
    public GameParams getParams() throws IOException {
        return new GameParams(readPlayerType(1), readPlayerType(2));
    }

    /**
     * Prompt the user for a move
     * @param board the object representing the current state of the game
     * @param player the player to prompt for a move
     * @return a move object
     * @throws IOException if an IO error occurs reading from stdin
     */
    public Move getMoveFromUser(Board board, HumanPlayer player) throws IOException {
        displayGameState(board);

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        BoardLocation fromPosition = null;
        BoardLocation toPosition = null;
        boolean valid;
        do {
            try {
                switch (board.getStage(player)) {
                    case PLACING:
                        System.out.println("Where do you want to place your piece? (a1 - g7)");
                        printPrompt();
                        toPosition = new BoardLocation(in.readLine());
                        break;
                    case MOVING:
                    case FLYING:
                        System.out.print("Which piece do you want to move? (a1 - g7): ");
                        printPrompt();
                        fromPosition = new BoardLocation(in.readLine());
                        System.out.print("Where do you want to move the piece to? (a1 - g7): ");
                        printPrompt();
                        toPosition = new BoardLocation(in.readLine());
                        break;
                }
                valid = true;
            } catch (BoardLocation.InvalidLocationException ex) {
                System.err.println("Invalid location");
                valid = false;
            }
        } while(!valid);

        return new Move(fromPosition, toPosition);
    }

    /**
     * Prompt the end-user for which piece to remove
     * @param board the object representing the current state of the game
     * @param player the player to prompt
     * @return a move object with the toPosition set to null
     * @throws IOException if an IO error occurs reading from stdin
     */
    public Move getPieceToRemoveFromUser(Board board, HumanPlayer player) throws IOException {
        System.out.println("A mill has been formed!\n");
        displayGameState(board);

        // Read in the location of the piece to remove
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        BoardLocation fromPosition = null;
        do {
            try {
                System.out.println("Which piece to remove? (a1 - g7)");
                printPrompt();
                fromPosition = new BoardLocation(in.readLine());
            } catch (BoardLocation.InvalidLocationException ex) {
                System.err.println("Invalid location");
            }
        } while(fromPosition == null);

        return new Move(fromPosition, null);
    }
    /**
     * Prints a representation of the game board to the console
     * @param board the board model to get the game state from
     */
    private void displayGameState(Board board) {
        int boardSize = Board.VALID_LOCATIONS.length; // assumes a square board

        // Print the rows of the board
        for (int y = 0; y < boardSize; y++) {
            System.out.print((y+1) + " "); // prints the row numbers

            // Loop through the x positions, printing either the lines or the piece on the board
            for( int x = 0; x < boardSize; x++) {
                if (Board.VALID_LOCATIONS[y][x]) {
                    Piece piece = board.getPieceAt(x, y);
                    System.out.print(displayPiece(piece, board));
                    System.out.print((!USE_EMOJI || piece == null) ? BOARD_LINES[2*y][x].substring(1) : " ");
                } else {
                    System.out.print(BOARD_LINES[2 * y][x]);
                }
            }

            // Print the interspersing lines
            System.out.print("\n  ");
            if (y + 1 != boardSize) {
                for (int x = 0; x < boardSize; x++) {
                    System.out.print(BOARD_LINES[2 * y + 1][x]);
                }
                System.out.print("\n");
            }
        }

        // Print the column letters (a - g)
        for( int x = 0; x < boardSize; x++) {
            System.out.print(((char) ('a' + x)) + " ");
        }
        System.out.print("\n");
    }

    /**
     * Displays a piece according to the owner of the piece and if it in fact exists
     * @param piece either a valid Piece object or null
     * @param board the board object (to check which player should be which)
     * @return a string representing the piece / location on the board
     */
    private String displayPiece(Piece piece, Board board) {
        if (piece == null) {
            return "◦"; // unoccupied intersection
        } else {
            if (piece.getOwner() == board.getPlayer(0)) {
                return USE_EMOJI ?  "⚫" : "●";
            } else {
                return USE_EMOJI ? "⚪" : "○";
            }
        }
    }

    /**
     * Read the player type from the end-user and convert it to a PlayerType
     * @param id the player index to prompt for
     * @return a PlayerType enum
     * @throws IOException if an IO error occurs while reading stdin
     */
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

    /**
     * Print a prompt with the current player of which input is being requested
     */
    private void printPrompt() {
        System.out.print("PLAYER" + (playerIdx + 1) + "> ");
    }

    /**
     * Display who's turn it is
     * @param player the current player
     * @param id the id of the current player
     */
    public void notifyCurrentPlayer(Player player, int id) {
        playerIdx = id;
        System.out.println("\nPlayer " + (id + 1) + "'s Turn");
    }

    /**
     * Display errors (e.g. invalid moves)
     * @param ex the exception to display
     */
    public void displayError(Exception ex) {
        System.err.println(ex.getMessage());
    }
}
