package com.github.adammw.ninemanmorris;

/**
 * This class represents a location on the board
 */
public class BoardLocation {
    private int x;
    private int y;

    public class InvalidLocationException extends Exception {
        public InvalidLocationException(String message) {
            super(message);
        }
    }

    public BoardLocation(String location) throws InvalidLocationException {
        if (location.length() != 2) throw new InvalidLocationException("Invalid location");
        if (location.charAt(0) >= 'a' && location.charAt(0) <= 'g') {
            x = location.charAt(0) - 'a';
        } else {
            throw new InvalidLocationException("Invalid location");
        }
        if (location.charAt(1) >= '1' && location.charAt(1) <= '7') {
            y = location.charAt(1) - '1';
        } else {
            throw new InvalidLocationException("Invalid location");
        }
        if (!Board.VALID_LOCATIONS[y][x]) {
            throw new InvalidLocationException("Invalid location");
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}