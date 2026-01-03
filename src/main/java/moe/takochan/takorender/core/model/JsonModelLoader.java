package moe.takochan.takorender.core.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.graphics.Mesh;
import moe.takochan.takorender.api.graphics.Model;
import moe.takochan.takorender.api.graphics.StandardMaterial;
import moe.takochan.takorender.api.graphics.mesh.StaticMesh;
import moe.takochan.takorender.api.graphics.mesh.VertexFormat;

/**
 * MC JSON 模型加载器
 *
 * <p>
 * 解析 Minecraft 1.8+ / BlockBench 风格的 JSON 模型格式。
 * </p>
 *
 * <p>
 * <b>支持的特性</b>:
 * </p>
 * <ul>
 * <li>elements - 方块元素列表</li>
 * <li>faces - 六个面（up/down/north/south/east/west）</li>
 * <li>uv - 纹理坐标</li>
 * <li>rotation - 元素旋转</li>
 * <li>textures - 纹理变量映射</li>
 * </ul>
 *
 * <p>
 * <b>注意</b>: 自动计算面法线，用于光照计算。
 * </p>
 */
public class JsonModelLoader {

    /** 纹理变量映射 */
    private final Map<String, String> textureVariables = new HashMap<>();

    /** 纹理路径前缀 */
    private String texturePrefix = "";

    /** 模型缩放（MC JSON 使用 0-16 坐标系） */
    private static final float SCALE = 1.0f / 16.0f;

    /** 面方向定义 */
    private static final String[] FACE_NAMES = { "up", "down", "north", "south", "east", "west" };

    /** 各面的法线 */
    private static final Vector3f[] FACE_NORMALS = { new Vector3f(0, 1, 0), // up
        new Vector3f(0, -1, 0), // down
        new Vector3f(0, 0, -1), // north
        new Vector3f(0, 0, 1), // south
        new Vector3f(1, 0, 0), // east
        new Vector3f(-1, 0, 0) // west
    };

    /**
     * 从输入流加载 JSON 模型
     *
     * @param inputStream JSON 文件输入流
     * @param name        模型名称
     * @return 加载的模型
     * @throws IOException 如果读取失败
     */
    public Model load(InputStream inputStream, String name) throws IOException {
        clear();

        JsonObject root;
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            root = new JsonParser().parse(reader)
                .getAsJsonObject();
        }

        // 解析纹理变量
        if (root.has("textures")) {
            parseTextures(root.getAsJsonObject("textures"));
        }

        // 解析元素
        List<ElementData> elements = new ArrayList<>();
        if (root.has("elements")) {
            JsonArray elementsArray = root.getAsJsonArray("elements");
            for (JsonElement elem : elementsArray) {
                ElementData data = parseElement(elem.getAsJsonObject());
                if (data != null) {
                    elements.add(data);
                }
            }
        }

