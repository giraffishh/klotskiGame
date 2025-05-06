package controller.game.timer;

import java.text.DecimalFormat;

import javax.swing.Timer;

import view.game.GamePanel;

/**
 * 计时器管理类，负责游戏计时相关的所有功能
 */
public class TimerManager {

    // 游戏面板引用，用于更新时间显示
    private GamePanel view;

    // 计时相关
    private Timer gameTimer;                  // 游戏计时器
    private long startTime;                   // 计时开始时间
    private long elapsedTimeBeforeStart = 0;  // 计时器启动前已经过的时间（用于暂停/继续）
    private boolean timerRunning = false;     // 计时器运行状态

    // 用于格式化毫秒显示的格式器
    private final DecimalFormat millisFormat = new DecimalFormat("00");

    /**
     * 构造函数
     *
     * @param view 游戏面板视图
     */
    public TimerManager(GamePanel view) {
        this.view = view;
        initializeTimer();
    }

    /**
     * 更新视图引用
     *
     * @param newView 新的游戏面板视图
     */
    public void updateView(GamePanel newView) {
        this.view = newView;
    }

    /**
     * 初始化游戏计时器
     */
    private void initializeTimer() {
        gameTimer = new Timer(50, e -> {
            // 计算当前经过的总时间（毫秒）
            long currentTime = System.currentTimeMillis();
            long totalElapsed = elapsedTimeBeforeStart + (currentTime - startTime);

            // 更新时间显示
            updateTimeDisplay(totalElapsed);
        });
    }

    /**
     * 启动游戏计时器
     */
    public void startTimer() {
        if (!timerRunning) {
            // 记录启动时间点
            startTime = System.currentTimeMillis();
            gameTimer.start();
            timerRunning = true;
        }
    }

    /**
     * 停止游戏计时器
     */
    public void stopTimer() {
        if (timerRunning) {
            // 保存已经过的时间
            long currentTime = System.currentTimeMillis();
            elapsedTimeBeforeStart += (currentTime - startTime);
            gameTimer.stop();
            timerRunning = false;
        }
    }

    /**
     * 重置游戏计时器
     */
    public void resetTimer() {
        // 停止计时器
        if (gameTimer != null) {
            gameTimer.stop();
        }
        // 重置计时数据
        elapsedTimeBeforeStart = 0;
        timerRunning = false;
        // 更新显示为零
        updateTimeDisplay(0);
    }

    /**
     * 更新时间显示，格式为 mm:ss.xx（分:秒.厘秒）
     *
     * @param totalMillis 总毫秒数
     */
    private void updateTimeDisplay(long totalMillis) {
        int minutes = (int) (totalMillis / 60000);
        int seconds = (int) ((totalMillis % 60000) / 1000);
        int centiseconds = (int) ((totalMillis % 1000) / 10);

        String timeText = String.format("Time: %02d:%02d.%s",
                minutes, seconds, millisFormat.format(centiseconds));

        if (view != null) {
            view.updateTimeDisplay(timeText);
        }
    }

    /**
     * 获取当前游戏用时（毫秒）
     *
     * @return 游戏用时（毫秒）
     */
    public long getGameTimeInMillis() {
        // 计算当前经过的总时间（毫秒）
        if (timerRunning) {
            long currentTime = System.currentTimeMillis();
            return elapsedTimeBeforeStart + (currentTime - startTime);
        } else {
            return elapsedTimeBeforeStart;
        }
    }

    /**
     * 设置加载的游戏时间
     *
     * @param gameTime 游戏时间（毫秒）
     */
    public void setLoadedGameTime(long gameTime) {
        // 停止计时器
        stopTimer();
        // 设置已经过的时间
        elapsedTimeBeforeStart = gameTime;
        // 更新显示
        updateTimeDisplay(gameTime);
    }

    /**
     * 检查计时器是否运行
     *
     * @return 计时器运行状态
     */
    public boolean isTimerRunning() {
        return timerRunning;
    }
}
