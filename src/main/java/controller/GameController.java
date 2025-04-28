package controller;

import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
import view.game.GamePanel;
import controller.solver.KlotskiSolver;
import controller.solver.BoardState;
import controller.history.HistoryManager;

// 导入移动策略类
import controller.mover.BlockMover;
import controller.mover.SingleBlockMover;
import controller.mover.HorizontalBlockMover;
import controller.mover.VerticalBlockMover;
import controller.mover.BigBlockMover;

// 导入格子布局序列化工具类和存档管理器
import controller.save.SaveManager;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;

import java.util.List;
import view.game.GameFrame;
import view.level.LevelSelectFrame;
import view.util.FontManager;
import view.victory.VictoryView;

/**
 * 该类作为GamePanel(视图)和MapMatrix(模型)之间的桥梁，实现MVC设计模式中的控制器。
 * 负责处理游戏逻辑，如移动方块、重启游戏等操作。
 */
public class GameController {
    // 游戏视图组件引用
    private final GamePanel view;
    // 游戏地图模型引用
    private final MapModel model;
    
    // 方块移动策略对象
    private final BlockMover singleBlockMover;
    private final BlockMover horizontalBlockMover;
    private final BlockMover verticalBlockMover;
    private final BlockMover bigBlockMover;

    // 游戏存档管理器
    private final SaveManager saveManager;

    // 华容道求解器
    private KlotskiSolver solver;

    // 历史记录管理
    private final HistoryManager historyManager;

    // 父窗口引用，用于更新按钮状态
    private GameFrame parentFrame;

    // 游戏胜利状态标志，防止重复弹出胜利提示
    private boolean victoryAchieved = false;

    // 胜利界面引用
    private VictoryView victoryView;

    // 选关界面引用
    private LevelSelectFrame levelSelectFrame;

    // 当前关卡索引
    private int currentLevelIndex = 0;

    /**
     * 构造函数初始化控制器，建立视图和模型之间的连接
     * @param view 游戏面板视图
     * @param model 地图数据模型
     */
    public GameController(GamePanel view, MapModel model) {
        this.view = view;
        this.model = model;
        view.setController(this); // 将当前控制器设置到视图中，使视图能够调用控制器方法
        
        // 初始化方块移动策略
        this.singleBlockMover = new SingleBlockMover();
        this.horizontalBlockMover = new HorizontalBlockMover();
        this.verticalBlockMover = new VerticalBlockMover();
        this.bigBlockMover = new BigBlockMover();

        // 初始化游戏状态管理器
        this.saveManager = new SaveManager(view, model);

        // 设置加载完成后更新最短步数的回调
        this.saveManager.setOnLoadCompleteCallback(this::updateMinStepsDisplay);

        // 初始化华容道求解器，但不立即计算和更新显示
        this.solver = new KlotskiSolver();

        // 初始化历史记录管理器
        this.historyManager = new HistoryManager(view, model);
    }

    /**
     * 设置父窗口引用，用于更新UI按钮状态
     * @param frame 父窗口
     */
    public void setParentFrame(GameFrame frame) {
        this.parentFrame = frame;
        // 将父窗口引用也传递给历史管理器
        historyManager.setParentFrame(frame);
    }

    /**
     * 设置胜利视图
     * @param victoryView 胜利界面视图
     */
    public void setVictoryView(VictoryView victoryView) {
        this.victoryView = victoryView;
        // 为胜利界面设置按钮监听器
        setupVictoryListeners();
    }

    /**
     * 设置关卡选择界面
     * @param levelSelectFrame 关卡选择界面
     */
    public void setLevelSelectFrame(LevelSelectFrame levelSelectFrame) {
        this.levelSelectFrame = levelSelectFrame;
    }

    /**
     * 设置当前关卡索引
     * @param index 关卡索引
     */
    public void setCurrentLevelIndex(int index) {
        this.currentLevelIndex = index;
    }

