package moe.takochan.takorender.core.system;

import org.joml.Vector3f;

import moe.takochan.takorender.api.component.TrailComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 拖尾系统
 *
 * <p>
 * 负责更新 {@link TrailComponent} 的位置历史。
 * </p>
 *
 * <p>
 * <b>职责</b>:
 * </p>
 * <ul>
 * <li>记录实体位置到拖尾历史</li>
 * <li>更新拖尾点的 age</li>
 * <li>移除过期的拖尾点</li>
 * </ul>
 *
 * <p>
 * <b>执行阶段</b>: UPDATE
 * </p>
 */
@RequiresComponent({ TransformComponent.class, TrailComponent.class })
public class TrailSystem extends GameSystem {

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public void update(float deltaTime) {
        for (Entity entity : getRequiredEntities()) {
            TransformComponent transform = entity.getComponent(TransformComponent.class)
                .orElse(null);
            TrailComponent trail = entity.getComponent(TrailComponent.class)
                .orElse(null);

            if (transform == null || trail == null) {
                continue;
            }

            // 更新所有点的 age
            trail.updateAges(deltaTime);

            // 如果正在发射，添加新点
            if (trail.isEmitting()) {
                Vector3f pos = transform.getPositionRef();
                trail.addPoint(pos.x, pos.y, pos.z);
            }
        }
    }
}
