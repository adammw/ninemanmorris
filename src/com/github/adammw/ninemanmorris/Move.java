package com.github.adammw.ninemanmorris;

/**
 * This class represents a possible move of the game
 */
public class Move {
    private BoardLocation previousPieceLocation;
    private BoardLocation newPieceLocation;

    public Move(BoardLocation previousPieceLocation, BoardLocation newPieceLocation) {
        this.previousPieceLocation = previousPieceLocation;
        this.newPieceLocation = newPieceLocation;
    }

    public BoardLocation getPreviousPieceLocation() {
        return previousPieceLocation;
    }

    public BoardLocation getNewPieceLocation() {
        return newPieceLocation;
    }
}
