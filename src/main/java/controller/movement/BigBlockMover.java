package controller.movement;

import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
import view.game.GamePanel;

/**
 * 2x2大方块移动策略实现
 */
public class BigBlockMover implements BlockMover {
    
    @Override
    public boolean move(int row, int col, Direction direction, MapModel model, GamePanel view, BoxComponent selectedBox) {
        // 计算移动后的目标位置
        int nextRow = row + direction.getRow();
        int nextCol = col + direction.getCol();
        
        // 确保当前位置是2x2方块的左上角起点
        if (row + 1 < model.getHeight() && col + 1 < model.getWidth() && 
            model.getId(row + 1, col) == 4 && model.getId(row, col + 1) == 4 && model.getId(row + 1, col + 1) == 4) {
            
            // 根据移动方向检查所需的新位置是否可用
            if (direction == Direction.LEFT) {
                // 向左移动：检查左侧两个位置是否可用
                if (model.checkInWidthSize(col - 1) && 
                    model.getId(row, col - 1) == 0 && model.getId(row + 1, col - 1) == 0) {
                    
                    // 更新模型数据
                    // 清除原右侧位置
                    model.getMatrix()[row][col + 1] = 0;
                    model.getMatrix()[row + 1][col + 1] = 0;
                    // 设置新左侧位置
                    model.getMatrix()[row][col - 1] = 4;
                    model.getMatrix()[row + 1][col - 1] = 4;
                    // 保留原左侧位置（现为中间位置）
                    model.getMatrix()[row][col] = 4;
                    model.getMatrix()[row + 1][col] = 4;
                    
                    // 更新视图
                    selectedBox.setRow(row);
                    selectedBox.setCol(col - 1);  // 左移一格
                    selectedBox.setLocation(selectedBox.getCol() * view.getGRID_SIZE() + 2, 
                                           selectedBox.getRow() * view.getGRID_SIZE() + 2);
                    selectedBox.repaint();
                    
                    return true;
                }
            } else if (direction == Direction.RIGHT) {
                // 向右移动：检查右侧两个位置是否可用
                if (model.checkInWidthSize(col + 2) && 
                    model.getId(row, col + 2) == 0 && model.getId(row + 1, col + 2) == 0) {
                    
                    // 更新模型数据
                    // 清除原左侧位置
                    model.getMatrix()[row][col] = 0;
                    model.getMatrix()[row + 1][col] = 0;
                    // 现有右侧位置变为左侧位置
                    model.getMatrix()[row][col + 1] = 4;
                    model.getMatrix()[row + 1][col + 1] = 4;
                    // 设置新右侧位置
                    model.getMatrix()[row][col + 2] = 4;
                    model.getMatrix()[row + 1][col + 2] = 4;
                    
                    // 更新视图
                    selectedBox.setRow(row);
                    selectedBox.setCol(col + 1);  // 右移一格
                    selectedBox.setLocation(selectedBox.getCol() * view.getGRID_SIZE() + 2, 
                                           selectedBox.getRow() * view.getGRID_SIZE() + 2);
                    selectedBox.repaint();
                    
                    return true;
                }
            } else if (direction == Direction.UP) {
                // 向上移动：检查上方两个位置是否可用
                if (model.checkInHeightSize(row - 1) && 
                    model.getId(row - 1, col) == 0 && model.getId(row - 1, col + 1) == 0) {
                    
                    // 更新模型数据
                    // 清除原底部位置
                    model.getMatrix()[row + 1][col] = 0;
                    model.getMatrix()[row + 1][col + 1] = 0;
                    // 设置新顶部位置
                    model.getMatrix()[row - 1][col] = 4;
                    model.getMatrix()[row - 1][col + 1] = 4;
                    // 保留原顶部位置（现为中间位置）
                    model.getMatrix()[row][col] = 4;
                    model.getMatrix()[row][col + 1] = 4;
                    
                    // 更新视图
                    selectedBox.setRow(row - 1);  // 上移一格
                    selectedBox.setCol(col);
                    selectedBox.setLocation(selectedBox.getCol() * view.getGRID_SIZE() + 2, 
                                           selectedBox.getRow() * view.getGRID_SIZE() + 2);
                    selectedBox.repaint();
                    
                    return true;
                }
            } else if (direction == Direction.DOWN) {
                // 向下移动：检查下方两个位置是否可用
                if (model.checkInHeightSize(row + 2) && 
                    model.getId(row + 2, col) == 0 && model.getId(row + 2, col + 1) == 0) {
                    
                    // 更新模型数据
                    // 清除原顶部位置
                    model.getMatrix()[row][col] = 0;
                    model.getMatrix()[row][col + 1] = 0;
                    // 现有底部位置变为顶部位置
                    model.getMatrix()[row + 1][col] = 4;
                    model.getMatrix()[row + 1][col + 1] = 4;
                    // 设置新底部位置
                    model.getMatrix()[row + 2][col] = 4;
                    model.getMatrix()[row + 2][col + 1] = 4;
                    
                    // 更新视图
                    selectedBox.setRow(row + 1);  // 下移一格
                    selectedBox.setCol(col);
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
