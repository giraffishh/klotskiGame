package model;

/**
 * 用户类，表示游戏用户
 */
public class User {
    private String username;
    private String password;
    private int maxUnlockedLevel; // 用户解锁的最高关卡

    /**
     * 创建用户
     * @param username 用户名
     * @param password 密码
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.maxUnlockedLevel = 0; // 默认只解锁第一关
    }
    
    /**
     * 创建用户（包含解锁关卡信息）
     * @param username 用户名
     * @param password 密码
     * @param maxUnlockedLevel 已解锁的最高关卡索引
     */
    public User(String username, String password, int maxUnlockedLevel) {
        this.username = username;
        this.password = password;
        this.maxUnlockedLevel = maxUnlockedLevel;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public int getMaxUnlockedLevel() {
        return maxUnlockedLevel;
    }
    
    public void setMaxUnlockedLevel(int maxUnlockedLevel) {
        this.maxUnlockedLevel = maxUnlockedLevel;
    }
    
    /**
     * 解锁下一关
     * @return 解锁后的关卡索引
     */
    public int unlockNextLevel() {
        this.maxUnlockedLevel++;
        return this.maxUnlockedLevel;
    }
    
    /**
     * 检查指定关卡是否已解锁
     * @param levelIndex 关卡索引
     * @return 如果已解锁返回true
     */
    public boolean isLevelUnlocked(int levelIndex) {
        return levelIndex <= maxUnlockedLevel;
    }
}
