package model;

/**
 * 方向枚举，定义了四个移动方向（左、上、右、下）
 * 每个方向包含行和列的相对位移值
 */
public enum Direction {
    LEFT(0, -1),   // 向左移动，行不变，列减1
    UP(-1, 0),     // 向上移动，行减1，列不变
    RIGHT(0, 1),   // 向右移动，行不变，列加1
    DOWN(1, 0),    // 向下移动，行加1，列不变
    ;
    private final int row;  // 行方向位移
    private final int col;  // 列方向位移

    /**
     * 构造方法，设置行列位移
     * @param row 行位移
     * @param col 列位移
     */
    Direction(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * 获取行位移
     * @return 行位移值
     */
    public int getRow() {
        return row;
    }

    /**
     * 获取列位移
     * @return 列位移值
     */
    public int getCol() {
        return col;
    }
}
