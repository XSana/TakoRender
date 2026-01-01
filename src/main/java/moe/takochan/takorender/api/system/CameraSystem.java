package moe.takochan.takorender.api.system;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 相机系统 - 负责更新相机的投影矩阵和视图矩阵
 *
 * <p>
 * CameraSystem 在 UPDATE 阶段执行，处理所有拥有 {@link CameraComponent} 的实体。
 * </p>
 *
 * <p>
 * <b>主要功能</b>:
 * </p>
 * <ul>
 * <li>更新投影矩阵（透视/正交）</li>
 * <li>根据 TransformComponent 更新视图矩阵</li>
 * <li>计算视图投影矩阵（VP = P * V）</li>
 * <li>可选的 Minecraft 相机同步</li>
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
 *
 *     Entity camera = world.createEntity();
 *     camera.addComponent(new TransformComponent(0, 64, 0));
 *     camera.addComponent(
 *         new CameraComponent().setPerspective(70.0f, 16.0f / 9.0f, 0.05f, 256.0f)
 *             .setSyncWithMinecraft(true)
 *             .setActive(true));
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent(CameraComponent.class)
public class CameraSystem extends GameSystem {

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void update(float deltaTime) {
        List<Entity> entities = getRequiredEntities();

        for (Entity entity : entities) {
            CameraComponent camera = entity.getComponent(CameraComponent.class)
                .orElse(null);
            TransformComponent transform = entity.getComponent(TransformComponent.class)
                .orElse(null);

            if (camera == null || transform == null) {
                continue;
            }

            if (camera.isSyncWithMinecraft()) {
                syncFromMinecraft(camera, transform, deltaTime);
            }

            if (camera.isProjectionDirty()) {
                updateProjectionMatrix(camera);
            }

            if (camera.isViewDirty() || transform.isDirty()) {
                updateViewMatrix(camera, transform);
            }

            camera.getViewProjectionMatrix()
                .set(camera.getProjectionMatrix())
                .mul(camera.getViewMatrix());

            camera.clearDirtyFlags();
        }
    }

    /**
     * 更新投影矩阵
     */
    private void updateProjectionMatrix(CameraComponent camera) {
        Matrix4f proj = camera.getProjectionMatrix();

        if (camera.getProjectionType() == CameraComponent.ProjectionType.PERSPECTIVE) {
            proj.setPerspective(
                (float) Math.toRadians(camera.getVFov()),
                camera.getAspectRatio(),
                camera.getNearPlane(),
                camera.getFarPlane());
        } else {
            proj.setOrtho(
                camera.getOrthoLeft(),
                camera.getOrthoRight(),
                camera.getOrthoBottom(),
                camera.getOrthoTop(),
                camera.getNearPlane(),
                camera.getFarPlane());
        }
    }

    /**
     * 更新视图矩阵
     */
    private void updateViewMatrix(CameraComponent camera, TransformComponent transform) {
        Vector3f position = transform.getPosition();
        Vector3f forward = transform.getForward();
        Vector3f up = transform.getUp();

        Vector3f target = new Vector3f(position).add(forward);
        camera.getViewMatrix()
            .setLookAt(position, target, up);
    }

    /**
     * 从 Minecraft 玩家同步相机状态
     */
    private void syncFromMinecraft(CameraComponent camera, TransformComponent transform, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;

        if (player == null) {
            return;
        }

        double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks + player.getEyeHeight();
        double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;

        transform.setPosition(x, y, z);
        transform.setRotation(pitch, yaw, 0);

        int displayWidth = mc.displayWidth;
        int displayHeight = mc.displayHeight;

        if (displayWidth > 0 && displayHeight > 0) {
            camera.setViewport(0, 0, displayWidth, displayHeight);
            camera.setAspectRatio((float) displayWidth / displayHeight);
        }

        camera.markViewDirty();
    }

    /**
     * 获取当前活动的相机实体
     *
     * @return 活动的相机实体，如果没有则返回 null
     */
    public Entity getActiveCamera() {
        for (Entity entity : getRequiredEntities()) {
            CameraComponent camera = entity.getComponent(CameraComponent.class)
                .orElse(null);
            if (camera != null && camera.isActive()) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 获取当前活动的相机组件
     *
     * @return 活动的相机组件，如果没有则返回 null
     */
    public CameraComponent getActiveCameraComponent() {
        Entity entity = getActiveCamera();
        if (entity != null) {
            return entity.getComponent(CameraComponent.class)
                .orElse(null);
        }
        return null;
    }
}
