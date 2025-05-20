package controller.game.sound;

import model.AppSettings;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 游戏音效管理器
 * 负责加载和播放游戏中的方块移动音效和背景音乐
 */
public class SoundManager {
    
    // 音效类型枚举
    public enum SoundType {
        MOVE    // 方块移动
    }
    
    // 音效资源路径
    private static final String SOUND_PATH = "/sounds/";
    private static final String BACKGROUND_MUSIC_FILE = "background.wav";
    
    // 音效文件映射
    private final Map<SoundType, String> soundFiles;
    
    // 已加载的音效剪辑
    private final Map<SoundType, Clip> soundClips;
    
    // 背景音乐Clip
    private Clip backgroundMusicClip;
    
    // 是否启用音效
    private boolean soundEnabled = true;
    
    // 是否启用背景音乐
    private boolean musicEnabled = true;
    
    // 音效音量级别 (0.0f - 1.0f)
    private float volume = 0.5f;
    
    // 背景音乐音量级别 (0.0f - 1.0f)
    private float musicVolume = 0.3f;
    
    /**
     * 构造函数，初始化音效管理器
     */
    public SoundManager() {
        soundFiles = new HashMap<>();
        soundClips = new HashMap<>();
        
        // 配置音效文件
        initSoundFiles();
        
        // 预加载音效
        loadSounds();
        
        // 仅加载背景音乐，但不自动播放
        loadBackgroundMusic();
        // 注意：移除了自动开始播放背景音乐的代码
    }
    
    /**
     * 初始化音效文件配置
     */
    private void initSoundFiles() {
        soundFiles.put(SoundType.MOVE, "move.wav");
    }
    
    /**
     * 加载所有音效
     */
    private void loadSounds() {
        for (Map.Entry<SoundType, String> entry : soundFiles.entrySet()) {
            try {
                String resourcePath = SOUND_PATH + entry.getValue();
                URL soundURL = getClass().getResource(resourcePath);
                
                if (soundURL != null) {
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    soundClips.put(entry.getKey(), clip);
                } else {
                    System.err.println("无法找到音效文件: " + resourcePath);
                }
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.err.println("加载音效失败: " + entry.getValue() + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * 加载背景音乐
     */
    private void loadBackgroundMusic() {
        try {
            String resourcePath = SOUND_PATH + BACKGROUND_MUSIC_FILE;
            URL musicURL = getClass().getResource(resourcePath);
            
            if (musicURL != null) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicURL);
                backgroundMusicClip = AudioSystem.getClip();
                backgroundMusicClip.open(audioStream);
                
                // 不在这里设置循环播放，而是在实际开始播放时设置
                // backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
                
                // 设置初始音量
                setMusicVolume(musicVolume);
            } else {
                System.err.println("无法找到背景音乐文件: " + resourcePath);
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("加载背景音乐失败: " + e.getMessage());
        }
    }
    
    /**
     * 播放指定类型的音效
     * 
     * @param type 要播放的音效类型
     */
    public void playSound(SoundType type) {
        if (!soundEnabled) {
            return;
        }
        
        Clip clip = soundClips.get(type);
        if (clip != null) {
            try {
                if (clip.isRunning()) {
                    clip.stop();
                }
                clip.setFramePosition(0);
                
                // 设置音量
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = 
                        (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float) (Math.log10(volume) * 20.0);
                    gainControl.setValue(dB);
                }
                
                clip.start();
            } catch (Exception e) {
                System.err.println("播放音效失败: " + type + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * 开始播放背景音乐
     */
    public void startBackgroundMusic() {
        // 检查全局设置，如果设置中禁用了音乐，则不播放
        if (!AppSettings.getInstance().isMusicEnabled()) {
            return;
        }
        
        if (!musicEnabled || backgroundMusicClip == null) {
            return;
        }
        
        try {
            if (backgroundMusicClip.isRunning()) {
                return; // 已经在播放中
            }
            
            // 在实际开始播放时设置循环
            backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusicClip.setFramePosition(0);
            backgroundMusicClip.start();
            System.out.println("背景音乐开始播放");
        } catch (Exception e) {
            System.err.println("播放背景音乐失败: " + e.getMessage());
        }
    }
    
    /**
     * 暂停背景音乐
     */
    public void pauseBackgroundMusic() {
        if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
            backgroundMusicClip.stop();
            System.out.println("背景音乐已暂停");
        }
    }
    
    /**
     * 恢复播放背景音乐（从暂停状态）
     */
    public void resumeBackgroundMusic() {
        if (!musicEnabled || backgroundMusicClip == null) {
            return;
        }
        
        try {
            if (!backgroundMusicClip.isRunning()) {
                backgroundMusicClip.start();
                System.out.println("背景音乐已恢复");
            }
        } catch (Exception e) {
            System.err.println("恢复背景音乐失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止背景音乐
     */
    public void stopBackgroundMusic() {
        if (backgroundMusicClip != null) {
            backgroundMusicClip.stop();
            backgroundMusicClip.setFramePosition(0);
            System.out.println("背景音乐已停止");
        }
    }
    
    /**
     * 设置背景音乐音量
     * 
     * @param volume 音量级别 (0.0f - 1.0f)
     */
    public void setMusicVolume(float volume) {
        if (volume < 0.0f) {
            volume = 0.0f;
        } else if (volume > 1.0f) {
            volume = 1.0f;
        }
        this.musicVolume = volume;
        
        if (backgroundMusicClip != null && backgroundMusicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            try {
                FloatControl gainControl = 
                    (FloatControl) backgroundMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log10(volume) * 20.0);
                gainControl.setValue(dB);
            } catch (Exception e) {
                System.err.println("设置背景音乐音量失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取背景音乐音量
     * 
     * @return 当前背景音乐音量级别
     */
    public float getMusicVolume() {
        return musicVolume;
    }
    
    /**
     * 设置是否启用背景音乐
     * 
     * @param enabled 是否启用背景音乐
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        
        if (enabled) {
            resumeBackgroundMusic();
        } else {
            pauseBackgroundMusic();
        }
    }
    
    /**
     * 获取背景音乐是否启用
     * 
     * @return 背景音乐是否启用
     */
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
    
    /**
     * 设置是否启用音效
     * 
     * @param enabled 是否启用音效
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        
        if (!enabled) {
            // 停止所有正在播放的音效
            stopAllSounds();
        }
    }
    
    /**
     * 获取音效是否启用
     * 
     * @return 音效是否启用
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    /**
     * 设置音量级别
     * 
     * @param volume 音量级别 (0.0f - 1.0f)
     */
    public void setVolume(float volume) {
        if (volume < 0.0f) {
            volume = 0.0f;
        } else if (volume > 1.0f) {
            volume = 1.0f;
        }
        this.volume = volume;
    }
    
    /**
     * 获取当前音量级别
     * 
     * @return 当前音量级别
     */
    public float getVolume() {
        return volume;
    }
    
    /**
     * 停止所有音效播放
     */
    public void stopAllSounds() {
        for (Clip clip : soundClips.values()) {
            if (clip.isRunning()) {
                clip.stop();
            }
        }
    }
    
    /**
     * 释放所有音效资源
     */
    public void dispose() {
        // 停止背景音乐
        if (backgroundMusicClip != null) {
            backgroundMusicClip.stop();
            backgroundMusicClip.close();
        }
        
        // 释放音效
        for (Clip clip : soundClips.values()) {
            clip.close();
        }
        soundClips.clear();
        
        System.out.println("音频资源已释放");
    }
}
