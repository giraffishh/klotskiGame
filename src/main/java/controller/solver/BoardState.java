package controller.solver; // Updated package name

import java.util.Arrays;

/**
 * Represents the state of the Klotski board using a compact long integer.
 * This class is immutable. The internal layout long is final.
 * Relies on BoardSerializer for conversion to/from int[][].
 */
public final class BoardState {

    public final long layout;
    private transient int[][] boardArrayCache = null; // Cache for visualization/debugging

    /** Constructor from long layout */
    public BoardState(long layout) {
        this.layout = layout;
    }

    /** Constructor from 2D array */
    public BoardState(int[][] boardArray) {
        this.layout = BoardSerializer.serialize(boardArray);
        // Cache the initial array defensively
        this.boardArrayCache = new int[BoardSerializer.ROWS][];
        for (int i = 0; i < BoardSerializer.ROWS; i++) {
            if (boardArray[i] == null || boardArray[i].length != BoardSerializer.COLS) {
                throw new IllegalArgumentException("Board must be " + BoardSerializer.ROWS + "x" + BoardSerializer.COLS);
            }
            this.boardArrayCache[i] = Arrays.copyOf(boardArray[i], BoardSerializer.COLS);
        }
    }

    /** Gets the compact long representation */
    public long getLayout() {
        return layout;
    }

    /**
     * Gets the 2D array representation (deserializes on demand, returns copy).
     * Primarily for display or interfacing with array-based logic.
     */
    public int[][] getBoardArray() {
        if (this.boardArrayCache == null) {
            this.boardArrayCache = BoardSerializer.deserialize(this.layout);
        }
        // Return a defensive copy
        int[][] copy = new int[BoardSerializer.ROWS][BoardSerializer.COLS];
        for (int i = 0; i < BoardSerializer.ROWS; i++) {
            copy[i] = Arrays.copyOf(this.boardArrayCache[i], BoardSerializer.COLS);
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardState)) return false;
        BoardState that = (BoardState) o;
        return layout == that.layout;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(layout); // Standard way to hash a long
    }

    @Override
    public String toString() {
        return "BoardState{" + layout + "}";
    }
}