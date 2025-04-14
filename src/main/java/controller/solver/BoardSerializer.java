package controller.solver;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements serialization and deserialization for a Klotski (Hua Rong Dao) board layout
 * between a 2D array representation and a 60-bit long integer representation,
 * based on the paper's methodology.
 *
 * 将华容道棋盘布局在二维数组和60位长整型之间进行序列化和反序列化。
 * 基于论文《一种改进的广度优先求解华容道问题的方法》中的思路。
 */
public class BoardSerializer {

    // Constants for piece types in the 2D array representation
    // 二维数组中棋子类型的常量定义
    public static final int EMPTY = 0;
    public static final int SOLDIER = 1; // 单元格 (兵)
    public static final int HORIZONTAL = 2; // 水平方块 (横二)
    public static final int VERTICAL = 3; // 垂直方块 (竖二)
    public static final int CAO_CAO = 4; // 大方块 (曹操)

    // Constants for the 3-bit codes used in the long representation
    // 长整型中使用的3位编码常量定义
    private static final long CODE_EMPTY = 0b000; // 0
    private static final long CODE_SOLDIER = 0b001; // 1
    private static final long CODE_VERTICAL = 0b010; // 2 (对应数组中的 VERTICAL = 3)
    private static final long CODE_HORIZONTAL = 0b011; // 3 (对应数组中的 HORIZONTAL = 2)
    private static final long CODE_CAO_CAO = 0b100; // 4 (对应数组中的 CAO_CAO = 4)

    // Mappings between array values and 3-bit codes
    // 数组值与3位编码之间的映射
    private static final Map<Integer, Long> arrayToCodeMap = new HashMap<>();
    private static final Map<Long, Integer> codeToArrayMap = new HashMap<>();

    static {
        arrayToCodeMap.put(EMPTY, CODE_EMPTY);
        arrayToCodeMap.put(SOLDIER, CODE_SOLDIER);
        arrayToCodeMap.put(HORIZONTAL, CODE_HORIZONTAL); // 注意：数组值 2 映射到编码 3
        arrayToCodeMap.put(VERTICAL, CODE_VERTICAL);     // 注意：数组值 3 映射到编码 2
        arrayToCodeMap.put(CAO_CAO, CODE_CAO_CAO);

        codeToArrayMap.put(CODE_EMPTY, EMPTY);
        codeToArrayMap.put(CODE_SOLDIER, SOLDIER);
        codeToArrayMap.put(CODE_HORIZONTAL, HORIZONTAL); // 注意：编码 3 映射回数组值 2
        codeToArrayMap.put(CODE_VERTICAL, VERTICAL);     // 注意：编码 2 映射回数组值 3
        codeToArrayMap.put(CODE_CAO_CAO, CAO_CAO);
    }

    public static final int ROWS = 5;
    public static final int COLS = 4;
    private static final int BITS_PER_CELL = 3;
    private static final long MASK_3_BITS = 0b111; // Mask to extract 3 bits (7) 用于提取3个比特位的掩码

