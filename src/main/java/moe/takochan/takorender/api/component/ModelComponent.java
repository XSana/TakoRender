package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.graphics.Model;

/**
 * 模型组件
 *
 * <p>
 * ModelComponent 存储对 Model 的引用，由 ModelRenderSystem 渲染。
 * 支持多子网格模型。
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
 *     // 创建带模型的 Entity
 *     Entity entity = world.createEntity();
 *     entity.addComponent(new TransformComponent(x, y, z));
 *     entity.addComponent(new ModelComponent().setModel(model));
 *     entity.addComponent(new LayerComponent(Layer.WORLD_3D));
 *     entity.addComponent(new VisibilityComponent());
 * }
 * </pre>
 *
 * <p>
 * <b>与 MeshRendererComponent 的区别</b>:
 * </p>
 * <ul>
 * <li>MeshRendererComponent - 单个 Mesh + 单个 Material</li>
 * <li>ModelComponent - 多个子网格，每个有独立材质</li>
 * </ul>
 */
public class ModelComponent extends Component {

    /** 模型引用 */
    private Model model;

    /** 是否投射阴影（预留） */
    private boolean castShadow = true;

    /** 是否接收阴影（预留） */
    private boolean receiveShadow = true;

    /**
     * 创建空模型组件
     */
    public ModelComponent() {}

    /**
     * 创建指定模型的组件
     *
     * @param model 模型
     */
    public ModelComponent(Model model) {
        this.model = model;
    }

    /**
     * 获取模型
     *
     * @return 模型
     */
    public Model getModel() {
        return model;
    }

    /**
     * 设置模型
     *
     * @param model 模型
     * @return this（链式调用）
     */
    public ModelComponent setModel(Model model) {
        this.model = model;
        return this;
    }

    /**
     * 是否投射阴影
     *
     * @return true 表示投射阴影
     */
    public boolean isCastShadow() {
        return castShadow;
    }

    /**
     * 设置是否投射阴影
     *
     * @param castShadow 是否投射阴影
     * @return this（链式调用）
     */
    public ModelComponent setCastShadow(boolean castShadow) {
        this.castShadow = castShadow;
        return this;
    }

    /**
     * 是否接收阴影
     *
     * @return true 表示接收阴影
     */
    public boolean isReceiveShadow() {
        return receiveShadow;
    }

    /**
     * 设置是否接收阴影
     *
     * @param receiveShadow 是否接收阴影
     * @return this（链式调用）
     */
    public ModelComponent setReceiveShadow(boolean receiveShadow) {
        this.receiveShadow = receiveShadow;
        return this;
    }

    /**
     * 检查是否有有效模型
     *
     * @return true 表示有模型
     */
    public boolean hasModel() {
        return model != null && !model.isDisposed() && model.getSubMeshCount() > 0;
    }
}
