package moe.takochan.takorender.api.system;

import java.util.Random;

import org.joml.Vector3f;

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
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.api.particle.EmitterShape;
import moe.takochan.takorender.api.particle.ParticleEmitter;
import moe.takochan.takorender.core.particle.ParticleBuffer;
import moe.takochan.takorender.core.particle.ParticleCPU;

/**
 * 粒子发射系统
 *
 * <p>
 * 负责计算发射数量、生成粒子数据、上传到 GPU 或 CPU 缓冲区。
 * 在 UPDATE 阶段执行。
 * </p>
 *
 * <p>
 * <b>兼容性说明</b>:
 * 初始化时通过 ShaderProgram.isSSBOSupported() 检测 GPU 能力。
 * SSBO 需要 OpenGL 4.3+，但 macOS 最高只支持 OpenGL 4.1。
 * 不支持时自动切换到 CPU 回退模式 (ParticleCPU)。
 * </p>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent({ ParticleEmitterComponent.class, ParticleBufferComponent.class, ParticleStateComponent.class,
    TransformComponent.class })
public class ParticleEmitSystem extends GameSystem {

    private final Random random = new Random();

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        // 在 CameraSystem (100) 之后
        return 200;
    }

    @Override
    public void onInit() {
        TakoRenderMod.LOG.info("ParticleEmitSystem: Initialized");
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
        TransformComponent transform = entity.getComponent(TransformComponent.class)
            .orElse(null);

        if (emitter == null || buffer == null || state == null || transform == null) {
            return;
        }

        if (state.isPaused() || state.isCompleted()) {
            return;
        }

        if (!buffer.isInitialized()) {
            initializeBuffer(buffer);
        }

        if (!emitter.isEmitting()) {
            return;
        }

        state.setSystemTime(state.getSystemTime() + deltaTime);

        if (!state.isLooping() && state.getSystemTime() >= state.getDuration()) {
            state.setCompleted(true);
            return;
        }

        int emitCount = calculateEmitCount(emitter, state, deltaTime);
        if (emitCount <= 0) {
            return;
        }

        Vector3f position = transform.getPosition();
        emitParticles(emitter, buffer, state, position, emitCount);

        processSubEmitters(emitter, buffer, state);
    }

    private void processSubEmitters(ParticleEmitterComponent emitter, ParticleBufferComponent buffer,
        ParticleStateComponent state) {

        if (emitter.getSubEmitters()
            .isEmpty()) {
            return;
        }

        int deadCount = state.getDeadParticleCount();
        float[] deadData = state.getDeadParticleData();

        if (deadCount <= 0 || deadData == null) {
            return;
        }

        for (ParticleEmitter.SubEmitterEntry entry : emitter.getSubEmitters()) {
            if (entry.trigger != ParticleEmitter.SubEmitterTrigger.DEATH) {
                continue;
            }

            for (int i = 0; i < deadCount; i++) {
                int base = i * 4;
                float x = deadData[base];
                float y = deadData[base + 1];
                float z = deadData[base + 2];
                float parentVelMag = deadData[base + 3];

                emitSubEmitterParticles(entry, buffer, state, x, y, z, parentVelMag);
            }
        }

        state.setDeadParticleCount(0);
    }

    private void emitSubEmitterParticles(ParticleEmitter.SubEmitterEntry entry, ParticleBufferComponent buffer,
        ParticleStateComponent state, float x, float y, float z, float parentVelMag) {

        ParticleEmitter subEmitter = entry.emitter;
        int count = entry.emitCount;
        float inheritVelocity = entry.inheritVelocity;

        float[] particles = new float[count * ParticleBuffer.PARTICLE_SIZE_FLOATS];
        Vector3f position = new Vector3f(x, y, z);

        for (int i = 0; i < count; i++) {
            int offset = i * ParticleBuffer.PARTICLE_SIZE_FLOATS;
            generateSubEmitterParticle(subEmitter, state, position, particles, offset, parentVelMag, inheritVelocity);
        }

        if (buffer.isUseCPUFallback()) {
            ParticleCPU cpuBuffer = buffer.getCpuBuffer();
            if (cpuBuffer != null) {
                for (int i = 0; i < count; i++) {
                    int base = i * ParticleBuffer.PARTICLE_SIZE_FLOATS;
                    cpuBuffer.emit(
                        particles[base],
                        particles[base + 1],
                        particles[base + 2],
                        particles[base + 4],
                        particles[base + 5],
                        particles[base + 6],
                        particles[base + 3],
                        particles[base + 12],
                        particles[base + 8],
                        particles[base + 9],
                        particles[base + 10],
                        particles[base + 11],
                        particles[base + 13],
                        particles[base + 15]);
                }
            }
        } else {
            ParticleBuffer gpuBuffer = buffer.getGpuBuffer();
            if (gpuBuffer != null && gpuBuffer.isValid()) {
                gpuBuffer.uploadParticles(particles, count);
            }
        }
    }

    /**
     * 生成子发射器粒子（支持速度继承）
     *
     * @param parentVelMag    父粒子速度大小
     * @param inheritVelocity 速度继承比例 (0-1)
     */
    private void generateSubEmitterParticle(ParticleEmitter subEmitter, ParticleStateComponent state, Vector3f position,
        float[] particles, int offset, float parentVelMag, float inheritVelocity) {

        float lifetime = randomRange(subEmitter.getLifetimeMin(), subEmitter.getLifetimeMax());
        particles[offset] = position.x;
        particles[offset + 1] = position.y;
        particles[offset + 2] = position.z;
        particles[offset + 3] = lifetime;

        float speed = subEmitter.getSpeed();

        float vx = (random.nextFloat() * 2 - 1) * speed;
        float vy = (random.nextFloat() * 2 - 1) * speed;
        float vz = (random.nextFloat() * 2 - 1) * speed;
        vx += subEmitter.getVelocityX();
        vy += subEmitter.getVelocityY();
        vz += subEmitter.getVelocityZ();

        if (inheritVelocity > 0 && parentVelMag > 0) {
            float inheritedSpeed = parentVelMag * inheritVelocity;
            float dirX = random.nextFloat() * 2 - 1;
            float dirY = random.nextFloat() * 2 - 1;
            float dirZ = random.nextFloat() * 2 - 1;
            float len = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (len > 0.001f) {
                dirX /= len;
                dirY /= len;
                dirZ /= len;
            }
            vx += dirX * inheritedSpeed;
            vy += dirY * inheritedSpeed;
            vz += dirZ * inheritedSpeed;
        }

        particles[offset + 4] = vx;
        particles[offset + 5] = vy;
        particles[offset + 6] = vz;
        particles[offset + 7] = lifetime;

        particles[offset + 8] = subEmitter.getColorR();
        particles[offset + 9] = subEmitter.getColorG();
        particles[offset + 10] = subEmitter.getColorB();
        particles[offset + 11] = subEmitter.getColorA();

        particles[offset + 12] = randomRange(subEmitter.getSizeMin(), subEmitter.getSizeMax());
        particles[offset + 13] = 0;
        particles[offset + 14] = 0;
        particles[offset + 15] = 0;
    }

    /**
     * 初始化粒子缓冲区
     *
     * <p>
     * 检测 SSBO 支持并选择 GPU 或 CPU 模式。
     * OpenGL 4.3+ 支持 SSBO，低于此版本使用 CPU 回退。
     * </p>
     */
    private void initializeBuffer(ParticleBufferComponent buffer) {
        boolean ssboSupported = ShaderProgram.isSSBOSupported();

        if (ssboSupported) {
            int maxDeadParticles = buffer.getMaxDeadParticles();
            ParticleBuffer gpuBuffer = new ParticleBuffer(buffer.getMaxParticles(), maxDeadParticles);
            gpuBuffer.initialize();
            buffer.setGpuBuffer(gpuBuffer);
            buffer.setUseCPUFallback(false);
        } else {
            ParticleCPU cpuBuffer = new ParticleCPU(buffer.getMaxParticles());
            buffer.setCpuBuffer(cpuBuffer);
            buffer.setUseCPUFallback(true);
            TakoRenderMod.LOG.info("ParticleEmitSystem: Using CPU fallback (SSBO not supported)");
        }

        buffer.setInitialized(true);
    }

    private int calculateEmitCount(ParticleEmitterComponent emitter, ParticleStateComponent state, float deltaTime) {
        int count = 0;

        if (emitter.getBurstCount() > 0) {
            if (!state.isInitialBurstTriggered()) {
                count += emitter.getBurstCount();
                state.setInitialBurstTriggered(true);
                state.setBurstAccumulator(0);
            } else if (emitter.getBurstInterval() > 0) {
                state.setBurstAccumulator(state.getBurstAccumulator() + deltaTime);
                while (state.getBurstAccumulator() >= emitter.getBurstInterval()) {
                    state.setBurstAccumulator(state.getBurstAccumulator() - emitter.getBurstInterval());
                    count += emitter.getBurstCount();
                }
            }
        }

        if (emitter.getEmissionRate() > 0) {
            state.setEmissionAccumulator(state.getEmissionAccumulator() + deltaTime * emitter.getEmissionRate());
            int rateCount = (int) state.getEmissionAccumulator();
            state.setEmissionAccumulator(state.getEmissionAccumulator() - rateCount);
            count += rateCount;
        }

        return count;
    }

    private void emitParticles(ParticleEmitterComponent emitter, ParticleBufferComponent buffer,
        ParticleStateComponent state, Vector3f basePosition, int count) {

        float[] particles = new float[count * ParticleBuffer.PARTICLE_SIZE_FLOATS];

        for (int i = 0; i < count; i++) {
            generateParticle(emitter, state, basePosition, particles, i * ParticleBuffer.PARTICLE_SIZE_FLOATS);
        }

        if (buffer.isUseCPUFallback()) {
            ParticleCPU cpuBuffer = buffer.getCpuBuffer();
            if (cpuBuffer != null) {
                for (int i = 0; i < count; i++) {
                    int base = i * ParticleBuffer.PARTICLE_SIZE_FLOATS;
                    cpuBuffer.emit(
                        particles[base],
                        particles[base + 1],
                        particles[base + 2],
                        particles[base + 4],
                        particles[base + 5],
                        particles[base + 6],
                        particles[base + 3],
                        particles[base + 12],
                        particles[base + 8],
                        particles[base + 9],
                        particles[base + 10],
                        particles[base + 11],
                        particles[base + 13],
                        particles[base + 15]);
                }
            }
        } else {
            ParticleBuffer gpuBuffer = buffer.getGpuBuffer();
            if (gpuBuffer != null && gpuBuffer.isValid()) {
                gpuBuffer.uploadParticles(particles, count);
            }
        }
    }

    private void generateParticle(ParticleEmitterComponent emitter, ParticleStateComponent state, Vector3f basePosition,
        float[] particles, int offset) {

        float[] localPos = generatePosition(emitter);
        particles[offset] = localPos[0] + basePosition.x;
        particles[offset + 1] = localPos[1] + basePosition.y;
        particles[offset + 2] = localPos[2] + basePosition.z;

        float lifetime = randomRange(emitter.getLifetimeMin(), emitter.getLifetimeMax());
        particles[offset + 3] = lifetime;

        float[] velocity = generateVelocity(emitter, localPos, basePosition);
        particles[offset + 4] = velocity[0];
        particles[offset + 5] = velocity[1];
        particles[offset + 6] = velocity[2];
        particles[offset + 7] = lifetime;

        particles[offset + 8] = emitter.getColorR();
        particles[offset + 9] = emitter.getColorG();
        particles[offset + 10] = emitter.getColorB();
        particles[offset + 11] = emitter.getColorA();

        particles[offset + 12] = randomRange(emitter.getSizeMin(), emitter.getSizeMax());
        particles[offset + 13] = randomRange(emitter.getRotationMin(), emitter.getRotationMax());
        particles[offset + 14] = 0;
        particles[offset + 15] = randomRange(emitter.getAngularVelocityMin(), emitter.getAngularVelocityMax());
    }

    private float[] generatePosition(ParticleEmitterComponent emitter) {
        EmitterShape shape = emitter.getShape();
        float p1 = emitter.getShapeParam1();
        float p2 = emitter.getShapeParam2();
        float p3 = emitter.getShapeParam3();
        boolean fromSurface = emitter.isEmitFromSurface();

        switch (shape) {
            case POINT:
                return new float[] { 0, 0, 0 };
            case SPHERE:
                return fromSurface ? randomOnSphere(p1) : randomInSphere(p1);
            case SPHERE_SURFACE:
                return randomOnSphere(p1);
            case HEMISPHERE:
                return randomInHemisphere(p1);
            case CIRCLE:
                return randomInCircle(p1);
            case RING:
                return randomOnRing(p1, p2);
            case CONE:
                return randomInCone(p1, p2, p3);
            case BOX:
                return fromSurface ? randomOnBox(p1, p2, p3) : randomInBox(p1, p2, p3);
            case CYLINDER:
                return randomInCylinder(p1, p2);
            case LINE:
                return new float[] { (random.nextFloat() - 0.5f) * p1, 0, 0 };
            case RECTANGLE:
                return new float[] { (random.nextFloat() - 0.5f) * p1, 0, (random.nextFloat() - 0.5f) * p2 };
            default:
                return new float[] { 0, 0, 0 };
        }
    }

    private float[] generateVelocity(ParticleEmitterComponent emitter, float[] localPos, Vector3f basePosition) {
        float vx = emitter.getVelocityX();
        float vy = emitter.getVelocityY();
        float vz = emitter.getVelocityZ();

        if (emitter.getSpeed() != 0 && emitter.isEmitAlongNormal()) {
            float len = (float) Math
                .sqrt(localPos[0] * localPos[0] + localPos[1] * localPos[1] + localPos[2] * localPos[2]);
            if (len > 0.0001f) {
                float dx = localPos[0] / len;
                float dy = localPos[1] / len;
                float dz = localPos[2] / len;
                vx += dx * emitter.getSpeed();
                vy += dy * emitter.getSpeed();
                vz += dz * emitter.getSpeed();
            } else {
                float[] dir = randomOnSphere(1.0f);
                vx += dir[0] * emitter.getSpeed();
                vy += dir[1] * emitter.getSpeed();
                vz += dir[2] * emitter.getSpeed();
            }
        }

        if (emitter.getVelocityVariation() > 0) {
            float var = 1.0f - emitter.getVelocityVariation() + random.nextFloat() * emitter.getVelocityVariation() * 2;
            vx *= var;
            vy *= var;
            vz *= var;
        }

        return new float[] { vx, vy, vz };
    }

    private float[] randomInSphere(float radius) {
        float x, y, z;
        do {
            x = random.nextFloat() * 2 - 1;
            y = random.nextFloat() * 2 - 1;
            z = random.nextFloat() * 2 - 1;
        } while (x * x + y * y + z * z > 1);
        return new float[] { x * radius, y * radius, z * radius };
    }

    private float[] randomOnSphere(float radius) {
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        float phi = (float) Math.acos(2 * random.nextFloat() - 1);
        float sinPhi = (float) Math.sin(phi);
        return new float[] { radius * sinPhi * (float) Math.cos(theta), radius * (float) Math.cos(phi),
            radius * sinPhi * (float) Math.sin(theta) };
    }

    private float[] randomInHemisphere(float radius) {
        float[] pos = randomInSphere(radius);
        pos[1] = Math.abs(pos[1]);
        return pos;
    }

    private float[] randomInCircle(float radius) {
        float r = radius * (float) Math.sqrt(random.nextFloat());
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        return new float[] { r * (float) Math.cos(theta), 0, r * (float) Math.sin(theta) };
    }

    private float[] randomOnRing(float outerRadius, float innerRadius) {
        float r = innerRadius + (outerRadius - innerRadius) * (float) Math.sqrt(random.nextFloat());
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        return new float[] { r * (float) Math.cos(theta), 0, r * (float) Math.sin(theta) };
    }

    private float[] randomInCone(float radius, float angle, float height) {
        float h = random.nextFloat() * height;
        float r = (h / Math.max(height, 0.001f)) * radius * (float) Math.tan(angle);
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        return new float[] { r * (float) Math.cos(theta), h, r * (float) Math.sin(theta) };
    }

    private float[] randomInBox(float width, float height, float depth) {
        return new float[] { (random.nextFloat() - 0.5f) * width, (random.nextFloat() - 0.5f) * height,
            (random.nextFloat() - 0.5f) * depth };
    }

    private float[] randomOnBox(float width, float height, float depth) {
        int face = random.nextInt(6);
        float x = (random.nextFloat() - 0.5f) * width;
        float y = (random.nextFloat() - 0.5f) * height;
        float z = (random.nextFloat() - 0.5f) * depth;

        switch (face) {
            case 0:
                x = width / 2;
                break;
            case 1:
                x = -width / 2;
                break;
            case 2:
                y = height / 2;
                break;
            case 3:
                y = -height / 2;
                break;
            case 4:
                z = depth / 2;
                break;
            case 5:
                z = -depth / 2;
                break;
        }
        return new float[] { x, y, z };
    }

    private float[] randomInCylinder(float radius, float height) {
        float r = radius * (float) Math.sqrt(random.nextFloat());
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        float y = (random.nextFloat() - 0.5f) * height;
        return new float[] { r * (float) Math.cos(theta), y, r * (float) Math.sin(theta) };
    }

    private float randomRange(float min, float max) {
        if (min == max) return min;
        return min + random.nextFloat() * (max - min);
    }

    @Override
    public void onDestroy() {
        TakoRenderMod.LOG.info("ParticleEmitSystem: Destroyed");
    }
}
