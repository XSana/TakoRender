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
        return 101;
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
            emitter.getVelocityOverLifetime(),
            emitter.getRotationOverLifetime(),
            maxDeadParticles);

        int aliveCount = gpuBuffer.readAtomicCounter();
        state.setAliveCount(aliveCount);
    }

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
