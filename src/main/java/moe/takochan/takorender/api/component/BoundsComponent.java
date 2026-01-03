package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.graphics.AABB;

/**
 * 包围盒组件 - 存储 Entity 的边界数据
 *
 * <p>
 * BoundsComponent 存储本地空间和世界空间的 AABB 包围盒，
 * 用于视锥剔除 (Frustum Culling) 等空间优化。
 * </p>
 *
 * <p>
 * <b>数据流</b>:
 * </p>
 * <ol>
 * <li>用户设置 localBounds（或从 Mesh 自动获取）</li>
 * <li>TransformSystem 根据 Transform 计算 worldBounds</li>
 * <li>FrustumCullingSystem 使用 worldBounds 进行剔除</li>
 * </ol>
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
 *     entity.addComponent(new BoundsComponent(new AABB(-0.5f, 0, -0.5f, 0.5f, 2.0f, 0.5f))); // 1x2x1 方块
 *     entity.addComponent(new VisibilityComponent());
 *     entity.addComponent(new MeshRendererComponent().setMesh(mesh));
 * }
 * </pre>
 */
@RequiresComponent(TransformComponent.class)
public class BoundsComponent extends Component {

    /** 本地空间包围盒 */
    private AABB localBounds = AABB.UNIT;

    /** 世界空间包围盒（由 TransformSystem 计算） */
    private AABB worldBounds = AABB.UNIT;

    /** 是否需要重新计算 worldBounds */
    private boolean dirty = true;

    /**
     * 创建默认包围盒组件（单位立方体）
     */
    public BoundsComponent() {}

    /**
     * 创建指定本地包围盒的组件
     *
     * @param localBounds 本地空间包围盒
     */
    public BoundsComponent(AABB localBounds) {
        this.localBounds = localBounds;
    }

    /**
     * 获取本地空间包围盒
     */
    public AABB getLocalBounds() {
        return localBounds;
    }

    /**
     * 设置本地空间包围盒
     *
     * @param localBounds 本地包围盒
     * @return this（链式调用）
     */
    public BoundsComponent setLocalBounds(AABB localBounds) {
        this.localBounds = localBounds != null ? localBounds : AABB.UNIT;
        this.dirty = true;
        return this;
    }

    /**
     * 获取世界空间包围盒
     *
     * <p>
     * 由 TransformSystem 计算和更新。
     * </p>
     */
    public AABB getWorldBounds() {
        return worldBounds;
    }

    /**
     * 设置世界空间包围盒（由 TransformSystem 调用）
     *
     * @param worldBounds 世界包围盒
     */
    public void setWorldBounds(AABB worldBounds) {
        this.worldBounds = worldBounds != null ? worldBounds : AABB.UNIT;
        this.dirty = false;
    }

    /**
     * 检查是否需要重新计算
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * 标记需要重新计算
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * 清除脏标记
     */
    public void clearDirty() {
        this.dirty = false;
    }
}
