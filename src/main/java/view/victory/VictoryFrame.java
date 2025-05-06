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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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

import controller.storage.rank.RankManager;
import view.util.FontManager;
import view.util.FrameUtil;

/**
 * 游戏胜利界面，显示胜利消息和提供不同的后续操作按钮
 */
public class VictoryFrame extends JFrame implements VictoryView {

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
    // 新增：存储当前游戏用时（毫秒），用于访客排名显示
    private long currentGameTimeMillis;

    /**
     * 构造方法，初始化胜利界面的UI组件
     *
     * @param parent 父窗口引用 (不再需要，JFrame不需要父窗口来显示在任务栏)
     */
    public VictoryFrame(JFrame parent) {
        // 修改 super 调用，使用 JFrame 的构造函数
        super("Victory!");

        // 设置窗口基本属性
        setSize(850, 650);
        // 使窗口在屏幕中央显示
        setLocationRelativeTo(null);
        // 设置关闭操作为销毁窗口
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        // JFrames are not modal by default, which is usually desired for taskbar presence.

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
                                    "windowClosingToLevelSelect")
                    );
                } else if (homeListener != null) { // 如果 levelSelectListener 未设置，则回退到 homeListener (理论上不应发生)
                    homeListener.actionPerformed(
                            new java.awt.event.ActionEvent(homeButton,
                                    java.awt.event.ActionEvent.ACTION_PERFORMED,
                                    "windowClosingToHomeFallback")
                    );
                }
                // Note: DefaultCloseOperation is DISPOSE_ON_CLOSE, so the window will be disposed after listeners run.
            }
        });

        // 创建主面板，使用BoxLayout实现垂直布局
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        // 减少顶部边距从 20 到 10
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        setContentPane(mainPanel);

        // 创建顶部胜利消息面板
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // 创建胜利消息标签
        messageLabel = new JLabel("Victory!", SwingConstants.CENTER);
        messageLabel.setFont(FontManager.getTitleFont(32));
        messageLabel.setForeground(FrameUtil.ACCENT_COLOR);
        messageLabel.setAlignmentX(CENTER_ALIGNMENT);
        headerPanel.add(messageLabel, BorderLayout.CENTER);

        mainPanel.add(headerPanel);

        // 创建中央信息面板（步数和用时）
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 5));
        infoPanel.setOpaque(false);
        infoPanel.setMaximumSize(new Dimension(800, 50));

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
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // 创建排行榜面板
        leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new BoxLayout(leaderboardPanel, BoxLayout.Y_AXIS));
        leaderboardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        leaderboardPanel.setBackground(new Color(250, 250, 250));
        leaderboardPanel.setMaximumSize(new Dimension(800, 400)); // 进一步增加最大高度从 350 到 400
        leaderboardPanel.setAlignmentX(CENTER_ALIGNMENT);

        // 排行榜标题
        leaderboardTitleLabel = new JLabel("Leaderboard", SwingConstants.CENTER);
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
        scrollPane.setPreferredSize(new Dimension(700, 300)); // 增加滚动面板的首选高度从 250 到 300
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
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10)); // 减少顶部边距
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
    public void showVictory(String victoryMessage, int steps, String timeElapsed, long gameTimeInMillis) {
        this.currentGameTimeMillis = gameTimeInMillis; // 存储时间
        messageLabel.setText(victoryMessage);
        stepsLabel.setText("Steps: " + steps);
        timeLabel.setText(timeElapsed); // timeElapsed 是格式化后的字符串
        confettiPanel.startAnimation();
        setVisible(true); // Make the JFrame visible
        toFront(); // Bring the frame to the front
        requestFocus(); // Request focus for the frame
    }

    @Override
    public void hideVictory() {
        confettiPanel.stopAnimation();

        // 检查窗口是否可见，避免重复操作
        if (isVisible()) {
            setVisible(false);
            dispose(); // dispose() 会释放窗口资源 (Correct for JFrame with DISPOSE_ON_CLOSE)
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
    public void updateLeaderboard(List<Document> processedLeaderboardData, String currentUsername) {
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

        // 从 RankManager 获取完整的排行榜数据
        List<Document> fullLeaderboardData = RankManager.getInstance().getLastLoadedData();
        if (fullLeaderboardData == null) {
            fullLeaderboardData = new ArrayList<>();
        }

        // 处理传入的 processedLeaderboardData 为空的情况
        if (processedLeaderboardData == null || processedLeaderboardData.isEmpty()) {
            loadingLabel.setText("No leaderboard data available");
            loadingLabel.setVisible(true);
            if (scrollPane != null) {
                scrollPane.setVisible(false);
            }
            setupTableRenderers(currentUsername, !fullLeaderboardData.isEmpty());
            return;
        }

        try {
            if (scrollPane != null) {
                scrollPane.setVisible(true);
            }

            leaderboardTitleLabel.setText("Level Leaderboard");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            Document currentUserScoreDoc = null;
            int currentUserRank = -1;
            boolean isGuest = "Guest".equals(currentUsername);

            if (currentUsername != null) {
                if (isGuest) {
                    int currentMoves = 0;
                    try {
                        currentMoves = Integer.parseInt(stepsLabel.getText().replace("Steps: ", ""));
                        for (Document doc : processedLeaderboardData) {
                            if ("Guest".equals(doc.getString("playerName"))) {
                                currentUserScoreDoc = doc;
                                break;
                            }
                        }
                        if (currentUserScoreDoc == null) {
                            currentUserScoreDoc = new Document()
                                    .append("playerName", "Guest")
                                    .append("moves", currentMoves)
                                    .append("timeInMillis", this.currentGameTimeMillis) // 使用存储的实际时间
                                    .append("timestamp", new Date());
                            currentUserRank = 999;
                        } else {
                            for (int i = 0; i < processedLeaderboardData.size(); i++) {
                                if ("Guest".equals(processedLeaderboardData.get(i).getString("playerName"))) {
                                    currentUserRank = i;
                                    break;
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing steps label for guest score.");
                    }
                } else {
                    for (int i = 0; i < fullLeaderboardData.size(); i++) {
                        Document doc = fullLeaderboardData.get(i);
                        String pName = doc.getString("playerName");
                        if (currentUsername.equals(pName)) {
                            currentUserScoreDoc = doc;
                            currentUserRank = i;
                            break;
                        }
                    }
                }
            }
            boolean currentUserExists = (currentUserScoreDoc != null);

            boolean userDisplayedInTable = false;
            for (int i = 0; i < processedLeaderboardData.size(); i++) {
                Document score = processedLeaderboardData.get(i);
                addRowToTable(score, i + 1, dateFormat);
                if (currentUsername != null && currentUsername.equals(score.getString("playerName"))) {
                    userDisplayedInTable = true;
                }
            }

            if (currentUserExists && !userDisplayedInTable && currentUserScoreDoc != null) {
                int rankToDisplay = (currentUserRank == 999) ? -1 : currentUserRank + 1;
                addRowToTable(currentUserScoreDoc, rankToDisplay, dateFormat);
            }

            setupTableRenderers(currentUsername, false);

        } catch (Exception e) {
            System.err.println("[VictoryFrame] 更新排行榜UI时出错: " + e.getMessage());
            e.printStackTrace();
            loadingLabel.setText("Error displaying leaderboard");
            loadingLabel.setVisible(true);
            if (scrollPane != null) {
                scrollPane.setVisible(false);
            }
        }
    }

    private void addRowToTable(Document score, int rank, SimpleDateFormat dateFormat) {
        String playerName = score.getString("playerName");
        Integer movesObj = score.getInteger("moves");
        int moves = movesObj != null ? movesObj : 0;

        Long timeObj = null;
        try {
            Object timeVal = score.get("timeInMillis");
            if (timeVal instanceof Long) {
                timeObj = (Long) timeVal;
            } else if (timeVal instanceof Integer) {
                timeObj = ((Integer) timeVal).longValue();
            }
        } catch (Exception e) {
            System.err.println("[VictoryFrame] 获取 timeInMillis 失败 for " + playerName + ": " + e.getMessage());
        }
        long timeInMillis = timeObj != null ? timeObj : 0L;

        Date timestamp = null;
        try {
            timestamp = score.getDate("timestamp");
        } catch (Exception e) {
            System.err.println("[VictoryFrame] 获取 timestamp 失败 for " + playerName + ": " + e.getMessage());
        }

        String timeFormatted = formatTimeForDisplay(timeInMillis);
        String dateFormatted = timestamp != null ? dateFormat.format(timestamp) : "N/A";

        Object[] rowData = {
            (rank == -1) ? "..." : rank,
            playerName,
            moves,
            timeFormatted,
            dateFormatted
        };
        tableModel.addRow(rowData);
    }

    private void setupTableRenderers(String currentUsername, boolean currentUserInLeaderboardData_IGNORED) {
        leaderboardTable.setRowSelectionAllowed(false);
        leaderboardTable.setCellSelectionEnabled(false);
        leaderboardTable.setFocusable(false);

        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, false, false, row, column);

                if (row >= 0 && row < table.getRowCount() && table.getValueAt(row, 1) != null) {
                    String rowPlayerName = table.getValueAt(row, 1).toString();
                    if (rowPlayerName.equals(currentUsername)) {
                        Color baseColor = FrameUtil.ACCENT_COLOR;
                        Color highlightColor = new Color(
                                baseColor.getRed(),
                                baseColor.getGreen(),
                                baseColor.getBlue(),
                                200);
                        setFont(getFont().deriveFont(Font.BOLD));
                        setBackground(highlightColor);
                        setForeground(Color.WHITE);
                    } else {
                        setFont(getFont().deriveFont(Font.PLAIN));
                        setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                        setForeground(Color.BLACK);
                    }
                } else {
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                }

                setHorizontalAlignment(JLabel.CENTER);
                return this;
            }
        };

        for (int col = 0; col < leaderboardTable.getColumnCount(); col++) {
            leaderboardTable.getColumnModel().getColumn(col).setCellRenderer(customRenderer);
        }

        boolean isGuest = "Guest".equals(currentUsername);
        if (isGuest) {
            loadingLabel.setVisible(false);
        } else if (currentUsername != null) {
            int userRank = findUserRank(currentUsername);

            if (userRank != -1) {
                if (userRank >= 10) {
                    loadingLabel.setText("Your score is submitted! Rank: " + (userRank + 1));
                    loadingLabel.setVisible(true);
                } else {
                    loadingLabel.setVisible(false);
                }
            } else {
                loadingLabel.setText("Your score has been submitted!");
                loadingLabel.setVisible(true);
            }
        } else {
            loadingLabel.setVisible(false);
        }
    }

    private int findUserRank(String username) {
        List<Document> originalData = RankManager.getInstance().getLastLoadedData();
        if (originalData != null && username != null) {
            for (int i = 0; i < originalData.size(); i++) {
                Document doc = originalData.get(i);
                String playerName = doc.getString("playerName");
                if (username.equals(playerName)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void setLeaderboardLoading(boolean isLoading) {
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
        }
    }

    private String formatTimeForDisplay(long timeInMillis) {
        int minutes = (int) (timeInMillis / 60000);
        int seconds = (int) ((timeInMillis % 60000) / 1000);
        int centiseconds = (int) ((timeInMillis % 1000) / 10);

        return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds);
    }
}
