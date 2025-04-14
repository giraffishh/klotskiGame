package controller.solver;

import java.util.Arrays;
import java.util.Objects; // For Objects.hash

/**
 * Represents the state of the Klotski board using a compact long integer.
 * This class is immutable. The internal layout long is final.
 * It relies on BoardSerializer for conversion to/from int[][].
 * This class definition is shared across all solver algorithms.
 */
public final class BoardState { // Made final as it's intended to be immutable

    public final long layout; // The compact 60-bit representation of the board

    // Cache for the deserialized board array to avoid repeated deserialization.
    // Marked transient as it doesn't need to be serialized if BoardState itself were serialized.
    // NOTE: If this object is accessed concurrently, this cache is not thread-safe.
    // However, in the context of single-threaded solvers, it's fine.
    private transient int[][] boardArrayCache = null;

    /**
     * Constructor from a long layout representation.
     * @param layout The 60-bit long representing the board state.
     */
    public BoardState(long layout) {
        this.layout = layout;
    }

    /**
     * Constructor from a 2D integer array representation.
     * Serializes the array into the internal long layout.
     * @param boardArray The 5x4 int array representing the board state.
     * @throws IllegalArgumentException if board dimensions are incorrect or piece types invalid.
     */
    public BoardState(int[][] boardArray) {
        // Serialize the input array to get the canonical long representation
        this.layout = BoardSerializer.serialize(boardArray);
        // Store the input array in the cache (avoids immediate deserialization)
        // Make a defensive copy to ensure external modifications don't affect cache
        this.boardArrayCache = new int[BoardSerializer.ROWS][];
        for (int i = 0; i < BoardSerializer.ROWS; i++) {
            if (boardArray[i] == null || boardArray[i].length != BoardSerializer.COLS) {
                throw new IllegalArgumentException("Board must be " + BoardSerializer.ROWS + "x" + BoardSerializer.COLS);
            }
            this.boardArrayCache[i] = Arrays.copyOf(boardArray[i], BoardSerializer.COLS);
        }
    }

    /**
     * Gets the compact long representation of the board state.
     * @return The 60-bit long layout.
     */
    public long getLayout() {
        return layout;
    }

    /**
     * Gets the 2D integer array representation of the board state.
     * Deserializes from the long layout if not already cached.
     * Returns a defensive copy to prevent external modification of the cache.
     * @return A new 5x4 int array representing the board.
     */
    public int[][] getBoardArray() {
        // Check cache first
        if (this.boardArrayCache == null) {
            // Deserialize on demand if cache is empty
            this.boardArrayCache = BoardSerializer.deserialize(this.layout);
        }
        // Return a defensive copy to maintain immutability of the cached array
        int[][] copy = new int[BoardSerializer.ROWS][BoardSerializer.COLS];
        for (int i = 0; i < BoardSerializer.ROWS; i++) {
            // boardArrayCache should always be non-null here and correctly dimensioned
            copy[i] = Arrays.copyOf(this.boardArrayCache[i], BoardSerializer.COLS);
        }
        return copy;
    }

    /**
     * Checks if this BoardState is equal to another object.
     * Equality is based solely on the long layout value.
     * @param o The object to compare with.
     * @return true if the other object is a BoardState with the same layout, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Use instanceof check which handles null implicitly
        if (!(o instanceof BoardState)) return false;
        BoardState that = (BoardState) o;
        // The core comparison: layouts must match
        return layout == that.layout;
    }

    /**
     * Computes the hash code for this BoardState.
     * The hash code is based solely on the long layout value.
     * Uses a simple folding approach suitable for long hash codes.
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        // A simple way to hash a long into an int.
        // Equivalent to Long.hashCode(layout) in Java 8+
        return (int)(layout ^ (layout >>> 32));

        /* Alternative folding hash code used previously (also valid):
        final long MASK_14_BITS = 0x3FFFL; // 11111111111111 (14 ones)
        int hashCode = 0;
        // Split the 60 bits into 4 chunks of 14 bits and 1 chunk of 4 bits
        // (Alternatively, could use 5 chunks of 12 bits)
        for (int i = 0; i < 4; i++) {
             // Extract 14 bits and add to hash code
            hashCode += (int)((layout >> (i * 14)) & MASK_14_BITS);
        }
        // Add the remaining top bits (bits 56-59)
        hashCode += (int)(layout >> 56); // Extracts remaining bits
        return hashCode;
        */
        // Using Objects.hash is also an option, though might be slightly less performant
        // return Objects.hash(layout);
    }

    /**
     * Returns a string representation of the BoardState (primarily for debugging).
     * @return A string showing the internal layout value.
     */
    @Override
    public String toString() {
        // Mainly for debugging, showing the unique ID
        return "BoardState{" + layout + "}";
    }
}