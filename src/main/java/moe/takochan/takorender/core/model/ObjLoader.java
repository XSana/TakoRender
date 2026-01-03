package moe.takochan.takorender.core.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector2f;
import org.joml.Vector3f;

import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.graphics.Material;
import moe.takochan.takorender.api.graphics.Mesh;
import moe.takochan.takorender.api.graphics.Model;
import moe.takochan.takorender.api.graphics.mesh.StaticMesh;
import moe.takochan.takorender.api.graphics.mesh.VertexFormat;

/**
 * OBJ 模型加载器
 *
 * <p>
 * 解析 Wavefront OBJ 格式文件，生成 Model 对象。
 * 支持顶点位置、纹理坐标、法线和多材质/子网格。
 * </p>
 *
 * <p>
 * <b>支持的 OBJ 指令</b>:
 * </p>
 * <ul>
 * <li>v - 顶点位置</li>
 * <li>vt - 纹理坐标</li>
 * <li>vn - 法线向量</li>
 * <li>f - 面定义（三角形/四边形）</li>
 * <li>usemtl - 使用材质</li>
 * <li>mtllib - 引用材质库</li>
 * <li>o/g - 对象/组名（用于子网格分割）</li>
 * </ul>
 */
public class ObjLoader {

    /** 顶点位置列表 */
    private final List<Vector3f> positions = new ArrayList<>();
    /** 纹理坐标列表 */
    private final List<Vector2f> texCoords = new ArrayList<>();
    /** 法线列表 */
    private final List<Vector3f> normals = new ArrayList<>();

    /** 当前子网格的面数据 */
    private final List<int[]> currentFaces = new ArrayList<>();
    /** 当前材质名称 */
    private String currentMaterial = null;

    /** 子网格数据 */
    private final List<SubMeshData> subMeshDataList = new ArrayList<>();

    /** 材质库名称 */
    private String mtlLibName = null;

    /** 材质映射（从 MTL 加载） */
    private Map<String, Material> materials = new HashMap<>();