        // 构建模型
        return buildModel(name, elements);
    }

    /**
     * 设置纹理路径前缀
     *
     * @param prefix 纹理前缀（如 "takorender:models/block"）
     */
    public void setTexturePrefix(String prefix) {
        this.texturePrefix = prefix != null ? prefix : "";
    }

    /**
     * 解析纹理变量映射
     */
    private void parseTextures(JsonObject textures) {
        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue()
                .getAsString();
            textureVariables.put(key, value);
        }
    }

    /**
     * 解析单个元素
     */
    private ElementData parseElement(JsonObject element) {
        if (!element.has("from") || !element.has("to")) {
            return null;
        }

        ElementData data = new ElementData();

        // 解析 from/to 坐标
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");

        data.from = new Vector3f(
            from.get(0)
                .getAsFloat() * SCALE,
            from.get(1)
                .getAsFloat() * SCALE,
            from.get(2)
                .getAsFloat() * SCALE);

        data.to = new Vector3f(
            to.get(0)
                .getAsFloat() * SCALE,
            to.get(1)
                .getAsFloat() * SCALE,
            to.get(2)
                .getAsFloat() * SCALE);

        // 解析旋转（可选）
        if (element.has("rotation")) {
            data.rotation = parseRotation(element.getAsJsonObject("rotation"));
        }

        // 解析面
        if (element.has("faces")) {
            JsonObject faces = element.getAsJsonObject("faces");
            for (int i = 0; i < FACE_NAMES.length; i++) {
                String faceName = FACE_NAMES[i];
                if (faces.has(faceName)) {
                    FaceData face = parseFace(faces.getAsJsonObject(faceName), i);
                    if (face != null) {
                        data.faces.add(face);
                    }
                }
            }
        }

        return data;
    }

    /**
     * 解析旋转信息
     */
    private RotationData parseRotation(JsonObject rotation) {
        RotationData data = new RotationData();

        if (rotation.has("origin")) {
            JsonArray origin = rotation.getAsJsonArray("origin");
            data.origin = new Vector3f(
                origin.get(0)
                    .getAsFloat() * SCALE,
                origin.get(1)
                    .getAsFloat() * SCALE,
                origin.get(2)
                    .getAsFloat() * SCALE);
        }

        if (rotation.has("axis")) {
            data.axis = rotation.get("axis")
                .getAsString();
        }

        if (rotation.has("angle")) {
            data.angle = rotation.get("angle")
                .getAsFloat();
        }

        return data;
    }

    /**
     * 解析单个面
     */
    private FaceData parseFace(JsonObject face, int faceIndex) {
        FaceData data = new FaceData();
        data.faceIndex = faceIndex;
        data.normal = FACE_NORMALS[faceIndex];

        // 解析 UV
        if (face.has("uv")) {
            JsonArray uv = face.getAsJsonArray("uv");
            data.uv = new float[] { uv.get(0)
                .getAsFloat() / 16f,
                uv.get(1)
                    .getAsFloat() / 16f,
                uv.get(2)
                    .getAsFloat() / 16f,
                uv.get(3)
                    .getAsFloat() / 16f };
        } else {
            // 默认 UV
            data.uv = new float[] { 0, 0, 1, 1 };
        }

        // 解析纹理引用
        if (face.has("texture")) {
            String texRef = face.get("texture")
                .getAsString();
            data.texture = resolveTexture(texRef);
        }

        // 解析旋转（UV 旋转）
        if (face.has("rotation")) {
            data.uvRotation = face.get("rotation")
                .getAsInt();
        }

        return data;
    }

    /**
     * 解析纹理引用（处理 #variable 格式）
     */
    private String resolveTexture(String texRef) {
        if (texRef.startsWith("#")) {
            String varName = texRef.substring(1);
            String resolved = textureVariables.get(varName);
            if (resolved != null) {
                return buildTextureKey(resolved);
            }
        }
        return buildTextureKey(texRef);
    }

    /**
     * 构建纹理资源键
     */
    private String buildTextureKey(String texturePath) {
        // 如果已经是完整路径（包含 :），直接返回
        if (texturePath.contains(":")) {
            return texturePath;
        }

        // 如果有前缀，拼接
        if (!texturePrefix.isEmpty()) {
            if (texturePrefix.endsWith("/")) {
                return texturePrefix + texturePath;
            } else {
                return texturePrefix + "/" + texturePath;
            }
        }

        return texturePath;
    }

    /**
     * 构建模型
     */
    private Model buildModel(String name, List<ElementData> elements) {
        Model model = new Model(name);

        // 按纹理分组（相同纹理的面合并为一个子网格）
        Map<String, List<FaceVertexData>> textureGroups = new HashMap<>();

        for (ElementData element : elements) {
            for (FaceData face : element.faces) {
                String texture = face.texture != null ? face.texture : "";
                textureGroups.computeIfAbsent(texture, k -> new ArrayList<>())
                    .addAll(generateFaceVertices(element, face));
            }
        }

        // 为每个纹理组创建子网格
        for (Map.Entry<String, List<FaceVertexData>> entry : textureGroups.entrySet()) {
            String texture = entry.getKey();
            List<FaceVertexData> vertices = entry.getValue();

            if (vertices.isEmpty()) continue;

            Mesh mesh = buildMesh(vertices);
            if (mesh != null) {
                StandardMaterial material = new StandardMaterial();
                if (!texture.isEmpty()) {
                    material.setTexture(texture);
                }
                model.addSubMesh(mesh, material);
            }
        }

        return model;
    }

    /**
     * 生成面的顶点数据
     */
    private List<FaceVertexData> generateFaceVertices(ElementData element, FaceData face) {
        List<FaceVertexData> vertices = new ArrayList<>();

        Vector3f[] corners = getFaceCorners(element.from, element.to, face.faceIndex);
        float[] uv = face.uv;

        // UV 坐标（根据 uvRotation 调整）
        float[][] uvCoords = getUVCoords(uv, face.uvRotation);

        // 两个三角形（四边形）
        // 0-1-2, 0-2-3
        int[][] indices = { { 0, 1, 2 }, { 0, 2, 3 } };

        for (int[] tri : indices) {
            for (int idx : tri) {
                FaceVertexData v = new FaceVertexData();
                v.position = new Vector3f(corners[idx]);
                v.normal = face.normal;
                v.u = uvCoords[idx][0];
                v.v = uvCoords[idx][1];
                vertices.add(v);
            }
        }

        return vertices;
    }

    /**
     * 获取面的四个角点
     */
    private Vector3f[] getFaceCorners(Vector3f from, Vector3f to, int faceIndex) {
        float x0 = from.x, y0 = from.y, z0 = from.z;
        float x1 = to.x, y1 = to.y, z1 = to.z;

        switch (faceIndex) {
            case 0: // up (y+)
                return new Vector3f[] { new Vector3f(x0, y1, z0), new Vector3f(x1, y1, z0), new Vector3f(x1, y1, z1),
                    new Vector3f(x0, y1, z1) };
            case 1: // down (y-)
                return new Vector3f[] { new Vector3f(x0, y0, z1), new Vector3f(x1, y0, z1), new Vector3f(x1, y0, z0),
                    new Vector3f(x0, y0, z0) };
            case 2: // north (z-)
                return new Vector3f[] { new Vector3f(x1, y1, z0), new Vector3f(x0, y1, z0), new Vector3f(x0, y0, z0),
                    new Vector3f(x1, y0, z0) };
            case 3: // south (z+)
                return new Vector3f[] { new Vector3f(x0, y1, z1), new Vector3f(x1, y1, z1), new Vector3f(x1, y0, z1),
                    new Vector3f(x0, y0, z1) };
            case 4: // east (x+)
                return new Vector3f[] { new Vector3f(x1, y1, z1), new Vector3f(x1, y1, z0), new Vector3f(x1, y0, z0),
                    new Vector3f(x1, y0, z1) };
            case 5: // west (x-)
                return new Vector3f[] { new Vector3f(x0, y1, z0), new Vector3f(x0, y1, z1), new Vector3f(x0, y0, z1),
                    new Vector3f(x0, y0, z0) };
            default:
                return new Vector3f[4];
        }
    }

    /**
     * 获取 UV 坐标（考虑旋转）
     */
    private float[][] getUVCoords(float[] uv, int rotation) {
        float u0 = uv[0], v0 = uv[1];
        float u1 = uv[2], v1 = uv[3];

        // 基础 UV 坐标（左上、右上、右下、左下）
        float[][] coords = { { u0, v0 }, { u1, v0 }, { u1, v1 }, { u0, v1 } };

        // 应用旋转（每 90 度轮换一次）
        int steps = (rotation / 90) % 4;
        if (steps > 0) {
            float[][] rotated = new float[4][2];
            for (int i = 0; i < 4; i++) {
                rotated[i] = coords[(i + steps) % 4];
            }
            return rotated;
        }

        return coords;
    }

    /**
     * 构建网格
     */
    private Mesh buildMesh(List<FaceVertexData> vertices) {
        if (vertices.isEmpty()) return null;

        VertexFormat format = VertexFormat.POSITION_3D_NORMAL_TEX;
        int floatsPerVertex = format.getFloatsPerVertex(); // 8: pos(3) + normal(3) + uv(2)

        float[] vertexData = new float[vertices.size() * floatsPerVertex];
        int[] indexData = new int[vertices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            FaceVertexData v = vertices.get(i);
            int offset = i * floatsPerVertex;

            // position
            vertexData[offset] = v.position.x;
            vertexData[offset + 1] = v.position.y;
            vertexData[offset + 2] = v.position.z;

            // normal
            vertexData[offset + 3] = v.normal.x;
            vertexData[offset + 4] = v.normal.y;
            vertexData[offset + 5] = v.normal.z;

            // uv
            vertexData[offset + 6] = v.u;
            vertexData[offset + 7] = v.v;

            indexData[i] = i;
        }

        try {
            return new StaticMesh(vertexData, indexData, format);
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to create mesh from JSON model", e);
            return null;
        }
    }

    /**
     * 清理状态
     */
    private void clear() {
        textureVariables.clear();
    }

    /** 元素数据 */
    private static class ElementData {

        Vector3f from;
        Vector3f to;
        RotationData rotation;
        List<FaceData> faces = new ArrayList<>();
    }

    /** 旋转数据 */
    private static class RotationData {

        Vector3f origin = new Vector3f(0.5f, 0.5f, 0.5f);
        String axis = "y";
        float angle = 0;
    }

    /** 面数据 */
    private static class FaceData {

        int faceIndex;
        Vector3f normal;
        float[] uv;
        String texture;
        int uvRotation = 0;
    }

    /** 面顶点数据 */
    private static class FaceVertexData {

        Vector3f position;
        Vector3f normal;
        float u, v;
    }
}
