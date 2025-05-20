package view.game;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controller.core.GameController;
import model.AppSettings;
import model.MapModel;
import service.UserSession;
import view.util.FrameManager;
import view.util.FrameUtil;
import view.util.ImageManager;
import view.victory.VictoryFrame;
import view.util.SvgIconManager;

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
    // 提示按钮
    private JButton hintBtn; // 新增提示按钮

    // 方向控制按钮 
    private JButton upBtn;
    private JButton downBtn;
    private JButton leftBtn;
    private JButton rightBtn;

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
        this.homeBtn = FrameUtil.createStyledButton("Home", false, SvgIconManager.getHomeIcon());
        homeBtn.setBounds(10, 10, 100, 35); // 位置在左上角，大小为100x35
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
        int controlX = panelX + panelWidth - 10;
        int controlY = panelY;
        int controlWidth = windowWidth - controlX - 50; // 右边距从40增加到50
        int buttonWidth = Math.min((controlWidth - 20) / 2 + 20, 120); // 限制按钮最大宽度

        // 用时显示标签 - 放在最上方
        this.timeLabel = FrameUtil.createTitleLabel("Time: 00:00.00", JLabel.CENTER);
        timeLabel.setBounds(controlX, controlY, controlWidth, 30);
        this.add(timeLabel);
        controlY += 35;

        // 步数显示标签 - 放在中间
        this.stepLabel = FrameUtil.createTitleLabel("Steps: 0", JLabel.CENTER);
        stepLabel.setBounds(controlX, controlY, controlWidth, 30);
        this.add(stepLabel);
        controlY += 35;

        // 最短步数显示标签 - 放在最下方
        this.minStepsLabel = FrameUtil.createTitleLabel("Min Steps: --", JLabel.CENTER);
        minStepsLabel.setBounds(controlX, controlY, controlWidth, 30);
        this.add(minStepsLabel);
        controlY += 40;

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
        this.restartBtn = FrameUtil.createStyledButton("Restart", true, SvgIconManager.getRestartIcon());
        restartBtn.setBounds(controlX, controlY, buttonWidth, 45);
        this.add(restartBtn);

        this.saveBtn = FrameUtil.createStyledButton("Save", true, SvgIconManager.getSaveIcon());
        saveBtn.setBounds(controlX + buttonWidth + 20, controlY, buttonWidth, 45);
        this.add(saveBtn);
        controlY += 55;

        // 第二行按钮：左侧撤销，右侧重做
        this.undoBtn = FrameUtil.createStyledButton("Undo", false, SvgIconManager.getUndoIcon());
        undoBtn.setBounds(controlX, controlY, buttonWidth, 45);
        undoBtn.setEnabled(false); // 初始时禁用
        this.add(undoBtn);

        this.redoBtn = FrameUtil.createStyledButton("Redo", false, SvgIconManager.getRedoIcon());
        redoBtn.setBounds(controlX + buttonWidth + 20, controlY, buttonWidth, 45);
        redoBtn.setEnabled(false); // 初始时禁用
        this.add(redoBtn);
        controlY += 55;

        // 提示按钮 - 放在撤销/重做下方
        this.hintBtn = FrameUtil.createStyledButton("Hint", true, SvgIconManager.getHintIcon()); // 添加图标
        hintBtn.setBounds(controlX + buttonWidth / 2 + 10, controlY, buttonWidth, 45);
        this.add(hintBtn);
        controlY += 55;

        // 使用新方法添加方向控制按钮
        addDirectionButtons(controlX, controlY, controlWidth, buttonWidth);

        // 初始化胜利界面
        this.victoryFrame = new VictoryFrame(this);

        // 添加按钮事件监听器
        addButtonListeners();

        // 将此窗口注册为用户会话监听器
        UserSession.getInstance().addListener(this);

        // 窗口关闭时取消注册监听器
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 阻止默认关闭行为
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 调用 returnToHome 方法，该方法包含确认对话框
                // 如果用户确认返回主页，returnToHome 会处理导航
                // 如果用户取消，则不执行任何操作，窗口保持打开
                boolean shouldClose = returnToHomeAndConfirm();
                if (shouldClose) {
                    // 如果 returnToHomeAndConfirm 返回 true (用户确认且导航成功)
                    // 那么我们已经在 returnToHomeAndConfirm 中调用了 controller.shutdown()
                    UserSession.getInstance().removeListener(GameFrame.this);
                    // dispose(); // 如果需要释放资源，可以取消注释，但通常 setVisible(false) 就够了
                }
                // 如果 shouldClose 为 false，则不执行任何操作，窗口保持打开
            }

            @Override
            public void windowClosed(WindowEvent e) {
                // 在窗口完全关闭时确保网络连接被关闭
                if (controller != null) {
                    controller.shutdown();
                }
            }
        });

        // 添加窗口大小变化监听器
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustLayoutForNewSize();
            }
        });
    }

    /**
     * 调整窗口大小变化时的界面布局
     */
    private void adjustLayoutForNewSize() {
        if (gamePanel == null || gamePanel.getModel() == null) {
            return;
        }

        int frameWidth = getWidth();
        int frameHeight = getHeight();

        // 计算控制区域所需最小宽度
        int minControlWidth = 220;

        // 计算游戏面板的最大可用空间
        int maxPanelWidth = frameWidth - minControlWidth - 60; // 左右边距
        int maxPanelHeight = frameHeight - 100; // 上下边距

        // 获取地图尺寸
        int mapWidth = gamePanel.getModel().getWidth();
        int mapHeight = gamePanel.getModel().getHeight();

        // 计算新的网格大小，保持地图的宽高比
        int newGridSize = Math.min(
                maxPanelWidth / mapWidth,
                maxPanelHeight / mapHeight
        );

        // 设置最小网格大小限制
        newGridSize = Math.max(newGridSize, 30);

        // 更新游戏面板的网格大小 (这也会更新 gamePanel 的尺寸)
        gamePanel.setGridSize(newGridSize);

        // 获取游戏面板更新后的实际尺寸
        int newPanelWidth = gamePanel.getWidth();
        int newPanelHeight = gamePanel.getHeight();

        // 计算控制区域宽度，至少保证minControlWidth
        int controlWidth = Math.max(minControlWidth, frameWidth - newPanelWidth - 100);

        // 重新定位游戏面板
        int panelX = 50; // X 坐标保持不变 (左侧边距50)

        // --- 新的 panelY 计算逻辑 ---
        int panelY_centered = (frameHeight - newPanelHeight) / 2; // 原始居中 Y

        int downwardShift = 15; // 向下移动的像素量
        int minPanelYClearance = 60; // 棋盘顶部的最小 Y 坐标 (确保在 Home 按钮下方，Home 按钮底部 y=45, 45+15=60)
        int minBottomPanelMargin = 25; // 棋盘底部与窗口底部的最小间距

        int panelY = panelY_centered + downwardShift;

        // 确保 panelY 不小于最小顶部间隙
        panelY = Math.max(panelY, minPanelYClearance);

        // 确保 panelY 不会导致底部间隙过小
        // (frameHeight - newPanelHeight) 是总的垂直空白区域
        // panelY 最大不能超过 frameHeight - newPanelHeight - minBottomPanelMargin
        panelY = Math.min(panelY, frameHeight - newPanelHeight - minBottomPanelMargin);

        // 最后再次确保 panelY 不会因为 minBottomPanelMargin 的限制而变得过小，至少要满足 minPanelYClearance
        // 这种情况可能在 frameHeight 非常小，导致 minPanelYClearance 和 minBottomPanelMargin 冲突时发生
        if (panelY < minPanelYClearance && (newPanelHeight + minPanelYClearance + minBottomPanelMargin <= frameHeight)) {
            panelY = minPanelYClearance;
        }
        // --- 结束新的 panelY 计算逻辑 ---

        gamePanel.setLocation(panelX, panelY);

        // 更新控制组件位置
        updateControlComponentsPosition(panelX + newPanelWidth + 20, panelY, controlWidth, newPanelHeight);
    }

    /**
     * 更新控制组件的位置
     */
    private void updateControlComponentsPosition(int x, int y, int width, int height) {
        // Home按钮保持在左上角
        homeBtn.setBounds(20, 20, 100, 35);

        // 计算按钮宽度
        int buttonWidth = Math.min((width - 20) / 2, 120);
        int buttonPairGap = 10; // 按钮对之间的间隙

        // 时间标签
        timeLabel.setBounds(x, y, width, 30);

        // 步数标签
        stepLabel.setBounds(x, y + 35, width, 30);

        // 最短步数标签
        minStepsLabel.setBounds(x, y + 70, width, 30);

        // --- 居中按钮对的计算 ---
        // 一对按钮（例如重启和保存）及其间隙的总宽度
        int totalButtonPairWidth = 2 * buttonWidth + buttonPairGap;
        // 按钮对在控制区域内居中时的起始 X 偏移量（相对于控制区域的 x）
        int buttonPairStartXOffset = (width - totalButtonPairWidth) / 2;
        // 按钮对的绝对起始 X 坐标
        int absoluteButtonPairStartX = x + buttonPairStartXOffset;

        // 重启和保存按钮
        restartBtn.setBounds(absoluteButtonPairStartX, y + 110, buttonWidth, 45);
        saveBtn.setBounds(absoluteButtonPairStartX + buttonWidth + buttonPairGap, y + 110, buttonWidth, 45);

        // 撤销和重做按钮
        undoBtn.setBounds(absoluteButtonPairStartX, y + 165, buttonWidth, 45);
        redoBtn.setBounds(absoluteButtonPairStartX + buttonWidth + buttonPairGap, y + 165, buttonWidth, 45);

        // 提示按钮 (单个按钮居中)
        hintBtn.setBounds(x + (width - buttonWidth) / 2, y + 220, buttonWidth, 45);

        // 方向按钮
        int dirButtonSize = 40;
        int dirButtonGap = 15;
        int centerX = x + width / 2 - dirButtonSize / 2;
        int dirControlsY = y + 275;

        // 上方向按钮
        upBtn.setBounds(centerX, dirControlsY, dirButtonSize, dirButtonSize);

        // 左、下、右方向按钮
        leftBtn.setBounds(centerX - dirButtonSize - dirButtonGap, dirControlsY + dirButtonSize + dirButtonGap,
                dirButtonSize, dirButtonSize);
        downBtn.setBounds(centerX, dirControlsY + dirButtonSize + dirButtonGap,
                dirButtonSize, dirButtonSize);
        rightBtn.setBounds(centerX + dirButtonSize + dirButtonGap, dirControlsY + dirButtonSize + dirButtonGap,
                dirButtonSize, dirButtonSize);

        // 重新绘制
        revalidate();
        repaint();
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
            returnToHomeAndConfirm(); // 修改为调用新的确认方法
            if (gamePanel != null) {
                gamePanel.requestFocusInWindow();
            }
        });

        // 为提示按钮添加点击事件监听器
        this.hintBtn.addActionListener(e -> {
            if (controller != null) {
                controller.showNextMoveHint(); // 调用 GameController 中的方法
                gamePanel.requestFocusInWindow();
            } else {
                System.err.println("Hint button clicked but controller is null.");
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
     * 返回主页面，并处理用户的确认。
     *
     * @return 如果用户确认返回并且导航成功，则返回 true；否则返回 false。
     */
    private boolean returnToHomeAndConfirm() {
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
            // 在导航到主页之前先关闭控制器和网络连接
            if (controller != null) {
                controller.shutdown(); // 确保在关闭窗口时关闭网络连接
            }
            FrameManager.getInstance().navigateFromGameToHome();
            return true; // 用户确认，导航已执行
        } else {
            if (controller != null) {
                controller.getTimerManager().startTimer(); // 用户取消，恢复计时器
            }
            return false; // 用户取消
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

            // 在初始化完成后更新控制按钮可见性
            updateControlButtonsVisibility();

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
     * 设置提示按钮的可见性。
     *
     * @param visible 是否可见
     */
    public void setHintButtonVisible(boolean visible) {
        if (hintBtn != null) {
            hintBtn.setVisible(visible);
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
     * 刷新游戏面板 当设置改变时需要重新加载图片资源
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

        // 更新控制按钮的可见性
        updateControlButtonsVisibility();
    }

    // 添加方向控制按钮
    private void addDirectionButtons(int controlX, int controlY, int controlWidth, int buttonWidth) {
        // 检查是否启用控制按钮
        AppSettings appSettings = AppSettings.getInstance();
        boolean controlButtonsEnabled = appSettings.isControlButtonsEnabled();

        // 创建按钮
        int dirButtonSize = 40; // 方向按钮大小
        int dirButtonGap = 15;  // 按钮之间的间隙

        // 计算方向按钮的位置
        int centerX = controlX + controlWidth / 2 - dirButtonSize / 2;

        // 上方向按钮
        this.upBtn = FrameUtil.createDirectionButton("↑");
        upBtn.setBounds(centerX, controlY, dirButtonSize, dirButtonSize);
        upBtn.setVisible(controlButtonsEnabled);
        this.add(upBtn);

        controlY += dirButtonSize + dirButtonGap;

        // 左、右方向按钮
        this.leftBtn = FrameUtil.createDirectionButton("←");
        leftBtn.setBounds(centerX - dirButtonSize - dirButtonGap, controlY, dirButtonSize, dirButtonSize);
        leftBtn.setVisible(controlButtonsEnabled);
        this.add(leftBtn);

        this.rightBtn = FrameUtil.createDirectionButton("→");
        rightBtn.setBounds(centerX + dirButtonSize + dirButtonGap, controlY, dirButtonSize, dirButtonSize);
        rightBtn.setVisible(controlButtonsEnabled);
        this.add(rightBtn);

        // 下方向按钮
        this.downBtn = FrameUtil.createDirectionButton("↓");
        downBtn.setBounds(centerX, controlY, dirButtonSize, dirButtonSize);
        downBtn.setVisible(controlButtonsEnabled);
        this.add(downBtn);
    }

    /**
     * 更新控制按钮的可见性
     */
    public void updateControlButtonsVisibility() {
        boolean controlButtonsEnabled = AppSettings.getInstance().isControlButtonsEnabled();

        if (upBtn != null) {
            upBtn.setVisible(controlButtonsEnabled);
        }
        if (downBtn != null) {
            downBtn.setVisible(controlButtonsEnabled);
        }
        if (leftBtn != null) {
            leftBtn.setVisible(controlButtonsEnabled);
        }
        if (rightBtn != null) {
            rightBtn.setVisible(controlButtonsEnabled);
        }

        this.revalidate();
        this.repaint();
    }
}
