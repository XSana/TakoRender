package moe.takochan.takorender.api.component;

import java.util.EnumSet;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.StaticFlags;

/**
 * 静态标记组件
 *
 * <p>
 * 存储 Entity 的静态特性标记，供渲染优化系统使用。
 * </p>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * {@code
 * // 标记为可合批
 * entity.addComponent(new StaticFlagsComponent(StaticFlags.BATCHING));
 *
 * // 标记为遮挡体
 * entity.addComponent(new StaticFlagsComponent(StaticFlags.OCCLUDER));
 *
 * // 多个标记
 * entity.addComponent(new StaticFlagsComponent()
 *     .addFlag(StaticFlags.BATCHING)
 *     .addFlag(StaticFlags.OCCLUDEE));
 * }
 * </pre>
 */
public class StaticFlagsComponent extends Component {

    /** 标记集合 */
    private final EnumSet<StaticFlags> flags = EnumSet.noneOf(StaticFlags.class);

    /**
     * 创建空标记组件
     */
    public StaticFlagsComponent() {}

    /**
     * 创建带初始标记的组件
     *
     * @param flags 初始标记
     */
    public StaticFlagsComponent(StaticFlags... flags) {
        for (StaticFlags flag : flags) {
            this.flags.add(flag);
        }
    }

    /**
     * 添加标记
     *
     * @param flag 要添加的标记
     * @return this（链式调用）
     */
    public StaticFlagsComponent addFlag(StaticFlags flag) {
        flags.add(flag);
        return this;
    }

    /**
     * 移除标记
     *
     * @param flag 要移除的标记
     * @return this（链式调用）
     */
    public StaticFlagsComponent removeFlag(StaticFlags flag) {
        flags.remove(flag);
        return this;
    }

    /**
     * 检查是否有指定标记
     *
     * @param flag 要检查的标记
     * @return true 如果有该标记
     */
    public boolean hasFlag(StaticFlags flag) {
        return flags.contains(flag);
    }

    /**
     * 检查是否可参与静态合批
     *
     * @return true 如果有 BATCHING 标记
     */
    public boolean isBatchable() {
        return flags.contains(StaticFlags.BATCHING);
    }

    /**
     * 检查是否为遮挡体
     *
     * @return true 如果有 OCCLUDER 标记
     */
    public boolean isOccluder() {
        return flags.contains(StaticFlags.OCCLUDER);
    }

    /**
     * 检查是否为被遮挡体
     *
     * @return true 如果有 OCCLUDEE 标记
     */
    public boolean isOccludee() {
        return flags.contains(StaticFlags.OCCLUDEE);
    }

    /**
     * 获取所有标记的只读副本
     *
     * @return 标记集合
     */
    public EnumSet<StaticFlags> getFlags() {
        return EnumSet.copyOf(flags);
    }

    /**
     * 清空所有标记
     */
    public void clearFlags() {
        flags.clear();
    }
}
