package moe.takochan.takorender.api.component;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import moe.takochan.takorender.api.ecs.Component;

/**
 * 变换组件 - 存储实体的位置、旋转和缩放数据
 *
 * <p>
 * TransformComponent 是 ECS 架构中最基础的组件之一，
 * 几乎所有需要空间定位的实体都需要此组件。
 * </p>
 *
 * <p>
 * <b>ECS 原则</b>: 此组件只存储数据，不包含任何逻辑。
 * 矩阵计算由 {@link moe.takochan.takorender.api.system.TransformSystem} 处理。
 * </p>
 *
 * <p>
 * <b>坐标系</b>: 使用 Minecraft 坐标系（Y 轴向上，Z 轴向南）
 * </p>
 *
 * <p>
 * <b>旋转约定</b>:
 * </p>
 * <ul>
 * <li>Pitch: 绕 X 轴旋转（俯仰）</li>
 * <li>Yaw: 绕 Y 轴旋转（偏航）</li>
 * <li>Roll: 绕 Z 轴旋转（翻滚）</li>
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
 *     Entity entity = world.createEntity();
 *     TransformComponent transform = new TransformComponent();
 *     transform.setPosition(100.0, 64.0, 200.0);
 *     transform.setRotation(0, 90, 0); // 面向西
 *     entity.addComponent(transform);
 * }
 * </pre>
 */
public class TransformComponent extends Component {

    // 基础变换数据
    private final Vector3f position = new Vector3f(0, 0, 0);
    private final Vector3f rotation = new Vector3f(0, 0, 0);
    private final Vector3f scale = new Vector3f(1, 1, 1);

    // 派生数据（由 TransformSystem 计算和更新）
    private final Matrix4f worldMatrix = new Matrix4f();
    private final Vector3f forward = new Vector3f(0, 0, -1);
    private final Vector3f up = new Vector3f(0, 1, 0);
    private final Vector3f right = new Vector3f(1, 0, 0);

    // 脏标记
    private boolean dirty = true;

    /**
     * 创建默认变换组件（原点位置，无旋转，单位缩放）
     */
    public TransformComponent() {}

    /**
     * 创建指定位置的变换组件
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     */
    public TransformComponent(double x, double y, double z) {
        this.position.set((float) x, (float) y, (float) z);
    }

    /**
     * 创建指定位置的变换组件
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     */
    public TransformComponent(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    /**
     * 获取位置向量（只读副本）
     *
     * @return 位置向量副本
     */
    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    /**
     * 获取位置向量（直接引用，修改后需调用 markDirty）
     *
     * @return 位置向量引用
     */
    public Vector3f getPositionRef() {
        return position;
    }

    /**
     * 设置位置
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     */
    public void setPosition(double x, double y, double z) {
        this.position.set((float) x, (float) y, (float) z);
        markDirty();
    }

    /**
     * 设置位置
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     */
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        markDirty();
    }

    /**
     * 设置位置
     *
     * @param position 位置向量
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
        markDirty();
    }

    /**
     * 获取旋转角度（度）
     *
     * @return (pitch, yaw, roll) 向量副本
     */
    public Vector3f getRotation() {
        return new Vector3f(rotation);
    }

    /**
     * 获取旋转向量（直接引用，修改后需调用 markDirty）
     *
     * @return 旋转向量引用
     */
    public Vector3f getRotationRef() {
        return rotation;
    }

    /**
     * 获取 Pitch 角度（度）
     *
     * @return Pitch 角度
     */
    public float getPitch() {
        return rotation.x;
    }

    /**
     * 获取 Yaw 角度（度）
     *
     * @return Yaw 角度
     */
    public float getYaw() {
        return rotation.y;
    }

    /**
     * 获取 Roll 角度（度）
     *
     * @return Roll 角度
     */
    public float getRoll() {
        return rotation.z;
    }

    /**
     * 设置旋转角度（度）
     *
     * @param pitch Pitch 角度（俯仰）
     * @param yaw   Yaw 角度（偏航）
     * @param roll  Roll 角度（翻滚）
     */
    public void setRotation(float pitch, float yaw, float roll) {
        this.rotation.set(pitch, yaw, roll);
        markDirty();
    }

    /**
     * 获取缩放向量（只读副本）
     *
     * @return 缩放向量副本
     */
    public Vector3f getScale() {
        return new Vector3f(scale);
    }

    /**
     * 获取缩放向量（直接引用，修改后需调用 markDirty）
     *
     * @return 缩放向量引用
     */
    public Vector3f getScaleRef() {
        return scale;
    }

    /**
     * 设置统一缩放
     *
     * @param scale 缩放值
     */
    public void setScale(float scale) {
        this.scale.set(scale, scale, scale);
        markDirty();
    }

    /**
     * 设置分轴缩放
     *
     * @param scaleX X 轴缩放
     * @param scaleY Y 轴缩放
     * @param scaleZ Z 轴缩放
     */
    public void setScale(float scaleX, float scaleY, float scaleZ) {
        this.scale.set(scaleX, scaleY, scaleZ);
        markDirty();
    }

    /**
     * 检查是否需要重新计算矩阵
     *
     * @return true 表示数据已变更
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * 标记数据已变更
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * 清除脏标记（由 TransformSystem 调用）
     */
    public void clearDirty() {
        this.dirty = false;
    }

    /**
     * 获取世界变换矩阵（直接引用）
     *
     * <p>
     * 注意：矩阵由 TransformSystem 负责计算和更新。
     * 如果 isDirty() 为 true，矩阵可能未更新。
     * </p>
     *
     * @return 世界变换矩阵引用
     */
    public Matrix4f getWorldMatrix() {
        return worldMatrix;
    }

    /**
     * 获取前向向量（直接引用）
     *
     * <p>
     * 注意：由 TransformSystem 负责计算和更新。
     * </p>
     *
     * @return 前向向量引用
     */
    public Vector3f getForward() {
        return forward;
    }

    /**
     * 获取上方向量（直接引用）
     *
     * <p>
     * 注意：由 TransformSystem 负责计算和更新。
     * </p>
     *
     * @return 上方向量引用
     */
    public Vector3f getUp() {
        return up;
    }

    /**
     * 获取右向向量（直接引用）
     *
     * <p>
     * 注意：由 TransformSystem 负责计算和更新。
     * </p>
     *
     * @return 右向向量引用
     */
    public Vector3f getRight() {
        return right;
    }

    /**
     * 平移位置
     *
     * @param dx X 方向增量
     * @param dy Y 方向增量
     * @param dz Z 方向增量
     */
    public void translate(float dx, float dy, float dz) {
        position.add(dx, dy, dz);
        markDirty();
    }

    /**
     * 旋转（增量）
     *
     * @param dPitch Pitch 增量
     * @param dYaw   Yaw 增量
     * @param dRoll  Roll 增量
     */
    public void rotate(float dPitch, float dYaw, float dRoll) {
        rotation.add(dPitch, dYaw, dRoll);
        markDirty();
    }
}
