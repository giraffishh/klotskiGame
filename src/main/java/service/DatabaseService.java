package service;

import model.User;
import java.sql.*;
import java.io.File;

/**
 * 数据库服务类，提供用户登录和注册功能
 * 使用H2轻量级数据库存储用户信息
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
            // 创建用户表(如果不存在)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "username VARCHAR(255) PRIMARY KEY, " +
                         "password VARCHAR(255) NOT NULL)");
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
     * 尝试登录或注册用户
     * @param username 用户名
     * @param password 密码
     * @return 登录结果：0-登录成功，1-注册成功，2-密码错误，-1-出错
     */
    public int loginOrRegister(String username, String password) {
        try (Connection conn = getConnection()) {
            // 检查用户是否存在
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT password FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                // 用户存在，检查密码
                String storedPassword = rs.getString("password");
                if (storedPassword.equals(password)) {
                    return 0; // 登录成功
                } else {
                    return 2; // 密码错误
                }
            } else {
                // 用户不存在，创建新用户
                PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO users (username, password) VALUES (?, ?)");
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.executeUpdate();
                return 1; // 注册成功
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1; // 发生错误
        }
    }
}
