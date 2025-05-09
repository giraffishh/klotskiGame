package service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import model.User;

/**
 * 数据库服务类，提供用户登录和注册功能 使用H2轻量级数据库存储用户信息 密码使用SHA-256加盐哈希存储，提高安全性
 */
public class DatabaseService {

    private static final String DB_URL = "jdbc:h2:file:./gamedb";
    private static final String USER = "sa";
    private static final String PASS = "";

    private static DatabaseService instance;

    /**
     * 私有构造方法，初始化数据库
     */
    private DatabaseService() {
        initDatabase();
    }

    /**
     * 获取单例实例
     *
     * @return 数据库服务实例
     */
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /**
     * 初始化数据库，创建用户表、游戏存档表和用户设置表
     */
    private void initDatabase() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();

            // 创建用户表(如果不存在)，包含密码哈希和盐值列
            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "username VARCHAR(255) PRIMARY KEY, "
                    + "password_hash VARCHAR(255) NOT NULL, "
                    + "salt VARCHAR(255) NOT NULL)");

            // 创建游戏保存记录表(如果不存在)，增加data_hash列用于数据完整性验证
            // 增加game_time列用于存储游戏用时（毫秒）
            // 增加level_index列用于存储当前关卡索引
            stmt.execute("CREATE TABLE IF NOT EXISTS game_saves ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(255) NOT NULL, "
                    + "map_state VARCHAR(1000) NOT NULL, "
                    + // 存储地图状态的字符串
                    "steps INT NOT NULL, "
                    + // 已走步数
                    "game_time BIGINT DEFAULT 0, "
                    + // 游戏用时（毫秒）
                    "level_index INT DEFAULT 0, "
                    + // 当前关卡索引
                    "save_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + // 保存时间
                    "description VARCHAR(255), "
                    + // 可选描述
                    "data_hash VARCHAR(255) NOT NULL, "
                    + // 数据哈希，用于验证完整性
                    "FOREIGN KEY (username) REFERENCES users(username))");

            // 创建user_settings表(如果不存在)
            stmt.execute("CREATE TABLE IF NOT EXISTS user_settings ("
                    + "username VARCHAR(255) PRIMARY KEY, "
                    + "theme VARCHAR(50) NOT NULL DEFAULT 'Light', "
                    + "FOREIGN KEY (username) REFERENCES users(username))");

            // 检查blockTheme列是否存在，如果不存在则添加
            try {
                ResultSet rs = conn.getMetaData().getColumns(null, null, "USER_SETTINGS", "BLOCKTHEME");
                if (!rs.next()) {
                    System.out.println("Adding blockTheme column to user_settings table");
                    stmt.execute("ALTER TABLE user_settings ADD COLUMN blockTheme VARCHAR(50) DEFAULT 'Classic' NOT NULL");
                }
                rs.close();
            } catch (SQLException e) {
                System.err.println("Error checking or adding blockTheme column: " + e.getMessage());
            }

            System.out.println("Database setup successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库连接
     *
     * @return 数据库连接
     * @throws SQLException 连接异常
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    /**
     * 生成随机盐值
     *
     * @return Base64编码的盐值字符串
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 使用SHA-256算法和盐值对密码进行哈希处理
     *
     * @param password 明文密码
     * @param salt 盐值
     * @return 哈希后的密码
     */
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 验证密码是否正确
     *
     * @param password 输入的明文密码
     * @param storedHash 存储的密码哈希值
     * @param salt 存储的盐值
     * @return 密码是否正确
     */
    private boolean verifyPassword(String password, String storedHash, String salt) {
        String newHash = hashPassword(password, salt);
        return newHash != null && newHash.equals(storedHash);
    }

    /**
     * 检查用户名是否已经注册
     *
     * @param username 用户名
     * @return 如果用户名已注册返回true，否则返回false
     */
    public boolean checkUserExists(String username) {
        try (Connection conn = getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT username FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            return rs.next(); // 如果有数据返回，说明用户名已注册
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // 出错时返回false
        }
    }

    /**
     * 尝试登录或注册用户
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果：0-登录成功，1-注册成功，2-密码错误，-1-出错
     */
    public int loginOrRegister(String username, String password) {
        try (Connection conn = getConnection()) {
            // 检查用户是否存在
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT password_hash, salt FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // 用户存在，检查密码
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");

                if (verifyPassword(password, storedHash, salt)) {
                    // 创建User对象表示登录成功的用户
                    User user = new User(username, null); // 不保存明文密码
                    // 将用户信息存入会话
                    UserSession.getInstance().setCurrentUser(user);
                    return 0; // 登录成功
                } else {
                    return 2; // 密码错误
                }
            } else {
                // 用户不存在，创建新用户
                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);

                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)");
                insertStmt.setString(1, username);
                insertStmt.setString(2, passwordHash);
                insertStmt.setString(3, salt);
                insertStmt.executeUpdate();

                // 创建User对象表示注册成功的用户
                User user = new User(username, null); // 不保存明文密码
                // 将用户信息存入会话
                UserSession.getInstance().setCurrentUser(user);
                return 1; // 注册成功
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1; // 发生错误
        }
    }

    /**
     * 获取用户对象
     *
     * @param username 用户名
     * @return 对应的User对象，如果用户不存在则返回null
     */
    public User getUser(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT username FROM users WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(rs.getString("username"), null); // 不返回密码
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 检查用户是否有已保存的游戏存档
     *
     * @param username 用户名
     * @return 如果用户已有存档返回true，否则返回false
     */
    public boolean hasUserGameSave(String username) {
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM game_saves WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0; // 如果count大于0，说明用户已有存档
            }

            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 为数据生成哈希值，用于验证数据完整性
     *
     * @param mapState 地图状态
     * @param steps 步数
     * @param levelIndex 关卡索引
     * @param username 用户名
     * @return 数据哈希值
     */
    private String generateDataHash(String mapState, int steps, int levelIndex, String username) {
        try {
            String dataString = mapState + steps + levelIndex + username;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(dataString.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 更新用户现有游戏存档
     *
     * @param username 用户名
     * @param mapState 地图状态字符串
     * @param steps 当前步数
     * @param gameTime 游戏用时（毫秒）
     * @param levelIndex 当前关卡索引
     * @param description 保存描述
     * @return 是否更新成功
     */
    public boolean updateGameSave(String username, String mapState, int steps, long gameTime, int levelIndex, String description) {
        try (Connection conn = getConnection()) {
            // 计算数据哈希值，包含levelIndex
            String dataHash = generateDataHash(mapState, steps, levelIndex, username);

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE game_saves SET map_state = ?, steps = ?, game_time = ?, level_index = ?, save_time = CURRENT_TIMESTAMP, "
                    + "description = ?, data_hash = ? WHERE username = ?");
            stmt.setString(1, mapState);
            stmt.setInt(2, steps);
            stmt.setLong(3, gameTime);
            stmt.setInt(4, levelIndex);
            stmt.setString(5, description);
            stmt.setString(6, dataHash);
            stmt.setString(7, username);
            int result = stmt.executeUpdate();
            return result > 0; // 如果更新影响了行数，则返回true
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存游戏状态
     *
     * @param username 用户名
     * @param mapState 地图状态字符串
     * @param steps 当前步数
     * @param gameTime 游戏用时（毫秒）
     * @param levelIndex 当前关卡索引
     * @param description 保存描述
     * @return 是否保存成功
     */
    public boolean saveGameState(String username, String mapState, int steps, long gameTime, int levelIndex, String description) {
        // 检查用户是否已有存档
        boolean hasExistingSave = hasUserGameSave(username);

        if (hasExistingSave) {
            // 更新现有存档
            return updateGameSave(username, mapState, steps, gameTime, levelIndex, description);
        } else {
            // 创建新存档
            try (Connection conn = getConnection()) {
                // 计算数据哈希值，包含levelIndex
                String dataHash = generateDataHash(mapState, steps, levelIndex, username);

                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO game_saves (username, map_state, steps, game_time, level_index, description, data_hash) VALUES (?, ?, ?, ?, ?, ?, ?)");
                stmt.setString(1, username);
                stmt.setString(2, mapState);
                stmt.setInt(3, steps);
                stmt.setLong(4, gameTime);
                stmt.setInt(5, levelIndex);
                stmt.setString(6, description);
                stmt.setString(7, dataHash);
                int result = stmt.executeUpdate();
                return result > 0; // 如果插入成功返回true
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * 游戏存档数据类，封装游戏存档的各项信息
     */
    public static class GameSaveData {

        private int id;
        private String username;
        private String mapState;
        private int steps;
        private long gameTime;
        private int levelIndex;
        private Timestamp saveTime;
        private String description;

        public GameSaveData(int id, String username, String mapState, int steps, long gameTime, int levelIndex,
                Timestamp saveTime, String description) {
            this.id = id;
            this.username = username;
            this.mapState = mapState;
            this.steps = steps;
            this.gameTime = gameTime;
            this.levelIndex = levelIndex;
            this.saveTime = saveTime;
            this.description = description;
        }

        public int getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getMapState() {
            return mapState;
        }

        public int getSteps() {
            return steps;
        }

        public long getGameTime() {
            return gameTime;
        }

        public int getLevelIndex() {
            return levelIndex;
        }

        public Timestamp getSaveTime() {
            return saveTime;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 加载用户的游戏存档
     *
     * @param username 用户名
     * @return 游戏存档数据，如果不存在或数据不完整则返回null
     */
    public GameSaveData loadGameSave(String username) {
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, username, map_state, steps, game_time, level_index, save_time, description, data_hash "
                    + "FROM game_saves WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String mapState = rs.getString("map_state");
                int steps = rs.getInt("steps");
                long gameTime = rs.getLong("game_time");
                int levelIndex = rs.getInt("level_index");
                Timestamp saveTime = rs.getTimestamp("save_time");
                String description = rs.getString("description");
                String storedHash = rs.getString("data_hash");

                // 验证数据完整性，包含levelIndex
                String calculatedHash = generateDataHash(mapState, steps, levelIndex, username);
                if (calculatedHash.equals(storedHash)) {
                    return new GameSaveData(id, username, mapState, steps, gameTime, levelIndex, saveTime, description);
                } else {
                    System.out.println("数据完整性验证失败：数据可能已被篡改");
                    return null;
                }
            }

            return null; // 未找到存档
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存用户设置
     *
     * @param username 用户名
     * @param settings 设置键值对
     * @return 是否保存成功
     */
    public boolean saveUserSettings(String username, Map<String, String> settings) {
        try (Connection conn = getConnection()) {
            // 首先检查用户设置是否存在
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT username FROM user_settings WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            PreparedStatement stmt;
            boolean exists = rs.next();

            // 使用一个临时Map来存储标准化的键值，避免重复列名
            Map<String, String> normalizedSettings = new HashMap<>();
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                String normalizedKey = entry.getKey().toLowerCase(); // 转为小写作为标准化键
                normalizedSettings.put(normalizedKey, entry.getValue());
            }

            if (exists) {
                // 更新现有设置
                StringBuilder updateSQL = new StringBuilder("UPDATE user_settings SET ");
                boolean first = true;

                for (Map.Entry<String, String> entry : normalizedSettings.entrySet()) {
                    if (!first) {
                        updateSQL.append(", ");
                    }
                    updateSQL.append(entry.getKey()).append(" = ?");
                    first = false;
                }

                updateSQL.append(" WHERE username = ?");

                try {
                    stmt = conn.prepareStatement(updateSQL.toString());

                    int paramIndex = 1;
                    for (String value : normalizedSettings.values()) {
                        stmt.setString(paramIndex++, value);
                    }
                    stmt.setString(paramIndex, username);

                    int result = stmt.executeUpdate();
                    return result > 0;
                } catch (SQLException e) {
                    System.err.println("Error updating settings: " + e.getMessage());
                    
                    // 如果是列不存在的错误，尝试添加列并重试
                    if (e.getMessage().contains("Column") && e.getMessage().contains("not found")) {
                        // 为每个不存在的列添加列
                        for (String key : normalizedSettings.keySet()) {
                            try {
                                ResultSet colCheck = conn.getMetaData().getColumns(null, null, "USER_SETTINGS", key.toUpperCase());
                                if (!colCheck.next()) {
                                    System.out.println("Adding missing column " + key + " to user_settings table");
                                    Statement alterStmt = conn.createStatement();
                                    alterStmt.execute("ALTER TABLE user_settings ADD COLUMN " + key + " VARCHAR(50) DEFAULT '' NOT NULL");
                                    alterStmt.close();
                                }
                                colCheck.close();
                            } catch (SQLException innerEx) {
                                System.err.println("Error adding column " + key + ": " + innerEx.getMessage());
                            }
                        }
                        
                        // 重试更新操作
                        try {
                            stmt = conn.prepareStatement(updateSQL.toString());
                            int paramIndex = 1;
                            for (String value : normalizedSettings.values()) {
                                stmt.setString(paramIndex++, value);
                            }
                            stmt.setString(paramIndex, username);
                            int result = stmt.executeUpdate();
                            return result > 0;
                        } catch (SQLException retryEx) {
                            System.err.println("Failed retry updating settings: " + retryEx.getMessage());
                            return false;
                        }
                    }
                    return false;
                }
            } else {
                // 插入新设置
                StringBuilder insertSQL = new StringBuilder("INSERT INTO user_settings (username");
                StringBuilder valuesSQL = new StringBuilder(") VALUES (?");

                for (String key : normalizedSettings.keySet()) {
                    insertSQL.append(", ").append(key);
                    valuesSQL.append(", ?");
                }

                insertSQL.append(valuesSQL).append(")");

                stmt = conn.prepareStatement(insertSQL.toString());

                stmt.setString(1, username);
                int paramIndex = 2;
                for (String value : normalizedSettings.values()) {
                    stmt.setString(paramIndex++, value);
                }
            }

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存单个用户设置
     *
     * @param username 用户名
     * @param settingKey 设置键
     * @param settingValue 设置值
     * @return 是否保存成功
     */
    public boolean saveUserSetting(String username, String settingKey, String settingValue) {
        Map<String, String> settings = new HashMap<>();
        settings.put(settingKey, settingValue);
        return saveUserSettings(username, settings);
    }

    /**
     * 加载用户设置
     *
     * @param username 用户名
     * @return 包含用户设置的Map
     */
    public Map<String, String> loadUserSettings(String username) {
        Map<String, String> settings = new HashMap<>();
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM user_settings WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    // 跳过username列
                    if (!"USERNAME".equalsIgnoreCase(columnName)) {
                        settings.put(columnName.toLowerCase(), rs.getString(i));
                    }
                }
            } else {
                // 用户没有设置，返回默认值
                settings.put("theme", "Light");
                settings.put("blocktheme", "Default"); // 使用小写，与AppSettings中的键名一致
            }

            return settings;
        } catch (SQLException e) {
            e.printStackTrace();
            // 出错时返回默认设置
            settings.put("theme", "Light");
            settings.put("blocktheme", "Default"); // 使用小写，与AppSettings中的键名一致
            return settings;
        }
    }

    /**
     * 加载单个用户设置
     *
     * @param username 用户名
     * @param settingKey 设置键
     * @param defaultValue 默认值，如果设置不存在
     * @return 设置值
     */
    public String loadUserSetting(String username, String settingKey, String defaultValue) {
        Map<String, String> settings = loadUserSettings(username);
        return settings.getOrDefault(settingKey.toLowerCase(), defaultValue);
    }
}
