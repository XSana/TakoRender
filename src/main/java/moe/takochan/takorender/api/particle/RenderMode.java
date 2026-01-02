package moe.takochan.takorender.api.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子渲染模式
 *
 * <p>
 * 定义粒子的渲染方式。
 * </p>
 */
@SideOnly(Side.CLIENT)
public enum RenderMode {

    /**
     * Billboard 四边形（默认）
     *
     * <p>
     * 粒子始终面向摄像机，最常用模式。
     * </p>
     */
    BILLBOARD_QUAD(0),

    /**
     * 水平 Billboard
     *
     * <p>
     * 粒子水平放置，仅绕 Y 轴旋转面向摄像机。
     * 适用于地面效果（涟漪、脚印）。
     * </p>
     */
    HORIZONTAL_BILLBOARD(1),

    /**
     * 垂直 Billboard
     *
     * <p>
     * 粒子垂直放置，仅绕垂直轴旋转。
     * 适用于草、树叶等效果。
     * </p>
     */
    VERTICAL_BILLBOARD(2),

    /**
     * 拉伸 Billboard
     *
     * <p>
     * 粒子沿速度方向拉伸。
     * 适用于雨滴、流星、激光等效果。
     * </p>
     */
    STRETCHED_BILLBOARD(3),

    /**
     * 3D 网格
     *
     * <p>
     * 使用 3D 模型渲染粒子。
     * 适用于复杂形状粒子（碎片、物体）。
     * </p>
     */
    MESH(4);

    /** 渲染模式 ID */
    private final int id;

    RenderMode(int id) {
        this.id = id;
    }

    /**
     * 获取渲染模式 ID
     *
     * @return 模式 ID
     */
    public int getId() {
        return id;
    }

    /**
     * 是否为 Billboard 模式
     *
     * @return true 如果是 Billboard
     */
    public boolean isBillboard() {
        return this != MESH;
    }

    /**
     * 是否为拉伸模式
     *
     * @return true 如果粒子沿速度拉伸
     */
    public boolean isStretched() {
        return this == STRETCHED_BILLBOARD;
    }
}
