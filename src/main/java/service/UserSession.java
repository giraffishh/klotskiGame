package service;

import model.User;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户会话类，使用单例模式存储当前登录用户信息
 * 应用程序的其他部分可以通过此类获取当前登录用户的信息
 */
public class UserSession {
    private static UserSession instance;
    private User currentUser;
    private boolean isGuest = false;

    // 添加观察者列表，用于通知用户状态变化
    private List<UserSessionListener> listeners = new ArrayList<>();

    /**
     * 用户会话监听器接口，用于在用户状态变化时通知UI
     */
    public interface UserSessionListener {
        /**
         * 当用户会话状态发生变化时调用
         */
        void onSessionStateChanged();
    }

    /**
     * 私有构造方法，防止外部实例化
     */
    private UserSession() {
        // 私有构造器
    }
    
    /**
     * 获取单例实例
     * @return 用户会话实例
     */
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    /**
     * 设置当前登录用户
     * @param user 登录的用户对象
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.isGuest = false; // 设置正常用户时，取消访客状态
        notifyListeners(); // 通知观察者状态已变化
    }
    
    /**
     * 获取当前登录用户
     * @return 当前登录的用户对象，如果未登录则返回null
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * 检查用户是否已登录
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * 注销当前用户
     */
    public void logout() {
        currentUser = null;
        isGuest = false;
        notifyListeners(); // 通知观察者状态已变化
    }

    /**
     * 设置访客状态
     * @param guest 是否为访客
     */
    public void setGuest(boolean guest) {
        isGuest = guest;
        notifyListeners(); // 通知观察者状态已变化
    }

    /**
     * 检查当前用户是否为访客
     * @return 是否为访客
     */
    public boolean isGuest() {
        return isGuest;
    }

    /**
     * 添加用户会话监听器
     * @param listener 监听器对象
     */
    public void addListener(UserSessionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除用户会话监听器
     * @param listener 监听器对象
     */
    public void removeListener(UserSessionListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器用户会话状态已发生变化
     */
    private void notifyListeners() {
        for (UserSessionListener listener : listeners) {
            listener.onSessionStateChanged();
        }
    }

    /**
     * 检查指定关卡是否已解锁
     * 
     * @param levelIndex 关卡索引
     * @return 如果是访客或该关卡已解锁，返回true
     */
    public boolean isLevelUnlocked(int levelIndex) {
        // 访客模式下所有关卡都可用
        if (isGuest) {
            return true;
        }
        
        // 检查用户是否登录
        if (!isLoggedIn() || currentUser == null) {
            return levelIndex == 0; // 默认只解锁第一关
        }
        
        // 检查用户解锁状态
        return currentUser.isLevelUnlocked(levelIndex);
    }
    
    /**
     * 解锁下一关卡
     * 
     * @return 是否成功解锁
     */
    public boolean unlockNextLevel() {
        // 访客模式不保存解锁进度
        if (isGuest || !isLoggedIn() || currentUser == null) {
            return false;
        }
        
        // 解锁下一关
        int newLevel = currentUser.unlockNextLevel();
        
        // 保存到数据库
        DatabaseService dbService = DatabaseService.getInstance();
        boolean result = dbService.updateUserUnlockedLevel(currentUser.getUsername(), newLevel);
        
        if (result) {
            System.out.println("Unlocked level " + newLevel + " for user: " + currentUser.getUsername());
        } else {
            System.out.println("Failed to update unlocked level in database");
        }
        
        return result;
    }
}
