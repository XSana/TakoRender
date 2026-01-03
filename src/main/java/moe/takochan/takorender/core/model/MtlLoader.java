package moe.takochan.takorender.core.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import moe.takochan.takorender.api.graphics.Material;
import moe.takochan.takorender.api.graphics.StandardMaterial;

/**
 * MTL 材质库加载器
 *
 * <p>
 * 解析 Wavefront MTL 格式文件，生成 Material 对象映射。
 * 与 ObjLoader 配合使用。
 * </p>
 *
 * <p>
 * <b>支持的 MTL 指令</b>:
 * </p>
 * <ul>
 * <li>newmtl - 定义新材质</li>
 * <li>Kd - 漫反射颜色</li>
 * <li>Ka - 环境光颜色</li>
 * <li>Ks - 高光颜色</li>
 * <li>Ns - 高光指数</li>
 * <li>d/Tr - 透明度</li>
 * <li>map_Kd - 漫反射贴图</li>
 * <li>map_Bump/bump - 法线/凹凸贴图</li>
 * </ul>
 */
public class MtlLoader {

    /** 当前材质名称 */
    private String currentMaterialName = null;

    /** 当前材质构建器 */
    private StandardMaterial currentMaterial = null;

    /** 材质映射 */
    private final Map<String, Material> materials = new HashMap<>();

    /** 纹理路径前缀（用于构建完整纹理键） */
    private String texturePrefix = "";

