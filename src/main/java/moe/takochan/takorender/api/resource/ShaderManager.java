package moe.takochan.takorender.api.resource;

import java.util.LinkedHashSet;
import java.util.Set;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.Reference;
import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;

/**
 * Shader 资源管理器
 *
 * <p>
 * ShaderManager 负责管理 ShaderProgram 资源的加载、缓存和卸载。
 * </p>
 *
 * <p>
 * <b>资源键格式</b>:
 * </p>
 * <ul>
 * <li>{@code "domain:path"} - 从 assets/{domain}/shaders/{path}.vert/.frag 加载</li>
 * <li>{@code "domain:path:geom"} - 带 geometry shader</li>
 * <li>{@code "domain:path:compute"} - compute shader</li>
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
 *     // 获取 shader（自动加载）
 *     ResourceHandle<ShaderProgram> shader = ShaderManager.instance()
 *         .get("takorender:sprite");
 *
 *     // 使用 shader
 *     shader.get()
 *         .use();
 *     shader.get()
 *         .setUniform("uColor", 1.0f, 0.0f, 0.0f, 1.0f);
 *
 *     // 使用完毕释放
 *     shader.release();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class ShaderManager extends ResourceManager<ShaderProgram> {

    private static final String SHADER_BASE_PATH = "shaders/";
    private static final String D = Reference.MODID + ":";

    /** Core batch shaders */
    public static final String SHADER_GUI_COLOR = D + "core/gui_color";
    public static final String SHADER_WORLD3D = D + "core/world3d";
    public static final String SHADER_WORLD3D_LIT = D + "core/world3d_lit";
    public static final String SHADER_LINE = D + "core/line";
    public static final String SHADER_MODEL = D + "core/model";
    public static final String SHADER_INSTANCED = D + "core/instanced";

    /** Post-process shaders */
    public static final String SHADER_BRIGHTNESS_EXTRACT = D + "postprocess/brightness_extract";
    public static final String SHADER_BLUR = D + "postprocess/blur";
    public static final String SHADER_COMPOSITE = D + "postprocess/composite";

    /** Particle shaders */
    public static final String SHADER_PARTICLE = D + "particle/particle";
    public static final String SHADER_PARTICLE_MESH = D + "particle/particle_mesh";
    public static final String SHADER_PARTICLE_UPDATE = D + "particle/particle_update:compute";
    public static final String SHADER_PARTICLE_EMIT = D + "particle/particle_emit:compute";

    /** 框架内置着色器 */
    private static final String[] BUILTIN_SHADERS = { SHADER_GUI_COLOR, SHADER_WORLD3D, SHADER_WORLD3D_LIT, SHADER_LINE,
        SHADER_MODEL, SHADER_INSTANCED, SHADER_BRIGHTNESS_EXTRACT, SHADER_BLUR, SHADER_COMPOSITE, SHADER_PARTICLE,
        SHADER_PARTICLE_MESH, };

    /** 框架内置 Compute Shader（需要 OpenGL 4.3+） */
    private static final String[] BUILTIN_COMPUTE_SHADERS = { SHADER_PARTICLE_UPDATE, SHADER_PARTICLE_EMIT, };

    /** 用户注册的着色器键 */
    private final Set<String> registered = new LinkedHashSet<>();

    private static ShaderManager INSTANCE;

    private ShaderManager() {
        super("ShaderManager");
    }

    /**
     * 获取单例实例
     */
    public static ShaderManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new ShaderManager();
        }
        return INSTANCE;
    }

    @Override
    protected ShaderProgram loadResource(String key) {
        String[] parts = key.split(":");
        if (parts.length < 2) {
            TakoRenderMod.LOG.error("Invalid shader key format: {} (expected domain:path)", key);
            return null;
        }

        String domain = parts[0];
        String path = parts[1];
        String shaderType = parts.length > 2 ? parts[2] : "standard";

        try {
            switch (shaderType) {
                case "compute":
                    return loadComputeShader(domain, path);
                case "geom":
                case "geometry":
                    return loadGeometryShader(domain, path);
                default:
                    return loadStandardShader(domain, path);
            }
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to load shader: {}", key, e);
            return null;
        }
    }

    private ShaderProgram loadStandardShader(String domain, String path) {
        String vertPath = SHADER_BASE_PATH + path + ".vert";
        String fragPath = SHADER_BASE_PATH + path + ".frag";

        ShaderProgram program = new ShaderProgram(domain, vertPath, null, fragPath);
        if (!program.isValid()) {
            TakoRenderMod.LOG.error("Failed to compile shader: {}:{}", domain, path);
            return null;
        }
        return program;
    }

    private ShaderProgram loadGeometryShader(String domain, String path) {
        String vertPath = SHADER_BASE_PATH + path + ".vert";
        String geomPath = SHADER_BASE_PATH + path + ".geom";
        String fragPath = SHADER_BASE_PATH + path + ".frag";

        ShaderProgram program = new ShaderProgram(domain, vertPath, geomPath, fragPath);
        if (!program.isValid()) {
            TakoRenderMod.LOG.error("Failed to compile geometry shader: {}:{}", domain, path);
            return null;
        }
        return program;
    }

    private ShaderProgram loadComputeShader(String domain, String path) {
        String computePath = SHADER_BASE_PATH + path + ".comp";

        ShaderProgram program = ShaderProgram.createCompute(domain, computePath);
        if (!program.isValid()) {
            TakoRenderMod.LOG.error("Failed to compile compute shader: {}:{}", domain, path);
            return null;
        }
        return program;
    }

    @Override
    protected void unloadResource(ShaderProgram resource) {
        if (resource != null) {
            resource.close();
        }
    }

    /**
     * 重置单例（用于测试或重载）
     */
    public static void reset() {
        if (INSTANCE != null) {
            INSTANCE.dispose();
            INSTANCE = null;
        }
    }

    /**
     * 注册用户着色器以便预加载
     *
     * <p>
     * 在 Mod 初始化时调用此方法注册着色器，然后调用 {@link #preloadAll()} 批量编译。
     * </p>
     *
     * @param key 着色器资源键（格式：domain:path 或 domain:path:compute）
     */
    public void register(String key) {
        if (key != null && !key.isEmpty()) {
            registered.add(key);
        }
    }

    /**
     * 预加载所有着色器（框架内置 + 用户注册）
     *
     * <p>
     * 在 Mod 初始化阶段（如 postInit）调用，批量编译着色器避免首帧卡顿。
     * </p>
     *
     * @param includeComputeShaders 是否包含 Compute Shader（需要 OpenGL 4.3+）
     */
    public void preloadAll(boolean includeComputeShaders) {
        int[] counts = { 0, 0 }; // [loaded, failed]

        preloadBuiltin(BUILTIN_SHADERS, counts);
        if (includeComputeShaders) {
            preloadBuiltin(BUILTIN_COMPUTE_SHADERS, counts);
        }
        preloadKeys(registered, counts);

        TakoRenderMod.LOG.info("ShaderManager: Preloaded {} shaders ({} failed)", counts[0], counts[1]);
    }

    private void preloadBuiltin(String[] keys, int[] counts) {
        for (String key : keys) {
            if (tryPreload(key)) {
                counts[0]++;
            } else {
                counts[1]++;
                TakoRenderMod.LOG.warn("Failed to preload shader: {}", key);
            }
        }
    }

    private void preloadKeys(Iterable<String> keys, int[] counts) {
        for (String key : keys) {
            if (tryPreload(key)) {
                counts[0]++;
            } else {
                counts[1]++;
                TakoRenderMod.LOG.warn("Failed to preload shader: {}", key);
            }
        }
    }

    private boolean tryPreload(String key) {
        ResourceHandle<ShaderProgram> handle = get(key);
        if (handle != null && handle.isValid()) {
            handle.release();
            return true;
        }
        return false;
    }

    /**
     * 预加载所有着色器（包含 Compute Shader）
     */
    public void preloadAll() {
        preloadAll(true);
    }
}
