package view.game;

import controller.GameController;
import model.MapModel;
import view.util.FrameUtil;
import service.UserSession;
import view.home.HomeFrame;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 游戏主窗口类
 * 包含游戏面板和控制按钮等组件
 * 负责显示游戏界面和处理用户交互
 */
public class GameFrame extends JFrame implements UserSession.UserSessionListener {

    // 游戏控制器，处理游戏逻辑
    private GameController controller;
    // 重新开始按钮
    private JButton restartBtn;
    // 加载游戏按钮
    private JButton loadBtn;
    // 保存游戏按钮
    private JButton saveBtn;
    // 步数显示标签
    private JLabel stepLabel;
    // 最短步数显示标签
    private JLabel minStepsLabel;
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

    /**
     * 创建游戏窗口
     * 
     * @param width 窗口宽度
     * @param height 窗口高度
     * @param mapModel 游戏地图模型
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

        // 创建游戏面板
        gamePanel = new GamePanel(mapModel);
        
        // 调整窗口大小以适应更大的棋盘
        int windowWidth = Math.max(width, gamePanel.getWidth() + 250);
        int windowHeight = Math.max(height, gamePanel.getHeight() + 80);
        this.setSize(windowWidth, windowHeight);
        
        // 将游戏面板居中放置
        int panelX = (windowWidth - gamePanel.getWidth()) / 2 - 60; // 左移一点给右侧控制区留空间
        int panelY = (windowHeight - gamePanel.getHeight()) / 2;
        gamePanel.setLocation(panelX, panelY);
        this.add(gamePanel);
        
        // 创建游戏控制器，关联面板和模型
        this.controller = new GameController(gamePanel, mapModel);

        // 计算右侧控制区域的起始位置和尺寸
        int controlX = panelX + gamePanel.getWidth() + 20;
        int controlY = panelY + 20;
        int controlWidth = windowWidth - controlX - 20;
        int buttonWidth = (controlWidth - 20) / 2; // 两列按钮，中间留20px间距
        
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

        // 将步数标签和最短步数标签设置到游戏面板中
        gamePanel.setStepLabel(stepLabel);
        gamePanel.setMinStepsLabel(minStepsLabel);

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
        controlY += 65;

        // 第三行按钮：居中放置加载按钮
        this.loadBtn = FrameUtil.createStyledButton("Load", false);
        loadBtn.setBounds(controlX + controlWidth/4, controlY, controlWidth/2, 45);
        this.add(loadBtn);

        // 设置GameController对GameFrame的引用，用于更新按钮状态
        controller.setParentFrame(this);

        // 为重新开始按钮添加点击事件监听器
        this.restartBtn.addActionListener(e -> {
            // 重新开始游戏
            controller.restartGame();
            // 将焦点设置回游戏面板以便接收键盘事件
            gamePanel.requestFocusInWindow();
        });
        
        // 为加载游戏按钮添加点击事件监听器
        this.loadBtn.addActionListener(e -> {
            // 调用控制器的加载游戏方法
            controller.loadGameState();

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

        // 更新按钮状态
        updateButtonsState();

        // 将此窗口注册为用户会话监听器
        UserSession.getInstance().addListener(this);

        // 窗口关闭时取消注册监听器
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                UserSession.getInstance().removeListener(GameFrame.this);
            }
        });

        // 在UI组件完全初始化后，调用控制器初始化游戏
        // 这将计算并显示初始的最短步数
        controller.initializeGame();

        // 窗口居中显示
        this.setLocationRelativeTo(null);
        // 设置关闭窗口时退出程序
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    /**
     * 设置主页面窗口引用
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
     * 实现UserSessionListener接口方法，在用户会话状态变化时调用
     */
    @Override
    public void onSessionStateChanged() {
        // 在UI线程中安全地更新按钮状态
        SwingUtilities.invokeLater(this::updateButtonsState);
    }

    /**
     * 更新按钮状态
     * 根据用户会话状态启用或禁用保存和加载按钮
     */
    public void updateButtonsState() {
        UserSession session = UserSession.getInstance();
        boolean isGuest = session.isGuest();

        // 访客模式下禁用保存和加载功能
        loadBtn.setEnabled(!isGuest);
        saveBtn.setEnabled(!isGuest);
    }

    /**
     * 更新撤销和重做按钮状态
     *
     * @param canUndo 是否可以撤销
     * @param canRedo 是否可以重做
     */
    public void updateUndoRedoButtons(boolean canUndo, boolean canRedo) {
        undoBtn.setEnabled(canUndo);
        redoBtn.setEnabled(canRedo);
    }
}
