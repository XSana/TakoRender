package moe.takochan.takorender.api.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.graphics.Material;
import moe.takochan.takorender.api.graphics.Model;
import moe.takochan.takorender.core.model.JsonModelLoader;
import moe.takochan.takorender.core.model.MtlLoader;
import moe.takochan.takorender.core.model.ObjLoader;

/**
 * 模型资源管理器
 *
 * <p>
 * ModelManager 负责加载和缓存 3D 模型。
 * 支持 OBJ 和 MC JSON 格式。
 * </p>
 *
 * <p>
 * <b>资源键格式</b>:
 * </p>
 * <ul>
 * <li>OBJ: "domain:path/to/model" (自动添加 .obj 扩展名)</li>
 * <li>JSON: "domain:path/to/model" (自动添加 .json 扩展名)</li>
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
 *     // 加载 OBJ 模型
 *     Model model = ModelManager.instance()
 *         .loadObj("takorender:models/cube");
 *
 *     // 加载 MC JSON 模型
 *     Model model = ModelManager.instance()
 *         .loadJson("takorender:models/block/custom");
 * }
 * </pre>
 */
public class ModelManager {

    private static ModelManager instance;

    /** 模型缓存 */
    private final Map<String, Model> cache = new HashMap<>();

    /** OBJ 加载器 */
    private final ObjLoader objLoader = new ObjLoader();

    /** MTL 加载器 */
    private final MtlLoader mtlLoader = new MtlLoader();

    /** JSON 模型加载器 */
    private final JsonModelLoader jsonLoader = new JsonModelLoader();

    private ModelManager() {}

    /**
     * 获取单例实例
     *
     * @return ModelManager 实例
     */
    public static synchronized ModelManager instance() {
        if (instance == null) {
            instance = new ModelManager();
        }
        return instance;
    }

    /**
     * 加载 OBJ 模型
     *
     * @param resourceKey 资源键（如 "takorender:models/cube"）
     * @return 加载的模型，如果失败返回 null
     */
    public Model loadObj(String resourceKey) {
        // 检查缓存
        Model cached = cache.get(resourceKey);
        if (cached != null) {
            return cached;
        }

        try {
            // 解析资源路径
            String[] parts = parseResourceKey(resourceKey);
            String domain = parts[0];
            String path = parts[1];

            // 构建完整路径
            String objPath = "assets/" + domain + "/" + path + ".obj";
            String mtlPath = "assets/" + domain + "/" + path + ".mtl";

            // 尝试加载 MTL 材质
            Map<String, Material> materials = new HashMap<>();
            try (InputStream mtlStream = getResourceStream(mtlPath)) {
                if (mtlStream != null) {
                    // 设置纹理前缀
                    String texturePrefix = domain + ":" + getParentPath(path);
                    mtlLoader.setTexturePrefix(texturePrefix);
                    materials = mtlLoader.load(mtlStream);
                }
            } catch (Exception e) {
                // MTL 可选，忽略错误
            }

            // 加载 OBJ
            try (InputStream objStream = getResourceStream(objPath)) {
                if (objStream == null) {
                    TakoRenderMod.LOG.warn("Model not found: {}", objPath);
                    return null;
                }

                objLoader.setMaterials(materials);
                Model model = objLoader.load(objStream, resourceKey);

                // 缓存
                cache.put(resourceKey, model);
                TakoRenderMod.LOG.debug("Loaded OBJ model: {} ({} sub-meshes)", resourceKey, model.getSubMeshCount());

                return model;
            }
        } catch (IOException e) {
            TakoRenderMod.LOG.error("Failed to load OBJ model: {}", resourceKey, e);
            return null;
        }
    }

    /**
     * 加载 MC JSON 模型
     *
     * @param resourceKey 资源键（如 "takorender:models/block/custom"）
     * @return 加载的模型，如果失败返回 null
     */
    public Model loadJson(String resourceKey) {
        // 检查缓存
        Model cached = cache.get(resourceKey);
        if (cached != null) {
            return cached;
        }

        try {
            // 解析资源路径
            String[] parts = parseResourceKey(resourceKey);
            String domain = parts[0];
            String path = parts[1];

            // 构建完整路径
            String jsonPath = "assets/" + domain + "/" + path + ".json";

            try (InputStream jsonStream = getResourceStream(jsonPath)) {
                if (jsonStream == null) {
                    TakoRenderMod.LOG.warn("Model not found: {}", jsonPath);
                    return null;
                }

                // 设置纹理前缀
                String texturePrefix = domain + ":" + getParentPath(path);
                jsonLoader.setTexturePrefix(texturePrefix);

                Model model = jsonLoader.load(jsonStream, resourceKey);

                // 缓存
                cache.put(resourceKey, model);
                TakoRenderMod.LOG.debug("Loaded JSON model: {} ({} sub-meshes)", resourceKey, model.getSubMeshCount());

                return model;
            }
        } catch (IOException e) {
            TakoRenderMod.LOG.error("Failed to load JSON model: {}", resourceKey, e);
            return null;
        }
    }

    /**
     * 从缓存获取模型
     *
     * @param resourceKey 资源键
     * @return 缓存的模型，如果不存在返回 null
     */
    public Model get(String resourceKey) {
        return cache.get(resourceKey);
    }

    /**
     * 检查模型是否已缓存
     *
     * @param resourceKey 资源键
     * @return true 如果已缓存
     */
    public boolean isCached(String resourceKey) {
        return cache.containsKey(resourceKey);
    }

    /**
     * 从缓存移除模型
     *
     * @param resourceKey 资源键
     */
    public void remove(String resourceKey) {
        Model model = cache.remove(resourceKey);
        if (model != null) {
            model.dispose();
        }
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        for (Model model : cache.values()) {
            model.dispose();
        }
        cache.clear();
    }

    /**
     * 获取缓存的模型数量
     *
     * @return 缓存数量
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 解析资源键为 [domain, path]
     */
    private String[] parseResourceKey(String key) {
        int colonIdx = key.indexOf(':');
        if (colonIdx > 0) {
            return new String[] { key.substring(0, colonIdx), key.substring(colonIdx + 1) };
        }
        return new String[] { "minecraft", key };
    }

    /**
     * 获取父路径（用于纹理相对路径解析）
     */
    private String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            return path.substring(0, lastSlash);
        }
        return "";
    }

    /**
     * 获取资源输入流
     */
    private InputStream getResourceStream(String path) {
        try {
            // 首先尝试从类路径加载
            InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(path);
            if (stream != null) {
                return stream;
            }

            // 然后尝试通过 MC 资源管理器加载
            String[] parts = path.replace("assets/", "")
                .split("/", 2);
            if (parts.length == 2) {
                ResourceLocation loc = new ResourceLocation(parts[0], parts[1]);
                return Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(loc)
                    .getInputStream();
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }
}
