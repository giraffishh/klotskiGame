package view.victory;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import view.util.FontManager;
import view.util.FrameUtil;

/**
 * 游戏胜利界面，显示胜利消息和提供不同的后续操作按钮
 */
public class VictoryFrame extends JDialog implements VictoryView {

    private final JLabel messageLabel;
    private final JLabel stepsLabel;
    private final JLabel timeLabel;
    private final JButton homeButton;
    private final JButton levelSelectButton;
    private final JButton restartButton;
    private final JButton nextLevelButton;

    // 撒花效果面板
    private ConfettiPanel confettiPanel;

    // 存储返回主页的监听器，用于窗口关闭时调用
    private ActionListener homeListener;

    /**
     * 构造方法，初始化胜利界面的UI组件
     *
     * @param parent 父窗口引用
     */
    public VictoryFrame(JFrame parent) {
        super(parent, "Victory!", true); // 创建模态对话框

        // 设置对话框基本属性
        setSize(450, 380); // 增加高度以适应时间标签
        setLocationRelativeTo(parent); // 居中显示
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        // 添加窗口关闭监听器，使关闭窗口时执行与点击"Back to Home"相同的操作
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (homeListener != null) {
                    // 创建一个ActionEvent并传递给homeListener
                    homeListener.actionPerformed(
                            new java.awt.event.ActionEvent(homeButton,
                                    java.awt.event.ActionEvent.ACTION_PERFORMED,
                                    "windowClosing")
                    );
                }
            }
        });

        // 创建主面板
        JPanel mainPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 20, 20, 20, 20);
        setContentPane(mainPanel);

        // 创建顶部胜利消息面板
        JPanel headerPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 0, 0, 15, 0);
        headerPanel.setOpaque(true);

        // 创建胜利消息标签
        messageLabel = FrameUtil.createTitleLabel("Victory!", JLabel.CENTER);
        messageLabel.setFont(FontManager.getTitleFont(32));
        messageLabel.setForeground(FrameUtil.ACCENT_COLOR);
        headerPanel.add(messageLabel, BorderLayout.CENTER);

        // 添加副标题
        JLabel subtitleLabel = FrameUtil.createTitleLabel("You completed the puzzle!", JLabel.CENTER);
        subtitleLabel.setFont(FontManager.getRegularFont(18));
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 创建中央信息面板（步数和用时）
        JPanel infoPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 10, 0, 10, 0);
        infoPanel.setOpaque(true);

        // 子面板用于步数和时间
        JPanel statsPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        statsPanel.setOpaque(false);

        // 创建用时标签
        timeLabel = FrameUtil.createTitleLabel("Time: 00:00.00", JLabel.CENTER);
        timeLabel.setFont(FontManager.getTitleFont(22));
        timeLabel.setForeground(FrameUtil.PRIMARY_COLOR);
        statsPanel.add(timeLabel);

        // 创建步数标签
        stepsLabel = FrameUtil.createTitleLabel("Steps: 0", JLabel.CENTER);
        stepsLabel.setFont(FontManager.getTitleFont(22));
        stepsLabel.setForeground(FrameUtil.PRIMARY_COLOR);
        statsPanel.add(stepsLabel);

        infoPanel.add(statsPanel, BorderLayout.CENTER);
        mainPanel.add(infoPanel, BorderLayout.CENTER);

        // 创建按钮
        homeButton = FrameUtil.createStyledButton("Back to Home", true);
        levelSelectButton = FrameUtil.createStyledButton("Level Select", true);
        restartButton = FrameUtil.createStyledButton("Play Again", true);
        nextLevelButton = FrameUtil.createStyledButton("Next Level", true);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        FrameUtil.setPadding(buttonPanel, 0, 10, 10, 10);
        buttonPanel.setOpaque(true);

        // 将按钮添加到按钮面板
        buttonPanel.add(homeButton);
        buttonPanel.add(levelSelectButton);
        buttonPanel.add(restartButton);
        buttonPanel.add(nextLevelButton);

        // 将按钮面板添加到主面板
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 初始化撒花效果面板
        confettiPanel = new ConfettiPanel();
        JRootPane rootPane = getRootPane();
        rootPane.setGlassPane(confettiPanel);
        confettiPanel.setVisible(true);
    }

    @Override
    public void showVictory(String victoryMessage) {
        messageLabel.setText(victoryMessage);
        stepsLabel.setText("Steps: 0");  // 重置步数显示
        timeLabel.setText("Time: 00:00.00");  // 重置时间显示
        confettiPanel.startAnimation();
        setVisible(true);
    }

    @Override
    public void showVictory(String victoryMessage, int steps) {
        messageLabel.setText(victoryMessage);
        stepsLabel.setText("Steps: " + steps);
        timeLabel.setText("Time: 00:00.00");  // 使用默认时间
        confettiPanel.startAnimation();
        setVisible(true);
    }

    @Override
    public void showVictory(String victoryMessage, int steps, String timeElapsed) {
        messageLabel.setText(victoryMessage);
        stepsLabel.setText("Steps: " + steps);
        timeLabel.setText(timeElapsed);
        confettiPanel.startAnimation();
        setVisible(true);
    }

    @Override
    public void hideVictory() {
        confettiPanel.stopAnimation();
        setVisible(false);
        dispose();
    }

    @Override
    public void setHomeListener(ActionListener listener) {
        // 保存监听器引用，用于窗口关闭时调用
        this.homeListener = listener;
        homeButton.addActionListener(listener);
    }

    @Override
    public void setLevelSelectListener(ActionListener listener) {
        levelSelectButton.addActionListener(listener);
    }

    @Override
    public void setRestartListener(ActionListener listener) {
        restartButton.addActionListener(listener);
    }

    @Override
    public void setNextLevelListener(ActionListener listener) {
        nextLevelButton.addActionListener(listener);
    }

    @Override
    public void setNextLevelButtonEnabled(boolean enabled) {
        nextLevelButton.setEnabled(enabled);
    }

    @Override
    public void setVictoryMessage(String message) {
        messageLabel.setText(message);
    }
}
