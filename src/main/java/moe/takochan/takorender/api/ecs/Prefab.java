package moe.takochan.takorender.api.ecs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Prefab - 实体预制件
 *
 * <p>
 * Prefab 是实体模板，用于快速创建具有相同 Component 组合的实体。
 * 每次实例化都会创建全新的 Component 实例，避免状态共享。
 * </p>
 *
 * <p>
 * <b>设计特点</b>:
 * </p>
 * <ul>
 * <li>使用 {@code Supplier<Component>} 延迟创建，每次实例化都是新对象</li>
 * <li>支持初始化器，在所有 Component 添加后执行自定义逻辑</li>
 * <li>按添加顺序创建 Component，满足 {@link RequiresComponent} 依赖</li>
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
 *     // 定义 Prefab
 *     Prefab firePrefab = new Prefab("Fire").add(TransformComponent::new)
 *         .add(() -> {
 *             ParticleEmitterComponent emitter = new ParticleEmitterComponent();
 *             emitter.setRate(100);
 *             return emitter;
 *         })
 *         .initializer(
 *             entity -> {
 *                 entity.getComponent(TransformComponent.class)
 *                     .ifPresent(t -> t.setPosition(0, 64, 0));
 *             });
 *
 *     // 实例化
 *     Entity fire1 = firePrefab.instantiate(world);
 *     Entity fire2 = firePrefab.instantiate(world);
 * }
 * </pre>
 */
public class Prefab {

    private final String name;
    private final List<Supplier<? extends Component>> componentFactories = new ArrayList<>();
    private final List<Consumer<Entity>> initializers = new ArrayList<>();

    /**
     * 创建 Prefab。
     *
     * @param name 预制件名称（用于调试）
     */
    public Prefab(String name) {
        this.name = name;
    }

    /**
     * 添加 Component 工厂。
     *
     * <p>
     * 每次 {@link #instantiate(World)} 时都会调用工厂创建新实例。
     * 按添加顺序创建，确保满足 {@link RequiresComponent} 依赖。
     * </p>
     *
     * @param factory Component 工厂（Supplier）
     * @return this（链式调用）
     */
    public Prefab add(Supplier<? extends Component> factory) {
        componentFactories.add(factory);
        return this;
    }

    /**
     * 添加初始化器。
     *
     * <p>
     * 在所有 Component 添加完成后执行。可用于设置 Component 之间的关联或初始值。
     * </p>
     *
     * @param initializer 初始化回调
     * @return this（链式调用）
     */
    public Prefab initializer(Consumer<Entity> initializer) {
        initializers.add(initializer);
        return this;
    }

    /**
     * 实例化 Prefab，创建新实体。
     *
     * <p>
     * 执行步骤：
     * </p>
     * <ol>
     * <li>创建空 Entity</li>
     * <li>按顺序调用工厂创建并添加 Component</li>
     * <li>执行所有初始化器</li>
     * </ol>
     *
     * @param world 目标 World
     * @return 新创建的实体
     */
    public Entity instantiate(World world) {
        Entity entity = world.createEntity();

        // 按顺序创建并添加 Component
        for (Supplier<? extends Component> factory : componentFactories) {
            Component component = factory.get();
            entity.addComponent(component);
        }

        // 执行初始化器
        for (Consumer<Entity> initializer : initializers) {
            initializer.accept(entity);
        }

        return entity;
    }

    /**
     * 获取 Prefab 名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 获取 Component 工厂数量。
     */
    public int getComponentCount() {
        return componentFactories.size();
    }

    @Override
    public String toString() {
        return "Prefab[" + name + ", components=" + componentFactories.size() + "]";
    }
}
