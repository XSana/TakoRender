package moe.takochan.takorender.api.graphics.mesh;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 顶点格式定义，封装顶点属性布局。
 *
 * <p>
 * 提供预定义的常用格式，遵循 OpenGL 标准实践。
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class VertexFormat {

    /** 2D 位置 (vec2) - 8 字节 */
    public static final VertexFormat POSITION_2D = new VertexFormat(8, VertexAttribute.position2D(0));

    /** 2D 位置 + RGBA 颜色 (vec2 + vec4) - 24 字节 */
    public static final VertexFormat POSITION_COLOR = new VertexFormat(
        24,
        VertexAttribute.position2D(0),
        VertexAttribute.colorFloat(8));

    /** 2D 位置 + 纹理坐标 (vec2 + vec2) - 16 字节 */
    public static final VertexFormat POSITION_TEX = new VertexFormat(
        16,
        VertexAttribute.position2D(0),
        VertexAttribute.texCoord(8));

    /** 2D 位置 + 纹理坐标 + RGBA 颜色 (vec2 + vec2 + vec4) - 32 字节 */
    public static final VertexFormat POSITION_TEX_COLOR = new VertexFormat(
        32,
        VertexAttribute.position2D(0),
        VertexAttribute.texCoord(8),
        VertexAttribute.colorFloat(16));

    /** 3D 位置 (vec3) - 12 字节 */
    public static final VertexFormat POSITION_3D = new VertexFormat(12, VertexAttribute.position3D(0));

    /** 3D 位置 + 纹理坐标 (vec3 + vec2) - 20 字节 */
    public static final VertexFormat POSITION_3D_TEX = new VertexFormat(
        20,
        VertexAttribute.position3D(0),
        VertexAttribute.texCoord(12));

    /** 3D 位置 + 法线 + 纹理坐标 (vec3 + vec3 + vec2) - 32 字节 */
    public static final VertexFormat POSITION_3D_NORMAL_TEX = new VertexFormat(
        32,
        VertexAttribute.position3D(0),
        VertexAttribute.normal(12),
        VertexAttribute.texCoord(24));

    /** 3D 位置 + RGBA 颜色 (vec3 + vec4) - 28 字节 */
    public static final VertexFormat POSITION_3D_COLOR = new VertexFormat(
        28,
        VertexAttribute.position3D(0),
        VertexAttribute.colorFloat(12));

    /** 3D 位置 + 法线 + 颜色 (vec3 + vec3 + vec4) - 40 字节 */
    public static final VertexFormat POSITION_3D_NORMAL_COLOR = new VertexFormat(
        40,
        VertexAttribute.position3D(0),
        VertexAttribute.normal(12),
        VertexAttribute.colorFloat(24));

    /** 3D 位置 + 颜色 + 光照坐标 (vec3 + vec4 + vec2) - 36 字节 */
    public static final VertexFormat POSITION_3D_COLOR_LIGHT = new VertexFormat(
        36,
        VertexAttribute.position3D(0),
        VertexAttribute.colorFloat(12),
        VertexAttribute.lightCoord(28));

    /** 3D 位置 + 颜色 + 光照坐标 + 法线 (vec3 + vec4 + vec2 + vec3) - 48 字节 */
    public static final VertexFormat POSITION_3D_COLOR_LIGHT_NORMAL = new VertexFormat(
        48,
        VertexAttribute.position3D(0),
        VertexAttribute.colorFloat(12),
        VertexAttribute.lightCoord(28),
        VertexAttribute.normal(36));

    private final int stride;
    private final VertexAttribute[] attributes;

    /**
     * 创建顶点格式
     *
     * @param stride     每个顶点的字节大小
     * @param attributes 顶点属性数组
     */
    public VertexFormat(int stride, VertexAttribute... attributes) {
        this.stride = stride;
        this.attributes = attributes.clone();
    }

    /**
     * 获取每个顶点的字节大小
     */
    public int getStride() {
        return stride;
    }

    /**
     * 获取顶点属性数组
     */
    public VertexAttribute[] getAttributes() {
        return attributes.clone();
    }

    /**
     * 获取属性数量
     */
    public int getAttributeCount() {
        return attributes.length;
    }

    /**
     * 获取每个顶点的浮点数数量
     */
    public int getFloatsPerVertex() {
        return stride / 4;
    }

    /**
     * 计算指定数量顶点所需的字节数
     */
    public int calculateBufferSize(int vertexCount) {
        return vertexCount * stride;
    }

    /**
     * 创建自定义格式的构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 顶点格式构建器
     */
    public static class Builder {

        private int currentOffset = 0;
        private List<VertexAttribute> attrs = new ArrayList<>();

        public Builder position2D() {
            attrs.add(VertexAttribute.position2D(currentOffset));
            currentOffset += 8;
            return this;
        }

        public Builder position3D() {
            attrs.add(VertexAttribute.position3D(currentOffset));
            currentOffset += 12;
            return this;
        }

        public Builder texCoord() {
            attrs.add(VertexAttribute.texCoord(currentOffset));
            currentOffset += 8;
            return this;
        }

        public Builder normal() {
            attrs.add(VertexAttribute.normal(currentOffset));
            currentOffset += 12;
            return this;
        }

        public Builder colorFloat() {
            attrs.add(VertexAttribute.colorFloat(currentOffset));
            currentOffset += 16;
            return this;
        }

        public Builder colorByte() {
            attrs.add(VertexAttribute.colorByte(currentOffset));
            currentOffset += 4;
            return this;
        }

        public Builder lightCoord() {
            attrs.add(VertexAttribute.lightCoord(currentOffset));
            currentOffset += 8;
            return this;
        }

        public VertexFormat build() {
            return new VertexFormat(currentOffset, attrs.toArray(new VertexAttribute[0]));
        }
    }
}
