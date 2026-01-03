package moe.takochan.takorender.core.debug;

import java.nio.FloatBuffer;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

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

    /** 当前调试模式 */
    private DebugMode mode = DebugMode.NONE;

    /** Line shader 句柄 */
    private ResourceHandle<ShaderProgram> lineShader;

    /** 矩阵缓冲区 */
    private final FloatBuffer viewProjBuffer = BufferUtils.createFloatBuffer(16);

    /** 临时矩阵 */
    private final Matrix4f tempMatrix = new Matrix4f();

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
    }

    @Override
    public void onDestroy() {
        if (lineShader != null) {
            lineShader.release();
            lineShader = null;
        }
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
                    drawAABB(worldBounds);
                }
            }
        }

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

                // 绘制一个小立方体表示 LOD 级别
                Vector3f pos = transform.getPositionRef();
                drawPoint(pos, 0.5f);
            }
        }

        ShaderProgram.unbind();
    }

    /**
     * 绘制 AABB 线框
     */
    private void drawAABB(AABB aabb) {
        Vector3f min = aabb.getMin();
        Vector3f max = aabb.getMax();

        float x0 = min.x, y0 = min.y, z0 = min.z;
        float x1 = max.x, y1 = max.y, z1 = max.z;

        GL11.glBegin(GL11.GL_LINES);

        // 底面
        GL11.glVertex3f(x0, y0, z0);
        GL11.glVertex3f(x1, y0, z0);
        GL11.glVertex3f(x1, y0, z0);
        GL11.glVertex3f(x1, y0, z1);
        GL11.glVertex3f(x1, y0, z1);
        GL11.glVertex3f(x0, y0, z1);
        GL11.glVertex3f(x0, y0, z1);
        GL11.glVertex3f(x0, y0, z0);

        // 顶面
        GL11.glVertex3f(x0, y1, z0);
        GL11.glVertex3f(x1, y1, z0);
        GL11.glVertex3f(x1, y1, z0);
        GL11.glVertex3f(x1, y1, z1);
        GL11.glVertex3f(x1, y1, z1);
        GL11.glVertex3f(x0, y1, z1);
        GL11.glVertex3f(x0, y1, z1);
        GL11.glVertex3f(x0, y1, z0);

        // 垂直边
        GL11.glVertex3f(x0, y0, z0);
        GL11.glVertex3f(x0, y1, z0);
        GL11.glVertex3f(x1, y0, z0);
        GL11.glVertex3f(x1, y1, z0);
        GL11.glVertex3f(x1, y0, z1);
        GL11.glVertex3f(x1, y1, z1);
        GL11.glVertex3f(x0, y0, z1);
        GL11.glVertex3f(x0, y1, z1);

        GL11.glEnd();
    }

    /**
     * 绘制点（小立方体）
     */
    private void drawPoint(Vector3f pos, float size) {
        float half = size * 0.5f;
        float x0 = pos.x - half, y0 = pos.y - half, z0 = pos.z - half;
        float x1 = pos.x + half, y1 = pos.y + half, z1 = pos.z + half;

        GL11.glBegin(GL11.GL_LINES);

        // 简化：只画交叉线
        GL11.glVertex3f(x0, pos.y, pos.z);
        GL11.glVertex3f(x1, pos.y, pos.z);
        GL11.glVertex3f(pos.x, y0, pos.z);
        GL11.glVertex3f(pos.x, y1, pos.z);
        GL11.glVertex3f(pos.x, pos.y, z0);
        GL11.glVertex3f(pos.x, pos.y, z1);

        GL11.glEnd();
    }

    /**
     * 查找活动相机
     */
    private Entity findActiveCamera() {
        for (Entity entity : getWorld().getEntitiesWith(CameraComponent.class)) {
            CameraComponent camera = entity.getComponent(CameraComponent.class)
                .orElse(null);
            if (camera != null && camera.isActive()) {
                return entity;
            }
        }
        return null;
    }
}
