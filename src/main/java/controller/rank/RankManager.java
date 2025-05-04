package controller.rank;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.bson.Document;

import service.RankingDatabase;
import service.UserSession;
import view.victory.VictoryView;

/**
 * 排行榜管理器类，负责排行榜数据的加载、上传和管理
 */
public class RankManager {

    private static RankManager instance;

    /**
     * 获取RankManager单例
     */
    public static synchronized RankManager getInstance() {
        if (instance == null) {
            instance = new RankManager();
        }
        return instance;
    }

    private RankManager() {
        // 私有构造函数，防止外部实例化
    }

    /**
     * 加载排行榜数据并更新到视图组件
     *
     * @param victoryView 胜利视图组件
     * @param levelIndex 要加载的关卡索引
     * @param isGuest 是否是访客模式
     * @param username 当前用户名
     * @param moves 当前步数（访客模式下用）
     * @param timeInMillis 当前用时（访客模式下用）
     */
    public void loadLeaderboardData(final VictoryView victoryView, final int levelIndex,
            final boolean isGuest, final String username, final int moves, final long timeInMillis) {

        // 记录加载的关卡索引，用于调试
        System.out.println("[RankManager] 请求加载关卡 " + (levelIndex + 1) + " 的排行榜数据");

        // 设置加载状态
        if (victoryView != null) {
            victoryView.setLeaderboardLoading(true);
        }

        // 使用SwingWorker在后台线程加载排行榜数据
        new SwingWorker<List<Document>, Void>() {
            @Override
            protected List<Document> doInBackground() {
                // 在后台线程开始时记录levelIndex
                System.out.println("[RankManager Worker BG] 开始获取 levelIndex: " + levelIndex + " 的排行榜");
                try {
                    RankingDatabase rankingDb = RankingDatabase.getInstance();
                    if (!rankingDb.isConnected()) {
                        System.err.println("[RankManager Worker BG] 数据库未连接，无法加载 levelIndex: " + levelIndex);
                        return new ArrayList<>(); // 返回空列表
                    }

                    // 获取前10名排行榜数据
                    List<Document> leaderboardData = rankingDb.getLeaderboard(levelIndex, 10);
                    System.out.println("[RankManager Worker BG] 从数据库获取到 " + (leaderboardData != null ? leaderboardData.size() : 0) + " 条 levelIndex: " + levelIndex + " 的记录");

                    // 如果是访客模式，将当前成绩添加到临时列表中以便排序和显示
                    if (isGuest) {
                        List<Document> combined = new ArrayList<>(leaderboardData);
                        // 创建访客成绩文档
                        Document guestScore = new Document()
                                .append("playerName", "Guest")
                                .append("levelIndex", levelIndex) // 确保使用正确的levelIndex
                                .append("moves", moves)
                                .append("timeInMillis", timeInMillis)
                                .append("timestamp", new Date());
                        combined.add(guestScore);

                        // 对列表重新排序（按照时间和步数）
                        combined.sort((doc1, doc2) -> {
                            Long time1 = doc1.getLong("timeInMillis");
                            Long time2 = doc2.getLong("timeInMillis");
                            if (time1 == null) {
                                time1 = Long.MAX_VALUE; // 处理可能的null值

                            }
                            if (time2 == null) {
                                time2 = Long.MAX_VALUE;
                            }

                            int timeCompare = Long.compare(time1, time2);
                            if (timeCompare != 0) {
                                return timeCompare;
                            }

                            // 时间相同时，比较步数
                            Integer moves1 = doc1.getInteger("moves", Integer.MAX_VALUE);
                            Integer moves2 = doc2.getInteger("moves", Integer.MAX_VALUE);
                            return Integer.compare(moves1, moves2);
                        });

                        // 只保留前10名
                        if (combined.size() > 10) {
                            combined = combined.subList(0, 10);
                        }
                        System.out.println("[RankManager Worker BG] 访客模式，合并并排序后 levelIndex: " + levelIndex + " 的记录数: " + combined.size());
                        return combined;
                    }

                    return leaderboardData;
                } catch (Exception e) {
                    System.err.println("[RankManager Worker BG] 获取 levelIndex: " + levelIndex + " 排行榜数据时发生异常: " + e.getMessage());
                    e.printStackTrace(); // 打印堆栈跟踪以获取详细信息
                    return new ArrayList<>(); // 出错时返回空列表
                }
            }

            @Override
            protected void done() {
                try {
                    final List<Document> leaderboardData = get();
                    // 在EDT线程准备更新UI前记录levelIndex
                    System.out.println("[RankManager Worker Done] 准备更新UI，levelIndex: " + levelIndex + "，数据条数: " + (leaderboardData != null ? leaderboardData.size() : 0));

                    // 确保在EDT中更新UI，避免线程问题
                    SwingUtilities.invokeLater(() -> {
                        if (victoryView != null) {
                            // 在实际更新UI前再次确认levelIndex
                            System.out.println("[RankManager EDT Update] 调用 updateLeaderboard，levelIndex: " + levelIndex);

                            // 确保数据中的levelIndex正确（作为后备检查）
                            if (leaderboardData != null && !leaderboardData.isEmpty()) {
                                for (Document doc : leaderboardData) {
                                    if (!doc.containsKey("levelIndex") || doc.getInteger("levelIndex", -1) != levelIndex) {
                                        System.err.println("[RankManager EDT Update] 警告：排行榜数据中发现不匹配的levelIndex！强制设置为 " + levelIndex);
                                        doc.put("levelIndex", levelIndex); // 强制修正
                                    }
                                }
                            }

                            victoryView.updateLeaderboard(leaderboardData, username);
                            victoryView.setLeaderboardLoading(false);
                        } else {
                            System.err.println("[RankManager Worker Done] victoryView 为 null，无法更新UI，levelIndex: " + levelIndex);
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[RankManager Worker Done] 处理 levelIndex: " + levelIndex + " 排行榜数据失败: " + e.getMessage());
                    e.printStackTrace(); // 打印堆栈跟踪
                    SwingUtilities.invokeLater(() -> {
                        if (victoryView != null) {
                            victoryView.setLeaderboardLoading(false); // 确保加载状态被重置
                        }
                    });
                }
            }
        }.execute();
    }

    /**
     * 将当前游戏成绩上传到排行榜数据库
     *
     * @param levelIndex 关卡索引
     * @param playerName 玩家名称
     * @param moves 完成步数
     * @param timeInMillis 完成时间（毫秒）
     */
    public void uploadScore(final int levelIndex, final String playerName,
            final int moves, final long timeInMillis) {

        // 再次检查是否为访客
        if (UserSession.getInstance().isGuest()) {
            System.out.println("[RankManager] 访客模式无法上传分数 (levelIndex: " + levelIndex + ")");
            return;
        }

        RankingDatabase rankingDb = RankingDatabase.getInstance();
        // 检查数据库是否连接成功
        if (!rankingDb.isConnected()) {
            System.out.println("[RankManager] 排行榜数据库未连接，跳过分数上传 (levelIndex: " + levelIndex + ")");
            return;
        }

        // 检查获取到的用户名是否有效
        if (playerName == null || playerName.trim().isEmpty()) {
            System.err.println("[RankManager] 无法上传分数：玩家名称无效 (levelIndex: " + levelIndex + ")");
            return; // 修正：之前是抛出异常，改为打印错误并返回
        }

        System.out.println("[RankManager] 准备上传分数: Player=" + playerName
                + ", LevelIndex=" + levelIndex
                + ", Moves=" + moves
                + ", Time=" + timeInMillis);

        // 在后台线程上传分数，避免阻塞UI线程
        new Thread(() -> {
            try {
                System.out.println("[RankManager Upload Thread] 开始上传分数 for levelIndex: " + levelIndex);
                rankingDb.uploadScore(playerName, levelIndex, moves, timeInMillis);
                System.out.println("[RankManager Upload Thread] 分数上传完成 for levelIndex: " + levelIndex);
            } catch (Exception e) {
                // 记录上传失败，但通常不打断用户流程
                System.err.println("[RankManager Upload Thread] 后台上传分数时发生错误 (levelIndex: " + levelIndex + "): " + e.getMessage());
                e.printStackTrace(); // 打印堆栈跟踪
            }
        }).start();
    }
}
