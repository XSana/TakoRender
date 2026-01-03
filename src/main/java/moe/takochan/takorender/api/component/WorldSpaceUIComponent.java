package moe.takochan.takorender.api.component;

import org.joml.Vector2f;
import org.joml.Vector3f;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 世界空间 UI 组件
 *
 * <p>
 * 用于将 3D 世界坐标转换为 2D 屏幕坐标，实现名字标签、血条等跟随效果。
 * </p>
 *
 * <p>
 * <b>使用场景</b>:
 * </p>
 * <ul>
 * <li>玩家/NPC 头顶名字标签</li>
 * <li>生物血条显示</li>
 * <li>3D 标记点</li>
 * <li>伤害数字飘字</li>
 * </ul>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     Entity nameTag = world.createEntity();
 *     nameTag.addComponent(new TransformComponent(0, 70, 0));
 *     nameTag.addComponent(
 *         new WorldSpaceUIComponent().setOffset(0, 2.5f, 0) // 头顶偏移
 *             .setScreenOffset(0, 10) // 屏幕上再偏移 10 像素
 *             .setScaleWithDistance(true)
 *             .setMinScale(0.5f)
 *             .setMaxDistance(64.0f));
 * }
 * </pre>
 *
 * <p>
 * <b>屏幕坐标更新</b>: 由 WorldSpaceUISystem 每帧根据 CameraComponent 计算。
 * </p>
 */
@RequiresComponent(TransformComponent.class)
public class WorldSpaceUIComponent extends Component {

    /** 世界空间偏移量（相对于 Transform 位置） */
    private final Vector3f worldOffset = new Vector3f(0, 0, 0);

    /** 屏幕空间偏移量（像素） */
    private final Vector2f screenOffset = new Vector2f(0, 0);

    /** 计算后的屏幕坐标（由 System 更新） */
    private final Vector2f screenPosition = new Vector2f(0, 0);

    /** 当前深度值（0-1，用于排序） */
    private float depth = 0;

    /** 是否在屏幕内可见 */
    private boolean visible = false;

    /** 是否根据距离缩放 */
    private boolean scaleWithDistance = true;

    /** 距离缩放因子（由 System 计算） */
    private float distanceScale = 1.0f;

    /** 最小缩放比例 */
    private float minScale = 0.3f;

    /** 最大缩放比例 */
    private float maxScale = 2.0f;

    /** 缩放参考距离（此距离时 scale = 1.0） */
    private float referenceDistance = 10.0f;

    /** 最大可见距离（超出则不显示） */
    private float maxDistance = 128.0f;

    /** 当前到相机的距离（由 System 计算） */
    private float currentDistance = 0;

    /** 是否启用遮挡检测 */
    private boolean occlusionCheck = false;

    /** 是否被遮挡（由 System 计算） */
    private boolean occluded = false;

    /**
     * 创建默认世界空间 UI 组件
     */
    public WorldSpaceUIComponent() {}

    /**
     * 设置世界空间偏移量
     *
     * @param x X 偏移
     * @param y Y 偏移
     * @param z Z 偏移
     * @return this（链式调用）
     */
    public WorldSpaceUIComponent setOffset(float x, float y, float z) {
        worldOffset.set(x, y, z);
        return this;
    }

    /**
     * 设置世界空间偏移量
     *
     * @param offset 偏移向量
     * @return this（链式调用）
     */
    public WorldSpaceUIComponent setOffset(Vector3f offset) {
        worldOffset.set(offset);
        return this;
    }

    /**
     * 获取世界空间偏移量
     */
    public Vector3f getWorldOffset() {
        return worldOffset;
    }

    /**
     * 设置屏幕空间偏移量（像素）
     *
     * @param x X 偏移
     * @param y Y 偏移
     * @return this（链式调用）
     */
    public WorldSpaceUIComponent setScreenOffset(float x, float y) {
        screenOffset.set(x, y);
        return this;
    }

    /**
     * 获取屏幕空间偏移量
     */
    public Vector2f getScreenOffset() {
        return screenOffset;
    }

