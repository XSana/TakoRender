package moe.takochan.takorender.api.graphics;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.api.resource.ResourceHandle;
import moe.takochan.takorender.api.resource.TextureManager;

/**
 * 标准材质实现
 *
 * <p>
 * StandardMaterial 是 Material 接口的默认实现，
 * 支持自动 uniform 绑定和属性管理。
 * </p>
 *
 * <p>
 * <b>特性</b>:
 * </p>
 * <ul>
 * <li>属性 Map 存储任意 uniform 值</li>
 * <li>深拷贝支持（instantiate）</li>
 * <li>纹理槽管理</li>
 * <li>PBR 参数（metallic, roughness, emissive）</li>
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
 *     StandardMaterial mat = new StandardMaterial(shader).setColor(1, 0.5f, 0, 1)
 *         .setProperty("uMetallic", 0.8f)
 *         .setTexture(textureId);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class StandardMaterial implements Material {

    private ShaderProgram shader;
    private RenderMode renderMode = RenderMode.COLOR;
    private boolean transparent = false;

    /** 动态属性容器 */
    private final Map<String, Object> properties = new HashMap<>();

    /** 主纹理资源键 */
    private String textureKey;

    /** 法线贴图资源键 */
    private String normalMapKey;

    /** 纹理句柄缓存 */
    private transient ResourceHandle<Integer> textureHandle;
    private transient ResourceHandle<Integer> normalMapHandle;

    /** 基础颜色 RGBA */
    private float colorR = 1.0f;
    private float colorG = 1.0f;
    private float colorB = 1.0f;
    private float colorA = 1.0f;

    /** PBR 参数 */
    private float metallic = 0.0f;
    private float roughness = 0.5f;
    private float emissive = 0.0f;

    /**
     * 创建无着色器的材质
     */
    public StandardMaterial() {}

    /**
     * 创建指定着色器的材质
     *
     * @param shader 着色器程序
     */
    public StandardMaterial(ShaderProgram shader) {
        this.shader = shader;
    }

    @Override
    public RenderMode getRenderMode() {
        return renderMode;
    }

    /**
     * 设置渲染模式
     *
     * @param mode 渲染模式
     * @return this（链式调用）
     */
    public StandardMaterial setRenderMode(RenderMode mode) {
        this.renderMode = mode != null ? mode : RenderMode.COLOR;
        return this;
    }

    @Override
    public boolean isTransparent() {
        return transparent || colorA < 1.0f;
    }

    /**
     * 设置是否透明
     *
     * @param transparent 是否透明
     * @return this（链式调用）
     */
    public StandardMaterial setTransparent(boolean transparent) {
        this.transparent = transparent;
        return this;
    }

    @Override
    public ShaderProgram getShader() {
        return shader;
    }

    /**
     * 设置着色器程序
     *
     * @param shader 着色器
     * @return this（链式调用）
     */
    public StandardMaterial setShader(ShaderProgram shader) {
        this.shader = shader;
        return this;
    }

    /**
     * 设置基础颜色
     *
     * @param r 红色 (0-1)
     * @param g 绿色 (0-1)
     * @param b 蓝色 (0-1)
     * @param a 透明度 (0-1)
     * @return this（链式调用）
     */
    public StandardMaterial setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
        return this;
    }

    /**
     * 设置不透明颜色
     */
    public StandardMaterial setColor(float r, float g, float b) {
        return setColor(r, g, b, 1.0f);
    }

    /**
     * 设置金属度
     *
     * @param metallic 金属度 (0-1)
     * @return this（链式调用）
     */
    public StandardMaterial setMetallic(float metallic) {
        this.metallic = Math.max(0, Math.min(1, metallic));
        return this;
    }

    /**
     * 设置粗糙度
     *
     * @param roughness 粗糙度 (0-1)
     * @return this（链式调用）
     */
    public StandardMaterial setRoughness(float roughness) {
        this.roughness = Math.max(0, Math.min(1, roughness));
        return this;
    }

    /**
     * 设置自发光强度
     *
     * @param emissive 自发光 (0-1)
     * @return this（链式调用）
     */
    public StandardMaterial setEmissive(float emissive) {
        this.emissive = Math.max(0, emissive);
        return this;
    }

    /**
     * 设置主纹理
     *
     * @param textureKey 纹理资源键（格式：domain:path，如 "takorender:particle/fire"）
     * @return this（链式调用）
     */
    public StandardMaterial setTexture(String textureKey) {
        // 释放旧句柄
        if (this.textureHandle != null) {
            this.textureHandle.release();
            this.textureHandle = null;
        }
        this.textureKey = textureKey;
        if (textureKey != null && !textureKey.isEmpty()) {
            this.renderMode = RenderMode.TEXTURE;
        }
        return this;
    }

    /**
     * 设置法线贴图
     *
     * @param normalMapKey 法线贴图资源键
     * @return this（链式调用）
     */
    public StandardMaterial setNormalMap(String normalMapKey) {
        // 释放旧句柄
        if (this.normalMapHandle != null) {
            this.normalMapHandle.release();
            this.normalMapHandle = null;
        }
        this.normalMapKey = normalMapKey;
        return this;
    }

    /**
     * 检查是否有纹理
     */
    public boolean hasTexture() {
        return textureKey != null && !textureKey.isEmpty();
    }

    /**
     * 检查是否有法线贴图
     */
    public boolean hasNormalMap() {
        return normalMapKey != null && !normalMapKey.isEmpty();
    }

    /**
     * 设置自定义属性
     *
     * <p>
     * 属性会在 apply() 时自动设置为 shader uniform。
     * </p>
     *
     * @param name  uniform 名称
     * @param value 值（支持 float, int, boolean, float[]）
     * @return this（链式调用）
     */
    public StandardMaterial setProperty(String name, Object value) {
        if (name != null && value != null) {
            properties.put(name, value);
        }
        return this;
    }

    /**
     * 获取属性值
     *
     * @param name 属性名
     * @return 属性值，如果不存在返回 null
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * 获取 float 属性
     *
     * @param name         属性名
     * @param defaultValue 默认值
     * @return 属性值
     */
    public float getFloat(String name, float defaultValue) {
        Object val = properties.get(name);
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        return defaultValue;
    }

    @Override
    public void apply() {
        if (shader == null) {
            return;
        }

        shader.use();

        // 绑定主纹理（通过 TextureManager 延迟加载）
        int textureId = getTextureId();
        if (textureId > 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            shader.setUniformInt("uTexture", 0);
            shader.setUniformBool("uHasTexture", true);
        } else {
            shader.setUniformBool("uHasTexture", false);
        }

        // 绑定法线贴图
        int normalMapId = getNormalMapId();
        if (normalMapId > 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalMapId);
            shader.setUniformInt("uNormalMap", 1);
            shader.setUniformBool("uHasNormalMap", true);
        } else {
            shader.setUniformBool("uHasNormalMap", false);
        }

        // 应用内置属性
        applyBuiltinProperties();

        // 应用动态属性
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            applyProperty(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 应用内置属性到 shader uniform
     */
    private void applyBuiltinProperties() {
        shader.setUniformVec4("uColor", colorR, colorG, colorB, colorA);
        shader.setUniformFloat("uMetallic", metallic);
        shader.setUniformFloat("uRoughness", roughness);
        shader.setUniformFloat("uEmissive", emissive);
    }

    /**
     * 应用单个属性到 shader uniform
     */
    private void applyProperty(String name, Object value) {
        if (value instanceof Float) {
            shader.setUniformFloat(name, (Float) value);
        } else if (value instanceof Integer) {
            shader.setUniformInt(name, (Integer) value);
        } else if (value instanceof Boolean) {
            shader.setUniformBool(name, (Boolean) value);
        } else if (value instanceof float[]) {
            float[] arr = (float[]) value;
            switch (arr.length) {
                case 2:
                    shader.setUniformVec2(name, arr[0], arr[1]);
                    break;
                case 3:
                    shader.setUniformVec3(name, arr[0], arr[1], arr[2]);
                    break;
                case 4:
                    shader.setUniformVec4(name, arr[0], arr[1], arr[2], arr[3]);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public Material instantiate() {
        StandardMaterial copy = new StandardMaterial(shader);
        copy.renderMode = this.renderMode;
        copy.transparent = this.transparent;
        copy.textureKey = this.textureKey;
        copy.normalMapKey = this.normalMapKey;
        copy.colorR = this.colorR;
        copy.colorG = this.colorG;
        copy.colorB = this.colorB;
        copy.colorA = this.colorA;
        copy.metallic = this.metallic;
        copy.roughness = this.roughness;
        copy.emissive = this.emissive;

        // 深拷贝属性
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof float[]) {
                copy.properties.put(entry.getKey(), ((float[]) val).clone());
            } else {
                copy.properties.put(entry.getKey(), val);
            }
        }

        return copy;
    }

    /**
     * 释放纹理资源
     */
    public void dispose() {
        if (textureHandle != null) {
            textureHandle.release();
            textureHandle = null;
        }
        if (normalMapHandle != null) {
            normalMapHandle.release();
            normalMapHandle = null;
        }
    }

    // ==================== Getter ====================

    public float getColorR() {
        return colorR;
    }

    public float getColorG() {
        return colorG;
    }

    public float getColorB() {
        return colorB;
    }

    public float getColorA() {
        return colorA;
    }

    public float getMetallic() {
        return metallic;
    }

    public float getRoughness() {
        return roughness;
    }

    public float getEmissive() {
        return emissive;
    }

    public String getTextureKey() {
        return textureKey;
    }

    public String getNormalMapKey() {
        return normalMapKey;
    }

    /**
     * 获取纹理 ID（延迟加载）
     */
    public int getTextureId() {
        if (textureKey == null || textureKey.isEmpty()) {
            return 0;
        }
        if (textureHandle == null || !textureHandle.isValid()) {
            textureHandle = TextureManager.instance()
                .get(textureKey);
        }
        return textureHandle != null && textureHandle.isValid() ? textureHandle.get() : 0;
    }

    /**
     * 获取法线贴图 ID（延迟加载）
     */
    public int getNormalMapId() {
        if (normalMapKey == null || normalMapKey.isEmpty()) {
            return 0;
        }
        if (normalMapHandle == null || !normalMapHandle.isValid()) {
            normalMapHandle = TextureManager.instance()
                .get(normalMapKey);
        }
        return normalMapHandle != null && normalMapHandle.isValid() ? normalMapHandle.get() : 0;
    }

    @Override
    public String toString() {
        return String.format(
            "StandardMaterial[mode=%s, color=(%.2f,%.2f,%.2f,%.2f), metallic=%.2f, roughness=%.2f]",
            renderMode,
            colorR,
            colorG,
            colorB,
            colorA,
            metallic,
            roughness);
    }
}
