package moe.takochan.takorender.api.ecs;

import java.util.Collections;
import java.util.List;

/**
 * ECS 系统基类
 *
 * <p>
 * System 是 ECS 架构中唯一包含逻辑的组件。
 * 它根据特定 Component 组合查询 Entity 并处理业务逻辑。
 * </p>
 *
 * <p>
 * <b>Phase 分离</b>:
 * 系统分为 UPDATE 和 RENDER 两个阶段，通过 {@link #getPhase()} 指定。
 * World 会在 update() 时调用 UPDATE 阶段的系统，在 render() 时调用 RENDER 阶段的系统。
 * </p>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     public class MeshRenderSystem extends GameSystem {
 *
 *         &#64;Override
 *         public Phase getPhase() {
 *             return Phase.RENDER; // 渲染阶段执行
 *         }
 *
 *         &#64;Override
 *         public void update(float deltaTime) {
 *             for (Entity entity : getWorld().getEntitiesWith(MeshComponent.class)) {
 *                 // 渲染逻辑
 *             }
 *         }
 *     }
 * }
 * </pre>
 */
public abstract class GameSystem {

    private World world;
    private boolean enabled = true;
    private int priority = 0;

    /**
     * 获取此系统所属的 World。
     */
    public World getWorld() {
        return world;
    }

    /**
     * 设置此系统所属的 World（内部方法）。
     */
    void setWorld(World world) {
        this.world = world;
    }

    /**
     * 检查此系统是否启用。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 启用或禁用此系统。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取此系统的执行优先级。
     *
     * <p>
     * 优先级越小越先执行。默认为 0。
     * </p>
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 设置此系统的执行优先级。
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * 获取此系统的执行阶段。
     *
     * <p>
     * 默认返回 {@link Phase#UPDATE}。
     * 渲染相关的系统应覆盖此方法返回 {@link Phase#RENDER}。
     * </p>
     *
     * @return 系统执行阶段
     */
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    /**
     * 系统初始化时调用。
     *
     * <p>
     * 在系统被添加到 World 后调用。
     * </p>
     */
    public void onInit() {}

    /**
     * 每帧调用以更新系统。
     *
     * @param deltaTime 距上次更新的时间（秒）
     */
    public abstract void update(float deltaTime);

    /**
     * 系统销毁时调用。
     *
     * <p>
     * 在系统从 World 移除时调用。
     * </p>
     */
    public void onDestroy() {}

    /**
     * 获取拥有所有必需 Component 的实体。
     *
     * <p>
     * 根据 {@link RequiresComponent} 注解声明的 Component 类型查询实体。
     * 如果没有声明注解，返回空列表。
     * </p>
     *
     * <p>
     * <b>使用示例</b>:
     * </p>
     *
     * <pre>
     * 
     * {
     *     &#64;code
     *     &#64;RequiresComponent({ TransformComponent.class, MeshComponent.class })
     *     public class MeshRenderSystem extends GameSystem {
     *
     *         &#64;Override
     *         public void update(float deltaTime) {
     *             for (Entity entity : getRequiredEntities()) {
     *                 // 处理拥有 Transform 和 Mesh 的实体
     *             }
     *         }
     *     }
     * }
     * </pre>
     *
     * @return 拥有所有必需 Component 的实体列表
     */
    protected List<Entity> getRequiredEntities() {
        if (world == null) {
            return Collections.emptyList();
        }

        RequiresComponent annotation = getClass().getAnnotation(RequiresComponent.class);
        if (annotation == null || annotation.value().length == 0) {
            return Collections.emptyList();
        }

        return world.getEntitiesWith(annotation.value());
    }
}
