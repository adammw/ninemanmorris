package com.github.adammw.ninemanmorris;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class represents the game board and contains all the data for the current state of the game
 */
public class Board {
    private GameStage stage = GameStage.PLACING;
    private HashMap<Player, List<Piece>> pieceMap = new HashMap<>();
    private Player[] players;
    private Piece[][] board = new Piece[7][7];
    private List<Move> history = new ArrayList<>();

    public static final boolean[][] VALID_LOCATIONS = {
            {true, false, false, true, false, false, true},
            {false, true, false, true, false, true, false},
            {false, false, true, true, true, false, false},
            {true, true, true, false, true, true, true},
            {false, false, true, true, true, false, false},
            {false, true, false, true, false, true, false},
            {true, false, false, true, false, false, true}
    };

    public class MoveException extends Exception {
        public MoveException(String message) {
            super(message);
        }
    }

    public Board(Player[] players) {
        this.players = players;

        // Create pieces for each player
        Stream.of(players).forEach(player -> {
            List<Piece> pieces = IntStream.range(0, 9).mapToObj(i -> new Piece(player)).collect(Collectors.toList());
            pieceMap.put(player, pieces);
        });
    }

    /**
     * @return the current stage of the game
     */
    public GameStage getStage() {
        return stage;
    }

    /**
     * @return if the game is over
     */
    public boolean isGameOver() {
        return stage == GameStage.GAME_OVER;
    }

    /**
     * @param x
     * @param y
     * @return the piece on the board at the specified coordinates
     */
    public Piece getPieceAt(int x, int y) {
        return board[y][x];
    }

    /**
     * @param location
     * @return the piece on the board at the specified coordinates, or null if there is no piece
     */
    public Piece getPieceAt(BoardLocation location) {
        return getPieceAt(location.getX(), location.getY());
    }

    /**
     * @param id
     * @return
     */
    public Player getPlayer(int id) {
        return players[id];
    }

    /**
     * Perform a move
     * @param move the move to perform
     * @param player the player performing the move
     */
    public void performMove(Move move, Player player) throws MoveException {
        Piece piece;

        BoardLocation prevLocation = move.getPreviousPieceLocation();
        if (stage == GameStage.PLACING) {
            assert(prevLocation == null);
            piece = pieceMap.get(player).remove(0);
        } else {
            piece = getPieceAt(prevLocation);
            if (piece.getOwner() != player) {
                throw new MoveException("Can't move another player's piece");
            }
            removePieceFrom(prevLocation);
        }

        BoardLocation newLocation = move.getNewPieceLocation();
        assert(newLocation != null);
        if (getPieceAt(newLocation) != null) {
            throw new MoveException("Piece already placed there");
        }
        addPieceTo(newLocation, piece);

        // TODO: detect mill

        recalculateGameStage();
    }

    private void recalculateGameStage() {
        if (stage == GameStage.PLACING && pieceMap.get(players[1]).size() == 0) {
            stage = GameStage.MOVING;
        }
        // TODO: moving -> flying transition
    }

    private void removePieceFrom(BoardLocation loc) {
        board[loc.getY()][loc.getX()] = null;
    }

    private void addPieceTo(BoardLocation loc, Piece piece) {
        board[loc.getY()][loc.getX()] = piece;
    }
}
