package controller;

import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import model.MapModel;
import view.game.GameFrame;
import view.level.LevelSelectFrame;
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

    // 关卡选择界面引用
    private LevelSelectFrame levelSelectFrame;

    // 当前关卡索引
    private int currentLevelIndex = 0;

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
     * 设置关卡选择界面
     *
     * @param levelSelectFrame 关卡选择界面
     */
    public void setLevelSelectFrame(LevelSelectFrame levelSelectFrame) {
        this.levelSelectFrame = levelSelectFrame;
    }

    /**
     * 设置当前关卡索引
     *
     * @param index 关卡索引
     */
    public void setCurrentLevelIndex(int index) {
        this.currentLevelIndex = index;
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
                // 先关闭游戏界面，再显示关卡选择界面
                if (parentFrame != null) {
                    parentFrame.dispose(); // 关闭游戏窗口
                }
                levelSelectFrame.showLevelSelect();
            } else {
                System.err.println("Level selection frame reference is not set");
            }
        });

        // 设置再来一次按钮监听器
        victoryView.setRestartListener(e -> {

            victoryView.hideVictory();
            // 重新开始游戏
            gameController.restartGame();

        });

        // 设置下一关按钮监听器
        victoryView.setNextLevelListener(e -> {
            if (!isLastLevel()) {
                // 先隐藏胜利界面，再加载下一关
                victoryView.hideVictory();
                System.out.println("\nLoading next level...");
                SwingUtilities.invokeLater(this::loadNextLevel); // 使用invokeLater确保UI更新完成后再加载
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

            // 直接返回主页，不需要用户选择
            if (parentFrame != null) {
                parentFrame.returnToHomeDirectly();
            }
            return; // 直接返回，不执行后续加载逻辑
        }

        try {
            // 获取下一关索引
            int nextLevelIndex = getNextLevelIndex();

            if (nextLevelIndex == -1) {
                // 理论上这里不会执行到，因为前面的isLastLevel()已经处理了这种情况
                return;
            }

            // 获取关卡选择控制器
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
            MapModel mapModel = new MapModel(nextLevel.getLayout());

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
                    controller.setCurrentLevelIndex(nextLevelIndex);
                    controller.resetTimer();

                    // 更新当前关卡索引
                    this.currentLevelIndex = nextLevelIndex;
                }

                // 确保游戏面板获得焦点以接收键盘事件
                if (parentFrame.getGamePanel() != null) {
                    parentFrame.getGamePanel().requestFocusInWindow();
                }
            } else {
                showErrorMessage("Game window is not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Failed to load next level: " + e.getMessage());
        }
    }

    /**
     * 获取下一关索引
     *
     * @return 下一关索引，如果没有下一关则返回-1
     */
    private int getNextLevelIndex() {
        if (levelSelectFrame != null) {
            LevelSelectController levelController = levelSelectFrame.getController();
            if (levelController != null) {
                java.util.List<LevelSelectController.LevelData> levels = levelController.getLevels();
                int nextIndex = currentLevelIndex + 1;
                if (nextIndex < levels.size()) {
                    return nextIndex;
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
