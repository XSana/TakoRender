package moe.takochan.takorender.api.system;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.component.ParticleBufferComponent;
import moe.takochan.takorender.api.component.ParticleEmitterComponent;
import moe.takochan.takorender.api.component.ParticleStateComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.core.particle.ParticleBuffer;
import moe.takochan.takorender.core.particle.ParticleCPU;
import moe.takochan.takorender.core.particle.ParticleCompute;

/**
 * 粒子物理系统
 *
 * <p>
 * 负责调度 Compute Shader 或 CPU 回退来更新粒子物理状态。
 * 处理力场、碰撞、速度/旋转曲线。
 * 在 UPDATE 阶段执行，优先级低于 ParticleEmitSystem。
 * </p>
 *
 * <p>
 * <b>兼容性说明</b>:
 * </p>
 * <ul>
 * <li>GPU 模式: 使用 Compute Shader 进行并行物理计算 (OpenGL 4.3+)</li>
 * <li>CPU 模式: 使用 ParticleCPU 进行串行物理计算 (macOS 等)</li>
 * <li>CPU 模式支持简化的力场和曲线，但不支持碰撞检测和子发射器</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent({ ParticleEmitterComponent.class, ParticleBufferComponent.class, ParticleStateComponent.class,
    TransformComponent.class })
public class ParticlePhysicsSystem extends GameSystem {

    /** Compute Shader 调度器（延迟初始化） */
    private ParticleCompute compute;

    /** 是否已初始化 */
    private boolean initialized = false;

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        // 在 ParticleEmitSystem (200) 之后
        return 300;
    }

    @Override
    public void onInit() {
        TakoRenderMod.LOG.info("ParticlePhysicsSystem: Initialized");
    }

    @Override
    public void update(float deltaTime) {
        for (Entity entity : getRequiredEntities()) {
            processEntity(entity, deltaTime);
        }
    }

    private void processEntity(Entity entity, float deltaTime) {
        ParticleEmitterComponent emitter = entity.getComponent(ParticleEmitterComponent.class)
            .orElse(null);
        ParticleBufferComponent buffer = entity.getComponent(ParticleBufferComponent.class)
            .orElse(null);
        ParticleStateComponent state = entity.getComponent(ParticleStateComponent.class)
            .orElse(null);

        if (emitter == null || buffer == null || state == null) {
            return;
        }

        if (state.isPaused() || !buffer.isInitialized()) {
            return;
        }

        if (buffer.isUseCPUFallback()) {
            updateCPU(emitter, buffer, state, deltaTime);
        } else {
            updateGPU(emitter, buffer, state, deltaTime);
        }
    }

    private void updateGPU(ParticleEmitterComponent emitter, ParticleBufferComponent buffer,
        ParticleStateComponent state, float deltaTime) {

        ParticleBuffer gpuBuffer = buffer.getGpuBuffer();
        if (gpuBuffer == null || !gpuBuffer.isValid()) {
            return;
        }

        ensureComputeInitialized();
        if (compute == null || !compute.isInitialized()) {
            return;
        }

        // 计算子发射器追踪数
        int maxDeadParticles = buffer.getMaxDeadParticles();

        compute.dispatchUpdateWithCurves(
            gpuBuffer,
            deltaTime,
            emitter.getForces(),
            emitter.getCollisionMode(),
            emitter.getCollisionResponse(),
            emitter.getBounciness(),
            emitter.getBounceChance(),
            emitter.getBounceSpread(),
            emitter.getCollisionPlaneNX(),
            emitter.getCollisionPlaneNY(),
            emitter.getCollisionPlaneNZ(),
            emitter.getCollisionPlaneD(),
            emitter.getCollisionSphereCenterX(),
            emitter.getCollisionSphereCenterY(),
            emitter.getCollisionSphereCenterZ(),
            emitter.getCollisionSphereRadius(),
            emitter.getCollisionBoxMinX(),
            emitter.getCollisionBoxMinY(),
            emitter.getCollisionBoxMinZ(),
            emitter.getCollisionBoxMaxX(),
            emitter.getCollisionBoxMaxY(),
            emitter.getCollisionBoxMaxZ(),
            emitter.getVelocityOverLifetime(),
            emitter.getRotationOverLifetime(),
            maxDeadParticles);

        int aliveCount = gpuBuffer.readAtomicCounter();
        state.setAliveCount(aliveCount);

        if (gpuBuffer.getMaxDeadParticles() > 0) {
            int deadCount = gpuBuffer.readDeadCounter();
            if (deadCount > 0) {
                float[] deadData = gpuBuffer.readDeadParticles(deadCount);
                state.setDeadParticleData(deadData);
                state.setDeadParticleCount(deadCount);
            } else {
                state.setDeadParticleCount(0);
            }
        }
    }

    /**
     * CPU 物理更新（回退模式）
     *
     * <p>
     * 支持: 重力、风力、阻力、吸引/排斥、湍流、速度曲线、旋转曲线。
     * 不支持: 碰撞检测、子发射器触发。
     * </p>
     */
    private void updateCPU(ParticleEmitterComponent emitter, ParticleBufferComponent buffer,
        ParticleStateComponent state, float deltaTime) {

        ParticleCPU cpuBuffer = buffer.getCpuBuffer();
        if (cpuBuffer == null) {
            return;
        }

        cpuBuffer.setVelocityOverLifetime(emitter.getVelocityOverLifetime());
        cpuBuffer.setRotationOverLifetime(emitter.getRotationOverLifetime());

        cpuBuffer.update(deltaTime, emitter.getForces());

        state.setAliveCount(cpuBuffer.getAliveCount());
    }

    private void ensureComputeInitialized() {
        if (initialized) {
            return;
        }

        try {
            compute = new ParticleCompute();
            if (!compute.initialize()) {
                TakoRenderMod.LOG.warn("ParticlePhysicsSystem: Failed to initialize compute shader");
                compute = null;
            }
        } catch (Exception e) {
            TakoRenderMod.LOG.error("ParticlePhysicsSystem: Error initializing compute shader", e);
            compute = null;
        }

        initialized = true;
    }

    @Override
    public void onDestroy() {
        if (compute != null) {
            compute.cleanup();
            compute = null;
        }
        initialized = false;
        TakoRenderMod.LOG.info("ParticlePhysicsSystem: Destroyed");
    }
}
