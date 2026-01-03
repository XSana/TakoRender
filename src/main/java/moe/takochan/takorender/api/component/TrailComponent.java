package moe.takochan.takorender.api.component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.joml.Vector3f;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.graphics.RenderQueue;

/**
 * 拖尾组件
 *
 * <p>
 * 存储实体的位置历史，用于渲染运动拖尾效果。
 * 适用于剑光、粒子轨迹、飞行物尾迹等。
 * </p>
 *
 * <p>
 * <b>工作原理</b>:
 * </p>
 * <ol>
 * <li>TrailSystem 每帧记录 Transform 位置到历史队列</li>
 * <li>历史点超过 maxPoints 时自动移除最旧的点</li>
 * <li>渲染时根据点的新旧程度应用颜色/宽度渐变</li>
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
 *     Entity sword = world.createEntity();
 *     sword.addComponent(new TransformComponent(0, 64, 0));
 *     sword.addComponent(
 *         new TrailComponent().setMaxPoints(32)
 *             .setStartColor(1.0f, 1.0f, 1.0f, 1.0f)
 *             .setEndColor(1.0f, 1.0f, 1.0f, 0.0f) // 渐隐
 *             .setStartWidth(0.5f)
 *             .setEndWidth(0.0f) // 渐细
 *             .setEmitting(true));
 * }
 * </pre>
 */
@RequiresComponent(TransformComponent.class)
public class TrailComponent extends Component {

    /**
     * 拖尾点数据
     */
    public static class TrailPoint {

        /** 位置 */
        public final Vector3f position = new Vector3f();
        /** 存在时间（秒） */
        public float age = 0;
        /** 创建时间戳 */
        public long timestamp;

        public TrailPoint(float x, float y, float z) {
            position.set(x, y, z);
            timestamp = System.currentTimeMillis();
        }
    }

    /** 位置历史队列（头部是最新，尾部是最旧） */
    private final Deque<TrailPoint> points = new ArrayDeque<>();

    /** 最大点数 */
    private int maxPoints = 32;

    /** 点的最大存在时间（秒，0 表示无限制） */
    private float maxAge = 0;

    /** 每帧最小移动距离才记录新点 */
    private float minDistance = 0.01f;

    /** 是否正在发射拖尾 */
    private boolean emitting = true;

    /** 起点颜色 */
    private float startColorR = 1.0f;
    private float startColorG = 1.0f;
    private float startColorB = 1.0f;
    private float startColorA = 1.0f;

    /** 终点颜色（最旧的点） */
    private float endColorR = 1.0f;
    private float endColorG = 1.0f;
    private float endColorB = 1.0f;
    private float endColorA = 0.0f;

    /** 起点宽度 */
    private float startWidth = 0.5f;

    /** 终点宽度 */
    private float endWidth = 0.0f;

    /** 渲染队列 */
    private RenderQueue renderQueue = RenderQueue.TRANSPARENT;

    /**
     * 创建默认拖尾组件
     */
    public TrailComponent() {}

    /**
     * 添加新的拖尾点
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     */
    public void addPoint(float x, float y, float z) {
        // 检查与最新点的距离
        if (!points.isEmpty()) {
            TrailPoint newest = points.peekFirst();
            float dx = x - newest.position.x;
            float dy = y - newest.position.y;
            float dz = z - newest.position.z;
            float distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < minDistance * minDistance) {
                return;
            }
        }

        // 添加新点
        points.addFirst(new TrailPoint(x, y, z));

