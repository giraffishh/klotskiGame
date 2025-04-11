package view.game;

import controller.GameController;
import model.MapModel;
import view.util.FrameUtil;
import view.util.FontManager;

import javax.swing.*;
import java.awt.*;

/**
 * 游戏主窗口类
 * 包含游戏面板和控制按钮等组件
 * 负责显示游戏界面和处理用户交互
 */
public class GameFrame extends JFrame {

    // 游戏控制器，处理游戏逻辑
    private GameController controller;
    // 重新开始按钮
    private JButton restartBtn;
    // 加载游戏按钮
    private JButton loadBtn;
    // 步数显示标签
    private JLabel stepLabel;
    // 游戏主面板，显示游戏地图
    private GamePanel gamePanel;

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
        // 设置窗口大小
        this.setSize(width, height);
        
        // 创建游戏面板并居中显示
        gamePanel = new GamePanel(mapModel);
        gamePanel.setLocation(30, height / 2 - gamePanel.getHeight() / 2);
        this.add(gamePanel);
        
        // 创建游戏控制器，关联面板和模型
        this.controller = new GameController(gamePanel, mapModel);

        // 创建重新开始按钮
        this.restartBtn = FrameUtil.createButton(this, "Restart", new Point(gamePanel.getWidth() + 80, 120), 80, 50);
        restartBtn.setFont(FontManager.getButtonFont());

        // 创建加载游戏按钮
        this.loadBtn = FrameUtil.createButton(this, "Load", new Point(gamePanel.getWidth() + 80, 210), 80, 50);
        loadBtn.setFont(FontManager.getButtonFont());

        // 创建步数显示标签
        this.stepLabel = FrameUtil.createJLabel(this, "Start", FontManager.getTitleFont(20),
                new Point(gamePanel.getWidth() + 80, 70), 180, 50);

        // 将步数标签设置到游戏面板中
        gamePanel.setStepLabel(stepLabel);

        // 为重新开始按钮添加点击事件监听器
        this.restartBtn.addActionListener(e -> {
            // 重新开始游戏
            controller.restartGame();
            // 将焦点设置回游戏面板以便接收键盘事件
            gamePanel.requestFocusInWindow();
        });
        
        // 为加载游戏按钮添加点击事件监听器
        this.loadBtn.addActionListener(e -> {
            // 弹出输入对话框获取游戏存档路径，使用输入字体
            JTextField inputField = new JTextField();
            inputField.setFont(FontManager.getInputFont());

            String string = JOptionPane.showInputDialog(this, "Input path:");
            System.out.println(string);
            // 将焦点设置回游戏面板以便接收键盘事件
            gamePanel.requestFocusInWindow();
        });
        
        //todo: add other button here
        
        // 窗口居中显示
        this.setLocationRelativeTo(null);
        // 设置关闭窗口时退出程序
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
