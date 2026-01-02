package moe.takochan.takorender.core.particle;

import java.util.List;
import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.particle.ParticleForce;
import moe.takochan.takorender.api.particle.RotationOverLifetime;
import moe.takochan.takorender.api.particle.VelocityOverLifetime;

/**
 * CPU 端粒子物理更新
 *
 * <p>
 * 提供 CPU 回退实现，用于不支持 Compute Shader 的系统（如 macOS）。
 * 实现简化的物理模拟：重力、阻力、基础力场。
 * </p>
 *
 * <p>
 * <b>性能注意</b>: CPU 实现比 GPU 慢很多，建议限制最大粒子数。
 * </p>
 */
@SideOnly(Side.CLIENT)
public class ParticleCPU {

    /** 粒子数据数组（与 GPU 布局一致） */
    private float[] particles;

    /** 最大粒子数 */
    private int maxParticles;

    /** 当前存活粒子数 */
    private int aliveCount;

    /** 随机数生成器 */
    private final Random random = new Random();

    /** 速度曲线 */
    private VelocityOverLifetime velocityOverLifetime;

    /** 旋转曲线 */
    private RotationOverLifetime rotationOverLifetime;

    /**
     * 创建 CPU 粒子系统
     *
     * @param maxParticles 最大粒子数
     */
    public ParticleCPU(int maxParticles) {
        this.maxParticles = maxParticles;
        this.particles = new float[maxParticles * ParticleBuffer.PARTICLE_SIZE_FLOATS];
        this.aliveCount = 0;
    }

    /**
     * 设置速度曲线
     */
    public void setVelocityOverLifetime(VelocityOverLifetime curve) {
        this.velocityOverLifetime = curve;
    }

    /**
     * 设置旋转曲线
     */
    public void setRotationOverLifetime(RotationOverLifetime curve) {
        this.rotationOverLifetime = curve;
    }

    /**
     * 更新所有粒子
     *
     * @param deltaTime 时间增量
     * @param forces    力场列表
     */
    public void update(float deltaTime, List<ParticleForce> forces) {
        aliveCount = 0;

        for (int i = 0; i < maxParticles; i++) {
            int base = i * ParticleBuffer.PARTICLE_SIZE_FLOATS;

            float life = particles[base + 3];
            if (life <= 0) continue;

            float maxLife = particles[base + 7];
            float lifePercent = 1.0f - (life / maxLife);

            life -= deltaTime;
            if (life <= 0) {
                particles[base + 3] = 0;
                continue;
            }
            particles[base + 3] = life;

            float posX = particles[base];
            float posY = particles[base + 1];
            float posZ = particles[base + 2];

            float velX = particles[base + 4];
            float velY = particles[base + 5];
            float velZ = particles[base + 6];

            float rotation = particles[base + 13];
            float angularVel = particles[base + 15];

            float forceX = 0, forceY = 0, forceZ = 0;
            if (forces != null) {
                for (ParticleForce force : forces) {
                    if (!force.isEnabled()) continue;

                    float[] f = applyForce(force, posX, posY, posZ, velX, velY, velZ);
                    forceX += f[0];
                    forceY += f[1];
                    forceZ += f[2];
                }
            }

            velX += forceX * deltaTime;
            velY += forceY * deltaTime;
            velZ += forceZ * deltaTime;

            float effectiveVelX = velX;
            float effectiveVelY = velY;
            float effectiveVelZ = velZ;

            if (velocityOverLifetime != null) {
                float[] mult = velocityOverLifetime.evaluate(lifePercent);
                effectiveVelX *= mult[0];
                effectiveVelY *= mult[1];
                effectiveVelZ *= mult[2];
            }

            posX += effectiveVelX * deltaTime;
            posY += effectiveVelY * deltaTime;
            posZ += effectiveVelZ * deltaTime;

            float additionalAngVel = 0;
            if (rotationOverLifetime != null) {
                additionalAngVel = rotationOverLifetime.evaluateZ(lifePercent);
            }
            rotation += (angularVel + additionalAngVel) * deltaTime;

            particles[base] = posX;
            particles[base + 1] = posY;
            particles[base + 2] = posZ;
            particles[base + 4] = velX;
            particles[base + 5] = velY;
            particles[base + 6] = velZ;
            particles[base + 13] = rotation;

            aliveCount++;
        }
    }

