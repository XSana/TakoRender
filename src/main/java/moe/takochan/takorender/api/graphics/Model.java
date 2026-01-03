package moe.takochan.takorender.api.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 3D 模型，包含多个子网格和材质
 *
 * <p>
 * Model 是 Mesh 和 Material 的容器，支持多子网格结构。
 * 常用于加载外部模型格式（OBJ、MC JSON 等）。
 * </p>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // 创建模型
 *     Model model = new Model();
 *     model.addSubMesh(mesh1, material1);
 *     model.addSubMesh(mesh2, material2);
 *
 *     // 在 ModelComponent 中使用
 *     entity.addComponent(new ModelComponent().setModel(model));
 * }
 * </pre>
 */
public class Model {

    /** 子网格列表 */
    private final List<SubMesh> subMeshes = new ArrayList<>();

    /** 模型整体包围盒（所有子网格的并集） */
    private AABB bounds;

    /** 模型名称（用于调试和缓存） */
    private String name;

    /**
     * 创建空模型
     */
    public Model() {}

    /**
     * 创建命名模型
     *
     * @param name 模型名称
     */
    public Model(String name) {
        this.name = name;
    }

    /**
     * 添加子网格
     *
     * @param mesh     网格
     * @param material 材质
     */
    public void addSubMesh(Mesh mesh, Material material) {
        subMeshes.add(new SubMesh(mesh, material));
        invalidateBounds();
    }

    /**
     * 添加子网格（无材质）
     *
     * @param mesh 网格
     */
    public void addSubMesh(Mesh mesh) {
        addSubMesh(mesh, null);
    }

    /**
     * 获取子网格列表（只读）
     *
     * @return 子网格列表
     */
    public List<SubMesh> getSubMeshes() {
        return Collections.unmodifiableList(subMeshes);
    }

    /**
     * 获取子网格数量
     *
     * @return 子网格数量
     */
    public int getSubMeshCount() {
        return subMeshes.size();
    }

    /**
     * 获取指定索引的子网格
     *
     * @param index 索引
     * @return 子网格
     */
    public SubMesh getSubMesh(int index) {
        if (index < 0 || index >= subMeshes.size()) {
            return null;
        }
        return subMeshes.get(index);
    }

    /**
     * 获取模型整体包围盒
     *
     * @return 包围盒
     */
    public AABB getBounds() {
        if (bounds == null) {
            computeBounds();
        }
        return bounds;
    }

    /**
     * 计算整体包围盒
     */
    private void computeBounds() {
        AABB result = null;
        for (SubMesh sub : subMeshes) {
            AABB meshBounds = sub.mesh.getBounds();
            if (meshBounds != null && meshBounds.isValid()) {
                if (result == null) {
                    result = meshBounds;
                } else {
                    result = result.merge(meshBounds);
                }
            }
        }
        this.bounds = result;
    }

    /**
     * 使包围盒缓存失效
     */
    private void invalidateBounds() {
        this.bounds = null;
    }

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置模型名称
     *
     * @param name 名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 释放所有资源
     */
    public void dispose() {
        for (SubMesh sub : subMeshes) {
            if (sub.mesh != null) {
                sub.mesh.dispose();
            }
        }
        subMeshes.clear();
        bounds = null;
    }

    /**
     * 检查是否已释放
     *
     * @return true 表示已释放
     */
    public boolean isDisposed() {
        return subMeshes.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("Model[name=%s, subMeshes=%d]", name, subMeshes.size());
    }

    /**
     * 子网格，包含 Mesh 和关联的 Material
     */
    public static class SubMesh {

        private final Mesh mesh;
        private Material material;

        /**
         * 创建子网格
         *
         * @param mesh     网格
         * @param material 材质
         */
        public SubMesh(Mesh mesh, Material material) {
            this.mesh = mesh;
            this.material = material;
        }

        /**
         * 获取网格
         *
         * @return 网格
         */
        public Mesh getMesh() {
            return mesh;
        }

        /**
         * 获取材质
         *
         * @return 材质
         */
        public Material getMaterial() {
            return material;
        }

        /**
         * 设置材质
         *
         * @param material 材质
         */
        public void setMaterial(Material material) {
            this.material = material;
        }
    }
}
