package com.github.adammw.ninemanmorris;

/**
 * Player class representing actions entered by a human end-user
 */
public class HumanPlayer extends Player {
    /**
     * Create a new HumanPlayer object
     * @param controller the game controller the object should ask for getting moves from the view
     * @param name the player name
     */
    public HumanPlayer(GameController controller, String name) {
        super(controller, name);
    }

    /**
     * Get a move for the human player
     * @param board the current game board state
     * @return a move returned by the end-user
     */
    public Move getMove(Board board) {
        return controller.getMoveFromUser(board, this);
    }

    /**
     * Get which piece to remove for the human player
     * @param board the current board state
     * @return a move containing the piece to remove from by the end-user
     */
    public Move getPieceToRemove(Board board) {
        return controller.getPieceToRemoveFromUser(board, this);
    }
}
