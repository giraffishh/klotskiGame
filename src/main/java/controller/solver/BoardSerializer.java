package controller.solver; // Updated package name

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles serialization/deserialization between int[][] and long layout.
 * Defines piece codes and constants.
 */
public class BoardSerializer {

    // --- Array Representation Constants ---
    public static final int EMPTY = 0;
    public static final int SOLDIER = 1;     // 1x1
    public static final int HORIZONTAL = 2;  // 1x2
    public static final int VERTICAL = 3;    // 2x1
    public static final int CAO_CAO = 4;     // 2x2

    // --- 3-bit Code Constants (for long layout) ---
    // 将这些常量改为公有，以便KlotskiSolver可以访问
    public static final long CODE_EMPTY = 0b000;      // 0
    public static final long CODE_SOLDIER = 0b001;    // 1
    public static final long CODE_VERTICAL = 0b010;   // 2 (Note: Different from array value)
    public static final long CODE_HORIZONTAL = 0b011; // 3 (Note: Different from array value)
    public static final long CODE_CAO_CAO = 0b100;    // 4

    // --- Mappings ---
    // Public map used by KlotskiSolver to get codes
    public static final Map<Integer, Long> arrayToCodeMap = new HashMap<>();
    // Private map for deserialization
    private static final Map<Long, Integer> codeToArrayMap = new HashMap<>();

    static {
        // Array Value -> Code for Serialization
        arrayToCodeMap.put(EMPTY, CODE_EMPTY);
        arrayToCodeMap.put(SOLDIER, CODE_SOLDIER);
        arrayToCodeMap.put(HORIZONTAL, CODE_HORIZONTAL); // Array 2 -> Code 3
        arrayToCodeMap.put(VERTICAL, CODE_VERTICAL);     // Array 3 -> Code 2
        arrayToCodeMap.put(CAO_CAO, CODE_CAO_CAO);       // Array 4 -> Code 4

        // Code -> Array Value for Deserialization
        codeToArrayMap.put(CODE_EMPTY, EMPTY);
        codeToArrayMap.put(CODE_SOLDIER, SOLDIER);
        codeToArrayMap.put(CODE_HORIZONTAL, HORIZONTAL); // Code 3 -> Array 2
        codeToArrayMap.put(CODE_VERTICAL, VERTICAL);     // Code 2 -> Array 3
        codeToArrayMap.put(CODE_CAO_CAO, CAO_CAO);       // Code 4 -> Array 4
    }

    // --- Board Dimensions ---
    public static final int ROWS = 5;
    public static final int COLS = 4;
    private static final int BITS_PER_CELL = 3;
    private static final long MASK_3_BITS = (1L << BITS_PER_CELL) - 1L; // 0b111 = 7L

    /** Serializes int[][] board to long layout. */
    public static long serialize(int[][] board) {
        if (board == null || board.length != ROWS || board[0] == null || board[0].length != COLS) {
            throw new IllegalArgumentException("Board must be " + ROWS + "x" + COLS);
        }

        long layout = 0L;
        for (int r = 0; r < ROWS; r++) {
            if (board[r] == null || board[r].length != COLS) { // Check inner array
                throw new IllegalArgumentException("Board row " + r + " has incorrect length or is null.");
            }
            for (int c = 0; c < COLS; c++) {
                int pieceType = board[r][c];
                Long code = arrayToCodeMap.get(pieceType);

                if (code == null) {
                    throw new IllegalArgumentException("Invalid piece type " + pieceType + " at [" + r + "," + c + "]");
                }

                int positionIndex = r * COLS + c;
                int shiftAmount = positionIndex * BITS_PER_CELL;
                layout |= (code << shiftAmount);
            }
        }
        return layout;
    }

    /** Deserializes long layout to int[][] board. */
    public static int[][] deserialize(long layout) {
        int[][] board = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int positionIndex = r * COLS + c;
                int shiftAmount = positionIndex * BITS_PER_CELL;
                long code = (layout >> shiftAmount) & MASK_3_BITS;

                Integer pieceType = codeToArrayMap.get(code);
                if (pieceType == null) {
                    // Provide more context in error message
                    String binaryString = Long.toBinaryString(layout);
                    while (binaryString.length() < ROWS * COLS * BITS_PER_CELL) binaryString = "0" + binaryString;
                    throw new IllegalArgumentException(
                            "Invalid 3-bit code " + code + " (binary " + Long.toBinaryString(code) + ") " +
                                    "found in layout " + layout + " (binary " + binaryString +") " +
                                    "at position index " + positionIndex + " [r=" + r + ", c=" + c + "]"
                    );
                }
                board[r][c] = pieceType;
            }
        }
        return board;
    }

    /** Helper to print board to console. */
    public static void printBoard(int[][] board) {
        if (board == null) {
            System.out.println("Board is null.");
            return;
        }
        System.out.println("--- Board ---");
        for (int[] row : board) {
            if (row == null) {
                System.out.println("[null row]");
            } else {
                // Simple alignment for single digits
                StringBuilder sb = new StringBuilder("[");
                for(int i=0; i<row.length; i++){
                    sb.append(row[i]);
                    if(i < row.length - 1) sb.append(", ");
                }
                sb.append("]");
                System.out.println(sb.toString());
            }
        }
        System.out.println("-------------");
    }

    // --- Main method for testing serialization/deserialization ---
    public static void main(String[] args) {
        int[][] exampleBoard = {
                {VERTICAL, CAO_CAO, CAO_CAO, VERTICAL},
                {VERTICAL, CAO_CAO, CAO_CAO, VERTICAL},
                {VERTICAL, HORIZONTAL, HORIZONTAL, VERTICAL},
                {VERTICAL, SOLDIER, SOLDIER, VERTICAL},
                {SOLDIER, EMPTY, EMPTY, SOLDIER}
        };

        System.out.println("Original Board (Using Constants):");
        printBoard(exampleBoard);

        try {
            long serializedLayout = serialize(exampleBoard);
            System.out.println("\nSerialized Layout (long): " + serializedLayout);
            String binaryString = Long.toBinaryString(serializedLayout);
            while (binaryString.length() < ROWS * COLS * BITS_PER_CELL) binaryString = "0" + binaryString;
            System.out.println("Serialized Layout (binary, 60 bits): " + binaryString);

            int[][] deserializedBoard = deserialize(serializedLayout);
            System.out.println("\nDeserialized Board:");
            printBoard(deserializedBoard);

            boolean match = Arrays.deepEquals(exampleBoard, deserializedBoard);
            System.out.println("\nOriginal and Deserialized boards match: " + match);

            // Test invalid code during deserialization (manually craft a bad layout)
            long badLayout = serializedLayout | (0b111L << (2 * BITS_PER_CELL)); // Put invalid code 7 at [0,2]
            System.out.println("\nAttempting to deserialize layout with invalid code: " + badLayout);
            try {
                deserialize(badLayout);
            } catch (IllegalArgumentException e) {
                System.out.println("Caught expected error: " + e.getMessage());
            }


        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}