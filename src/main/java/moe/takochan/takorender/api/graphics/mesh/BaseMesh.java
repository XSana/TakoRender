package moe.takochan.takorender.api.graphics.mesh;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.graphics.AABB;
import moe.takochan.takorender.api.graphics.Mesh;

/**
 * Mesh 基类，封装 VAO/VBO/EBO 的通用操作。
 *
 * <p>
 * 遵循 OpenGL 标准命名和资源管理模式。
 * 实现 AutoCloseable 以支持 try-with-resources。
 * </p>
 *
 * <p>
 * <b>状态契约</b>: 所有 draw 方法保证恢复调用前的 GL 状态：
 * </p>
 * <ul>
 * <li>VAO 绑定 (GL_VERTEX_ARRAY_BINDING)</li>
 * <li>VBO 绑定 (GL_ARRAY_BUFFER_BINDING)</li>
 * <li>EBO 绑定 (GL_ELEMENT_ARRAY_BUFFER_BINDING，仅当调用前 VAO=0 时)</li>
 * <li>顶点属性启用状态</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public abstract class BaseMesh implements Mesh, AutoCloseable {

    protected int vao = 0;
    protected int vbo = 0;
    protected int ebo = 0;
    protected int vertexCount = 0;
    protected int elementCount = 0;
    protected int drawMode = GL11.GL_TRIANGLES;
    protected boolean valid = false;
    protected boolean disposed = false;

    protected final VertexFormat format;
    protected final int strideBytes;
    protected final VertexAttribute[] attributes;

    /** 包围盒 */
    protected AABB bounds;

    /** 保存的状态（用于 bind/unbind） */
    private int savedVao, savedVbo, savedEbo;

    /**
     * 使用 VertexFormat 创建 Mesh
     *
     * @param format 顶点格式
     */
    protected BaseMesh(VertexFormat format) {
        this.format = format;
        this.strideBytes = format.getStride();
        this.attributes = format.getAttributes();
    }

    /**
     * 使用自定义属性创建 Mesh（向后兼容）
     *
     * @param strideBytes 每个顶点的字节数
     * @param attributes  顶点属性定义
     */
    protected BaseMesh(int strideBytes, VertexAttribute[] attributes) {
        this.format = null;
        this.strideBytes = strideBytes;
        this.attributes = attributes;
    }

    /**
     * 初始化 VAO/VBO/EBO
     */
    protected void initBuffers() {
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        ebo = GL15.glGenBuffers();
    }

    /**
     * 配置顶点属性指针。
     *
     * <p>
     * 必须在 VAO 绑定状态下调用。
     * </p>
     */
    protected void setupVertexAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            VertexAttribute attr = attributes[i];
            GL20.glEnableVertexAttribArray(i);
            GL20.glVertexAttribPointer(i, attr.size, attr.type, attr.normalized, strideBytes, attr.offset);
        }
    }

    /**
     * 启用顶点属性
     */
    protected void enableAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            GL20.glEnableVertexAttribArray(i);
        }
    }

    /**
     * 禁用顶点属性（恢复固定管线兼容状态）
     */
    protected void disableAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            GL20.glDisableVertexAttribArray(i);
        }
    }

    @Override
    public void bind() {
        if (valid) {
            // 保存当前状态
            savedVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            savedVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            savedEbo = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);

            // 绑定 VAO 并显式绑定 VBO/EBO（某些驱动需要）
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);

            // 确保顶点属性正确配置
            setupVertexAttributes();
        }
    }

    @Override
    public void unbind() {
        // 先在当前 VAO 中禁用顶点属性
        disableAttributes();

        // 切换 VAO 前解绑 VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // 恢复 VAO（会自动恢复其记录的 EBO 绑定）
        GL30.glBindVertexArray(savedVao);

        // 恢复 VBO（全局状态）
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);

        // 只有在 VAO 0 时才手动恢复 EBO
        if (savedVao == 0) {
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, savedEbo);
        }
    }

    @Override
    public void draw() {
        if (valid && elementCount > 0 && vao != 0 && vbo != 0 && ebo != 0) {
            bind();
            GL11.glDrawElements(drawMode, elementCount, GL11.GL_UNSIGNED_INT, 0);
            unbind();
        }
    }

    /**
     * 使用指定的绘制模式绘制 Mesh
     *
     * @param mode 绘制模式 (GL_TRIANGLES, GL_LINES, GL_POINTS 等)
     */
    public void draw(int mode) {
        if (valid && elementCount > 0 && vao != 0 && vbo != 0 && ebo != 0) {
            bind();
            GL11.glDrawElements(mode, elementCount, GL11.GL_UNSIGNED_INT, 0);
            unbind();
        }
    }

    @Override
    public void dispose() {
        cleanup();
    }

    /**
     * 实现 AutoCloseable，支持 try-with-resources
     */
    @Override
    public void close() {
        cleanup();
    }

    /**
     * 清理资源。
     *
     * <p>
     * 注意：必须在具有 OpenGL 上下文的线程中调用。
     * </p>
     */
    protected void cleanup() {
        if (valid && !disposed) {
            try {
                GL30.glDeleteVertexArrays(vao);
                GL15.glDeleteBuffers(vbo);
                GL15.glDeleteBuffers(ebo);
            } catch (Exception e) {
                // 忽略清理时的错误（可能没有 GL 上下文）
            }
            vao = vbo = ebo = 0;
            valid = false;
            disposed = true;
        }
    }

    /**
     * 设置绘制模式
     *
     * @param mode GL_TRIANGLES, GL_LINES, GL_POINTS 等
     */
    public void setDrawMode(int mode) {
        this.drawMode = mode;
    }

    public int getDrawMode() {
        return drawMode;
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    public int getIndexCount() {
        return elementCount;
    }

    public int getElementCount() {
        return elementCount;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public AABB getBounds() {
        return bounds;
    }

    /**
     * 设置包围盒
     *
     * @param bounds 包围盒
     */
    public void setBounds(AABB bounds) {
        this.bounds = bounds;
    }

    /**
     * 从顶点数据计算包围盒
     *
     * @param vertexData 顶点数据
     */
    protected void computeBounds(float[] vertexData) {
        if (vertexData == null || vertexData.length == 0) {
            this.bounds = null;
            return;
        }
        int floatsPerVertex = strideBytes / 4;
        this.bounds = AABB.fromVertices(vertexData, floatsPerVertex, 0);
    }

    public int getVao() {
        return vao;
    }

    public int getVbo() {
        return vbo;
    }

    public int getEbo() {
        return ebo;
    }

    /**
     * 是否使用索引缓冲区
     */
    public boolean hasIndices() {
        return ebo != 0;
    }

    public VertexFormat getFormat() {
        return format;
    }

    public int getStrideBytes() {
        return strideBytes;
    }
}
