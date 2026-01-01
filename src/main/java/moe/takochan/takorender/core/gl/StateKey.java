package moe.takochan.takorender.core.gl;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * GL 状态键枚举
 *
 * <p>
 * 用于标识不同的 GL 状态，在 RenderEvent 中实现去重。
 * 通过 HashMap 以 StateKey 为键，确保每个状态在单次渲染事件中只保存一次。
 * </p>
 *
 * <p>
 * <b>设计目的</b>:
 * </p>
 * <ul>
 * <li>防止重复保存：多次调用 enableBlend() 时，只在第一次保存状态</li>
 * <li>精确追踪：HashMap 记录哪些状态已被修改</li>
 * <li>LIFO 恢复：List 记录修改顺序，恢复时反向遍历</li>
 * </ul>
 *
 * <p>
 * <b>组织方式</b>: 按 OpenGL 属性位分组（GL_ATTRIB_BITS）
 * </p>
 *
 * <p>
 * <b>总计</b>: 67 个状态键，覆盖 OpenGL 1.3 固定管线所有状态
 * </p>
 *
 * @see RenderEvent#saveState(StateKey, StateTracker)
 * @see RenderEvent#savedStates
 */
@SideOnly(Side.CLIENT)
enum StateKey {
    // GL_COLOR_BUFFER_BIT (9 states)
    /** 混合开关状态 (GL_BLEND) */
    BLEND_ENABLED,
    /** 混合函数 (glBlendFunc) */
    BLEND_FUNC,
    /** Alpha 测试开关 (GL_ALPHA_TEST) */
    ALPHA_TEST_ENABLED,
    /** Alpha 测试函数 (glAlphaFunc) */
    ALPHA_TEST_FUNC,
    /** 抖动开关 (GL_DITHER) */
    DITHER_ENABLED,
    /** 逻辑操作开关 (GL_COLOR_LOGIC_OP) */
    LOGIC_OP_ENABLED,
    /** 逻辑操作模式 (glLogicOp) */
    LOGIC_OP_MODE,
    /** 颜色掩码 (glColorMask) */
    COLOR_MASK,
    /** 清除颜色 (glClearColor) */
    CLEAR_COLOR,

    // GL_CURRENT_BIT (3 states)
    /** 当前颜色 (glColor4f) */
    CURRENT_COLOR,
    /** 当前法线 (glNormal3f) */
    CURRENT_NORMAL,
    /** 当前纹理坐标 (glTexCoord4f) */
    CURRENT_TEXCOORD,

    // GL_DEPTH_BUFFER_BIT (4 states)
    /** 深度测试开关 (GL_DEPTH_TEST) */
    DEPTH_TEST_ENABLED,
    /** 深度写入掩码 (glDepthMask) */
    DEPTH_MASK,
    /** 深度测试函数 (glDepthFunc) */
    DEPTH_FUNC,
    /** 清除深度值 (glClearDepth) */
    CLEAR_DEPTH,

    // GL_ENABLE_BIT (15 states)
    /** 2D 纹理开关 (GL_TEXTURE_2D) */
    TEXTURE_2D_ENABLED,
    /** 光照开关 (GL_LIGHTING) */
    LIGHTING_ENABLED,
    /** 面剔除开关 (GL_CULL_FACE) */
    CULL_FACE_ENABLED,
    /** 雾效开关 (GL_FOG) */
    FOG_ENABLED,
    /** 裁剪测试开关 (GL_SCISSOR_TEST) */
    SCISSOR_TEST_ENABLED,
    /** 模板测试开关 (GL_STENCIL_TEST) */
    STENCIL_TEST_ENABLED,
    /** 法线归一化开关 (GL_NORMALIZE) */
    NORMALIZE_ENABLED,
    /** 法线重缩放开关 (GL_RESCALE_NORMAL) */
    RESCALE_NORMAL_ENABLED,
    /** 多边形填充偏移开关 (GL_POLYGON_OFFSET_FILL) */
    POLYGON_OFFSET_FILL_ENABLED,
    /** 多边形线框偏移开关 (GL_POLYGON_OFFSET_LINE) */
    POLYGON_OFFSET_LINE_ENABLED,
    /** 线条点画开关 (GL_LINE_STIPPLE) */
    LINE_STIPPLE_ENABLED,
    /** 线条平滑开关 (GL_LINE_SMOOTH) */
    LINE_SMOOTH_ENABLED,
    /** 多边形点画开关 (GL_POLYGON_STIPPLE) */
    POLYGON_STIPPLE_ENABLED,
    /** 多边形平滑开关 (GL_POLYGON_SMOOTH) */
    POLYGON_SMOOTH_ENABLED,
    /** 点平滑开关 (GL_POINT_SMOOTH) */
    POINT_SMOOTH_ENABLED,

