package controller;

import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controller.rank.RankManager;
import model.MapModel;
import service.UserSession;
import view.game.GameFrame;
import view.level.LevelSelectFrame;
import view.util.FrameManager;
import view.victory.VictoryView;

/**
 * 胜利控制器，负责处理游戏胜利相关的逻辑 将胜利判断、胜利界面显示和按钮处理从GameController中分离出来
 */
public class VictoryController {

    // 胜利界面引用
    private VictoryView victoryView;

    // 游戏控制器引用（用于回调）
    private final GameController gameController;

    // 游戏窗口引用
    private GameFrame parentFrame;

    // 胜利状态标志，防止重复弹出胜利提示
    private boolean victoryAchieved = false;

    // 格式化毫秒显示
    private final DecimalFormat millisFormat = new DecimalFormat("00");

    /**
     * 构造函数，初始化胜利控制器
     *
     * @param gameController 游戏控制器引用
     */
    public VictoryController(GameController gameController) {
        this.gameController = gameController;
    }

    /**
     * 设置父窗口引用
     *
     * @param frame 父窗口
     */
    public void setParentFrame(GameFrame frame) {
        this.parentFrame = frame;
    }

    /**
     * 设置胜利视图
     *
     * @param victoryView 胜利界面视图
     */
    public void setVictoryView(VictoryView victoryView) {
        this.victoryView = victoryView;
        setupVictoryListeners();
    }

    /**
     * 重置胜利状态
     */
    public void resetVictoryState() {
        victoryAchieved = false;
    }

    /**
     * 设置胜利界面的按钮监听器
     */
    private void setupVictoryListeners() {
        if (victoryView == null) {
            return;
        }

        // 获取FrameManager实例
        FrameManager frameManager = FrameManager.getInstance();

        // 设置回到主页按钮监听器 - 直接返回，不显示确认对话框
        victoryView.setHomeListener(e -> {
            victoryView.hideVictory();
            if (parentFrame != null) {
                // 使用FrameManager导航到主页
                frameManager.navigateFromGameToHome();
            }
        });

        // 设置关卡选择按钮监听器
        victoryView.setLevelSelectListener(e -> {
            victoryView.hideVictory();
            // 使用FrameManager导航到关卡选择界面
            frameManager.navigateFromGameToLevelSelect();
        });

        // 设置再来一次按钮监听器
        victoryView.setRestartListener(e -> {
            if (parentFrame != null) {
                parentFrame.setVisible(true);
                System.out.println("GameFrame shown after restart.");
            }
            victoryView.hideVictory();

            // 重新开始游戏，如果游戏是从存档加载的，首先会在restartGame方法中处理
            gameController.restartGame();
            // 确保游戏面板获得焦点
            if (parentFrame != null && parentFrame.getGamePanel() != null) {
                parentFrame.getGamePanel().requestFocusInWindow();
            }
        });

        // 设置下一关按钮监听器
        victoryView.setNextLevelListener(e -> {
            if (!isLastLevel()) {
                if (parentFrame != null) {
                    parentFrame.setVisible(true);
                    System.out.println("GameFrame shown before loading next level.");
                }
                victoryView.hideVictory();

                // 获取模型并检查/设置加载状态
                MapModel model = gameController.getModel();
                if (model != null && model.isLoadedFromSave()) {
                    model.setLoadedFromSave(false);
                    System.out.println("Cleared loadedFromSave flag in model before loading next level.");
                }

                System.out.println("\nLoading next level...");
                SwingUtilities.invokeLater(this::loadNextLevel);
            }
        });
    }

