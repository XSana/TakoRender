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

    /** 死亡粒子数据（子发射器用）: [x, y, z, velocityMag, ...] */
    private float[] deadParticleData;

    /** 死亡粒子数量 */
    private int deadParticleCount;

    public ParticleStateComponent() {
        this.randomSeed = (int) System.nanoTime();
    }

    public float getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(float systemTime) {
        this.systemTime = systemTime;
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

    public int getAliveCount() {
        return aliveCount;
    }

    public void setAliveCount(int aliveCount) {
        this.aliveCount = aliveCount;
    }

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

    public float getEmissionAccumulator() {
        return emissionAccumulator;
    }

    public void setEmissionAccumulator(float emissionAccumulator) {
        this.emissionAccumulator = emissionAccumulator;
    }

    public float getBurstAccumulator() {
        return burstAccumulator;
    }

    public void setBurstAccumulator(float burstAccumulator) {
        this.burstAccumulator = burstAccumulator;
    }

    public boolean isInitialBurstTriggered() {
        return initialBurstTriggered;
    }

    public void setInitialBurstTriggered(boolean initialBurstTriggered) {
        this.initialBurstTriggered = initialBurstTriggered;
    }

    public int getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(int randomSeed) {
        this.randomSeed = randomSeed;
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

    public float[] getDeadParticleData() {
        return deadParticleData;
    }

    public void setDeadParticleData(float[] deadParticleData) {
        this.deadParticleData = deadParticleData;
    }

    public int getDeadParticleCount() {
        return deadParticleCount;
    }

    public void setDeadParticleCount(int deadParticleCount) {
        this.deadParticleCount = deadParticleCount;
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
