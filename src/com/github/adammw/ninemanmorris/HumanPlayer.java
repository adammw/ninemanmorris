package com.github.adammw.ninemanmorris;

/**
 * Player class representing actions entered by a human end-user
 */
public class HumanPlayer extends Player {
    /**
     * Create a new HumanPlayer object
     * @param controller the game controller the object should ask for getting moves from the view
     */
    public HumanPlayer(GameController controller) {
        super(controller);
    }

    /**
     * Get a move for the human player
     * @param board the current game board state
     * @return a move returned by the end-user
     */
    public Move getMove(Board board) {
        return controller.getMoveFromUser(board, this);
    }
}
