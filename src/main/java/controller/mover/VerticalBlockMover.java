package controller.mover;

import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
import view.game.GamePanel;

/**
 * 1x2垂直方块移动策略实现
 */
public class VerticalBlockMover implements BlockMover {
    
    @Override
    public boolean move(int row, int col, Direction direction, MapModel model, GamePanel view, BoxComponent selectedBox) {
        // 计算移动后的目标位置
        int nextRow = row + direction.getRow();
        int nextCol = col + direction.getCol();
        
        // 确保当前位置是垂直方块的顶部起点
        if (row + 1 < model.getHeight() && model.getId(row + 1, col) == 3) {
            // 根据方向检查移动是否可行
            if (direction == Direction.UP) {
                // 向上移动：检查上方一格是否可用
                if (model.checkInHeightSize(nextRow) && model.getId(nextRow, nextCol) == 0) {
                    // 更新模型数据
                    model.getMatrix()[row][col] = 0;  // 清除原顶部位置
                    model.getMatrix()[row + 1][col] = 0;  // 清除原底部位置
                    model.getMatrix()[nextRow][col] = 3;  // 设置新顶部位置
                    model.getMatrix()[nextRow + 1][col] = 3;  // 设置新底部位置
                    
                    // 更新视图
                    selectedBox.setRow(nextRow);
                    selectedBox.setCol(nextCol);
                    selectedBox.setLocation(selectedBox.getCol() * view.getGRID_SIZE() + 2, 
                                           selectedBox.getRow() * view.getGRID_SIZE() + 2);
                    selectedBox.repaint();
                    
                    return true;
                }
            } else if (direction == Direction.DOWN) {
                // 向下移动：检查最下方的下一格是否可用
                if (model.checkInHeightSize(row + 2) && model.getId(row + 2, col) == 0) {
                    // 更新模型数据
                    model.getMatrix()[row][col] = 0;  // 清除原顶部位置
                    model.getMatrix()[row + 1][col] = 3;  // 设置新顶部位置
                    model.getMatrix()[row + 2][col] = 3;  // 设置新底部位置
                    
                    // 更新视图
                    selectedBox.setRow(row + 1);
                    selectedBox.setCol(col);
                    selectedBox.setLocation(selectedBox.getCol() * view.getGRID_SIZE() + 2, 
                                           selectedBox.getRow() * view.getGRID_SIZE() + 2);
                    selectedBox.repaint();
                    
                    return true;
                }
            } else if (direction == Direction.LEFT || direction == Direction.RIGHT) {
                // 左右移动：检查移动方向上的两个位置是否都可用
                int nextRow2 = nextRow + 1;  // 垂直方块底部位置的行
                int nextCol2 = nextCol;  // 垂直方块底部位置的列
                
                if (model.checkInHeightSize(nextRow) && model.checkInWidthSize(nextCol) && 
                    model.checkInHeightSize(nextRow2) && model.checkInWidthSize(nextCol2) &&
                    model.getId(nextRow, nextCol) == 0 && model.getId(nextRow2, nextCol2) == 0) {
                    
                    // 更新模型数据
                    model.getMatrix()[row][col] = 0;
                    model.getMatrix()[row + 1][col] = 0;
                    model.getMatrix()[nextRow][nextCol] = 3;
                    model.getMatrix()[nextRow + 1][nextCol] = 3;
                    
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
