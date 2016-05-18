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
    private HashMap<Player, GameStage> playerStages;
    private Board.MillFormedCallback callback;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        controller = mock(GameController.class);
        callback = mock(Board.MillFormedCallback.class);
        players = new Player[] {
                PlayerFactory.build(controller, PlayerType.HUMAN_PLAYER),
                PlayerFactory.build(controller, PlayerType.HUMAN_PLAYER)
        };
        board = new Board(players);

        // Expose private fields with reflection for use in tests
        Field boardField = Board.class.getDeclaredField("board");
        boardField.setAccessible(true);
        internalBoard = (Piece[][]) boardField.get(board);
        Field playerStagesField = Board.class.getDeclaredField("playerStages");
        playerStagesField.setAccessible(true);
        playerStages = (HashMap<Player, GameStage>) playerStagesField.get(board);
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
        board.performMove(move, players[0], callback);
        verifyZeroInteractions(callback);
        assertNotNull(internalBoard[0][0]);
    }

    @Test
    public void testPlacingOccupiedLocation() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[0]).remove(0);

        thrown.expect(Board.IllegalMoveException.class);
        thrown.expectMessage("Board location is occupied");

        Move move = new Move(null, "a1");
        board.performMove(move, players[0], callback);
        verifyZeroInteractions(callback);
    }


    @Test
    public void testMillDetectionColumn() throws Exception {
        internalBoard[2][3] = playerPieces.get(players[0]).remove(0);
        internalBoard[1][3] = playerPieces.get(players[0]).remove(0);

        Move move = new Move(null, "d1");
        board.performMove(move, players[0], callback);
        verify(callback).millFormed();
    }

    @Test
    public void testMillDetectionRow() throws Exception {
        internalBoard[6][0] = playerPieces.get(players[0]).remove(0);
        internalBoard[6][6] = playerPieces.get(players[0]).remove(0);

        Move move = new Move(null, "d7");
        board.performMove(move, players[0], callback);
        verify(callback).millFormed();
    }

    @Test
    public void testOpponentMillNotDetected() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[0]).remove(0);
        internalBoard[0][3] = playerPieces.get(players[0]).remove(0);

        Move move = new Move(null, "g1");
        board.performMove(move, players[1], callback);
        verifyZeroInteractions(callback);
    }

    @Test
    public void testAcrossMidpointMillNotDetected() throws Exception {
        internalBoard[3][1] = playerPieces.get(players[0]).remove(0);
        internalBoard[3][2] = playerPieces.get(players[0]).remove(0);
        internalBoard[1][3] = playerPieces.get(players[0]).remove(0);
        internalBoard[2][3] = playerPieces.get(players[0]).remove(0);

        board.performMove(new Move(null, "e4"), players[0], callback);
        verifyZeroInteractions(callback);

        board.performMove(new Move(null, "d5"), players[0], callback);
        verifyZeroInteractions(callback);
    }

    @Test
    public void testCantRemoveNoPiece() throws Exception {
        internalBoard[6][0] = playerPieces.get(players[0]).remove(0);
        internalBoard[6][6] = playerPieces.get(players[0]).remove(0);

        Move removeD7 = new Move("d7", null);
        board.performMove(new Move(null, "d1"), players[0], () -> {
            try {
                board.performMove(removeD7, players[0], null);
                fail();
            } catch (Board.IllegalMoveException e) {
                assertEquals(e.getMessage(), "There is no piece at the specified location");
            }
        });
    }

    @Test
    public void testCantRemoveOwnPiece() throws Exception {
        internalBoard[6][0] = playerPieces.get(players[0]).remove(0);
        internalBoard[6][6] = playerPieces.get(players[0]).remove(0);

        Move addD7 = new Move(null, "d7");
        Move removeD7 = new Move("d7", null);
        board.performMove(addD7, players[0], () -> {
            try {
                board.performMove(removeD7, players[0], null);
                fail();
            } catch (Board.IllegalMoveException e) {
                assertEquals(e.getMessage(), "Can't remove your own piece");
            }
        });
    }


    @Test
    public void testCantRemoveMillPieceUnlessNoRemaining() throws Exception {
        final Move removeA1 = new Move("a1", null);
        final Move removeD2 = new Move("d2", null);

        // Set up the board with the opponent having a mill a1 - g1, and also a piece in d2
        internalBoard[0][0] = playerPieces.get(players[1]).remove(0);
        internalBoard[0][3] = playerPieces.get(players[1]).remove(0);
        internalBoard[0][6] = playerPieces.get(players[1]).remove(0);
        internalBoard[1][3] = playerPieces.get(players[1]).remove(0);

        // Create a mill a7-g7
        internalBoard[6][0] = playerPieces.get(players[0]).remove(0);
        internalBoard[6][6] = playerPieces.get(players[0]).remove(0);
        board.performMove(new Move(null, "d7"), players[0], () -> {
            // Expect removal of a1 to fail (non-mill pieces exist)
            try {
                board.performMove(removeA1, players[0], null);
                fail();
            } catch(Board.IllegalMoveException expectedException) {
                assertEquals(expectedException.getMessage(), "Can't remove a piece which is part of a mill");
            }

            // Expect removal of d2 to succeed
            try {
                board.performMove(removeD2, players[0], null);
            } catch(Board.IllegalMoveException exception) {
                fail(exception.getMessage());
            }
            assertNull(internalBoard[1][3]);
        });


        // Now only a mill remains, expect removal of a1 to succeed
        internalBoard[6][3] = null;
        board.performMove(new Move(null, "d7"), players[0], () -> {
            try {
                board.performMove(removeA1, players[0], null);
            } catch(Board.IllegalMoveException exception) {
                fail(exception.getMessage());
            }
            assertNull(internalBoard[0][0]);
        });
    }

    @Test
    public void testGameStageTransitions() throws Exception {
        for (int i = 0; i < 5; i++) { playerPieces.get(players[0]).remove(0); } // throw 5 pieces away and place 4
        board.performMove(new Move(null, "a1"), players[0], callback);
        board.performMove(new Move(null, "d2"), players[0], callback);
        board.performMove(new Move(null, "g4"), players[0], callback);
        board.performMove(new Move(null, "a4"), players[0], callback);

        assertEquals(board.getStage(players[0]), GameStage.MOVING);

        // form a mill to remove one of player 1's pieces
        internalBoard[6][0] = playerPieces.get(players[1]).remove(0);
        internalBoard[6][3] = playerPieces.get(players[1]).remove(0);
        Move removeA4 = new Move("a4", null);
        board.performMove(new Move(null, "g7"), players[1], () -> {
            try {
                board.performMove(removeA4, players[1], null);
            } catch (Board.IllegalMoveException e) {
                fail(e.getMessage());
            }
        });

        assertEquals(board.getStage(players[0]), GameStage.FLYING);

        // form a mill to remove one of player 1's pieces
        internalBoard[6][6] = null;
        Move removeA1 = new Move("a1", null);
        board.performMove(new Move(null, "g7"), players[1], () -> {
            try {
                board.performMove(removeA1, players[1], null);
            } catch (Board.IllegalMoveException e) {
                fail(e.getMessage());
            }
        });

        assertEquals(board.getStage(players[0]), GameStage.GAME_OVER);
        assertTrue(board.isGameOver());
    }

    @Test
    public void testCantMoveOtherPlayer() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[1]).remove(0);
        playerStages.put(players[0], GameStage.MOVING);

        thrown.expect(Board.IllegalMoveException.class);
        thrown.expectMessage("Can't move another player's piece");

        board.performMove(new Move("a1", "d1"), players[0], callback);
    }

    @Test
    public void testCantMoveOccupiedLocation() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[0]).remove(0);
        internalBoard[0][3] = playerPieces.get(players[0]).remove(0);
        playerStages.put(players[0], GameStage.MOVING);

        thrown.expect(Board.IllegalMoveException.class);
        thrown.expectMessage("Board location is occupied");

        board.performMove(new Move("a1", "d1"), players[0], callback);
    }

    @Test
    public void testCantMoveWhereNotConnected() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[0]).remove(0);
        playerStages.put(players[0], GameStage.MOVING);

        thrown.expect(Board.IllegalMoveException.class);
        thrown.expectMessage("Flying is not allowed yet");

        board.performMove(new Move("a1","b2"), players[0], callback);
    }

    @Test
    public void testCantMoveAcrossMidpoint() throws Exception {
        internalBoard[3][2] = playerPieces.get(players[0]).remove(0);
        playerStages.put(players[0], GameStage.MOVING);

        thrown.expect(Board.IllegalMoveException.class);
        thrown.expectMessage("Flying is not allowed yet");

        board.performMove(new Move("c4","e4"), players[0], callback);
    }

    @Test
    public void testCanMoveHorizontallyOnMidpointRow() throws Exception {
        internalBoard[3][0] = playerPieces.get(players[0]).remove(0);

        playerStages.put(players[0], GameStage.MOVING);
        board.performMove(new Move("a4","b4"), players[0], callback);

        playerStages.put(players[0], GameStage.MOVING);
        board.performMove(new Move("b4","a4"), players[0], callback);
    }

    @Test
    public void testCanMoveVerticallyOnMidpointColumn() throws Exception {
        internalBoard[0][3] = playerPieces.get(players[0]).remove(0);

        playerStages.put(players[0], GameStage.MOVING);
        board.performMove(new Move("d1","d2"), players[0], callback);

        playerStages.put(players[0], GameStage.MOVING);
        board.performMove(new Move("d2","d1"), players[0], callback);
    }


    @Test
    public void testCanMoveHorizontally() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[0]).remove(0);

        playerStages.put(players[0], GameStage.MOVING);
        board.performMove(new Move("a1","d1"), players[0], callback);

        playerStages.put(players[0], GameStage.MOVING);
        board.performMove(new Move("d1","a1"), players[0], callback);
    }

    @Test
    public void testCanMoveVertically() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[0]).remove(0);

        playerStages.put(players[0], GameStage.MOVING);
        board.performMove(new Move("a1","a4"), players[0], callback);

        playerStages.put(players[0], GameStage.MOVING);
        board.performMove(new Move("a4","a1"), players[0], callback);
    }

    @Test
    public void testMoving() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[0]).remove(0);
        playerStages.put(players[0], GameStage.MOVING);
        board.performMove(new Move("a1","d1"), players[0], callback);
        verifyZeroInteractions(callback);
    }

    @Test
    public void testFlying() throws Exception {
        internalBoard[0][0] = playerPieces.get(players[0]).remove(0);
        playerStages.put(players[0], GameStage.FLYING);
        board.performMove(new Move("a1","b2"), players[0], callback);
        verifyZeroInteractions(callback);
    }

}