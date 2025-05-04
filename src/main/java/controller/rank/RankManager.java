package controller.rank;

// --- Necessary MongoDB Imports ---
// Ensure your project includes the MongoDB Java Driver dependency.
// For Maven, add to pom.xml:
// <dependency>
//     <groupId>org.mongodb</groupId>
//     <artifactId>mongodb-driver-sync</artifactId>
//     <version>LATEST_VERSION</version> <!-- Replace LATEST_VERSION with the desired version, e.g., 4.11.1 -->
// </dependency>
import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
// --- End MongoDB Imports ---

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import service.RankingDatabase;
import service.UserSession;
import view.victory.VictoryView;

/**
 * 排行榜管理器类，负责排行榜数据的加载、上传和管理
 */
public class RankManager {

    private static RankManager instance;
    private volatile SwingWorker<List<Document>, Void> currentWorker = null;

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
     * 取消当前正在进行的排行榜加载操作
     */
    public void cancelLoad() {
        SwingWorker<List<Document>, Void> worker = currentWorker;
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
    }

    /**
     * 加载排行榜数据并更新到视图组件。 如果不是访客模式，此方法现在也会先尝试上传/更新分数，然后再加载排行榜。
     *
     * @param victoryView 胜利视图组件
     * @param levelIndex 要加载的关卡索引
     * @param isGuest 是否是访客模式
     * @param username 当前用户名
     * @param moves 当前步数
     * @param timeInMillis 当前用时
     */
    public void loadLeaderboardData(final VictoryView victoryView, final int levelIndex,
            final boolean isGuest, final String username, final int moves, final long timeInMillis) {

        cancelLoad();

        if (victoryView != null) {
            victoryView.setLeaderboardLoading(true);
        } else {
            System.err.println("[RankManager] VictoryView is null when requesting leaderboard load for levelIndex: " + levelIndex);
            return;
        }

        final SwingWorker<List<Document>, Void> worker = new SwingWorker<List<Document>, Void>() {
            @Override
            protected List<Document> doInBackground() throws InterruptedException {
                try {
                    if (!isGuest) {
                        RankingDatabase rankingDb = RankingDatabase.getInstance();
                        if (rankingDb.isConnected()) {
                            if (username != null && !username.trim().isEmpty() && !username.equals("ErrorUser")) {
                                System.out.println("[RankManager Worker BG] 开始上传/更新分数 for levelIndex: " + levelIndex + ", Player: " + username);
                                try {
                                    Bson filter = Filters.and(
                                            Filters.eq("playerName", username),
                                            Filters.eq("levelIndex", levelIndex)
                                    );
                                    Bson update = Updates.combine(
                                            Updates.set("moves", moves),
                                            Updates.set("timeInMillis", timeInMillis),
                                            Updates.set("timestamp", new Date())
                                    );
                                    UpdateOptions options = new UpdateOptions().upsert(true);
                                    UpdateResult result = rankingDb.getScoresCollection().updateOne(filter, update, options);

                                    if (result.getUpsertedId() != null) {
                                        System.out.println("[RankManager Worker BG] 新分数记录插入成功。");
                                    } else if (result.getModifiedCount() > 0) {
                                        System.out.println("[RankManager Worker BG] 分数记录更新成功。");
                                    } else {
                                        System.out.println("[RankManager Worker BG] 分数记录已存在且无需更新。");
                                    }
                                } catch (MongoException e) {
                                    System.err.println("[RankManager Worker BG] 上传或更新分数时发生错误: " + e.getMessage());
                                }
                            } else {
                                System.err.println("[RankManager Worker BG] 无法上传分数：玩家名称无效 (" + username + ")");
                            }
                        } else {
                            System.err.println("[RankManager Worker BG] 无法上传分数：数据库未连接。");
                        }
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Leaderboard load interrupted before DB query for levelIndex: " + levelIndex);
                    }

                    RankingDatabase rankingDb = RankingDatabase.getInstance();
                    if (!rankingDb.isConnected()) {
                        System.err.println("[RankManager Worker BG] 数据库未连接，无法加载 levelIndex: " + levelIndex);
                        return new ArrayList<>();
                    }

                    List<Document> leaderboardData = rankingDb.getLeaderboard(levelIndex, 10);

                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Leaderboard load interrupted after DB query for levelIndex: " + levelIndex);
                    }

                    if (isGuest) {
                        List<Document> combined = new ArrayList<>(leaderboardData);
                        Document guestScore = new Document()
                                .append("playerName", "Guest")
                                .append("levelIndex", levelIndex)
                                .append("moves", moves)
                                .append("timeInMillis", timeInMillis)
                                .append("timestamp", new Date());
                        combined.add(guestScore);

                        combined.sort((doc1, doc2) -> {
                            Long time1 = doc1.getLong("timeInMillis");
                            Long time2 = doc2.getLong("timeInMillis");
                            if (time1 == null) {
                                time1 = Long.MAX_VALUE;
                            }
                            if (time2 == null) {
                                time2 = Long.MAX_VALUE;
                            }

                            int timeCompare = Long.compare(time1, time2);
                            if (timeCompare != 0) {
                                return timeCompare;
                            }

                            Integer moves1 = doc1.getInteger("moves", Integer.MAX_VALUE);
                            Integer moves2 = doc2.getInteger("moves", Integer.MAX_VALUE);
                            return Integer.compare(moves1, moves2);
                        });

                        if (combined.size() > 10) {
                            combined = combined.subList(0, 10);
                        }
                        return combined;
                    }

                    return leaderboardData;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                } catch (Exception e) {
                    System.err.println("[RankManager Worker BG] 获取 levelIndex: " + levelIndex + " 排行榜数据时发生异常: " + e.getMessage());
                    e.printStackTrace();
                    return new ArrayList<>();
                }
            }

            @Override
            protected void done() {
                boolean stillCurrent = (currentWorker == this);
                if (stillCurrent) {
                    currentWorker = null;
                } else {
                    return;
                }

                if (isCancelled()) {
                    SwingUtilities.invokeLater(() -> {
                        if (victoryView != null) {
                            victoryView.setLeaderboardLoading(false);
                        }
                    });
                    return;
                }

                List<Document> leaderboardData = null;
                try {
                    leaderboardData = get();

                    if (leaderboardData == null) {
                        SwingUtilities.invokeLater(() -> {
                            if (victoryView != null) {
                                victoryView.setLeaderboardLoading(false);
                            }
                        });
                        return;
                    }

                    if (victoryView == null) {
                        System.err.println("[RankManager Worker Done] VictoryView became null before UI update for levelIndex: " + levelIndex);
                        return;
                    }

                    final List<Document> finalLeaderboardData = leaderboardData;
                    SwingUtilities.invokeLater(() -> {
                        if (victoryView != null) {
                            if (finalLeaderboardData != null && !finalLeaderboardData.isEmpty()) {
                                for (Document doc : finalLeaderboardData) {
                                    if (!doc.containsKey("levelIndex") || doc.getInteger("levelIndex", -1) != levelIndex) {
                                        doc.put("levelIndex", levelIndex);
                                    }
                                }
                            }

                            victoryView.updateLeaderboard(finalLeaderboardData, username);
                            victoryView.setLeaderboardLoading(false);
                        } else {
                            System.err.println("[RankManager EDT Update] VictoryView is null inside EDT task for levelIndex: " + levelIndex);
                        }
                    });

                } catch (CancellationException e) {
                    SwingUtilities.invokeLater(() -> {
                        if (victoryView != null) {
                            victoryView.setLeaderboardLoading(false);
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    SwingUtilities.invokeLater(() -> {
                        if (victoryView != null) {
                            victoryView.setLeaderboardLoading(false);
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[RankManager Worker Done] Error getting result for levelIndex: " + levelIndex + ": " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        if (victoryView != null) {
                            victoryView.setLeaderboardLoading(false);
                        }
                    });
                }
            }
        };

        this.currentWorker = worker;
        worker.execute();
    }

    /**
     * 将当前游戏成绩上传到排行榜数据库 (此方法现在不再被VictoryController直接调用，
     * 上传逻辑已移至loadLeaderboardData的worker中，但保留此方法以备将来使用或内部调用)
     *
     * @param levelIndex 关卡索引
     * @param playerName 玩家名称
     * @param moves 完成步数
     * @param timeInMillis 完成时间（毫秒）
     */
    public void uploadScore(final int levelIndex, final String playerName,
            final int moves, final long timeInMillis) {

        if (UserSession.getInstance().isGuest()) {
            return;
        }

        RankingDatabase rankingDb = RankingDatabase.getInstance();
        if (!rankingDb.isConnected()) {
            System.err.println("[RankManager] 排行榜数据库未连接，跳过分数上传 (levelIndex: " + levelIndex + ")");
            return;
        }
        if (playerName == null || playerName.trim().isEmpty() || playerName.equals("ErrorUser")) {
            System.err.println("[RankManager] 无法上传分数：玩家名称无效 (levelIndex: " + levelIndex + ")");
            return;
        }

        new Thread(() -> {
            try {
                Bson filter = Filters.and(
                        Filters.eq("playerName", playerName),
                        Filters.eq("levelIndex", levelIndex)
                );
                Bson update = Updates.combine(
                        Updates.set("moves", moves),
                        Updates.set("timeInMillis", timeInMillis),
                        Updates.set("timestamp", new Date())
                );
                UpdateOptions options = new UpdateOptions().upsert(true);
                rankingDb.getScoresCollection().updateOne(filter, update, options);
            } catch (Exception e) {
                System.err.println("[RankManager Upload Thread] 后台上传分数时发生错误 (levelIndex: " + levelIndex + "): " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}
