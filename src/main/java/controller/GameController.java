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
     * 根据方块所在位置和类型，调用对应的移动方法
     * 
     * @param row 当前方块的行索引
     * @param col 当前方块的列索引
     * @param direction 移动方向枚举(UP, DOWN, LEFT, RIGHT)
     * @return 移动是否成功执行
     */
    public boolean doMove(int row, int col, Direction direction) {
        // 获取当前位置方块的ID
        int blockId = model.getId(row, col);
        
        // 如果不是有效的方块ID，返回false
        if (blockId <= 0) {
            return false;
        }
        
        // 根据不同类型的方块处理移动
        switch (blockId) {
            case 1: // 1x1方块
                return moveSingleBlock(row, col, direction);
            case 2: // 2x1水平方块
                return moveHorizontalBlock(row, col, direction);
            case 3: // 1x2垂直方块
                return moveVerticalBlock(row, col, direction);
            case 4: // 2x2大方块
                return moveBigBlock(row, col, direction);
            default:
                return false;
        }
    }
    
    /**
     * 移动1x1单元方块
     * 判断目标位置是否可用，更新模型数据和视图位置
     * 
     * @param row 方块当前行索引
     * @param col 方块当前列索引
     * @param direction 移动方向
     * @return 是否成功移动
     */
    private boolean moveSingleBlock(int row, int col, Direction direction) {
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
            BoxComponent box = view.getSelectedBox();
            box.setRow(nextRow);  // 更新方块的行属性
            box.setCol(nextCol);  // 更新方块的列属性
            // 更新方块在面板中的实际像素位置（加2是为了边框偏移）
            box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
            box.repaint();  // 重绘方块
            
            return true;  // 移动成功
        }
        return false;  // 移动失败
    }
    
    /**
     * 移动2x1水平方块
     * 水平方块占据两个水平相邻的格子
     * 
     * @param row 方块当前行索引
     * @param col 方块当前列索引
     * @param direction 移动方向
     * @return 是否成功移动
     */
    private boolean moveHorizontalBlock(int row, int col, Direction direction) {
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
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(nextRow);
                    box.setCol(nextCol);
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
                    return true;
                }
            } else if (direction == Direction.RIGHT) {
                // 向右移动：检查最右侧的下一格是否可用
                if (model.checkInWidthSize(col + 2) && model.getId(row, col + 2) == 0) {
                    // 更新模型数据
                    model.getMatrix()[row][col] = 0;
                    model.getMatrix()[row][col + 1] = 0;
                    model.getMatrix()[row][col + 1] = 2;
                    model.getMatrix()[row][col + 2] = 2;
                    
                    // 更新视图
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(row);
                    box.setCol(col + 1);
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
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
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(nextRow);
                    box.setCol(nextCol);
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
                    return true;
                }
            }
        }
        return false;  // 无法移动
    }
    
    /**
     * 移动1x2垂直方块
     * 垂直方块占据两个垂直相邻的格子
     * 
     * @param row 方块当前行索引
     * @param col 方块当前列索引
     * @param direction 移动方向
     * @return 是否成功移动
     */
    private boolean moveVerticalBlock(int row, int col, Direction direction) {
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
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(nextRow);
                    box.setCol(nextCol);
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
                    return true;
                }
            } else if (direction == Direction.DOWN) {
                // 向下移动：检查最下方的下一格是否可用
                if (model.checkInHeightSize(row + 2) && model.getId(row + 2, col) == 0) {
                    // 更新模型数据
                    model.getMatrix()[row][col] = 0;  // 清除原顶部位置
                    model.getMatrix()[row + 1][col] = 0;  // 清除原底部位置
                    model.getMatrix()[row + 1][col] = 3;  // 设置新顶部位置
                    model.getMatrix()[row + 2][col] = 3;  // 设置新底部位置
                    
                    // 更新视图
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(row + 1);
                    box.setCol(col);
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
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
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(nextRow);
                    box.setCol(nextCol);
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
                    return true;
                }
            }
        }
        return false;  // 无法移动
    }
    
    /**
     * 移动2x2大方块
     * 大方块占据2x2=4个格子
     * 
     * @param row 方块当前行索引（左上角位置）
     * @param col 方块当前列索引（左上角位置）
     * @param direction 移动方向
     * @return 是否成功移动
     */
    private boolean moveBigBlock(int row, int col, Direction direction) {
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
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(row);
                    box.setCol(col - 1);  // 左移一格
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
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
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(row);
                    box.setCol(col + 1);  // 右移一格
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
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
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(row - 1);  // 上移一格
                    box.setCol(col);
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
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
                    BoxComponent box = view.getSelectedBox();
                    box.setRow(row + 1);  // 下移一格
                    box.setCol(col);
                    box.setLocation(box.getCol() * view.getGRID_SIZE() + 2, box.getRow() * view.getGRID_SIZE() + 2);
                    box.repaint();
                    
                    return true;
                }
            }
        }
        return false;  // 无法移动
    }

    //todo: 添加其他方法如loadGame, saveGame等游戏功能
}
