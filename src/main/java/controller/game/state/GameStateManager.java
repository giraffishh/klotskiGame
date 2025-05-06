package controller.game.state;

import java.util.List;

import controller.core.LevelSelectController;
import controller.core.LevelSelectController.LevelData;
import controller.core.VictoryController;
import controller.game.history.HistoryManager;
import controller.game.solver.SolverManager;
import controller.game.timer.TimerManager;
import model.MapModel;
import view.game.GamePanel;
import view.level.LevelSelectFrame;
import view.util.FrameManager;

/**
 * 游戏状态管理类，处理游戏初始化与重启逻辑
 */
public class GameStateManager {

    private MapModel model;
    private GamePanel view;
    private HistoryManager historyManager;
    private VictoryController victoryController;
    private SolverManager solverManager;
    private TimerManager timerManager;

    /**
     * 构造函数
     *
     * @param model 游戏地图模型
     * @param view 游戏面板视图
     * @param historyManager 历史记录管理器
     * @param victoryController 胜利控制器
     * @param solverManager 求解器管理器
     * @param timerManager 计时器管理器
     */
    public GameStateManager(
            MapModel model,
            GamePanel view,
            HistoryManager historyManager,
            VictoryController victoryController,
            SolverManager solverManager,
            TimerManager timerManager) {
        this.model = model;
        this.view = view;
        this.historyManager = historyManager;
        this.victoryController = victoryController;
        this.solverManager = solverManager;
        this.timerManager = timerManager;
    }

    /**
     * 更新引用
     *
     * @param newModel 新的游戏地图模型
     * @param newView 新的游戏面板视图
     */
    public void updateReferences(MapModel newModel, GamePanel newView) {
        this.model = newModel;
        this.view = newView;
    }

    /**
     * 使用新的模型和视图重置游戏状态
     *
     * @param newModel 新的游戏地图模型
     * @param newView 新的游戏面板视图
     */
    public void resetWithNewModel(MapModel newModel, GamePanel newView) {
        System.out.println("Resetting game state with new model and view...");

        // 更新本类中的引用
        updateReferences(newModel, newView);

        // 更新其他管理器中的引用
        if (historyManager != null) {
            historyManager.updateReferences(newView, newModel); // HistoryManager 内部会清空历史
        }
        if (solverManager != null) {
            solverManager.updateReferences(newModel, newView);
        }
        if (timerManager != null) {
            timerManager.updateView(newView);
        }

        // 重置计时器
        if (timerManager != null) {
            timerManager.resetTimer();
        }

        // 重置胜利控制器状态
        if (victoryController != null) {
            victoryController.resetVictoryState();
        }

        // 确保新视图的步数显示为0
        if (this.view != null) {
            this.view.setSteps(0);
        }

        System.out.println("Game state reset complete.");
    }

    /**
     * 重新开始游戏
     */
    public void restartGame() {
        System.out.println("\nRestarting game...");

        try {
            // 检查model是否为null
            if (model == null) {
                System.err.println("Cannot restart game: Model is null.");
                return;
            }

            // 检查是否从存档加载
            if (model.isLoadedFromSave()) {
                System.out.println("Restarting a game loaded from save. Resetting to original level layout.");
                int levelIndexToLoad = model.getCurrentLevelIndex();
                int[][] originalLayout = null;

                // 通过FrameManager获取LevelSelectController来加载原始布局
                FrameManager frameManager = FrameManager.getInstance();
                LevelSelectFrame levelSelectFrame = frameManager.getLevelSelectFrame();
                if (levelSelectFrame != null) {
                    LevelSelectController levelController = levelSelectFrame.getController();
                    if (levelController != null) {
                        List<LevelData> levels = levelController.getLevels();
                        if (levelIndexToLoad >= 0 && levelIndexToLoad < levels.size()) {
                            LevelData levelData = levels.get(levelIndexToLoad);
                            if (levelData != null && levelData.getLayout() != null) {
                                // 获取原始布局
                                int[][] layoutSource = levelData.getLayout();
                                // 创建深拷贝
                                originalLayout = new int[layoutSource.length][layoutSource[0].length];
                                for (int i = 0; i < layoutSource.length; i++) {
                                    System.arraycopy(layoutSource[i], 0, originalLayout[i], 0, layoutSource[i].length);
                                }
                                System.out.println("Successfully retrieved original layout for level " + (levelIndexToLoad + 1) + " via LevelSelectController.");
                            } else {
                                System.err.println("Level data or layout is null for index: " + levelIndexToLoad);
                            }
                        } else {
                            System.err.println("Invalid level index to load original layout: " + levelIndexToLoad);
                        }
                    } else {
                        System.err.println("LevelSelectController is null, cannot load original layout.");
                    }
                } else {
                    System.err.println("LevelSelectFrame is null, cannot load original layout.");
                }

                if (originalLayout != null) {
                    // 使用原始布局重置当前模型状态
                    model.setMatrix(originalLayout); // 重置当前布局
                    model.updateInitialMatrix(originalLayout); // 更新模型的初始状态记录
                    model.setLoadedFromSave(false); // 清除从存档加载的标志
                    System.out.println("Model reset to original layout for level " + (levelIndexToLoad + 1));
                } else {
                    System.err.println("Failed to load original layout for level index: " + levelIndexToLoad + ". Resetting to saved initial state instead.");
                    // 如果加载失败，回退到重置为保存时的初始状态（可能不是关卡初始状态）
                    model.resetToInitialState();
                    model.setLoadedFromSave(false); // 仍然清除标志
                }
            } else {
                // 正常重置到当前关卡的初始状态
                model.resetToInitialState();
                System.out.println("Game reset to its initial state.");
            }

            // 重置游戏面板 (会重置步数显示并重绘)
            if (view != null) {
                view.resetGame();
            }

            // 重新初始化求解器
            if (solverManager != null) {
                solverManager.initializeSolver();
                solverManager.updateMinStepsDisplay(0, 0);
            }

            // 清空历史记录
            if (historyManager != null) {
                historyManager.clearHistory();
            }

            // 重置胜利控制器状态
            if (victoryController != null) {
                victoryController.resetVictoryState();
            }

            // 重置计时器
            if (timerManager != null) {
                timerManager.resetTimer();
            }

            System.out.println("Game restarted successfully");
        } catch (Exception e) {
            System.err.println("Error during game restart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化游戏，主要是初始化求解器和更新显示
     */
    public void initializeGame() {
        // 初始化华容道求解器
        if (solverManager != null) {
            solverManager.initializeSolver();
            // 确保更新最短步数显示
            solverManager.updateMinStepsDisplay(0, 0);
        }
    }
}
