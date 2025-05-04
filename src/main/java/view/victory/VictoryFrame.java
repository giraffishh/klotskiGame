package view.victory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.bson.Document;

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

    // 排行榜组件
    private JPanel leaderboardPanel;
    private final JTable leaderboardTable;
    private final DefaultTableModel tableModel;
    private JLabel leaderboardTitleLabel;
    private JLabel loadingLabel;

    // 撒花效果面板
    private final ConfettiPanel confettiPanel;

    // 存储返回主页的监听器，用于窗口关闭时调用
    private ActionListener homeListener;
    // 新增：存储关卡选择的监听器，用于窗口关闭时调用
    private ActionListener levelSelectListener;

    /**
     * 构造方法，初始化胜利界面的UI组件
     *
     * @param parent 父窗口引用
     */
    public VictoryFrame(JFrame parent) {
        super(parent, "Victory!", true); // 创建模态对话框

        // 设置对话框基本属性
        setSize(850, 650); // 增加尺寸以适应排行榜
        setLocationRelativeTo(parent); // 居中显示
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        // 添加窗口关闭监听器，使关闭窗口时执行与点击"Level Select"相同的操作
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 优先调用 levelSelectListener
                if (levelSelectListener != null) {
                    // 创建一个ActionEvent并传递给levelSelectListener
                    levelSelectListener.actionPerformed(
                            new java.awt.event.ActionEvent(levelSelectButton, // 使用 levelSelectButton 作为事件源
                                    java.awt.event.ActionEvent.ACTION_PERFORMED,
                                    "windowClosingToLevelSelect") // 修改 action command
                    );
                } else if (homeListener != null) { // 如果 levelSelectListener 未设置，则回退到 homeListener (理论上不应发生)
                    homeListener.actionPerformed(
                            new java.awt.event.ActionEvent(homeButton,
                                    java.awt.event.ActionEvent.ACTION_PERFORMED,
                                    "windowClosingToHomeFallback")
                    );
                }
            }
        });

        // 创建主面板，使用BoxLayout实现垂直布局
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        // 创建顶部胜利消息面板
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // 创建胜利消息标签
        messageLabel = new JLabel("Victory!", SwingConstants.CENTER);
        messageLabel.setFont(FontManager.getTitleFont(32));
        messageLabel.setForeground(FrameUtil.ACCENT_COLOR);
        messageLabel.setAlignmentX(CENTER_ALIGNMENT);
        headerPanel.add(messageLabel, BorderLayout.CENTER);

        // 添加副标题
        JLabel subtitleLabel = new JLabel("You completed the puzzle!", SwingConstants.CENTER);
        subtitleLabel.setFont(FontManager.getRegularFont(18));
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel);

        // 创建中央信息面板（步数和用时）
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 10));
        infoPanel.setOpaque(false);
        infoPanel.setMaximumSize(new Dimension(800, 100));

        // 创建用时标签
        timeLabel = new JLabel("Time: 00:00.00", SwingConstants.CENTER);
        timeLabel.setFont(FontManager.getTitleFont(22));
        timeLabel.setForeground(FrameUtil.PRIMARY_COLOR);
        infoPanel.add(timeLabel);

        // 创建步数标签
        stepsLabel = new JLabel("Steps: 0", SwingConstants.CENTER);
        stepsLabel.setFont(FontManager.getTitleFont(22));
        stepsLabel.setForeground(FrameUtil.PRIMARY_COLOR);
        infoPanel.add(stepsLabel);

        mainPanel.add(infoPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 创建排行榜面板
        leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new BoxLayout(leaderboardPanel, BoxLayout.Y_AXIS));
        leaderboardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        leaderboardPanel.setBackground(new Color(250, 250, 250));
        leaderboardPanel.setMaximumSize(new Dimension(800, 300));
        leaderboardPanel.setAlignmentX(CENTER_ALIGNMENT);

        // 排行榜标题
        leaderboardTitleLabel = new JLabel("Level Leaderboard", SwingConstants.CENTER);
        leaderboardTitleLabel.setFont(FontManager.getTitleFont(18));
        leaderboardTitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        leaderboardPanel.add(leaderboardTitleLabel);
        leaderboardPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 加载状态标签
        loadingLabel = new JLabel("Loading leaderboard data...", SwingConstants.CENTER);
        loadingLabel.setFont(FontManager.getRegularFont(14));
        loadingLabel.setAlignmentX(CENTER_ALIGNMENT);
        leaderboardPanel.add(loadingLabel);

        // 创建表格模型
        String[] columnNames = {"Rank", "Player", "Steps", "Time", "Date"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 使表格不可编辑
            }
        };

        // 创建表格并设置样式
        leaderboardTable = new JTable(tableModel);
        leaderboardTable.setFillsViewportHeight(true);
        leaderboardTable.setRowHeight(25);
        leaderboardTable.getTableHeader().setFont(FontManager.getRegularFont(14));
        leaderboardTable.setFont(FontManager.getRegularFont(14));

        // 设置表格列宽
        leaderboardTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Rank
        leaderboardTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Player
        leaderboardTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Steps
        leaderboardTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Time
        leaderboardTable.getColumnModel().getColumn(4).setPreferredWidth(120); // Date

        // 设置表格渲染器，使内容居中
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < leaderboardTable.getColumnCount(); i++) {
            leaderboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 创建滚动面板
        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        scrollPane.setPreferredSize(new Dimension(700, 200));
        scrollPane.setAlignmentX(CENTER_ALIGNMENT);

        // 默认隐藏表格，显示加载文本
        scrollPane.setVisible(false);

        leaderboardPanel.add(scrollPane);
        mainPanel.add(leaderboardPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 创建按钮
        homeButton = FrameUtil.createStyledButton("Back to Home", true);
        levelSelectButton = FrameUtil.createStyledButton("Level Select", true);
        restartButton = FrameUtil.createStyledButton("Play Again", true);
        nextLevelButton = FrameUtil.createStyledButton("Next Level", true);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setMaximumSize(new Dimension(800, 100));
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);

        // 将按钮添加到按钮面板
        buttonPanel.add(homeButton);
        buttonPanel.add(levelSelectButton);
        buttonPanel.add(restartButton);
        buttonPanel.add(nextLevelButton);

        // 将按钮面板添加到主面板
        mainPanel.add(buttonPanel);

        // 初始化撒花效果面板
        confettiPanel = new ConfettiPanel();
        JRootPane rootPane = getRootPane();
        rootPane.setGlassPane(confettiPanel);
        confettiPanel.setVisible(true);
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

        // 检查窗口是否可见，避免重复操作
        if (isVisible()) {
            setVisible(false);
            dispose(); // dispose() 会释放窗口资源
        }
    }

    @Override
    public void setHomeListener(ActionListener listener) {
        // 保存监听器引用，用于窗口关闭时调用
        this.homeListener = listener;
        homeButton.addActionListener(listener);
    }

    @Override
    public void setLevelSelectListener(ActionListener listener) {
        // 保存监听器引用，用于窗口关闭时调用
        this.levelSelectListener = listener;
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

    @Override
    public void updateLeaderboard(List<Document> leaderboardData, String currentUsername) {
        System.out.println("[VictoryFrame] updateLeaderboard called. 数据条数: " + (leaderboardData != null ? leaderboardData.size() : 0) + ", 当前用户: " + currentUsername);
        // 清空表格
        tableModel.setRowCount(0);

        // 获取滚动面板的引用
        JScrollPane scrollPane = null;
        if (leaderboardTable.getParent() instanceof javax.swing.JViewport
                && leaderboardTable.getParent().getParent() instanceof JScrollPane) {
            scrollPane = (JScrollPane) leaderboardTable.getParent().getParent();
        } else {
            System.err.println("[VictoryFrame] 无法找到排行榜表格的 JScrollPane！");
        }

        if (leaderboardData == null || leaderboardData.isEmpty()) {
            System.out.println("[VictoryFrame] 排行榜数据为空或null。");
            loadingLabel.setText("No leaderboard data available");
            loadingLabel.setVisible(true);
            if (scrollPane != null) {
                scrollPane.setVisible(false);
            }
            return;
        }

        // 确保有数据可以显示
        try {
            // 隐藏加载标签，显示表格
            loadingLabel.setVisible(false);
            if (scrollPane != null) {
                scrollPane.setVisible(true);
            }

            // 设置排行榜标题 - 从第一条记录获取levelIndex
            Document firstRecord = leaderboardData.get(0);
            // 添加健壮性检查，如果第一条记录没有levelIndex，尝试从其他记录获取或使用默认值
            int levelIndex = -1;
            if (firstRecord.containsKey("levelIndex")) {
                levelIndex = firstRecord.getInteger("levelIndex", -1);
            } else {
                System.err.println("[VictoryFrame] 警告：排行榜第一条记录缺少 levelIndex！");
                // 尝试从其他记录查找
                for (Document doc : leaderboardData) {
                    if (doc.containsKey("levelIndex")) {
                        levelIndex = doc.getInteger("levelIndex", -1);
                        if (levelIndex != -1) {
                            break;
                        }
                    }
                }
                if (levelIndex == -1) {
                    levelIndex = 0; // 最后回退到0

                }
            }

            System.out.println("[VictoryFrame] 设置排行榜标题为 Level " + (levelIndex + 1));
            leaderboardTitleLabel.setText("Level " + (levelIndex + 1) + " Leaderboard");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            // 判断当前用户是否在排行榜中
            boolean currentUserInLeaderboard = false;
            for (Document doc : leaderboardData) {
                String pName = doc.getString("playerName");
                if (pName != null && pName.equals(currentUsername)) {
                    currentUserInLeaderboard = true;
                    break;
                }
            }
            System.out.println("[VictoryFrame] 当前用户 '" + currentUsername + "' 是否在排行榜中: " + currentUserInLeaderboard);

            // 填充排行榜数据
            for (int i = 0; i < leaderboardData.size(); i++) {
                Document score = leaderboardData.get(i);
                String playerName = score.getString("playerName");
                // 安全获取 moves
                Integer movesObj = score.getInteger("moves");
                int moves = movesObj != null ? movesObj : 0;

                // 安全获取 timeInMillis
                Long timeObj = null;
                try {
                    Object timeVal = score.get("timeInMillis");
                    if (timeVal instanceof Long) {
                        timeObj = (Long) timeVal;
                    } else if (timeVal instanceof Integer) { // 兼容可能存为Integer的情况
                        timeObj = ((Integer) timeVal).longValue();
                    }
                } catch (Exception e) {
                    System.err.println("[VictoryFrame] 获取 timeInMillis 失败 for " + playerName + ": " + e.getMessage());
                }
                long timeInMillis = timeObj != null ? timeObj : 0L;

                // 安全获取 timestamp
                Date timestamp = null;
                try {
                    timestamp = score.getDate("timestamp");
                } catch (Exception e) {
                    System.err.println("[VictoryFrame] 获取 timestamp 失败 for " + playerName + ": " + e.getMessage());
                }

                // 格式化时间显示
                String timeFormatted = formatTimeForDisplay(timeInMillis);

                // 格式化日期
                String dateFormatted = timestamp != null ? dateFormat.format(timestamp) : "N/A";

                // 添加行数据
                Object[] rowData = {
                    i + 1,
                    playerName,
                    moves,
                    timeFormatted,
                    dateFormatted
                };
                tableModel.addRow(rowData);
            }

            // 应用渲染器设置
            setupTableRenderers(currentUsername, currentUserInLeaderboard);

        } catch (Exception e) {
            System.err.println("[VictoryFrame] 更新排行榜UI时出错: " + e.getMessage());
            e.printStackTrace(); // 打印详细错误
            loadingLabel.setText("Error displaying leaderboard");
            loadingLabel.setVisible(true);
            if (scrollPane != null) {
                scrollPane.setVisible(false);
            }
        }
    }

    /**
     * 设置表格渲染器，处理选择和高亮问题
     */
    private void setupTableRenderers(String currentUsername, boolean currentUserInLeaderboard) {
        // 1. 首先禁用表格的选择行为，防止点击时文本变成白色
        leaderboardTable.setRowSelectionAllowed(false);
        leaderboardTable.setCellSelectionEnabled(false);
        leaderboardTable.setFocusable(false);

        // 2. 创建自定义渲染器
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // 确保不使用系统默认的选择样式
                super.getTableCellRendererComponent(table, value, false, false, row, column);

                // 检查是否为当前用户所在行
                // 添加边界检查和null检查
                if (row >= 0 && row < table.getRowCount() && table.getValueAt(row, 1) != null) {
                    String rowPlayerName = table.getValueAt(row, 1).toString();
                    if (rowPlayerName.equals(currentUsername)) {
                        // 使用与Victory标题相似但饱和度更低的颜色高亮当前用户
                        // 创建半透明版本的强调色
                        Color baseColor = FrameUtil.ACCENT_COLOR;
                        // 创建更柔和的高亮色
                        Color highlightColor = new Color(
                                baseColor.getRed(),
                                baseColor.getGreen(),
                                baseColor.getBlue(),
                                200);  // 透明度值，0-255

                        setFont(getFont().deriveFont(Font.BOLD));
                        setBackground(highlightColor);
                        setForeground(Color.WHITE);
                    } else {
                        // 其他行使用默认样式
                        setFont(getFont().deriveFont(Font.PLAIN));
                        setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                    }
                } else {
                    // 如果行无效或玩家名为空，使用默认样式
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                }

                // 所有单元格文本居中
                setHorizontalAlignment(JLabel.CENTER);
                return this;
            }
        };

        // 3. 应用渲染器到所有列
        for (int col = 0; col < leaderboardTable.getColumnCount(); col++) {
            leaderboardTable.getColumnModel().getColumn(col).setCellRenderer(customRenderer);
        }

        // 4. 如果当前用户不在排行榜中且不是访客，可以添加提示信息
        if (!currentUserInLeaderboard && currentUsername != null && !currentUsername.equals("Guest")) {
            loadingLabel.setText("Your score has been submitted!"); // 简化提示信息
            loadingLabel.setVisible(true);
        } else if (currentUserInLeaderboard || (currentUsername != null && currentUsername.equals("Guest"))) {
            // 如果用户在榜上，或者用户是访客（访客成绩已临时加入），则隐藏加载/提示标签
            loadingLabel.setVisible(false);
        }
    }

    @Override
    public void setLeaderboardLoading(boolean isLoading) {
        System.out.println("[VictoryFrame] setLeaderboardLoading called with: " + isLoading);
        JScrollPane scrollPane = null;
        if (leaderboardTable.getParent() instanceof javax.swing.JViewport
                && leaderboardTable.getParent().getParent() instanceof JScrollPane) {
            scrollPane = (JScrollPane) leaderboardTable.getParent().getParent();
        }

        if (isLoading) {
            loadingLabel.setText("Loading leaderboard data...");
            loadingLabel.setVisible(true);
            if (scrollPane != null) {
                scrollPane.setVisible(false);
            }
        } else {
            // 只有在数据成功加载后才隐藏loading标签，updateLeaderboard会处理这个
            // 这里只处理加载失败或取消的情况（虽然目前没有取消逻辑）
            // loadingLabel.setVisible(false);
            // if (scrollPane != null) scrollPane.setVisible(true);
        }
    }

    /**
     * 格式化时间显示，格式为 mm:ss.xx（分:秒.厘秒）
     */
    private String formatTimeForDisplay(long timeInMillis) {
        int minutes = (int) (timeInMillis / 60000);
        int seconds = (int) ((timeInMillis % 60000) / 1000);
        int centiseconds = (int) ((timeInMillis % 1000) / 10);

        return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds);
    }
}
