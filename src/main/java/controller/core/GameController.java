package controller.core;

import controller.game.history.HistoryManager;
import controller.game.movement.BigBlockMover;
import controller.game.movement.BlockMover;
import controller.game.movement.HorizontalBlockMover;
import controller.game.movement.SingleBlockMover;
import controller.game.movement.VerticalBlockMover;
import controller.game.solver.SolverManager;
import controller.game.state.GameStateManager;
import controller.game.timer.TimerManager;
import controller.storage.save.SaveManager;
import model.Direction;
import model.MapModel;
import view.game.BoxComponent;
import view.game.GameFrame;
import view.game.GamePanel;
import view.victory.VictoryView;

/**
 * 该类作为GamePanel(视图)和MapMatrix(模型)之间的桥梁，实现MVC设计模式中的控制器。 负责处理游戏逻辑，如移动方块、重启游戏等操作。
 */
public class GameController {

    // 游戏视图组件引用
    private GamePanel view;
    // 游戏地图模型引用
    private MapModel model;

    // 方块移动策略对象
    private final BlockMover singleBlockMover;
    private final BlockMover horizontalBlockMover;
    private final BlockMover verticalBlockMover;
    private final BlockMover bigBlockMover;

    // 游戏存档管理器
    private final SaveManager saveManager;

    // 历史记录管理
    private final HistoryManager historyManager;

    // 父窗口引用，用于更新按钮状态
    private GameFrame parentFrame;

    // 胜利控制器
    private final VictoryController victoryController;

    // 新增的管理器类
    private final TimerManager timerManager;
    private final SolverManager solverManager;
    private final GameStateManager gameStateManager;

    /**
     * 构造函数初始化控制器，建立视图和模型之间的连接
     *
     * @param view 游戏面板视图 (Should not be null)
     * @param model 地图数据模型 (Should not be null)
     */
    public GameController(GamePanel view, MapModel model) {
        // If the calling logic is correct, view and model should not be null here.
        // Log if they are, indicating a problem in GameFrame's initialization flow.
        if (view == null) {
            System.err.println("CRITICAL ERROR: GamePanel (view) is null during GameController construction!");
        }
        if (model == null) {
            System.err.println("CRITICAL ERROR: MapModel (model) is null during GameController construction!");
        }

        this.view = view;
        this.model = model;
        // Assuming view is not null based on the corrected flow
        view.setController(this);

        // 初始化方块移动策略
        this.singleBlockMover = new SingleBlockMover();
        this.horizontalBlockMover = new HorizontalBlockMover();
        this.verticalBlockMover = new VerticalBlockMover();
        this.bigBlockMover = new BigBlockMover();

        // 初始化胜利控制器
        this.victoryController = new VictoryController(this);

        // 初始化计时器管理器
        this.timerManager = new TimerManager(view);

        // 初始化求解器管理器
        this.solverManager = new SolverManager(model, view, victoryController);

        // 初始化游戏存档管理器
        this.saveManager = new SaveManager(view, model);
        this.saveManager.setOnLoadCompleteCallback(()
                -> solverManager.updateMinStepsDisplay(timerManager.getGameTimeInMillis(), view.getSteps()));

        // 初始化历史记录管理器
        this.historyManager = new HistoryManager(view, model);

        // 初始化游戏状态管理器
        this.gameStateManager = new GameStateManager(
                model, view, historyManager, victoryController, solverManager, timerManager);
    }

    /**
     * 获取当前游戏模型
     *
     * @return 当前地图模型
     */
    public MapModel getModel() {
        return model;
    }

