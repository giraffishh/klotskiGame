package view.settings;

/**
 * Settings页面界面接口
 * 定义Settings页面需实现的方法
 */
public interface SettingsView {
    /**
     * 显示主题设置
     * @param themeName 主题名称
     */
    void displayThemeSetting(String themeName);

    /**
     * 获取当前选择的主题
     * @return 选择的主题名称
     */
    String getSelectedTheme();
    
    /**
     * 显示方块主题设置
     * @param blockThemeName 方块主题名称
     */
    void displayBlockThemeSetting(String blockThemeName);

    /**
     * 获取当前选择的方块主题
     * @return 选择的方块主题名称
     */
    String getSelectedBlockTheme();

    /**
     * 显示样式化消息对话框
     */
    void showStyledMessage(String message, String title, int messageType);

    /**
     * 关闭设置页面
     */
    void closeSettings();

    /**
     * 显示控制按钮设置
     * @param enabled 是否启用控制按钮
     */
    void displayControlButtonsSetting(boolean enabled);

    /**
     * 获取当前控制按钮设置
     * @return 是否启用控制按钮
     */
    boolean isControlButtonsEnabled();

    /**
     * 显示背景音乐设置
     * @param enabled 是否启用背景音乐
     */
    void displayMusicSetting(boolean enabled);

    /**
     * 获取当前背景音乐设置
     * @return 是否启用背景音乐
     */
    boolean isMusicEnabled();
}
