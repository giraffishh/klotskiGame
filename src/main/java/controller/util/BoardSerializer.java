package controller.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理棋盘状态的序列化和反序列化。
 * 提供将二维矩阵和长整型表示之间的转换，定义棋子类型和常量。
 */
public class BoardSerializer {
    // --- 数组表示中的常量 ---
    public static final int EMPTY = 0;
    public static final int SOLDIER = 1;     // 单元格 (兵) 1x1
    public static final int HORIZONTAL = 2;  // 水平方块 (横二) 1x2
    public static final int VERTICAL = 3;    // 垂直方块 (竖二) 2x1
    public static final int CAO_CAO = 4;     // 大方块 (曹操) 2x2

    // --- 长整型中使用的3位编码常量 ---
    public static final long CODE_EMPTY = 0b000;      // 0
    public static final long CODE_SOLDIER = 0b001;    // 1
    public static final long CODE_VERTICAL = 0b010;   // 2
    public static final long CODE_HORIZONTAL = 0b011; // 3
    public static final long CODE_CAO_CAO = 0b100;    // 4

    // --- 映射关系 ---
    public static final Map<Integer, Long> arrayToCodeMap = new HashMap<>();
    private static final Map<Long, Integer> codeToArrayMap = new HashMap<>();

    static {
        // 数组值 -> 编码（用于序列化）
        arrayToCodeMap.put(EMPTY, CODE_EMPTY);
        arrayToCodeMap.put(SOLDIER, CODE_SOLDIER);
        arrayToCodeMap.put(HORIZONTAL, CODE_HORIZONTAL);
        arrayToCodeMap.put(VERTICAL, CODE_VERTICAL);
        arrayToCodeMap.put(CAO_CAO, CODE_CAO_CAO);

        // 编码 -> 数组值（用于反序列化）
        codeToArrayMap.put(CODE_EMPTY, EMPTY);
        codeToArrayMap.put(CODE_SOLDIER, SOLDIER);
        codeToArrayMap.put(CODE_HORIZONTAL, HORIZONTAL);
        codeToArrayMap.put(CODE_VERTICAL, VERTICAL);
        codeToArrayMap.put(CODE_CAO_CAO, CAO_CAO);
    }

    // --- 棋盘尺寸 ---
    public static final int ROWS = 5;
    public static final int COLS = 4;
    private static final int BITS_PER_CELL = 3;
    private static final long MASK_3_BITS = (1L << BITS_PER_CELL) - 1L; // 0b111 = 7L

    /**
     * 将二维矩阵转换为长整型表示（标准棋盘尺寸）
     * 
     * @param board 要转换的二维矩阵
     * @return 矩阵的长整型表示
     */
    public static long serialize(int[][] board) {
        if (board == null || board.length != ROWS || board[0] == null || board[0].length != COLS) {
            throw new IllegalArgumentException("棋盘必须为 " + ROWS + "x" + COLS);
        }

        long layout = 0L;
        for (int r = 0; r < ROWS; r++) {
            if (board[r] == null || board[r].length != COLS) {
                throw new IllegalArgumentException("棋盘第 " + r + " 行长度不正确或为空");
            }
            for (int c = 0; c < COLS; c++) {
                int pieceType = board[r][c];
                Long code = arrayToCodeMap.get(pieceType);

                if (code == null) {
                    throw new IllegalArgumentException("无效的棋子类型 " + pieceType + " 位于 [" + r + "," + c + "]");
                }

                int positionIndex = r * COLS + c;
                int shiftAmount = positionIndex * BITS_PER_CELL;
                layout |= (code << shiftAmount);
            }
        }
        return layout;
    }

    /**
     * 将长整型表示转换为二维矩阵（标准棋盘尺寸）
     * 
     * @param layout 矩阵的长整型表示
     * @return 解析后的二维矩阵
     */
    public static int[][] deserialize(long layout) {
        int[][] board = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int positionIndex = r * COLS + c;
                int shiftAmount = positionIndex * BITS_PER_CELL;
                long code = (layout >> shiftAmount) & MASK_3_BITS;

                Integer pieceType = codeToArrayMap.get(code);
                if (pieceType == null) {
                    String binaryString = Long.toBinaryString(layout);
                    while (binaryString.length() < ROWS * COLS * BITS_PER_CELL) binaryString = "0" + binaryString;
                    throw new IllegalArgumentException(
                            "无效的3位编码 " + code + " (二进制 " + Long.toBinaryString(code) + ") " +
                                    "在布局 " + layout + " (二进制 " + binaryString +") " +
                                    "位置索引 " + positionIndex + " [r=" + r + ", c=" + c + "]"
                    );
                }
                board[r][c] = pieceType;
            }
        }
        return board;
    }

    /** 辅助方法：打印棋盘到控制台 */
    public static void printBoard(int[][] board) {
        if (board == null) {
            System.out.println("棋盘为空。");
            return;
        }
        System.out.println("--- 棋盘 ---");
        for (int[] row : board) {
            if (row == null) {
                System.out.println("[空行]");
            } else {
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

    // --- 测试用主方法 ---
    public static void main(String[] args) {
        int[][] exampleBoard = {
                {VERTICAL, CAO_CAO, CAO_CAO, VERTICAL},
                {VERTICAL, CAO_CAO, CAO_CAO, VERTICAL},
                {VERTICAL, HORIZONTAL, HORIZONTAL, VERTICAL},
                {VERTICAL, SOLDIER, SOLDIER, VERTICAL},
                {SOLDIER, EMPTY, EMPTY, SOLDIER}
        };

        System.out.println("原始棋盘 (使用常量):");
        printBoard(exampleBoard);

        try {
            long serializedLayout = serialize(exampleBoard);
            System.out.println("\n序列化布局 (长整型): " + serializedLayout);
            String binaryString = Long.toBinaryString(serializedLayout);
            while (binaryString.length() < ROWS * COLS * BITS_PER_CELL) binaryString = "0" + binaryString;
            System.out.println("序列化布局 (二进制, 60位): " + binaryString);

            int[][] deserializedBoard = deserialize(serializedLayout);
            System.out.println("\n反序列化棋盘:");
            printBoard(deserializedBoard);

            boolean match = Arrays.deepEquals(exampleBoard, deserializedBoard);
            System.out.println("\n原始棋盘和反序列化棋盘是否匹配: " + match);

            // 测试反序列化时的无效编码 (手动构造错误布局)
            long badLayout = serializedLayout | (0b111L << (2 * BITS_PER_CELL)); // 在 [0,2] 放置无效编码 7
            System.out.println("\n尝试反序列化包含无效编码的布局: " + badLayout);
            try {
                deserialize(badLayout);
            } catch (IllegalArgumentException e) {
                System.out.println("捕获到预期错误: " + e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
