package moe.takochan.takorender.api.graphics.batch;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.graphics.mesh.DynamicMesh;
import moe.takochan.takorender.api.graphics.mesh.VertexFormat;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.api.resource.ResourceHandle;
import moe.takochan.takorender.api.resource.ShaderManager;
import moe.takochan.takorender.core.gl.GLStateContext;

/**
 * 2D 图元批量渲染器
 *
 * <p>
 * 支持颜色四边形批量渲染，自动管理 GL 状态。
 * </p>
 *
 * <p>
 * <b>状态管理契约</b>: 本类使用 GLStateContext 自动管理 GL 状态。
 * </p>
 * <ul>
 * <li>begin() 通过 GLStateContext 保存当前 GL 状态</li>
 * <li>end() 自动恢复之前的 GL 状态</li>
 * <li>调用者不应在 begin/end 之外使用 glPushAttrib</li>
 * <li>所有 Batch 类（SpriteBatch、World3DBatch、World3DBatchLit）遵循相同契约</li>
 * </ul>
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     SpriteBatch batch = RenderSystem.getSpriteBatch();
 *     batch.setProjectionOrtho(width, height);
 *     batch.begin(); // 自动保存状态
 *     batch.drawQuad(x1, y1, x2, y2, x3, y3, x4, y4, r, g, b, a);
 *     batch.end(); // 自动恢复状态
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class SpriteBatch implements AutoCloseable {

    /** 每顶点浮点数: 2(位置) + 4(颜色) = 6 */
    private static final int FLOATS_PER_VERTEX = 6;
    /** 每四边形顶点数 */
    private static final int VERTICES_PER_QUAD = 4;
    /** 每四边形索引数 */
    private static final int INDICES_PER_QUAD = 6;
    /** 默认最大四边形数量 */
    private static final int DEFAULT_MAX_QUADS = 256;
    /** Shader 资源键 */
    private static final String SHADER_KEY = ShaderManager.SHADER_GUI_COLOR;

    private final int maxQuads;
    private final DynamicMesh mesh;

    /** 预分配的顶点数据数组 */
    private final float[] vertexData;
    /** 预分配的索引数据数组 */
    private final int[] indexData;

    /** 当前顶点写入位置 */
    private int vertexOffset = 0;
    /** 当前索引写入位置 */
    private int indexOffset = 0;
    /** 当前四边形数量 */
    private int quadCount = 0;

    /** 缓存的投影矩阵 */
    private final FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
    /** 缓存的屏幕尺寸 */
    private int cachedWidth = -1;
    private int cachedHeight = -1;

    /** 是否正在绘制 */
    private boolean drawing = false;
    private boolean disposed = false;

    /** GL 状态上下文（用于自动状态管理） */
    private GLStateContext glStateContext = null;

    /** Shader 资源句柄 */
    private ResourceHandle<ShaderProgram> shaderHandle = null;

    /**
     * 使用默认容量创建 SpriteBatch
     */
    public SpriteBatch() {
        this(DEFAULT_MAX_QUADS);
    }

    /**
     * 使用指定容量创建 SpriteBatch
     *
     * @param maxQuads 最大四边形数量
     */
    public SpriteBatch(int maxQuads) {
        this.maxQuads = maxQuads;
        this.vertexData = new float[maxQuads * VERTICES_PER_QUAD * FLOATS_PER_VERTEX];
        this.indexData = new int[maxQuads * INDICES_PER_QUAD];

        this.mesh = new DynamicMesh(
            maxQuads * VERTICES_PER_QUAD,
            maxQuads * INDICES_PER_QUAD,
            VertexFormat.POSITION_COLOR);
    }

    /**
     * 设置正交投影矩阵
     *
     * @param width  屏幕宽度
     * @param height 屏幕高度
     */
    public void setProjectionOrtho(int width, int height) {
        if (width != cachedWidth || height != cachedHeight) {
            updateProjectionMatrix(width, height);
            cachedWidth = width;
            cachedHeight = height;
        }
    }

    /**
     * 设置自定义投影矩阵
     *
     * @param matrix 投影矩阵（16 个浮点数，列优先）
     */
    public void setProjectionMatrix(FloatBuffer matrix) {
        projMatrix.clear();
        projMatrix.put(matrix);
        projMatrix.flip();
        cachedWidth = -1;
        cachedHeight = -1;
    }

    /**
     * 开始批量渲染
     *
     * <p>
     * <b>状态管理契约</b>: 本方法保存并恢复 GL 状态。
     * </p>
     */
    public void begin() {
        if (drawing) {
            throw new IllegalStateException("SpriteBatch.end() must be called before begin()");
        }

        if (!ShaderProgram.isSupported()) {
            return;
        }

        // 开始 GL 状态上下文（替代 glPushAttrib）
        glStateContext = GLStateContext.begin();

        // 通过 GLStateContext 设置渲染状态
        glStateContext.enableBlend();
        glStateContext.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        glStateContext.disableDepthTest();
        glStateContext.disableTexture2D();
        glStateContext.disableCullFace();
        glStateContext.disableAlphaTest();
        glStateContext.disableLighting();
        glStateContext.setDepthMask(false);

        // 重置计数器
        vertexOffset = 0;
        indexOffset = 0;
        quadCount = 0;

        drawing = true;
    }

    /**
     * 添加颜色四边形（任意形状）
     */
    public void drawQuad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float r,
        float g, float b, float a) {
        if (!drawing) {
            throw new IllegalStateException("SpriteBatch.begin() must be called before drawing");
        }
        if (quadCount >= maxQuads) {
            flush();
        }

        int baseVertex = quadCount * VERTICES_PER_QUAD;

        // 顶点 0
        vertexData[vertexOffset++] = x1;
        vertexData[vertexOffset++] = y1;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;

        // 顶点 1
        vertexData[vertexOffset++] = x2;
        vertexData[vertexOffset++] = y2;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;

        // 顶点 2
        vertexData[vertexOffset++] = x3;
        vertexData[vertexOffset++] = y3;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;

        // 顶点 3
        vertexData[vertexOffset++] = x4;
        vertexData[vertexOffset++] = y4;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;

        // 索引（两个三角形）
        indexData[indexOffset++] = baseVertex;
        indexData[indexOffset++] = baseVertex + 1;
        indexData[indexOffset++] = baseVertex + 2;
        indexData[indexOffset++] = baseVertex;
        indexData[indexOffset++] = baseVertex + 2;
        indexData[indexOffset++] = baseVertex + 3;

        quadCount++;
    }

    /**
     * 添加矩形（轴对齐）
     */
    public void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        drawQuad(x, y, x + w, y, x + w, y + h, x, y + h, r, g, b, a);
    }

    /**
     * 添加矩形（使用 RGBA 整数颜色）
     *
     * @param color RGBA 颜色值 (0xRRGGBBAA)
     */
    public void drawRect(float x, float y, float w, float h, int color) {
        float r = ((color >> 24) & 0xFF) / 255f;
        float g = ((color >> 16) & 0xFF) / 255f;
        float b = ((color >> 8) & 0xFF) / 255f;
        float a = (color & 0xFF) / 255f;
        drawRect(x, y, w, h, r, g, b, a);
    }

    /**
     * 刷新当前批次（提交并绘制）
     */
    public void flush() {
        if (quadCount == 0) return;

        mesh.updateData(vertexData, vertexOffset, indexData, indexOffset);

        ShaderProgram shader = getShader();
        if (shader == null || !shader.isValid()) {
            vertexOffset = 0;
            indexOffset = 0;
            quadCount = 0;
            return;
        }
        shader.use();

        projMatrix.rewind();
        shader.setUniformMatrix4("uProjection", false, projMatrix);

        mesh.draw();

        ShaderProgram.unbind();

        vertexOffset = 0;
        indexOffset = 0;
        quadCount = 0;
    }

    /**
     * 获取 Shader（延迟加载）
     */
    private ShaderProgram getShader() {
        if (shaderHandle == null || !shaderHandle.isValid()) {
            shaderHandle = ShaderManager.instance()
                .get(SHADER_KEY);
        }
        return shaderHandle != null ? shaderHandle.get() : null;
    }

    /**
     * 结束批量渲染
     *
     * <p>
     * <b>状态管理契约</b>: 本方法恢复 begin() 保存的 GL 状态。
     * </p>
     */
    public void end() {
        if (!drawing) {
            throw new IllegalStateException("SpriteBatch.begin() must be called before end()");
        }

        flush();

        GL20.glUseProgram(0);

        // 关闭 GL 状态上下文（替代 glPopAttrib）
        if (glStateContext != null) {
            glStateContext.close();
            glStateContext = null;
        }

        drawing = false;
    }

    /**
     * 检查是否正在绘制
     */
    public boolean isDrawing() {
        return drawing;
    }

    /**
     * 获取最大四边形数量
     */
    public int getMaxQuads() {
        return maxQuads;
    }

    /**
     * 获取当前批次中的四边形数量
     */
    public int getQuadCount() {
        return quadCount;
    }

    /**
     * 释放资源（旧版方法）
     */
    public void dispose() {
        close();
    }

    /**
     * 实现 AutoCloseable
     */
    @Override
    public void close() {
        if (!disposed) {
            if (glStateContext != null) {
                glStateContext.close();
                glStateContext = null;
            }
            if (shaderHandle != null) {
                shaderHandle.release();
                shaderHandle = null;
            }
            drawing = false;
            mesh.close();
            disposed = true;
        }
    }

    /**
     * 更新投影矩阵缓存
     */
    private void updateProjectionMatrix(int screenWidth, int screenHeight) {
        float left = 0;
        float right = screenWidth;
        float bottom = screenHeight;
        float top = 0;
        float near = -1;
        float far = 1;

        float tx = -(right + left) / (right - left);
        float ty = -(top + bottom) / (top - bottom);
        float tz = -(far + near) / (far - near);

        projMatrix.clear();
        projMatrix.put(2.0f / (right - left));
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(2.0f / (top - bottom));
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(-2.0f / (far - near));
        projMatrix.put(0);
        projMatrix.put(tx);
        projMatrix.put(ty);
        projMatrix.put(tz);
        projMatrix.put(1);
        projMatrix.flip();
    }
}
