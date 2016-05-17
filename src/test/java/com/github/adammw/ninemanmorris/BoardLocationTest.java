package com.github.adammw.ninemanmorris;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the BoardLocation class
 */
public class BoardLocationTest {
    @Test(expected = BoardLocation.InvalidLocationException.class)
    public void testInvalidLocationFormat1() throws Exception {
        new BoardLocation("abc123");
    }

    @Test(expected = BoardLocation.InvalidLocationException.class)
    public void testInvalidLocationFormat2() throws Exception {
        new BoardLocation("7g");
    }

    @Test(expected = BoardLocation.InvalidLocationException.class)
    public void testOutOfBounds() throws Exception {
        new BoardLocation("h9");
    }

    @Test(expected = BoardLocation.InvalidLocationException.class)
    public void testNonIntersection() throws Exception {
        new BoardLocation("a2");
    }

    @Test
    public void testValidLocation() throws Exception {
        BoardLocation loc = new BoardLocation("d7");
        assertEquals(loc.getX(), 3);
        assertEquals(loc.getY(), 6);
    }
}