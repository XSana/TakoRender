package moe.takochan.takorender.api.graphics;

/**
 * 网格接口 - 定义可渲染的 3D 网格数据
 *
 * <p>
 * Mesh 包含顶点数据（位置、法线、UV 等）和索引数据，
 * 由 MeshRenderSystem 用于绘制。
 * </p>
 *
 * <p>
 * <b>实现说明</b>:
 * </p>
 * <ul>
 * <li>顶点数据应上传到 GPU（VBO）</li>
 * <li>支持索引绘制（IBO）</li>
 * <li>需要正确管理 OpenGL 资源生命周期</li>
 * </ul>
 */
public interface Mesh {

    /**
     * 绑定此网格以准备绘制
     */
    void bind();

    /**
     * 解绑此网格
     */
    void unbind();

    /**
     * 绘制此网格
     */
    void draw();

    /**
     * 获取顶点数量
     *
     * @return 顶点数量
     */
    int getVertexCount();

    /**
     * 获取索引数量（如果使用索引绘制）
     *
     * @return 索引数量，如果不使用索引则返回 0
     */
    int getIndexCount();

    /**
     * 释放 GPU 资源
     */
    void dispose();

    /**
     * 检查资源是否已释放
     *
     * @return true 表示已释放
     */
    boolean isDisposed();
}