    /**
     * 设置胜利界面的按钮监听器
     */
    private void setupVictoryListeners() {
        if (victoryView == null) return;

        // 设置回到主页按钮监听器 - 直接返回，不显示确认对话框
        victoryView.setHomeListener(e -> {
            if (parentFrame != null) {
                victoryView.hideVictory();
                parentFrame.returnToHomeDirectly(); // 使用直接返回方法，不显示确认对话框
            }
        });

        // 设置关卡选择按钮监听器
        victoryView.setLevelSelectListener(e -> {
            victoryView.hideVictory();
            // 显示关卡选择界面
            if (levelSelectFrame != null) {
                levelSelectFrame.showLevelSelect();
            } else {
                System.err.println("Level selection frame reference is not set");
            }
        });

        // 设置再来一次按钮监听器
        victoryView.setRestartListener(e -> {
            victoryView.hideVictory();
            restartGame();
        });

        // 设置下一关按钮监听器
        victoryView.setNextLevelListener(e -> {
            // 增加检查：如果当前是最后一关，不执行任何操作
            if (!isLastLevel()) {
                // 先隐藏胜利界面，再加载下一关
                victoryView.hideVictory();
                SwingUtilities.invokeLater(this::loadNextLevel); // 使用invokeLater确保UI更新完成后再加载
            }
        });
    }

    /**
     * 初始化游戏，在UI组件完全准备好后调用
     * 这个方法应在GameFrame完成所有UI元素设置后调用
     */
    public void initializeGame() {
        // 初始化华容道求解器并计算最优解
        initializeSolver();
        
        // 确保更新最短步数显示
        updateMinStepsDisplay();

        // 清空历史记录
        clearHistory();
    }

    /**
     * 初始化华容道求解器并预先计算最优解
     * 将此逻辑抽取为单独方法，以便在构造函数和加载游戏后调用
     */
    private void initializeSolver() {
        System.out.println("=== Initializing Klotski Solver ===");

        // 获取当前布局并预先计算最优解
        BoardState initialState = new BoardState(model.getSerializedLayout());

        // 记录求解开始时间
        long startTime = System.currentTimeMillis();

        // 执行初始求解
        List<BoardState> solution = solver.initialSolve(initialState);

        // 记录求解结束时间
        long endTime = System.currentTimeMillis();

        // 输出求解统计信息
        System.out.println("Solving time: " + (endTime - startTime) + " ms");
        System.out.println("BFS nodes explored: " + solver.getNodesExploredBFS());

        // 输出求解结果
        if (solution != null && !solution.isEmpty()) {
            System.out.println("Optimal solution found, steps: " + (solution.size() - 1));
        } else {
            System.out.println("No solution found");
        }
        System.out.println("==============================");
    }

    /**
     * 重新开始游戏的方法
     */
    public void restartGame() {
        System.out.println("Restarting game...");

        // 重置地图模型到初始状态
        model.resetToInitialState();

        // 重置游戏面板
        view.resetGame();

        // 重置后更新最短步数显示
        this.solver = new KlotskiSolver();
        initializeSolver();
        updateMinStepsDisplay();

        // 清空历史记录
        historyManager.clearHistory();

        // 重置胜利状态
        victoryAchieved = false;

        // 隐藏胜利界面
        if (victoryView != null) {
            victoryView.hideVictory();
        }

        System.out.println("Game restarted successfully");
    }

