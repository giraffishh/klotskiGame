package view.home;

/**
 * Home页面界面接口
 * 定义Home页面需实现的方法
 */
public interface HomeView {
    /**
     * 更新显示的用户名
     * @param username 用户名
     */
    void updateUsername(String username);
    
    /**
     * 显示样式化消息对话框
     * @param message 消息内容
     * @param title 对话框标题
     * @param messageType 消息类型 (如 JOptionPane.INFORMATION_MESSAGE)
     */
    void showStyledMessage(String message, String title, int messageType);
    
    /**
     * 关闭Home页面
     */
    void closeHome();

    /**
     * 显示Home页面
     */
    void showHome();
}