    /**
     * 从输入流加载 OBJ 模型
     *
     * @param inputStream OBJ 文件输入流
     * @param name        模型名称
     * @return 加载的模型
     * @throws IOException 如果读取失败
     */
    public Model load(InputStream inputStream, String name) throws IOException {
        clear();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line.trim());
            }
        }

        // 保存最后一个子网格
        flushCurrentSubMesh();

        // 构建 Model
        return buildModel(name);
    }

    /**
     * 设置材质映射（从 MTL 文件加载后设置）
     *
     * @param materials 材质映射
     */
    public void setMaterials(Map<String, Material> materials) {
        this.materials = materials != null ? materials : new HashMap<>();
    }

    /**
     * 获取材质库名称
     *
     * @return mtllib 指定的材质库名称
     */
    public String getMtlLibName() {
        return mtlLibName;
    }

    /**
     * 解析单行
     */
    private void parseLine(String line) {
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        String[] parts = line.split("\\s+");
        if (parts.length == 0) {
            return;
        }

        String cmd = parts[0];

        switch (cmd) {
            case "v":
                parseVertex(parts);
                break;
            case "vt":
                parseTexCoord(parts);
                break;
            case "vn":
                parseNormal(parts);
                break;
            case "f":
                parseFace(parts);
                break;
            case "usemtl":
                parseUseMtl(parts);
                break;
            case "mtllib":
                parseMtlLib(parts);
                break;
            case "o":
            case "g":
                parseGroup(parts);
                break;
            default:
                break;
        }
    }

    /**
     * 解析顶点位置 (v x y z [w])
     */
    private void parseVertex(String[] parts) {
        if (parts.length < 4) return;
        float x = parseFloat(parts[1]);
        float y = parseFloat(parts[2]);
        float z = parseFloat(parts[3]);
        positions.add(new Vector3f(x, y, z));
    }

    /**
     * 解析纹理坐标 (vt u v [w])
     */
    private void parseTexCoord(String[] parts) {
        if (parts.length < 3) return;
        float u = parseFloat(parts[1]);
        float v = parseFloat(parts[2]);
        texCoords.add(new Vector2f(u, v));
    }

    /**
     * 解析法线 (vn x y z)
     */
    private void parseNormal(String[] parts) {
        if (parts.length < 4) return;
        float x = parseFloat(parts[1]);
        float y = parseFloat(parts[2]);
        float z = parseFloat(parts[3]);
        normals.add(new Vector3f(x, y, z).normalize());
    }

    /**
     * 解析面 (f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...)
     */
    private void parseFace(String[] parts) {
        if (parts.length < 4) return;

        // 解析所有顶点索引
        List<int[]> faceVertices = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            int[] indices = parseVertexIndices(parts[i]);
            if (indices != null) {
                faceVertices.add(indices);
            }
        }

        // 三角化（将多边形拆分为三角形）
        if (faceVertices.size() >= 3) {
            for (int i = 1; i < faceVertices.size() - 1; i++) {
                currentFaces.add(faceVertices.get(0));
                currentFaces.add(faceVertices.get(i));
                currentFaces.add(faceVertices.get(i + 1));
            }
        }
    }

    /**
     * 解析顶点索引 (v/vt/vn 格式)
     *
     * @return [positionIndex, texCoordIndex, normalIndex]，索引为 0 表示未指定
     */
    private int[] parseVertexIndices(String part) {
        String[] indices = part.split("/");
        int[] result = new int[3];

        // 位置索引（必须）
        if (indices.length > 0 && !indices[0].isEmpty()) {
            result[0] = parseInt(indices[0]);
        }

        // 纹理坐标索引（可选）
        if (indices.length > 1 && !indices[1].isEmpty()) {
            result[1] = parseInt(indices[1]);
        }

        // 法线索引（可选）
        if (indices.length > 2 && !indices[2].isEmpty()) {
            result[2] = parseInt(indices[2]);
        }

        return result;
    }

    /**
     * 解析 usemtl 指令
     */
    private void parseUseMtl(String[] parts) {
        // 切换材质时保存当前子网格
        flushCurrentSubMesh();

        if (parts.length > 1) {
            currentMaterial = parts[1];
        }
    }

    /**
     * 解析 mtllib 指令
     */
    private void parseMtlLib(String[] parts) {
        if (parts.length > 1) {
            mtlLibName = parts[1];
        }
    }

    /**
     * 解析 o/g 指令（对象/组）
     */
    private void parseGroup(String[] parts) {
        // 切换组时保存当前子网格
        flushCurrentSubMesh();
    }

    /**
     * 保存当前子网格数据
     */
    private void flushCurrentSubMesh() {
        if (currentFaces.isEmpty()) {
            return;
        }

        SubMeshData data = new SubMeshData();
        data.materialName = currentMaterial;
        data.faces = new ArrayList<>(currentFaces);

        subMeshDataList.add(data);
        currentFaces.clear();
    }

    /**
     * 构建 Model 对象
     */
    private Model buildModel(String name) {
        Model model = new Model(name);

        for (SubMeshData data : subMeshDataList) {
            Mesh mesh = buildMesh(data);
            if (mesh != null) {
                Material material = materials.get(data.materialName);
                model.addSubMesh(mesh, material);
            }
        }

        return model;
    }

    /**
     * 构建子网格
     */
    private Mesh buildMesh(SubMeshData data) {
        if (data.faces.isEmpty()) {
            return null;
        }

        boolean hasTexCoords = !texCoords.isEmpty();
        boolean hasNormals = !normals.isEmpty();

        // 选择顶点格式
        VertexFormat format;
        if (hasNormals && hasTexCoords) {
            format = VertexFormat.POSITION_3D_NORMAL_TEX;
        } else if (hasNormals) {
            format = VertexFormat.builder()
                .position3D()
                .normal()
                .build();
        } else if (hasTexCoords) {
            format = VertexFormat.POSITION_3D_TEX;
        } else {
            format = VertexFormat.POSITION_3D;
        }

        // 顶点去重和索引构建
        Map<String, Integer> vertexMap = new HashMap<>();
        List<Float> vertexData = new ArrayList<>();
        List<Integer> indexData = new ArrayList<>();

        for (int[] indices : data.faces) {
            String key = indices[0] + "/" + indices[1] + "/" + indices[2];
            Integer existingIndex = vertexMap.get(key);

            if (existingIndex != null) {
                indexData.add(existingIndex);
            } else {
                int newIndex = vertexMap.size();
                vertexMap.put(key, newIndex);
                indexData.add(newIndex);

                // 添加顶点数据
                addVertexData(vertexData, indices, hasTexCoords, hasNormals);
            }
        }

        // 转换为数组
        float[] vertices = toFloatArray(vertexData);
        int[] indices = toIntArray(indexData);

        try {
            return new StaticMesh(vertices, indices, format);
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to create mesh", e);
            return null;
        }
    }

    /**
     * 添加单个顶点数据
     */
    private void addVertexData(List<Float> vertexData, int[] indices, boolean hasTexCoords, boolean hasNormals) {

        // 位置（必须）
        int posIdx = indices[0];
        if (posIdx > 0 && posIdx <= positions.size()) {
            Vector3f pos = positions.get(posIdx - 1);
            vertexData.add(pos.x);
            vertexData.add(pos.y);
            vertexData.add(pos.z);
        } else {
            vertexData.add(0f);
            vertexData.add(0f);
            vertexData.add(0f);
        }

        // 法线（如果格式需要）
        if (hasNormals) {
            int normIdx = indices[2];
            if (normIdx > 0 && normIdx <= normals.size()) {
                Vector3f norm = normals.get(normIdx - 1);
                vertexData.add(norm.x);
                vertexData.add(norm.y);
                vertexData.add(norm.z);
            } else {
                vertexData.add(0f);
                vertexData.add(1f);
                vertexData.add(0f);
            }
        }

        // 纹理坐标（如果格式需要）
        if (hasTexCoords) {
            int texIdx = indices[1];
            if (texIdx > 0 && texIdx <= texCoords.size()) {
                Vector2f tex = texCoords.get(texIdx - 1);
                vertexData.add(tex.x);
                vertexData.add(tex.y);
            } else {
                vertexData.add(0f);
                vertexData.add(0f);
            }
        }
    }

    /**
     * 清理状态
     */
    private void clear() {
        positions.clear();
        texCoords.clear();
        normals.clear();
        currentFaces.clear();
        currentMaterial = null;
        subMeshDataList.clear();
        mtlLibName = null;
    }

    private float parseFloat(String s) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    /**
     * 子网格临时数据
     */
    private static class SubMeshData {

        String materialName;
        List<int[]> faces;
    }
}
