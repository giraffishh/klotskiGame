package view.settings;

/**
 * Settings页面界面接口
 * 定义Settings页面需实现的方法
 */
public interface SettingsView {
    /**
     * 显示主题设置
     * @param themeName 主题名称 (例如 "Light", "Dark")
     */
    void displayThemeSetting(String themeName);

    /**
     * 获取当前选择的主题
     * @return 选择的主题名称
     */
    String getSelectedTheme();

    /**
     * 显示样式化消息对话框
     * @param message 消息内容
     * @param title 对话框标题
     * @param messageType 消息类型 (如 JOptionPane.INFORMATION_MESSAGE)
     */
    void showStyledMessage(String message, String title, int messageType);

    /**
     * 关闭设置页面
     */
    void closeSettings();
}
