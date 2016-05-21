package com.github.adammw.ninemanmorris;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Controls the game and mediates the flow of data between the View and the Model
 */
public class GameController {
    private GameInterface view = new GameInterface();
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
            Player[] players = new Player[2];
            for (int i = 0; i < players.length; i++) {
                players[i] = PlayerFactory.build(this, gameParams.playerTypes[i], "PLAYER " + (i+1));
            }

            // Create the board model
            this.board = new Board(players);
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
            // Notify the view who the current player is
            Player currentPlayer = board.getPlayer(currentPlayerIdx);
            view.notifyCurrentPlayer(currentPlayer);

            // When a mill is formed, prompt the player for which piece to remove continually until valid
            Board.MillFormedCallback millFormedCallback = () -> {
                boolean validMove;
                do {
                    try {
                        Move millRemovalMove = currentPlayer.getPieceToRemove(board);
                        board.performMove(millRemovalMove, currentPlayer, null);
                        validMove = true;
                    } catch (Board.IllegalMoveException ex) {
                        view.displayError(ex);
                        validMove = false;
                    }
                } while(!validMove);
            };

            // Prompt the player for a move
            try {
                Move move = currentPlayer.getMove(board);
                board.performMove(move, currentPlayer, millFormedCallback);
                currentPlayerIdx = (currentPlayerIdx + 1) % board.getPlayerCount();
            } catch(Board.IllegalMoveException ex) {
                view.displayError(ex);
            }
        } while(!board.isGameOver());

        // Announce winner if game is over
        Player winningPlayer = board.getWinningPlayer();
        view.announceWinner(board, winningPlayer);
    }

    /**
     * Ask the current interface for a move from the end-user
     * @param board the board representation to display as the current state of the game
     * @param player the player who's move is being asked for
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
     * Ask the current interface for which piece to remove from the end-user
     * @param board the board representation to display as the current state of the game
     * @param player the player who's move is being asked for
     * @return the user's chosen move
     */
    public Move getPieceToRemoveFromUser(Board board, HumanPlayer player) {
        try {
            return view.getPieceToRemoveFromUser(board, player);
        } catch(Exception ex) {
            System.err.println(ex);
            System.exit(1);
            return null;
        }
    }
}
