package view.victory;

import view.util.FontManager;
import view.util.FrameUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 游戏胜利界面，显示胜利消息和提供不同的后续操作按钮
 */
public class VictoryFrame extends JDialog implements VictoryView {

    private final JLabel messageLabel;
    private final JLabel stepsLabel;
    private final JButton homeButton;
    private final JButton levelSelectButton;
    private final JButton restartButton;
    private final JButton nextLevelButton;

    /**
     * 构造方法，初始化胜利界面的UI组件
     * @param parent 父窗口引用
     */
    public VictoryFrame(JFrame parent) {
        super(parent, "Victory!", true); // 创建模态对话框

        // 设置对话框基本属性
        setSize(450, 350);
        setLocationRelativeTo(parent); // 居中显示
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

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

        // 创建中央步数显示面板
        JPanel stepsPanel = FrameUtil.createPaddedPanel(new BorderLayout(), 10, 0, 20, 0);
        stepsPanel.setOpaque(true);

        // 创建步数标签
        stepsLabel = FrameUtil.createTitleLabel("Steps: 0", JLabel.CENTER);
        stepsLabel.setFont(FontManager.getTitleFont(22));
        stepsLabel.setForeground(FrameUtil.PRIMARY_COLOR);
        stepsPanel.add(stepsLabel, BorderLayout.CENTER);

        mainPanel.add(stepsPanel, BorderLayout.CENTER);

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
    }

    @Override
    public void showVictory(String victoryMessage) {
        messageLabel.setText(victoryMessage);
        setVisible(true);
    }

    @Override
    public void showVictory(String victoryMessage, int steps) {
        messageLabel.setText(victoryMessage);
        stepsLabel.setText("Steps: " + steps);
        setVisible(true);
    }

    @Override
    public void hideVictory() {
        setVisible(false);
        dispose();
    }

    @Override
    public void setHomeListener(ActionListener listener) {
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
}
