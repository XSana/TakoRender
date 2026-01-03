package moe.takochan.takorender.api.component;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.Disposable;
import moe.takochan.takorender.core.particle.ParticleBuffer;
import moe.takochan.takorender.core.particle.ParticleCPU;

/**
 * 粒子缓冲区组件 - 纯数据
 *
 * <p>
 * 存储粒子 GPU/CPU 缓冲区配置和状态。逻辑由 System 处理。
 * </p>
 *
 * <p>
 * <b>兼容性设计</b>:
 * </p>
 * <ul>
 * <li>GPU 模式: 使用 SSBO + Compute Shader (OpenGL 4.3+)</li>
 * <li>CPU 模式: 使用 ParticleCPU 回退实现 (macOS 等不支持 Compute Shader 的系统)</li>
 * </ul>
 *
 * <p>
 * ParticleEmitSystem 会在初始化时检测 SSBO 支持情况，自动选择合适的模式。
 * 当 useCPUFallback=true 时，物理更新和渲染都使用 CPU 实现。
 * </p>
 */
@SideOnly(Side.CLIENT)
public class ParticleBufferComponent extends Component implements Disposable {

    /** 最大粒子数量 */
    private int maxParticles;

    /** GPU 缓冲区（SSBO） */
    private ParticleBuffer gpuBuffer;

    /** CPU 回退缓冲区 */
    private ParticleCPU cpuBuffer;

    /** 死亡粒子缓冲区 ID（用于子发射器） */
    private int deadBufferId;

    /** 最大死亡粒子追踪数 */
    private int maxDeadParticles;

    /** 是否使用 CPU 回退模式 */
    private boolean useCPUFallback;

    /** 是否已初始化 */
    private boolean initialized;

    /**
     * 创建粒子缓冲区组件
     *
     * @param maxParticles 最大粒子数
     */
    public ParticleBufferComponent(int maxParticles) {
        this.maxParticles = maxParticles;
        this.maxDeadParticles = 0;
        this.useCPUFallback = false;
        this.initialized = false;
    }

    /**
     * 创建粒子缓冲区组件（带子发射器支持）
     *
     * @param maxParticles     最大粒子数
     * @param maxDeadParticles 最大死亡粒子追踪数
     */
    public ParticleBufferComponent(int maxParticles, int maxDeadParticles) {
        this.maxParticles = maxParticles;
        this.maxDeadParticles = maxDeadParticles;
        this.useCPUFallback = false;
        this.initialized = false;
    }

    public int getMaxParticles() {
        return maxParticles;
    }

    public ParticleBufferComponent setMaxParticles(int maxParticles) {
        this.maxParticles = maxParticles;
        return this;
    }

    public ParticleBuffer getGpuBuffer() {
        return gpuBuffer;
    }

    public void setGpuBuffer(ParticleBuffer gpuBuffer) {
        this.gpuBuffer = gpuBuffer;
    }

    public ParticleCPU getCpuBuffer() {
        return cpuBuffer;
    }

    public void setCpuBuffer(ParticleCPU cpuBuffer) {
        this.cpuBuffer = cpuBuffer;
    }

    public int getDeadBufferId() {
        return deadBufferId;
    }

    public void setDeadBufferId(int deadBufferId) {
        this.deadBufferId = deadBufferId;
    }

    public int getMaxDeadParticles() {
        return maxDeadParticles;
    }

    public ParticleBufferComponent setMaxDeadParticles(int maxDeadParticles) {
        this.maxDeadParticles = maxDeadParticles;
        return this;
    }

    public boolean isUseCPUFallback() {
        return useCPUFallback;
    }

    public void setUseCPUFallback(boolean useCPUFallback) {
        this.useCPUFallback = useCPUFallback;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public String toString() {
        return String.format(
            "ParticleBufferComponent[maxParticles=%d, cpu=%b, initialized=%b]",
            maxParticles,
            useCPUFallback,
            initialized);
    }

    @Override
    public void dispose() {
        if (gpuBuffer != null) {
            gpuBuffer.close();
            gpuBuffer = null;
        }
        cpuBuffer = null;
        initialized = false;
    }
}
