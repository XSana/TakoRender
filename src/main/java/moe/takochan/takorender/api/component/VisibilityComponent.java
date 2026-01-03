package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;

/**
 * 可见性组件 - 控制 Entity 的渲染可见性
 *
 * <p>
 * VisibilityComponent 统一管理所有渲染相关的可见性状态：
 * </p>
 * <ul>
 * <li>{@code visible}: 用户手动控制，替代加载/卸载</li>
 * <li>{@code culled}: FrustumCullingSystem 在 Phase 3.1 添加，视锥剔除结果</li>
 * </ul>
 *
 * <p>
 * <b>渲染条件</b>: {@code visible && !culled}
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
 *     entity.addComponent(new VisibilityComponent()); // 默认可见
 *     entity.addComponent(
 *         new MeshRendererComponent().setMesh(mesh)
 *             .setMaterial(material));
 *
 *     // 隐藏实体
 *     entity.getComponent(VisibilityComponent.class)
 *         .ifPresent(v -> v.setVisible(false));
 * }
 * </pre>
 */
public class VisibilityComponent extends Component {

    /** 用户控制的可见性 */
    private boolean visible = true;

    /** FrustumCullingSystem 写入的剔除结果 */
    private boolean culled = false;

    /**
     * 创建默认可见性组件（可见）
     */
    public VisibilityComponent() {}

    /**
     * 创建指定可见性的组件
     *
     * @param visible 是否可见
     */
    public VisibilityComponent(boolean visible) {
        this.visible = visible;
    }

    /**
     * 检查是否可见
     *
     * @return 是否可见
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
    public VisibilityComponent setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * 检查是否被视锥剔除
     *
     * @return true 表示被剔除（在视锥外）
     */
    public boolean isCulled() {
        return culled;
    }

    /**
     * 设置剔除状态（由 FrustumCullingSystem 调用）
     *
     * @param culled 是否被剔除
     */
    public void setCulled(boolean culled) {
        this.culled = culled;
    }

    /**
     * 检查是否应该渲染
     *
     * <p>
     * 渲染条件：visible && !culled
     * </p>
     *
     * @return 是否应该渲染
     */
    public boolean shouldRender() {
        return visible && !culled;
    }
}
