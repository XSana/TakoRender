package moe.takochan.takorender.core.render;

import java.util.Objects;

import moe.takochan.takorender.api.graphics.Material;
import moe.takochan.takorender.api.graphics.Mesh;

/**
 * 批次键
 *
 * <p>
 * BatchKey 用于按 Mesh+Material 分组可实例化渲染的 Entity。
 * 相同 BatchKey 的 Entity 可以合并为单次 Instanced Draw Call。
 * </p>
 *
 * <p>
 * <b>使用场景</b>: InstancedRenderSystem 内部使用
 * </p>
 */
public final class BatchKey {

    private final Mesh mesh;
    private final Material material;
    private final int hashCode;

    /**
     * 创建批次键
     *
     * @param mesh     网格（不能为 null）
     * @param material 材质（不能为 null）
     */
    public BatchKey(Mesh mesh, Material material) {
        if (mesh == null || material == null) {
            throw new IllegalArgumentException("Mesh and Material cannot be null");
        }
        this.mesh = mesh;
        this.material = material;
        // 预计算 hashCode（不可变对象）
        this.hashCode = Objects.hash(System.identityHashCode(mesh), System.identityHashCode(material));
    }

    /**
     * 获取网格
     */
    public Mesh getMesh() {
        return mesh;
    }

    /**
     * 获取材质
     */
    public Material getMaterial() {
        return material;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BatchKey)) {
            return false;
        }
        BatchKey other = (BatchKey) obj;
        // 使用引用相等（相同实例）
        return mesh == other.mesh && material == other.material;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "BatchKey[mesh=" + mesh + ", material=" + material + "]";
    }
}
