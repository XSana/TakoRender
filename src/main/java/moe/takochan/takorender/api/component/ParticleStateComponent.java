package moe.takochan.takorender.api.component;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.ecs.Component;

/**
 * 粒子状态组件 - 纯数据
 *
 * <p>
 * 存储粒子系统的运行时状态。逻辑由 System 处理。
 * </p>
 */
@SideOnly(Side.CLIENT)
public class ParticleStateComponent extends Component {

    /** 系统运行时间 */
    private float systemTime;

    /** 是否暂停 */
    private boolean paused;

    /** 是否循环 */
    private boolean looping = true;

    /** 系统持续时间（非循环模式） */
    private float duration = 5.0f;

    /** 存活粒子数（从 GPU 读回） */
    private int aliveCount;

    /** 上一帧位置 X */
    private float lastPosX;

    /** 上一帧位置 Y */
    private float lastPosY;

    /** 上一帧位置 Z */
    private float lastPosZ;

    /** 发射累积器（用于平滑发射） */
    private float emissionAccumulator;

    /** Burst 累积器 */
    private float burstAccumulator;

    /** 是否已触发初始 Burst */
    private boolean initialBurstTriggered;

    /** 随机种子 */
    private int randomSeed;

    /** 是否已启动 */
    private boolean started;

    /** 是否已完成（非循环模式） */
    private boolean completed;

    public ParticleStateComponent() {
        this.randomSeed = (int) System.nanoTime();
    }

    // ==================== 时间状态 ====================

    public float getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(float systemTime) {
        this.systemTime = systemTime;
    }

    public void addTime(float deltaTime) {
        this.systemTime += deltaTime;
    }

    public boolean isPaused() {
        return paused;
    }

    public ParticleStateComponent setPaused(boolean paused) {
        this.paused = paused;
        return this;
    }

    public boolean isLooping() {
        return looping;
    }

    public ParticleStateComponent setLooping(boolean looping) {
        this.looping = looping;
        return this;
    }

    public float getDuration() {
        return duration;
    }

    public ParticleStateComponent setDuration(float duration) {
        this.duration = duration;
        return this;
    }

    // ==================== 粒子计数 ====================

    public int getAliveCount() {
        return aliveCount;
    }

    public void setAliveCount(int aliveCount) {
        this.aliveCount = aliveCount;
    }

    // ==================== 位置追踪 ====================

    public float getLastPosX() {
        return lastPosX;
    }

    public void setLastPosX(float lastPosX) {
        this.lastPosX = lastPosX;
    }

    public float getLastPosY() {
        return lastPosY;
    }

    public void setLastPosY(float lastPosY) {
        this.lastPosY = lastPosY;
    }

    public float getLastPosZ() {
        return lastPosZ;
    }

    public void setLastPosZ(float lastPosZ) {
        this.lastPosZ = lastPosZ;
    }

    public void setLastPosition(float x, float y, float z) {
        this.lastPosX = x;
        this.lastPosY = y;
        this.lastPosZ = z;
    }

    // ==================== 发射累积器 ====================

    public float getEmissionAccumulator() {
        return emissionAccumulator;
    }

    public void setEmissionAccumulator(float emissionAccumulator) {
        this.emissionAccumulator = emissionAccumulator;
    }

    public void addEmissionAccumulator(float delta) {
        this.emissionAccumulator += delta;
    }

    public float getBurstAccumulator() {
        return burstAccumulator;
    }

    public void setBurstAccumulator(float burstAccumulator) {
        this.burstAccumulator = burstAccumulator;
    }

    public void addBurstAccumulator(float delta) {
        this.burstAccumulator += delta;
    }

    public boolean isInitialBurstTriggered() {
        return initialBurstTriggered;
    }

    public void setInitialBurstTriggered(boolean initialBurstTriggered) {
        this.initialBurstTriggered = initialBurstTriggered;
    }

    // ==================== 随机/状态 ====================

    public int getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(int randomSeed) {
        this.randomSeed = randomSeed;
    }

    public void nextRandomSeed() {
        this.randomSeed++;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // ==================== 控制方法 ====================

    /**
     * 重置状态
     */
    public void reset() {
        this.systemTime = 0;
        this.emissionAccumulator = 0;
        this.burstAccumulator = 0;
        this.initialBurstTriggered = false;
        this.started = false;
        this.completed = false;
        this.aliveCount = 0;
    }

    /**
     * 暂停
     */
    public void pause() {
        this.paused = true;
    }

    /**
     * 恢复
     */
    public void resume() {
        this.paused = false;
    }

    /**
     * 停止
     */
    public void stop() {
        this.paused = true;
        this.completed = true;
    }

    @Override
    public String toString() {
        return String.format(
            "ParticleStateComponent[time=%.2f, alive=%d, paused=%b, looping=%b]",
            systemTime,
            aliveCount,
            paused,
            looping);
    }
}
