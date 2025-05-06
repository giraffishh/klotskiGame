package model;

import controller.util.BoardSerializer;

/**
 * 地图模型类，用于记录和管理游戏的地图数据 地图由整数矩阵表示，不同数字代表不同类型的方块： 0 - 空白区域 1 - 1x1单元格方块 2 -
 * 2x1水平方块 3 - 1x2垂直方块 4 - 2x2大方块 该类负责提供地图数据访问和管理功能，支持从存档加载和重置地图
 */
public class MapModel {

    /**
     * 当前地图状态的二维整数矩阵 存储每个位置的方块类型
     */
    int[][] matrix;

    /**
     * 初始地图状态的二维整数矩阵 用于重置游戏时恢复到初始状态
     */
    int[][] initialMatrix; // 保存初始地图状态

    /**
     * 当前关卡索引 用于标识当前加载的关卡编号
     */
    private int currentLevelIndex = 0;

    /**
     * 标记地图是否从存档加载
     */
    private boolean loadedFromSave = false;

    /**
     * 游戏模式常量
     */
    public static final int PRACTICE_MODE = 0; // 练习模式
    public static final int SPEED_MODE = 1;    // 竞速模式

    /**
     * 当前游戏模式
     */
    private int gameMode = PRACTICE_MODE; // 默认为练习模式

    /**
     * 构造函数，初始化地图模型
     *
     * @param matrix 初始地图数据的二维整数数组
     */
    public MapModel(int[][] matrix) {
        this.matrix = matrix;
        // 保存初始地图状态的深拷贝
        this.initialMatrix = new int[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            System.arraycopy(matrix[i], 0, initialMatrix[i], 0, matrix[i].length);
        }
    }

    /**
     * 构造函数，初始化地图模型并设置关卡索引
     *
     * @param matrix 初始地图数据的二维整数数组
     * @param levelIndex 关卡索引
     */
    public MapModel(int[][] matrix, int levelIndex) {
        this(matrix);
        this.currentLevelIndex = levelIndex;
    }

    /**
     * 获取地图的宽度（列数）
     *
     * @return 地图宽度
     */
    public int getWidth() {
        return this.matrix[0].length;
    }

    /**
     * 获取地图的高度（行数）
     *
     * @return 地图高度
     */
    public int getHeight() {
        return this.matrix.length;
    }

    /**
     * 获取指定位置的方块类型ID
     *
     * @param row 行索引
     * @param col 列索引
     * @return 方块类型ID：0-空白，1-单元格，2-水平方块，3-垂直方块，4-大方块
     */
    public int getId(int row, int col) {
        return matrix[row][col];
    }

    /**
     * 获取完整的地图矩阵
     *
     * @return 当前地图的二维整数数组
     */
    public int[][] getMatrix() {
        return matrix;
    }

    /**
     * 设置新的地图矩阵 用于从保存的游戏中加载地图状态
     *
     * @param newMatrix 新的地图矩阵
     */
    public void setMatrix(int[][] newMatrix) {
        this.matrix = newMatrix;
    }

    /**
     * 创建一个二维数组的深拷贝 用于在移动前保存当前状态，支持撤销功能
     *
     * @return 当前地图矩阵的深拷贝
     */
    public int[][] copyMatrix() {
        int[][] copy = new int[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            System.arraycopy(matrix[i], 0, copy[i], 0, matrix[i].length);
        }
        return copy;
    }

    /**
     * 检查指定列索引是否在地图宽度范围内
     *
     * @param col 列索引
     * @return 如果列索引有效返回true，否则返回false
     */
    public boolean checkInWidthSize(int col) {
        return col >= 0 && col < matrix[0].length;
    }

    /**
     * 检查指定行索引是否在地图高度范围内
     *
     * @param row 行索引
     * @return 如果行索引有效返回true，否则返回false
     */
    public boolean checkInHeightSize(int row) {
        return row >= 0 && row < matrix.length;
    }

    /**
     * 重置地图到初始状态 用于重新开始游戏时恢复地图
     */
    public void resetToInitialState() {
        for (int i = 0; i < matrix.length; i++) {
            System.arraycopy(initialMatrix[i], 0, matrix[i], 0, matrix[i].length);
        }
    }

    /**
     * 获取当前地图状态的序列化长整型表示 用于华容道求解器的计算
     *
     * @return 当前地图的序列化长整型表示
     */
    public long getSerializedLayout() {
        return BoardSerializer.serialize(this.matrix);
    }

    /**
     * 获取当前关卡索引
     *
     * @return 当前关卡索引
     */
    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    /**
     * 设置当前关卡索引
     *
     * @param index 关卡索引
     */
    public void setCurrentLevelIndex(int index) {
        this.currentLevelIndex = index;
    }

    /**
     * 设置是否从存档加载
     *
     * @param loadedFromSave 是否从存档加载
     */
    public void setLoadedFromSave(boolean loadedFromSave) {
        this.loadedFromSave = loadedFromSave;
    }

    /**
     * 检查是否从存档加载
     *
     * @return 是否从存档加载
     */
    public boolean isLoadedFromSave() {
        return loadedFromSave;
    }

    /**
     * 更新初始矩阵，用于从存档重置时设置新的初始状态
     *
     * @param newInitialMatrix 新的初始矩阵
     */
    public void updateInitialMatrix(int[][] newInitialMatrix) {
        if (newInitialMatrix == null || newInitialMatrix.length == 0 || newInitialMatrix[0].length == 0) {
            throw new IllegalArgumentException("Invalid matrix provided");
        }

        // 创建新的初始矩阵副本
        this.initialMatrix = new int[newInitialMatrix.length][newInitialMatrix[0].length];
        for (int i = 0; i < newInitialMatrix.length; i++) {
            System.arraycopy(newInitialMatrix[i], 0, initialMatrix[i], 0, newInitialMatrix[i].length);
        }

        System.out.println("Initial matrix has been updated");
    }

    /**
     * 获取当前游戏模式
     *
     * @return 当前游戏模式：PRACTICE_MODE 或 SPEED_MODE
     */
    public int getGameMode() {
        return gameMode;
    }

    /**
     * 设置游戏模式
     *
     * @param gameMode 游戏模式：PRACTICE_MODE 或 SPEED_MODE
     */
    public void setGameMode(int gameMode) {
        if (gameMode != PRACTICE_MODE && gameMode != SPEED_MODE) {
            throw new IllegalArgumentException("Invalid game mode");
        }
        this.gameMode = gameMode;
    }
}