    /**
     * 获取计算后的屏幕坐标
     *
     * <p>
     * 坐标原点在左下角。
     * </p>
     */
    public Vector2f getScreenPosition() {
        return screenPosition;
    }

    /**
     * 设置屏幕坐标（由 System 调用）
     */
    public void setScreenPosition(float x, float y) {
        screenPosition.set(x, y);
    }

    /**
     * 获取深度值
     */
    public float getDepth() {
        return depth;
    }

    /**
     * 设置深度值（由 System 调用）
     */
    public void setDepth(float depth) {
        this.depth = depth;
    }

    /**
     * 检查是否在屏幕内可见
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * 设置可见性（由 System 调用）
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * 检查是否根据距离缩放
     */
    public boolean isScaleWithDistance() {
        return scaleWithDistance;
    }

    /**
     * 设置是否根据距离缩放
     *
     * @param scaleWithDistance true 则根据距离调整缩放
     * @return this（链式调用）
     */
    public WorldSpaceUIComponent setScaleWithDistance(boolean scaleWithDistance) {
        this.scaleWithDistance = scaleWithDistance;
        return this;
    }

    /**
     * 获取距离缩放因子
     */
    public float getDistanceScale() {
        return distanceScale;
    }

    /**
     * 设置距离缩放因子（由 System 调用）
     */
    public void setDistanceScale(float distanceScale) {
        this.distanceScale = distanceScale;
    }

    /**
     * 获取最小缩放比例
     */
    public float getMinScale() {
        return minScale;
    }

    /**
     * 设置最小缩放比例
     *
     * @param minScale 最小缩放（默认 0.3）
     * @return this（链式调用）
     */
    public WorldSpaceUIComponent setMinScale(float minScale) {
        this.minScale = minScale;
        return this;
    }

    /**
     * 获取最大缩放比例
     */
    public float getMaxScale() {
        return maxScale;
    }

    /**
     * 设置最大缩放比例
     *
     * @param maxScale 最大缩放（默认 2.0）
     * @return this（链式调用）
     */
    public WorldSpaceUIComponent setMaxScale(float maxScale) {
        this.maxScale = maxScale;
        return this;
    }

    /**
     * 获取缩放参考距离
     */
    public float getReferenceDistance() {
        return referenceDistance;
    }

    /**
     * 设置缩放参考距离
     *
     * <p>
     * 在此距离时 distanceScale = 1.0。
     * </p>
     *
     * @param referenceDistance 参考距离（默认 10.0）
     * @return this（链式调用）
     */
    public WorldSpaceUIComponent setReferenceDistance(float referenceDistance) {
        this.referenceDistance = referenceDistance;
        return this;
    }

    /**
     * 获取最大可见距离
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * 设置最大可见距离
     *
     * <p>
     * 超出此距离则不显示。
     * </p>
     *
     * @param maxDistance 最大距离（默认 128.0）
     * @return this（链式调用）
     */
    public WorldSpaceUIComponent setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    /**
     * 获取当前到相机的距离
     */
    public float getCurrentDistance() {
        return currentDistance;
    }

    /**
     * 设置当前距离（由 System 调用）
     */
    public void setCurrentDistance(float currentDistance) {
        this.currentDistance = currentDistance;
    }

    /**
     * 检查是否启用遮挡检测
     */
    public boolean isOcclusionCheck() {
        return occlusionCheck;
    }

    /**
     * 设置是否启用遮挡检测
     *
     * @param occlusionCheck true 则检测是否被遮挡
     * @return this（链式调用）
     */
    public WorldSpaceUIComponent setOcclusionCheck(boolean occlusionCheck) {
        this.occlusionCheck = occlusionCheck;
        return this;
    }

    /**
     * 检查是否被遮挡
     */
    public boolean isOccluded() {
        return occluded;
    }

    /**
     * 设置遮挡状态（由 System 调用）
     */
    public void setOccluded(boolean occluded) {
        this.occluded = occluded;
    }
}
