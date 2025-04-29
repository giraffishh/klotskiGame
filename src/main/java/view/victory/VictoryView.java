package view.victory;

import java.awt.event.ActionListener;

/**
 * 定义胜利界面的接口，包含所有胜利界面需要实现的功能
 */
public interface VictoryView {

    /**
     * 显示胜利界面
     */
    void showVictory(String victoryMessage);

    /**
     * 显示胜利界面，包含步数信息
     */
    void showVictory(String victoryMessage, int steps);

    /**
     * 显示胜利界面，包含步数和用时信息
     */
    void showVictory(String victoryMessage, int steps, String timeElapsed);

    /**
     * 隐藏胜利界面
     */
    void hideVictory();

    /**
     * 设置"回到主页"按钮的监听器
     */
    void setHomeListener(ActionListener listener);

    /**
     * 设置"回到选关界面"按钮的监听器
     */
    void setLevelSelectListener(ActionListener listener);

    /**
     * 设置"再来一次"按钮的监听器
     */
    void setRestartListener(ActionListener listener);

    /**
     * 设置"下一关"按钮的监听器
     */
    void setNextLevelListener(ActionListener listener);


    /**
     * 设置"下一关"按钮的启用状态
     * @param enabled 是否启用
     */
    void setNextLevelButtonEnabled(boolean enabled);

    /**
     * 设置胜利消息文本
     * @param message 消息内容
     */
    void setVictoryMessage(String message);
}
