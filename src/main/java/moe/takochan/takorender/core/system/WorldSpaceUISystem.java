package moe.takochan.takorender.core.system;

import org.joml.Vector3f;
import org.joml.Vector4f;

import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.component.WorldSpaceUIComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 世界空间 UI 系统
 *
 * <p>
 * 负责将带有 {@link WorldSpaceUIComponent} 的 Entity 的世界坐标
 * 转换为屏幕坐标，供 UI 渲染使用。
 * </p>
 *
 * <p>
 * <b>功能</b>:
 * </p>
 * <ul>
 * <li>世界坐标 → 屏幕坐标转换</li>
 * <li>距离计算与缩放</li>
 * <li>可见性判断（视锥、距离）</li>
 * </ul>
 *
 * <p>
 * <b>执行阶段</b>: UPDATE（在 CameraSystem 之后执行）
 * </p>
 */
@RequiresComponent({ TransformComponent.class, WorldSpaceUIComponent.class })
public class WorldSpaceUISystem extends GameSystem {

    private final Vector3f tempWorldPos = new Vector3f();
    private final Vector3f tempCameraPos = new Vector3f();

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        // 在 CameraSystem (100) 之后执行
        return 150;
    }

    @Override
    public void update(float deltaTime) {
        // 获取活动相机
        CameraComponent activeCamera = findActiveCamera();
        if (activeCamera == null) {
            return;
        }

        // 获取相机位置
        Entity cameraEntity = findActiveCameraEntity();
        if (cameraEntity == null) {
            return;
        }

        TransformComponent cameraTransform = cameraEntity.getComponent(TransformComponent.class)
            .orElse(null);
        if (cameraTransform == null) {
            return;
        }

        Vector3f cameraPos = cameraTransform.getPositionRef();
        tempCameraPos.set(cameraPos.x, cameraPos.y, cameraPos.z);

        // 处理所有 WorldSpaceUI 组件
        for (Entity entity : getRequiredEntities()) {
            processUIComponent(entity, activeCamera, tempCameraPos);
        }
    }

    private void processUIComponent(Entity entity, CameraComponent camera, Vector3f cameraPos) {
        TransformComponent transform = entity.getComponent(TransformComponent.class)
            .orElse(null);
        WorldSpaceUIComponent ui = entity.getComponent(WorldSpaceUIComponent.class)
            .orElse(null);

        if (transform == null || ui == null) {
            return;
        }

        Vector3f pos = transform.getPositionRef();

        // 计算世界坐标（Transform 位置 + 偏移）
        tempWorldPos.set(pos.x + ui.getWorldOffset().x, pos.y + ui.getWorldOffset().y, pos.z + ui.getWorldOffset().z);

        // 计算距离
        float distance = tempWorldPos.distance(cameraPos);
        ui.setCurrentDistance(distance);

        // 检查最大距离
        if (distance > ui.getMaxDistance()) {
            ui.setVisible(false);
            return;
        }

        // 投影到屏幕坐标
        Vector4f projected = camera.projectToViewport(tempWorldPos);

        // 检查是否在相机前方
        if (projected.w <= 0) {
            ui.setVisible(false);
            return;
        }

        // 检查是否在屏幕内（使用 NDC 范围）
        Vector4f ndc = camera.project(tempWorldPos);
        boolean inScreen = ndc.x >= -1.0f && ndc.x <= 1.0f
            && ndc.y >= -1.0f
            && ndc.y <= 1.0f
            && ndc.z >= 0.0f
            && ndc.z <= 1.0f;

        if (!inScreen) {
            ui.setVisible(false);
            return;
        }

        // 设置屏幕坐标（加上屏幕偏移）
        ui.setScreenPosition(projected.x + ui.getScreenOffset().x, projected.y + ui.getScreenOffset().y);

        // 设置深度
        ui.setDepth(ndc.z);

        // 计算距离缩放
        if (ui.isScaleWithDistance()) {
            float scale = ui.getReferenceDistance() / Math.max(distance, 0.001f);
            scale = Math.max(ui.getMinScale(), Math.min(ui.getMaxScale(), scale));
            ui.setDistanceScale(scale);
        } else {
            ui.setDistanceScale(1.0f);
        }

        // 标记为可见
        ui.setVisible(true);
    }

    private CameraComponent findActiveCamera() {
        for (Entity entity : getWorld().getEntitiesWith(CameraComponent.class)) {
            CameraComponent camera = entity.getComponent(CameraComponent.class)
                .orElse(null);
            if (camera != null && camera.isActive()) {
                return camera;
            }
        }
        return null;
    }

    private Entity findActiveCameraEntity() {
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
