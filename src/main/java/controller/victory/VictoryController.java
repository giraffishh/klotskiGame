package controller.victory;

import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controller.GameController;
import controller.LevelSelectController;
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

        // 原有的加载下一关逻辑
        if (levelSelectFrame != null) {
            LevelSelectController levelController = levelSelectFrame.getController();
            if (levelController != null) {
                // 首先隐藏胜利界面（确保界面关闭后再加载新关卡）
                if (victoryView != null) {
                    victoryView.hideVictory();
                }

                // 尝试加载下一关
                boolean success = levelController.loadNextLevel(currentLevelIndex);

                if (success) {
                    // 更新当前关卡索引
                    currentLevelIndex = levelController.getNextLevelIndex(currentLevelIndex);
                } else {
                    // 这部分逻辑理论上不会执行到，因为前面的isLastLevel()检查已经处理了
                    // 但为了代码健壮性，显示确认提示并直接返回主页
                    JLabel messageLabel = new JLabel("Congratulations! You have completed all levels!");
                    messageLabel.setFont(view.util.FontManager.getRegularFont(16));

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
     *
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
}