    // GL_FOG_BIT (5 states)
    /** 雾效模式 (glFogi GL_FOG_MODE) */
    FOG_MODE,
    /** 雾效密度 (glFogf GL_FOG_DENSITY) */
    FOG_DENSITY,
    /** 雾效起始距离 (glFogf GL_FOG_START) */
    FOG_START,
    /** 雾效结束距离 (glFogf GL_FOG_END) */
    FOG_END,
    /** 雾效颜色 (glFog GL_FOG_COLOR) */
    FOG_COLOR,

    // GL_HINT_BIT (5 states)
    /** 透视修正提示 (glHint GL_PERSPECTIVE_CORRECTION_HINT) */
    PERSPECTIVE_CORRECTION_HINT,
    /** 点平滑提示 (glHint GL_POINT_SMOOTH_HINT) */
    POINT_SMOOTH_HINT,
    /** 线条平滑提示 (glHint GL_LINE_SMOOTH_HINT) */
    LINE_SMOOTH_HINT,
    /** 多边形平滑提示 (glHint GL_POLYGON_SMOOTH_HINT) */
    POLYGON_SMOOTH_HINT,
    /** 雾效提示 (glHint GL_FOG_HINT) */
    FOG_HINT,

    // GL_LIGHTING_BIT (9 states)
    /** 颜色材质开关 (GL_COLOR_MATERIAL) */
    COLOR_MATERIAL_ENABLED,
    /** 颜色材质面 (glColorMaterial face) */
    COLOR_MATERIAL_FACE,
    /** 颜色材质模式 (glColorMaterial mode) */
    COLOR_MATERIAL_MODE,
    /** 着色模型 (glShadeModel) */
    SHADE_MODEL,
    /** 材质环境光 (glMaterial GL_AMBIENT) */
    MATERIAL_AMBIENT,
    /** 材质漫反射 (glMaterial GL_DIFFUSE) */
    MATERIAL_DIFFUSE,
    /** 材质镜面反射 (glMaterial GL_SPECULAR) */
    MATERIAL_SPECULAR,
    /** 材质自发光 (glMaterial GL_EMISSION) */
    MATERIAL_EMISSION,
    /** 材质光泽度 (glMaterialf GL_SHININESS) */
    MATERIAL_SHININESS,

    // GL_LINE_BIT (1 state)
    /** 线条宽度 (glLineWidth) */
    LINE_WIDTH,

    // GL_POINT_BIT (1 state)
    /** 点大小 (glPointSize) */
    POINT_SIZE,

    // GL_POLYGON_BIT (4 states)
    /** 面剔除模式 (glCullFace) */
    CULL_FACE_MODE,
    /** 正面方向 (glFrontFace) */
    FRONT_FACE,
    /** 多边形模式 (glPolygonMode) */
    POLYGON_MODE,
    /** 多边形偏移 (glPolygonOffset) */
    POLYGON_OFFSET,

    // GL_SCISSOR_BIT (1 state)
    /** 裁剪框 (glScissor) */
    SCISSOR_BOX,

    // GL_STENCIL_BUFFER_BIT (4 states)
    /** 模板测试函数 (glStencilFunc) */
    STENCIL_FUNC,
    /** 模板操作 (glStencilOp) */
    STENCIL_OP,
    /** 模板写入掩码 (glStencilMask) */
    STENCIL_MASK,
    /** 清除模板值 (glClearStencil) */
    CLEAR_STENCIL,

    // GL_TEXTURE_BIT (3 states)
    /** 活动纹理单元 (glActiveTexture) */
    ACTIVE_TEXTURE,
    /** 2D 纹理绑定 (glBindTexture GL_TEXTURE_2D) */
    TEXTURE_2D_BINDING,
    /** 纹理环境模式 (glTexEnvi GL_TEXTURE_ENV_MODE) */
    TEX_ENV_MODE,

    // GL_VIEWPORT_BIT (2 states)
    /** 视口 (glViewport) */
    VIEWPORT,
    /** 深度范围 (glDepthRange) */
    DEPTH_RANGE
}
