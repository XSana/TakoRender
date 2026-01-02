package moe.takochan.takorender.api.component;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.particle.BlendMode;
import moe.takochan.takorender.api.particle.RenderMode;

/**
 * 粒子渲染组件 - 纯数据
 *
 * <p>
 * 存储粒子渲染参数（混合、纹理、光照）。逻辑由 ParticleRenderSystem 处理。
 * </p>
 */
@SideOnly(Side.CLIENT)
public class ParticleRenderComponent extends Component {

    /** 混合模式 */
    private BlendMode blendMode = BlendMode.ALPHA;

    /** 渲染模式 */
    private RenderMode renderMode = RenderMode.BILLBOARD_QUAD;

    /** 纹理 ID */
    private int textureId;

    /** 纹理动画 - 水平分块数 */
    private int textureTilesX = 1;

    /** 纹理动画 - 垂直分块数 */
    private int textureTilesY = 1;

    /** 纹理动画 - 速度倍率 */
    private float animationSpeed = 1.0f;

    /** 纹理动画模式：0=按生命周期，1=按速度 */
    private int animationMode = 0;

    /** 自发光强度 (0-1，1=完全自发光，不受光照影响) */
    private float emissive = 0.0f;

    /** 是否接收 MC 光照 */
    private boolean receiveLighting = true;

    /** 最小亮度 */
    private float minBrightness = 0.1f;

    /** 软粒子（与深度缓冲混合） */
    private boolean softParticles = false;

    /** 软粒子距离 */
    private float softDistance = 0.5f;

    /** 是否写入深度 */
    private boolean depthWrite = false;

    /** 是否启用深度测试 */
    private boolean depthTest = true;

    public ParticleRenderComponent() {}

    // ==================== 混合/渲染模式 ====================

    public BlendMode getBlendMode() {
        return blendMode;
    }

    public ParticleRenderComponent setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode;
        return this;
    }

    public RenderMode getRenderMode() {
        return renderMode;
    }

    public ParticleRenderComponent setRenderMode(RenderMode renderMode) {
        this.renderMode = renderMode;
        return this;
    }

    // ==================== 纹理 ====================

    public int getTextureId() {
        return textureId;
    }

    public ParticleRenderComponent setTextureId(int textureId) {
        this.textureId = textureId;
        return this;
    }

    public int getTextureTilesX() {
        return textureTilesX;
    }

    public int getTextureTilesY() {
        return textureTilesY;
    }

    public ParticleRenderComponent setTextureAnimation(int tilesX, int tilesY) {
        this.textureTilesX = tilesX;
        this.textureTilesY = tilesY;
        return this;
    }

    public ParticleRenderComponent setTextureAnimation(int tilesX, int tilesY, float speed) {
        this.textureTilesX = tilesX;
        this.textureTilesY = tilesY;
        this.animationSpeed = speed;
        return this;
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public ParticleRenderComponent setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
        return this;
    }

    public int getAnimationMode() {
        return animationMode;
    }

    public ParticleRenderComponent setAnimationMode(int animationMode) {
        this.animationMode = animationMode;
        return this;
    }

    // ==================== 光照 ====================

    public float getEmissive() {
        return emissive;
    }

    public ParticleRenderComponent setEmissive(float emissive) {
        this.emissive = Math.max(0, Math.min(1, emissive));
        return this;
    }

    public boolean isReceiveLighting() {
        return receiveLighting;
    }

    public ParticleRenderComponent setReceiveLighting(boolean receiveLighting) {
        this.receiveLighting = receiveLighting;
        return this;
    }

    public float getMinBrightness() {
        return minBrightness;
    }

    public ParticleRenderComponent setMinBrightness(float minBrightness) {
        this.minBrightness = minBrightness;
        return this;
    }

    // ==================== 软粒子/深度 ====================

    public boolean isSoftParticles() {
        return softParticles;
    }

    public ParticleRenderComponent setSoftParticles(boolean softParticles) {
        this.softParticles = softParticles;
        return this;
    }

    public float getSoftDistance() {
        return softDistance;
    }

    public ParticleRenderComponent setSoftDistance(float softDistance) {
        this.softDistance = softDistance;
        return this;
    }

    public boolean isDepthWrite() {
        return depthWrite;
    }

    public ParticleRenderComponent setDepthWrite(boolean depthWrite) {
        this.depthWrite = depthWrite;
        return this;
    }

    public boolean isDepthTest() {
        return depthTest;
    }

    public ParticleRenderComponent setDepthTest(boolean depthTest) {
        this.depthTest = depthTest;
        return this;
    }

    @Override
    public String toString() {
        return String.format(
            "ParticleRenderComponent[blend=%s, emissive=%.2f, lighting=%b]",
            blendMode,
            emissive,
            receiveLighting);
    }
}
