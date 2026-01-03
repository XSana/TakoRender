package moe.takochan.takorender.api.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * 视锥体 (Frustum)
 *
 * <p>
 * Frustum 表示相机的视锥体，由 6 个平面定义。
 * 用于视锥剔除 (Frustum Culling)，判断物体是否在视野内。
 * </p>
 *
 * <p>
 * <b>平面定义</b>:
 * </p>
 * <ul>
 * <li>LEFT, RIGHT: 左右裁剪面</li>
 * <li>BOTTOM, TOP: 上下裁剪面</li>
 * <li>NEAR, FAR: 远近裁剪面</li>
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
 *     Frustum frustum = new Frustum();
 *     frustum.update(camera.getViewProjectionMatrix());
 *
 *     if (frustum.intersects(bounds.getWorldBounds())) {
 *         // 物体在视锥内，需要渲染
 *     }
 * }
 * </pre>
 */
public class Frustum {

    /** 平面索引 */
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int TOP = 3;
    public static final int NEAR = 4;
    public static final int FAR = 5;

    /** 6 个平面 (ax + by + cz + d = 0 形式，存储为 (a, b, c, d)) */
    private final Vector4f[] planes = new Vector4f[6];

    public Frustum() {
        for (int i = 0; i < 6; i++) {
            planes[i] = new Vector4f();
        }
    }

    /**
     * 从视图投影矩阵更新视锥平面
     *
     * <p>
     * 使用 Gribb/Hartmann 方法从 ViewProjection 矩阵提取平面。
     * </p>
     *
     * @param viewProjection 视图投影矩阵
     */
    public void update(Matrix4f viewProjection) {
        // 提取平面（行向量形式）
        // Left: row4 + row1
        // Right: row4 - row1
        // Bottom: row4 + row2
        // Top: row4 - row2
        // Near: row4 + row3
        // Far: row4 - row3

        // 矩阵元素访问（JOML 使用列主序）
        // m00 m10 m20 m30 = column 0
        // m01 m11 m21 m31 = column 1
        // m02 m12 m22 m32 = column 2
        // m03 m13 m23 m33 = column 3

        // Row 1: m00, m10, m20, m30
        // Row 2: m01, m11, m21, m31
        // Row 3: m02, m12, m22, m32
        // Row 4: m03, m13, m23, m33

        float m00 = viewProjection.m00();
        float m01 = viewProjection.m01();
        float m02 = viewProjection.m02();
        float m03 = viewProjection.m03();
        float m10 = viewProjection.m10();
        float m11 = viewProjection.m11();
        float m12 = viewProjection.m12();
        float m13 = viewProjection.m13();
        float m20 = viewProjection.m20();
        float m21 = viewProjection.m21();
        float m22 = viewProjection.m22();
        float m23 = viewProjection.m23();
        float m30 = viewProjection.m30();
        float m31 = viewProjection.m31();
        float m32 = viewProjection.m32();
        float m33 = viewProjection.m33();

        // Left: row4 + row1
        planes[LEFT].set(m03 + m00, m13 + m10, m23 + m20, m33 + m30);
        normalizePlane(planes[LEFT]);

        // Right: row4 - row1
        planes[RIGHT].set(m03 - m00, m13 - m10, m23 - m20, m33 - m30);
        normalizePlane(planes[RIGHT]);

        // Bottom: row4 + row2
        planes[BOTTOM].set(m03 + m01, m13 + m11, m23 + m21, m33 + m31);
        normalizePlane(planes[BOTTOM]);

        // Top: row4 - row2
        planes[TOP].set(m03 - m01, m13 - m11, m23 - m21, m33 - m31);
        normalizePlane(planes[TOP]);

        // Near: row4 + row3
        planes[NEAR].set(m03 + m02, m13 + m12, m23 + m22, m33 + m32);
        normalizePlane(planes[NEAR]);

        // Far: row4 - row3
        planes[FAR].set(m03 - m02, m13 - m12, m23 - m22, m33 - m32);
        normalizePlane(planes[FAR]);
    }

    /**
     * 归一化平面
     */
    private void normalizePlane(Vector4f plane) {
        float length = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z * plane.z);
        if (length > 1e-6f) {
            plane.x /= length;
            plane.y /= length;
            plane.z /= length;
            plane.w /= length;
        }
    }

    /**
     * 检查点是否在视锥内
     *
     * @param point 要检查的点
     * @return true 如果点在视锥内
     */
    public boolean contains(Vector3f point) {
        for (int i = 0; i < 6; i++) {
            Vector4f plane = planes[i];
            float distance = plane.x * point.x + plane.y * point.y + plane.z * point.z + plane.w;
            if (distance < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查 AABB 是否与视锥相交
     *
     * <p>
     * 使用保守测试：如果 AABB 的所有角点都在某个平面的负半空间，则完全在视锥外。
     * </p>
     *
     * @param aabb 要检查的包围盒
     * @return true 如果 AABB 与视锥相交（可能在视野内）
     */
    public boolean intersects(AABB aabb) {
        if (!aabb.isValid()) {
            return false;
        }

        Vector3f min = aabb.getMin();
        Vector3f max = aabb.getMax();

        for (int i = 0; i < 6; i++) {
            Vector4f plane = planes[i];

            // 找到 AABB 在平面法线方向上最远的点 (P-vertex)
            float px = plane.x > 0 ? max.x : min.x;
            float py = plane.y > 0 ? max.y : min.y;
            float pz = plane.z > 0 ? max.z : min.z;

            // 如果 P-vertex 在平面负半空间，则 AABB 完全在视锥外
            float distance = plane.x * px + plane.y * py + plane.z * pz + plane.w;
            if (distance < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查球体是否与视锥相交
     *
     * @param center 球心
     * @param radius 半径
     * @return true 如果球体与视锥相交
     */
    public boolean intersectsSphere(Vector3f center, float radius) {
        for (int i = 0; i < 6; i++) {
            Vector4f plane = planes[i];
            float distance = plane.x * center.x + plane.y * center.y + plane.z * center.z + plane.w;
            if (distance < -radius) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取指定索引的平面
     *
     * @param index 平面索引 (LEFT, RIGHT, BOTTOM, TOP, NEAR, FAR)
     * @return 平面向量 (a, b, c, d)
     */
    public Vector4f getPlane(int index) {
        return new Vector4f(planes[index]);
    }
}
