package moe.takochan.takorender.core.debug;

import java.nio.FloatBuffer;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.BoundsComponent;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.LODComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.component.VisibilityComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.graphics.AABB;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.api.resource.ResourceHandle;
import moe.takochan.takorender.api.resource.ShaderManager;
import moe.takochan.takorender.core.gl.GLStateContext;

/**
 * 调试渲染系统
 *
 * <p>
 * DebugRenderSystem 提供多种调试可视化模式，帮助开发者调试渲染问题。
 * </p>
 *
 * <p>
 * <b>支持的模式</b>:
 * </p>
 * <ul>
 * <li>WIREFRAME - 线框模式</li>
 * <li>BOUNDING_BOX - 显示包围盒</li>
 * <li>DEPTH - 深度可视化</li>
 * <li>NORMALS - 法线可视化</li>
 * <li>LOD_LEVEL - LOD 级别颜色</li>
 * </ul>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     DebugRenderSystem debug = world.getSystem(DebugRenderSystem.class);
 *     debug.setMode(DebugMode.BOUNDING_BOX);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class DebugRenderSystem extends GameSystem {

    /** AABB 线框顶点数（12 条边 * 2 顶点） */
    private static final int AABB_LINE_VERTICES = 24;

    /** 每顶点 float 数（xyz） */
    private static final int FLOATS_PER_VERTEX = 3;

    /** 初始缓冲容量（实体数） */
    private static final int INITIAL_CAPACITY = 64;

    /** 当前调试模式 */
    private DebugMode mode = DebugMode.NONE;

    /** Line shader 句柄 */
    private ResourceHandle<ShaderProgram> lineShader;

    /** 矩阵缓冲区 */
    private final FloatBuffer viewProjBuffer = BufferUtils.createFloatBuffer(16);

    /** 临时矩阵 */
    private final Matrix4f tempMatrix = new Matrix4f();

    /** VAO */
    private int vao;

    /** VBO */
    private int vbo;

    /** CPU 端顶点缓冲 */
    private FloatBuffer vertexBuffer;

    /** 当前缓冲容量（实体数） */
    private int bufferCapacity;

    /** 当前顶点数量 */
    private int vertexCount;

    /** LOD 级别颜色 */
    private static final float[][] LOD_COLORS = { { 0.0f, 1.0f, 0.0f }, // LOD 0: 绿色（最高精度）
        { 1.0f, 1.0f, 0.0f }, // LOD 1: 黄色
        { 1.0f, 0.5f, 0.0f }, // LOD 2: 橙色
        { 1.0f, 0.0f, 0.0f }, // LOD 3: 红色（最低精度）
        { 0.5f, 0.0f, 0.5f }, // LOD 4+: 紫色
    };

    @Override
    public Phase getPhase() {
        return Phase.RENDER;
    }

    @Override
    public int getPriority() {
        // 在其他渲染系统之后执行
        return 1000;
    }

    @Override
    public void onInit() {
        lineShader = ShaderManager.instance()
            .get(ShaderManager.SHADER_LINE);

        // 创建 VAO/VBO
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // 顶点属性: location 0 = position (vec3)
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);

        GL30.glBindVertexArray(0);

        // 初始化 CPU 缓冲
        bufferCapacity = INITIAL_CAPACITY;
        vertexBuffer = BufferUtils.createFloatBuffer(bufferCapacity * AABB_LINE_VERTICES * FLOATS_PER_VERTEX);

        // 初始化 GPU 缓冲
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(
            GL15.GL_ARRAY_BUFFER,
            (long) bufferCapacity * AABB_LINE_VERTICES * FLOATS_PER_VERTEX * Float.BYTES,
            GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void onDestroy() {
        if (lineShader != null) {
            lineShader.release();
            lineShader = null;
        }
        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
            vao = 0;
        }
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
            vbo = 0;
        }
        vertexBuffer = null;
    }

    @Override
    public void update(float deltaTime) {
        if (mode == DebugMode.NONE) {
            return;
        }

        // 获取活动相机
        Entity cameraEntity = findActiveCamera();
        if (cameraEntity == null) {
            return;
        }

        CameraComponent camera = cameraEntity.getComponent(CameraComponent.class)
            .orElse(null);
        if (camera == null) {
            return;
        }

        // 缓存 ViewProjection 矩阵
        tempMatrix.set(camera.getViewProjectionMatrix());
        viewProjBuffer.clear();
        tempMatrix.get(viewProjBuffer);

        try (GLStateContext ctx = GLStateContext.begin()) {
            switch (mode) {
                case WIREFRAME:
                    renderWireframe(ctx);
                    break;
                case BOUNDING_BOX:
                    renderBoundingBoxes(ctx);
                    break;
                case LOD_LEVEL:
                    renderLODLevels(ctx);
                    break;
                case DEPTH:
                case NORMALS:
                    // 需要特殊 shader，暂不实现
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 设置调试模式
     */
    public void setMode(DebugMode mode) {
        this.mode = mode != null ? mode : DebugMode.NONE;
    }

    /**
     * 获取当前调试模式
     */
    public DebugMode getMode() {
        return mode;
    }

    /**
     * 切换到下一个调试模式
     */
    public void cycleMode() {
        DebugMode[] modes = DebugMode.values();
        int nextIndex = (mode.ordinal() + 1) % modes.length;
        mode = modes[nextIndex];
    }

    /**
     * 渲染线框模式
     */
    private void renderWireframe(GLStateContext ctx) {
        ctx.setPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
    }

    /**
     * 渲染包围盒
     */
    private void renderBoundingBoxes(GLStateContext ctx) {
        if (lineShader == null || !lineShader.isValid()) {
            return;
        }

        ShaderProgram shader = lineShader.get();
        shader.use();

        viewProjBuffer.rewind();
        shader.setUniformMatrix4("uViewProjection", false, viewProjBuffer);
        shader.setUniformVec4("uColor", 0.0f, 1.0f, 0.0f, 1.0f);

        ctx.disableDepthTest();
        ctx.enableBlend();
        ctx.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 开始收集顶点
        beginBatch();

        // 遍历所有有 BoundsComponent 的实体
        List<Entity> entities = getWorld().getEntitiesWith(BoundsComponent.class);
        for (Entity entity : entities) {
            VisibilityComponent visibility = entity.getComponent(VisibilityComponent.class)
                .orElse(null);
            if (visibility != null && !visibility.shouldRender()) {
                continue;
            }

            BoundsComponent bounds = entity.getComponent(BoundsComponent.class)
                .orElse(null);
            if (bounds != null) {
                AABB worldBounds = bounds.getWorldBounds();
                if (worldBounds != null && worldBounds.isValid()) {
                    addAABB(worldBounds);
                }
            }
        }

        // 提交并渲染
        endBatchAndRender();

        ShaderProgram.unbind();
    }

    /**
     * 渲染 LOD 级别颜色
     */
    private void renderLODLevels(GLStateContext ctx) {
        if (lineShader == null || !lineShader.isValid()) {
            return;
        }

        ShaderProgram shader = lineShader.get();
        shader.use();

        viewProjBuffer.rewind();
        shader.setUniformMatrix4("uViewProjection", false, viewProjBuffer);

        ctx.disableDepthTest();
        ctx.enableBlend();
        ctx.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        List<Entity> entities = getWorld().getEntitiesWith(LODComponent.class, TransformComponent.class);
        for (Entity entity : entities) {
            VisibilityComponent visibility = entity.getComponent(VisibilityComponent.class)
                .orElse(null);
            if (visibility != null && !visibility.shouldRender()) {
                continue;
            }

            LODComponent lod = entity.getComponent(LODComponent.class)
                .orElse(null);
            TransformComponent transform = entity.getComponent(TransformComponent.class)
                .orElse(null);

            if (lod != null && transform != null) {
                int level = Math.min(lod.getActiveLevel(), LOD_COLORS.length - 1);
                float[] color = LOD_COLORS[level];
                shader.setUniformVec4("uColor", color[0], color[1], color[2], 0.7f);

                // 开始收集顶点
                beginBatch();

                // 绘制一个小立方体表示 LOD 级别
                Vector3f pos = transform.getPositionRef();
                addCrossLines(pos, 0.5f);

                // 提交并渲染
                endBatchAndRender();
            }
        }

        ShaderProgram.unbind();
    }

    /**
     * 开始批量收集顶点
     */
    private void beginBatch() {
        vertexBuffer.clear();
        vertexCount = 0;
    }

    /**
     * 添加 AABB 线框到批量缓冲
     */
    private void addAABB(AABB aabb) {
        ensureCapacity(1);

        Vector3f min = aabb.getMin();
        Vector3f max = aabb.getMax();

        float x0 = min.x, y0 = min.y, z0 = min.z;
        float x1 = max.x, y1 = max.y, z1 = max.z;

        // 底面 4 条边
        addLine(x0, y0, z0, x1, y0, z0);
        addLine(x1, y0, z0, x1, y0, z1);
        addLine(x1, y0, z1, x0, y0, z1);
        addLine(x0, y0, z1, x0, y0, z0);

        // 顶面 4 条边
        addLine(x0, y1, z0, x1, y1, z0);
        addLine(x1, y1, z0, x1, y1, z1);
        addLine(x1, y1, z1, x0, y1, z1);
        addLine(x0, y1, z1, x0, y1, z0);

        // 垂直边 4 条
        addLine(x0, y0, z0, x0, y1, z0);
        addLine(x1, y0, z0, x1, y1, z0);
        addLine(x1, y0, z1, x1, y1, z1);
        addLine(x0, y0, z1, x0, y1, z1);
    }

    /**
     * 添加十字线到批量缓冲（用于标记点）
     */
    private void addCrossLines(Vector3f pos, float size) {
        ensureCapacity(1);

        float half = size * 0.5f;

        // X 轴
        addLine(pos.x - half, pos.y, pos.z, pos.x + half, pos.y, pos.z);
        // Y 轴
        addLine(pos.x, pos.y - half, pos.z, pos.x, pos.y + half, pos.z);
        // Z 轴
        addLine(pos.x, pos.y, pos.z - half, pos.x, pos.y, pos.z + half);
    }

    /**
     * 添加一条线段
     */
    private void addLine(float x0, float y0, float z0, float x1, float y1, float z1) {
        vertexBuffer.put(x0)
            .put(y0)
            .put(z0);
        vertexBuffer.put(x1)
            .put(y1)
            .put(z1);
        vertexCount += 2;
    }

    /**
     * 确保缓冲区有足够容量
     */
    private void ensureCapacity(int additionalEntities) {
        int requiredVertices = vertexCount + additionalEntities * AABB_LINE_VERTICES;
        int requiredFloats = requiredVertices * FLOATS_PER_VERTEX;

        if (requiredFloats > vertexBuffer.capacity()) {
            // 扩容
            int newCapacity = Math.max(bufferCapacity * 2, (requiredVertices / AABB_LINE_VERTICES) + 1);
            bufferCapacity = newCapacity;

            FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity * AABB_LINE_VERTICES * FLOATS_PER_VERTEX);

            // 复制现有数据
            vertexBuffer.flip();
            newBuffer.put(vertexBuffer);

            vertexBuffer = newBuffer;

            // 重新分配 GPU 缓冲
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(
                GL15.GL_ARRAY_BUFFER,
                (long) newCapacity * AABB_LINE_VERTICES * FLOATS_PER_VERTEX * Float.BYTES,
                GL15.GL_DYNAMIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }
    }

    /**
     * 结束批量收集并渲染
     */
    private void endBatchAndRender() {
        if (vertexCount == 0) {
            return;
        }

        vertexBuffer.flip();

        // 上传到 GPU
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        // 渲染
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_LINES, 0, vertexCount);
        GL30.glBindVertexArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 查找活动相机
     */
    private Entity findActiveCamera() {
        return getWorld().findActiveCamera();
    }
}
