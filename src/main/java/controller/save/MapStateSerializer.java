package controller.save;

/**
 * 负责处理游戏地图状态的序列化和反序列化
 * 提供将二维矩阵转换为长整型表示和将长整型解析回矩阵的方法
 */
public class MapStateSerializer {
    // 二维数组中棋子类型的常量定义
    public static final int EMPTY = 0;
    public static final int SOLDIER = 1; // 单元格 (兵)
    public static final int HORIZONTAL = 2; // 水平方块 (横二)
    public static final int VERTICAL = 3; // 垂直方块 (竖二)
    public static final int CAO_CAO = 4; // 大方块 (曹操)

    // 长整型中使用的3位编码常量定义
    private static final long CODE_EMPTY = 0b000; // 0
    private static final long CODE_SOLDIER = 0b001; // 1
    private static final long CODE_VERTICAL = 0b010; // 2 (对应数组中的 VERTICAL = 3)
    private static final long CODE_HORIZONTAL = 0b011; // 3 (对应数组中的 HORIZONTAL = 2)
    private static final long CODE_CAO_CAO = 0b100; // 4 (对应数组中的 CAO_CAO = 4)

    private static final int BITS_PER_CELL = 3;
    private static final long MASK_3_BITS = 0b111; // 用于提取3个比特位的掩码

    /**
     * 将二维矩阵转换为长整型表示
     * 
     * @param matrix 要转换的二维矩阵
     * @return 矩阵的长整型表示
     */
    public static long convertMatrixToLong(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            throw new IllegalArgumentException("矩阵不能为空");
        }

        int rows = matrix.length;
        int cols = matrix[0].length;
        
        // 确保60位长整型足以存储整个矩阵
        if (rows * cols * BITS_PER_CELL > 60) {
            throw new IllegalArgumentException("矩阵太大，无法转换为长整型");
        }

        long result = 0L;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int pieceType = matrix[r][c];
                // 获取对应的3位编码
                long code = getCodeForArrayValue(pieceType);

                // 计算位置索引和位移量
                int positionIndex = r * cols + c;
                int shiftAmount = positionIndex * BITS_PER_CELL;

                // 将3位编码左移到正确位置，并合并到结果中
                result |= (code << shiftAmount);
            }
        }
        return result;
    }

    /**
     * 将长整型表示转换回二维矩阵
     * 
     * @param longValue 矩阵的长整型表示
     * @param rows 矩阵的行数
     * @param cols 矩阵的列数
     * @return 解析后的二维矩阵
     */
    public static int[][] convertLongToMatrix(long longValue, int rows, int cols) {
        int[][] matrix = new int[rows][cols];
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // 计算位置索引和位移量
                int positionIndex = r * cols + c;
                int shiftAmount = positionIndex * BITS_PER_CELL;

                // 提取当前单元格的3个比特位
                long code = (longValue >> shiftAmount) & MASK_3_BITS;
                
                // 将3位编码转换为数组值
                matrix[r][c] = getArrayValueForCode(code);
            }
        }
        return matrix;
    }

    /**
     * 获取数组值对应的3位编码
     * 
     * @param arrayValue 数组中的值
     * @return 对应的3位编码
     */
    private static long getCodeForArrayValue(int arrayValue) {
        switch (arrayValue) {
            case EMPTY: return CODE_EMPTY;
            case SOLDIER: return CODE_SOLDIER;
            case HORIZONTAL: return CODE_HORIZONTAL;
            case VERTICAL: return CODE_VERTICAL;
            case CAO_CAO: return CODE_CAO_CAO;
            default:
                throw new IllegalArgumentException("无效的棋子类型: " + arrayValue);
        }
    }

    /**
     * 获取3位编码对应的数组值
     * 
     * @param code 3位编码
     * @return 对应的数组值
     */
    private static int getArrayValueForCode(long code) {
        if (code == CODE_EMPTY) return EMPTY;
        if (code == CODE_SOLDIER) return SOLDIER;
        if (code == CODE_HORIZONTAL) return HORIZONTAL;
        if (code == CODE_VERTICAL) return VERTICAL;
        if (code == CODE_CAO_CAO) return CAO_CAO;
        
        throw new IllegalArgumentException("无效的3位编码: " + code);
    }
}
