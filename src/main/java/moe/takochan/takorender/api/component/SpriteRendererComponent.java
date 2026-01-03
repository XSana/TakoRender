package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.graphics.RenderQueue;

/**
 * 2D 精灵渲染组件 - 存储 2D 四边形渲染数据
 *
 * <p>
 * SpriteRendererComponent 用于渲染 2D 图形元素，如 GUI 背景、图标、按钮等。
 * 在屏幕空间中渲染，使用正交投影。
 * </p>
 *
 * <p>
 * <b>ECS 原则</b>: Component 只存储数据，不包含逻辑。
 * 所有渲染由 SpriteRenderSystem 执行。
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
 *     Entity panel = world.createEntity();
 *     panel.addComponent(new TransformComponent(100, 100, 0)); // 屏幕坐标
 *     panel.addComponent(
 *         new SpriteRendererComponent().setSize(200, 150)
 *             .setColor(0.2f, 0.2f, 0.2f, 0.8f)); // 半透明深灰
 * }
 * </pre>
 */
@RequiresComponent(TransformComponent.class)
public class SpriteRendererComponent extends Component {

    /** 宽度（像素） */
    private float width = 100.0f;

    /** 高度（像素） */
    private float height = 100.0f;

    /** 颜色 R 分量（0-1） */
    private float colorR = 1.0f;

    /** 颜色 G 分量（0-1） */
    private float colorG = 1.0f;

    /** 颜色 B 分量（0-1） */
    private float colorB = 1.0f;

    /** 颜色 A 分量（0-1） */
    private float colorA = 1.0f;

    /** 排序顺序（同队列内排序） */
    private int sortingOrder = 0;

    /** 渲染队列 */
    private RenderQueue renderQueue = RenderQueue.OVERLAY;

    /**
     * 创建默认精灵渲染组件
     */
    public SpriteRendererComponent() {}

    /**
     * 获取宽度
     *
     * @return 宽度（像素）
     */
    public float getWidth() {
        return width;
    }

    /**
     * 设置宽度
     *
     * @param width 宽度（像素）
     * @return this（链式调用）
     */
    public SpriteRendererComponent setWidth(float width) {
        this.width = width;
        return this;
    }

    /**
     * 获取高度
     *
     * @return 高度（像素）
     */
    public float getHeight() {
        return height;
    }

    /**
     * 设置高度
     *
     * @param height 高度（像素）
     * @return this（链式调用）
     */
    public SpriteRendererComponent setHeight(float height) {
        this.height = height;
        return this;
    }

    /**
     * 设置尺寸
     *
     * @param width  宽度（像素）
     * @param height 高度（像素）
     * @return this（链式调用）
     */
    public SpriteRendererComponent setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * 获取颜色 R 分量
     *
     * @return R 分量（0-1）
     */
    public float getColorR() {
        return colorR;
    }

    /**
     * 获取颜色 G 分量
     *
     * @return G 分量（0-1）
     */
    public float getColorG() {
        return colorG;
    }

    /**
     * 获取颜色 B 分量
     *
     * @return B 分量（0-1）
     */
    public float getColorB() {
        return colorB;
    }

    /**
     * 获取颜色 A 分量
     *
     * @return A 分量（0-1）
     */
    public float getColorA() {
        return colorA;
    }

    /**
     * 设置颜色
     *
     * @param r 红色（0-1）
     * @param g 绿色（0-1）
     * @param b 蓝色（0-1）
     * @param a 透明度（0-1）
     * @return this（链式调用）
     */
    public SpriteRendererComponent setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
        return this;
    }

    /**
     * 设置颜色（不透明）
     *
     * @param r 红色（0-1）
     * @param g 绿色（0-1）
     * @param b 蓝色（0-1）
     * @return this（链式调用）
     */
    public SpriteRendererComponent setColor(float r, float g, float b) {
        return setColor(r, g, b, 1.0f);
    }

    /**
     * 设置颜色（使用 RGBA 整数）
     *
     * @param color RGBA 颜色值（0xRRGGBBAA）
     * @return this（链式调用）
     */
    public SpriteRendererComponent setColor(int color) {
        this.colorR = ((color >> 24) & 0xFF) / 255f;
        this.colorG = ((color >> 16) & 0xFF) / 255f;
        this.colorB = ((color >> 8) & 0xFF) / 255f;
        this.colorA = (color & 0xFF) / 255f;
        return this;
    }

    /**
     * 获取排序顺序
     *
     * @return 排序顺序
     */
    public int getSortingOrder() {
        return sortingOrder;
    }

    /**
     * 设置排序顺序
     *
     * <p>
     * 同一渲染队列内，sortingOrder 较小的先渲染（在下层）。
     * </p>
     *
     * @param sortingOrder 排序顺序
     * @return this（链式调用）
     */
    public SpriteRendererComponent setSortingOrder(int sortingOrder) {
        this.sortingOrder = sortingOrder;
        return this;
    }

    /**
     * 获取渲染队列
     *
     * @return 渲染队列（默认 OVERLAY）
     */
    public RenderQueue getRenderQueue() {
        return renderQueue;
    }

    /**
     * 设置渲染队列
     *
     * <p>
     * 精灵默认在 OVERLAY 层渲染（GUI 层）。
     * </p>
     *
     * @param renderQueue 渲染队列
     * @return this（链式调用）
     */
    public SpriteRendererComponent setRenderQueue(RenderQueue renderQueue) {
        this.renderQueue = renderQueue != null ? renderQueue : RenderQueue.OVERLAY;
        return this;
    }
}
