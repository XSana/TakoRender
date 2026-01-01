package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.graphics.Material;
import moe.takochan.takorender.api.graphics.Mesh;

/**
 * 网格渲染组件 - 存储网格渲染所需的数据
 *
 * <p>
 * MeshRendererComponent 是纯数据组件，不包含任何渲染逻辑。
 * 所有渲染逻辑由 MeshRenderSystem 处理。
 * </p>
 *
 * <p>
 * <b>ECS 原则</b>: Component 只存储数据，不包含逻辑。
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
 *     Entity entity = world.createEntity();
 *     entity.addComponent(new TransformComponent(0, 64, 0));
 *     entity.addComponent(
 *         new MeshRendererComponent().setMesh(cubeMesh)
 *             .setMaterial(stoneMaterial)
 *             .setVisible(true)
 *             .setCastShadows(true));
 * }
 * </pre>
 */
@RequiresComponent(TransformComponent.class)
public class MeshRendererComponent extends Component {

    private Mesh mesh;
    private Material material;
    private boolean visible = true;
    private int sortingOrder = 0;
    private boolean castShadows = true;
    private boolean receiveShadows = true;

    /**
     * 创建默认网格渲染组件
     */
    public MeshRendererComponent() {}

    /**
     * 获取网格
     *
     * @return 网格，可能为 null
     */
    public Mesh getMesh() {
        return mesh;
    }

    /**
     * 设置网格
     *
     * @param mesh 网格
     * @return this（链式调用）
     */
    public MeshRendererComponent setMesh(Mesh mesh) {
        this.mesh = mesh;
        return this;
    }

    /**
     * 获取材质
     *
     * @return 材质，可能为 null
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * 设置材质
     *
     * @param material 材质
     * @return this（链式调用）
     */
    public MeshRendererComponent setMaterial(Material material) {
        this.material = material;
        return this;
    }

    /**
     * 获取材质实例（独立副本）
     *
     * <p>
     * 如果需要修改材质参数而不影响其他使用相同材质的组件，
     * 应该使用此方法获取独立副本。
     * </p>
     *
     * @return 材质的独立副本，如果材质为 null 则返回 null
     */
    public Material getMaterialInstance() {
        if (material == null) {
            return null;
        }
        Material instance = material.instantiate();
        this.material = instance;
        return instance;
    }

    /**
     * 检查是否可见
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * 设置是否可见
     *
     * @param visible 是否可见
     * @return this（链式调用）
     */
    public MeshRendererComponent setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * 获取排序顺序
     *
     * <p>
     * 值越小越先渲染。相同材质的物体会被批量渲染。
     * </p>
     */
    public int getSortingOrder() {
        return sortingOrder;
    }

    /**
     * 设置排序顺序
     *
     * @param sortingOrder 排序顺序
     * @return this（链式调用）
     */
    public MeshRendererComponent setSortingOrder(int sortingOrder) {
        this.sortingOrder = sortingOrder;
        return this;
    }

    /**
     * 检查是否投射阴影
     */
    public boolean isCastShadows() {
        return castShadows;
    }

    /**
     * 设置是否投射阴影
     *
     * @param castShadows 是否投射阴影
     * @return this（链式调用）
     */
    public MeshRendererComponent setCastShadows(boolean castShadows) {
        this.castShadows = castShadows;
        return this;
    }

    /**
     * 检查是否接收阴影
     */
    public boolean isReceiveShadows() {
        return receiveShadows;
    }

    /**
     * 设置是否接收阴影
     *
     * @param receiveShadows 是否接收阴影
     * @return this（链式调用）
     */
    public MeshRendererComponent setReceiveShadows(boolean receiveShadows) {
        this.receiveShadows = receiveShadows;
        return this;
    }
}
