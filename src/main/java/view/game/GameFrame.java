package view.game;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controller.GameController;
import controller.history.HistoryManager; // 添加导入
import controller.save.SaveManager;       // 添加导入
import model.MapModel;
import service.UserSession;
import view.home.HomeFrame;
import view.level.LevelSelectFrame;
import view.util.FrameUtil;
import view.victory.VictoryFrame;

/**
 * 游戏主窗口类 包含游戏面板和控制按钮等组件 负责显示游戏界面和处理用户交互
 */
public class GameFrame extends JFrame implements UserSession.UserSessionListener {

    // 游戏控制器，处理游戏逻辑
    private GameController controller;
    // 重新开始按钮
    private JButton restartBtn;
    // 保存游戏按钮
    private JButton saveBtn;
    // 步数显示标签
    private JLabel stepLabel;
    // 最短步数显示标签
    private JLabel minStepsLabel;
    // 用时显示标签
    private JLabel timeLabel;
    // 游戏主面板，显示游戏地图
    private GamePanel gamePanel;
    // 撤销按钮
    private JButton undoBtn;
    // 重做按钮
    private JButton redoBtn;
    // 返回主页面按钮
    private JButton homeBtn;
    // 主页面引用
    private HomeFrame homeFrame;
    // 胜利界面
    private VictoryFrame victoryFrame;
    // 添加关卡选择界面引用
    private LevelSelectFrame levelSelectFrame;

