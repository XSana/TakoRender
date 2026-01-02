package moe.takochan.takorender.api.component;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 后处理组件 - 存储后处理效果参数
 *
 * <p>
 * PostProcessComponent 是纯数据组件，附加到相机实体上以启用后处理效果
 * （如 Bloom、HDR、色调映射等）。
 * </p>
 *
 * <p>
 * <b>ECS 原则</b>: Component 只存储数据，不包含逻辑。
 * 所有处理由 PostProcessSystem 执行。
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
 *     Entity camera = world.createEntity();
 *     camera.addComponent(new TransformComponent(0, 64, 0));
 *     camera.addComponent(
 *         new CameraComponent().setPerspective(70, 16f / 9f, 0.1f, 1000f)
 *             .setActive(true));
 *     camera.addComponent(
 *         new PostProcessComponent().setBloomEnabled(true)
 *             .setBloomThreshold(0.8f)
 *             .setBloomIntensity(1.5f)
 *             .setExposure(1.0f));
 * }
 * </pre>
 */
@RequiresComponent(CameraComponent.class)
public class PostProcessComponent extends Component {

    /** 是否启用 Bloom 效果 */
    private boolean bloomEnabled = false;

    /** Bloom 亮度提取阈值 (0.0 - 2.0) */
    private float bloomThreshold = 0.8f;

    /** 软膝盖系数，用于平滑阈值过渡 (0.0 - 1.0) */
    private float bloomSoftKnee = 0.5f;

    /** Bloom 强度倍数 (0.0 - 5.0) */
    private float bloomIntensity = 1.5f;

    /** 模糊迭代次数 (1 - 8) */
    private int blurIterations = 4;

    /** 模糊缩放因子 (0.5 - 2.0) */
    private float blurScale = 1.0f;

    /** 曝光值 (0.1 - 10.0) */
    private float exposure = 1.0f;

    /** 是否启用色调映射 */
    private boolean tonemapEnabled = false;

    /** Bloom Alpha 缩放（用于 Overlay 模式）(0.5 - 4.0) */
    private float bloomAlphaScale = 2.0f;

    /**
     * 创建默认后处理组件（所有效果禁用）
     */
    public PostProcessComponent() {}

    /**
     * 检查是否启用 Bloom
     */
    public boolean isBloomEnabled() {
        return bloomEnabled;
    }

    /**
     * 启用或禁用 Bloom 效果
     *
     * @param enabled true 启用 Bloom
     * @return this（链式调用）
     */
    public PostProcessComponent setBloomEnabled(boolean enabled) {
        this.bloomEnabled = enabled;
        return this;
    }

    /**
     * 获取 Bloom 亮度阈值
     */
    public float getBloomThreshold() {
        return bloomThreshold;
    }

    /**
     * 设置 Bloom 亮度阈值
     *
     * <p>
     * 亮度高于此阈值的像素将产生 Bloom 效果。
     * 典型值: 0.5 - 1.5
     * </p>
     *
     * @param threshold 亮度阈值 (0.0 - 2.0)
     * @return this（链式调用）
     */
    public PostProcessComponent setBloomThreshold(float threshold) {
        this.bloomThreshold = Math.max(0.0f, Math.min(2.0f, threshold));
        return this;
    }

    /**
     * 获取 Bloom 软膝盖系数
     */
    public float getBloomSoftKnee() {
        return bloomSoftKnee;
    }

    /**
     * 设置 Bloom 软膝盖系数
     *
     * <p>
     * 控制阈值过渡的平滑度。
     * 0.0 = 硬切，1.0 = 非常平滑。
     * </p>
     *
     * @param softKnee 软膝盖系数 (0.0 - 1.0)
     * @return this（链式调用）
     */
    public PostProcessComponent setBloomSoftKnee(float softKnee) {
        this.bloomSoftKnee = Math.max(0.0f, Math.min(1.0f, softKnee));
        return this;
    }

    /**
     * 获取 Bloom 强度
     */
    public float getBloomIntensity() {
        return bloomIntensity;
    }

