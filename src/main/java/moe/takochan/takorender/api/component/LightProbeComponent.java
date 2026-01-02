package moe.takochan.takorender.api.component;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 光照探针组件 - 存储实体位置的 MC 光照数据
 *
 * <p>
 * LightProbeComponent 由 {@link moe.takochan.takorender.api.system.LightProbeSystem}
 * 在 UPDATE 阶段更新，渲染系统在 RENDER 阶段读取。
 * </p>
 *
 * <p>
 * <b>光照来源</b>:
 * </p>
 * <ul>
 * <li>blockLight - MC 方块光照（火把、熔岩等）</li>
 * <li>skyLight - MC 天空光照（太阳、月亮）</li>
 * </ul>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     Entity entity = world.createEntity();
 *     entity.addComponent(new TransformComponent(100, 64, 200));
 *     entity.addComponent(new LightProbeComponent());
 *     entity.addComponent(new MeshRendererComponent().setMesh(mesh));
 *     // LightProbeSystem 会自动查询并更新光照值
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent(TransformComponent.class)
public class LightProbeComponent extends Component {

    /** MC 方块光照 (0-1) */
    private float blockLight = 1.0f;

    /** MC 天空光照 (0-1) */
    private float skyLight = 1.0f;

    /** 是否接收光照 */
    private boolean receiveLighting = true;

    /** 最小亮度（防止全黑） */
    private float minBrightness = 0.1f;

    /** 组合后的最终光照值（由 System 计算） */
    private float combinedLight = 1.0f;

    public LightProbeComponent() {}

    // ==================== Getter/Setter ====================

    /**
     * 获取方块光照值
     *
     * @return 方块光照 (0-1)
     */
    public float getBlockLight() {
        return blockLight;
    }

    /**
     * 设置方块光照值（由 LightProbeSystem 调用）
     */
    public void setBlockLight(float blockLight) {
        this.blockLight = Math.max(0, Math.min(1, blockLight));
    }

    /**
     * 获取天空光照值
     *
     * @return 天空光照 (0-1)
     */
    public float getSkyLight() {
        return skyLight;
    }

    /**
     * 设置天空光照值（由 LightProbeSystem 调用）
     */
    public void setSkyLight(float skyLight) {
        this.skyLight = Math.max(0, Math.min(1, skyLight));
    }

    /**
     * 检查是否接收光照
     */
    public boolean isReceiveLighting() {
        return receiveLighting;
    }

    /**
     * 设置是否接收光照
     *
     * @param receiveLighting 是否接收
     * @return this（链式调用）
     */
    public LightProbeComponent setReceiveLighting(boolean receiveLighting) {
        this.receiveLighting = receiveLighting;
        return this;
    }

    /**
     * 获取最小亮度
     */
    public float getMinBrightness() {
        return minBrightness;
    }

    /**
     * 设置最小亮度
     *
     * @param minBrightness 最小亮度 (0-1)
     * @return this（链式调用）
     */
    public LightProbeComponent setMinBrightness(float minBrightness) {
        this.minBrightness = Math.max(0, Math.min(1, minBrightness));
        return this;
    }

    /**
     * 获取组合后的光照值
     *
     * <p>
     * 计算公式: max(max(blockLight, skyLight), minBrightness)
     * </p>
     *
     * @return 最终光照值 (0-1)
     */
    public float getCombinedLight() {
        return combinedLight;
    }

    /**
     * 设置组合光照值（由 LightProbeSystem 调用）
     */
    public void setCombinedLight(float combinedLight) {
        this.combinedLight = combinedLight;
    }

    /**
     * 更新光照值（由 LightProbeSystem 调用）
     *
     * @param blockLight 方块光照 (0-15 将被归一化)
     * @param skyLight   天空光照 (0-15 将被归一化)
     */
    public void updateLighting(int blockLight, int skyLight) {
        this.blockLight = blockLight / 15.0f;
        this.skyLight = skyLight / 15.0f;

        if (receiveLighting) {
            this.combinedLight = Math.max(Math.max(this.blockLight, this.skyLight), minBrightness);
        } else {
            this.combinedLight = 1.0f;
        }
    }

    @Override
    public String toString() {
        return String.format(
            "LightProbeComponent[block=%.2f, sky=%.2f, combined=%.2f, receive=%s]",
            blockLight,
            skyLight,
            combinedLight,
            receiveLighting);
    }
}
