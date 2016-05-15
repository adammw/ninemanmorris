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
    private boolean allowRemoval = false; // set when the previous move formed a mill

    /**
     * This constant array lists which locations are valid positions on the board.
     * The array should be indexed y first then x, although since it is symmetrical it doesn't change the result
     */
    public static final boolean[][] VALID_LOCATIONS = {
            // false for x = midpoint && y = midpoint, otherwise
            // true for x = midpoint || y = midpoint, otherwise
            // true for every x = y, otherwise
            // false
            {true,  false, false, true,  false, false, true},
            {false, true,  false, true,  false, true,  false},
            {false, false, true,  true,  true,  false, false},
            {true,  true,  true,  false, true,  true,  true},
            {false, false, true,  true,  true,  false, false},
            {false, true,  false, true,  false, true,  false},
            {true,  false, false, true,  false, false, true}
    };

    /**
     * This interface is used for providing callbacks when a mill is formed while processing a move
     */
    public interface MillFormedCallback {
        void millFormed();
    }

    /**
     * A custom exception class raised when invalid moves are encountered
     */
    public class IllegalMoveException extends Exception {
        public IllegalMoveException(String message) {
            super(message);
        }
    }

    /**
     * Create a new Board model
     * @param players the players of the game
     */
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
    public void performMove(Move move, Player player, MillFormedCallback millFormedCallback) throws IllegalMoveException {
        Piece piece;

        BoardLocation prevLocation = move.getPreviousPieceLocation();
        BoardLocation newLocation = move.getNewPieceLocation();
        GameStage currentStage = getStage(player);
        assert((allowRemoval && newLocation == null) || (!allowRemoval && newLocation != null));
        assert(currentStage != GameStage.GAME_OVER);

        // Validate the move according to the game state and game rules
        if (currentStage == GameStage.PLACING) {
            assert(prevLocation == null);
            piece = pieceMap.get(player).remove(0);
        } else {
            piece = getPieceAt(prevLocation);
            if (piece == null) {
                throw new IllegalMoveException("There is no piece at the specified location");
            }
            if (!allowRemoval && piece.getOwner() != player) {
                throw new IllegalMoveException("Can't move another player's piece");
            }
            if (allowRemoval && piece.getOwner() == player) {
                throw new IllegalMoveException("Can't remove your own piece");
            }
            if (prevLocation.equals(newLocation)) {
                throw new IllegalMoveException("Must move a piece");
            }
            if (currentStage == GameStage.MOVING) {
                if (!isAdjacent(prevLocation, newLocation)) {
                    throw new IllegalMoveException("Flying is not allowed yet");
                }
            }
        }

        // Ensure if placing or moving, that the piece doesn't already exist at that location
        if (newLocation != null && getPieceAt(newLocation) != null) {
            throw new IllegalMoveException("Piece already placed there");
        }

        // Remove the old piece from the board and add it to it's new location
        // null checks are necessary - placing mode makes prevLocation=null and mill formation makes newLocation=null
        if (prevLocation != null) {
            removePiece(prevLocation);
        }
        if (newLocation != null) {
            addPiece(newLocation, piece);
        }

        // Save move history for undo
        history.add(move);

        recalculateGameStage(player);

        // Reset allowRemoval flag if it was set (only allow a single move per millFormed callback)
        if (allowRemoval) {
            allowRemoval = false;
        } else if(wasMillFormed(player, newLocation)) { // if a mill was formed, notify callback and recalculate opponent's stage
            allowRemoval = true;
            millFormedCallback.millFormed();
            allowRemoval = false;
            recalculateGameStage(getOpposingPlayer(player));

        }
    }

    /**
     * Checks the two locations are adjacent to each other on the board and connected by lines
     * @param loc1
     * @param loc2
     * @return
     */
    private boolean isAdjacent(BoardLocation loc1, BoardLocation loc2) {
        int boardSize = board.length;
        int midpoint = boardSize / 2;
        int x = loc1.getX();
        int y = loc1.getY();

        if (loc1.getX() == loc2.getX()) { // vertical movement
            // check if next spot in positive direction is the next location,
            // unless it's d5 (as otherwise it would treat d3 as adjacent)
            if(x != midpoint || y != (midpoint - 1)) {
                for (int y2 = y + 1; y2 < boardSize; y2++) {
                    if (VALID_LOCATIONS[y2][x]) {
                        if (loc2.getY() == y2) {
                            return true;
                        }
                        break;
                    }
                }
            }

            // check if next spot in negative direction is the next location,
            // unless it's d3 (as otherwise it would treat d5 as adjacent)
            if (x != midpoint || y != (midpoint + 1)) {
                for (int y2 = y - 1; y2 > 0; y2--) {
                    if (VALID_LOCATIONS[y2][x]) {
                        if (loc2.getY() == y2) {
                            return true;
                        }
                        break;
                    }
                }
            }
        } else if (loc1.getY() == loc2.getY()) { // horizontal movement
            // check if next spot in positive direction is the next location,
            // unless it's c4 (as otherwise it would treat e4 as adjacent)
            if(y != midpoint || x != (midpoint - 1)) {
                for (int x2 = x + 1; x2 < boardSize; x2++) {
                    if (VALID_LOCATIONS[y][x2]) {
                        if (loc2.getX() == x2) {
                            return true;
                        }
                        break;
                    }
                }
            }

            // check if next spot in negative direction is the next location,
            // unless it's e4 (as otherwise it would treat c4 as adjacent)
            if (y != midpoint || x != (midpoint + 1)) {
                for (int x2 = y - 1; x2 > 0; x2--) {
                    if (VALID_LOCATIONS[y][x2]) {
                        if (loc2.getX() == x2) {
                            return true;
                        }
                        break;
                    }
                }
            }
        }

        return false;
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
                // Move to Moving stage when the player has no more pieces to place
                if (pieceMap.get(player).size() == 0) {
                    stageMap.put(player, GameStage.MOVING);
                }
                break;
            case MOVING:
                // Move to flying stage when the player has only 3 pieces left
                if (numPiecesOnBoardOwnedByPlayer(player) < 4) {
                    stageMap.put(player, GameStage.FLYING);
                }
                break;
            case FLYING:
                // The game is over when the player has less than 3 pieces left
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

    /**
     * Remove a piece from the board at the specified location
     * @param loc location on the board to remove the piece
     */
    private void removePiece(BoardLocation loc) {
        board[loc.getY()][loc.getX()] = null;
    }

    /**
     * Add a piece to the board at the specified location
     * @param loc location on the board to add the piece
     * @param piece the piece to add
     */
    private void addPiece(BoardLocation loc, Piece piece) {
        board[loc.getY()][loc.getX()] = piece;
    }
}
