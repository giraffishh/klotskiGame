package controller;

import model.Direction;
import model.MapModel;
import service.DatabaseService;
import service.UserSession;
import view.game.BoxComponent;
import view.game.GamePanel;
import view.util.FontManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.Font;

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
     * 重置地图模型到初始状态，并重置游戏面板
     */
    public void restartGame() {
        System.out.println("Restarting game...");

        // 重置地图模型到初始状态
        model.resetToInitialState();

        // 重置游戏面板
        view.resetGame();

        System.out.println("Game restarted successfully");
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
        return switch (blockId) {
            case 1 -> // 1x1方块
                    moveSingleBlock(row, col, direction);
            case 2 -> // 2x1水平方块
                    moveHorizontalBlock(row, col, direction);
            case 3 -> // 1x2垂直方块
                    moveVerticalBlock(row, col, direction);
            case 4 -> // 2x2大方块
                    moveBigBlock(row, col, direction);
            default -> false;
        };
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

    /**
     * 加载游戏存档
     * 从数据库中读取存档并验证完整性
     *
     * @return 加载是否成功
     */
    public boolean loadGameState() {
        // 检查用户是否已登录
        if (!UserSession.getInstance().isLoggedIn()) {
            System.out.println("Unable to load game: Login status is abnormal");
            JOptionPane.showMessageDialog(view,
                    "Unable to load game: You are not logged in",
                    "Load Failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // 获取当前登录用户名
        String username = UserSession.getInstance().getCurrentUser().getUsername();

        // 调用数据库服务加载游戏状态
        DatabaseService.GameSaveData saveData = DatabaseService.getInstance().loadGameSave(username);

        if (saveData == null) {
            JOptionPane.showMessageDialog(view,
                    "No valid save found or save data is corrupted",
                    "Load Failed",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            // 创建一个包含存档信息的确认对话框
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String saveTimeStr = dateFormat.format(saveData.getSaveTime());

            String message = String.format("Save Information:\n" +
                                          " Steps: %d\n" +
                                          " Save Time: %s\n\n" +
                                          "Are you sure you want to load this save?\nCurrent progress will be lost.",
                                          saveData.getSteps(), saveTimeStr);

            int choice = JOptionPane.showConfirmDialog(
                view,
                message,
                "Confirm Load",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            // 如果用户取消了加载，则返回
            if (choice != JOptionPane.YES_OPTION) {
                return false;
            }

            // 用户确认加载，继续处理存档数据

            // 解析地图状态字符串到二维数组
            String mapState = saveData.getMapState();
            int steps = saveData.getSteps();

            // 移除方括号
            mapState = mapState.substring(1, mapState.length() - 1);
            String[] rows = mapState.split("],\\[");

            // 修正第一行和最后一行的格式
            rows[0] = rows[0].substring(1);
            rows[rows.length - 1] = rows[rows.length - 1].substring(0, rows[rows.length - 1].length() - 1);

            int[][] newMatrix = new int[rows.length][];
            for (int i = 0; i < rows.length; i++) {
                String[] cols = rows[i].split(",");
                newMatrix[i] = new int[cols.length];
                for (int j = 0; j < cols.length; j++) {
                    newMatrix[i][j] = Integer.parseInt(cols[j].trim());
                }
            }

            // 更新模型数据
            model.setMatrix(newMatrix);

            // 重置游戏面板显示新地图
            view.resetGame();

            // 设置已加载的步数
            view.setSteps(steps);

            JOptionPane.showMessageDialog(view,
                    "Game loaded successfully!",
                    "Load Success",
                    JOptionPane.INFORMATION_MESSAGE);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view,
                    "Error parsing saved game data: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * 保存当前游戏状态到数据库
     * 检查用户是否有已存在的存档，提示新建或覆盖
     * @return 保存是否成功
     */
    public boolean saveGameState() {
        // 检查用户是否已登录
        if (!UserSession.getInstance().isLoggedIn()) {
            System.out.println("Unable to save game: Login status is abnormal");
            return false;
        }

        // 获取当前登录用户名
        String username = UserSession.getInstance().getCurrentUser().getUsername();

        // 将地图状态转换为字符串
        StringBuilder mapStateBuilder = new StringBuilder("[");
        int[][] matrix = model.getMatrix();
        for (int i = 0; i < matrix.length; i++) {
            mapStateBuilder.append("[");
            for (int j = 0; j < matrix[i].length; j++) {
                mapStateBuilder.append(matrix[i][j]);
                if (j < matrix[i].length - 1) {
                    mapStateBuilder.append(",");
                }
            }
            mapStateBuilder.append("]");
            if (i < matrix.length - 1) {
                mapStateBuilder.append(",");
            }
        }
        mapStateBuilder.append("]");
        String mapState = mapStateBuilder.toString();

        // 获取当前步数
        int steps = view.getSteps();

        // 生成存档描述信息（使用当前日期时间）
        String description = "Saved at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 判断用户是否已有存档
        boolean hasExistingSave = hasUserSave();
        String message;
        String title;

        if (hasExistingSave) {
            message = "You already have a saved game. Do you want to overwrite it?";
            title = "Overwrite Save";
        } else {
            message = "Do you want to create a new save?";
            title = "Create Save";
        }

        int result = JOptionPane.showConfirmDialog(view, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // 调用数据库服务保存游戏状态
            boolean saved = DatabaseService.getInstance().saveGameState(username, mapState, steps, description);

            // 显示保存结果
            if (saved) {
                JOptionPane.showMessageDialog(view,
                        hasExistingSave ? "Save successfully overwritten !" : "New save successfully created!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(view,
                        "Save failed. Please make sure you are logged in.",
                        "Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
            
            return saved;
        }
        
        return false;
    }

    /**
     * 检查当前用户是否已有存档
     *
     * @return 用户是否已有存档
     */
    public boolean hasUserSave() {
        if (!UserSession.getInstance().isLoggedIn()) {
            return false;
        }

        String username = UserSession.getInstance().getCurrentUser().getUsername();
        return DatabaseService.getInstance().hasUserGameSave(username);
    }
}