    /**
     * 计算力场效果
     */
    private float[] applyForce(ParticleForce force, float posX, float posY, float posZ, float velX, float velY,
        float velZ) {

        float[] result = new float[] { 0, 0, 0 };

        switch (force.getType()) {
            case GRAVITY:
                result[0] = force.getX() * force.getStrength();
                result[1] = force.getY() * force.getStrength();
                result[2] = force.getZ() * force.getStrength();
                break;

            case WIND:
                result[0] = force.getX() * force.getStrength();
                result[1] = force.getY() * force.getStrength();
                result[2] = force.getZ() * force.getStrength();
                break;

            case DRAG:
                result[0] = -velX * force.getStrength();
                result[1] = -velY * force.getStrength();
                result[2] = -velZ * force.getStrength();
                break;

            case ATTRACTOR: {
                float dx = force.getX() - posX;
                float dy = force.getY() - posY;
                float dz = force.getZ() - posZ;
                float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 0.01f) {
                    float factor = force.getStrength() / dist;
                    result[0] = dx * factor;
                    result[1] = dy * factor;
                    result[2] = dz * factor;
                }
                break;
            }

            case REPULSOR: {
                float dx = posX - force.getX();
                float dy = posY - force.getY();
                float dz = posZ - force.getZ();
                float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 0.01f) {
                    float factor = force.getStrength() / dist;
                    result[0] = dx * factor;
                    result[1] = dy * factor;
                    result[2] = dz * factor;
                }
                break;
            }

            case TURBULENCE: {
                float scale = force.getParam1();
                float nx = simplexNoise3D(posX * scale, posY * scale, posZ * scale);
                float ny = simplexNoise3D(posX * scale + 100, posY * scale + 100, posZ * scale + 100);
                float nz = simplexNoise3D(posX * scale + 200, posY * scale + 200, posZ * scale + 200);
                result[0] = nx * force.getStrength();
                result[1] = ny * force.getStrength();
                result[2] = nz * force.getStrength();
                break;
            }

            case VELOCITY_LIMIT: {
                float speed = (float) Math.sqrt(velX * velX + velY * velY + velZ * velZ);
                if (speed > force.getStrength()) {
                    float factor = 1.0f - force.getStrength() / speed;
                    result[0] = -velX * factor;
                    result[1] = -velY * factor;
                    result[2] = -velZ * factor;
                }
                break;
            }

            default:
                break;
        }

        return result;
    }

    /**
     * 简化的 3D 噪声（用于湍流）
     */
    private float simplexNoise3D(float x, float y, float z) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iz = (int) Math.floor(z);
        float fx = x - ix;
        float fy = y - iy;
        float fz = z - iz;

        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        fz = fz * fz * (3 - 2 * fz);

        int n = ix + iy * 57 + iz * 113;
        float v000 = hash(n);
        float v001 = hash(n + 1);
        float v010 = hash(n + 57);
        float v011 = hash(n + 58);
        float v100 = hash(n + 113);
        float v101 = hash(n + 114);
        float v110 = hash(n + 170);
        float v111 = hash(n + 171);

        float x00 = lerp(v000, v001, fx);
        float x01 = lerp(v010, v011, fx);
        float x10 = lerp(v100, v101, fx);
        float x11 = lerp(v110, v111, fx);

        float y0 = lerp(x00, x01, fy);
        float y1 = lerp(x10, x11, fy);

        return lerp(y0, y1, fz) * 2 - 1;
    }

    private float hash(int n) {
        n = (n << 13) ^ n;
        return (1.0f - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * 发射粒子
     */
    public int emit(float posX, float posY, float posZ, float velX, float velY, float velZ, float life, float size,
        float r, float g, float b, float a, float rotation, float angularVel) {

        for (int i = 0; i < maxParticles; i++) {
            int base = i * ParticleBuffer.PARTICLE_SIZE_FLOATS;
            if (particles[base + 3] <= 0) {
                particles[base] = posX;
                particles[base + 1] = posY;
                particles[base + 2] = posZ;
                particles[base + 3] = life;

                particles[base + 4] = velX;
                particles[base + 5] = velY;
                particles[base + 6] = velZ;
                particles[base + 7] = life;

                particles[base + 8] = r;
                particles[base + 9] = g;
                particles[base + 10] = b;
                particles[base + 11] = a;

                particles[base + 12] = size;
                particles[base + 13] = rotation;
                particles[base + 14] = 0;
                particles[base + 15] = angularVel;

                return i;
            }
        }
        return -1;
    }

    /**
     * 获取粒子数据（用于渲染）
     */
    public float[] getParticles() {
        return particles;
    }

    /**
     * 获取存活粒子数
     */
    public int getAliveCount() {
        return aliveCount;
    }

    /**
     * 获取最大粒子数
     */
    public int getMaxParticles() {
        return maxParticles;
    }

    /**
     * 清空所有粒子
     */
    public void clear() {
        for (int i = 0; i < particles.length; i++) {
            particles[i] = 0;
        }
        aliveCount = 0;
    }
}
