package controller;

import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import model.MapModel;
import model.User; // 确保导入 User 类
import service.RankingDatabase; // 新增导入
import service.UserSession; // 新增导入
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
            victoryView.hideVictory();
            // 重新开始游戏
            // 如果游戏是从存档加载的，首先会在restartGame方法中处理
            gameController.restartGame();
        });

        // 设置下一关按钮监听器
        victoryView.setNextLevelListener(e -> {
            if (!isLastLevel()) {
                // 先隐藏胜利界面，再加载下一关
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

            // --- 上传分数到排行榜 (如果不是访客) ---
            if (!UserSession.getInstance().isGuest()) {
                uploadScoreToLeaderboard(currentMoveCount, gameTimeInMillis);
            } else {
                System.out.println("访客模式，跳过分数上传。");
            }
            // --- 上传分数结束 ---

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
                } else {
                    // 如果胜利视图未设置，使用旧的对话框显示
                    JLabel messageLabel = new JLabel("Congratulations! You have completed the Klotski challenge!");
                    messageLabel.setFont(view.util.FontManager.getTitleFont(16));
                    JOptionPane.showMessageDialog(parentFrame, messageLabel, "Victory", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            return true;
        }
        return false;
    }

    /**
     * 将当前游戏成绩上传到排行榜数据库
     *
     * @param moves 步数
     * @param timeInMillis 用时（毫秒）
     */
    private void uploadScoreToLeaderboard(int moves, long timeInMillis) {
        // 再次检查是否为访客
        if (UserSession.getInstance().isGuest()) {
            System.out.println("访客模式无法上传分数。");
            return;
        }

        RankingDatabase rankingDb = RankingDatabase.getInstance();
        // 检查数据库是否连接成功
        if (!rankingDb.isConnected()) {
            System.out.println("排行榜数据库未连接，跳过分数上传。");
            return;
        }

        // 获取当前用户对象
        User currentUser = UserSession.getInstance().getCurrentUser();

        // 检查用户对象是否为 null (理论上非访客模式下不应为 null)
        if (currentUser == null) {
            throw new IllegalStateException("非访客用户尝试上传分数，但 UserSession 中的 currentUser 为 null。");
        }

        // 获取当前玩家名称
        String playerName = currentUser.getUsername();

        // 检查获取到的用户名是否有效 (理论上已登录用户应该有用户名)
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalStateException("非访客用户尝试上传分数，但无法从 User 对象获取有效的用户名。");
        }

        // 获取当前关卡信息
        MapModel model = gameController.getModel();
        if (model == null) {
            System.err.println("无法获取地图模型，无法上传分数。");
            return;
        }
        int levelIndex = model.getCurrentLevelIndex();
        // 尝试获取关卡名称 (如果LevelData可用)
        String levelName = "Level " + (levelIndex + 1); // 默认名称
        try {
            LevelSelectFrame lsf = FrameManager.getInstance().getLevelSelectFrame();
            if (lsf != null && lsf.getController() != null) {
                java.util.List<LevelSelectController.LevelData> levels = lsf.getController().getLevels();
                if (levels != null && levelIndex >= 0 && levelIndex < levels.size()) {
                    // 确保获取到的名称不为空
                    String fetchedName = levels.get(levelIndex).getName();
                    if (fetchedName != null && !fetchedName.trim().isEmpty()) {
                        levelName = fetchedName; // 获取真实的关卡名称
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取关卡名称时出错: " + e.getMessage());
        }

        System.out.println("Uploading Score: Player=" + playerName + ", LevelIndex=" + levelIndex + ", LevelName=" + levelName + ", Moves=" + moves + ", Time=" + timeInMillis);

        final String finalPlayerName = playerName;
        final int finalLevelIndex = levelIndex;
        final int finalMoves = moves;
        final long finalTimeInMillis = timeInMillis;

        // 在后台线程上传分数，避免阻塞UI线程
        new Thread(() -> {
            try {
                // 使用 final 副本
                rankingDb.uploadScore(finalPlayerName, finalLevelIndex, finalMoves, finalTimeInMillis);
            } catch (Exception e) {
                // 记录上传失败，但通常不打断用户流程
                System.err.println("后台上传分数时发生错误: " + e.getMessage());
            }
        }).start();
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
