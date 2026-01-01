package moe.takochan.takorender.api.component;

import org.joml.Matrix4f;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 相机组件 - 存储相机的投影参数和渲染状态
 *
 * <p>
 * CameraComponent 定义了渲染时的视角和投影方式。
 * 必须与 {@link TransformComponent} 配合使用（位置和朝向由 Transform 控制）。
 * </p>
 *
 * <p>
 * <b>投影类型</b>:
 * </p>
 * <ul>
 * <li>{@link ProjectionType#PERSPECTIVE}: 透视投影（3D 场景）</li>
 * <li>{@link ProjectionType#ORTHOGRAPHIC}: 正交投影（2D/UI/等距视角）</li>
 * </ul>
 *
 * <p>
 * <b>FOV 设计</b>: 只存储垂直视场角（vFOV），水平视场角（hFOV）根据宽高比自动计算。
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
 *     Entity cameraEntity = world.createEntity();
 *     cameraEntity.addComponent(new TransformComponent(0, 64, 0));
 *     cameraEntity.addComponent(
 *         new CameraComponent().setPerspective(70.0f, 16.0f / 9.0f, 0.05f, 256.0f)
 *             .setActive(true));
 * }
 * </pre>
 */
@RequiresComponent(TransformComponent.class)
public class CameraComponent extends Component {

    /**
     * 投影类型枚举
     */
    public enum ProjectionType {
        /** 透视投影 - 用于 3D 场景渲染 */
        PERSPECTIVE,
        /** 正交投影 - 用于 2D/UI/等距视角渲染 */
        ORTHOGRAPHIC
    }

    private ProjectionType projectionType = ProjectionType.PERSPECTIVE;

    private float vFov = 70.0f;
    private float aspectRatio = 16.0f / 9.0f;
    private float nearPlane = 0.05f;
    private float farPlane = 256.0f;

    private float orthoLeft = -10.0f;
    private float orthoRight = 10.0f;
    private float orthoBottom = -10.0f;
    private float orthoTop = 10.0f;

    private int viewportX = 0;
    private int viewportY = 0;
    private int viewportWidth = 0;
    private int viewportHeight = 0;

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f viewProjectionMatrix = new Matrix4f();

    private boolean projectionDirty = true;
    private boolean viewDirty = true;

    private boolean active = false;
    private int priority = 0;

    private boolean syncWithMinecraft = false;

    /**
     * 创建默认相机组件（透视投影，70° FOV）
     */
    public CameraComponent() {}

    /**
     * 创建指定 FOV 的透视相机
     *
     * @param vFov 垂直视场角（度）
     */
    public CameraComponent(float vFov) {
        this.vFov = vFov;
    }

    /**
     * 创建透视相机
     *
     * @param vFov      垂直视场角（度）
     * @param nearPlane 近裁剪面距离
     * @param farPlane  远裁剪面距离
     */
    public CameraComponent(float vFov, float nearPlane, float farPlane) {
        this.vFov = vFov;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
    }

    /**
     * 设置透视投影参数
     *
     * @param vFov   垂直视场角（度）
     * @param aspect 宽高比（width / height）
     * @param near   近裁剪面距离
     * @param far    远裁剪面距离
     * @return this（链式调用）
     */
    public CameraComponent setPerspective(float vFov, float aspect, float near, float far) {
        this.projectionType = ProjectionType.PERSPECTIVE;
        this.vFov = vFov;
        this.aspectRatio = aspect;
        this.nearPlane = near;
        this.farPlane = far;
        this.projectionDirty = true;
        return this;
    }

    /**
     * 设置正交投影参数
     *
     * @param left   左边界
     * @param right  右边界
     * @param bottom 下边界
     * @param top    上边界
     * @param near   近裁剪面距离
     * @param far    远裁剪面距离
     * @return this（链式调用）
     */
    public CameraComponent setOrthographic(float left, float right, float bottom, float top, float near, float far) {
        this.projectionType = ProjectionType.ORTHOGRAPHIC;
        this.orthoLeft = left;
        this.orthoRight = right;
        this.orthoBottom = bottom;
        this.orthoTop = top;
        this.nearPlane = near;
        this.farPlane = far;
        this.projectionDirty = true;
        return this;
    }

    /**
     * 获取投影类型
     */
    public ProjectionType getProjectionType() {
        return projectionType;
    }

    /**
     * 获取垂直视场角（度）
     */
    public float getVFov() {
        return vFov;
    }

    /**
     * 设置垂直视场角（度）
     */
    public void setVFov(float vFov) {
        this.vFov = vFov;
        this.projectionDirty = true;
    }

    /**
     * 获取水平视场角（度）- 根据 vFOV 和宽高比自动计算
     *
     * <p>
     * hFOV = 2 * atan(tan(vFOV/2) * aspectRatio)
     * </p>
     */
    public float getHFov() {
        double vFovRad = Math.toRadians(vFov);
        double hFovRad = 2.0 * Math.atan(Math.tan(vFovRad / 2.0) * aspectRatio);
        return (float) Math.toDegrees(hFovRad);
    }

    /**
     * 设置水平视场角（度）- 自动转换为 vFOV 存储
     *
     * <p>
     * vFOV = 2 * atan(tan(hFOV/2) / aspectRatio)
     * </p>
     */
    public void setHFov(float hFov) {
        double hFovRad = Math.toRadians(hFov);
        double vFovRad = 2.0 * Math.atan(Math.tan(hFovRad / 2.0) / aspectRatio);
        this.vFov = (float) Math.toDegrees(vFovRad);
        this.projectionDirty = true;
    }

    /**
     * 获取宽高比
     */
    public float getAspectRatio() {
        return aspectRatio;
    }

    /**
     * 设置宽高比
     *
     * @param aspect 宽高比（width / height）
     */
    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
        this.projectionDirty = true;
    }

    /**
     * 获取近裁剪面距离
     */
    public float getNearPlane() {
        return nearPlane;
    }

    /**
     * 设置近裁剪面距离
     */
    public void setNearPlane(float nearPlane) {
        this.nearPlane = nearPlane;
        this.projectionDirty = true;
    }

    /**
     * 获取远裁剪面距离
     */
    public float getFarPlane() {
        return farPlane;
    }

    /**
     * 设置远裁剪面距离
     */
    public void setFarPlane(float farPlane) {
        this.farPlane = farPlane;
        this.projectionDirty = true;
    }

    /**
     * 获取正交投影左边界
     */
    public float getOrthoLeft() {
        return orthoLeft;
    }

    /**
     * 获取正交投影右边界
     */
    public float getOrthoRight() {
        return orthoRight;
    }

    /**
     * 获取正交投影下边界
     */
    public float getOrthoBottom() {
        return orthoBottom;
    }

    /**
     * 获取正交投影上边界
     */
    public float getOrthoTop() {
        return orthoTop;
    }

    /**
     * 设置视口
     *
     * @param x      视口左下角 X 坐标
     * @param y      视口左下角 Y 坐标
     * @param width  视口宽度
     * @param height 视口高度
     */
    public void setViewport(int x, int y, int width, int height) {
        this.viewportX = x;
        this.viewportY = y;
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    /**
     * 获取视口 X 坐标
     */
    public int getViewportX() {
        return viewportX;
    }

    /**
     * 获取视口 Y 坐标
     */
    public int getViewportY() {
        return viewportY;
    }

    /**
     * 获取视口宽度
     */
    public int getViewportWidth() {
        return viewportWidth;
    }

    /**
     * 获取视口高度
     */
    public int getViewportHeight() {
        return viewportHeight;
    }

    /**
     * 检查此相机是否为活动相机
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 设置此相机是否为活动相机
     *
     * @param active 是否活动
     * @return this（链式调用）
     */
    public CameraComponent setActive(boolean active) {
        this.active = active;
        return this;
    }

    /**
     * 获取渲染优先级（值越小越先渲染）
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 设置渲染优先级
     *
     * @param priority 优先级值
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * 检查是否与 Minecraft 相机同步
     */
    public boolean isSyncWithMinecraft() {
        return syncWithMinecraft;
    }

    /**
     * 设置是否与 Minecraft 相机同步
     *
     * <p>
     * 启用后，CameraSystem 会自动从 Minecraft 玩家获取位置和视角。
     * </p>
     *
     * @param sync 是否同步
     * @return this（链式调用）
     */
    public CameraComponent setSyncWithMinecraft(boolean sync) {
        this.syncWithMinecraft = sync;
        return this;
    }

    /**
     * 检查投影矩阵是否需要重新计算
     */
    public boolean isProjectionDirty() {
        return projectionDirty;
    }

    /**
     * 标记投影矩阵需要重新计算
     */
    public void markProjectionDirty() {
        this.projectionDirty = true;
    }

    /**
     * 检查视图矩阵是否需要重新计算
     */
    public boolean isViewDirty() {
        return viewDirty;
    }

    /**
     * 标记视图矩阵需要重新计算
     */
    public void markViewDirty() {
        this.viewDirty = true;
    }

    /**
     * 清除所有脏标记（由 CameraSystem 调用）
     */
    public void clearDirtyFlags() {
        this.projectionDirty = false;
        this.viewDirty = false;
    }

    /**
     * 获取投影矩阵
     *
     * <p>
     * 注意：矩阵由 CameraSystem 负责计算和更新。
     * </p>
     */
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    /**
     * 获取视图矩阵
     *
     * <p>
     * 注意：矩阵由 CameraSystem 负责计算和更新。
     * </p>
     */
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    /**
     * 获取视图投影矩阵（View * Projection）
     *
     * <p>
     * 注意：矩阵由 CameraSystem 负责计算和更新。
     * </p>
     */
    public Matrix4f getViewProjectionMatrix() {
        return viewProjectionMatrix;
    }
}
