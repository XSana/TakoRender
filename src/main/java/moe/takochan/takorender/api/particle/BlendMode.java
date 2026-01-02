package moe.takochan.takorender.api.particle;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子混合模式
 *
 * <p>
 * 定义粒子与背景的混合方式。
 * </p>
 */
@SideOnly(Side.CLIENT)
public enum BlendMode {

    /**
     * Alpha 混合（默认）
     *
     * <p>
     * 标准透明混合，适用于大多数效果。
     * </p>
     */
    ALPHA(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA),

    /**
     * 加法混合
     *
     * <p>
     * 颜色叠加，适用于发光效果（火焰、闪光、魔法）。
     * </p>
     */
    ADDITIVE(GL11.GL_SRC_ALPHA, GL11.GL_ONE),

    /**
     * 柔和加法
     *
     * <p>
     * 比加法更柔和的发光效果。
     * </p>
     */
    SOFT_ADDITIVE(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR),

    /**
     * 乘法混合
     *
     * <p>
     * 颜色相乘，适用于阴影、染色效果。
     * </p>
     */
    MULTIPLY(GL11.GL_DST_COLOR, GL11.GL_ZERO),

    /**
     * 预乘 Alpha
     *
     * <p>
     * 纹理已预乘 Alpha，适用于抗锯齿边缘。
     * </p>
     */
    PREMULTIPLIED(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA),

    /**
     * 不透明
     *
     * <p>
     * 完全覆盖背景，适用于不透明粒子。
     * </p>
     */
    OPAQUE(GL11.GL_ONE, GL11.GL_ZERO);

    /** 源混合因子 */
    private final int srcFactor;

    /** 目标混合因子 */
    private final int dstFactor;

    BlendMode(int srcFactor, int dstFactor) {
        this.srcFactor = srcFactor;
        this.dstFactor = dstFactor;
    }

    /**
     * 获取源混合因子
     *
     * @return GL 源因子
     */
    public int getSrcFactor() {
        return srcFactor;
    }

    /**
     * 获取目标混合因子
     *
     * @return GL 目标因子
     */
    public int getDstFactor() {
        return dstFactor;
    }

    /**
     * 应用混合模式
     */
    public void apply() {
        GL11.glBlendFunc(srcFactor, dstFactor);
    }
}
