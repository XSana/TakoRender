package moe.takochan.takorender.api.ecs;

/**
 * 静态标记枚举
 *
 * <p>
 * 用于标记 Entity 的静态特性，供渲染优化系统使用。
 * </p>
 *
 * <p>
 * <b>使用场景</b>:
 * </p>
 * <ul>
 * <li>Phase 3.1: FrustumCullingSystem 使用 OCCLUDER/OCCLUDEE</li>
 * <li>Phase 3.3: InstancedRenderSystem 使用 BATCHING</li>
 * </ul>
 */
public enum StaticFlags {

    /**
     * 可参与静态合批
     * <p>
     * 标记此 Entity 的几何体在场景中不会移动，
     * 可与其他相同 Mesh+Material 的 Entity 合并为单次 Draw Call。
     * </p>
     */
    BATCHING,

    /**
     * 遮挡体
     * <p>
     * 此 Entity 会遮挡其他物体，用于遮挡剔除优化。
     * 通常是大型不透明物体（墙壁、地形等）。
     * </p>
     */
    OCCLUDER,

    /**
     * 被遮挡体
     * <p>
     * 此 Entity 可被其他 OCCLUDER 遮挡，
     * 当完全被遮挡时可跳过渲染。
     * </p>
     */
    OCCLUDEE
}
