package moe.takochan.takorender.api.graphics;

/**
 * 渲染队列枚举 - 定义渲染顺序分层
 *
 * <p>
 * RenderQueue 用于控制物体的渲染顺序，确保正确的渲染结果：
 * </p>
 * <ul>
 * <li>不透明物体先渲染（利用 Early-Z 优化）</li>
 * <li>透明物体后渲染（从后往前，正确混合）</li>
 * <li>叠加层最后渲染（UI、调试线条等）</li>
 * </ul>
 *
 * <p>
 * <b>渲染顺序</b>:
 * </p>
 * <ol>
 * <li>BACKGROUND (1000) - 天空盒、远景</li>
 * <li>OPAQUE (2000) - 不透明物体（默认）</li>
 * <li>TRANSPARENT (3000) - 透明/半透明物体</li>
 * <li>OVERLAY (4000) - UI、调试可视化</li>
 * </ol>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * {@code
 * // 不透明物体（默认）
 * meshRenderer.setRenderQueue(RenderQueue.OPAQUE);
 *
 * // 透明玻璃
 * meshRenderer.setRenderQueue(RenderQueue.TRANSPARENT);
 *
 * // 调试线条（始终可见）
 * lineRenderer.setRenderQueue(RenderQueue.OVERLAY);
 * }
 * </pre>
 */
public enum RenderQueue {

    /**
     * 背景层 - 天空盒、远景等
     *
     * <p>
     * 最先渲染，通常禁用深度写入。
     * </p>
     */
    BACKGROUND(1000),

    /**
     * 不透明层 - 默认渲染队列
     *
     * <p>
     * 从前往后渲染，利用 Early-Z 剔除。
     * </p>
     */
    OPAQUE(2000),

    /**
     * 透明层 - 半透明物体
     *
     * <p>
     * 从后往前渲染，确保正确的 Alpha 混合。
     * </p>
     */
    TRANSPARENT(3000),

    /**
     * 叠加层 - UI、调试可视化
     *
     * <p>
     * 最后渲染，通常禁用深度测试。
     * </p>
     */
    OVERLAY(4000);

    private final int priority;

    RenderQueue(int priority) {
        this.priority = priority;
    }

    /**
     * 获取渲染优先级
     *
     * <p>
     * 值越小越先渲染。
     * </p>
     *
     * @return 优先级值
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 检查是否需要深度排序
     *
     * <p>
     * 透明队列需要从后往前排序。
     * </p>
     *
     * @return 是否需要深度排序
     */
    public boolean requiresDepthSorting() {
        return this == TRANSPARENT;
    }

    /**
     * 检查是否为透明队列
     *
     * @return 是否透明
     */
    public boolean isTransparent() {
        return this == TRANSPARENT;
    }
}
