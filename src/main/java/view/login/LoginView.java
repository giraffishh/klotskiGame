package view.login;

/**
 * 登录视图接口，定义登录界面所需的方法
 * 用于解耦视图和控制器
 */
public interface LoginView {
    /**
     * 设置用户名错误状态
     * @param isError 是否显示错误
     */
    void setUsernameError(boolean isError);
    
    /**
     * 设置密码错误状态和错误消息
     * @param isError 是否显示错误
     * @param errorMessage 错误消息
     */
    void setPasswordError(boolean isError, String errorMessage);

    /**
     * 设置确认密码错误状态
     * @param isError 是否显示错误
     */
    void setConfirmPasswordError(boolean isError);

    /**
     * 清除所有错误状态
     */
    void clearAllErrors();
    
    /**
     * 重置表单
     */
    void resetForm();
    
    /**
     * 显示提示消息
     * @param message 消息内容
     * @param title 对话框标题
     * @param messageType 消息类型
     */
    void showStyledMessage(String message, String title, int messageType);
    
    /**
     * 设置窗口可见性
     * @param visible 是否可见
     */
    void setVisible(boolean visible);
}
