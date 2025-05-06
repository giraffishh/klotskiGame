package controller.game.movement;

import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
import view.game.GamePanel;

/**
 * 2x1水平方块移动策略实现
 */
public class HorizontalBlockMover implements BlockMover {
    
    @Override
    public boolean move(int row, int col, Direction direction, MapModel model, GamePanel view, BoxComponent selectedBox) {
        // 计算移动后的目标位置
        int nextRow = row + direction.getRow();
        int nextCol = col + direction.getCol();
        
        // 确保当前位置是水平方块的左侧起点
        if (col + 1 < model.getWidth() && model.getId(row, col + 1) == 2) {
            // 根据方向检查移动是否可行
            if (direction == Direction.LEFT) {
                // 向左移动：检查左侧一格是否可用
                if (model.checkInWidthSize(nextCol) && model.getId(nextRow, nextCol) == 0) {
                    // 更新模型数据：清除原位置，设置新位置
                    model.getMatrix()[row][col] = 0;
                    model.getMatrix()[row][col + 1] = 0;
                    model.getMatrix()[row][nextCol] = 2;
                    model.getMatrix()[row][nextCol + 1] = 2;
                    
                    // 更新视图
                    selectedBox.setRow(nextRow);
                    selectedBox.setCol(nextCol);
                    selectedBox.setLocation(selectedBox.getCol() * view.getGRID_SIZE() + 2, 
                                           selectedBox.getRow() * view.getGRID_SIZE() + 2);
                    selectedBox.repaint();
                    
                    return true;
                }
            } else if (direction == Direction.RIGHT) {
                // 向右移动：检查最右侧的下一格是否可用
                if (model.checkInWidthSize(col + 2) && model.getId(row, col + 2) == 0) {
                    // 更新模型数据
                    model.getMatrix()[row][col] = 0;
                    model.getMatrix()[row][col + 1] = 2;
                    model.getMatrix()[row][col + 2] = 2;
                    
                    // 更新视图
                    selectedBox.setRow(row);
                    selectedBox.setCol(col + 1);
                    selectedBox.setLocation(selectedBox.getCol() * view.getGRID_SIZE() + 2, 
                                           selectedBox.getRow() * view.getGRID_SIZE() + 2);
                    selectedBox.repaint();
                    
                    return true;
                }
            } else if (direction == Direction.UP || direction == Direction.DOWN) {
                // 上下移动：检查移动方向上的两个位置是否都可用（整个2x1方块需要同时移动）
                int nextRow2 = nextRow;
                int nextCol2 = nextCol + 1;
                
                if (model.checkInHeightSize(nextRow) && model.checkInWidthSize(nextCol) && 
                    model.checkInHeightSize(nextRow2) && model.checkInWidthSize(nextCol2) &&
                    model.getId(nextRow, nextCol) == 0 && model.getId(nextRow2, nextCol2) == 0) {
                    
                    // 更新模型数据
                    model.getMatrix()[row][col] = 0;
                    model.getMatrix()[row][col + 1] = 0;
                    model.getMatrix()[nextRow][nextCol] = 2;
                    model.getMatrix()[nextRow][nextCol + 1] = 2;
                    
                    // 更新视图
                    selectedBox.setRow(nextRow);
                    selectedBox.setCol(nextCol);
                    selectedBox.setLocation(selectedBox.getCol() * view.getGRID_SIZE() + 2, 
                                           selectedBox.getRow() * view.getGRID_SIZE() + 2);
                    selectedBox.repaint();
                    
                    return true;
                }
            }
        }
        return false;  // 无法移动
    }
}
