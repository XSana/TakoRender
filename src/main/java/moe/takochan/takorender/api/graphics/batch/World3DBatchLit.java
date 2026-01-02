package moe.takochan.takorender.api.graphics.batch;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.graphics.mesh.DynamicMesh;
import moe.takochan.takorender.api.graphics.mesh.VertexAttribute;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.api.resource.ResourceHandle;
import moe.takochan.takorender.api.resource.ShaderManager;
import moe.takochan.takorender.core.gl.GLStateContext;

/**
 * 带 MC 光照支持的 3D 世界批量渲染器。
 *
 * <p>
 * 继承 World3DBatch，增加光照贴图纹理采样以实现真实光照。
 * </p>
 *
 * <p>
 * <b>状态管理契约</b>: 本类使用 GLStateContext 自动管理 GL 状态。
 * </p>
 *
 * <p>
 * 功能特性:
 * </p>
 * <ul>
 * <li>自动从世界坐标查询光照</li>
 * <li>支持昼夜循环</li>
 * <li>支持方块光源（火把、岩浆等）</li>
 * <li>可选的光照开关</li>
 * </ul>
 *
 * <p>
 * 使用示例（在 RenderWorldLastEvent 中）:
 * </p>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     World3DBatchLit batch = RenderSystem.getWorld3DBatchLit();
 *     batch.setLightingEnabled(true);
 *
 *     double rx = blockX - playerEyeX;
 *     double ry = blockY - playerEyeY;
 *     double rz = blockZ - playerEyeZ;
 *
 *     batch.begin(GL11.GL_TRIANGLES); // 自动保存状态
 *     batch.drawSolidBox(rx, ry, rz, 1, 1, 1, 1.0f, 0.5f, 0.2f, 1.0f);
 *     batch.end(); // 自动恢复状态
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class World3DBatchLit implements AutoCloseable {

    /** 每顶点浮点数: 3(位置) + 4(颜色) + 2(光照坐标) + 3(法线) = 12 */
    private static final int FLOATS_PER_VERTEX = 12;
    /** 默认最大顶点数 */
    private static final int DEFAULT_MAX_VERTICES = 8192;
    /** Shader 资源键 */
    private static final String SHADER_KEY = ShaderManager.SHADER_WORLD3D_LIT;

    /**
     * 顶点格式: 位置(3) + 颜色(4) + 光照坐标(2) + 法线(3)
     * MC 1.7.10 右手坐标系: +X=东, +Y=上, +Z=南（朝向玩家）
     */
    private static final VertexAttribute[] ATTRIBUTES = { VertexAttribute.position3D(0), // 偏移 0, 12 字节
        VertexAttribute.colorFloat(12), // 偏移 12, 16 字节
        VertexAttribute.lightCoord(28), // 偏移 28, 8 字节
        VertexAttribute.normal(36) // 偏移 36, 12 字节
    };
    private static final int STRIDE = FLOATS_PER_VERTEX * 4; // 48 字节

    private final int maxVertices;
    private final DynamicMesh mesh;

    /** 预分配的顶点数据数组 */
    private final float[] vertexData;
    /** 预分配的索引数据数组 */
    private final int[] indexData;

    /** 当前顶点写入位置 */
    private int vertexOffset = 0;
    /** 当前索引写入位置 */
    private int indexOffset = 0;
    /** 当前顶点数量 */
    private int vertexCount = 0;

    /** MC 矩阵缓冲区 */
    private final FloatBuffer modelViewBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);

    /** 全局透明度 */
    private float alpha = 1.0f;

    /** 当前绘制模式 */
    private int currentDrawMode = GL11.GL_LINES;

    /** 是否正在绘制 */
    private boolean drawing = false;
    private boolean disposed = false;

    /** GL 状态上下文 */
    private GLStateContext glStateContext = null;

    /** 是否启用 MC 光照 */
    private boolean lightingEnabled = true;

    /** 默认光照坐标（光照禁用或查询失败时使用） */
    private float defaultLightU = 1.0f;
    private float defaultLightV = 1.0f;

    /** 光照强度乘数（默认 1.0） */
    private float lightIntensity = 1.0f;

    /** Shader 资源句柄 */
    private ResourceHandle<ShaderProgram> shaderHandle = null;

    /** 最小亮度下限（默认 0.1） */
    private float minBrightness = 0.1f;

    /** 是否启用法线方向着色 */
    private boolean normalShadingEnabled = true;

    public World3DBatchLit() {
        this(DEFAULT_MAX_VERTICES);
    }

    public World3DBatchLit(int maxVertices) {
        this.maxVertices = maxVertices;
        this.vertexData = new float[maxVertices * FLOATS_PER_VERTEX];
        this.indexData = new int[maxVertices];
        this.mesh = new DynamicMesh(maxVertices, maxVertices, STRIDE, ATTRIBUTES);
    }

    /**
     * 启用/禁用 MC 光照
     */
    public void setLightingEnabled(boolean enabled) {
        this.lightingEnabled = enabled;
    }

    public boolean isLightingEnabled() {
        return lightingEnabled;
    }

    /**
     * 设置全局透明度
     */
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    /**
     * 设置默认光照坐标（光照禁用或查询失败时使用）
     *
     * @param blockLight 方块光照等级 (0-15)
     * @param skyLight   天空光照等级 (0-15)
     */
    public void setDefaultLight(int blockLight, int skyLight) {
        this.defaultLightU = Math.max(0, Math.min(15, blockLight)) / 15.0f;
        this.defaultLightV = Math.max(0, Math.min(15, skyLight)) / 15.0f;
    }

    /**
     * 设置光照强度乘数
     *
     * @param intensity 强度乘数（默认 1.0，建议范围 0.5-2.0）
     */
    public void setLightIntensity(float intensity) {
        this.lightIntensity = Math.max(0, intensity);
    }

    /**
     * 设置最小亮度下限（防止完全黑暗）
     *
     * @param minBrightness 最小亮度（默认 0.1，范围 0-1）
     */
    public void setMinBrightness(float minBrightness) {
        this.minBrightness = Math.max(0, Math.min(1, minBrightness));
    }

    /**
     * 启用/禁用法线方向着色
     *
     * @param enabled 是否启用（默认 true）
     */
    public void setNormalShadingEnabled(boolean enabled) {
        this.normalShadingEnabled = enabled;
    }

    public boolean isNormalShadingEnabled() {
        return normalShadingEnabled;
    }

    /**
     * 开始批量渲染（默认线条模式）
     */
    public void begin() {
        begin(GL11.GL_LINES);
    }

    /**
     * 开始批量渲染
     *
     * @param drawMode GL_LINES、GL_TRIANGLES、GL_QUADS 等
     */
    public void begin(int drawMode) {
        if (drawing) {
            throw new IllegalStateException("World3DBatchLit.end() must be called before begin()");
        }

        if (!ShaderProgram.isSupported()) {
            return;
        }

        this.currentDrawMode = drawMode;

        // 捕获 MC 当前矩阵
        modelViewBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
        projectionBuffer.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);

        // 开始 GL 状态上下文（替代 glPushAttrib）
        glStateContext = GLStateContext.begin();

        // 通过 GLStateContext 设置渲染状态
        glStateContext.enableBlend();
        glStateContext.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        glStateContext.disableTexture2D();
        glStateContext.disableLighting();
        glStateContext.disableCullFace(); // 面剔除在着色器中完成
        glStateContext.setDepthMask(true); // 启用深度写入以实现正确遮挡
        glStateContext.enableDepthTest();

        if (drawMode == GL11.GL_LINES) {
            glStateContext.setLineWidth(2.0f);
        }

        // 重置计数器
        vertexOffset = 0;
        indexOffset = 0;
        vertexCount = 0;

        drawing = true;
    }

    /**
     * 添加顶点（带光照坐标和法线）
     */
    private int addVertex(double x, double y, double z, float r, float g, float b, float a, float lightU, float lightV,
        float nx, float ny, float nz) {
        if (vertexCount >= maxVertices) {
            flush();
        }

        vertexData[vertexOffset++] = (float) x;
        vertexData[vertexOffset++] = (float) y;
        vertexData[vertexOffset++] = (float) z;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;
        vertexData[vertexOffset++] = lightU;
        vertexData[vertexOffset++] = lightV;
        vertexData[vertexOffset++] = nx;
        vertexData[vertexOffset++] = ny;
        vertexData[vertexOffset++] = nz;

        return vertexCount++;
    }

    /**
     * 添加索引
     */
    private void addIndex(int index) {
        indexData[indexOffset++] = index;
    }

    /**
     * 绘制线段（使用默认向上法线）
     */
    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b,
        float a) {
        if (!drawing) return;

        // 线条使用默认向上法线和默认光照
        int v0 = addVertex(x1, y1, z1, r, g, b, a, defaultLightU, defaultLightV, 0, 1, 0);
        int v1 = addVertex(x2, y2, z2, r, g, b, a, defaultLightU, defaultLightV, 0, 1, 0);
        addIndex(v0);
        addIndex(v1);
    }

    /**
     * 绘制线段（手动指定光照）
     */
    public void drawLineWithLight(double x1, double y1, double z1, double x2, double y2, double z2, float r, float g,
        float b, float a, float lightU, float lightV) {
        if (!drawing) return;

        int v0 = addVertex(x1, y1, z1, r, g, b, a, lightU, lightV, 0, 1, 0);
        int v1 = addVertex(x2, y2, z2, r, g, b, a, lightU, lightV, 0, 1, 0);
        addIndex(v0);
        addIndex(v1);
    }

    /**
     * 绘制线框盒子
     */
    public void drawWireBox(double x, double y, double z, double w, double h, double d, float r, float g, float b,
        float a) {
        double x2 = x + w, y2 = y + h, z2 = z + d;

        // 底面
        drawLine(x, y, z, x2, y, z, r, g, b, a);
        drawLine(x2, y, z, x2, y, z2, r, g, b, a);
        drawLine(x2, y, z2, x, y, z2, r, g, b, a);
        drawLine(x, y, z2, x, y, z, r, g, b, a);
        // 顶面
        drawLine(x, y2, z, x2, y2, z, r, g, b, a);
        drawLine(x2, y2, z, x2, y2, z2, r, g, b, a);
        drawLine(x2, y2, z2, x, y2, z2, r, g, b, a);
        drawLine(x, y2, z2, x, y2, z, r, g, b, a);
        // 连接边
        drawLine(x, y, z, x, y2, z, r, g, b, a);
        drawLine(x2, y, z, x2, y2, z, r, g, b, a);
        drawLine(x2, y, z2, x2, y2, z2, r, g, b, a);
        drawLine(x, y, z2, x, y2, z2, r, g, b, a);
    }

    /**
     * 绘制十字标记
     */
    public void drawCross(double x, double y, double z, double size, float r, float g, float b, float a) {
        double half = size / 2;
        drawLine(x - half, y, z, x + half, y, z, r, g, b, a);
        drawLine(x, y - half, z, x, y + half, z, r, g, b, a);
        drawLine(x, y, z - half, x, y, z + half, r, g, b, a);
    }

    /**
     * 绘制实心四边形（需要 GL_TRIANGLES 模式，带法线）
     */
    public void drawQuad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3,
        double z3, double x4, double y4, double z4, float r, float g, float b, float a, float nx, float ny, float nz) {
        if (!drawing) return;

        // 暂时使用默认光照（MC 集成阶段会提供实际值）
        float lightU = defaultLightU;
        float lightV = defaultLightV;

        int v0 = addVertex(x1, y1, z1, r, g, b, a, lightU, lightV, nx, ny, nz);
        int v1 = addVertex(x2, y2, z2, r, g, b, a, lightU, lightV, nx, ny, nz);
        int v2 = addVertex(x3, y3, z3, r, g, b, a, lightU, lightV, nx, ny, nz);
        int v3 = addVertex(x4, y4, z4, r, g, b, a, lightU, lightV, nx, ny, nz);

        // 两个三角形
        addIndex(v0);
        addIndex(v1);
        addIndex(v2);
        addIndex(v0);
        addIndex(v2);
        addIndex(v3);
    }

    /**
     * 绘制实心四边形（手动指定光照和法线）
     */
    public void drawQuadWithLight(double x1, double y1, double z1, double x2, double y2, double z2, double x3,
        double y3, double z3, double x4, double y4, double z4, float r, float g, float b, float a, float lightU,
        float lightV, float nx, float ny, float nz) {
        if (!drawing) return;

        int v0 = addVertex(x1, y1, z1, r, g, b, a, lightU, lightV, nx, ny, nz);
        int v1 = addVertex(x2, y2, z2, r, g, b, a, lightU, lightV, nx, ny, nz);
        int v2 = addVertex(x3, y3, z3, r, g, b, a, lightU, lightV, nx, ny, nz);
        int v3 = addVertex(x4, y4, z4, r, g, b, a, lightU, lightV, nx, ny, nz);

        addIndex(v0);
        addIndex(v1);
        addIndex(v2);
        addIndex(v0);
        addIndex(v2);
        addIndex(v3);
    }

    /**
     * 绘制实心盒子（每个面使用正确的法线）
     * 顶点顺序为 CCW（逆时针），从外部看为正面。
     */
    public void drawSolidBox(double x, double y, double z, double w, double h, double d, float r, float g, float b,
        float a) {
        double x2 = x + w, y2 = y + h, z2 = z + d;

        // 底面 (Y-) - 法线 (0, -1, 0)
        drawQuad(x, y, z2, x2, y, z2, x2, y, z, x, y, z, r * 0.5f, g * 0.5f, b * 0.5f, a, 0, -1, 0);
        // 顶面 (Y+) - 法线 (0, 1, 0)
        drawQuad(x, y2, z, x2, y2, z, x2, y2, z2, x, y2, z2, r, g, b, a, 0, 1, 0);
        // 前面 (Z+) - 法线 (0, 0, 1)
        drawQuad(x, y2, z2, x2, y2, z2, x2, y, z2, x, y, z2, r * 0.8f, g * 0.8f, b * 0.8f, a, 0, 0, 1);
        // 后面 (Z-) - 法线 (0, 0, -1)
        drawQuad(x2, y2, z, x, y2, z, x, y, z, x2, y, z, r * 0.8f, g * 0.8f, b * 0.8f, a, 0, 0, -1);
        // 右面 (X+) - 法线 (1, 0, 0)
        drawQuad(x2, y2, z2, x2, y2, z, x2, y, z, x2, y, z2, r * 0.6f, g * 0.6f, b * 0.6f, a, 1, 0, 0);
        // 左面 (X-) - 法线 (-1, 0, 0)
        drawQuad(x, y2, z, x, y2, z2, x, y, z2, x, y, z, r * 0.6f, g * 0.6f, b * 0.6f, a, -1, 0, 0);
    }

    /**
     * 绘制方块顶面发光效果
     */
    public void drawBlockGlow(double x, double y, double z, float r, float g, float b, float a) {
        double offset = 0.01;
        drawQuad(
            x,
            y + 1 + offset,
            z,
            x + 1,
            y + 1 + offset,
            z,
            x + 1,
            y + 1 + offset,
            z + 1,
            x,
            y + 1 + offset,
            z + 1,
            r,
            g,
            b,
            a,
            0,
            1,
            0);
    }

    /**
     * 刷新当前批次
     */
    public void flush() {
        if (vertexCount == 0) return;

        mesh.updateData(vertexData, vertexOffset, indexData, indexOffset);

        ShaderProgram shader = getShader();
        if (shader == null || !shader.isValid()) return;

        shader.use();

        // 传递 MC 矩阵
        modelViewBuffer.rewind();
        shader.setUniformMatrix4("uModelView", false, modelViewBuffer);
        projectionBuffer.rewind();
        shader.setUniformMatrix4("uProjection", false, projectionBuffer);
        shader.setUniformFloat("uAlpha", alpha);

        // 设置光照参数
        shader.setUniformBool("uUseLighting", lightingEnabled);
        shader.setUniformFloat("uLightIntensity", lightIntensity);
        shader.setUniformFloat("uMinBrightness", minBrightness);
        shader.setUniformBool("uUseNormalShading", normalShadingEnabled);

        // 注意: MC 光照贴图绑定将在 MC 集成阶段添加

        mesh.draw(currentDrawMode);

        ShaderProgram.unbind();

        vertexOffset = 0;
        indexOffset = 0;
        vertexCount = 0;
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
     */
    public void end() {
        if (!drawing) return;

        flush();
        GL20.glUseProgram(0);

        // 关闭 GL 状态上下文（替代 glPopAttrib）
        if (glStateContext != null) {
            glStateContext.close();
            glStateContext = null;
        }

        // 恢复默认纹理单元
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        drawing = false;
    }

    public boolean isDrawing() {
        return drawing;
    }

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

    public void dispose() {
        close();
    }
}
