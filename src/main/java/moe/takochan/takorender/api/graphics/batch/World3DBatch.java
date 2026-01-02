package moe.takochan.takorender.api.graphics.batch;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.graphics.mesh.DynamicMesh;
import moe.takochan.takorender.api.graphics.mesh.VertexAttribute;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.api.graphics.shader.ShaderType;
import moe.takochan.takorender.core.gl.GLStateContext;

/**
 * 3D 世界批量渲染器，用于在 MC 世界中渲染图元。
 *
 * <p>
 * 直接使用 MC 的 ModelView/Projection 矩阵。
 * 坐标相对于玩家眼睛位置。
 * </p>
 *
 * <p>
 * <b>状态管理契约</b>: 本类使用 GLStateContext 自动管理 GL 状态。
 * </p>
 * <ul>
 * <li>begin() 通过 GLStateContext 保存当前 GL 状态</li>
 * <li>end() 自动恢复之前的 GL 状态</li>
 * <li>调用者不应在 begin/end 之外使用 glPushAttrib</li>
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
 *     double rx = blockX - playerEyeX;
 *     double ry = blockY - playerEyeY;
 *     double rz = blockZ - playerEyeZ;
 *
 *     World3DBatch batch = new World3DBatch();
 *     batch.begin(GL11.GL_LINES); // 自动保存状态
 *     batch.drawWireBox(rx, ry, rz, 1, 1, 1, r, g, b, a);
 *     batch.end(); // 自动恢复状态
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class World3DBatch implements AutoCloseable {

    /** 每顶点浮点数: 3(位置) + 4(颜色) = 7 */
    private static final int FLOATS_PER_VERTEX = 7;
    /** 默认最大顶点数 */
    private static final int DEFAULT_MAX_VERTICES = 8192;

    /** 顶点格式: 位置(3) + 颜色(4) */
    private static final VertexAttribute[] ATTRIBUTES = { VertexAttribute.position3D(0),
        VertexAttribute.colorFloat(12) };
    private static final int STRIDE = FLOATS_PER_VERTEX * 4; // 28 字节

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

    /** GL 状态上下文（用于自动状态管理） */
    private GLStateContext glStateContext = null;

    public World3DBatch() {
        this(DEFAULT_MAX_VERTICES);
    }

    public World3DBatch(int maxVertices) {
        this.maxVertices = maxVertices;
        this.vertexData = new float[maxVertices * FLOATS_PER_VERTEX];
        this.indexData = new int[maxVertices];
        this.mesh = new DynamicMesh(maxVertices, maxVertices, STRIDE, ATTRIBUTES);
    }

    /**
     * 设置全局透明度
     */
    public void setAlpha(float alpha) {
        this.alpha = alpha;
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
            throw new IllegalStateException("World3DBatch.end() must be called before begin()");
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

        beginInternal(drawMode);
    }

    /**
     * 开始批量渲染（使用自定义矩阵）
     *
     * <p>
     * 适用于 ECS 系统等需要自定义相机矩阵的场景。
     * </p>
     *
     * @param drawMode   GL_LINES、GL_TRIANGLES、GL_QUADS 等
     * @param viewMatrix 视图矩阵（来自 CameraComponent）
     * @param projMatrix 投影矩阵（来自 CameraComponent）
     */
    public void begin(int drawMode, Matrix4f viewMatrix, Matrix4f projMatrix) {
        if (drawing) {
            throw new IllegalStateException("World3DBatch.end() must be called before begin()");
        }

        if (!ShaderProgram.isSupported()) {
            return;
        }

        this.currentDrawMode = drawMode;

        // 使用自定义矩阵
        modelViewBuffer.clear();
        viewMatrix.get(modelViewBuffer);
        projectionBuffer.clear();
        projMatrix.get(projectionBuffer);

        beginInternal(drawMode);
    }

    /**
     * 内部初始化逻辑（由 begin 方法调用）
     */
    private void beginInternal(int drawMode) {
        // 开始 GL 状态上下文（替代 glPushAttrib）
        glStateContext = GLStateContext.begin();

        // 通过 GLStateContext 设置渲染状态
        glStateContext.enableBlend();
        glStateContext.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        glStateContext.disableTexture2D();
        glStateContext.disableLighting();
        glStateContext.disableCullFace();
        glStateContext.setDepthMask(false);
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
     * 添加顶点
     */
    private int addVertex(double x, double y, double z, float r, float g, float b, float a) {
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

        return vertexCount++;
    }

    /**
     * 添加索引
     */
    private void addIndex(int index) {
        indexData[indexOffset++] = index;
    }

    /**
     * 绘制线段
     */
    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b,
        float a) {
        if (!drawing) return;

        int v0 = addVertex(x1, y1, z1, r, g, b, a);
        int v1 = addVertex(x2, y2, z2, r, g, b, a);
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
     * 绘制圆形（XZ 平面）
     */
    public void drawCircleXZ(double cx, double cy, double cz, double radius, int segments, float r, float g, float b,
        float a) {
        double angleStep = Math.PI * 2 / segments;
        double prevX = cx + radius, prevZ = cz;

        for (int i = 1; i <= segments; i++) {
            double angle = angleStep * i;
            double currX = cx + Math.cos(angle) * radius;
            double currZ = cz + Math.sin(angle) * radius;
            drawLine(prevX, cy, prevZ, currX, cy, currZ, r, g, b, a);
            prevX = currX;
            prevZ = currZ;
        }
    }

    /**
     * 绘制线框球体
     *
     * @param cx       中心 X
     * @param cy       中心 Y
     * @param cz       中心 Z
     * @param radius   半径
     * @param segments 分段数
     * @param r        红色分量
     * @param g        绿色分量
     * @param b        蓝色分量
     * @param a        透明度
     */
    public void drawWireSphere(double cx, double cy, double cz, double radius, int segments, float r, float g, float b,
        float a) {
        // 水平圆环（赤道）
        drawCircleXZ(cx, cy, cz, radius, segments, r, g, b, a);

        // 垂直圆环（XY 平面）
        double angleStep = Math.PI * 2 / segments;
        double prevX = cx + radius, prevY = cy;
        for (int i = 1; i <= segments; i++) {
            double angle = angleStep * i;
            double currX = cx + Math.cos(angle) * radius;
            double currY = cy + Math.sin(angle) * radius;
            drawLine(prevX, prevY, cz, currX, currY, cz, r, g, b, a);
            prevX = currX;
            prevY = currY;
        }

        // 垂直圆环（YZ 平面）
        prevY = cy + radius;
        double prevZ = cz;
        for (int i = 1; i <= segments; i++) {
            double angle = angleStep * i;
            double currY = cy + Math.cos(angle) * radius;
            double currZ = cz + Math.sin(angle) * radius;
            drawLine(cx, prevY, prevZ, cx, currY, currZ, r, g, b, a);
            prevY = currY;
            prevZ = currZ;
        }
    }

    /**
     * 绘制实心四边形（需要 GL_TRIANGLES 模式）
     */
    public void drawQuad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3,
        double z3, double x4, double y4, double z4, float r, float g, float b, float a) {
        if (!drawing) return;

        int v0 = addVertex(x1, y1, z1, r, g, b, a);
        int v1 = addVertex(x2, y2, z2, r, g, b, a);
        int v2 = addVertex(x3, y3, z3, r, g, b, a);
        int v3 = addVertex(x4, y4, z4, r, g, b, a);

        // 两个三角形
        addIndex(v0);
        addIndex(v1);
        addIndex(v2);
        addIndex(v0);
        addIndex(v2);
        addIndex(v3);
    }

    /**
     * 绘制实心盒子
     */
    public void drawSolidBox(double x, double y, double z, double w, double h, double d, float r, float g, float b,
        float a) {
        double x2 = x + w, y2 = y + h, z2 = z + d;

        // 底面 (Y-)
        drawQuad(x, y, z, x2, y, z, x2, y, z2, x, y, z2, r * 0.5f, g * 0.5f, b * 0.5f, a);
        // 顶面 (Y+)
        drawQuad(x, y2, z2, x2, y2, z2, x2, y2, z, x, y2, z, r, g, b, a);
        // 前面 (Z+)
        drawQuad(x, y, z2, x2, y, z2, x2, y2, z2, x, y2, z2, r * 0.8f, g * 0.8f, b * 0.8f, a);
        // 后面 (Z-)
        drawQuad(x2, y, z, x, y, z, x, y2, z, x2, y2, z, r * 0.8f, g * 0.8f, b * 0.8f, a);
        // 右面 (X+)
        drawQuad(x2, y, z2, x2, y, z, x2, y2, z, x2, y2, z2, r * 0.6f, g * 0.6f, b * 0.6f, a);
        // 左面 (X-)
        drawQuad(x, y, z, x, y, z2, x, y2, z2, x, y2, z, r * 0.6f, g * 0.6f, b * 0.6f, a);
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
            a);
    }

    /**
     * 绘制螺旋效果
     */
    public void drawSpiral(double cx, double y, double cz, double radius, double height, int turns, int segments,
        float r, float g, float b, float a) {
        double totalAngle = Math.PI * 2 * turns;
        double angleStep = totalAngle / segments;
        double heightStep = height / segments;

        double prevX = cx + radius, prevY = y, prevZ = cz;

        for (int i = 1; i <= segments; i++) {
            double angle = angleStep * i;
            double currX = cx + Math.cos(angle) * radius;
            double currY = y + heightStep * i;
            double currZ = cz + Math.sin(angle) * radius;

            drawLine(prevX, prevY, prevZ, currX, currY, currZ, r, g, b, a);

            prevX = currX;
            prevY = currY;
            prevZ = currZ;
        }
    }

    /**
     * 刷新当前批次
     */
    public void flush() {
        if (vertexCount == 0) return;

        mesh.updateData(vertexData, vertexOffset, indexData, indexOffset);

        ShaderProgram shader = ShaderType.WORLD_3D.get();
        if (shader == null || !shader.isValid()) return;

        shader.use();

        modelViewBuffer.rewind();
        shader.setUniformMatrix4("uModelView", false, modelViewBuffer);
        projectionBuffer.rewind();
        shader.setUniformMatrix4("uProjection", false, projectionBuffer);
        shader.setUniformFloat("uAlpha", alpha);

        mesh.draw(currentDrawMode);

        ShaderProgram.unbind();

        vertexOffset = 0;
        indexOffset = 0;
        vertexCount = 0;
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

        drawing = false;
    }

    public boolean isDrawing() {
        return drawing;
    }

    /**
     * 设置线宽（在 begin/end 之间调用）
     *
     * @param width 线宽
     */
    public void setLineWidth(float width) {
        if (drawing && glStateContext != null) {
            glStateContext.setLineWidth(width);
        }
    }

    /**
     * 设置深度测试状态（在 begin/end 之间调用）
     *
     * @param enabled 是否启用深度测试
     */
    public void setDepthTest(boolean enabled) {
        if (drawing && glStateContext != null) {
            if (enabled) {
                glStateContext.enableDepthTest();
            } else {
                glStateContext.disableDepthTest();
            }
        }
    }

    /**
     * 设置颜色（影响后续添加的顶点）
     * <p>
     * 注意：World3DBatch 使用逐顶点颜色，此方法只是便捷接口。
     * 实际颜色由 drawLine/drawQuad 等方法的颜色参数决定。
     * </p>
     */
    public void setColor(float r, float g, float b, float a) {
        // 预留接口，当前实现使用逐顶点颜色
    }

    @Override
    public void close() {
        if (!disposed) {
            if (glStateContext != null) {
                glStateContext.close();
                glStateContext = null;
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