    /**
     * 设置 Bloom 强度
     *
     * <p>
     * 控制发光效果的强度。
     * 典型值: 0.5 - 3.0
     * </p>
     *
     * @param intensity Bloom 强度 (0.0 - 5.0)
     * @return this（链式调用）
     */
    public PostProcessComponent setBloomIntensity(float intensity) {
        this.bloomIntensity = Math.max(0.0f, Math.min(5.0f, intensity));
        return this;
    }

    /**
     * 获取模糊迭代次数
     */
    public int getBlurIterations() {
        return blurIterations;
    }

    /**
     * 设置模糊迭代次数
     *
     * <p>
     * 更多迭代 = 更大更柔和的 Bloom，但 GPU 开销更高。
     * 典型值: 2 - 6
     * </p>
     *
     * @param iterations 模糊次数 (1 - 8)
     * @return this（链式调用）
     */
    public PostProcessComponent setBlurIterations(int iterations) {
        this.blurIterations = Math.max(1, Math.min(8, iterations));
        return this;
    }

    /**
     * 获取模糊缩放因子
     */
    public float getBlurScale() {
        return blurScale;
    }

    /**
     * 设置模糊缩放因子
     *
     * <p>
     * 模糊采样距离的倍数。
     * 较高值 = 更快扩散但可能产生伪影。
     * </p>
     *
     * @param scale 模糊缩放 (0.5 - 2.0)
     * @return this（链式调用）
     */
    public PostProcessComponent setBlurScale(float scale) {
        this.blurScale = Math.max(0.5f, Math.min(2.0f, scale));
        return this;
    }

    /**
     * 获取曝光值
     */
    public float getExposure() {
        return exposure;
    }

    /**
     * 设置曝光值
     *
     * <p>
     * 控制整体亮度。值 &gt; 1.0 变亮，&lt; 1.0 变暗。
     * </p>
     *
     * @param exposure 曝光值 (0.1 - 10.0)
     * @return this（链式调用）
     */
    public PostProcessComponent setExposure(float exposure) {
        this.exposure = Math.max(0.1f, Math.min(10.0f, exposure));
        return this;
    }

    /**
     * 检查是否启用色调映射
     */
    public boolean isTonemapEnabled() {
        return tonemapEnabled;
    }

    /**
     * 启用或禁用色调映射
     *
     * <p>
     * 色调映射将 HDR 颜色压缩到可显示范围。
     * 使用 ACES 电影色调映射曲线。
     * </p>
     *
     * @param enabled true 启用色调映射
     * @return this（链式调用）
     */
    public PostProcessComponent setTonemapEnabled(boolean enabled) {
        this.tonemapEnabled = enabled;
        return this;
    }

    /**
     * 获取 Bloom Alpha 缩放
     */
    public float getBloomAlphaScale() {
        return bloomAlphaScale;
    }

    /**
     * 设置 Bloom Alpha 缩放（用于 Overlay 模式）
     *
     * <p>
     * 控制 Bloom 对 Alpha 通道的贡献。
     * 较高值使 Bloom 在透明区域更明显。
     * </p>
     *
     * @param scale Alpha 缩放 (0.5 - 4.0)
     * @return this（链式调用）
     */
    public PostProcessComponent setBloomAlphaScale(float scale) {
        this.bloomAlphaScale = Math.max(0.5f, Math.min(4.0f, scale));
        return this;
    }

    /**
     * 启用 Bloom（便捷方法）
     *
     * @param threshold 亮度阈值
     * @param intensity Bloom 强度
     * @return this（链式调用）
     */
    public PostProcessComponent enableBloom(float threshold, float intensity) {
        this.bloomEnabled = true;
        this.bloomThreshold = threshold;
        this.bloomIntensity = intensity;
        return this;
    }

    /**
     * 启用 HDR（便捷方法）
     *
     * @param exposure      曝光值
     * @param enableTonemap 是否启用色调映射
     * @return this（链式调用）
     */
    public PostProcessComponent enableHDR(float exposure, boolean enableTonemap) {
        this.exposure = exposure;
        this.tonemapEnabled = enableTonemap;
        return this;
    }

    /**
     * 检查是否有任何效果启用
     */
    public boolean hasAnyEffectEnabled() {
        return bloomEnabled || tonemapEnabled;
    }
}
