package moe.takochan.takorender.api.ecs;

/**
 * System 执行阶段枚举
 *
 * <p>
 * 定义 System 在帧循环中的执行时机，确保逻辑更新和渲染的正确顺序。
 * </p>
 *
 * <p>
 * <b>执行顺序</b>: UPDATE → RENDER
 * </p>
 *
 * <ul>
 * <li>{@link #UPDATE}: 逻辑更新阶段（物理、AI、状态更新）</li>
 * <li>{@link #RENDER}: 渲染阶段（绘制、后处理）</li>
 * </ul>
 */
public enum Phase {

    /**
     * 逻辑更新阶段
     *
     * <p>
     * 在此阶段执行：
     * </p>
     * <ul>
     * <li>物理模拟</li>
     * <li>AI 决策</li>
     * <li>状态更新</li>
     * <li>动画更新</li>
     * <li>粒子系统更新</li>
     * </ul>
     */
    UPDATE,

    /**
     * 渲染阶段
     *
     * <p>
     * 在此阶段执行：
     * </p>
     * <ul>
     * <li>场景渲染</li>
     * <li>UI 渲染</li>
     * <li>后处理效果</li>
     * <li>调试渲染</li>
     * </ul>
     */
    RENDER
}
