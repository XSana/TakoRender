package moe.takochan.takorender.core.particle;

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.api.particle.AnimationCurve;
import moe.takochan.takorender.api.particle.CollisionMode;
import moe.takochan.takorender.api.particle.CollisionResponse;
import moe.takochan.takorender.api.particle.ParticleEmitter;
import moe.takochan.takorender.api.particle.ParticleForce;
import moe.takochan.takorender.api.particle.RotationOverLifetime;
import moe.takochan.takorender.api.particle.VelocityOverLifetime;

/**
 * 粒子计算着色器调度器
 *
 * <p>
 * 管理粒子物理模拟的计算着色器。
 * </p>
 */
@SideOnly(Side.CLIENT)
public class ParticleCompute {

    /** 粒子更新着色器 */
    private ShaderProgram updateShader;

    /** 粒子发射着色器 */
    private ShaderProgram emitShader;

    /** 工作组大小 */
    private static final int WORK_GROUP_SIZE = 256;

    /** 最大力场数量 */
    private static final int MAX_FORCES = 16;

    /** 力场数据缓冲 (每个力场 12 个 float) */
    private final float[] forceData = new float[MAX_FORCES * 12];

    /** 是否已初始化 */
    private boolean initialized = false;

    /** 粒子 SSBO 绑定点 */
    public static final int PARTICLE_SSBO_BINDING = 0;

    /** 计数器绑定点 */
    public static final int COUNTER_BINDING = 1;

    /** 死亡粒子 SSBO 绑定点（用于子发射器） */
    public static final int DEAD_BUFFER_BINDING = 2;

    /** 死亡粒子计数器绑定点 */
    public static final int DEAD_COUNTER_BINDING = 3;

    /** 随机种子计数器 */
    private int randomSeedCounter = 0;

    public ParticleCompute() {}

