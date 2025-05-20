package controller.game.history;

/**
 * 移动记录类 用于存储每次移动的前后状态和相关信息
 */
public class MoveRecord {

    private final int[][] beforeState;  // 移动前的地图状态
    private final long layoutAfterMove;   // 移动后的地图布局（序列化）

    public MoveRecord(int[][] beforeState, long layoutAfterMove) {
        this.beforeState = beforeState;
        this.layoutAfterMove = layoutAfterMove;
    }

    public int[][] getBeforeState() {
        return beforeState;
    }

    public long getLayoutAfterMove() {
        return layoutAfterMove;
    }
}
