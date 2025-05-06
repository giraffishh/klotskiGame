package controller.game.history;

import model.Direction;

/**
 * 移动记录类
 * 用于存储每次移动的前后状态和相关信息
 */
public class MoveRecord {
    private final int[][] beforeState;  // 移动前的地图状态
    private final int[][] afterState;   // 移动后的地图状态
    private final int originalRow;      // 移动前的行位置
    private final int originalCol;      // 移动前的列位置
    private final int newRow;           // 移动后的行位置
    private final int newCol;           // 移动后的列位置
    private final int blockId;          // 方块ID
    private final Direction direction;  // 移动方向

    public MoveRecord(int[][] beforeState, int[][] afterState, int originalRow, int originalCol,
                      int newRow, int newCol, int blockId, Direction direction) {
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.originalRow = originalRow;
        this.originalCol = originalCol;
        this.newRow = newRow;
        this.newCol = newCol;
        this.blockId = blockId;
        this.direction = direction;
    }

    public int[][] getBeforeState() {
        return beforeState;
    }

    public int[][] getAfterState() {
        return afterState;
    }

    public int getOriginalRow() {
        return originalRow;
    }

    public int getOriginalCol() {
        return originalCol;
    }

    public int getNewRow() {
        return newRow;
    }

    public int getNewCol() {
        return newCol;
    }

    public int getBlockId() {
        return blockId;
    }

    public Direction getDirection() {
        return direction;
    }
}