    /**
     * 初始化（延迟加载着色器）
     *
     * @return true 初始化成功
     */
    public boolean initialize() {
        if (initialized) {
            return true;
        }

        if (!ShaderProgram.isComputeShaderSupported()) {
            TakoRenderMod.LOG.warn("ParticleCompute: Compute shader not supported");
            return false;
        }

        try {
            updateShader = ShaderProgram.createCompute("takorender", "shaders/particle/particle_update.comp");
            if (updateShader == null || !updateShader.isValid()) {
                TakoRenderMod.LOG.warn("ParticleCompute: Failed to create update shader, using fallback");
                updateShader = createFallbackUpdateShader();
            }

            emitShader = ShaderProgram.createCompute("takorender", "shaders/particle/particle_emit.comp");
            if (emitShader == null || !emitShader.isValid()) {
                TakoRenderMod.LOG.warn("ParticleCompute: Failed to create emit shader, using fallback");
                emitShader = createFallbackEmitShader();
            }

            if (updateShader == null || !updateShader.isValid() || emitShader == null || !emitShader.isValid()) {
                TakoRenderMod.LOG.error("ParticleCompute: Failed to initialize compute shaders");
                return false;
            }

            initialized = true;
            TakoRenderMod.LOG.info("ParticleCompute: Initialized successfully");
            return true;

        } catch (Exception e) {
            TakoRenderMod.LOG.error("ParticleCompute: Initialization failed", e);
            return false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 调度粒子更新
     */
    public void dispatchUpdate(ParticleBuffer buffer, float deltaTime, List<ParticleForce> forces,
        CollisionMode collisionMode, CollisionResponse collisionResponse, float bounciness, float bounceChance,
        float bounceSpread) {

        if (!initialized || updateShader == null) {
            return;
        }

        int particleCount = buffer.getMaxParticles();
        if (particleCount == 0) {
            return;
        }

        buffer.resetAtomicCounter(0);

        updateShader.use();

        randomSeedCounter++;

        setUniform("uDeltaTime", deltaTime);
        setUniform("uParticleCount", particleCount);
        setUniform("uCollisionMode", collisionMode.getId());
        setUniform("uCollisionResponse", collisionResponse.getId());
        setUniform("uBounciness", bounciness);
        setUniform("uBounceChance", bounceChance);
        setUniform("uBounceSpread", bounceSpread);
        setUniform("uRandomSeed", randomSeedCounter);
        setUniform("uCollisionPlane", 0.0f, 1.0f, 0.0f, 0.0f);

        int forceCount = Math.min(forces != null ? forces.size() : 0, MAX_FORCES);
        setUniform("uForceCount", forceCount);

        if (forceCount > 0) {
            for (int i = 0; i < forceCount; i++) {
                ParticleForce force = forces.get(i);
                float[] data = force.toFloatArray();
                System.arraycopy(data, 0, forceData, i * 12, 12);
            }
            setUniformArray("uForces", forceData, forceCount * 12);
        }

        buffer.bindToCompute(PARTICLE_SSBO_BINDING);
        buffer.bindAtomicCounter(COUNTER_BINDING);

        int workGroups = (particleCount + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE;

        updateShader.dispatch(workGroups, 1, 1);

        ShaderProgram.memoryBarrierSSBO();

        ShaderProgram.unbind();
    }

    /**
     * 调度粒子更新（简化版）
     */
    public void dispatchUpdate(ParticleBuffer buffer, float deltaTime, List<ParticleForce> forces,
        CollisionMode collisionMode, CollisionResponse collisionResponse, float bounciness) {
        dispatchUpdate(buffer, deltaTime, forces, collisionMode, collisionResponse, bounciness, 1.0f, 0.0f);
    }

    /**
     * 调度粒子更新（使用发射器参数）
     */
    public void dispatchUpdate(ParticleBuffer buffer, float deltaTime, ParticleEmitter emitter) {
        dispatchUpdateWithPlane(
            buffer,
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
            emitter.getCollisionPlaneD());
    }

    /**
     * 调度粒子更新（带碰撞平面参数）
     */
    public void dispatchUpdateWithPlane(ParticleBuffer buffer, float deltaTime, List<ParticleForce> forces,
        CollisionMode collisionMode, CollisionResponse collisionResponse, float bounciness, float bounceChance,
        float bounceSpread, float planeNX, float planeNY, float planeNZ, float planeD) {
        dispatchUpdateWithCurves(
            buffer,
            deltaTime,
            forces,
            collisionMode,
            collisionResponse,
            bounciness,
            bounceChance,
            bounceSpread,
            planeNX,
            planeNY,
            planeNZ,
            planeD,
            0,
            0,
            0,
            1,
            -1,
            -1,
            -1,
            1,
            1,
            1,
            null,
            null,
            0);
    }

    /**
     * 调度粒子更新（完整版：带曲线和子发射器支持）
     *
     * @param buffer            粒子缓冲区
     * @param deltaTime         时间增量
     * @param forces            力场列表
     * @param collisionMode     碰撞模式
     * @param collisionResponse 碰撞响应
     * @param bounciness        弹性
     * @param bounceChance      弹跳概率
     * @param bounceSpread      弹跳扩散角度
     * @param planeNX           碰撞平面法向 X
     * @param planeNY           碰撞平面法向 Y
     * @param planeNZ           碰撞平面法向 Z
     * @param planeD            碰撞平面距离
     * @param sphereCX          碰撞球心 X
     * @param sphereCY          碰撞球心 Y
     * @param sphereCZ          碰撞球心 Z
     * @param sphereR           碰撞球半径
     * @param boxMinX           碰撞盒最小 X
     * @param boxMinY           碰撞盒最小 Y
     * @param boxMinZ           碰撞盒最小 Z
     * @param boxMaxX           碰撞盒最大 X
     * @param boxMaxY           碰撞盒最大 Y
     * @param boxMaxZ           碰撞盒最大 Z
     * @param velocityOverLife  速度曲线（可为 null）
     * @param rotationOverLife  旋转曲线（可为 null）
     * @param maxDeadParticles  最大死亡粒子追踪数（0=禁用子发射器）
     */
    public void dispatchUpdateWithCurves(ParticleBuffer buffer, float deltaTime, List<ParticleForce> forces,
        CollisionMode collisionMode, CollisionResponse collisionResponse, float bounciness, float bounceChance,
        float bounceSpread, float planeNX, float planeNY, float planeNZ, float planeD, float sphereCX, float sphereCY,
        float sphereCZ, float sphereR, float boxMinX, float boxMinY, float boxMinZ, float boxMaxX, float boxMaxY,
        float boxMaxZ, VelocityOverLifetime velocityOverLife, RotationOverLifetime rotationOverLife,
        int maxDeadParticles) {

        if (!initialized || updateShader == null) {
            return;
        }

        int particleCount = buffer.getMaxParticles();
        if (particleCount == 0) {
            return;
        }

        buffer.resetAtomicCounter(0);
        updateShader.use();
        randomSeedCounter++;

        setUniform("uDeltaTime", deltaTime);
        setUniform("uParticleCount", particleCount);
        setUniform("uCollisionMode", collisionMode.getId());
        setUniform("uCollisionResponse", collisionResponse.getId());
        setUniform("uBounciness", bounciness);
        setUniform("uBounceChance", bounceChance);
        setUniform("uBounceSpread", bounceSpread);
        setUniform("uRandomSeed", randomSeedCounter);
        setUniform("uCollisionPlane", planeNX, planeNY, planeNZ, planeD);
        setUniform("uCollisionSphereCenter", sphereCX, sphereCY, sphereCZ);
        setUniform("uCollisionSphereRadius", sphereR);
        setUniform("uCollisionBoxMin", boxMinX, boxMinY, boxMinZ);
        setUniform("uCollisionBoxMax", boxMaxX, boxMaxY, boxMaxZ);
        setUniform("uMaxDeadParticles", maxDeadParticles);

        int forceCount = Math.min(forces != null ? forces.size() : 0, MAX_FORCES);
        setUniform("uForceCount", forceCount);

        if (forceCount > 0) {
            for (int i = 0; i < forceCount; i++) {
                ParticleForce force = forces.get(i);
                float[] data = force.toFloatArray();
                System.arraycopy(data, 0, forceData, i * 12, 12);
            }
            setUniformArray("uForces", forceData, forceCount * 12);
        }

        // 速度曲线
        if (velocityOverLife != null) {
            setUniform("uVelocityOverLifetimeEnabled", 1);
            setUniform("uVelocitySeparateAxes", velocityOverLife.isSeparateAxes() ? 1 : 0);

            if (velocityOverLife.isSeparateAxes()) {
                // 分离轴模式：需要交错格式 [t0, x0, y0, z0, t1, x1, y1, z1, ...]
                AnimationCurve cx = velocityOverLife.getCurveX();
                AnimationCurve cy = velocityOverLife.getCurveY();
                AnimationCurve cz = velocityOverLife.getCurveZ();
                int keyCount = cx.getKeyframeCount();
                setUniform("uVelocityOverLifetimeKeyCount", keyCount);

                if (keyCount > 0) {
                    float[] curveData = new float[keyCount * 4];
                    float[] arrX = cx.toArray();
                    float[] arrY = cy.toArray();
                    float[] arrZ = cz.toArray();
                    for (int i = 0; i < keyCount; i++) {
                        curveData[i * 4] = arrX[i * 2]; // time
                        curveData[i * 4 + 1] = arrX[i * 2 + 1]; // x
                        curveData[i * 4 + 2] = arrY[i * 2 + 1]; // y
                        curveData[i * 4 + 3] = arrZ[i * 2 + 1]; // z
                    }
                    setUniformArray("uVelocityOverLifetimeCurve", curveData, keyCount * 4);
                }
            } else {
                // 统一模式：[t0, v0, t1, v1, ...]
                AnimationCurve curve = velocityOverLife.getUniformCurve();
                int keyCount = curve.getKeyframeCount();
                setUniform("uVelocityOverLifetimeKeyCount", keyCount);

                if (keyCount > 0) {
                    float[] curveData = curve.toArray();
                    setUniformArray("uVelocityOverLifetimeCurve", curveData, keyCount * 2);
                }
            }
        } else {
            setUniform("uVelocityOverLifetimeEnabled", 0);
            setUniform("uVelocityOverLifetimeKeyCount", 0);
        }

        // 旋转曲线（仅使用统一模式，Billboard 粒子只需 Z 轴旋转）
        if (rotationOverLife != null) {
            setUniform("uRotationOverLifetimeEnabled", 1);
            AnimationCurve curve = rotationOverLife.getUniformCurve();
            int keyCount = curve.getKeyframeCount();
            setUniform("uRotationOverLifetimeKeyCount", keyCount);

            if (keyCount > 0) {
                float[] curveData = curve.toArray();
                setUniformArray("uRotationOverLifetimeCurve", curveData, keyCount * 2);
            }
        } else {
            setUniform("uRotationOverLifetimeEnabled", 0);
            setUniform("uRotationOverLifetimeKeyCount", 0);
        }

        buffer.bindToCompute(PARTICLE_SSBO_BINDING);
        buffer.bindAtomicCounter(COUNTER_BINDING);

        if (maxDeadParticles > 0 && buffer.getMaxDeadParticles() > 0) {
            buffer.resetDeadCounter();
            buffer.bindDeadBuffers(DEAD_BUFFER_BINDING, DEAD_COUNTER_BINDING);
        }

        int workGroups = (particleCount + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE;
        updateShader.dispatch(workGroups, 1, 1);
        ShaderProgram.memoryBarrierSSBO();
        ShaderProgram.unbind();
    }

    /**
     * 发射新粒子（使用 compute shader）
     */
    public void dispatchEmit(ParticleBuffer buffer, ParticleEmitter emitter, int count, float baseTime) {
        if (!initialized || emitShader == null || count <= 0) {
            return;
        }

        emitShader.use();

        setUniform("uEmitCount", count);
        setUniform("uMaxParticles", buffer.getMaxParticles());
        setUniform("uBaseTime", baseTime);
        setUniform("uEmitterPos", emitter.getPositionX(), emitter.getPositionY(), emitter.getPositionZ());
        setUniform(
            "uShapeType",
            emitter.getShape()
                .getId());
        setUniform("uShapeParam1", emitter.getShapeParam1());
        setUniform("uShapeParam2", emitter.getShapeParam2());
        setUniform("uShapeParam3", emitter.getShapeParam3());
        setUniform("uLifetimeMin", emitter.getLifetimeMin());
        setUniform("uLifetimeMax", emitter.getLifetimeMax());
        setUniform("uVelocity", emitter.getVelocityX(), emitter.getVelocityY(), emitter.getVelocityZ());
        setUniform("uSpeed", emitter.getSpeed());
        setUniform("uSizeMin", emitter.getSizeMin());
        setUniform("uSizeMax", emitter.getSizeMax());
        setUniform("uColor", emitter.getColorR(), emitter.getColorG(), emitter.getColorB(), emitter.getColorA());
        setUniform("uParticleType", emitter.getParticleType());
        setUniform("uRotationMin", 0.0f);
        setUniform("uRotationMax", 0.0f);
        setUniform("uAngularVelMin", 0.0f);
        setUniform("uAngularVelMax", 0.0f);

        buffer.bindToCompute(PARTICLE_SSBO_BINDING);
        buffer.bindAtomicCounter(COUNTER_BINDING);

        int workGroups = (count + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE;

        emitShader.dispatch(workGroups, 1, 1);

        ShaderProgram.memoryBarrierSSBO();

        ShaderProgram.unbind();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (updateShader != null) {
            updateShader.close();
            updateShader = null;
        }
        if (emitShader != null) {
            emitShader.close();
            emitShader = null;
        }
        initialized = false;
    }

    private void setUniform(String name, float value) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private void setUniform(String name, int value) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    private void setUniform(String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            GL20.glUniform3f(location, x, y, z);
        }
    }

    private void setUniform(String name, float x, float y, float z, float w) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            GL20.glUniform4f(location, x, y, z, w);
        }
    }

    private void setUniformArray(String name, float[] data, int count) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            FloatBuffer buffer = BufferUtils.createFloatBuffer(count);
            buffer.put(data, 0, count);
            buffer.flip();
            GL20.glUniform1(location, buffer);
        }
    }

