package moe.takochan.takorender.api.component;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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

    /**
     * 将世界坐标转换为屏幕坐标（NDC）
     *
     * <p>
     * 返回值 (x, y, z) 中：
     * </p>
     * <ul>
     * <li>x, y: 屏幕坐标（-1 到 1，左下角为 -1,-1）</li>
     * <li>z: 深度值（0 到 1，近平面为 0）</li>
     * <li>w: 用于判断点是否在相机前方（正值表示在前方）</li>
     * </ul>
     *
     * @param worldPos 世界坐标
     * @return NDC 坐标 (x, y, z, w)，w 为裁剪空间 w 分量
     */
    public Vector4f project(Vector3f worldPos) {
        Vector4f clipPos = new Vector4f(worldPos.x, worldPos.y, worldPos.z, 1.0f);
        clipPos.mul(viewProjectionMatrix);

        // 保存 w 用于判断是否在相机前方
        float w = clipPos.w;

        // 透视除法得到 NDC
        if (Math.abs(w) > 1e-6f) {
            clipPos.x /= w;
            clipPos.y /= w;
            clipPos.z /= w;
        }

        // 返回 NDC (x, y, z) 和原始 w
        return new Vector4f(clipPos.x, clipPos.y, clipPos.z, w);
    }

    /**
     * 将世界坐标转换为视口像素坐标
     *
     * <p>
     * 返回值 (x, y) 为视口内的像素坐标，原点在左下角。
     * z 分量为深度值，w 分量用于判断是否在相机前方。
     * </p>
     *
     * @param worldPos 世界坐标
     * @return 视口坐标 (pixelX, pixelY, depth, clipW)
     */
    public Vector4f projectToViewport(Vector3f worldPos) {
        Vector4f ndc = project(worldPos);

        // NDC (-1 to 1) 转换为视口像素坐标
        float pixelX = viewportX + (ndc.x + 1.0f) * 0.5f * viewportWidth;
        float pixelY = viewportY + (ndc.y + 1.0f) * 0.5f * viewportHeight;

        return new Vector4f(pixelX, pixelY, ndc.z, ndc.w);
    }

    /**
     * 将屏幕坐标（NDC）转换为世界坐标
     *
     * <p>
     * 需要提供深度值来确定射线上的具体点。
     * </p>
     *
     * @param ndcX  NDC x 坐标（-1 到 1）
     * @param ndcY  NDC y 坐标（-1 到 1）
     * @param depth 深度值（0 到 1，0 为近平面，1 为远平面）
     * @return 世界坐标
     */
    public Vector3f unproject(float ndcX, float ndcY, float depth) {
        // 计算逆视图投影矩阵
        Matrix4f invViewProj = new Matrix4f(viewProjectionMatrix).invert();

        // NDC 深度转换为裁剪空间 z（-1 到 1）
        float clipZ = depth * 2.0f - 1.0f;

        // 裁剪空间坐标
        Vector4f clipPos = new Vector4f(ndcX, ndcY, clipZ, 1.0f);
        clipPos.mul(invViewProj);

        // 透视除法
        if (Math.abs(clipPos.w) > 1e-6f) {
            clipPos.x /= clipPos.w;
            clipPos.y /= clipPos.w;
            clipPos.z /= clipPos.w;
        }

        return new Vector3f(clipPos.x, clipPos.y, clipPos.z);
    }

    /**
     * 将视口像素坐标转换为世界坐标
     *
     * @param pixelX 视口 x 坐标（像素）
     * @param pixelY 视口 y 坐标（像素）
     * @param depth  深度值（0 到 1）
     * @return 世界坐标
     */
    public Vector3f unprojectFromViewport(float pixelX, float pixelY, float depth) {
        // 视口像素坐标转换为 NDC
        float ndcX = (pixelX - viewportX) / viewportWidth * 2.0f - 1.0f;
        float ndcY = (pixelY - viewportY) / viewportHeight * 2.0f - 1.0f;

        return unproject(ndcX, ndcY, depth);
    }

    /**
     * 获取从相机发出经过屏幕点的射线方向
     *
     * <p>
     * 用于拾取（picking）等功能。返回归一化的方向向量。
     * </p>
     *
     * @param ndcX NDC x 坐标（-1 到 1）
     * @param ndcY NDC y 坐标（-1 到 1）
     * @return 射线方向（归一化）
     */
    public Vector3f getRayDirection(float ndcX, float ndcY) {
        Vector3f nearPoint = unproject(ndcX, ndcY, 0.0f);
        Vector3f farPoint = unproject(ndcX, ndcY, 1.0f);

        return farPoint.sub(nearPoint)
            .normalize();
    }

    /**
     * 检查世界坐标点是否在相机视锥内
     *
     * @param worldPos 世界坐标
     * @return true 如果点在视锥内（可见）
     */
    public boolean isInFrustum(Vector3f worldPos) {
        Vector4f projected = project(worldPos);

        // 检查是否在相机前方且在 NDC 范围内
        return projected.w > 0 && projected.x >= -1.0f
            && projected.x <= 1.0f
            && projected.y >= -1.0f
            && projected.y <= 1.0f
            && projected.z >= 0.0f
            && projected.z <= 1.0f;
    }

    /**
     * 检查当前 FOV/Ortho 参数是否与 aspectRatio 一致
     *
     * <p>
     * 用于验证宽高比变化后是否需要重新计算投影参数。
     * </p>
     *
     * @return true 表示一致，false 表示需要调整
     */
    public boolean isAspectValid() {
        final float epsilon = 1e-6f;
        if (projectionType == ProjectionType.PERSPECTIVE) {
            double vFovRad = Math.toRadians(vFov);
            double hFovRad = 2.0 * Math.atan(Math.tan(vFovRad / 2.0) * aspectRatio);
            double computedAspect = Math.tan(hFovRad / 2.0) / Math.tan(vFovRad / 2.0);
            return Math.abs(computedAspect - aspectRatio) < epsilon;
        } else {
            float orthoWidth = orthoRight - orthoLeft;
            float orthoHeight = orthoTop - orthoBottom;
            float orthoAspect = orthoWidth / orthoHeight;
            return Math.abs(orthoAspect - aspectRatio) < epsilon;
        }
    }
}