    /**
     * 加载下一关
     */
    public void loadNextLevel() {
        // 添加前置检查：如果当前已经是最后一关，直接显示提示
        if (isLastLevel()) {
            // 只显示一个"确定"按钮，点击后直接返回主页
            JLabel messageLabel = new JLabel("Congratulations! You have completed all levels!");
            messageLabel.setFont(FontManager.getRegularFont(16));

            JOptionPane.showMessageDialog(
                    parentFrame,
                    messageLabel,
                    "Game Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

            // 直接返回主页，不需要用户选择
            if (parentFrame != null) {
                parentFrame.returnToHomeDirectly();
            }
            return; // 直接返回，不执行后续加载逻辑
        }

        // 原有的加载下一关逻辑
        if (levelSelectFrame != null) {
            LevelSelectController levelController = levelSelectFrame.getController();
            if (levelController != null) {
                // 首先隐藏胜利界面（确保界面关闭后再加载新关卡）
                if (victoryView != null) {
                    victoryView.hideVictory();
                }

                // 使用我们新增的方法尝试加载下一关
                boolean success = levelController.loadNextLevel(currentLevelIndex);

                if (success) {
                    // 更新当前关卡索引
                    currentLevelIndex = levelController.getNextLevelIndex(currentLevelIndex);
                } else {
                    // 这部分逻辑理论上不会执行到，因为前面的isLastLevel()检查已经处理了
                    // 但为了代码健壮性，显示确认提示并直接返回主页
                    JLabel messageLabel = new JLabel("Congratulations! You have completed all levels!");
                    messageLabel.setFont(FontManager.getRegularFont(16));

                    JOptionPane.showMessageDialog(
                            parentFrame,
                            messageLabel,
                            "Game Complete",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    // 直接返回主页
                    if (parentFrame != null) {
                        parentFrame.returnToHomeDirectly();
                    }
                }
            } else {
                // 用户友好的错误提示
                if (parentFrame != null) {
                    JOptionPane.showMessageDialog(
                            parentFrame,
                            "Unable to load next level. Please return to level selection.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } else {
                    System.err.println("Level selection controller is null");
                }
            }
        } else {
            // 用户友好的错误提示
            if (parentFrame != null) {
                JOptionPane.showMessageDialog(
                        parentFrame,
                        "Unable to load next level. Level selection frame not initialized.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            } else {
                System.err.println("Level selection frame reference is not set");
            }
        }
    }

    /**
     * 检查当前是否为最后一关
     * @return 如果是最后一关返回true，否则返回false
     */
    private boolean isLastLevel() {
        if (levelSelectFrame != null) {
            LevelSelectController levelController = levelSelectFrame.getController();
            if (levelController != null) {
                return !levelController.hasNextLevel(currentLevelIndex);
            }
        }
        // 如果无法确定，为安全起见，假设是最后一关
        return true;
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
        
        // 获取当前选中的方块组件
        BoxComponent selectedBox = view.getSelectedBox();
        
        boolean moved = false;

        // 在移动前保存当前地图状态
        int[][] beforeState = model.copyMatrix();
        int originalRow = -1;
        int originalCol = -1;

        if (selectedBox != null) {
            originalRow = selectedBox.getRow();
            originalCol = selectedBox.getCol();
        }

        // 根据不同类型的方块应用相应的移动策略
        moved = switch (blockId) {
            case 1 -> // 1x1方块
                    singleBlockMover.move(row, col, direction, model, view, selectedBox);
            case 2 -> // 2x1水平方块
                    horizontalBlockMover.move(row, col, direction, model, view, selectedBox);
            case 3 -> // 1x2垂直方块
                    verticalBlockMover.move(row, col, direction, model, view, selectedBox);
            case 4 -> // 2x2大方块
                    bigBlockMover.move(row, col, direction, model, view, selectedBox);
            default -> false;
        };

        // 如果移动成功，记录操作并清空重做栈
        if (moved) {
            // 记录移动操作到历史管理器
            historyManager.recordMove(beforeState, originalRow, originalCol, selectedBox, blockId, direction);

            // 更新最短步数显示
            updateMinStepsDisplay();
        }

        return moved;
    }

    /**
     * 撤销上一次移动
     * @return 撤销是否成功
     */
    public boolean undoMove() {
        boolean success = historyManager.undoMove();
        if (success) {
            // 更新最短步数显示
            updateMinStepsDisplay();
        }
        return success;
    }

    /**
     * 重做上一次撤销的移动
     * @return 重做是否成功
     */
    public boolean redoMove() {
        boolean success = historyManager.redoMove();
        if (success) {
            // 更新最短步数显示
            updateMinStepsDisplay();
        }
        return success;
    }

    /**
     * 更新撤销和重做按钮状态
     */
    private void updateUndoRedoButtons() {
        if (parentFrame != null) {
            parentFrame.updateUndoRedoButtons(historyManager.canUndo(), historyManager.canRedo());
        }
    }

    /**
     * 清空移动历史
     */
    private void clearHistory() {
        historyManager.clearHistory();
    }

    /**
     * 更新最短步数显示
     * 使用求解器获取当前布局到目标的最短步数
     */
    public void updateMinStepsDisplay() {
        try {
            // 获取当前游戏布局的序列化表示
            long currentLayout = model.getSerializedLayout();
            BoardState currentState = new BoardState(currentLayout);

            // 记录求解开始时间
            long startTime = System.currentTimeMillis();

            // 使用求解器获取从当前状态到目标的路径
            List<BoardState> path = solver.findPathFrom(currentState);

            // 记录求解结束时间
            long endTime = System.currentTimeMillis();

            if (path != null && !path.isEmpty()) {
                // 路径长度减1即为所需最少步数
                int minSteps = path.size() - 1;
                view.setMinSteps(minSteps);

                // 输出当前求解信息
                System.out.println("Current layout solved in: " + (endTime - startTime) + " ms");
                System.out.println("A* nodes explored: " + solver.getNodesExploredAStar());
                System.out.println("Minimum steps: " + minSteps);

                // 检查是否达到胜利条件且尚未显示胜利提示
                // 在updateMinStepsDisplay方法内部，修改胜利界面显示部分
                if (minSteps == 0 && !victoryAchieved) {
                    victoryAchieved = true; // 标记已显示过胜利提示
                    // 显示胜利界面，并传递当前步数
                    SwingUtilities.invokeLater(() -> {
                        if (victoryView != null) {
                            int currentSteps = historyManager.getMoveCount(); // 获取当前步数

                            // 检查是否为最后一关
                            boolean lastLevel = isLastLevel();

                            // 设置胜利消息和按钮状态
                            if (lastLevel) {
                                victoryView.setVictoryMessage("Congratulations on completing the game!");
                                victoryView.setNextLevelButtonEnabled(false);
                            } else {
                                victoryView.setVictoryMessage("Victory!");
                                victoryView.setNextLevelButtonEnabled(true);
                            }

                            victoryView.showVictory("Victory!", currentSteps);
                        } else {
                            // 如果胜利视图未设置，使用旧的对话框显示
                            JLabel messageLabel = new JLabel("Congratulations! You have completed the Klotski challenge!");
                            messageLabel.setFont(FontManager.getRegularFont(16));
                            JOptionPane.showMessageDialog(view, messageLabel, "Victory", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                }
            } else {
                // 如果找不到路径，显示默认值
                view.setMinSteps(-1);
                System.out.println("No solution found for current layout");
            }
        } catch (Exception e) {
            System.err.println("Error calculating minimum steps: " + e.getMessage());
            view.setMinSteps(-1);
        }
    }

    /**
     * 加载游戏存档
     */
    public void loadGameState() {
        // 加载游戏状态
        boolean loadSuccess = saveManager.loadGameState();

        if (loadSuccess) {
            // 加载新布局后重新初始化求解器
            this.solver = new KlotskiSolver();
            initializeSolver();

            // 清空历史记录
            clearHistory();

            // 重置胜利状态，因为加载了新布局
            victoryAchieved = false;

            // 隐藏胜利界面
            if (victoryView != null) {
                victoryView.hideVictory();
            }

            // 注意：updateMinStepsDisplay方法现在通过回调在loadGameState内部调用，
            // 确保在显示成功消息之前更新最短步数
        }
    }

    /**
     * 保存当前游戏状态到数据库
     */
    public void saveGameState() {
        saveManager.saveGameState();
    }
}
