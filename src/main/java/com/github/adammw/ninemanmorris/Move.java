package com.github.adammw.ninemanmorris;

/**
 * This data-holding class represents a possible move of the game
 */
public class Move {
    private BoardLocation previousPieceLocation;
    private BoardLocation newPieceLocation;

    public Move(BoardLocation previousPieceLocation, BoardLocation newPieceLocation) {
        this.previousPieceLocation = previousPieceLocation;
        this.newPieceLocation = newPieceLocation;
    }

    public Move(String previousLocation, String newLocation) throws BoardLocation.InvalidLocationException {
        this.previousPieceLocation = previousLocation != null ? new BoardLocation(previousLocation) : null;
        this.newPieceLocation = newLocation != null ? new BoardLocation(newLocation) : null;
    }

    public BoardLocation getPreviousPieceLocation() {
        return previousPieceLocation;
    }

    public BoardLocation getNewPieceLocation() {
        return newPieceLocation;
    }
}
