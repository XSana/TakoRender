package moe.takochan.takorender.api.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.graphics.Mesh;

/**
 * LOD（Level of Detail）组件
 *
 * <p>
 * LODComponent 存储多个细节层次的 Mesh，根据到相机的距离自动切换。
 * 用于远距离物体使用低精度模型以提升性能。
 * </p>
 *
 * <p>
 * <b>工作流程</b>:
 * </p>
 * <ol>
 * <li>用户配置多个 LODLevel（按距离升序）</li>
 * <li>LODSystem 计算到相机的距离</li>
 * <li>LODSystem 根据距离和滞后值更新 activeLevel</li>
 * <li>MeshRenderSystem 使用 getActiveMesh() 获取当前 Mesh</li>
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
 *     Entity tree = world.createEntity();
 *     tree.addComponent(new TransformComponent(100, 64, 200));
 *     tree.addComponent(
 *         new LODComponent().addLevel(20.0f, highDetailMesh) // 0-20m: 高精度
 *             .addLevel(50.0f, mediumDetailMesh) // 20-50m: 中精度
 *             .addLevel(100.0f, lowDetailMesh) // 50-100m: 低精度
 *             .setHysteresis(0.1f)); // 10% 滞后
 *     tree.addComponent(new MeshRendererComponent().setMaterial(material));
 * }
 * </pre>
 */
@RequiresComponent(TransformComponent.class)
public class LODComponent extends Component {

    /**
     * LOD 级别定义
     */
    public static class LODLevel {

        private final float maxDistance;
        private final Mesh mesh;

        /**
         * 创建 LOD 级别
         *
         * @param maxDistance 该级别的最大距离
         * @param mesh        该级别使用的 Mesh
         */
        public LODLevel(float maxDistance, Mesh mesh) {
            this.maxDistance = maxDistance;
            this.mesh = mesh;
        }

        /**
         * 获取最大距离
         */
        public float getMaxDistance() {
            return maxDistance;
        }

        /**
         * 获取 Mesh
         */
        public Mesh getMesh() {
            return mesh;
        }
    }

    /** LOD 级别列表（按 maxDistance 升序排列） */
    private final List<LODLevel> levels = new ArrayList<>();

    /** 当前激活的 LOD 级别（由 LODSystem 写入） */
    private int activeLevel = 0;

    /** 滞后值（避免边界频繁切换） */
    private float hysteresis = 0.1f;

    /** 当前到相机的距离（由 LODSystem 写入） */
    private float currentDistance = 0;

    /**
     * 创建默认 LOD 组件
     */
    public LODComponent() {}

    /**
     * 添加 LOD 级别
     *
     * <p>
     * 级别会自动按 maxDistance 升序排列。
     * </p>
     *
     * @param maxDistance 该级别的最大可见距离
     * @param mesh        该级别使用的 Mesh
     * @return this（链式调用）
     */
    public LODComponent addLevel(float maxDistance, Mesh mesh) {
        levels.add(new LODLevel(maxDistance, mesh));
        Collections.sort(levels, Comparator.comparingDouble(LODLevel::getMaxDistance));
        return this;
    }

    /**
     * 获取所有 LOD 级别
     */
    public List<LODLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    /**
     * 获取级别数量
     */
    public int getLevelCount() {
        return levels.size();
    }

    /**
     * 获取当前激活的 LOD 级别索引
     */
    public int getActiveLevel() {
        return activeLevel;
    }

    /**
     * 设置当前激活的 LOD 级别（由 LODSystem 调用）
     *
     * @param activeLevel 级别索引
     */
    public void setActiveLevel(int activeLevel) {
        this.activeLevel = Math.max(0, Math.min(activeLevel, levels.size() - 1));
    }

    /**
     * 获取当前激活级别的 Mesh
     *
     * @return 当前 LOD 级别的 Mesh，如果没有配置则返回 null
     */
    public Mesh getActiveMesh() {
        if (levels.isEmpty()) {
            return null;
        }
        return levels.get(activeLevel)
            .getMesh();
    }

    /**
     * 获取滞后值
     *
     * <p>
     * 滞后值用于防止边界处频繁切换 LOD。
     * 例如 hysteresis=0.1 表示切换阈值有 10% 的缓冲区。
     * </p>
     */
    public float getHysteresis() {
        return hysteresis;
    }

    /**
     * 设置滞后值
     *
     * @param hysteresis 滞后值（0-1，推荐 0.1）
     * @return this（链式调用）
     */
    public LODComponent setHysteresis(float hysteresis) {
        this.hysteresis = Math.max(0, Math.min(1, hysteresis));
        return this;
    }

    /**
     * 获取当前到相机的距离
     */
    public float getCurrentDistance() {
        return currentDistance;
    }

    /**
     * 设置当前距离（由 LODSystem 调用）
     */
    public void setCurrentDistance(float distance) {
        this.currentDistance = distance;
    }

    /**
     * 根据距离计算应该使用的 LOD 级别
     *
     * <p>
     * 考虑滞后值防止频繁切换。
     * </p>
     *
     * @param distance     当前距离
     * @param currentLevel 当前级别
     * @return 新的级别索引
     */
    public int calculateLevel(float distance, int currentLevel) {
        if (levels.isEmpty()) {
            return 0;
        }

        // 找到基于纯距离的目标级别
        int targetLevel = 0;
        for (int i = 0; i < levels.size(); i++) {
            if (distance <= levels.get(i)
                .getMaxDistance()) {
                targetLevel = i;
                break;
            }
            targetLevel = i;
        }

        // 应用滞后值
        if (targetLevel != currentLevel && levels.size() > 1) {
            float currentMaxDist = levels.get(currentLevel)
                .getMaxDistance();
            float hysteresisRange = currentMaxDist * hysteresis;

            // 如果在滞后范围内，保持当前级别
            if (targetLevel > currentLevel) {
                // 要切换到更低精度，需要超过阈值 + 滞后
                if (distance < currentMaxDist + hysteresisRange) {
                    return currentLevel;
                }
            } else {
                // 要切换到更高精度，需要低于阈值 - 滞后
                float targetMaxDist = levels.get(targetLevel)
                    .getMaxDistance();
                if (distance > targetMaxDist - hysteresisRange) {
                    return currentLevel;
                }
            }
        }

        return targetLevel;
    }
}
