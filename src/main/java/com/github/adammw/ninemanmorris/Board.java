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
    private HashMap<Player, GameStage> playerStages = new HashMap<>();
    private HashMap<Player, List<Piece>> playerPieces = new HashMap<>();
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
            playerPieces.put(player, pieces);
            playerStages.put(player, GameStage.PLACING);
        };
    }

    /**
     * Returns the player's current stage in the game
     * @param player the player to check
     * @return the current stage of the game
     */
    public GameStage getStage(Player player) {
        return playerStages.get(player);
    }

    /**
     * Check if the game is over
     * @return if the game is over
     */
    public boolean isGameOver() {
        return playerStages.values().stream().anyMatch(stage -> stage == GameStage.GAME_OVER);
    }

    /**
     * Access the piece at the specified coordinates
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the piece on the board at the specified coordinates
     */
    public Piece getPieceAt(int x, int y) {
        return board[y][x];
    }

    /**
     * Access the piece at the specified location
     * @param location a BoardLocation object representing the location on the board
     * @return the piece on the board at the specified coordinates, or null if there is no piece
     */
    public Piece getPieceAt(BoardLocation location) {
        return getPieceAt(location.getX(), location.getY());
    }

    /**
     * Get the number of pieces remaining to be placed for the specified player
     * @param player the player to check
     * @return the number of pieces yet to be placed
     */
    public int getPiecesRemainingToBePlacedForPlayer(Player player) {
        return playerPieces.get(player).size();
    }

    /**
     * Get a specific player object
     * @param idx the index of the player to return
     * @return the player object at position idx
     */
    public Player getPlayer(int idx) {
        return players[idx];
    }

    /**
     * Get the number of players
     * @return the number of players
     */
    public int getPlayerCount() {
        return players.length;
    }

    /**
     * Get the winning player
     * @return the player object representing the player who won the game, or null if the game is still in play
     */
    public Player getWinningPlayer() {
        if (!isGameOver()) { return null; }
        for (Player p : players) {
            if (playerStages.get(p) != GameStage.GAME_OVER) {
                return p;
            }
        }
        return null;
    }

    /**
     * Perform a move
     * @param move the move to perform
     * @param player the player performing the move
     * @param millFormedCallback a callback to be called if a mill is formed
     * @throws IllegalMoveException when the move is not within the rules of the game or otherwise invalid
     */
    public void performMove(Move move, Player player, MillFormedCallback millFormedCallback) throws IllegalMoveException {
        Piece piece;
        Player opponent = getOpposingPlayer(player);
        BoardLocation prevLocation = move.getPreviousPieceLocation();
        BoardLocation newLocation = move.getNewPieceLocation();
        GameStage currentStage = getStage(player);
        assert(prevLocation != null || newLocation != null);
        assert((allowRemoval && newLocation == null) || (!allowRemoval && newLocation != null));
        assert(currentStage != GameStage.GAME_OVER);

        // Ensure if placing or moving, that the piece doesn't already exist at that location
        if (newLocation != null && getPieceAt(newLocation) != null) {
            throw new IllegalMoveException("Board location is occupied");
        }

        // Validate the move's from location according to the game state and game rules
        if (prevLocation != null) {
            piece = getPieceAt(prevLocation);

            // Ensure there is a piece at the from location
            if (piece == null) {
                throw new IllegalMoveException("There is no piece at the specified location");
            }

            // Ensure you can't remove your own piece if a mill is formed
            if (newLocation == null && piece.getOwner() == player) {
                throw new IllegalMoveException("Can't remove your own piece");
            }

            // Ensure you can't remove an opponents piece in a mill
            if (newLocation == null && isInMill(prevLocation, opponent) &&
                    (numPiecesOnBoardOwnedByPlayer(opponent) - numPiecesInMillsOwnedByPlayer(opponent)) > 0) {
                throw new IllegalMoveException("Can't remove a piece which is part of a mill");
            }

            // Ensure you can't remove another player's piece when moving/flying
            if (newLocation != null && piece.getOwner() != player) {
                throw new IllegalMoveException("Can't move another player's piece");
            }

            // Ensure that the piece was actually moved
            if (prevLocation.equals(newLocation)) {
                throw new IllegalMoveException("Must move a piece");
            }

            // Ensure you can't fly until you are in the flying stage
            if (newLocation != null && currentStage == GameStage.MOVING) {
                if (!isAdjacent(prevLocation, newLocation)) {
                    throw new IllegalMoveException("Flying is not allowed yet");
                }
            }

            // Remove the old piece from the board
            removePiece(prevLocation);
        } else {
            // Remove the piece from the player's available pieces to place
            assert(currentStage == GameStage.PLACING);
            piece = playerPieces.get(player).remove(0);
        }

        // Add the piece to it's new location (unless removing a piece, mill formation sets newLocation=null)
        if (newLocation != null) {
            addPiece(newLocation, piece);
        }

        // Save move history (for undo) and recalculate the player's stage of the game
        history.add(move);
        recalculateGameStage(player);

        // Reset allowRemoval flag if it was set (only allow a single move per millFormed callback)
        if (allowRemoval) {
            allowRemoval = false;
        } else if(isInMill(newLocation, player)) { // if a mill was formed, notify callback and recalculate opponent's stage
            allowRemoval = true;
            millFormedCallback.millFormed();
            allowRemoval = false;

            // Recalculate the opponent's game stage as the piece removal may now allow them to fly or lose the game
            recalculateGameStage(getOpposingPlayer(player));
        }
    }

    /**
     * Checks the two locations are adjacent to each other on the board and connected by lines
     * @param loc1 location 1
     * @param loc2 location 2
     * @return
     */
    private boolean isAdjacent(BoardLocation loc1, BoardLocation loc2) {
        int boardSize = board.length;
        int midpoint = boardSize / 2;
        int x = loc1.getX();
        int y = loc1.getY();

        if (x == loc2.getX()) { // vertical movement
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
                for (int y2 = y - 1; y2 >= 0; y2--) {
                    if (VALID_LOCATIONS[y2][x]) {
                        if (loc2.getY() == y2) {
                            return true;
                        }
                        break;
                    }
                }
            }
        } else if (y == loc2.getY()) { // horizontal movement
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
                for (int x2 = x - 1; x2 >= 0; x2--) {
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
                if (playerPieces.get(player).size() == 0) {
                    playerStages.put(player, GameStage.MOVING);
                }
                break;
            case MOVING:
                // Move to flying stage when the player has only 3 pieces left
                if (numPiecesOnBoardOwnedByPlayer(player) < 4) {
                    playerStages.put(player, GameStage.FLYING);
                }
                break;
            case FLYING:
                // The game is over when the player has less than 3 pieces left
                if (numPiecesOnBoardOwnedByPlayer(player) < 3) {
                    playerStages.put(player, GameStage.GAME_OVER);
                }
                break;
        }
    }

    /**
     * Determine if a mill exists in the location specified
     * @param loc the location to check for a mill
     * @param player the player to check for owning the mill
     * @return whether or not 3 pieces in a row (a mill) was formed at that location
     */
    private boolean isInMill(BoardLocation loc, Player player) {
        return isInMill(loc.getX(), loc.getY(), player);
    }

    /**
     * Determine if a mill exists in the location specified
     * @param x the x location to check for a mill
     * @param y the y location to check for a mill
     * @param player the player to check for owning the mill
     * @return whether or not 3 pieces in a row (a mill) was formed at that location
     */
    private boolean isInMill(int x, int y, Player player) {
        boolean millFormed = false;
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
     * Count the number of pieces that a player has on a board that are currently forming a mill
     * @param player the player to chec
     * @return the number of pieces forming a mill (must be a multiple of 3)
     */
    private int numPiecesInMillsOwnedByPlayer(Player player) {
        int count = 0;
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[y].length; x++) {
                if (!VALID_LOCATIONS[y][x]) { continue; }
                Piece piece = getPieceAt(x, y);
                if (piece == null || piece.getOwner() != player) { continue; }
                if (isInMill(x, y, player)) { count++; }
            }
        }
        return count;
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
