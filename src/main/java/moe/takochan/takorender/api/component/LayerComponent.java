package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.Layer;

/**
 * 层组件 - 标记 Entity 所属的渲染层
 *
 * <p>
 * LayerComponent 决定 Entity 在哪个渲染事件中被处理。
 * 如果 Entity 没有 LayerComponent，默认视为 WORLD_3D。
 * </p>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * {
 *     &#64;code
 *     // 3D 世界对象
 *     entity.addComponent(new LayerComponent(Layer.WORLD_3D));
 *
 *     // HUD 元素
 *     hudElement.addComponent(new LayerComponent(Layer.HUD));
 *
 *     // GUI 界面
 *     guiPanel.addComponent(new LayerComponent(Layer.GUI));
 * }
 * </pre>
 */
public class LayerComponent extends Component {

    private Layer layer;

    /**
     * 创建默认层组件（WORLD_3D）
     */
    public LayerComponent() {
        this.layer = Layer.WORLD_3D;
    }

    /**
     * 创建指定层的组件
     *
     * @param layer 渲染层
     */
    public LayerComponent(Layer layer) {
        this.layer = layer != null ? layer : Layer.WORLD_3D;
    }

    /**
     * 获取渲染层
     *
     * @return 渲染层
     */
    public Layer getLayer() {
        return layer;
    }

    /**
     * 设置渲染层
     *
     * @param layer 渲染层
     * @return this（链式调用）
     */
    public LayerComponent setLayer(Layer layer) {
        this.layer = layer != null ? layer : Layer.WORLD_3D;
        return this;
    }
}