    /**
     * 获取计时器管理器
     *
     * @return 计时器管理器实例
     */
    public TimerManager getTimerManager() {
        return timerManager;
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
     * 初始化游戏，在UI组件完全准备好后调用
     */
    public void initializeGame() {
        // 根据游戏模式控制UI元素
        if (parentFrame != null) {
            boolean isPracticeMode = model.getGameMode() == MapModel.PRACTICE_MODE;
            parentFrame.setMinStepsLabelVisible(isPracticeMode);
            // 竞速模式下禁用保存功能
            parentFrame.setSaveButtonEnabled(isPracticeMode);
            parentFrame.setHintButtonVisible(isPracticeMode); // 根据模式显示/隐藏提示按钮
        }

        gameStateManager.initializeGame();
    }

    /**
     * 使用新的模型和视图重置控制器状态
     *
     * @param newModel 新的游戏地图模型
     * @param newView 新的游戏面板视图
     */
    public void resetWithNewModel(MapModel newModel, GamePanel newView) {
        this.model = newModel;
        this.view = newView;
        this.view.setController(this);

        // 根据游戏模式控制UI元素
        if (parentFrame != null) {
            boolean isPracticeMode = newModel.getGameMode() == MapModel.PRACTICE_MODE;
            parentFrame.setMinStepsLabelVisible(isPracticeMode);
            parentFrame.setSaveButtonEnabled(isPracticeMode);
            parentFrame.setHintButtonVisible(isPracticeMode); // 根据模式显示/隐藏提示按钮
        }

        gameStateManager.resetWithNewModel(newModel, newView);
    }

    /**
     * 重新开始游戏的方法
     */
    public void restartGame() {
        gameStateManager.restartGame();
    }

    /**
     * 执行移动操作，根据方块所在位置和类型，调用对应的移动方法
     *
     * @param row 当前方块的行索引
     * @param col 当前方块的列索引
     * @param direction 移动方向枚举(UP, DOWN, LEFT, RIGHT)
     * @return 移动是否成功执行
     */
    public boolean doMove(int row, int col, Direction direction) {
        // 确保计时器在第一次移动时启动
        if (!timerManager.isTimerRunning()) {
            timerManager.startTimer();
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

        // 如果移动成功，记录操作并更新最短步数显示
        if (moved) {
            // 记录移动操作到历史管理器
            historyManager.recordMove(beforeState, originalRow, originalCol, selectedBox, blockId, direction);

            // 更新最短步数显示 (此方法内部已包含提示的计算和存储)
            solverManager.updateMinStepsDisplay(timerManager.getGameTimeInMillis(), view.getSteps());
            if (view != null) {
                view.clearHint(); // 移动后清除旧提示
            }
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
            // 更新最短步数显示 (此方法内部已包含提示的计算和存储)
            solverManager.updateMinStepsDisplay(timerManager.getGameTimeInMillis(), view.getSteps());
            if (view != null) {
                view.clearHint(); // 撤销后清除旧提示
            }
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
            // 更新最短步数显示 (此方法内部已包含提示的计算和存储)
            solverManager.updateMinStepsDisplay(timerManager.getGameTimeInMillis(), view.getSteps());
            if (view != null) {
                view.clearHint(); // 重做后清除旧提示
            }
        }
        return success;
    }

    /**
     * 清空移动历史
     */
    private void clearHistory() {
        if (historyManager != null) {
            historyManager.clearHistory();
        }
    }

    /**
     * 保存当前游戏状态到数据库 在保存过程中暂停计时器
     */
    public void saveGameState() {
        // 暂停计时器并记录之前的状态
        boolean wasRunning = timerManager.isTimerRunning();
        if (wasRunning) {
            timerManager.stopTimer();
        }

        try {
            // 保存游戏状态
            boolean saveSuccess = saveManager.saveGameState();

            // 输出保存结果到日志
            System.out.println("Game save " + (saveSuccess ? "successful" : "cancelled or failed"));
        } finally {
            // 无论保存是否成功或被取消，如果之前计时器在运行，都恢复计时器
            if (wasRunning) {
                timerManager.startTimer();
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
        return timerManager.getGameTimeInMillis();
    }

    /**
     * 设置加载的游戏时间
     *
     * @param gameTime 游戏时间（毫秒）
     */
    public void setLoadedGameTime(long gameTime) {
        timerManager.setLoadedGameTime(gameTime);
    }

    /**
     * 请求并显示下一步的移动提示。 提示的计算和存储由 SolverManager 在棋盘状态更新时完成。 此方法仅获取该提示并通知视图进行显示。
     */
    public void showNextMoveHint() {
        if (model == null || model.getGameMode() != MapModel.PRACTICE_MODE) {
            System.out.println("Hint feature is only available in Practice Mode.");
            if (view != null) {
                view.clearHint(); // 清除任何可能存在的旧提示
            }
            // 可选：通过弹窗或状态栏告知用户
            // JOptionPane.showMessageDialog(parentFrame, "Hints are only available in Practice Mode.", "Hint Unavailable", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (solverManager == null) {
            System.err.println("SolverManager is not initialized. Cannot show hint.");
            return;
        }
        if (view == null) {
            System.err.println("GamePanel (view) is not initialized. Cannot display hint.");
            return;
        }

        int[] hintCoordinates = solverManager.getNextMoveHint();

        if (hintCoordinates != null && hintCoordinates.length == 2) {
            System.out.println("Hint: Suggested piece to move is at (" + hintCoordinates[0] + ", " + hintCoordinates[1] + ")");
            // 调用 GamePanel 的方法来高亮显示这个棋子
            // 假设 GamePanel 有一个 highlightPieceForHint(int row, int col) 方法
            view.highlightPieceForHint(hintCoordinates[0], hintCoordinates[1]);
        } else {
            System.out.println("No hint available (already solved, unsolvable, or error).");
            // 可以选择通知 GamePanel 清除任何现有的提示高亮
            view.clearHint();
        }
    }

    /**
     * 强制游戏进入胜利状态（用于调试或演示）
     */
    public void forceVictory() {
        if (victoryController != null) {
            // 停止计时器
            timerManager.stopTimer();

            // 获取当前游戏状态参数
            long gameTime = timerManager.getGameTimeInMillis();
            int moveCount = view.getSteps();

            // 直接调用胜利检查，传入0作为最小步数（确保触发胜利条件）
            victoryController.checkVictory(0, gameTime, moveCount);

            System.out.println("Victory forced by shortcut key");
        }
    }
}