    /**
     * Serializes the 2D board array into a long integer.
     * 将二维棋盘数组序列化为长整型。
     *
     * @param board The 5x4 integer array representing the board. 代表棋盘的 5x4 整数数组。
     * @return A long integer representing the board layout. 代表棋盘布局的长整型。
     * @throws IllegalArgumentException If the board dimensions are incorrect or contain invalid piece codes.
     * 如果棋盘尺寸不正确或包含无效的棋子代码，则抛出异常。
     */
    public static long serialize(int[][] board) {
        if (board == null || board.length != ROWS || board[0].length != COLS) {
            throw new IllegalArgumentException("Board must be 5x4.");
        }

        long layout = 0L;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int pieceType = board[r][c];
                Long code = arrayToCodeMap.get(pieceType);

                if (code == null) {
                    throw new IllegalArgumentException("Invalid piece type " + pieceType + " at [" + r + "," + c + "]");
                }

                // Calculate the position index (0 to 19)
                // 计算位置索引 (0 到 19)
                int positionIndex = r * COLS + c;
                // Calculate the bit shift amount (0, 3, 6, ..., 57)
                // 计算比特位移量 (0, 3, 6, ..., 57)
                int shiftAmount = positionIndex * BITS_PER_CELL;

                // Shift the 3-bit code to its correct position and combine with the result using OR
                // 将3位编码左移到正确位置，并使用或运算符合并到结果中
                layout |= (code << shiftAmount);
            }
        }
        return layout;
    }

    /**
     * Deserializes a long integer back into a 2D board array.
     * 将长整型反序列化回二维棋盘数组。
     *
     * @param layout The long integer representing the board layout. 代表棋盘布局的长整型。
     * @return A 5x4 integer array representing the board. 代表棋盘的 5x4 整数数组。
     * @throws IllegalArgumentException If the layout contains invalid 3-bit codes.
     * 如果布局包含无效的3位编码，则抛出异常。
     */
    public static int[][] deserialize(long layout) {
        int[][] board = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                // Calculate the position index (0 to 19)
                // 计算位置索引 (0 到 19)
                int positionIndex = r * COLS + c;
                // Calculate the bit shift amount
                // 计算比特位移量
                int shiftAmount = positionIndex * BITS_PER_CELL;

                // Extract the 3 bits for this cell: shift right, then apply mask
                // 提取当前单元格的3个比特位：先右移，然后应用掩码
                long code = (layout >> shiftAmount) & MASK_3_BITS;

                Integer pieceType = codeToArrayMap.get(code);
                if (pieceType == null) {
                    throw new IllegalArgumentException("Invalid 3-bit code " + code + " found in layout at position index " + positionIndex);
                }

                board[r][c] = pieceType;
            }
        }
        return board;
    }

    /**
     * Helper method to print the board to the console.
     * 辅助方法，用于在控制台打印棋盘。
     * @param board The board to print. 要打印的棋盘。
     */
    public static void printBoard(int[][] board) {
        if (board == null) {
            System.out.println("Board is null.");
            return;
        }
        for (int[] row : board) {
            System.out.println(Arrays.toString(row));
        }
    }

    // Main method for testing
    // 用于测试的主方法
    public static void main(String[] args) {
        // Example board from the user query
        // 来自用户查询的示例棋盘
        int[][] exampleBoard = {
                {3, 4, 4, 3}, // VERTICAL, CAO_CAO, CAO_CAO, VERTICAL
                {3, 4, 4, 3}, // VERTICAL, CAO_CAO, CAO_CAO, VERTICAL
                {3, 2, 2, 3}, // VERTICAL, HORIZONTAL, HORIZONTAL, VERTICAL
                {3, 1, 1, 3}, // VERTICAL, SOLDIER, SOLDIER, VERTICAL
                {1, 0, 0, 1}  // SOLDIER, EMPTY, EMPTY, SOLDIER
        };

        System.out.println("Original Board:");
        printBoard(exampleBoard);

        try {
            // Serialize the board
            // 序列化棋盘
            long serializedLayout = serialize(exampleBoard);
            System.out.println("\nSerialized Layout (long): " + serializedLayout);
            // Optionally print binary representation (requires padding for full 60 bits)
            // 可选：打印二进制表示（需要填充以显示完整的60位）
            String binaryString = Long.toBinaryString(serializedLayout);
            // Pad with leading zeros to show 60 bits
            // 前面补零以显示60位
            while (binaryString.length() < ROWS * COLS * BITS_PER_CELL) {
                binaryString = "0" + binaryString;
            }
            System.out.println("Serialized Layout (binary, 60 bits): " + binaryString);


            // Deserialize the layout back to a board
            // 将布局反序列化回棋盘
            int[][] deserializedBoard = deserialize(serializedLayout);
            System.out.println("\nDeserialized Board:");
            printBoard(deserializedBoard);

            // Verify if the deserialized board matches the original
            // 验证反序列化的棋盘是否与原始棋盘匹配
            boolean match = true;
            for(int r=0; r<ROWS; r++) {
                if (!Arrays.equals(exampleBoard[r], deserializedBoard[r])) {
                    match = false;
                    break;
                }
            }
            System.out.println("\nOriginal and Deserialized boards match: " + match);

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

