package service;

import model.User;
import java.sql.*;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 数据库服务类，提供用户登录和注册功能
 * 使用H2轻量级数据库存储用户信息
 * 密码使用SHA-256加盐哈希存储，提高安全性
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
     * @return 数据库服务实例
     */
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }
    
    /**
     * 初始化数据库，创建用户表
     */
    private void initDatabase() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            // 创建用户表(如果不存在)，包含密码哈希和盐值列
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "username VARCHAR(255) PRIMARY KEY, " +
                         "password_hash VARCHAR(255) NOT NULL, " +
                         "salt VARCHAR(255) NOT NULL)");

            // 检查是否存在旧表结构（没有salt列）
            try {
                ResultSet rs = conn.getMetaData().getColumns(null, null, "USERS", "SALT");
                if (!rs.next()) {
                    // 如果不存在salt列，说明是旧表结构，需要删除重建
                    stmt.execute("DROP TABLE users");
                    stmt.execute("CREATE TABLE users (" +
                                "username VARCHAR(255) PRIMARY KEY, " +
                                "password_hash VARCHAR(255) NOT NULL, " +
                                "salt VARCHAR(255) NOT NULL)");
                    System.out.println("数据库表结构已更新");
                }
            } catch (SQLException e) {
                System.out.println("检查表结构时出错: " + e.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取数据库连接
     * @return 数据库连接
     * @throws SQLException 连接异常
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }
    
    /**
     * 生成随机盐值
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
}

