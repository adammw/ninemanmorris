package com.github.adammw.ninemanmorris;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Unit tests for the Board Class
 */
public class BoardTest {
    private GameController controller;
    private Player[] players;
    private Board board;
    private Piece[][] internalBoard;
    private HashMap<Player, List<Piece>> playerPieces;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        controller = mock(GameController.class);
        players = new Player[] {
                PlayerFactory.build(controller, PlayerType.HUMAN_PLAYER),
                PlayerFactory.build(controller, PlayerType.HUMAN_PLAYER)
        };
        board = new Board(players);

        // Expose private fields with reflection
        Field boardField = Board.class.getDeclaredField("board");
        boardField.setAccessible(true);
        internalBoard = (Piece[][]) boardField.get(board);
        Field playerPiecesField = Board.class.getDeclaredField("playerPieces");
        playerPiecesField.setAccessible(true);
        playerPieces = (HashMap<Player, List<Piece>>) playerPiecesField.get(board);
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
    public void testPlacingPiece() throws Exception {
        Move move = new Move(null, "a1");
        board.performMove(move, players[0], () -> {
            fail("Mill should not have been formed");
        });
    }

    @Test
    public void testPlacingOccupiedLocation() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[0]).remove(0);

        thrown.expect(Board.IllegalMoveException.class);
        thrown.expectMessage("Board location is occupied");

        Move move = new Move(null, "a1");
        board.performMove(move, players[0], () -> {
            fail("Mill should not have been formed");
        });
    }


    @Test
    public void testMillDetection() throws Exception {
        internalBoard[2][3] = playerPieces.get(players[0]).remove(0);
        internalBoard[1][3] = playerPieces.get(players[0]).remove(0);

        boolean millFormed = false;
        Move move = new Move(null, "d1");
        Board.MillFormedCallback callback = mock(Board.MillFormedCallback.class);
        board.performMove(move, players[0], callback);
        verify(callback).millFormed();
    }
}