    /**
     * 创建游戏窗口
     *
     * @param width 窗口宽度
     * @param height 窗口高度
     * @param mapModel 游戏地图模型，可以为null（此时将在选择关卡时初始化）
     */
    public GameFrame(int width, int height, MapModel mapModel) {
        // 设置窗口标题
        this.setTitle("2025 CS109 Project Demo");
        // 使用绝对布局
        this.setLayout(null);

        // 添加返回主页按钮到左上角
        this.homeBtn = FrameUtil.createStyledButton("Home", false);
        homeBtn.setBounds(10, 10, 80, 35); // 位置在左上角，大小为80x35
        this.add(homeBtn);

        // 创建游戏面板（可以传入null模型，稍后再设置）
        gamePanel = new GamePanel(mapModel);

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

        // 创建游戏控制器，关联面板和模型
        this.controller = new GameController(gamePanel, mapModel);
        controller.setParentFrame(this);

        // 计算右侧控制区域的起始位置和尺寸
        int controlX = panelX + panelWidth + 20;
        int controlY = panelY + 20;
        int controlWidth = windowWidth - controlX - 50; // 右边距从40增加到50
        int buttonWidth = Math.min((controlWidth - 20) / 2, 110); // 限制按钮最大宽度

        // 步数显示标签 - 放在顶部中央
        this.stepLabel = FrameUtil.createTitleLabel("Start", JLabel.CENTER);
        stepLabel.setBounds(controlX, controlY, controlWidth, 30);
        this.add(stepLabel);
        controlY += 40;

        // 最短步数显示标签 - 放在步数标签下方
        this.minStepsLabel = FrameUtil.createTitleLabel("Min Steps: --", JLabel.CENTER);
        minStepsLabel.setBounds(controlX, controlY, controlWidth, 30);
        this.add(minStepsLabel);
        controlY += 50;

        // 用时显示标签 - 放在最短步数标签下方
        this.timeLabel = FrameUtil.createTitleLabel("Time: 00:00.00", JLabel.CENTER);
        timeLabel.setBounds(controlX, controlY, controlWidth, 30);
        this.add(timeLabel);
        controlY += 50;

        // 将步数标签、最短步数标签和时间标签设置到游戏面板中
        gamePanel.setStepLabel(stepLabel);
        gamePanel.setMinStepsLabel(minStepsLabel);
        gamePanel.setTimeLabel(timeLabel);

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
        controller.setVictoryView(victoryFrame);

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
            // 重新开始游戏
            controller.restartGame();
            // 将焦点设置回游戏面板以便接收键盘事件
            gamePanel.requestFocusInWindow();
        });

        // 为保存游戏按钮添加点击事件监听器
        this.saveBtn.addActionListener(e -> {
            // 调用控制器的保存游戏方法
            controller.saveGameState();

            // 将焦点设置回游戏面板以便接收键盘事件
            gamePanel.requestFocusInWindow();
        });

        // 为撤销按钮添加点击事件监听器
        this.undoBtn.addActionListener(e -> {
            // 调用控制器的撤销方法
            controller.undoMove();
            // 将焦点设置回游戏面板以便接收键盘事件
            gamePanel.requestFocusInWindow();
        });

        // 为重做按钮添加点击事件监听器
        this.redoBtn.addActionListener(e -> {
            // 调用控制器的重做方法
            controller.redoMove();
            // 将焦点设置回游戏面板以便接收键盘事件
            gamePanel.requestFocusInWindow();
        });

        // 为返回主页面按钮添加点击事件监听器
        this.homeBtn.addActionListener(e -> {
            // 返回主页面
            returnToHome();
            // 将焦点设置回游戏面板以便接收键盘事件
            gamePanel.requestFocusInWindow();
        });
    }

    /**
     * 加载新关卡
     *
     * @param mapModel 游戏地图模型
     */
    public void initializeGamePanel(MapModel mapModel) {
        if (mapModel == null) {
            System.err.println("Error: Cannot load null MapModel");
            return;
        }

        try {
            // 停止现有计时器
            if (controller != null) {
                controller.stopTimer();
            }

            // 如果已有游戏面板，先从布局中移除
            if (gamePanel != null) {
                this.remove(gamePanel);
            }

            // 创建新的游戏面板
            gamePanel = new GamePanel(mapModel);

            // 计算面板位置，确保居中显示
            int panelX = (this.getWidth() - gamePanel.getWidth()) / 2 - 100; // 从-80改为-100
            int panelY = (this.getHeight() - gamePanel.getHeight()) / 2;
            gamePanel.setLocation(panelX, panelY);

            // 将面板添加到窗口
            this.add(gamePanel);

            // 更新面板的标签引用
            if (stepLabel != null) {
                gamePanel.setStepLabel(stepLabel);
            }
            if (minStepsLabel != null) {
                gamePanel.setMinStepsLabel(minStepsLabel);
            }
            if (timeLabel != null) {
                gamePanel.setTimeLabel(timeLabel);
            }

            // 复用或创建游戏控制器
            if (controller == null) {
                // 首次创建控制器
                controller = new GameController(gamePanel, mapModel);
                controller.setParentFrame(this);
                // 设置必要的引用
                if (victoryFrame != null) {
                    controller.setVictoryView(victoryFrame);
                }
                if (levelSelectFrame != null) {
                    controller.setLevelSelectFrame(levelSelectFrame);
                }
            } else {
                // 复用现有控制器，更新模型和视图引用
                controller.resetWithNewModel(mapModel, gamePanel);
                // 确保 gamePanel 的 controller 引用是最新的
                gamePanel.setController(controller);
            }

            // 初始化游戏（重置求解器、历史、计时器等）
            controller.initializeGame();

            // 更新按钮状态
            updateButtonsState();
            updateUndoRedoButtons(false, false); // 确保按钮状态在加载新关卡时重置

            // 重新设置窗口居中显示
            this.setLocationRelativeTo(null);

            // 刷新窗口布局
            revalidate();
            repaint();

            // 将焦点设置到游戏面板
            gamePanel.requestFocusInWindow();
        } catch (Exception e) {
            e.printStackTrace();
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
     * 设置主页面窗口引用
     *
     * @param homeFrame 主页面窗口
     */
    public void setHomeFrame(HomeFrame homeFrame) {
        this.homeFrame = homeFrame;
    }

    /**
     * 返回主页面
     */
    private void returnToHome() {
        if (homeFrame != null) {
            // 在显示确认对话框前暂停计时器
            controller.stopTimer();

            // 显示确认对话框，询问用户是否确定要返回主界面
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to return to the main menu? \nThe current game progress will be lost.",
                    "Return to Main Menu",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            // 如果用户确认，则返回主界面，但不重置游戏状态
            if (result == JOptionPane.YES_OPTION) {
                // 不再调用controller.restartGame()，防止不必要地重新初始化求解器
                // 只需在下次进入游戏时再重置状态就可以了

                // 显示主页面
                homeFrame.setVisible(true);
                // 隐藏游戏窗口
                this.setVisible(false);
            } else {
                // 用户选择继续游戏，恢复计时器
                controller.startTimer();
            }
            // 如果用户选择否，则什么都不做，继续游戏
        } else {
            // 如果homeFrame为null，显示错误消息
            JOptionPane.showMessageDialog(this,
                    "Cannot return to home page. Home page reference is missing.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 用于从胜利界面返回
     */
    public void returnToHomeDirectly() {
        if (homeFrame != null) {
            // 不再调用controller.restartGame()，防止不必要地重新初始化求解器

            controller.stopTimer();

            // 显示主页面
            homeFrame.setVisible(true);
            // 隐藏游戏窗口
            this.setVisible(false);
        } else {
            // 如果homeFrame为null，显示错误消息
            JOptionPane.showMessageDialog(this,
                    "Cannot return to home page. Home page reference is missing.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 设置关卡选择界面引用
     */
    public void setLevelSelectFrame(LevelSelectFrame levelSelectFrame) {
        this.levelSelectFrame = levelSelectFrame;
        if (controller != null) {
            controller.setLevelSelectFrame(levelSelectFrame);
        }
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
     * 实现UserSessionListener接口方法，在用户会话状态变化时调用
     */
    @Override
    public void onSessionStateChanged() {
        // 在UI线程中安全地更新按钮状态
        SwingUtilities.invokeLater(this::updateButtonsState);
    }

    /**
     * 更新按钮状态 根据用户会话状态启用或禁用保存按钮
     */
    public void updateButtonsState() {
        UserSession session = UserSession.getInstance();
        boolean isGuest = session.isGuest();

        // 访客模式下禁用保存功能
        saveBtn.setEnabled(!isGuest);
    }

    /**
     * 更新撤销和重做按钮状态
     *
     * @param canUndo 是否可以撤销
     * @param canRedo 是否可以重做
     */
    public void updateUndoRedoButtons(boolean canUndo, boolean canRedo) {
        // 添加空指针检查，确保按钮已经初始化
        if (undoBtn != null) {
            undoBtn.setEnabled(canUndo);
        }
        if (redoBtn != null) {
            redoBtn.setEnabled(canRedo);
        }
    }

    /**
     * 停止计时器
     */
    public void stopTimer() {
        if (controller != null) {
            controller.stopTimer();
        }
    }

    /**
     * 启动计时器
     */
    public void startTimer() {
        if (controller != null) {
            controller.startTimer();
        }
    }

    /**
     * 重置计时器
     */
    public void resetTimer() {
        if (controller != null) {
            controller.resetTimer();
        }
    }
}
