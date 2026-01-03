package moe.takochan.takorender.core.debug;

/**
 * 调试渲染模式
 */
public enum DebugMode {

    /** 正常渲染 */
    NONE,

    /** 线框模式 */
    WIREFRAME,

    /** 显示包围盒 */
    BOUNDING_BOX,

    /** 深度可视化（近白远黑） */
    DEPTH,

    /** 法线可视化 */
    NORMALS,

    /** LOD 级别颜色 */
    LOD_LEVEL
}
