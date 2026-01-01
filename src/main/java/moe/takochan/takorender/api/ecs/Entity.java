package moe.takochan.takorender.api.ecs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ECS 实体类
 *
 * <p>
 * Entity 是 Component 的容器，本身只是一个 ID。
 * 所有逻辑都在 System 中处理，Entity 只负责持有 Component。
 * </p>
 */
public class Entity {

    private final long id;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();
    private boolean active = true;
    private World world;

    Entity(long id, World world) {
        this.id = id;
        this.world = world;
    }

    public long getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 获取此实体所属的 World。
     */
    public World getWorld() {
        return world;
    }

    /**
     * 添加 Component 到此实体。
     *
     * <p>
     * 会自动检查 {@link RequiresComponent} 注解声明的依赖，
     * 如果缺少依赖的 Component 会抛出异常。
     * </p>
     *
     * @param component 要添加的 Component
     * @return 此实体（支持链式调用）
     * @throws IllegalStateException 如果缺少依赖的 Component
     */
    public <T extends Component> Entity addComponent(T component) {
        // 检查 @RequiresComponent 依赖
        RequiresComponent requires = component.getClass()
            .getAnnotation(RequiresComponent.class);
        if (requires != null) {
            for (Class<? extends Component> dep : requires.value()) {
                if (!hasComponent(dep)) {
                    throw new IllegalStateException(
                        component.getClass()
                            .getSimpleName() + " 需要先添加 "
                            + dep.getSimpleName()
                            + " 组件");
                }
            }
        }

        Class<? extends Component> componentClass = component.getClass();
        components.put(componentClass, component);
        component.setEntity(this);

        // 通知 World 更新索引
        if (world != null) {
            world.onComponentAdded(this, componentClass);
        }
        return this;
    }

    /**
     * 获取指定类型的 Component。
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getComponent(Class<T> componentClass) {
        return Optional.ofNullable((T) components.get(componentClass));
    }

    /**
     * 检查是否拥有指定类型的 Component。
     */
    public <T extends Component> boolean hasComponent(Class<T> componentClass) {
        return components.containsKey(componentClass);
    }

    /**
     * 检查是否拥有所有指定类型的 Component。
     *
     * @param componentClasses Component 类型数组
     * @return 如果拥有全部指定的 Component 则返回 true
     */
    public boolean hasComponents(Class<?>... componentClasses) {
        for (Class<?> clazz : componentClasses) {
            if (!components.containsKey(clazz)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从此实体移除 Component。
     */
    public <T extends Component> void removeComponent(Class<T> componentClass) {
        Component removed = components.remove(componentClass);
        if (removed != null && world != null) {
            world.onComponentRemoved(this, componentClass);
        }
    }

    /**
     * 获取此实体拥有的所有 Component 类型。
     */
    public Set<Class<? extends Component>> getComponentTypes() {
        return components.keySet();
    }

    /**
     * 获取此实体的所有 Component。
     */
    public Collection<Component> getComponents() {
        return components.values();
    }
}
