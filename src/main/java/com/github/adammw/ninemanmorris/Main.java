package com.github.adammw.ninemanmorris;

/**
 * This class defines the main entry point of the application
 */
public class Main {

    public static void main(String[] args) {
        // Create the controller, which will create the model and view
	    GameController controller = new GameController();
        controller.playGame();
    }
}
