package moe.takochan.takorender.api.resource;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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
}
