package moe.takochan.takorender.api.ecs;

/**
 * 渲染层枚举 - 区分不同渲染上下文
 *
 * <p>
 * Layer 决定 Entity 在哪个渲染事件中被处理：
 * </p>
 * <ul>
 * <li>{@link #WORLD_3D}: 3D 世界空间，参与后处理</li>
 * <li>{@link #HUD}: 游戏内覆盖层（血条、状态等），不受后处理影响</li>
 * <li>{@link #GUI}: 玩家打开的界面（背包、菜单等），不受后处理影响</li>
 * </ul>
 *
 * <p>
 * <b>渲染时机</b>:
 * </p>
 * <ul>
 * <li>WORLD_3D: RenderWorldLastEvent（MC 渲染后、Angelica 后处理前）</li>
 * <li>HUD: RenderGameOverlayEvent.Post(ALL)</li>
 * <li>GUI: DrawScreenEvent.Post</li>
 * </ul>
 */
public enum Layer {
    /**
     * 3D 世界空间
     * <p>
     * 使用世界坐标，参与深度测试，被光影 Mod 后处理影响。
     * </p>
     */
    WORLD_3D,

    /**
     * 游戏内覆盖层
     * <p>
     * 使用屏幕坐标，不参与深度测试，不受后处理影响。
     * 用于血条、小地图、状态图标等。
     * </p>
     */
    HUD,

    /**
     * GUI 界面层
     * <p>
     * 使用屏幕坐标，不参与深度测试，不受后处理影响。
     * 用于打开的界面（背包、菜单、对话框等）。
     * </p>
     */
    GUI
}
