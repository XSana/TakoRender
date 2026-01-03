package moe.takochan.takorender.core.system;

import org.joml.Vector3f;

import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.LODComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * LOD 系统
 *
 * <p>
 * LODSystem 负责根据 Entity 到相机的距离更新 LODComponent 的激活级别。
 * </p>
 *
 * <p>
 * <b>执行阶段</b>: UPDATE（在 TransformSystem 之后，FrustumCullingSystem 之前）
 * </p>
 *
 * <p>
 * <b>处理逻辑</b>:
 * </p>
 * <ol>
 * <li>获取活动相机位置</li>
 * <li>计算每个 Entity 到相机的距离</li>
 * <li>根据距离和滞后值更新 activeLevel</li>
 * </ol>
 */
@RequiresComponent({ TransformComponent.class, LODComponent.class })
public class LODSystem extends GameSystem {

    private final Vector3f tempCameraPos = new Vector3f();
    private final Vector3f tempEntityPos = new Vector3f();

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        // 在 TransformSystem (-1000) 之后，FrustumCullingSystem (-500) 之前
        return -800;
    }

    @Override
    public void update(float deltaTime) {
        // 获取活动相机位置
        Vector3f cameraPos = getActiveCameraPosition();
        if (cameraPos == null) {
            return;
        }
        tempCameraPos.set(cameraPos);

        // 处理所有 LOD 组件
        for (Entity entity : getRequiredEntities()) {
            processLOD(entity, tempCameraPos);
        }
    }

    private void processLOD(Entity entity, Vector3f cameraPos) {
        TransformComponent transform = entity.getComponent(TransformComponent.class)
            .orElse(null);
        LODComponent lod = entity.getComponent(LODComponent.class)
            .orElse(null);

        if (transform == null || lod == null || lod.getLevelCount() == 0) {
            return;
        }

        // 计算距离
        Vector3f entityPos = transform.getPositionRef();
        tempEntityPos.set(entityPos);
        float distance = tempEntityPos.distance(cameraPos);

        // 更新距离
        lod.setCurrentDistance(distance);

        // 计算新的 LOD 级别（考虑滞后）
        int currentLevel = lod.getActiveLevel();
        int newLevel = lod.calculateLevel(distance, currentLevel);

        // 更新级别
        if (newLevel != currentLevel) {
            lod.setActiveLevel(newLevel);
        }
    }

    private Vector3f getActiveCameraPosition() {
        for (Entity entity : getWorld().getEntitiesWith(CameraComponent.class)) {
            CameraComponent camera = entity.getComponent(CameraComponent.class)
                .orElse(null);
            if (camera != null && camera.isActive()) {
                TransformComponent transform = entity.getComponent(TransformComponent.class)
                    .orElse(null);
                if (transform != null) {
                    return transform.getPositionRef();
                }
            }
        }
        return null;
    }
}
