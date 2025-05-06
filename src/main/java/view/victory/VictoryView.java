package view.victory;

import java.awt.event.ActionListener;
import java.util.List;

import org.bson.Document;

/**
 * 定义胜利界面的接口，包含所有胜利界面需要实现的功能
 */
public interface VictoryView {

    /**
     * 显示胜利界面，包含步数、用时信息和原始用时（毫秒）
     *
     * @param victoryMessage 胜利消息
     * @param steps 步数
     * @param timeElapsed 格式化后的时间字符串
     * @param gameTimeInMillis 游戏用时（毫秒）
     */
    void showVictory(String victoryMessage, int steps, String timeElapsed, long gameTimeInMillis);

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
     *
     * @param enabled 是否启用
     */
    void setNextLevelButtonEnabled(boolean enabled);

    /**
     * 设置胜利消息文本
     *
     * @param message 消息内容
     */
    void setVictoryMessage(String message);

    /**
     * 更新排行榜显示
     *
     * @param leaderboardData 排行榜数据列表
     * @param currentUsername 当前玩家用户名
     */
    void updateLeaderboard(List<Document> leaderboardData, String currentUsername);

    /**
     * 设置排行榜加载状态
     *
     * @param isLoading 是否正在加载
     */
    void setLeaderboardLoading(boolean isLoading);

    /**
     * 设置练习模式提示信息
     *
     * @param tip 提示信息，为null时清除提示
     */
    void setPracticeModeTip(String tip);
}
