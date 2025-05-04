package service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * 排行榜数据库服务类负责与MongoDB数据库交互，上传和获取排行榜数据
 */
public class RankingDatabase {

    private static RankingDatabase instance;

    public static synchronized RankingDatabase getInstance() {
        if (instance == null) {
            instance = new RankingDatabase();
        }
        return instance;
    }

    private final String connectionUri = "mongodb+srv://giraffish:hibq67UTeg7bs1Yz@klotskigame.9yxxlqh.mongodb.net/?retryWrites=true&w=majority&appName=KlotskiGame";
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> scoresCollection;

    /**
     * 私有构造函数，初始化数据库连接 (单例模式)
     */
    private RankingDatabase() {
        try {
            // 创建MongoDB连接
            ServerApi serverApi = ServerApi.builder()
                    .version(ServerApiVersion.V1)
                    .build();

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionUri))
                    .serverApi(serverApi)
                    .build();

            // 创建MongoDB客户端
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("KlotskiGame");
            scoresCollection = database.getCollection("scores"); // 集合名称

            //测试连接是否真的有效
            database.runCommand(new Document("ping", 1));
            System.out.println("Ping MongoDB 成功!");

        } catch (MongoException e) {
            System.err.println("MongoDB连接错误 (排行榜): " + e.getMessage());
            // 根据需要处理连接失败的情况，例如禁用排行榜功能
            mongoClient = null;
            database = null;
            scoresCollection = null;
        } catch (IllegalArgumentException e) {
            System.err.println("MongoDB 连接字符串格式错误: " + e.getMessage());
            mongoClient = null;
        }
    }

    /**
     * 上传游戏分数到数据库
     *
     * @param playerName 玩家名称
     * @param levelIndex 关卡索引
     * @param moves 完成步数
     * @param timeInMillis 完成时间（毫秒）
     */
    public void uploadScore(String playerName, int levelIndex, int moves, long timeInMillis) {
        // 检查连接是否成功初始化
        if (scoresCollection == null) {
            System.err.println("无法上传分数：数据库未连接。");
            return;
        }
        // 检查玩家名称是否有效
        if (playerName == null || playerName.trim().isEmpty()) {
            System.err.println("无法上传分数：玩家名称无效。");
            return;
        }

        try {
            Document scoreDocument = new Document("playerName", playerName)
                    .append("levelIndex", levelIndex)
                    .append("moves", moves)
                    .append("timeInMillis", timeInMillis)
                    .append("timestamp", new Date());

            scoresCollection.insertOne(scoreDocument);
            System.out.println("Upload Successfully: Player=" + playerName + ", Level=" + levelIndex + ", Moves=" + moves + ", Time=" + timeInMillis);
        } catch (MongoException e) {
            System.err.println("上传分数失败: " + e.getMessage());
            // 可以选择性地重新抛出异常，让调用者知道失败
            // throw e;
        }
    }

    /**
     * 根据关卡获取排行榜数据
     *
     * @param levelIndex 关卡索引
     * @param limit 限制返回的记录数
     * @return 排行榜数据列表
     */
    public List<Document> getLeaderboard(int levelIndex, int limit) {
        List<Document> results = new ArrayList<>();
        // 检查连接是否成功初始化
        if (scoresCollection == null) {
            System.err.println("无法获取排行榜：数据库未连接。");
            return results; // 返回空列表
        }

        try {
            // 创建查询条件
            Document query = new Document("levelIndex", levelIndex);

            // 按步数升序，然后按时间升序排序
            Document sort = new Document("moves", 1)
                    .append("timeInMillis", 1);

            // 执行查询并获取结果
            scoresCollection.find(query)
                    .sort(sort)
                    .limit(limit)
                    .into(results);
        } catch (MongoException e) {
            System.err.println("获取排行榜数据失败: " + e.getMessage());
        }

        return results;
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                System.out.println("MongoDB连接已关闭 (排行榜)");
            } catch (Exception e) {
                System.err.println("关闭MongoDB连接失败 (排行榜): " + e.getMessage());
            }
        }
    }

    /**
     * 检查数据库是否已成功连接
     *
     * @return true 如果连接成功, false 否则
     */
    public boolean isConnected() {
        return mongoClient != null && scoresCollection != null;
    }
}
