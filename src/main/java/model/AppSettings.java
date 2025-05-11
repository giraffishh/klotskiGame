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
    private boolean settingsChanged = false;
    private String lastAppliedTheme = null;
    
    // 设置相关常量
    private static final String DEFAULT_THEME = "Light";
    private static final String DEFAULT_BLOCK_THEME = "Default";
    private static final String THEME_KEY = "theme";
    private static final String BLOCK_THEME_KEY = "blocktheme";
    
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
        if (UserSession.getInstance().isLoggedIn() && !UserSession.getInstance().isGuest()) {
            loadSettingsForUser();
        }
        
        // 添加程序退出钩子保存设置
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (settingsChanged) {
                System.out.println("应用程序关闭中，保存设置...");
                saveAllSettings();
            }
        }));
    }
    
    /**
     * 获取单例实例
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
        currentSettings.put(THEME_KEY, DEFAULT_THEME);
        currentSettings.put(BLOCK_THEME_KEY, DEFAULT_BLOCK_THEME);
        
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
     * 保存所有设置到数据库
     * @return 是否保存成功
     */
    public boolean saveAllSettings() {
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null || session.isGuest()) {
            settingsChanged = false;
            return false;
        }
        
        String username = session.getCurrentUser().getUsername();
        boolean result = dbService.saveUserSettings(username, currentSettings);
        
        if (result) {
            settingsChanged = false;
            System.out.println("所有设置已成功保存到数据库");
        }
        
        return result;
    }
    
    /**
     * 保存单个设置
     * @return 是否成功
     */
    public boolean saveSetting(String key, String value) {
        currentSettings.put(key, value);
        settingsChanged = true;
        
        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn() || session.getCurrentUser() == null || session.isGuest()) {
            return false;
        }
        
        return dbService.saveUserSetting(session.getCurrentUser().getUsername(), key, value);
    }
    
    /**
     * 获取设置值
     */
    public String getSetting(String key, String defaultValue) {
        return currentSettings.getOrDefault(key, defaultValue);
    }
    
    // 主题相关方法
    public String getCurrentTheme() {
        return getSetting(THEME_KEY, DEFAULT_THEME);
    }
    
    public boolean setCurrentTheme(String themeName) {
        currentSettings.put(THEME_KEY, themeName);
        settingsChanged = true;
        saveSetting(THEME_KEY, themeName);
        return applyCurrentTheme();
    }
    
    /**
     * 获取当前方块主题
     * @return 当前方块主题名称
     */
    public String getCurrentBlockTheme() {
        return getSetting(BLOCK_THEME_KEY, DEFAULT_BLOCK_THEME);
    }

    /**
     * 设置当前方块主题
     * @param blockThemeName 方块主题名称
     * @return 是否成功应用
     */
    public boolean setCurrentBlockTheme(String blockThemeName) {
        currentSettings.put(BLOCK_THEME_KEY, blockThemeName);
        settingsChanged = true;
        return saveSetting(BLOCK_THEME_KEY, blockThemeName);
    }
    
    /**
     * 应用当前设置的主题
     */
    public boolean applyCurrentTheme() {
        String themeName = getCurrentTheme();
        
        // 避免重复应用相同的主题
        if (themeName.equals(lastAppliedTheme)) {
            return true;
        }
        
        try {
            if ("Dark".equalsIgnoreCase(themeName)) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                System.out.println("Applied Dark Theme");
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                System.out.println("Applied Light Theme");
            }
            
            // 更新所有窗口
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
            
            lastAppliedTheme = themeName;
            return true;
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to apply theme: " + themeName);
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 强制重新应用当前主题
     */
    public boolean forceApplyCurrentTheme() {
        lastAppliedTheme = null;
        return applyCurrentTheme();
    }
    
    /**
     * 用户会话状态变化处理方法
     */
    @Override
    public void onSessionStateChanged() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && !session.isGuest()) {
            loadSettingsForUser();
        } else {
            // 先保存未保存的设置
            if (settingsChanged && UserSession.getInstance().getCurrentUser() != null 
                    && !UserSession.getInstance().isGuest()) {
                saveAllSettings();
            }
            resetToDefault();
        }
    }
    
    /**
     * 检查是否有未保存的设置
     */
    public boolean hasUnsavedChanges() {
        return settingsChanged;
    }
}
