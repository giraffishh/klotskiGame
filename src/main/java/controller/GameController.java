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

import javax.swing.Timer;

import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;

import view.game.GameFrame;
import view.level.LevelSelectFrame;
import view.victory.VictoryView;

/**
 * 该类作为GamePanel(视图)和MapMatrix(模型)之间的桥梁，实现MVC设计模式中的控制器。 负责处理游戏逻辑，如移动方块、重启游戏等操作。
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

    // 胜利控制器
    private VictoryController victoryController;

    // 选关界面引用
    private LevelSelectFrame levelSelectFrame;

    // 当前关卡索引
    private int currentLevelIndex = 0;

    // 计时相关
    private Timer gameTimer;                  // 游戏计时器
    private long startTime;                   // 计时开始时间
    private long elapsedTimeBeforeStart = 0;  // 计时器启动前已经过的时间（用于暂停/继续）
    private boolean timerRunning = false;     // 计时器运行状态

    // 用于格式化毫秒显示的格式器
    private final DecimalFormat millisFormat = new DecimalFormat("00");

    /**
     * 构造函数初始化控制器，建立视图和模型之间的连接
     *
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

        // 初始化胜利控制器
        this.victoryController = new VictoryController(this);

        // 初始化计时器
        initializeTimer();
    }

    /**
     * 初始化游戏计时器
     */
    private void initializeTimer() {
        gameTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 计算当前经过的总时间（毫秒）
                long currentTime = System.currentTimeMillis();
                long totalElapsed = elapsedTimeBeforeStart + (currentTime - startTime);

                // 更新时间显示
                updateTimeDisplay(totalElapsed);
            }
        });
    }

    /**
     * 启动游戏计时器
     */
    public void startTimer() {
        if (!timerRunning) {
            // 记录启动时间点
            startTime = System.currentTimeMillis();
            gameTimer.start();
            timerRunning = true;
        }
    }

    /**
     * 停止游戏计时器
     */
    public void stopTimer() {
        if (timerRunning) {
            // 保存已经过的时间
            long currentTime = System.currentTimeMillis();
            elapsedTimeBeforeStart += (currentTime - startTime);
            gameTimer.stop();
            timerRunning = false;
        }
    }

    /**
     * 重置游戏计时器
     */
    public void resetTimer() {
        // 停止计时器
        if (gameTimer != null) {
            gameTimer.stop();
        }
        // 重置计时数据
        elapsedTimeBeforeStart = 0;
        timerRunning = false;
        // 更新显示为零
        updateTimeDisplay(0);
    }

    /**
     * 更新时间显示，格式为 mm:ss.xx（分:秒.厘秒）
     *
     * @param totalMillis 总毫秒数
     */
    private void updateTimeDisplay(long totalMillis) {
        int minutes = (int) (totalMillis / 60000);
        int seconds = (int) ((totalMillis % 60000) / 1000);
        int centiseconds = (int) ((totalMillis % 1000) / 10);

        String timeText = String.format("Time: %02d:%02d.%s",
                minutes, seconds, millisFormat.format(centiseconds));

        if (view != null) {
            view.updateTimeDisplay(timeText);
        }
    }

    /**
     * 设置父窗口引用，用于更新UI按钮状态
     *
     * @param frame 父窗口
     */
    public void setParentFrame(GameFrame frame) {
        this.parentFrame = frame;

        // 安全地更新按钮状态
        if (frame != null) {
            try {
                // 将父窗口引用也传递给历史管理器和胜利控制器
                if (historyManager != null) {
                    historyManager.setParentFrame(frame);
                }
                if (victoryController != null) {
                    victoryController.setParentFrame(frame);
                }
            } catch (Exception e) {
                // 捕获可能的异常，防止初始化时出错
                System.err.println("Error setting parent frame: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置胜利视图
     *
     * @param victoryView 胜利界面视图
     */
    public void setVictoryView(VictoryView victoryView) {
        if (victoryController != null) {
            victoryController.setVictoryView(victoryView);
        }
    }

    /**
     * 设置关卡选择界面
     *
     * @param levelSelectFrame 关卡选择界面
     */
    public void setLevelSelectFrame(LevelSelectFrame levelSelectFrame) {
        this.levelSelectFrame = levelSelectFrame;
        if (victoryController != null) {
            victoryController.setLevelSelectFrame(levelSelectFrame);
        }
    }

    /**
     * 设置当前关卡索引
     *
     * @param index 关卡索引
     */
    public void setCurrentLevelIndex(int index) {
           this.currentLevelIndex = index;
        if (victoryController != null) {
            victoryController.setCurrentLevelIndex(index);
        }
    }

    /**
     * 初始化游戏，在UI组件完全准备好后调用 这个方法应在GameFrame完成所有UI元素设置后调用
     */
    public void initializeGame() {
        // 初始化华容道求解器并计算最优解
        initializeSolver();

        // 确保更新最短步数显示
        updateMinStepsDisplay();

        // 清空历史记录
        clearHistory();

        // 重置计时器
        resetTimer();

        // 重置胜利控制器状态
        if (victoryController != null) {
            victoryController.resetVictoryState();
        }
    }

    /**
     * 初始化华容道求解器并预先计算最优解 将此逻辑抽取为单独方法，以便在构造函数和加载游戏后调用
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

        try {
            // 检查model是否为null
            if (model == null) {
                return;
            }

            // 重置地图模型到初始状态
            model.resetToInitialState();

            // 重置游戏面板
            if (view != null) {
                view.resetGame();
            }

            // 重置后更新最短步数显示
            // 只有在游戏实际显示时才需要重新求解
            if (view != null && view.isShowing()) {
                this.solver = new KlotskiSolver();
                initializeSolver();
            }
            updateMinStepsDisplay();

            // 清空历史记录
            historyManager.clearHistory();

            // 重置胜利控制器状态
            if (victoryController != null) {
                victoryController.resetVictoryState();
            }

            // 重置计时器
            resetTimer();

            System.out.println("Game restarted successfully");
        } catch (Exception e) {
            System.err.println("Error during game restart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行移动操作 根据方块所在位置和类型，调用对应的移动方法
     *
     * @param row 当前方块的行索引
     * @param col 当前方块的列索引
     * @param direction 移动方向枚举(UP, DOWN, LEFT, RIGHT)
     * @return 移动是否成功执行
     */
    public boolean doMove(int row, int col, Direction direction) {
        // 确保计时器在第一次移动时启动
        if (!timerRunning) {
            startTimer();
        }

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
            default ->
                false;
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
     *
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
     *
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
     * 更新最短步数显示 使用求解器获取当前布局到目标的最短步数
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

                // 使用胜利控制器检查胜利条件
                if (victoryController != null) {
                    // 传递当前的最小步数、游戏用时和当前步数
                    victoryController.checkVictory(
                            minSteps,
                            getGameTimeInMillis(),
                            historyManager.getMoveCount()
                    );
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
     * 保存当前游戏状态到数据库 在保存过程中暂停计时器
     */
    public void saveGameState() {
        // 暂停计时器并记录之前的状态
        boolean wasRunning = timerRunning;
        if (wasRunning) {
            stopTimer();
        }

        try {
            // 保存游戏状态
            boolean saveSuccess = saveManager.saveGameState();

            // 输出保存结果到日志
            System.out.println("Game save " + (saveSuccess ? "successful" : "cancelled or failed"));
        } finally {
            // 无论保存是否成功或被取消，如果之前计时器在运行，都恢复计时器
            if (wasRunning) {
                startTimer();
                System.out.println("Timer resumed after save operation");
            }
        }
    }

    /**
     * 获取当前游戏用时（毫秒）
     *
     * @return 游戏用时（毫秒）
     */
    public long getGameTimeInMillis() {
        // 计算当前经过的总时间（毫秒）
        if (timerRunning) {
            long currentTime = System.currentTimeMillis();
            return elapsedTimeBeforeStart + (currentTime - startTime);
        } else {
            return elapsedTimeBeforeStart;
        }
    }

    /**
     * 设置加载的游戏时间
     *
     * @param gameTime 游戏时间（毫秒）
     */
    public void setLoadedGameTime(long gameTime) {
        // 停止计时器
        stopTimer();
        // 设置已经过的时间
        elapsedTimeBeforeStart = gameTime;
        // 更新显示
        updateTimeDisplay(gameTime);
    }

    /**
     * 强制游戏进入胜利状态（用于调试或演示）
     * 通过快捷键触发
     */
    public void forceVictory() {
        if (victoryController != null) {
            // 停止计时器
            stopTimer();

            // 获取当前游戏状态参数
            long gameTime = getGameTimeInMillis();
            int moveCount = view.getSteps();

            // 直接调用胜利检查，传入0作为最小步数（确保触发胜利条件）
            victoryController.checkVictory(0, gameTime, moveCount);

            System.out.println("Victory forced by shortcut key");
        }
    }
}
