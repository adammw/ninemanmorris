package com.github.adammw.ninemanmorris;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the Board Class
 */
public class BoardTest extends EasyMockSupport {
    private GameController controller;
    private Player[] players;
    private Board board;

    @Before
    public void setUp() {
        controller = mock(GameController.class);
        players = new Player[] {
                PlayerFactory.build(controller, PlayerType.HUMAN_PLAYER),
                PlayerFactory.build(controller, PlayerType.HUMAN_PLAYER)
        };
        board = new Board(players);
    }

    @Test
    public void testInitialStage() throws Exception {
        for (Player player : players) {
            assertEquals(board.getStage(player), GameStage.PLACING);
        }
    }

    @Test
    public void testPiecesCreated() throws Exception {
        for (Player player : players) {
            assertEquals(board.getPiecesRemainingToBePlacedForPlayer(player), 9);
        }
    }

    @Test
    public void testPerformMove() throws Exception {

    }
}