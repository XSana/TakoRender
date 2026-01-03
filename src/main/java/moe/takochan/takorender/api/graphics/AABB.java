package moe.takochan.takorender.api.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 轴对齐包围盒 (Axis-Aligned Bounding Box)
 *
 * <p>
 * AABB 用于表示 3D 对象的边界，主要用于：
 * </p>
 * <ul>
 * <li>视锥剔除 (Frustum Culling)</li>
 * <li>碰撞检测</li>
 * <li>空间划分</li>
 * </ul>
 *
 * <p>
 * AABB 是不可变对象，所有变换操作返回新实例。
 * </p>
 */
public final class AABB {

    /** 空包围盒（无限小） */
    public static final AABB EMPTY = new AABB(
        new Vector3f(Float.POSITIVE_INFINITY),
        new Vector3f(Float.NEGATIVE_INFINITY));

    /** 单位包围盒 (-0.5 到 0.5) */
    public static final AABB UNIT = new AABB(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f(0.5f, 0.5f, 0.5f));

    private final Vector3f min;
    private final Vector3f max;

    /**
     * 使用最小和最大点创建 AABB
     *
     * @param min 最小点
     * @param max 最大点
     */
    public AABB(Vector3f min, Vector3f max) {
        this.min = new Vector3f(min);
        this.max = new Vector3f(max);
    }

    /**
     * 使用坐标创建 AABB
     */
    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.min = new Vector3f(minX, minY, minZ);
        this.max = new Vector3f(maxX, maxY, maxZ);
    }

    /**
     * 从顶点数组计算 AABB
     *
     * @param vertices       顶点数据
     * @param vertexStride   每个顶点的浮点数数量
     * @param positionOffset 位置数据在顶点中的偏移（浮点数索引）
     * @return 计算得到的 AABB
     */
    public static AABB fromVertices(float[] vertices, int vertexStride, int positionOffset) {
        if (vertices == null || vertices.length < 3) {
            return EMPTY;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int i = positionOffset; i < vertices.length; i += vertexStride) {
            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 从 Vector3f 数组计算 AABB
     *
     * @param positions 位置数组
     * @return 计算得到的 AABB
     */
    public static AABB fromPositions(Vector3f[] positions) {
        if (positions == null || positions.length == 0) {
            return EMPTY;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (Vector3f pos : positions) {
            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            minZ = Math.min(minZ, pos.z);
            maxX = Math.max(maxX, pos.x);
            maxY = Math.max(maxY, pos.y);
            maxZ = Math.max(maxZ, pos.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 获取最小点
     */
    public Vector3f getMin() {
        return new Vector3f(min);
    }

    /**
     * 获取最大点
     */
    public Vector3f getMax() {
        return new Vector3f(max);
    }

    /**
     * 获取中心点
     */
    public Vector3f getCenter() {
        return new Vector3f((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f, (min.z + max.z) * 0.5f);
    }

    /**
     * 获取尺寸（各轴长度）
     */
    public Vector3f getSize() {
        return new Vector3f(max.x - min.x, max.y - min.y, max.z - min.z);
    }

    /**
     * 获取半尺寸（各轴长度的一半）
     */
    public Vector3f getHalfExtents() {
        return new Vector3f((max.x - min.x) * 0.5f, (max.y - min.y) * 0.5f, (max.z - min.z) * 0.5f);
    }

    /**
     * 检查是否包含某点
     *
     * @param point 要检查的点
     * @return true 如果点在 AABB 内
     */
    public boolean contains(Vector3f point) {
        return point.x >= min.x && point.x <= max.x
            && point.y >= min.y
            && point.y <= max.y
            && point.z >= min.z
            && point.z <= max.z;
    }

    /**
     * 检查是否与另一个 AABB 相交
     *
     * @param other 另一个 AABB
     * @return true 如果相交
     */
    public boolean intersects(AABB other) {
        return min.x <= other.max.x && max.x >= other.min.x
            && min.y <= other.max.y
            && max.y >= other.min.y
            && min.z <= other.max.z
            && max.z >= other.min.z;
    }

    /**
     * 合并两个 AABB
     *
     * @param other 另一个 AABB
     * @return 包含两个 AABB 的新 AABB
     */
    public AABB merge(AABB other) {
        return new AABB(
            Math.min(min.x, other.min.x),
            Math.min(min.y, other.min.y),
            Math.min(min.z, other.min.z),
            Math.max(max.x, other.max.x),
            Math.max(max.y, other.max.y),
            Math.max(max.z, other.max.z));
    }

    /**
     * 扩展 AABB 以包含某点
     *
     * @param point 要包含的点
     * @return 扩展后的新 AABB
     */
    public AABB expand(Vector3f point) {
        return new AABB(
            Math.min(min.x, point.x),
            Math.min(min.y, point.y),
            Math.min(min.z, point.z),
            Math.max(max.x, point.x),
            Math.max(max.y, point.y),
            Math.max(max.z, point.z));
    }

    /**
     * 应用变换矩阵
     *
     * <p>
     * 变换 AABB 的 8 个角点，然后重新计算包围盒。
     * 结果仍然是轴对齐的。
     * </p>
     *
     * @param matrix 变换矩阵
     * @return 变换后的新 AABB
     */
    public AABB transform(Matrix4f matrix) {
        Vector3f[] corners = getCorners();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (Vector3f corner : corners) {
            matrix.transformPosition(corner);
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 获取 8 个角点
     *
     * @return 角点数组
     */
    public Vector3f[] getCorners() {
        return new Vector3f[] { new Vector3f(min.x, min.y, min.z), new Vector3f(max.x, min.y, min.z),
            new Vector3f(min.x, max.y, min.z), new Vector3f(max.x, max.y, min.z), new Vector3f(min.x, min.y, max.z),
            new Vector3f(max.x, min.y, max.z), new Vector3f(min.x, max.y, max.z), new Vector3f(max.x, max.y, max.z) };
    }

    /**
     * 检查 AABB 是否有效（非空）
     */
    public boolean isValid() {
        return min.x <= max.x && min.y <= max.y && min.z <= max.z;
    }

    @Override
    public String toString() {
        return String
            .format("AABB[min=(%.2f,%.2f,%.2f), max=(%.2f,%.2f,%.2f)]", min.x, min.y, min.z, max.x, max.y, max.z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AABB)) return false;
        AABB other = (AABB) obj;
        return min.equals(other.min) && max.equals(other.max);
    }

    @Override
    public int hashCode() {
        return 31 * min.hashCode() + max.hashCode();
    }
}