        // 移除超出限制的旧点
        while (points.size() > maxPoints) {
            points.removeLast();
        }
    }

    /**
     * 更新所有点的 age 并移除过期的点
     *
     * @param deltaTime 帧时间（秒）
     */
    public void updateAges(float deltaTime) {
        Iterator<TrailPoint> iter = points.iterator();
        while (iter.hasNext()) {
            TrailPoint point = iter.next();
            point.age += deltaTime;

            // 移除过期的点
            if (maxAge > 0 && point.age > maxAge) {
                iter.remove();
            }
        }
    }

    /**
     * 清空所有拖尾点
     */
    public void clear() {
        points.clear();
    }

    /**
     * 获取拖尾点列表（只读迭代）
     */
    public Iterable<TrailPoint> getPoints() {
        return points;
    }

    /**
     * 获取点数量
     */
    public int getPointCount() {
        return points.size();
    }

    /**
     * 获取最大点数
     */
    public int getMaxPoints() {
        return maxPoints;
    }

    /**
     * 设置最大点数
     *
     * @param maxPoints 最大点数
     * @return this（链式调用）
     */
    public TrailComponent setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
        return this;
    }

    /**
     * 获取点最大存在时间（秒）
     */
    public float getMaxAge() {
        return maxAge;
    }

    /**
     * 设置点最大存在时间
     *
     * @param maxAge 最大存在时间（秒，0 表示无限制）
     * @return this（链式调用）
     */
    public TrailComponent setMaxAge(float maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    /**
     * 获取最小记录距离
     */
    public float getMinDistance() {
        return minDistance;
    }

    /**
     * 设置最小记录距离
     *
     * @param minDistance 最小距离
     * @return this（链式调用）
     */
    public TrailComponent setMinDistance(float minDistance) {
        this.minDistance = minDistance;
        return this;
    }

    /**
     * 检查是否正在发射拖尾
     */
    public boolean isEmitting() {
        return emitting;
    }

    /**
     * 设置是否发射拖尾
     *
     * @param emitting 是否发射
     * @return this（链式调用）
     */
    public TrailComponent setEmitting(boolean emitting) {
        this.emitting = emitting;
        return this;
    }

    /**
     * 获取起点颜色 R 分量
     */
    public float getStartColorR() {
        return startColorR;
    }

    /**
     * 获取起点颜色 G 分量
     */
    public float getStartColorG() {
        return startColorG;
    }

    /**
     * 获取起点颜色 B 分量
     */
    public float getStartColorB() {
        return startColorB;
    }

    /**
     * 获取起点颜色 A 分量
     */
    public float getStartColorA() {
        return startColorA;
    }

    /**
     * 设置起点颜色
     *
     * @param r 红色（0-1）
     * @param g 绿色（0-1）
     * @param b 蓝色（0-1）
     * @param a 透明度（0-1）
     * @return this（链式调用）
     */
    public TrailComponent setStartColor(float r, float g, float b, float a) {
        this.startColorR = r;
        this.startColorG = g;
        this.startColorB = b;
        this.startColorA = a;
        return this;
    }

    /**
     * 获取终点颜色 R 分量
     */
    public float getEndColorR() {
        return endColorR;
    }

    /**
     * 获取终点颜色 G 分量
     */
    public float getEndColorG() {
        return endColorG;
    }

    /**
     * 获取终点颜色 B 分量
     */
    public float getEndColorB() {
        return endColorB;
    }

    /**
     * 获取终点颜色 A 分量
     */
    public float getEndColorA() {
        return endColorA;
    }

    /**
     * 设置终点颜色
     *
     * @param r 红色（0-1）
     * @param g 绿色（0-1）
     * @param b 蓝色（0-1）
     * @param a 透明度（0-1）
     * @return this（链式调用）
     */
    public TrailComponent setEndColor(float r, float g, float b, float a) {
        this.endColorR = r;
        this.endColorG = g;
        this.endColorB = b;
        this.endColorA = a;
        return this;
    }

    /**
     * 获取起点宽度
     */
    public float getStartWidth() {
        return startWidth;
    }

    /**
     * 设置起点宽度
     *
     * @param startWidth 起点宽度
     * @return this（链式调用）
     */
    public TrailComponent setStartWidth(float startWidth) {
        this.startWidth = startWidth;
        return this;
    }

    /**
     * 获取终点宽度
     */
    public float getEndWidth() {
        return endWidth;
    }

    /**
     * 设置终点宽度
     *
     * @param endWidth 终点宽度
     * @return this（链式调用）
     */
    public TrailComponent setEndWidth(float endWidth) {
        this.endWidth = endWidth;
        return this;
    }

    /**
     * 设置宽度渐变
     *
     * @param startWidth 起点宽度
     * @param endWidth   终点宽度
     * @return this（链式调用）
     */
    public TrailComponent setWidthGradient(float startWidth, float endWidth) {
        this.startWidth = startWidth;
        this.endWidth = endWidth;
        return this;
    }

    /**
     * 获取渲染队列
     */
    public RenderQueue getRenderQueue() {
        return renderQueue;
    }

    /**
     * 设置渲染队列
     *
     * @param renderQueue 渲染队列
     * @return this（链式调用）
     */
    public TrailComponent setRenderQueue(RenderQueue renderQueue) {
        this.renderQueue = renderQueue != null ? renderQueue : RenderQueue.TRANSPARENT;
        return this;
    }

    /**
     * 获取指定索引点的插值比例（0=最新，1=最旧）
     *
     * @param index 点索引
     * @return 插值比例 0-1
     */
    public float getInterpolation(int index) {
        if (points.size() <= 1) {
            return 0;
        }
        return (float) index / (points.size() - 1);
    }
}
