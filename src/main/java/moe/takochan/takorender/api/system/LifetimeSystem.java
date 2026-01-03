package moe.takochan.takorender.api.system;

import java.util.ArrayList;
import java.util.List;

import moe.takochan.takorender.api.component.LifetimeComponent;
import moe.takochan.takorender.api.component.LifetimeComponent.Lifetime;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 生命周期系统 - 管理 Entity 的生命周期和销毁
 *
 * <p>
 * LifetimeSystem 在 UPDATE 阶段执行，负责：
 * </p>
 * <ul>
 * <li>TRANSIENT: 每帧计时，到期自动标记销毁</li>
 * <li>所有类型: 统一销毁已标记的 Entity</li>
 * </ul>
 *
 * <p>
 * <b>销毁流程</b>:
 * </p>
 * <ol>
 * <li>用户或系统调用 {@code LifetimeComponent.markForDestroy()}</li>
 * <li>LifetimeSystem 在下一帧检测并销毁 Entity</li>
 * </ol>
 *
 * <p>
 * <b>SESSION 类型清理</b>:
 * 存档退出时，外部调用 {@link #destroySessionEntities()} 批量标记销毁。
 * </p>
 */
@RequiresComponent(LifetimeComponent.class)
public class LifetimeSystem extends GameSystem {

    /** 待销毁的 Entity 列表（复用避免每帧分配） */
    private final List<Entity> toDestroy = new ArrayList<>();

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void update(float deltaTime) {
        toDestroy.clear();

        for (Entity entity : getRequiredEntities()) {
            LifetimeComponent lifetime = entity.getComponent(LifetimeComponent.class)
                .orElse(null);

            if (lifetime == null) {
                continue;
            }

            // 处理 TRANSIENT 计时
            if (lifetime.getLifetime() == Lifetime.TRANSIENT) {
                lifetime.addElapsed(deltaTime);
                if (lifetime.isExpired()) {
                    lifetime.markForDestroy();
                }
            }

            // 收集待销毁的 Entity
            if (lifetime.isMarkedForDestroy()) {
                toDestroy.add(entity);
            }
        }

        // 统一销毁
        for (Entity entity : toDestroy) {
            getWorld().removeEntity(entity.getId());
        }
    }

    /**
     * 销毁所有 SESSION 类型的 Entity
     * <p>
     * 应在存档退出时调用（ClientDisconnectionFromServerEvent）。
     * </p>
     */
    public void destroySessionEntities() {
        for (Entity entity : getRequiredEntities()) {
            LifetimeComponent lifetime = entity.getComponent(LifetimeComponent.class)
                .orElse(null);

            if (lifetime != null && lifetime.getLifetime() == Lifetime.SESSION) {
                lifetime.markForDestroy();
            }
        }
    }

    /**
     * 销毁所有有 LifetimeComponent 的 Entity
     * <p>
     * 应在游戏退出时调用。
     * </p>
     */
    public void destroyAllEntities() {
        for (Entity entity : getRequiredEntities()) {
            LifetimeComponent lifetime = entity.getComponent(LifetimeComponent.class)
                .orElse(null);

            if (lifetime != null) {
                lifetime.markForDestroy();
            }
        }
    }
}
