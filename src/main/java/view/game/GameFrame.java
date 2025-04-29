package view.game;

import controller.GameController;
import model.MapModel;
import view.level.LevelSelectFrame;
import view.util.FrameUtil;
import service.UserSession;
import view.home.HomeFrame;
import view.victory.VictoryFrame;

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
    // 胜利界面
    private VictoryFrame victoryFrame;

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

        // 设置窗口大小
        this.setSize(width, height);

        // 初始化UI组件
        initializeUIComponents();
        
        // 如果提供了地图模型，则立即初始化游戏面板
        if (mapModel != null) {
            initializeGamePanel(mapModel);
        }

        // 窗口居中显示
        this.setLocationRelativeTo(null);
        // 设置关闭窗口时退出程序
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    /**
     * 初始化UI组件
     */
    private void initializeUIComponents() {
        // 步数显示标签
        this.stepLabel = FrameUtil.createTitleLabel("Start", JLabel.CENTER);
        this.stepLabel.setBounds(this.getWidth() - 250, 80, 200, 30); 
        this.add(stepLabel);

        // 最短步数显示标签
        this.minStepsLabel = FrameUtil.createTitleLabel("Min Steps: --", JLabel.CENTER);
        this.minStepsLabel.setBounds(this.getWidth() - 250, 120, 200, 30);
        this.add(minStepsLabel);

        // 初始化按钮
        this.restartBtn = FrameUtil.createStyledButton("Restart", true);
        this.restartBtn.setBounds(this.getWidth() - 250, 170, 90, 45);
        this.add(restartBtn);
        
        this.saveBtn = FrameUtil.createStyledButton("Save", true);
        this.saveBtn.setBounds(this.getWidth() - 140, 170, 90, 45);
        this.add(saveBtn);
        
        this.undoBtn = FrameUtil.createStyledButton("Undo", false);
        this.undoBtn.setBounds(this.getWidth() - 250, 235, 90, 45);
        this.undoBtn.setEnabled(false);
        this.add(undoBtn);
        
        this.redoBtn = FrameUtil.createStyledButton("Redo", false);
        this.redoBtn.setBounds(this.getWidth() - 140, 235, 90, 45);
        this.redoBtn.setEnabled(false);
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
                if (gamePanel != null) {
                    gamePanel.requestFocusInWindow();
                }
            }
        });

        // 为保存游戏按钮添加点击事件监听器
        this.saveBtn.addActionListener(e -> {
            if (controller != null) {
                controller.saveGameState();
                if (gamePanel != null) {
                    gamePanel.requestFocusInWindow();
                }
            }
        });

        // 为撤销按钮添加点击事件监听器
        this.undoBtn.addActionListener(e -> {
            if (controller != null) {
                controller.undoMove();
                if (gamePanel != null) {
                    gamePanel.requestFocusInWindow();
                }
            }
        });
        
        // 为重做按钮添加点击事件监听器
        this.redoBtn.addActionListener(e -> {
            if (controller != null) {
                controller.redoMove();
                if (gamePanel != null) {
                    gamePanel.requestFocusInWindow();
                }
            }
        });

        // 为返回主页面按钮添加点击事件监听器
        this.homeBtn.addActionListener(e -> {
            returnToHome();
            if (gamePanel != null) {
                gamePanel.requestFocusInWindow();
            }
        });
    }

    /**
     * 初始化游戏面板
     * @param mapModel 游戏地图模型
     */
    private void initializeGamePanel(MapModel mapModel) {
        // 创建游戏面板
        gamePanel = new GamePanel(mapModel);
        
        // 调整游戏面板位置
        int panelX = (this.getWidth() - gamePanel.getWidth()) / 2 - 60;
        int panelY = (this.getHeight() - gamePanel.getHeight()) / 2;
        gamePanel.setLocation(panelX, panelY);
        this.add(gamePanel);
        
        // 创建游戏控制器，关联面板和模型
        this.controller = new GameController(gamePanel, mapModel);
        controller.setParentFrame(this);
        controller.setVictoryView(victoryFrame);
        
        // 设置步数标签和最短步数标签
        gamePanel.setStepLabel(stepLabel);
        gamePanel.setMinStepsLabel(minStepsLabel);
        
        // 初始化游戏
        controller.initializeGame();
        
        // 更新按钮状态
        updateButtonsState();
        
        // 刷新界面
        this.revalidate();
        this.repaint();
        
        // 将焦点设置到游戏面板
        gamePanel.requestFocusInWindow();
    }

    /**
     * 加载新关卡
     * @param mapModel 游戏地图模型
     */
    public void loadLevel(MapModel mapModel) {
        // 如果已有游戏面板，先移除它
        if (gamePanel != null) {
            this.remove(gamePanel);
        }
        
        // 初始化新的游戏面板
        initializeGamePanel(mapModel);
    }

    /**
     * 获取游戏面板
     * @return 游戏面板
     */
    public GamePanel getGamePanel() {
        return gamePanel;
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
     * 返回关卡选择界面
     */
    public void returnToLevelSelect() {
        if (controller != null && controller.getLevelSelectFrame() != null) {
            // 隐藏游戏窗口
            this.setVisible(false);
            // 显示关卡选择界面
            controller.getLevelSelectFrame().showLevelSelect();
        } else {
            // 如果关卡选择界面引用缺失，显示错误消息
            JOptionPane.showMessageDialog(this,
                "Cannot return to level selection. Level selection reference is missing.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 设置关卡选择界面引用
     */
    public void setLevelSelectFrame(LevelSelectFrame levelSelectFrame) {
        if (controller != null) {
            controller.setLevelSelectFrame(levelSelectFrame);
        }
    }

    /**
     * 获取游戏控制器
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
     * 更新按钮状态
     * 根据用户会话状态启用或禁用保存按钮
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
        undoBtn.setEnabled(canUndo);
        redoBtn.setEnabled(canRedo);
    }
}