    /**
     * 检查是否达到胜利条件，如果达到则显示胜利界面
     *
     * @param minSteps 最小步数，当为0时表示达到胜利条件
     * @param gameTimeInMillis 游戏用时（毫秒）
     * @param currentMoveCount 当前步数
     * @return 是否胜利
     */
    public boolean checkVictory(int minSteps, long gameTimeInMillis, int currentMoveCount) {
        if (minSteps == 0 && !victoryAchieved) {
            victoryAchieved = true; // 标记已经显示过胜利提示

            // 停止计时器
            gameController.stopTimer();

            // 获取当前关卡模型，并立即提取关键信息以避免线程同步问题
            final MapModel model = gameController.getModel();
            // 健壮性检查：确保model不为null
            if (model == null) {
                System.err.println("错误：在 checkVictory 时 model 为 null！无法继续。");
                victoryAchieved = false; // 重置状态，允许下次尝试
                return false;
            }
            final int currentLevelIndex = model.getCurrentLevelIndex();

            // 记录当前关卡，用于调试
            System.out.println("[VictoryController] 胜利! 当前关卡索引: " + currentLevelIndex);

            // --- 上传分数到排行榜 (如果不是访客) ---
            final boolean isGuest = UserSession.getInstance().isGuest();
            final String username = isGuest ? "Guest"
                    : (UserSession.getInstance().getCurrentUser() != null
                    ? UserSession.getInstance().getCurrentUser().getUsername() : "ErrorUser"); // 添加null检查

            // 显示胜利界面
            SwingUtilities.invokeLater(() -> {
                if (victoryView != null) {
                    // 格式化时间字符串
                    String timeText = formatTime(gameTimeInMillis);

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

                    // 只调用一次showVictory方法，避免重复显示
                    victoryView.showVictory("Victory!", currentMoveCount, timeText);

                    // 处理排行榜展示逻辑 - 使用已捕获的关卡索引
                    // 简化：先上传（RankManager内部后台），然后立即请求加载（RankManager内部SwingWorker）
                    if (!isGuest) {
                        // 触发分数上传（在后台线程中执行）
                        RankManager.getInstance().uploadScore(
                                currentLevelIndex, username, currentMoveCount, gameTimeInMillis);
                        // 立即触发排行榜加载（使用SwingWorker在后台获取数据，然后在EDT更新UI）
                        // 注意：这里不再需要sleep，因为上传和加载是独立的后台任务
                        RankManager.getInstance().loadLeaderboardData(
                                victoryView, currentLevelIndex, isGuest,
                                username, currentMoveCount, gameTimeInMillis);
                    } else {
                        // 访客模式直接加载排行榜，同时传入当前成绩以便显示
                        RankManager.getInstance().loadLeaderboardData(
                                victoryView, currentLevelIndex, isGuest,
                                username, currentMoveCount, gameTimeInMillis);
                    }
                } else {
                    // 如果胜利视图未设置，使用旧的对话框显示
                    System.err.println("[VictoryController] victoryView 为 null，无法显示胜利界面和排行榜！");
                    JLabel messageLabel = new JLabel("Congratulations! You have completed the Klotski challenge!");
                    messageLabel.setFont(view.util.FontManager.getTitleFont(16));
                    JOptionPane.showMessageDialog(parentFrame, messageLabel, "Victory", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            // 隐藏游戏窗口
            if (parentFrame != null) {
                parentFrame.setVisible(false);
                System.out.println("[VictoryController] GameFrame hidden due to victory.");
            }

            return true;
        }
        return false;
    }

    /**
     * 格式化时间显示，格式为 mm:ss.xx（分:秒.厘秒）
     *
     * @param totalMillis 总毫秒数
     * @return 格式化后的时间字符串
     */
    private String formatTime(long totalMillis) {
        int minutes = (int) (totalMillis / 60000);
        int seconds = (int) ((totalMillis % 60000) / 1000);
        int centiseconds = (int) ((totalMillis % 1000) / 10);

        return String.format("Time: %02d:%02d.%s",
                minutes, seconds, millisFormat.format(centiseconds));
    }

    /**
     * 加载下一关
     */
    public void loadNextLevel() {
        // 添加前置检查：如果当前已经是最后一关，直接显示提示
        if (isLastLevel()) {
            // 只显示一个"确定"按钮，点击后直接返回主页
            JLabel messageLabel = new JLabel("Congratulations! You have completed all levels!");
            messageLabel.setFont(view.util.FontManager.getRegularFont(16));

            JOptionPane.showMessageDialog(
                    parentFrame,
                    messageLabel,
                    "Game Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

            // 使用FrameManager直接返回主页
            FrameManager.getInstance().navigateFromGameToHome();
            return; // 直接返回，不执行后续加载逻辑
        }

        try {
            // 获取下一关索引
            int nextLevelIndex = getNextLevelIndex();

            if (nextLevelIndex == -1) {
                // 理论上这里不会执行到，因为前面的isLastLevel()已经处理了这种情况
                return;
            }

            // 通过FrameManager获取关卡选择控制器
            LevelSelectFrame levelSelectFrame = FrameManager.getInstance().getLevelSelectFrame();
            LevelSelectController levelController = levelSelectFrame.getController();

            if (levelController == null) {
                showErrorMessage("Level selection controller is null");
                return;
            }

            // 获取关卡数据
            java.util.List<LevelSelectController.LevelData> levels = levelController.getLevels();
            if (nextLevelIndex >= levels.size()) {
                showErrorMessage("Invalid next level index");
                return;
            }

            LevelSelectController.LevelData nextLevel = levels.get(nextLevelIndex);
            if (nextLevel == null || nextLevel.getLayout() == null) {
                showErrorMessage("Invalid level data");
                return;
            }

            // 创建新的地图模型
            MapModel mapModel = new MapModel(nextLevel.getLayout(), nextLevelIndex);

            // 停止当前游戏的计时器
            if (gameController != null) {
                gameController.stopTimer();
            }

            // 加载关卡到游戏窗口
            if (parentFrame != null) {
                parentFrame.initializeGamePanel(mapModel);

                // 确保游戏窗口可见（可能在监听器中已设置，但再次确认）
                if (!parentFrame.isVisible()) {
                    parentFrame.setVisible(true);
                    System.out.println("GameFrame made visible during loadNextLevel.");
                }

                // 设置新的关卡索引
                GameController controller = parentFrame.getController();
                if (controller != null) {
                    controller.resetTimer();
                }

                // 确保游戏面板获得焦点以接收键盘事件
                if (parentFrame.getGamePanel() != null) {
                    parentFrame.getGamePanel().requestFocusInWindow();
                }
            } else {
                showErrorMessage("Game window is not available");
            }
        } catch (Exception e) {
            // 使用日志记录代替打印堆栈跟踪
            System.err.println("Error loading next level: " + e.getMessage());
            // 考虑使用更健壮的日志框架
            showErrorMessage("Failed to load next level: " + e.getMessage());
        }
    }

    /**
     * 获取下一关索引
     *
     * @return 下一关索引，如果没有下一关则返回-1
     */
    private int getNextLevelIndex() {
        // 直接从模型获取当前索引
        int currentIdx = -1;
        if (gameController != null && gameController.getModel() != null) {
            currentIdx = gameController.getModel().getCurrentLevelIndex();
        } else {
            System.err.println("Warning: Cannot get current level index in getNextLevelIndex - GameController or Model is null.");
            return -1; // 无法确定当前关卡，返回-1
        }

        LevelSelectFrame levelSelectFrame = FrameManager.getInstance().getLevelSelectFrame();
        if (levelSelectFrame != null) {
            LevelSelectController levelController = levelSelectFrame.getController();
            if (levelController != null) {
                java.util.List<LevelSelectController.LevelData> levels = levelController.getLevels();
                if (levels != null) {
                    int nextIndex = currentIdx + 1;
                    if (nextIndex < levels.size()) {
                        return nextIndex;
                    }
                }
            }
        }
        return -1;  // 表示没有下一关
    }

    /**
     * 检查当前是否为最后一关
     *
     * @return 如果是最后一关返回true，否则返回false
     */
    private boolean isLastLevel() {
        return getNextLevelIndex() == -1;
    }

    /**
     * 显示错误信息
     *
     * @param message 错误信息
     */
    private void showErrorMessage(String message) {
        if (parentFrame != null) {
            JOptionPane.showMessageDialog(
                    parentFrame,
                    message,
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } else {
            System.err.println(message);
        }
    }
}
