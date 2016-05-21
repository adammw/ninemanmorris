package com.github.adammw.ninemanmorris;

/**
 * Abstract class representing the base Player class
 */
public abstract class Player {
    protected GameController controller;
    protected String name;

    public Player(GameController controller, String name) {
        this.controller = controller;
        this.name = name;
    }

    /**
     * Gets the player's name
     * @return the player name string
     */
    public String getName() {
        return name;
    }

    abstract Move getMove(Board board);

    abstract Move getPieceToRemove(Board board);
}
