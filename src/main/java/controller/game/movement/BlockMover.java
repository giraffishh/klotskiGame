package controller.game.movement;

import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
import view.game.GamePanel;

/**
 * 方块移动策略接口
 * 定义了不同类型方块的移动行为
 */
public interface BlockMover {
    
    /**
     * 执行方块移动操作
     * 
     * @param row 方块当前行索引
     * @param col 方块当前列索引
     * @param direction 移动方向
     * @param model 地图数据模型
     * @param view 游戏面板
     * @param selectedBox 当前选中的方块组件
     * @return 移动是否成功执行
     */
    boolean move(int row, int col, Direction direction, MapModel model, GamePanel view, BoxComponent selectedBox);
}
