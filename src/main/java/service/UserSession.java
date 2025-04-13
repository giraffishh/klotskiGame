package service;

import model.User;

/**
 * 用户会话类，使用单例模式存储当前登录用户信息
 * 应用程序的其他部分可以通过此类获取当前登录用户的信息
 */
public class UserSession {
    private static UserSession instance;
    private User currentUser;
    
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
    }
}
