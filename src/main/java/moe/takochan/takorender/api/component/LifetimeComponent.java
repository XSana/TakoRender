package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;

/**
 * 生命周期组件 - 控制 Entity 的生命周期
 *
 * <p>
 * LifetimeComponent 定义 Entity 的生命周期类型和销毁时机：
 * </p>
 * <ul>
 * <li>{@link Lifetime#TRANSIENT}: 瞬时（伤害数字、Toast）- 计时结束自动销毁</li>
 * <li>{@link Lifetime#VIEW}: 视图（窗口、面板）- 用户手动关闭</li>
 * <li>{@link Lifetime#SESSION}: 会话（血条、小地图）- 存档退出时销毁</li>
 * <li>{@link Lifetime#MANUAL}: 手动控制（TileEntity 关联）</li>
 * </ul>
 *
 * <p>
 * <b>LifetimeSystem 行为</b>:
 * </p>
 * <ul>
 * <li>TRANSIENT: 每帧计时，到期自动 markForDestroy</li>
 * <li>VIEW: 无自动行为，用户手动 markForDestroy</li>
 * <li>SESSION: 监听存档退出事件，批量 markForDestroy</li>
 * <li>MANUAL: 无自动行为，完全由用户控制</li>
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
 *     // 伤害数字（2 秒后消失）
 *     Entity damage = world.createEntity();
 *     damage.addComponent(new LifetimeComponent(Lifetime.TRANSIENT).setDuration(2.0f));
 *
 *     // 血条（存档退出时清理）
 *     Entity healthBar = world.createEntity();
 *     healthBar.addComponent(new LifetimeComponent(Lifetime.SESSION));
 *
 *     // TileEntity 关联（手动控制）
 *     Entity teEntity = world.createEntity();
 *     teEntity.addComponent(new LifetimeComponent(Lifetime.MANUAL));
 *     // 在 TileEntity.invalidate() 中调用：
 *     // teEntity.getComponent(LifetimeComponent.class).ifPresent(LifetimeComponent::markForDestroy);
 * }
 * </pre>
 */
public class LifetimeComponent extends Component {

    /**
     * 生命周期类型
     */
    public enum Lifetime {
        /** 瞬时 - 计时结束自动销毁（伤害数字、Toast） */
        TRANSIENT,

        /** 视图 - 用户手动关闭（窗口、面板） */
        VIEW,

        /** 会话 - 存档退出时销毁（血条、小地图） */
        SESSION,

        /** 手动 - 完全由用户控制（TileEntity 关联） */
        MANUAL
    }

    private Lifetime lifetime = Lifetime.MANUAL;
    private float duration = -1f;
    private float elapsed = 0f;
    private boolean markedForDestroy = false;

    /**
     * 创建默认生命周期组件（MANUAL）
     */
    public LifetimeComponent() {}

    /**
     * 创建指定类型的生命周期组件
     *
     * @param lifetime 生命周期类型
     */
    public LifetimeComponent(Lifetime lifetime) {
        this.lifetime = lifetime != null ? lifetime : Lifetime.MANUAL;
    }

    /**
     * 获取生命周期类型
     *
     * @return 生命周期类型
     */
    public Lifetime getLifetime() {
        return lifetime;
    }

    /**
     * 设置生命周期类型
     *
     * @param lifetime 生命周期类型
     * @return this（链式调用）
     */
    public LifetimeComponent setLifetime(Lifetime lifetime) {
        this.lifetime = lifetime != null ? lifetime : Lifetime.MANUAL;
        return this;
    }

    /**
     * 获取持续时间（仅 TRANSIENT 有效）
     *
     * @return 持续时间（秒），-1 表示由动画控制
     */
    public float getDuration() {
        return duration;
    }

    /**
     * 设置持续时间（仅 TRANSIENT 有效）
     *
     * @param duration 持续时间（秒），-1 表示由动画控制
     * @return this（链式调用）
     */
    public LifetimeComponent setDuration(float duration) {
        this.duration = duration;
        return this;
    }

    /**
     * 获取已过时间
     *
     * @return 已过时间（秒）
     */
    public float getElapsed() {
        return elapsed;
    }

    /**
     * 增加已过时间
     *
     * @param deltaTime 增量时间（秒）
     */
    public void addElapsed(float deltaTime) {
        this.elapsed += deltaTime;
    }

    /**
     * 重置计时器
     */
    public void resetTimer() {
        this.elapsed = 0f;
    }

    /**
     * 检查是否已过期（仅 TRANSIENT 有效）
     *
     * @return 是否已过期
     */
    public boolean isExpired() {
        return lifetime == Lifetime.TRANSIENT && duration > 0 && elapsed >= duration;
    }

    /**
     * 检查是否已标记为销毁
     *
     * @return 是否已标记为销毁
     */
    public boolean isMarkedForDestroy() {
        return markedForDestroy;
    }

    /**
     * 标记为销毁
     * <p>
     * LifetimeSystem 会在下一帧统一销毁所有标记的 Entity。
     * </p>
     */
    public void markForDestroy() {
        this.markedForDestroy = true;
    }
}
