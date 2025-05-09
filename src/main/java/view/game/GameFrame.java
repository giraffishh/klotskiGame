package view.game;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controller.core.GameController;
import model.MapModel;
import service.UserSession;
import view.util.FrameManager;
import view.util.FrameUtil;
import view.util.ImageManager;
import view.victory.VictoryFrame;

/**
 * 游戏主窗口类 包含游戏面板和控制按钮等组件 负责显示游戏界面和处理用户交互
 */
public class GameFrame extends JFrame implements UserSession.UserSessionListener {

    // 游戏控制器，处理游戏逻辑
    private GameController controller;
    // 重新开始按钮
    private final JButton restartBtn;
    // 保存游戏按钮
    private final JButton saveBtn;
    // 步数显示标签
    private final JLabel stepLabel;
    // 最短步数显示标签
    private final JLabel minStepsLabel;
    // 用时显示标签
    private final JLabel timeLabel;
    // 游戏主面板，显示游戏地图
    private GamePanel gamePanel;
    // 撤销按钮
    private final JButton undoBtn;
    // 重做按钮
    private final JButton redoBtn;
    // 返回主页面按钮
    private final JButton homeBtn;

    // 胜利界面
    private final VictoryFrame victoryFrame;

    /**
     * 创建游戏窗口
     *
     * @param width 窗口宽度
     * @param height 窗口高度
     * @param mapModel 游戏地图模型，可以为null（此时将在选择关卡时初始化）
     */
    public GameFrame(int width, int height, MapModel mapModel) { // mapModel is likely null here
        // 设置窗口标题
        this.setTitle("2025 CS109 Project Demo");
        // 使用绝对布局
        this.setLayout(null);

        // 添加返回主页按钮到左上角
        this.homeBtn = FrameUtil.createStyledButton("Home", false);
        homeBtn.setBounds(10, 10, 80, 35); // 位置在左上角，大小为80x35
        this.add(homeBtn);

        // 创建游戏面板（可以传入null模型，稍后再设置）
        gamePanel = new GamePanel(null); // Pass null, model will be set in initializeGamePanel

        // 使用默认大小计算窗口尺寸
        int panelWidth = gamePanel.getWidth();
        int panelHeight = gamePanel.getHeight();
        // 增加窗口宽度，给右侧控制区域留出更多空间
        int windowWidth = Math.max(width, panelWidth + 350); // 从300增加到350
        int windowHeight = Math.max(height, panelHeight + 80);
        this.setSize(windowWidth, windowHeight);

        // 设置窗口居中显示
        this.setLocationRelativeTo(null);

        // 将游戏面板居中放置
        int panelX = (windowWidth - panelWidth) / 2 - 100; // 从-80增加到-100，给右侧更多空间
        int panelY = (windowHeight - panelHeight) / 2;
        gamePanel.setLocation(panelX, panelY);
        this.add(gamePanel);

        // 计算右侧控制区域的起始位置和尺寸
        int controlX = panelX + panelWidth + 20;
        int controlY = panelY + 20;
        int controlWidth = windowWidth - controlX - 50; // 右边距从40增加到50
        int buttonWidth = Math.min((controlWidth - 20) / 2, 110); // 限制按钮最大宽度

        // 用时显示标签 - 放在最上方
        this.timeLabel = FrameUtil.createTitleLabel("Time: 00:00.00", JLabel.CENTER);
        timeLabel.setBounds(controlX, controlY, controlWidth, 30);
        this.add(timeLabel);
        controlY += 40;

        // 步数显示标签 - 放在中间
        this.stepLabel = FrameUtil.createTitleLabel("Steps: 0", JLabel.CENTER);
        stepLabel.setBounds(controlX, controlY, controlWidth, 30);
        this.add(stepLabel);
        controlY += 40;

        // 最短步数显示标签 - 放在最下方
        this.minStepsLabel = FrameUtil.createTitleLabel("Min Steps: --", JLabel.CENTER);
        minStepsLabel.setBounds(controlX, controlY, controlWidth, 30);
        this.add(minStepsLabel);
        controlY += 50;

        // 将步数标签、最短步数标签和时间标签设置到游戏面板中
        if (stepLabel != null) {
            gamePanel.setStepLabel(stepLabel);
        }
        if (minStepsLabel != null) {
            gamePanel.setMinStepsLabel(minStepsLabel);
        }
        if (timeLabel != null) {
            gamePanel.setTimeLabel(timeLabel);
        }

        // 第一行按钮：左侧重启，右侧保存
        this.restartBtn = FrameUtil.createStyledButton("Restart", true);
        restartBtn.setBounds(controlX, controlY, buttonWidth, 45);
        this.add(restartBtn);

        this.saveBtn = FrameUtil.createStyledButton("Save", true);
        saveBtn.setBounds(controlX + buttonWidth + 20, controlY, buttonWidth, 45);
        this.add(saveBtn);
        controlY += 65;

        // 第二行按钮：左侧撤销，右侧重做
        this.undoBtn = FrameUtil.createStyledButton("Undo", false);
        undoBtn.setBounds(controlX, controlY, buttonWidth, 45);
        undoBtn.setEnabled(false); // 初始时禁用
        this.add(undoBtn);

        this.redoBtn = FrameUtil.createStyledButton("Redo", false);
        redoBtn.setBounds(controlX + buttonWidth + 20, controlY, buttonWidth, 45);
        redoBtn.setEnabled(false); // 初始时禁用
        this.add(redoBtn);

        // 初始化胜利界面
        this.victoryFrame = new VictoryFrame(this);

        // 添加按钮事件监听器
        addButtonListeners();

        // 将此窗口注册为用户会话监听器
        UserSession.getInstance().addListener(this);

        // 窗口关闭时取消注册监听器
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                UserSession.getInstance().removeListener(GameFrame.this);
            }
        });
    }

    /**
     * 添加按钮事件监听器
     */
    private void addButtonListeners() {
        // 为重新开始按钮添加点击事件监听器
        this.restartBtn.addActionListener(e -> {
            if (controller != null) {
                controller.restartGame();
                gamePanel.requestFocusInWindow();
            } else {
                System.err.println("Restart button clicked but controller is null.");
            }
        });

        // 为保存游戏按钮添加点击事件监听器
        this.saveBtn.addActionListener(e -> {
            if (controller != null) {
                controller.saveGameState();
                gamePanel.requestFocusInWindow();
            } else {
                System.err.println("Save button clicked but controller is null.");
            }
        });

        // 为撤销按钮添加点击事件监听器
        this.undoBtn.addActionListener(e -> {
            if (controller != null) {
                controller.undoMove();
                gamePanel.requestFocusInWindow();
            } else {
                System.err.println("Undo button clicked but controller is null.");
            }
        });

        // 为重做按钮添加点击事件监听器
        this.redoBtn.addActionListener(e -> {
            if (controller != null) {
                controller.redoMove();
                gamePanel.requestFocusInWindow();
            } else {
                System.err.println("Redo button clicked but controller is null.");
            }
        });

        // 为返回主页面按钮添加点击事件监听器
        this.homeBtn.addActionListener(e -> {
            returnToHome();
            if (gamePanel != null) {
                gamePanel.requestFocusInWindow();
            }
        });
        
        // 为方向按钮添加事件监听器
        this.upBtn.addActionListener(e -> {
            if (gamePanel != null) {
                gamePanel.doMoveUp();
                gamePanel.requestFocusInWindow();
            }
        });
        
        this.downBtn.addActionListener(e -> {
            if (gamePanel != null) {
                gamePanel.doMoveDown();
                gamePanel.requestFocusInWindow();
            }
        });
        
        this.leftBtn.addActionListener(e -> {
            if (gamePanel != null) {
                gamePanel.doMoveLeft();
                gamePanel.requestFocusInWindow();
            }
        });
        
        this.rightBtn.addActionListener(e -> {
            if (gamePanel != null) {
                gamePanel.doMoveRight();
                gamePanel.requestFocusInWindow();
            }
        });
    }

    /**
     * 返回主页面
     */
    private void returnToHome() {
        if (controller != null) {
            controller.getTimerManager().stopTimer();
        } else {
            System.err.println("Return to home called but controller is null.");
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to return to the main menu? \nThe current game progress will be lost.",
                "Return to Main Menu",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            FrameManager.getInstance().navigateFromGameToHome();
        } else {
            if (controller != null) {
                controller.getTimerManager().startTimer();
            }
        }
    }

    /**
     * 初始化或重置游戏面板，并创建/重置控制器
     *
     * @param mapModel 游戏地图模型 (保证非 null)
     */
    public void initializeGamePanel(MapModel mapModel) {
        if (mapModel == null) {
            System.err.println("CRITICAL ERROR: initializeGamePanel called with null MapModel!");
            JOptionPane.showMessageDialog(this, "Failed to load level: Invalid map data.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (controller != null) {
                controller.getTimerManager().stopTimer();
            }

            if (gamePanel == null) {
                gamePanel = new GamePanel(mapModel);
                int panelX = (this.getWidth() - gamePanel.getWidth()) / 2 - 100;
                int panelY = (this.getHeight() - gamePanel.getHeight()) / 2;
                gamePanel.setLocation(panelX, panelY);
                this.add(gamePanel);
            } else {
                gamePanel.setModel(mapModel);
                gamePanel.resetGame();
            }

            if (stepLabel != null) {
                gamePanel.setStepLabel(stepLabel);
            }
            if (minStepsLabel != null) {
                gamePanel.setMinStepsLabel(minStepsLabel);
            }
            if (timeLabel != null) {
                gamePanel.setTimeLabel(timeLabel);
            }

            if (controller == null) {
                controller = new GameController(gamePanel, mapModel);
                controller.setParentFrame(this);
                if (victoryFrame != null) {
                    controller.setVictoryView(victoryFrame);
                } else {
                    System.err.println("Warning: VictoryFrame is null when setting VictoryView in GameController.");
                }
            } else {
                controller.resetWithNewModel(mapModel, gamePanel);
            }
            gamePanel.setController(controller);

            controller.initializeGame();

            updateButtonsState();
            updateUndoRedoButtons(false, false);

            this.setLocationRelativeTo(null);
            revalidate();
            repaint();

            gamePanel.requestFocusInWindow();

        } catch (Exception e) {
            System.err.println("Failed to load level: " + e.getMessage() + " Exception details: " + e);
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to load level: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * 获取游戏面板
     *
     * @return 游戏面板
     */
    public GamePanel getGamePanel() {
        return gamePanel;
    }

    /**
     * 获取游戏控制器
     *
     * @return 游戏控制器实例
     */
    public GameController getController() {
        return controller;
    }

    /**
     * 设置保存按钮的可用状态
     *
     * @param enabled 是否可用
     */
    public void setSaveButtonEnabled(boolean enabled) {
        if (saveBtn != null) {
            saveBtn.setEnabled(enabled);
        }
    }

    /**
     * 更新按钮状态 根据用户会话状态启用或禁用保存按钮
     */
    public void updateButtonsState() {
        UserSession session = UserSession.getInstance();
        boolean isGuest = session.isGuest();

        // 检查游戏模式，如果是竞速模式则禁用保存按钮
        boolean canSave = !isGuest;
        if (controller != null && controller.getModel() != null) {
            canSave = canSave && controller.getModel().getGameMode() != MapModel.SPEED_MODE;
        }

        saveBtn.setEnabled(canSave);
    }

    /**
     * 实现UserSessionListener接口方法，在用户会话状态变化时调用
     */
    @Override
    public void onSessionStateChanged() {
        SwingUtilities.invokeLater(this::updateButtonsState);
    }

    /**
     * 更新撤销和重做按钮状态
     *
     * @param canUndo 是否可以撤销
     * @param canRedo 是否可以重做
     */
    public void updateUndoRedoButtons(boolean canUndo, boolean canRedo) {
        if (undoBtn != null) {
            undoBtn.setEnabled(canUndo);
        }
        if (redoBtn != null) {
            redoBtn.setEnabled(canRedo);
        }
    }

    /**
     * 设置最短步数标签的可见性
     *
     * @param visible 是否可见
     */
    public void setMinStepsLabelVisible(boolean visible) {
        if (minStepsLabel != null) {
            minStepsLabel.setVisible(visible);
        }
    }

    /**
     * 停止计时器
     */
    public void stopTimer() {
        if (controller != null) {
            controller.getTimerManager().stopTimer();
        }
    }

    /**
     * 启动计时器
     */
    public void startTimer() {
        if (controller != null) {
            controller.getTimerManager().startTimer();
        }
    }

    /**
     * 重置计时器
     */
    public void resetTimer() {
        if (controller != null) {
            controller.getTimerManager().resetTimer();
        }
    }

    /**
     * 刷新游戏面板
     * 当设置改变时需要重新加载图片资源
     */
    public void refreshGamePanel() {
        // 如果游戏面板为空，无需刷新
        if (gamePanel == null) {
            return;
        }
        
        // 刷新面板中的所有方块图像
        for (BoxComponent box : gamePanel.getBoxes()) {
            // 根据方块类型重新加载对应图片
            switch (box.getBlockType()) {
                case 4: // 曹操
                    box.setImage(ImageManager.getCaoCaoImage());
                    break;
                case 3: // 黄忠（垂直方块）
                    box.setImage(ImageManager.getHuangZhongImage());
                    break;
                case 2: // 关羽（水平方块）
                    box.setImage(ImageManager.getGuanYuImage());
                    break;
                case 1: // 士兵
                    box.setImage(ImageManager.getSoldierImage());
                    break;
            }
        }
        
        // 重绘游戏面板
        gamePanel.repaint();
    }
}
