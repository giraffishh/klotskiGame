package model;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import service.DatabaseService;
import service.UserSession;
import service.UserSession.UserSessionListener;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 应用程序设置管理器
 * 管理全局设置并在用户切换时自动调整
 */
public class AppSettings implements UserSessionListener {
    private static AppSettings instance;
    private final DatabaseService dbService;
    private final Map<String, String> currentSettings;
    
    // 默认设置值
    private static final String DEFAULT_THEME = "Light";
    // 可以在此处添加更多默认设置...
    
    /**
     * 私有构造方法
     */
    private AppSettings() {
        this.dbService = DatabaseService.getInstance();
        this.currentSettings = new HashMap<>();
        
        // 设置默认值
        resetToDefault();
        
        // 注册为用户会话监听器
        UserSession.getInstance().addListener(this);
        
        // 如果用户已经登录，加载其设置
        if (UserSession.getInstance().isLoggedIn() && 
            !UserSession.getInstance().isGuest()) {
            loadSettingsForUser();
        }
    }
    
    /**
     * 获取单例实例
     * @return AppSettings实例
     */
    public static synchronized AppSettings getInstance() {
        if (instance == null) {
            instance = new AppSettings();
        }
        return instance;
    }
    
    /**
     * 将所有设置重置为默认值
     */
    public void resetToDefault() {
        currentSettings.clear();
        currentSettings.put("theme", DEFAULT_THEME);
        // 可以在此处添加更多默认设置...
        
        // 应用默认主题
        applyCurrentTheme();
    }
    
    /**
     * 为当前登录用户加载设置
     */
    private void loadSettingsForUser() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null || session.isGuest()) {
            return;
        }
        
        String username = session.getCurrentUser().getUsername();
        Map<String, String> userSettings = dbService.loadUserSettings(username);
        
        // 更新当前设置
        currentSettings.putAll(userSettings);
        
        // 应用当前主题
        applyCurrentTheme();
    }
    
    /**
     * 将当前设置保存到数据库（仅适用于已登录用户）
     * @return 是否保存成功
     */
    public boolean saveCurrentSettings() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null || session.isGuest()) {
            return false;
        }
        
        String username = session.getCurrentUser().getUsername();
        return dbService.saveUserSettings(username, currentSettings);
    }
    
    /**
     * 保存单个设置
     * @param key 设置键
     * @param value 设置值
     * @return 是否成功（非登录用户返回false）
     */
    public boolean saveSetting(String key, String value) {
        // 更新内存中的设置
        currentSettings.put(key, value);
        
        // 尝试保存到数据库（如果已登录）
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null || session.isGuest()) {
            return false;
        }
        
        String username = session.getCurrentUser().getUsername();
        return dbService.saveUserSetting(username, key, value);
    }
    
    /**
     * 获取设置值
     * @param key 设置键
     * @param defaultValue 默认值
     * @return 设置值，如果不存在则返回默认值
     */
    public String getSetting(String key, String defaultValue) {
        return currentSettings.getOrDefault(key, defaultValue);
    }
    
    /**
     * 获取当前主题
     * @return 当前主题名称
     */
    public String getCurrentTheme() {
        return getSetting("theme", DEFAULT_THEME);
    }
    
    /**
     * 设置当前主题
     * @param themeName 主题名称
     * @return 是否成功应用
     */
    public boolean setCurrentTheme(String themeName) {
        currentSettings.put("theme", themeName);
        return applyCurrentTheme();
    }
    
    /**
     * 应用当前设置的主题
     * @return 是否成功应用
     */
    public boolean applyCurrentTheme() {
        String themeName = getCurrentTheme();
        try {
            if ("Dark".equalsIgnoreCase(themeName)) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                System.out.println("Applied Dark Theme");
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                System.out.println("Applied Light Theme");
            }
            
            // 更新所有打开的窗口以应用新主题
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
            return true;
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to apply theme: " + themeName);
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 用户会话状态变化处理方法
     * 当用户登录或退出时自动调用
     */
    @Override
    public void onSessionStateChanged() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && !session.isGuest()) {
            // 用户登录，加载其设置
            loadSettingsForUser();
        } else {
            // 用户退出或切换为访客，恢复默认设置
            resetToDefault();
        }
    }
}
