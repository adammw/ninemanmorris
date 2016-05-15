package com.github.adammw.ninemanmorris;

/**
 * Abstract class representing the base Player class
 */
public abstract class Player {
    protected GameController controller;

    public Player(GameController controller) {
        this.controller = controller;
    }

    abstract Move getMove(Board board);
}
