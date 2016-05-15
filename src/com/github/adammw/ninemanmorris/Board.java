package com.github.adammw.ninemanmorris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class represents the game board and contains all the data for the current state of the game
 */
public class Board {
    private HashMap<Player, GameStage> stageMap = new HashMap<>();
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

    public interface MillFormedCallback {
        void millFormed();
    }

    public class MoveException extends Exception {
        public MoveException(String message) {
            super(message);
        }
    }

    public Board(Player[] players) {
        this.players = players;

        // Create pieces for each player
        for(Player player : players) {
            List<Piece> pieces = IntStream.range(0, 9).mapToObj(i -> new Piece(player)).collect(Collectors.toList());
            pieceMap.put(player, pieces);
            stageMap.put(player, GameStage.PLACING);
        };
    }

    /**
     * @return the current stage of the game
     */
    public GameStage getStage(Player player) {
        return stageMap.get(player);
    }

    /**
     * @return if the game is over
     */
    public boolean isGameOver() {
        return stageMap.values().stream().anyMatch(stage -> stage == GameStage.GAME_OVER);
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
    public void performMove(Move move, Player player, MillFormedCallback millFormedCallback) throws MoveException {
        Piece piece;

        BoardLocation prevLocation = move.getPreviousPieceLocation();
        GameStage currentStage = getStage(player);
        if (currentStage == GameStage.PLACING) {
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
        recalculateGameStage(player);

        if (wasMillFormed(player, newLocation)) {
            millFormedCallback.millFormed();
            recalculateGameStage(getOpposingPlayer(player));
        }
    }

    /**
     * Get the opposing player
     * @param player the current player
     * @return the next Player in the players array
     */
    private Player getOpposingPlayer(Player player) {
        int idx = Arrays.asList(players).indexOf(player);
        return players[(idx + 1) % players.length];
    }

    /**
     * Recalculate the game stage for the specified player and update it, if nessicary
     * @param player the player to update the game stage for
     */
    private void recalculateGameStage(Player player) {
        GameStage currentStage = getStage(player);
        switch (currentStage) {
            case PLACING:
                if (pieceMap.get(player).size() == 0) {
                    stageMap.put(player, GameStage.MOVING);
                }
                break;
            case MOVING:
                if (numPiecesOnBoardOwnedByPlayer(player) < 4) {
                    stageMap.put(player, GameStage.FLYING);
                }
                break;
            case FLYING:
                if (numPiecesOnBoardOwnedByPlayer(player) < 3) {
                    stageMap.put(player, GameStage.GAME_OVER);
                }
                break;
        }
    }

    /**
     * Determine if a mill was just formed by a player placing their piece
     * @param player the player to check
     * @param newPieceLocation the location of the last piece added/moved on the board
     * @return whether or not 3 pieces in a row (a mill) was formed
     */
    private boolean wasMillFormed(Player player, BoardLocation newPieceLocation) {
        boolean millFormed = false;
        int x = newPieceLocation.getX();
        int y = newPieceLocation.getY();
        int boardSize = board.length;
        int boardMidpoint = boardSize / 2;

        // Check row along where the piece was placed
        if (y == boardMidpoint) {
            if (x < boardMidpoint) {
                millFormed = (numPiecesInRowOwnedByPlayer(y, player, 0, boardMidpoint) == 3);
            } else {
                millFormed = (numPiecesInRowOwnedByPlayer(y, player, boardMidpoint, boardSize) == 3);
            }
        } else {
            millFormed = (numPiecesInRowOwnedByPlayer(y, player) == 3);
        }

        if (millFormed) { return true; }

        // Check column along where the piece was placed
        if (x == boardMidpoint) {
            if (y < boardMidpoint) {
                millFormed = (numPiecesInColOwnedByPlayer(x, player, 0, boardMidpoint) == 3);
            } else {
                millFormed = (numPiecesInColOwnedByPlayer(x, player, boardMidpoint, boardSize) == 3);
            }
        } else {
            millFormed = (numPiecesInColOwnedByPlayer(x, player) == 3);
        }

        return millFormed;
    }

    /**
     * Count the number of pieces that a player has in a particular row
     * @param rowIdx the row index to look in
     * @param player the player's pieces to look for
     * @return the count of pieces in the row owned by the player
     */
    private int numPiecesInRowOwnedByPlayer(int rowIdx, Player player) {
        int boardSize = board[rowIdx].length;
        return numPiecesInRowOwnedByPlayer(rowIdx, player, 0, boardSize);
    }

    /**
     * Count the number of pieces that a player has in a particular row
     * @param rowIdx the row index to look in
     * @param player the player's pieces to look for
     * @param offset the offset along the row (needed to check the centre row which contains two "rows")
     * @param xMax the maximum x value to check (exclusive; needed to check the centre row which contains two "rows")
     * @return the count of pieces in the row owned by the player
     */
    private int numPiecesInRowOwnedByPlayer(int rowIdx, Player player, int offset, int xMax) {
        int count = 0;
        for (int x = offset; x < xMax; x++) {
            if (!VALID_LOCATIONS[rowIdx][x]) { continue; }
            if (board[rowIdx][x] != null && board[rowIdx][x].getOwner() == player) {
                count++;
            }
        }
        return count;
    }

    /**
     * Count the number of pieces that a player has in a particular column
     * @param colIdx the row index to look in
     * @param player the player's pieces to look for
     * @return the count of pieces in the column owned by the player
     */
    private int numPiecesInColOwnedByPlayer(int colIdx, Player player) {
        int boardSize = board.length;
        return numPiecesInColOwnedByPlayer(colIdx, player, 0, boardSize);
    }

    /**
     * Count the number of pieces that a player has in a particular column
     * @param colIdx the column index to look in
     * @param player the player's pieces to look for
     * @param offset the offset along the column (needed to check the centre row which contains two "columns")
     * @param yMax the maximum y value to check (exclusive; needed to check the centre row which contains two "columns")
     * @return the count of pieces in the column owned by the player
     */
    private int numPiecesInColOwnedByPlayer(int colIdx, Player player, int offset, int yMax) {
        int count = 0;
        for (int y = offset; y < yMax; y++) {
            if (!VALID_LOCATIONS[y][colIdx]) { continue; }
            if (board[y][colIdx] != null && board[y][colIdx].getOwner() == player) {
                count++;
            }
        }
        return count;
    }

    /**
     * Count the number of pieces that a player has on the board
     * @param player the player to check
     * @return the number of pieces from that player remaining on the board
     */
    private long numPiecesOnBoardOwnedByPlayer(Player player) {
        return Arrays.stream(board)
                .flatMap(Arrays::stream)
                .filter(x -> x != null && x.getOwner() == player)
                .count();
    }

    private void removePieceFrom(BoardLocation loc) {
        board[loc.getY()][loc.getX()] = null;
    }

    private void addPieceTo(BoardLocation loc, Piece piece) {
        board[loc.getY()][loc.getX()] = piece;
    }
}
