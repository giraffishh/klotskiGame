package controller.core;

import java.util.Arrays;

import controller.game.history.HistoryManager;
import controller.game.movement.BigBlockMover;
import controller.game.movement.BlockMover;
import controller.game.movement.HorizontalBlockMover;
import controller.game.movement.SingleBlockMover;
import controller.game.movement.VerticalBlockMover;
import controller.game.solver.SolverManager;
import controller.game.sound.SoundManager;
import controller.game.state.GameStateManager;
import controller.game.timer.TimerManager;
import controller.storage.save.SaveManager;
import controller.util.BoardSerializer;
import model.Direction;
import model.MapModel;
import service.OnlineViewer;
import view.game.BoxComponent;
import view.game.GameFrame;
import view.game.GamePanel;
import view.util.FrameManager;
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
    private final SoundManager soundManager; // 新增音效管理器

    // 新增：本地网页视图服务
    private OnlineViewer onlineViewer;

    // 当前游戏会话ID
    private String currentSessionId;

    private int[] activeHintPieceCoordinates = null; // 新增：存储当前激活的提示方块坐标

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

        // 使用FrameManager中的共享SoundManager实例
        this.soundManager = FrameManager.getInstance().getSoundManager();

        // 初始化游戏存档管理器
        this.saveManager = new SaveManager(view, model);
        this.saveManager.setOnLoadCompleteCallback(()
                -> solverManager.updateMinStepsDisplay(timerManager.getGameTimeInMillis(), view.getSteps()));

        // 初始化历史记录管理器
        this.historyManager = new HistoryManager(view, model);

        // 初始化游戏状态管理器
        this.gameStateManager = new GameStateManager(
                model, view, historyManager, victoryController, solverManager, timerManager);

        // 初始化本地网页视图服务
        this.onlineViewer = OnlineViewer.getInstance();
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

        // 预热音效系统，确保第一次移动有声音
        soundManager.preloadSoundSystem();

        // 不在这里启动背景音乐，因为已经在登录后启动了
        gameStateManager.initializeGame();
        this.activeHintPieceCoordinates = null; // 初始化时清除活动提示
        if (view != null) {
            view.clearHint();
        }

        // 创建新的游戏会话并获取URL
        if (onlineViewer != null && model != null) {
            try {
                // 确保OnlineViewer服务在运行
                onlineViewer.ensureRunning();
                
                String sessionUrl = onlineViewer.createGameSession(model);
                // 从URL中提取会话ID (URL现在包含多行)
                String[] parts = sessionUrl.split("\n");
                if (parts.length > 0) {
                    String firstUrl = parts[0];
                    if (firstUrl.contains("session=")) {
                        currentSessionId = firstUrl.split("session=")[1];
                    }
                }

                // 在控制台显示所有URL选项
                System.out.println("\n===== 华容道游戏网页查看器 =====");
                System.out.println(sessionUrl);
                System.out.println("==================================\n");
            } catch (Exception e) {
                System.err.println("创建游戏会话时出错: " + e.getMessage());
            }
        }
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

        // 更新 SaveManager 中的模型和视图引用
        if (this.saveManager != null) {
            this.saveManager.updateReferences(newView, newModel);
        }

        // 根据游戏模式控制UI元素
        if (parentFrame != null) {
            boolean isPracticeMode = newModel.getGameMode() == MapModel.PRACTICE_MODE;
            parentFrame.setMinStepsLabelVisible(isPracticeMode);
            parentFrame.setSaveButtonEnabled(isPracticeMode);
            parentFrame.setHintButtonVisible(isPracticeMode); // 根据模式显示/隐藏提示按钮
        }

        // 移除重启背景音乐的代码，已经在登录时处理了
        gameStateManager.resetWithNewModel(newModel, newView);
        this.activeHintPieceCoordinates = null; // 重置模型时清除活动提示

        // 创建新的游戏会话
        if (onlineViewer != null && newModel != null) {
            try {
                // 确保OnlineViewer服务在运行
                onlineViewer.ensureRunning();
                
                String sessionUrl = onlineViewer.createGameSession(newModel);
                // 从URL中提取会话ID (URL现在包含多行)
                String[] parts = sessionUrl.split("\n");
                if (parts.length > 0) {
                    String firstUrl = parts[0];
                    if (firstUrl.contains("session=")) {
                        currentSessionId = firstUrl.split("session=")[1];
                    }
                }

                // 在控制台显示所有URL选项
                System.out.println("\n===== 华容道游戏网页查看器 =====");
                System.out.println(sessionUrl);
                System.out.println("==================================\n");
            } catch (Exception e) {
                System.err.println("创建新游戏会话时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 重新开始游戏的方法
     */
    public void restartGame() {
        gameStateManager.restartGame();
        this.activeHintPieceCoordinates = null; // 重启游戏时清除活动提示
        // view.clearHint() 应该由 gameStateManager.restartGame -> view.resetGame() 间接触发
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
            // 播放移动音效
            soundManager.playSound(SoundManager.SoundType.MOVE);

            // 记录移动操作到历史管理器
            // 不再传递 selectedBox, blockId, direction, originalRow, originalCol
            // 而是传递移动前的状态和移动后的序列化布局
            historyManager.recordMove(beforeState, model.getSerializedLayout());

            // 更新最短步数显示 (此方法内部已包含提示的计算和存储)
            solverManager.updateMinStepsDisplay(timerManager.getGameTimeInMillis(), view.getSteps());
            if (view != null) {
                view.clearHint(); // 移动后清除旧提示
            }
            this.activeHintPieceCoordinates = null; // 移动后清除活动提示状态

            // 更新网页视图
            if (onlineViewer != null && currentSessionId != null && model != null) {
                // 确保OnlineViewer服务在运行
                onlineViewer.ensureRunning();
                onlineViewer.updateGameSession(currentSessionId, model);
            }
        } else {
            // 移动失败不播放音效
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
            this.activeHintPieceCoordinates = null; // 撤销后清除活动提示状态
        }

        // 更新网页视图
        if (onlineViewer != null && currentSessionId != null && model != null) {
            // 确保OnlineViewer服务在运行
            onlineViewer.ensureRunning();
            onlineViewer.updateGameSession(currentSessionId, model);
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
            this.activeHintPieceCoordinates = null; // 重做后清除活动提示状态
        }

        // 更新网页视图
        if (onlineViewer != null && currentSessionId != null && model != null) {
            // 确保OnlineViewer服务在运行
            onlineViewer.ensureRunning();
            onlineViewer.updateGameSession(currentSessionId, model);
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
            this.activeHintPieceCoordinates = null; // 清除非练习模式下的活动提示
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

        int[] newHintCoords = solverManager.getNextMoveHint();

        if (newHintCoords == null) {
            // 没有可用的提示 (已解决, 无解, 或错误)
            System.out.println("No hint available.");
            this.activeHintPieceCoordinates = null;
            view.clearHint(); // 清除任何现有的提示高亮
            return;
        }

        // 检查是否是第二次点击相同的提示
        if (this.activeHintPieceCoordinates != null && Arrays.equals(this.activeHintPieceCoordinates, newHintCoords)) {
            // 第二次点击，尝试自动移动
            System.out.println("Second click on hint for piece at (" + newHintCoords[0] + ", " + newHintCoords[1] + "). Attempting auto-move.");
            long currentLayoutLong = model.getSerializedLayout();
            long nextOptimalLayoutLong = solverManager.getLastOptimalNextStepLayout();

            if (nextOptimalLayoutLong != -1) {
                determineAndExecuteAutomaticHintMove(newHintCoords, currentLayoutLong, nextOptimalLayoutLong);
            } else {
                System.err.println("Cannot auto-move: Next optimal layout is not available.");
                // 保持提示高亮，但不清除 activeHintPieceCoordinates，允许用户再次尝试或手动移动
                // 或者选择清除提示：
                // this.activeHintPieceCoordinates = null;
                // view.clearHint();
            }
        } else {
            // 第一次点击此提示，或提示了新的方块
            System.out.println("Hint: Suggested piece to move is at (" + newHintCoords[0] + ", " + newHintCoords[1] + ")");
            this.activeHintPieceCoordinates = newHintCoords;
            view.highlightPieceForHint(newHintCoords[0], newHintCoords[1]);
        }
    }

    /**
     * 确定提示方块的移动方向并执行移动的私有方法
     */
    private void determineAndExecuteAutomaticHintMove(int[] hintCoords, long currentLayoutLong, long nextOptimalLayoutLong) {
        // hintCoords 在此新逻辑中不再直接用于确定移动方向。
        // currentLayoutLong 代表了应用新布局之前的状态。

        try {
            // 保存自动应用前的棋盘状态，用于历史记录
            int[][] beforeState = model.copyMatrix(); // 或者 BoardSerializer.deserialize(currentLayoutLong)

            int[][] nextOptimalLayoutArray = BoardSerializer.deserialize(nextOptimalLayoutLong);

            System.out.println("Auto-applying optimal layout directly.");

            // 0. 在视图重置步数之前，保存当前步数
            int currentStepsBeforeApply = 0;
            if (view != null) {
                currentStepsBeforeApply = view.getSteps();
            }

            // 1. 直接更新模型
            model.setMatrix(nextOptimalLayoutArray);

            // 获取移动后的序列化布局，用于历史记录
            long layoutAfterAutoMove = model.getSerializedLayout();

            // 2. 更新视图以反映模型的更改
            // GamePanel.resetGame() 会清除现有棋子并根据 model 重新初始化棋盘，
            // 同时会将 view 内部的 steps 重置为0。
            if (view != null) {
                view.resetGame();
                // 3. 恢复并增加步数
                view.setSteps(currentStepsBeforeApply + 1);
            }

            // 3.5. 记录这次自动应用到历史管理器
            if (historyManager != null) {
                historyManager.recordMove(beforeState, layoutAfterAutoMove);
            }

            // 4. 播放移动音效
            if (soundManager != null) {
                soundManager.playSound(SoundManager.SoundType.MOVE);
            }

            // 5. 清除当前的提示状态
            this.activeHintPieceCoordinates = null;
            if (view != null) {
                view.clearHint();
            }

            // 6. 更新求解器状态和最小步数显示（基于新的棋盘布局）
            //    确保计时器已启动（如果这是第一“步”）
            if (timerManager != null && !timerManager.isTimerRunning() && view != null && view.getSteps() > 0) {
                timerManager.startTimer();
            }
            if (solverManager != null && timerManager != null && view != null) {
                solverManager.updateMinStepsDisplay(timerManager.getGameTimeInMillis(), view.getSteps());
            }

            // 更新网页视图
            if (onlineViewer != null && currentSessionId != null && model != null) {
                onlineViewer.updateGameSession(currentSessionId, model);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Error deserializing next optimal layout for auto-apply: " + e.getMessage());
            this.activeHintPieceCoordinates = null; // 出错时也清除提示
            if (view != null) {
                view.clearHint();
            }
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

    /**
     * 获取音效管理器
     *
     * @return 音效管理器实例
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }
    
    /**
     * 清理资源，在游戏界面关闭时调用
     * 确保网络服务和其他资源被正确关闭
     */
    public void shutdown() {
        // 停止计时器
        if (timerManager != null) {
            timerManager.stopTimer();
        }
        
        // 关闭网络服务
        if (onlineViewer != null) {
            try {
                System.out.println("正在关闭网页查看服务...");
                onlineViewer.stop();
                System.out.println("网页查看服务已关闭");
            } catch (Exception e) {
                System.err.println("关闭网页查看服务时出错: " + e.getMessage());
            }
        }
        
        // 释放其他可能需要清理的资源
        if (soundManager != null) {
            soundManager.stopBackgroundMusic();
        }
    }
}
