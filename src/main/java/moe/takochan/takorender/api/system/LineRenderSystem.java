package moe.takochan.takorender.api.system;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.LineRendererComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.core.gl.GLStateContext;

/**
 * 线条渲染系统 - 负责渲染所有拥有 LineRendererComponent 的实体
 *
 * <p>
 * LineRenderSystem 在 RENDER 阶段执行，使用 OpenGL 即时模式渲染线条。
 * 适用于调试可视化、边界框显示等场景。
 * </p>
 *
 * <p>
 * <b>支持的形状</b>:
 * </p>
 * <ul>
 * <li>LINE: 单条线段</li>
 * <li>BOX: 线框立方体（12 条边）</li>
 * <li>SPHERE: 线框球体（经纬线）</li>
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
 *     World world = new World();
 *     world.addSystem(new CameraSystem());
 *     world.addSystem(new LineRenderSystem());
 *
 *     // 创建调试边界框
 *     Entity debugBox = world.createEntity();
 *     debugBox.addComponent(new TransformComponent(0, 64, 0));
 *     debugBox.addComponent(
 *         new LineRendererComponent().setShape(LineShape.BOX)
 *             .setSize(1.0f)
 *             .setColor(1.0f, 0.0f, 0.0f, 1.0f));
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent(LineRendererComponent.class)
public class LineRenderSystem extends GameSystem {

    @Override
    public Phase getPhase() {
        return Phase.RENDER;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void update(float deltaTime) {
        Entity cameraEntity = findActiveCamera();
        if (cameraEntity == null) {
            return;
        }

        CameraComponent camera = cameraEntity.getComponent(CameraComponent.class)
            .orElse(null);
        if (camera == null) {
            return;
        }

        List<Entity> entities = getRequiredEntities();
        if (entities.isEmpty()) {
            return;
        }

        try (GLStateContext ctx = GLStateContext.begin()) {
            ctx.disableTexture2D();
            ctx.enableBlend();
            ctx.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            setupMatrices(camera);

            for (Entity entity : entities) {
                renderLineEntity(ctx, entity);
            }

            restoreMatrices();
        }
    }

    /**
     * 设置投影和视图矩阵
     */
    private void setupMatrices(CameraComponent camera) {
        Matrix4f projection = camera.getProjectionMatrix();
        Matrix4f view = camera.getViewMatrix();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        loadMatrix(projection);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        loadMatrix(view);
    }

    /**
     * 恢复矩阵状态
     */
    private void restoreMatrices() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    /**
     * 加载 JOML 矩阵到 OpenGL
     */
    private void loadMatrix(Matrix4f matrix) {
        float[] data = new float[16];
        matrix.get(data);

        java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
        buffer.put(data);
        buffer.flip();

        GL11.glLoadMatrix(buffer);
    }

    /**
     * 渲染单个线条实体
     */
    private void renderLineEntity(GLStateContext ctx, Entity entity) {
        LineRendererComponent line = entity.getComponent(LineRendererComponent.class)
            .orElse(null);
        TransformComponent transform = entity.getComponent(TransformComponent.class)
            .orElse(null);

        if (line == null || transform == null || !line.isVisible()) {
            return;
        }

        if (line.isDepthTest()) {
            ctx.enableDepthTest();
        } else {
            ctx.disableDepthTest();
        }

        ctx.setLineWidth(line.getLineWidth());
        GL11.glColor4f(line.getColorR(), line.getColorG(), line.getColorB(), line.getColorA());

        GL11.glPushMatrix();
        loadMatrix(transform.getWorldMatrix());

        switch (line.getShape()) {
            case LINE:
                renderLine(line);
                break;
            case BOX:
                renderBox(line);
                break;
            case SPHERE:
                renderSphere(line);
                break;
            default:
                break;
        }

        GL11.glPopMatrix();
    }

    /**
     * 渲染单条线段
     */
    private void renderLine(LineRendererComponent line) {
        Vector3f start = line.getStartPoint();
        Vector3f end = line.getEndPoint();

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3f(start.x, start.y, start.z);
        GL11.glVertex3f(end.x, end.y, end.z);
        GL11.glEnd();
    }

    /**
     * 渲染线框立方体
     */
    private void renderBox(LineRendererComponent line) {
        Vector3f size = line.getSize();
        float hw = size.x / 2;
        float hh = size.y / 2;
        float hd = size.z / 2;

        GL11.glBegin(GL11.GL_LINES);

        // 底面
        GL11.glVertex3f(-hw, -hh, -hd);
        GL11.glVertex3f(hw, -hh, -hd);
        GL11.glVertex3f(hw, -hh, -hd);
        GL11.glVertex3f(hw, -hh, hd);
        GL11.glVertex3f(hw, -hh, hd);
        GL11.glVertex3f(-hw, -hh, hd);
        GL11.glVertex3f(-hw, -hh, hd);
        GL11.glVertex3f(-hw, -hh, -hd);

        // 顶面
        GL11.glVertex3f(-hw, hh, -hd);
        GL11.glVertex3f(hw, hh, -hd);
        GL11.glVertex3f(hw, hh, -hd);
        GL11.glVertex3f(hw, hh, hd);
        GL11.glVertex3f(hw, hh, hd);
        GL11.glVertex3f(-hw, hh, hd);
        GL11.glVertex3f(-hw, hh, hd);
        GL11.glVertex3f(-hw, hh, -hd);

        // 竖边
        GL11.glVertex3f(-hw, -hh, -hd);
        GL11.glVertex3f(-hw, hh, -hd);
        GL11.glVertex3f(hw, -hh, -hd);
        GL11.glVertex3f(hw, hh, -hd);
        GL11.glVertex3f(hw, -hh, hd);
        GL11.glVertex3f(hw, hh, hd);
        GL11.glVertex3f(-hw, -hh, hd);
        GL11.glVertex3f(-hw, hh, hd);

        GL11.glEnd();
    }

    /**
     * 渲染线框球体
     */
    private void renderSphere(LineRendererComponent line) {
        Vector3f size = line.getSize();
        float radius = Math.min(size.x, Math.min(size.y, size.z)) / 2;

        int segments = 16;

        // 水平圆环（赤道）
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            GL11.glVertex3f((float) Math.cos(angle) * radius, 0, (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();

        // 垂直圆环（经线）
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            GL11.glVertex3f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, 0);
        }
        GL11.glEnd();

        // 另一条垂直圆环
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            GL11.glVertex3f(0, (float) Math.sin(angle) * radius, (float) Math.cos(angle) * radius);
        }
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
