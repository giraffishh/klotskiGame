package controller.storage.rank;

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
    // 新增：存储最近一次加载的完整排行榜数据
    private volatile List<Document> lastLoadedFullData = null;

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
     * 加载排行榜数据并更新到视图组件。 如果不是访客模式且不是练习模式，此方法现在也会先尝试上传/更新分数，然后再加载排行榜。
     *
     * @param victoryView 胜利视图组件
     * @param levelIndex 要加载的关卡索引
     * @param isGuest 是否是访客模式
     * @param username 当前用户名
     * @param moves 当前步数
     * @param timeInMillis 当前用时
     * @param gameMode 游戏模式，0为练习模式，1为竞速模式
     */
    public void loadLeaderboardData(final VictoryView victoryView, final int levelIndex,
            final boolean isGuest, final String username, final int moves, final long timeInMillis, final int gameMode) {

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
                    // 只有在以下条件下上传成绩：不是访客模式 且 不是练习模式
                    if (!isGuest && gameMode != model.MapModel.PRACTICE_MODE) {
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
                    } else if (gameMode == model.MapModel.PRACTICE_MODE) {
                        System.out.println("[RankManager Worker BG] 练习模式，跳过上传分数");
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Leaderboard load interrupted before DB query for levelIndex: " + levelIndex);
                    }

                    RankingDatabase rankingDb = RankingDatabase.getInstance();
                    if (!rankingDb.isConnected()) {
                        System.err.println("[RankManager Worker BG] 数据库未连接，无法加载 levelIndex: " + levelIndex);
                        lastLoadedFullData = new ArrayList<>(); // 清空旧数据
                        return lastLoadedFullData;
                    }

                    // 获取完整的排行榜数据（不限制数量）
                    List<Document> fullLeaderboardData = rankingDb.getLeaderboard(levelIndex, 0); // 0表示获取所有
                    lastLoadedFullData = fullLeaderboardData; // 存储完整数据

                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Leaderboard load interrupted after DB query for levelIndex: " + levelIndex);
                    }

                    // --- 后续处理（访客模式、限制数量） ---
                    List<Document> processedData;
                    if (isGuest) {
                        // 为访客添加临时分数并排序，然后取前10
                        List<Document> combined = new ArrayList<>(fullLeaderboardData); // 使用完整数据
                        Document guestScore = new Document()
                                .append("playerName", "Guest")
                                .append("levelIndex", levelIndex)
                                .append("moves", moves)
                                .append("timeInMillis", timeInMillis)
                                .append("timestamp", new Date());
                        combined.add(guestScore);

                        // 排序逻辑
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

                        // 取前10名
                        processedData = combined.subList(0, Math.min(combined.size(), 10));

                    } else if (gameMode == model.MapModel.PRACTICE_MODE) {
                        // 练习模式下只显示排行榜，不追加当前成绩
                        processedData = fullLeaderboardData.subList(0, Math.min(fullLeaderboardData.size(), 10));
                        System.out.println("[RankManager Worker BG] 练习模式，只显示排行榜，不追加当前成绩");
                    } else {
                        // 非访客，直接取前10名
                        processedData = fullLeaderboardData.subList(0, Math.min(fullLeaderboardData.size(), 10));
                    }

                    return processedData; // 返回处理后的数据（最多10条或包含访客）
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lastLoadedFullData = null; // 中断时清空
                    return null;
                } catch (Exception e) {
                    System.err.println("[RankManager Worker BG] 获取 levelIndex: " + levelIndex + " 排行榜数据时发生异常: " + e.getMessage());
                    e.printStackTrace();
                    lastLoadedFullData = new ArrayList<>(); // 出错时返回空列表
                    return lastLoadedFullData;
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

                            // 在练习模式下不高亮显示当前成绩
                            if (gameMode == model.MapModel.PRACTICE_MODE) {
                                victoryView.updateLeaderboard(finalLeaderboardData, null); // 传递null表示不高亮任何成绩
                                System.out.println("[RankManager EDT Update] 练习模式，不高亮显示玩家成绩");
                            } else {
                                victoryView.updateLeaderboard(finalLeaderboardData, username);
                            }
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
     * 获取最近一次成功加载的完整排行榜数据。
     *
     * @return 包含完整排行榜文档的列表，如果从未加载或加载失败则可能为 null 或空列表。
     */
    public List<Document> getLastLoadedData() {
        return lastLoadedFullData;
    }

    /**
     * 将当前游戏成绩上传到排行榜数据库 在练习模式下不会上传成绩
     *
     * @param levelIndex 关卡索引
     * @param playerName 玩家名称
     * @param moves 完成步数
     * @param timeInMillis 完成时间（毫秒）
     * @param gameMode 游戏模式
     */
    public void uploadScore(final int levelIndex, final String playerName,
            final int moves, final long timeInMillis, final int gameMode) {

        if (UserSession.getInstance().isGuest() || gameMode == model.MapModel.PRACTICE_MODE) {
            if (gameMode == model.MapModel.PRACTICE_MODE) {
                System.out.println("[RankManager] 练习模式，不上传分数");
            }
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
