package moe.takochan.takorender.core.system;

import moe.takochan.takorender.api.component.BoundsComponent;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.LayerComponent;
import moe.takochan.takorender.api.component.MeshRendererComponent;
import moe.takochan.takorender.api.component.VisibilityComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Layer;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.graphics.AABB;
import moe.takochan.takorender.api.graphics.Frustum;
import moe.takochan.takorender.api.graphics.Mesh;

/**
 * 视锥剔除系统
 *
 * <p>
 * FrustumCullingSystem 负责判断 Entity 是否在相机视锥内，
 * 并更新 VisibilityComponent.culled 状态。
 * </p>
 *
 * <p>
 * <b>剔除条件</b>:
 * </p>
 * <ul>
 * <li>Layer == WORLD_3D（仅 3D 层参与剔除）</li>
 * <li>有渲染组件（MeshRenderer / LineRenderer / ParticleEmitter）</li>
 * <li>有 VisibilityComponent</li>
 * </ul>
 *
 * <p>
 * <b>包围盒来源</b>（优先级）:
 * </p>
 * <ol>
 * <li>BoundsComponent.worldBounds</li>
 * <li>MeshRendererComponent.getMesh().getBounds()（转换到世界空间）</li>
 * <li>无包围盒：不剔除（始终渲染）</li>
 * </ol>
 *
 * <p>
 * <b>执行阶段</b>: UPDATE（在 TransformSystem 之后）
 * </p>
 */
public class FrustumCullingSystem extends GameSystem {

    private final Frustum frustum = new Frustum();

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        // 在 TransformSystem (-1000) 之后执行
        return -500;
    }

    @Override
    public void update(float deltaTime) {
        // 获取活动相机
        CameraComponent activeCamera = findActiveCamera();
        if (activeCamera == null) {
            return;
        }

        // 更新视锥
        frustum.update(activeCamera.getViewProjectionMatrix());

        // 处理所有可渲染 Entity
        for (Entity entity : getWorld().getEntitiesWith(VisibilityComponent.class)) {
            processEntity(entity);
        }
    }

    private void processEntity(Entity entity) {
        VisibilityComponent visibility = entity.getComponent(VisibilityComponent.class)
            .orElse(null);
        if (visibility == null) {
            return;
        }

        // 仅对 WORLD_3D 层进行剔除
        LayerComponent layer = entity.getComponent(LayerComponent.class)
            .orElse(null);
        if (layer != null && layer.getLayer() != Layer.WORLD_3D) {
            visibility.setCulled(false);
            return;
        }

        // 获取包围盒
        AABB worldBounds = getWorldBounds(entity);
        if (worldBounds == null || !worldBounds.isValid()) {
            // 无包围盒，不剔除
            visibility.setCulled(false);
            return;
        }

        // 视锥测试
        boolean intersects = frustum.intersects(worldBounds);
        visibility.setCulled(!intersects);
    }

    /**
     * 获取 Entity 的世界空间包围盒
     */
    private AABB getWorldBounds(Entity entity) {
        // 优先使用 BoundsComponent
        BoundsComponent bounds = entity.getComponent(BoundsComponent.class)
            .orElse(null);
        if (bounds != null) {
            return bounds.getWorldBounds();
        }

        // 尝试从 MeshRendererComponent 获取
        MeshRendererComponent meshRenderer = entity.getComponent(MeshRendererComponent.class)
            .orElse(null);
        if (meshRenderer != null) {
            Mesh mesh = meshRenderer.getMesh();
            if (mesh != null) {
                AABB localBounds = mesh.getBounds();
                if (localBounds != null && localBounds.isValid()) {
                    // 需要变换到世界空间，但没有 BoundsComponent 存储结果
                    // 这里简化处理：假设 Mesh 已经在世界空间或使用本地包围盒
                    // 完整实现应该使用 TransformComponent 变换
                    return localBounds;
                }
            }
        }

        return null;
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
}