    /**
     * 从输入流加载 MTL 材质库
     *
     * @param inputStream MTL 文件输入流
     * @return 材质名称到 Material 的映射
     * @throws IOException 如果读取失败
     */
    public Map<String, Material> load(InputStream inputStream) throws IOException {
        clear();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line.trim());
            }
        }

        // 保存最后一个材质
        flushCurrentMaterial();

        return materials;
    }

    /**
     * 设置纹理路径前缀
     *
     * <p>
     * 用于将 MTL 中的相对纹理路径转换为 TextureManager 可用的资源键。
     * 例如：prefix="takorender:models/cube" + texPath="texture.png"
     * = "takorender:models/cube/texture"
     * </p>
     *
     * @param prefix 纹理前缀
     */
    public void setTexturePrefix(String prefix) {
        this.texturePrefix = prefix != null ? prefix : "";
    }

    /**
     * 解析单行
     */
    private void parseLine(String line) {
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        String[] parts = line.split("\\s+", 2);
        if (parts.length == 0) {
            return;
        }

        String cmd = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "newmtl":
                parseNewMtl(args);
                break;
            case "Kd":
                parseKd(args);
                break;
            case "Ka":
                parseKa(args);
                break;
            case "Ks":
                parseKs(args);
                break;
            case "Ns":
                parseNs(args);
                break;
            case "d":
                parseD(args);
                break;
            case "Tr":
                parseTr(args);
                break;
            case "map_Kd":
                parseMapKd(args);
                break;
            case "map_Bump":
            case "bump":
                parseMapBump(args);
                break;
            default:
                break;
        }
    }

    /**
     * 解析 newmtl 指令
     */
    private void parseNewMtl(String name) {
        flushCurrentMaterial();
        currentMaterialName = name;
        currentMaterial = new StandardMaterial();
    }

    /**
     * 解析漫反射颜色 (Kd r g b)
     */
    private void parseKd(String args) {
        if (currentMaterial == null) return;
        float[] rgb = parseColor(args);
        if (rgb != null) {
            float alpha = currentMaterial.getColorA();
            currentMaterial.setColor(rgb[0], rgb[1], rgb[2], alpha);
        }
    }

    /**
     * 解析环境光颜色 (Ka r g b)
     * 暂不支持，忽略
     */
    private void parseKa(String args) {
        // StandardMaterial 暂不支持单独的环境光颜色
    }

    /**
     * 解析高光颜色 (Ks r g b)
     * 转换为金属度
     */
    private void parseKs(String args) {
        if (currentMaterial == null) return;
        float[] rgb = parseColor(args);
        if (rgb != null) {
            // 使用高光颜色亮度估算金属度
            float brightness = (rgb[0] + rgb[1] + rgb[2]) / 3f;
            currentMaterial.setMetallic(brightness);
        }
    }

    /**
     * 解析高光指数 (Ns value)
     * 转换为粗糙度
     */
    private void parseNs(String args) {
        if (currentMaterial == null) return;
        try {
            float ns = Float.parseFloat(args.trim());
            // Ns 范围通常 0-1000，转换为粗糙度 1-0
            float roughness = 1.0f - Math.min(1.0f, ns / 1000f);
            currentMaterial.setRoughness(roughness);
        } catch (NumberFormatException e) {
            // 忽略
        }
    }

    /**
     * 解析透明度 (d value)
     */
    private void parseD(String args) {
        if (currentMaterial == null) return;
        try {
            float d = Float.parseFloat(args.trim());
            currentMaterial
                .setColor(currentMaterial.getColorR(), currentMaterial.getColorG(), currentMaterial.getColorB(), d);
            if (d < 1.0f) {
                currentMaterial.setTransparent(true);
            }
        } catch (NumberFormatException e) {
            // 忽略
        }
    }

    /**
     * 解析透明度 (Tr value) - 与 d 相反
     */
    private void parseTr(String args) {
        if (currentMaterial == null) return;
        try {
            float tr = Float.parseFloat(args.trim());
            float alpha = 1.0f - tr;
            currentMaterial
                .setColor(currentMaterial.getColorR(), currentMaterial.getColorG(), currentMaterial.getColorB(), alpha);
            if (alpha < 1.0f) {
                currentMaterial.setTransparent(true);
            }
        } catch (NumberFormatException e) {
            // 忽略
        }
    }

    /**
     * 解析漫反射贴图 (map_Kd path)
     */
    private void parseMapKd(String args) {
        if (currentMaterial == null) return;
        String texturePath = extractTexturePath(args);
        if (texturePath != null) {
            String textureKey = buildTextureKey(texturePath);
            currentMaterial.setTexture(textureKey);
        }
    }

    /**
     * 解析法线/凹凸贴图 (map_Bump path 或 bump path)
     */
    private void parseMapBump(String args) {
        if (currentMaterial == null) return;
        String texturePath = extractTexturePath(args);
        if (texturePath != null) {
            String textureKey = buildTextureKey(texturePath);
            currentMaterial.setNormalMap(textureKey);
        }
    }

    /**
     * 从参数中提取纹理路径（处理可能的选项）
     */
    private String extractTexturePath(String args) {
        // MTL 可能有选项如 -s 1 1 1，纹理路径在最后
        String[] parts = args.split("\\s+");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }

    /**
     * 构建纹理资源键
     */
    private String buildTextureKey(String texturePath) {
        // 移除文件扩展名
        String path = texturePath;
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx > 0) {
            path = path.substring(0, dotIdx);
        }

        // 替换反斜杠为正斜杠
        path = path.replace('\\', '/');

        // 如果有前缀，拼接
        if (!texturePrefix.isEmpty()) {
            if (texturePrefix.endsWith("/")) {
                return texturePrefix + path;
            } else {
                return texturePrefix + "/" + path;
            }
        }

        return path;
    }

    /**
     * 解析颜色值 (r g b)
     */
    private float[] parseColor(String args) {
        String[] parts = args.trim()
            .split("\\s+");
        if (parts.length < 3) return null;

        try {
            float r = Float.parseFloat(parts[0]);
            float g = Float.parseFloat(parts[1]);
            float b = Float.parseFloat(parts[2]);
            return new float[] { r, g, b };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 保存当前材质
     */
    private void flushCurrentMaterial() {
        if (currentMaterialName != null && currentMaterial != null) {
            materials.put(currentMaterialName, currentMaterial);
        }
        currentMaterialName = null;
        currentMaterial = null;
    }

    /**
     * 清理状态
     */
    private void clear() {
        currentMaterialName = null;
        currentMaterial = null;
        materials.clear();
    }
}
