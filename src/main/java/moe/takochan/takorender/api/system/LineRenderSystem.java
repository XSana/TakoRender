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
import moe.takochan.takorender.api.graphics.batch.World3DBatch;

/**
 * 线条渲染系统 - 负责渲染所有拥有 LineRendererComponent 的实体
 *
 * <p>
 * LineRenderSystem 在 RENDER 阶段执行，使用 World3DBatch 进行批量渲染。
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

    /** 球体渲染时的默认分段数 */
    private static final int SPHERE_SEGMENTS = 16;

    /** 批量渲染器（复用实例避免重复分配） */
    private World3DBatch batch;

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

        // 延迟初始化批量渲染器
        if (batch == null) {
            batch = new World3DBatch();
        }

        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projMatrix = camera.getProjectionMatrix();

        // 开始批量渲染（使用相机矩阵）
        batch.begin(GL11.GL_LINES, viewMatrix, projMatrix);

        for (Entity entity : entities) {
            renderLineEntity(entity);
        }

        batch.end();
    }

    /**
     * 渲染单个线条实体
     */
    private void renderLineEntity(Entity entity) {
        LineRendererComponent line = entity.getComponent(LineRendererComponent.class)
            .orElse(null);
        TransformComponent transform = entity.getComponent(TransformComponent.class)
            .orElse(null);

        if (line == null || transform == null || !line.isVisible()) {
            return;
        }

        // 设置深度测试和线宽
        batch.setDepthTest(line.isDepthTest());
        batch.setLineWidth(line.getLineWidth());

        // 获取世界坐标
        Matrix4f worldMatrix = transform.getWorldMatrix();
        Vector3f position = new Vector3f();
        worldMatrix.getTranslation(position);

        float r = line.getColorR();
        float g = line.getColorG();
        float b = line.getColorB();
        float a = line.getColorA();

        switch (line.getShape()) {
            case LINE:
                renderLine(line, position, r, g, b, a);
                break;
            case BOX:
                renderBox(line, position, r, g, b, a);
                break;
            case SPHERE:
                renderSphere(line, position, r, g, b, a);
                break;
            default:
                break;
        }
    }

    /**
     * 渲染单条线段
     */
    private void renderLine(LineRendererComponent line, Vector3f position, float r, float g, float b, float a) {
        Vector3f start = line.getStartPoint();
        Vector3f end = line.getEndPoint();

        batch.drawLine(
            position.x + start.x,
            position.y + start.y,
            position.z + start.z,
            position.x + end.x,
            position.y + end.y,
            position.z + end.z,
            r,
            g,
            b,
            a);
    }

    /**
     * 渲染线框立方体
     */
    private void renderBox(LineRendererComponent line, Vector3f position, float r, float g, float b, float a) {
        Vector3f size = line.getSize();
        float hw = size.x / 2;
        float hh = size.y / 2;
        float hd = size.z / 2;

        // 使用 World3DBatch 的 drawWireBox
        batch.drawWireBox(position.x - hw, position.y - hh, position.z - hd, size.x, size.y, size.z, r, g, b, a);
    }

    /**
     * 渲染线框球体
     */
    private void renderSphere(LineRendererComponent line, Vector3f position, float r, float g, float b, float a) {
        Vector3f size = line.getSize();
        float radius = Math.min(size.x, Math.min(size.y, size.z)) / 2;

        batch.drawWireSphere(position.x, position.y, position.z, radius, SPHERE_SEGMENTS, r, g, b, a);
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

    @Override
    public void onDestroy() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
    }
}
