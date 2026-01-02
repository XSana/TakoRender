package moe.takochan.takorender.api.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子模拟空间
 *
 * <p>
 * 定义粒子的位置是相对于世界还是相对于发射器。
 * </p>
 *
 * <p>
 * <b>WORLD</b>: 粒子在世界空间中运动，发射器移动不影响已发射粒子。
 * 适用于：烟雾、爆炸、火焰等效果。
 * </p>
 *
 * <p>
 * <b>LOCAL</b>: 粒子跟随发射器移动，相对位置保持不变。
 * 适用于：光环、护盾、武器拖尾等效果。
 * </p>
 */
@SideOnly(Side.CLIENT)
public enum SimulationSpace {

    /**
     * 世界空间
     *
     * <p>
     * 粒子位置使用世界坐标，发射器移动后已发射粒子不受影响。
     * </p>
     */
    WORLD,

    /**
     * 本地空间
     *
     * <p>
     * 粒子位置相对于发射器，发射器移动时粒子跟随移动。
     * </p>
     */
    LOCAL;

    /**
     * 是否为世界空间
     *
     * @return true 世界空间
     */
    public boolean isWorld() {
        return this == WORLD;
    }

    /**
     * 是否为本地空间
     *
     * @return true 本地空间
     */
    public boolean isLocal() {
        return this == LOCAL;
    }
}
