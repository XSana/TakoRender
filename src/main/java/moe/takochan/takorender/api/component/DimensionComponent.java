package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;

/**
 * 维度组件 - 标记 Entity 所属的 MC 维度
 *
 * <p>
 * DimensionComponent 用于 3D 世界空间的 Entity，
 * 渲染时根据当前活动维度进行筛选。
 * </p>
 *
 * <p>
 * <b>维度切换</b>:
 * Entity 不会在维度切换时销毁，只是不被渲染。
 * 当玩家回到原维度时，Entity 会重新渲染。
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
 *     // 主世界的 Entity
 *     Entity overworld = world.createEntity();
 *     overworld.addComponent(new DimensionComponent(0)); // 主世界
 *
 *     // 下界的 Entity
 *     Entity nether = world.createEntity();
 *     nether.addComponent(new DimensionComponent(-1)); // 下界
 *
 *     // 末地的 Entity
 *     Entity end = world.createEntity();
 *     end.addComponent(new DimensionComponent(1)); // 末地
 * }
 * </pre>
 */
public class DimensionComponent extends Component {

    private int dimensionId;

    /**
     * 创建默认维度组件（主世界，dimensionId=0）
     */
    public DimensionComponent() {
        this.dimensionId = 0;
    }

    /**
     * 创建指定维度的组件
     *
     * @param dimensionId MC 维度 ID（0=主世界，-1=下界，1=末地）
     */
    public DimensionComponent(int dimensionId) {
        this.dimensionId = dimensionId;
    }

    /**
     * 获取维度 ID
     *
     * @return 维度 ID
     */
    public int getDimensionId() {
        return dimensionId;
    }

    /**
     * 设置维度 ID
     *
     * @param dimensionId 维度 ID
     * @return this（链式调用）
     */
    public DimensionComponent setDimensionId(int dimensionId) {
        this.dimensionId = dimensionId;
        return this;
    }
}
