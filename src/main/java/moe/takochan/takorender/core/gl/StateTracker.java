package moe.takochan.takorender.core.gl;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * GL 状态追踪器接口
 *
 * <p>
 * 封装单个 GL 状态的保存和恢复逻辑。
 * 每个状态创建自己的匿名 StateTracker 实例，捕获原始值并知道如何恢复它。
 * </p>
 *
 * <p>
 * <b>设计模式</b>: 策略模式 + 闭包
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
 *     // 创建状态追踪器（捕获当前值）
 *     StateTracker tracker = new StateTracker() {
 *
 *         private final boolean original = GL11.glIsEnabled(GL11.GL_BLEND);
 *
 *         &#64;Override
 *         public void restore() {
 *             if (original) GL11.glEnable(GL11.GL_BLEND);
 *             else GL11.glDisable(GL11.GL_BLEND);
 *         }
 *     };
 *
 *     // 修改状态
 *     GL11.glEnable(GL11.GL_BLEND);
 *
 *     // 恢复状态
 *     tracker.restore();
 * }
 * </pre>
 *
 * @see RenderEvent
 * @see RenderEventStateTrackers
 */
@SideOnly(Side.CLIENT)
interface StateTracker {

    /**
     * 恢复之前保存的 GL 状态
     *
     * <p>
     * 此方法将 GL 状态恢复到创建 StateTracker 时捕获的原始值。
     * 实现类应在匿名类的字段中保存原始状态，并在此方法中恢复。
     * </p>
     */
    void restore();
}
