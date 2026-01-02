package moe.takochan.takorender.core.particle;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;

/**
 * 粒子数据缓冲区管理
 *
 * <p>
 * 使用 SSBO (Shader Storage Buffer Object) 存储粒子数据，支持 GPU 直接读写。
 * </p>
 *
 * <p>
 * 粒子数据布局 (std430, 每粒子 64 bytes):
 * </p>
 *
 * <pre>
 * struct Particle {
 *     vec4 position;  // xyz: 位置, w: 当前生命周期
 *     vec4 velocity;  // xyz: 速度, w: 最大生命周期
 *     vec4 color;     // rgba
 *     vec4 params;    // x: 大小, y: 旋转, z: 类型, w: 自定义
 * };
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class ParticleBuffer implements AutoCloseable {

    /** GL_SHADER_STORAGE_BUFFER = 0x90D2 */
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;

    /** GL_ATOMIC_COUNTER_BUFFER = 0x92C0 */
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;

    /** 每个粒子的字节大小 (4 * vec4 = 64 bytes) */
    public static final int PARTICLE_SIZE_BYTES = 64;

    /** 每个粒子的浮点数数量 (16 floats) */
    public static final int PARTICLE_SIZE_FLOATS = 16;

    /** Position 在粒子结构中的偏移量 (bytes) */
    public static final int OFFSET_POSITION = 0;

    /** Velocity 在粒子结构中的偏移量 (bytes) */
    public static final int OFFSET_VELOCITY = 16;

    /** Color 在粒子结构中的偏移量 (bytes) */
    public static final int OFFSET_COLOR = 32;

    /** Params 在粒子结构中的偏移量 (bytes) */
    public static final int OFFSET_PARAMS = 48;

    /** 最大粒子数量 */
    private final int maxParticles;

    /** 缓冲区总大小 (bytes) */
    private final int bufferSize;

    /** SSBO ID */
    private int ssboId = 0;

    /** 原子计数器缓冲区 ID (存活粒子数) */
    private int atomicCounterId = 0;

    /** CPU 端缓冲区 (用于初始化和数据上传) */
    private FloatBuffer cpuBuffer;

    /** 是否已初始化 */
    private boolean initialized = false;

    /**
     * 创建粒子缓冲区
     *
     * @param maxParticles 最大粒子数量
     */
    public ParticleBuffer(int maxParticles) {
        if (!ShaderProgram.isSSBOSupported()) {
            TakoRenderMod.LOG.warn("SSBO not supported, ParticleBuffer will not function");
            this.maxParticles = 0;
            this.bufferSize = 0;
            return;
        }

        this.maxParticles = maxParticles;
        this.bufferSize = maxParticles * PARTICLE_SIZE_BYTES;

        TakoRenderMod.LOG
            .info("[ParticleBuffer] Creating buffer for {} particles ({} bytes)", maxParticles, bufferSize);
    }

    /**
     * 初始化 GPU 缓冲区（延迟初始化）
     */
    public void initialize() {
        init();
    }

    /**
     * 初始化 GPU 缓冲区（延迟初始化）
     */
    public void init() {
        if (initialized || maxParticles == 0) return;

        ssboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboId);

        ByteBuffer zeroData = BufferUtils.createByteBuffer(bufferSize);
        for (int i = 0; i < bufferSize; i++) {
            zeroData.put((byte) 0);
        }
        zeroData.flip();
        GL15.glBufferData(GL_SHADER_STORAGE_BUFFER, zeroData, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        atomicCounterId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, atomicCounterId);
        ByteBuffer zeroCounter = BufferUtils.createByteBuffer(4);
        zeroCounter.putInt(0, 0);
        GL15.glBufferData(GL_ATOMIC_COUNTER_BUFFER, zeroCounter, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, 0);

        cpuBuffer = BufferUtils.createFloatBuffer(maxParticles * PARTICLE_SIZE_FLOATS);

        initialized = true;
        TakoRenderMod.LOG.info("[ParticleBuffer] Initialized SSBO ID={}, AtomicCounter ID={}", ssboId, atomicCounterId);
    }

    private void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    /**
     * 将 SSBO 绑定到计算着色器的 binding point
     *
     * @param bindingPoint 绑定点
     */
    public void bindToCompute(int bindingPoint) {
        ensureInitialized();
        if (ssboId != 0) {
            GL30.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindingPoint, ssboId);
        }
    }

    /**
     * 将原子计数器绑定到 binding point
     *
     * @param bindingPoint 绑定点
     */
    public void bindAtomicCounter(int bindingPoint) {
        ensureInitialized();
        if (atomicCounterId != 0) {
            GL30.glBindBufferBase(GL_ATOMIC_COUNTER_BUFFER, bindingPoint, atomicCounterId);
        }
    }

    /**
     * 解绑 SSBO
     *
     * @param bindingPoint 绑定点
     */
    public static void unbind(int bindingPoint) {
        GL30.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindingPoint, 0);
    }

    /**
     * 重置原子计数器为指定值
     *
     * @param value 新值
     */
    public void resetAtomicCounter(int value) {
        ensureInitialized();
        if (atomicCounterId == 0) return;

        GL15.glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, atomicCounterId);
        ByteBuffer data = BufferUtils.createByteBuffer(4);
        data.putInt(0, value);
        GL15.glBufferSubData(GL_ATOMIC_COUNTER_BUFFER, 0, data);
        GL15.glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, 0);
    }

    /**
     * 读取原子计数器的值
     *
     * @return 当前存活粒子数
     */
    public int readAtomicCounter() {
        ensureInitialized();
        if (atomicCounterId == 0) return 0;

        GL15.glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, atomicCounterId);
        ByteBuffer data = GL15.glMapBuffer(GL_ATOMIC_COUNTER_BUFFER, GL15.GL_READ_ONLY, 4, null);
        int value = 0;
        if (data != null) {
            value = data.getInt(0);
            GL15.glUnmapBuffer(GL_ATOMIC_COUNTER_BUFFER);
        }
        GL15.glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, 0);
        return value;
    }

    /**
     * 上传粒子数据到 GPU
     *
     * @param particles 粒子数据数组 (每粒子 16 floats)
     * @param count     粒子数量
     */
    public void uploadParticles(float[] particles, int count) {
        ensureInitialized();
        if (ssboId == 0 || count <= 0) return;

        int uploadCount = Math.min(count, maxParticles);
        int dataSize = uploadCount * PARTICLE_SIZE_FLOATS;

        cpuBuffer.clear();
        cpuBuffer.put(particles, 0, dataSize);
        cpuBuffer.flip();

        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboId);
        GL15.glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, cpuBuffer);
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    /**
     * 上传单个粒子到指定位置
     */
    public void uploadParticle(int index, float posX, float posY, float posZ, float life, float velX, float velY,
        float velZ, float maxLife, float r, float g, float b, float a, float size, float rotation, float type,
        float custom) {
        ensureInitialized();
        if (ssboId == 0 || index < 0 || index >= maxParticles) return;

        FloatBuffer data = BufferUtils.createFloatBuffer(PARTICLE_SIZE_FLOATS);
        data.put(posX)
            .put(posY)
            .put(posZ)
            .put(life);
        data.put(velX)
            .put(velY)
            .put(velZ)
            .put(maxLife);
        data.put(r)
            .put(g)
            .put(b)
            .put(a);
        data.put(size)
            .put(rotation)
            .put(type)
            .put(custom);
        data.flip();

        long offset = (long) index * PARTICLE_SIZE_BYTES;

        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboId);
        GL15.glBufferSubData(GL_SHADER_STORAGE_BUFFER, offset, data);
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    /**
     * 清空所有粒子数据
     */
    public void clear() {
        ensureInitialized();
        if (ssboId == 0) return;

        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssboId);

        ByteBuffer zeroData = BufferUtils.createByteBuffer(bufferSize);
        for (int i = 0; i < bufferSize; i++) {
            zeroData.put((byte) 0);
        }
        zeroData.flip();

        GL15.glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, zeroData);
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        resetAtomicCounter(0);
    }

    /**
     * 将 SSBO 绑定为顶点属性（用于实例化渲染）
     *
     * @param vaoId VAO ID
     */
    public void bindAsVertexBuffer(int vaoId) {
        ensureInitialized();
        if (ssboId == 0) return;

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, ssboId);

        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, PARTICLE_SIZE_BYTES, OFFSET_POSITION);
        GL20.glEnableVertexAttribArray(0);

        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, PARTICLE_SIZE_BYTES, OFFSET_VELOCITY);
        GL20.glEnableVertexAttribArray(1);

        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, PARTICLE_SIZE_BYTES, OFFSET_COLOR);
        GL20.glEnableVertexAttribArray(2);

        GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, PARTICLE_SIZE_BYTES, OFFSET_PARAMS);
        GL20.glEnableVertexAttribArray(3);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    public int getMaxParticles() {
        return maxParticles;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getSSBOId() {
        return ssboId;
    }

    public int getSsboId() {
        return ssboId;
    }

    public void bindAsVertexAttribute() {
        ensureInitialized();
        if (ssboId != 0) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, ssboId);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isValid() {
        return initialized && ssboId != 0;
    }

    public void cleanup() {
        close();
    }

    @Override
    public void close() {
        if (ssboId != 0) {
            GL15.glDeleteBuffers(ssboId);
            ssboId = 0;
        }
        if (atomicCounterId != 0) {
            GL15.glDeleteBuffers(atomicCounterId);
            atomicCounterId = 0;
        }
        cpuBuffer = null;
        initialized = false;

        TakoRenderMod.LOG.info("[ParticleBuffer] Disposed");
    }
}
