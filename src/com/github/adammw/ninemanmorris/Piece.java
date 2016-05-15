package com.github.adammw.ninemanmorris;

/**
 * This class represents a piece on the Nine Man's Morris Board owned by an individual player
 */
public class Piece {
    private Player owner;

    public Piece(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }
}