    private int getCurrentProgramId() {
        return GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    }

    private ShaderProgram createFallbackUpdateShader() {
        String source = "#version 430 core\n" + "\n"
            + "layout(local_size_x = 256) in;\n"
            + "\n"
            + "struct Particle {\n"
            + "    vec4 position;\n"
            + "    vec4 velocity;\n"
            + "    vec4 color;\n"
            + "    vec4 params;\n"
            + "};\n"
            + "\n"
            + "layout(std430, binding = 0) buffer ParticleBuffer {\n"
            + "    Particle particles[];\n"
            + "};\n"
            + "\n"
            + "layout(binding = 1) uniform atomic_uint aliveCount;\n"
            + "\n"
            + "uniform float uDeltaTime;\n"
            + "uniform int uParticleCount;\n"
            + "\n"
            + "void main() {\n"
            + "    uint idx = gl_GlobalInvocationID.x;\n"
            + "    if (idx >= uParticleCount) return;\n"
            + "    \n"
            + "    Particle p = particles[idx];\n"
            + "    if (p.position.w <= 0.0) return;\n"
            + "    \n"
            + "    p.position.w -= uDeltaTime;\n"
            + "    if (p.position.w <= 0.0) {\n"
            + "        p.position.w = 0.0;\n"
            + "        particles[idx] = p;\n"
            + "        return;\n"
            + "    }\n"
            + "    \n"
            + "    p.velocity.y -= 9.8 * uDeltaTime;\n"
            + "    p.position.xyz += p.velocity.xyz * uDeltaTime;\n"
            + "    p.params.y += p.params.w * uDeltaTime;\n"
            + "    atomicCounterIncrement(aliveCount);\n"
            + "    particles[idx] = p;\n"
            + "}\n";

        return ShaderProgram.createComputeFromSource(source);
    }

