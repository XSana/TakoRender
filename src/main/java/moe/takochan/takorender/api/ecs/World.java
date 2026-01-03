package moe.takochan.takorender.api.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ECS World - 管理实体和系统
 *
 * <p>
 * World 是 ECS 的核心容器，负责：
 * </p>
 * <ul>
 * <li>创建和管理 Entity</li>
 * <li>维护 Component 索引（O(1) 查询）</li>
 * <li>管理和调度 GameSystem</li>
 * <li>支持 Layer 分层渲染</li>
 * </ul>
 *
 * <p>
 * <b>Component 索引优化</b>:
 * 使用 {@code Map<Class, Set<Entity>>} 维护 Component 类型到 Entity 的映射，
 * 实现 O(1) 复杂度的 Component 查询，替代 O(N) 的遍历方式。
 * </p>
 *
 * <p>
 * <b>Layer 分层渲染</b>:
 * 使用 {@link #update(Layer, float)} 和 {@link #render(Layer)} 按层更新和渲染。
 * System 通过 {@link #getCurrentLayer()} 获取当前渲染层进行筛选。
 * </p>
 */
public class World {

    private final AtomicLong entityIdCounter = new AtomicLong(0);
    private final Map<Long, Entity> entities = new HashMap<>();
    private final List<GameSystem> systems = new ArrayList<>();

    /** Component 类型索引 - 支持 O(1) 查询 */
    private final Map<Class<? extends Component>, Set<Entity>> componentIndex = new HashMap<>();

    /** 场景管理器 */
    private final SceneManager sceneManager = new SceneManager(this);

    /** 当前渲染层（渲染期间有效） */
    private Layer currentLayer = null;

    /**
     * 创建新实体。
     *
     * @return 新创建的实体
     */
    public Entity createEntity() {
        long id = entityIdCounter.incrementAndGet();
        Entity entity = new Entity(id, this);
        entities.put(id, entity);
        return entity;
    }

    /**
     * 根据 ID 获取实体。
     */
    public Optional<Entity> getEntity(long id) {
        return Optional.ofNullable(entities.get(id));
    }

    /**
     * 从 World 中移除实体。
     */
    public void removeEntity(long id) {
        Entity entity = entities.remove(id);
        if (entity != null) {
            // 从所有 Component 索引中移除此实体
            for (Class<? extends Component> componentClass : entity.getComponentTypes()) {
                Set<Entity> indexed = componentIndex.get(componentClass);
                if (indexed != null) {
                    indexed.remove(entity);
                }
            }
        }
    }

    /**
     * 获取所有实体。
     */
    public List<Entity> getEntities() {
        return new ArrayList<>(entities.values());
    }

    /**
     * 获取拥有指定 Component 的所有实体（O(1) 复杂度）。
     *
     * <p>
     * 使用 Component 索引实现快速查询，只返回活跃的实体。
     * </p>
     *
     * @param componentClass Component 类型
     * @return 拥有该 Component 的活跃实体列表
     */
    public <T extends Component> List<Entity> getEntitiesWith(Class<T> componentClass) {
        Set<Entity> indexed = componentIndex.get(componentClass);
        if (indexed == null || indexed.isEmpty()) {
            return Collections.emptyList();
        }

        List<Entity> result = new ArrayList<>();
        for (Entity entity : indexed) {
            if (entity.isActive()) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * 获取拥有所有指定 Component 的实体。
     *
     * <p>
     * 使用最小索引集作为起始点进行过滤，优化多 Component 查询性能。
     * </p>
     *
     * @param componentClasses Component 类型数组
     * @return 拥有所有指定 Component 的活跃实体列表
     */
    @SafeVarargs
    public final List<Entity> getEntitiesWith(Class<? extends Component>... componentClasses) {
        if (componentClasses.length == 0) {
            return Collections.emptyList();
        }

        if (componentClasses.length == 1) {
            return getEntitiesWith(componentClasses[0]);
        }

        // 找到最小的索引集作为起始点
        Set<Entity> smallestSet = null;
        int minSize = Integer.MAX_VALUE;

        for (Class<? extends Component> clazz : componentClasses) {
            Set<Entity> indexed = componentIndex.get(clazz);
            if (indexed == null || indexed.isEmpty()) {
                return Collections.emptyList();
            }
            if (indexed.size() < minSize) {
                minSize = indexed.size();
                smallestSet = indexed;
            }
        }

        // 从最小集合开始过滤
        List<Entity> result = new ArrayList<>();
        for (Entity entity : smallestSet) {
            if (entity.isActive() && entity.hasComponents(componentClasses)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * 当 Entity 添加 Component 时调用（内部方法）。
     *
     * @param entity         添加了 Component 的实体
     * @param componentClass Component 类型
     */
    void onComponentAdded(Entity entity, Class<? extends Component> componentClass) {
        componentIndex.computeIfAbsent(componentClass, k -> new HashSet<>())
            .add(entity);
    }

    /**
     * 当 Entity 移除 Component 时调用（内部方法）。
     *
     * @param entity         移除了 Component 的实体
     * @param componentClass Component 类型
     */
    void onComponentRemoved(Entity entity, Class<? extends Component> componentClass) {
        Set<Entity> indexed = componentIndex.get(componentClass);
        if (indexed != null) {
            indexed.remove(entity);
        }
    }

    /**
     * 添加系统到 World。
     *
     * <p>
     * 系统添加后会按 {@link GameSystem#getPriority()} 重新排序，
     * 确保低优先级数字的系统先执行。
     * </p>
     */
    public <T extends GameSystem> T addSystem(T system) {
        system.setWorld(this);
        systems.add(system);
        sortSystems();
        system.onInit();
        return system;
    }

    /**
     * 按优先级排序系统（数字小的先执行）。
     */
    private void sortSystems() {
        systems.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
    }

    /**
     * 从 World 移除系统。
     */
    public void removeSystem(GameSystem system) {
        system.onDestroy();
        systems.remove(system);
    }

    /**
     * 获取指定类型的系统实例。
     *
     * @param systemClass 系统类型
     * @return 系统实例，如果未找到返回 null
     */
    @SuppressWarnings("unchecked")
    public <T extends GameSystem> T getSystem(Class<T> systemClass) {
        for (GameSystem system : systems) {
            if (systemClass.isInstance(system)) {
                return (T) system;
            }
        }
        return null;
    }

    /**
     * 执行 UPDATE 阶段的所有系统。
     *
     * <p>
     * 只调用 {@link Phase#UPDATE} 阶段的系统。
     * 应在游戏逻辑更新时调用。
     * </p>
     *
     * @param deltaTime 距上次更新的时间（秒）
     */
    public void update(float deltaTime) {
        for (GameSystem system : systems) {
            if (system.isEnabled() && system.getPhase() == Phase.UPDATE) {
                system.update(deltaTime);
            }
        }
    }

    /**
     * 执行指定 Layer 的 UPDATE 阶段系统。
     *
     * <p>
     * 设置当前 Layer，然后调用所有 UPDATE 阶段的系统。
     * System 可通过 {@link #getCurrentLayer()} 获取当前层进行筛选。
     * </p>
     *
     * @param layer     渲染层
     * @param deltaTime 距上次更新的时间（秒）
     */
    public void update(Layer layer, float deltaTime) {
        this.currentLayer = layer;
        try {
            update(deltaTime);
        } finally {
            this.currentLayer = null;
        }
    }

    /**
     * 执行 RENDER 阶段的所有系统。
     *
     * <p>
     * 只调用 {@link Phase#RENDER} 阶段的系统。
     * 应在渲染帧时调用。
     * </p>
     *
     * @param deltaTime 距上次渲染的时间（秒）
     */
    public void render(float deltaTime) {
        for (GameSystem system : systems) {
            if (system.isEnabled() && system.getPhase() == Phase.RENDER) {
                system.update(deltaTime);
            }
        }
    }

    /**
     * 执行指定 Layer 的 RENDER 阶段系统。
     *
     * <p>
     * 设置当前 Layer，然后调用所有 RENDER 阶段的系统。
     * System 可通过 {@link #getCurrentLayer()} 获取当前层进行筛选。
     * </p>
     *
     * @param layer 渲染层
     */
    public void render(Layer layer) {
        this.currentLayer = layer;
        try {
            render(0f);
        } finally {
            this.currentLayer = null;
        }
    }

    /**
     * 获取当前渲染层。
     *
     * <p>
     * 仅在 update(Layer, deltaTime) 或 render(Layer) 期间有效。
     * </p>
     *
     * @return 当前渲染层，如果不在分层渲染期间返回 null
     */
    public Layer getCurrentLayer() {
        return currentLayer;
    }

    /**
     * 获取场景管理器。
     *
     * @return 场景管理器
     */
    public SceneManager getSceneManager() {
        return sceneManager;
    }

    /**
     * 清空所有实体和系统。
     */
    public void clear() {
        for (GameSystem system : new ArrayList<>(systems)) {
            removeSystem(system);
        }
        entities.clear();
        componentIndex.clear();
    }

    /**
     * 清空所有实体（保留系统）。
     *
     * <p>
     * 用于存档退出等场景，清空所有 Entity 但保留 System。
     * </p>
     */
    public void clearAllEntities() {
        entities.clear();
        componentIndex.clear();
    }

    /**
     * 获取实体数量。
     */
    public int getEntityCount() {
        return entities.size();
    }

    /**
     * 获取系统数量。
     */
    public int getSystemCount() {
        return systems.size();
    }
}
