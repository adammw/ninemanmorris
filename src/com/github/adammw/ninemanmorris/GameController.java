package com.github.adammw.ninemanmorris;

import java.util.stream.Stream;

/**
 * Controls the game and mediates the flow of data between the View and the Model
 */
public class GameController {
    private GameInterface view = new GameInterface();
    private Player[] players = {null, null};
    private Board board;
    private int currentPlayerIdx = 0;

    /**
     * Constructs a new Game Controller
     */
    public GameController() {
        try {
            // Get the game parameters from the GameInterface
            GameInterface.GameParams gameParams = view.getParams();

            // Build the player instances
            this.players = Stream.of(gameParams.playerTypes)
                    .map(playerType -> PlayerFactory.build(this, playerType))
                    .toArray(Player[]::new);

            // Create the board model
            this.board = new Board(this.players);
        } catch(Exception ex) {
            System.err.println(ex);
            System.exit(1);
        }
    }

    /**
     * Start the game loop and allow the game moves to be entered
     */
    public void playGame() {
        do {
            Player currentPlayer = getCurrentPlayer();
            view.notifyCurrentPlayer(currentPlayer, currentPlayerIdx);
            try {
                Move move = currentPlayer.getMove(board);
                board.performMove(move, currentPlayer, () -> {
                    // TODO: piece removal
                    System.out.println("mill formed!!!");
                });
                currentPlayerIdx = (currentPlayerIdx + 1) % players.length;
            } catch(Board.IllegalMoveException ex) {
                view.displayError(ex);
            }
        } while(!board.isGameOver());
    }

    /**
     * Ask the current interface for a move from the end-user
     * @param board the board representation to display as the current state of the game
     * @return the user's chosen move
     */
    public Move getMoveFromUser(Board board, HumanPlayer player) {
        try {
            return view.getMoveFromUser(board, player);
        } catch(Exception ex) {
            System.err.println(ex);
            System.exit(1);
            return null;
        }
    }

    /**
     * Get the players in the game
     * @return array of player objects
     */
    public Player[] getPlayers() {
        return players;
    }

    /**
     * Get the current player
     * @return player object for the current player
     */
    public Player getCurrentPlayer() {
        return players[currentPlayerIdx];
    }
}
