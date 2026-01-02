package moe.takochan.takorender.api.graphics.shader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.Reference;
import moe.takochan.takorender.TakoRenderMod;

/**
 * Shader 类型枚举
 *
 * <p>
 * 管理所有内置 Shader 的加载和缓存。
 * 使用 {@link #register()} 初始化所有 Shader，使用 {@link #get()} 获取实例。
 * </p>
 *
 * <p>
 * <b>Shader 目录结构</b>:
 * </p>
 *
 * <pre>
 * assets/takorender/shaders/
 * ├── core/           - 核心渲染 Shader
 * ├── postprocessing/ - 后处理 Shader
 * └── effects/        - 特效 Shader
 * </pre>
 */
@SideOnly(Side.CLIENT)
public enum ShaderType {

    /**
     * GUI 纯色渲染着色器（SpriteBatch 使用）
     */
    GUI_COLOR("shaders/core/gui_color.vert", "shaders/core/gui_color.frag"),

    /**
     * 3D 世界渲染着色器（World3DBatch 使用）
     */
    WORLD_3D("shaders/core/world3d.vert", "shaders/core/world3d.frag"),

    /**
     * 3D 世界渲染着色器（带 MC 光照支持，World3DBatchLit 使用）
     */
    WORLD_3D_LIT("shaders/core/world3d_lit.vert", "shaders/core/world3d_lit.frag"),

    /**
     * 线条渲染着色器（LineRenderSystem 使用）
     */
    LINE("shaders/core/line.vert", "shaders/core/line.frag");

    private final String vertFilepath;
    private final String fragFilepath;

    private static final Map<ShaderType, ShaderProgram> SHADER_CACHE = new ConcurrentHashMap<>();
    private static final String DOMAIN = Reference.MODID;

    ShaderType(String vertFilepath, String fragFilepath) {
        this.vertFilepath = vertFilepath;
        this.fragFilepath = fragFilepath;
    }

    /**
     * 初始化所有 ShaderType 枚举项对应的着色器程序
     *
     * <p>
     * 应在 mod 加载阶段或渲染前调用一次。
     * </p>
     */
    public static void register() {
        TakoRenderMod.LOG.info("Registering {} shader types...", values().length);
        for (ShaderType type : values()) {
            create(type);
        }
        TakoRenderMod.LOG.info("Shader registration complete. {} shaders loaded.", SHADER_CACHE.size());
    }

    /**
     * 获取顶点着色器路径
     */
    public String getVertShaderFilename() {
        return vertFilepath;
    }

    /**
     * 获取片元着色器路径
     */
    public String getFragShaderFilename() {
        return fragFilepath;
    }

    /**
     * 获取当前 ShaderType 对应的着色器程序
     *
     * @return 对应的 ShaderProgram 实例
     * @throws IllegalStateException 若该着色器尚未初始化
     */
    public ShaderProgram get() {
        if (!SHADER_CACHE.containsKey(this)) {
            throw new IllegalStateException("Shader " + name() + " not initialized. Call register() first.");
        }
        return SHADER_CACHE.get(this);
    }

    /**
     * 安全获取着色器程序，不抛出异常
     *
     * @return 对应的 ShaderProgram 实例，如果未初始化或加载失败则返回 null
     */
    public ShaderProgram getOrNull() {
        return SHADER_CACHE.get(this);
    }

    /**
     * 检查该着色器是否已成功加载
     */
    public boolean isLoaded() {
        ShaderProgram shader = SHADER_CACHE.get(this);
        return shader != null && shader.isValid();
    }

    /**
     * 清理并删除所有着色器程序，释放 GPU 资源
     *
     * <p>
     * 通常在游戏退出或资源重载时调用。
     * 注意：必须在具有 OpenGL 上下文的线程中调用。
     * </p>
     */
    public static void cleanupAll() {
        TakoRenderMod.LOG.info("Cleaning up {} shaders...", SHADER_CACHE.size());
        SHADER_CACHE.forEach((type, shader) -> {
            try {
                shader.close();
            } catch (Exception e) {
                // 忽略清理时的错误（可能没有 GL 上下文）
            }
        });
        SHADER_CACHE.clear();
    }

    /**
     * 重新加载所有着色器（用于热重载）
     */
    public static void reloadAll() {
        cleanupAll();
        register();
    }

    private static void create(ShaderType type) {
        if (!SHADER_CACHE.containsKey(type)) {
            ShaderProgram shader = new ShaderProgram(DOMAIN, type.vertFilepath, type.fragFilepath);
            if (shader.getProgram() != 0) {
                SHADER_CACHE.put(type, shader);
                TakoRenderMod.LOG.info("Shader '{}' loaded (ID = {})", type.name(), shader.getProgram());
            } else {
                TakoRenderMod.LOG.error(
                    "Failed to load shader '{}'. vert='{}', frag='{}'",
                    type.name(),
                    type.vertFilepath,
                    type.fragFilepath);
            }
        }
    }
}
