package controller.core;

import javax.swing.JOptionPane;

import model.AppSettings;
import service.UserSession;
import view.settings.SettingsView;
import view.util.FrameManager;
import view.util.ImageManager;

/**
 * Settings页面控制器
 * 处理Settings页面的业务逻辑，如加载和保存设置
 */
public class SettingsController {
    private final SettingsView settingsView;
    private final AppSettings appSettings;

    /**
     * 创建SettingsController
     */
    public SettingsController(SettingsView settingsView) {
        this.settingsView = settingsView;
        this.appSettings = AppSettings.getInstance();
    }

    /**
     * 从AppSettings加载设置并更新视图
     */
    public void loadSettings() {
        // 加载并显示当前设置
        String currentTheme = appSettings.getCurrentTheme();
        String currentBlockTheme = appSettings.getCurrentBlockTheme();
        boolean controlButtonsEnabled = appSettings.isControlButtonsEnabled();
        
        settingsView.displayThemeSetting(currentTheme);
        settingsView.displayBlockThemeSetting(currentBlockTheme);
        settingsView.displayControlButtonsSetting(controlButtonsEnabled);
    }

    /**
     * 保存设置并处理结果
     */
    public void saveSettings() {
        // 获取用户选择的设置
        String selectedTheme = settingsView.getSelectedTheme();
        String selectedBlockTheme = settingsView.getSelectedBlockTheme();
        boolean controlButtonsEnabled = settingsView.isControlButtonsEnabled();
        UserSession session = UserSession.getInstance();

        // 检查设置是否变化
        String currentTheme = appSettings.getCurrentTheme();
        String currentBlockTheme = appSettings.getCurrentBlockTheme();
        boolean currentControlButtonsEnabled = appSettings.isControlButtonsEnabled();
        boolean settingsChanged = !currentTheme.equals(selectedTheme) || 
                                 !currentBlockTheme.equals(selectedBlockTheme) ||
                                 currentControlButtonsEnabled != controlButtonsEnabled;

        // 应用设置
        boolean themeApplied = appSettings.setCurrentTheme(selectedTheme);
        boolean blockThemeApplied = appSettings.setCurrentBlockTheme(selectedBlockTheme);
        boolean controlButtonsApplied = appSettings.setControlButtonsEnabled(controlButtonsEnabled);
        
        // 重置图片缓存
        ImageManager.resetImageCache();

        // 保存设置到数据库
        boolean allSettingsSaved = false;
        if (session.isLoggedIn() && !session.isGuest()) {
            allSettingsSaved = appSettings.saveAllSettings();
        }

        // 处理结果
        if (!themeApplied) {
            settingsView.showStyledMessage(
                    "Failed to apply theme. Please try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 显示适当的消息
        if (session.isGuest() || !session.isLoggedIn()) {
            showGuestMessage();
        } else if (!allSettingsSaved) {
            showSaveFailedMessage();
        } else {
            showSuccessMessage();
        }

        // 如果图片主题改变了，刷新游戏界面
        if (settingsChanged) {
            FrameManager.getInstance().refreshGameInterface();
        }

        // 返回主页
        FrameManager.getInstance().navigateFromSettingsToHome();
    }
    
    // 提取显示消息的方法
    private void showGuestMessage() {
        settingsView.showStyledMessage(
                "<html>Settings has been temporarily applied.<br>Create an account to permanently save settings.</html>",
                "Notice",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showSaveFailedMessage() {
        settingsView.showStyledMessage(
                "Settings applied but failed to save to your profile.",
                "Warning",
                JOptionPane.WARNING_MESSAGE);
    }
    
    private void showSuccessMessage() {
        settingsView.showStyledMessage(
                "Settings saved successfully.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 取消设置并返回主页
     */
    public void cancelSettings() {
        FrameManager.getInstance().navigateFromSettingsToHome();
    }
}