    private ShaderProgram createFallbackEmitShader() {
        String source = "#version 430 core\n" + "\n"
            + "layout(local_size_x = 256) in;\n"
            + "\n"
            + "struct Particle {\n"
            + "    vec4 position;\n"
            + "    vec4 velocity;\n"
            + "    vec4 color;\n"
            + "    vec4 params;\n"
            + "};\n"
            + "\n"
            + "layout(std430, binding = 0) buffer ParticleBuffer {\n"
            + "    Particle particles[];\n"
            + "};\n"
            + "\n"
            + "uniform int uEmitCount;\n"
            + "uniform vec3 uEmitterPos;\n"
            + "uniform float uLifetimeMin;\n"
            + "uniform float uLifetimeMax;\n"
            + "uniform vec3 uVelocity;\n"
            + "uniform float uSizeMin;\n"
            + "uniform float uSizeMax;\n"
            + "uniform vec4 uColor;\n"
            + "uniform int uParticleType;\n"
            + "uniform float uBaseTime;\n"
            + "\n"
            + "uint hash(uint x) {\n"
            + "    x += (x << 10u);\n"
            + "    x ^= (x >> 6u);\n"
            + "    x += (x << 3u);\n"
            + "    x ^= (x >> 11u);\n"
            + "    x += (x << 15u);\n"
            + "    return x;\n"
            + "}\n"
            + "\n"
            + "float random(uint seed) {\n"
            + "    return float(hash(seed)) / 4294967295.0;\n"
            + "}\n"
            + "\n"
            + "void main() {\n"
            + "    uint idx = gl_GlobalInvocationID.x;\n"
            + "    if (idx >= uEmitCount) return;\n"
            + "    \n"
            + "    uint slot = idx;\n"
            + "    uint seed = idx + uint(uBaseTime * 1000.0);\n"
            + "    \n"
            + "    Particle p;\n"
            + "    p.position = vec4(uEmitterPos, mix(uLifetimeMin, uLifetimeMax, random(seed)));\n"
            + "    p.velocity = vec4(uVelocity, p.position.w);\n"
            + "    p.color = uColor;\n"
            + "    p.params = vec4(mix(uSizeMin, uSizeMax, random(seed + 1u)), 0.0, float(uParticleType), 0.0);\n"
            + "    \n"
            + "    particles[slot] = p;\n"
            + "}\n";

        return ShaderProgram.createComputeFromSource(source);
    }

    public static int getWorkGroupSize() {
        return WORK_GROUP_SIZE;
    }
}
