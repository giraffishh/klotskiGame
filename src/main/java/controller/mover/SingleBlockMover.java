package controller.mover;

import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
import view.game.GamePanel;

/**
 * 1x1单元方块移动策略实现
 */
public class SingleBlockMover implements BlockMover {
    
    @Override
    public boolean move(int row, int col, Direction direction, MapModel model, GamePanel view, BoxComponent selectedBox) {
        // 计算移动后的目标位置
        int nextRow = row + direction.getRow();
        int nextCol = col + direction.getCol();
        
        // 检查目标位置是否在地图范围内且为空
        if (model.checkInHeightSize(nextRow) && model.checkInWidthSize(nextCol) && 
            model.getId(nextRow, nextCol) == 0) {
            
            // 更新模型数据：将方块从原位置移除，放置到新位置
            model.getMatrix()[row][col] = 0;  // 原位置设为空
            model.getMatrix()[nextRow][nextCol] = 1;  // 新位置设为1x1方块
            
            // 更新视图中的方块位置
            selectedBox.setRow(nextRow);  // 更新方块的行属性
            selectedBox.setCol(nextCol);  // 更新方块的列属性
            // 更新方块在面板中的实际像素位置（加2是为了边框偏移）
            selectedBox.setLocation(selectedBox.getCol() * view.getGRID_SIZE() + 2, 
                                   selectedBox.getRow() * view.getGRID_SIZE() + 2);
            selectedBox.repaint();  // 重绘方块
            
            return true;  // 移动成功
        }
        return false;  // 移动失败
    }
}
