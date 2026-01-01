package moe.takochan.takorender.api.graphics;

/**
 * 材质接口 - 定义渲染时的外观属性
 *
 * <p>
 * Material 包含着色器、纹理和 uniform 参数，
 * 决定网格的最终渲染效果。
 * </p>
 *
 * <p>
 * <b>渲染模式</b>:
 * </p>
 * <ul>
 * <li>COLOR - 纯色渲染</li>
 * <li>TEXTURE - 纹理渲染</li>
 * <li>PBR - 基于物理的渲染</li>
 * </ul>
 */
public interface Material {

    /**
     * 渲染模式枚举
     */
    enum RenderMode {
        /** 纯色渲染 */
        COLOR,
        /** 纹理渲染 */
        TEXTURE,
        /** 基于物理的渲染 */
        PBR
    }

    /**
     * 获取渲染模式
     *
     * @return 渲染模式
     */
    RenderMode getRenderMode();

    /**
     * 检查是否为透明材质
     *
     * @return true 表示透明
     */
    boolean isTransparent();

    /**
     * 应用此材质（绑定着色器、设置 uniform、绑定纹理）
     */
    void apply();

    /**
     * 创建此材质的独立副本
     *
     * @return 新的材质实例
     */
    Material instantiate();
}
