package controller;

import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
import view.game.GamePanel;

/**
 * 该类作为GamePanel(视图)和MapMatrix(模型)之间的桥梁，实现MVC设计模式中的控制器。
 * 负责处理游戏逻辑，如移动方块、重启游戏等操作。
 */
public class GameController {
    // 游戏视图组件引用
    private final GamePanel view;
    // 游戏地图模型引用
    private final MapModel model;

    /**
     * 构造函数初始化控制器，建立视图和模型之间的连接
     * @param view 游戏面板视图
     * @param model 地图数据模型
     */
    public GameController(GamePanel view, MapModel model) {
        this.view = view;
        this.model = model;
        view.setController(this); // 将当前控制器设置到视图中，使视图能够调用控制器方法
    }

    /**
     * 重新开始游戏的方法
     * 目前仅打印消息，需要进一步实现重置游戏状态的逻辑
     */
    public void restartGame() {
        System.out.println("Do restart game here");
    }

    /**
     * 执行移动操作
     * @param row 当前行索引
     * @param col 当前列索引
     * @param direction 移动方向
     * @return 移动是否成功
     */
    public boolean doMove(int row, int col, Direction direction) {
        // 检查当前位置是否为可移动方块(值为1)
        if (model.getId(row, col) == 1) {
            // 计算目标位置
            int nextRow = row + direction.getRow();
            int nextCol = col + direction.getCol();
            
            // 检查目标位置是否在地图范围内
            if (model.checkInHeightSize(nextRow) && model.checkInWidthSize(nextCol)) {
                // 检查目标位置是否为空(值为0)
                if (model.getId(nextRow, nextCol) == 0) {
                    // 更新模型数据
                    model.getMatrix()[row][col] = 0;
                    model.getMatrix()[nextRow][nextCol] = 1;
                    
                    // 更新视图中的方块位置
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(nextRow);
                    box.setCol(nextCol);
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
                    return true;
                }
            }
        }
        return false;
    }

    //todo: 添加其他方法如loadGame, saveGame等游戏功能

}
