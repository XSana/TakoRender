package moe.takochan.takorender.api.ecs;

import java.util.Optional;

import moe.takochan.takorender.api.component.CameraComponent;

/**
 * 场景管理器 - 管理维度和相机
 *
 * <p>
 * SceneManager 是 World 的一部分，负责：
 * </p>
 * <ul>
 * <li>维度管理：设置/获取当前活动维度</li>
 * <li>相机管理：获取当前活动相机</li>
 * </ul>
 *
 * <p>
 * <b>维度筛选</b>:
 * 渲染 System 通过 SceneManager.getActiveDimensionId() 筛选 Entity。
 * 只有 DimensionComponent.dimensionId 匹配的 Entity 才会被渲染。
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
 *     SceneManager scene = world.getSceneManager();
 *
 *     // 维度切换时
 *     scene.setActiveDimensionId(dimensionId);
 *
 *     // 渲染时获取当前维度
 *     int dim = scene.getActiveDimensionId();
 *
 *     // 获取活动相机
 *     scene.getActiveCamera()
 *         .ifPresent(camera -> {
 *             // 使用相机
 *         });
 * }
 * </pre>
 */
public class SceneManager {

    private final World world;
    private int activeDimensionId = 0;

    /**
     * 创建场景管理器
     *
     * @param world 所属的 World
     */
    SceneManager(World world) {
        this.world = world;
    }

    /**
     * 获取当前活动维度 ID
     *
     * @return 维度 ID（0=主世界，-1=下界，1=末地）
     */
    public int getActiveDimensionId() {
        return activeDimensionId;
    }

    /**
     * 设置当前活动维度 ID
     *
     * @param dimensionId 维度 ID
     */
    public void setActiveDimensionId(int dimensionId) {
        this.activeDimensionId = dimensionId;
    }

    /**
     * 获取活动相机实体
     *
     * @return 活动相机实体，如果没有则返回 empty
     */
    public Optional<Entity> getActiveCamera() {
        for (Entity entity : world.getEntitiesWith(CameraComponent.class)) {
            CameraComponent camera = entity.getComponent(CameraComponent.class)
                .orElse(null);
            if (camera != null && camera.isActive()) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    /**
     * 获取活动相机组件
     *
     * @return 活动相机组件，如果没有则返回 empty
     */
    public Optional<CameraComponent> getActiveCameraComponent() {
        return getActiveCamera().flatMap(e -> e.getComponent(CameraComponent.class));
    }
}
