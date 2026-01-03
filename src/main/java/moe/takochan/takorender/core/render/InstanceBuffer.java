package moe.takochan.takorender.core.render;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 实例缓冲区
 *
 * <p>
 * InstanceBuffer 管理实例化渲染所需的变换矩阵数据。
 * 每个实例存储一个 4x4 变换矩阵（16 floats）。
 * </p>
 *
 * <p>
 * <b>属性布局</b>（占用 4 个属性槽位）:
 * </p>
 * <ul>
 * <li>location+0: mat4 第 1 列 (vec4)</li>
 * <li>location+1: mat4 第 2 列 (vec4)</li>
 * <li>location+2: mat4 第 3 列 (vec4)</li>
 * <li>location+3: mat4 第 4 列 (vec4)</li>
 * </ul>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * {@code
 * InstanceBuffer buffer = new InstanceBuffer(100); // 初始容量 100 个实例
 * buffer.begin();
 * buffer.addInstance(entity1Transform);
 * buffer.addInstance(entity2Transform);
 * buffer.end(); // 上传到 GPU
 *
 * mesh.bind();
 * buffer.bindAttributes(3); // 从 location 3 开始
 * GL31.glDrawElementsInstanced(..., buffer.getInstanceCount());
 * buffer.unbindAttributes(3);
 * mesh.unbind();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class InstanceBuffer implements AutoCloseable {

    /** 每个实例的 float 数量（mat4 = 16 floats） */
    private static final int FLOATS_PER_INSTANCE = 16;

    /** 每个实例的字节数 */
    private static final int BYTES_PER_INSTANCE = FLOATS_PER_INSTANCE * Float.BYTES;

    /** VBO ID */
    private int vbo;

    /** 当前容量（实例数） */
    private int capacity;

    /** 当前实例数量 */
    private int instanceCount;

    /** CPU 端缓冲区 */
    private FloatBuffer buffer;

    /** 临时矩阵（复用避免分配） */
    private final float[] tempMatrix = new float[16];

    /** 是否已释放 */
    private boolean disposed;

    /**
     * 创建实例缓冲区
     *
     * @param initialCapacity 初始容量（实例数）
     */
    public InstanceBuffer(int initialCapacity) {
        this.capacity = Math.max(16, initialCapacity);
        this.buffer = BufferUtils.createFloatBuffer(capacity * FLOATS_PER_INSTANCE);
        this.vbo = GL15.glGenBuffers();
        this.disposed = false;

        // 初始化 VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) capacity * BYTES_PER_INSTANCE, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 开始填充实例数据
     */
    public void begin() {
        buffer.clear();
        instanceCount = 0;
    }

    /**
     * 添加一个实例的变换矩阵
     *
     * @param transform 4x4 变换矩阵
     */
    public void addInstance(Matrix4f transform) {
        ensureCapacity(instanceCount + 1);

        // 获取矩阵数据（列主序）
        transform.get(tempMatrix);
        buffer.put(tempMatrix);
        instanceCount++;
    }

    /**
     * 结束填充并上传到 GPU
     */
    public void end() {
        if (instanceCount == 0) {
            return;
        }

        buffer.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 绑定实例属性到指定位置
     *
     * <p>
     * mat4 需要 4 个连续的属性槽位（location 到 location+3）。
     * </p>
     *
     * @param location 起始属性位置
     */
    public void bindAttributes(int location) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // mat4 需要 4 个 vec4 属性
        for (int i = 0; i < 4; i++) {
            int loc = location + i;
            GL20.glEnableVertexAttribArray(loc);
            GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, BYTES_PER_INSTANCE, (long) i * 16);
            // 每个实例更新一次（divisor = 1）
            GL33.glVertexAttribDivisor(loc, 1);
        }
    }

    /**
     * 解绑实例属性
     *
     * @param location 起始属性位置
     */
    public void unbindAttributes(int location) {
        for (int i = 0; i < 4; i++) {
            int loc = location + i;
            GL33.glVertexAttribDivisor(loc, 0);
            GL20.glDisableVertexAttribArray(loc);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 获取当前实例数量
     */
    public int getInstanceCount() {
        return instanceCount;
    }

    /**
     * 获取当前容量
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return instanceCount == 0;
    }

    /**
     * 确保有足够容量
     */
    private void ensureCapacity(int required) {
        if (required <= capacity) {
            return;
        }

        // 扩容为 1.5 倍或 required，取较大值
        int newCapacity = Math.max(required, capacity + (capacity >> 1));
        resize(newCapacity);
    }

    /**
     * 调整缓冲区大小
     */
    private void resize(int newCapacity) {
        // 创建新的 CPU 缓冲区
        FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity * FLOATS_PER_INSTANCE);

        // 复制现有数据
        buffer.flip();
        newBuffer.put(buffer);

        buffer = newBuffer;
        capacity = newCapacity;

        // 重新分配 GPU 缓冲区
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) capacity * BYTES_PER_INSTANCE, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 释放资源
     */
    public void dispose() {
        if (!disposed) {
            GL15.glDeleteBuffers(vbo);
            vbo = 0;
            buffer = null;
            disposed = true;
        }
    }

    @Override
    public void close() {
        dispose();
    }

    /**
     * 检查是否已释放
     */
    public boolean isDisposed() {
        return disposed;
    }
}
