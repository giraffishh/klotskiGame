package view.level;

import javax.swing.*;
import java.util.List;
import controller.LevelSelectController;

public interface LevelSelectView {
    /**
     * 隐藏关卡选择界面
     */
    void hideLevelSelect();

    /**
     * 显示关卡选择界面
     */
    void showLevelSelect();

    /**
     * 设置控制器
     * @param controller 关卡选择控制器
     */
    void setController(LevelSelectController controller);

    /**
     * 显示自定义样式的消息对话框
     * @param message 消息内容
     * @param title 对话框标题
     * @param messageType 消息类型
     */
    void showStyledMessage(String message, String title, int messageType);
}