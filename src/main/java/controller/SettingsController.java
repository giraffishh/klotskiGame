package controller;

import model.AppSettings;
import service.UserSession;
import view.settings.SettingsView;

import javax.swing.*;

/**
 * Settings页面控制器
 * 处理Settings页面的业务逻辑，如加载和保存设置
 */
public class SettingsController {
    private final SettingsView settingsView;
    private final AppSettings appSettings;

    /**
     * 创建SettingsController
     * @param settingsView Settings页面视图
     */
    public SettingsController(SettingsView settingsView) {
        this.settingsView = settingsView;
        this.appSettings = AppSettings.getInstance();
    }

    /**
     * 加载设置
     * 从AppSettings加载设置并更新视图
     */
    public void loadSettings() {
        // 显示当前主题设置
        String currentTheme = appSettings.getCurrentTheme();
        settingsView.displayThemeSetting(currentTheme);
        System.out.println("Loading settings, current theme: " + currentTheme);
    }

    /**
     * 保存设置
     * 获取视图中的设置值并保存
     */
    public void saveSettings() {
        // 从视图获取当前选择的主题
        String selectedTheme = settingsView.getSelectedTheme();
        UserSession session = UserSession.getInstance();
        
        // 更新AppSettings中的主题设置并应用
        boolean themeApplied = appSettings.setCurrentTheme(selectedTheme);
        
        // 尝试保存设置（如果用户已登录）
        boolean settingsSaved = appSettings.saveSetting("theme", selectedTheme);
        
        if (!themeApplied) {
            // 主题应用失败
            settingsView.showStyledMessage(
                "Failed to apply theme. Please try again.",
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (session.isGuest() || !session.isLoggedIn()) {
            // 访客模式或未登录
            settingsView.showStyledMessage(
                "<html>Settings has been temporarily applied.<br>Create an account to permanently save settings.</html>",
                "Notice", 
                JOptionPane.INFORMATION_MESSAGE);
        } else if (!settingsSaved) {
            // 设置保存失败
            settingsView.showStyledMessage(
                "Theme applied but failed to save settings to your profile.",
                "Warning", 
                JOptionPane.WARNING_MESSAGE);
        } else {
            // 保存成功
            settingsView.showStyledMessage(
                "Settings saved successfully.",
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        settingsView.closeSettings(); // 关闭设置窗口
    }

    /**
     * 取消设置
     * 关闭设置窗口，不保存任何更改
     */
    public void cancelSettings() {
        System.out.println("Cancelling settings...");
        settingsView.closeSettings();
    }
}
