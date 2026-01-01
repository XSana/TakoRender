package moe.takochan.takorender.core.gl;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * GL 状态管理上下文。
 *
 * <p>
 * 替代 glPushAttrib/glPopAttrib 的零 GL 栈消耗方案，支持嵌套渲染事件。
 * </p>
 *
 * <p>
 * <b>使用模式</b>:
 * </p>
 *
 * <pre>
 * {@code
 * // Try-with-resources 自动管理（推荐）
 * try (var ctx = GLStateContext.begin()) {
 *     ctx.enableBlend();
 *     ctx.setBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
 *     renderObjects();
 * } // 自动恢复所有修改过的状态
 * }
 * </pre>
 *
 * <p>
 * <b>设计原则</b>:
 * </p>
 * <ul>
 * <li>异常安全：使用 try-with-resources 保证状态恢复，即使抛出异常</li>
 * <li>懒加载保存：首次修改时查询并保存原始值，后续修改不再查询</li>
 * <li>精确恢复：只恢复真正修改过的状态（基于列表追踪）</li>
 * <li>防重复保存：每个状态只保存一次原始值</li>
 * <li>支持嵌套：使用栈结构支持多层嵌套（主线程单栈设计）</li>
 * <li>零 GL 栈消耗：不使用 glPushAttrib/glPopAttrib</li>
 * <li>易维护：所有状态恢复逻辑封装在 StateTracker 接口中</li>
 * </ul>
 *
 * <p>
 * <b>线程安全说明</b>:
 * Minecraft 客户端渲染都在主线程（Render Thread）执行，
 * 不存在多线程并发渲染，因此使用简单的静态栈而非 ThreadLocal。
 * </p>
 */
@SideOnly(Side.CLIENT)
public class GLStateContext implements AutoCloseable {

    /** 渲染事件栈 - 支持嵌套 */
    private static final Deque<RenderEvent> eventStack = new ArrayDeque<>();

    /**
     * 开始渲染作用域。
     *
     * <p>
     * 创建新的渲染上下文并返回 GLStateContext 实例。
     * 使用 try-with-resources 语法确保状态恢复。
     * </p>
     *
     * <p>
     * <b>使用示例</b>:
     * </p>
     *
     * <pre>
     * {@code
     * try (var ctx = GLStateContext.begin()) {
     *     ctx.enableBlend();
     *     ctx.setBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
     *     renderTransparentObjects();
     * } // 自动恢复状态
     * }
     * </pre>
     *
     *
     * @return GLStateContext 实例，用于 try-with-resources
     */
    public static GLStateContext begin() {
        return new GLStateContext();
    }

    // 实例字段和方法

    private final RenderEvent event;

    /**
     * 私有构造函数 - 只能通过 begin() 创建。
     */
    private GLStateContext() {
        this.event = new RenderEvent();
        eventStack.push(event);
    }

    /**
     * 恢复所有修改过的 GL 状态。
     *
     * <p>
     * 由 try-with-resources 自动调用，无需手动调用。
     * </p>
     *
     * @throws IllegalStateException 如果 GLStateContext 被以错误的顺序关闭
     */
    @Override
    public void close() {
        if (eventStack.isEmpty()) {
            throw new IllegalStateException("GLStateContext.close() called but stack is empty");
        }

        RenderEvent top = eventStack.peek();
        if (top != this.event) {
            throw new IllegalStateException(
                "GLStateContext.close() called out of order (nested contexts must be closed in reverse order)");
        }

        eventStack.pop()
            .restoreStates();
    }

    /**
     * 获取当前嵌套深度（调试用）。
     *
     * @return 栈深度
     */
    public static int getStackDepth() {
        return eventStack.size();
    }

    // GL 状态修改方法 - 对应 OpenGL 固定管线所有状态

    // GL_COLOR_BUFFER_BIT

    /** 启用混合。 */
    public void enableBlend() {
        this.event.saveBlendEnabled();
        GL11.glEnable(GL11.GL_BLEND);
    }

    /** 禁用混合。 */
    public void disableBlend() {
        this.event.saveBlendEnabled();
        GL11.glDisable(GL11.GL_BLEND);
    }

    /** 设置混合函数。 */
    public void setBlendFunc(int sfactor, int dfactor) {
        this.event.saveBlendFunc();
        GL11.glBlendFunc(sfactor, dfactor);
    }

    /** 启用 Alpha 测试。 */
    public void enableAlphaTest() {
        this.event.saveAlphaTestEnabled();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    /** 禁用 Alpha 测试。 */
    public void disableAlphaTest() {
        this.event.saveAlphaTestEnabled();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    /** 设置 Alpha 测试函数。 */
    public void setAlphaFunc(int func, float ref) {
        this.event.saveAlphaTestFunc();
        GL11.glAlphaFunc(func, ref);
    }

    /** 启用抖动。 */
    public void enableDither() {
        this.event.saveDitherEnabled();
        GL11.glEnable(GL11.GL_DITHER);
    }

    /** 禁用抖动。 */
    public void disableDither() {
        this.event.saveDitherEnabled();
        GL11.glDisable(GL11.GL_DITHER);
    }

    /** 启用颜色逻辑运算。 */
    public void enableLogicOp() {
        this.event.saveLogicOpEnabled();
        GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
    }

    /** 禁用颜色逻辑运算。 */
    public void disableLogicOp() {
        this.event.saveLogicOpEnabled();
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
    }

    /** 设置逻辑运算模式。 */
    public void setLogicOp(int opcode) {
        this.event.saveLogicOpMode();
        GL11.glLogicOp(opcode);
    }

    /** 设置颜色掩码。 */
    public void setColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        this.event.saveColorMask();
        GL11.glColorMask(red, green, blue, alpha);
    }

    /** 设置清屏颜色。 */
    public void setClearColor(float red, float green, float blue, float alpha) {
        this.event.saveClearColor();
        GL11.glClearColor(red, green, blue, alpha);
    }

    // GL_CURRENT_BIT

    /** 设置当前颜色。 */
    public void setColor(float r, float g, float b, float a) {
        this.event.saveColor();
        GL11.glColor4f(r, g, b, a);
    }

    /** 设置当前法线。 */
    public void setNormal(float x, float y, float z) {
        this.event.saveNormal();
        GL11.glNormal3f(x, y, z);
    }

    /** 设置当前纹理坐标。 */
    public void setTexCoord(float s, float t, float r, float q) {
        this.event.saveTexCoord();
        GL11.glTexCoord4f(s, t, r, q);
    }

    // GL_DEPTH_BUFFER_BIT

    /** 启用深度测试。 */
    public void enableDepthTest() {
        this.event.saveDepthTestEnabled();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    /** 禁用深度测试。 */
    public void disableDepthTest() {
        this.event.saveDepthTestEnabled();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    /** 设置深度掩码。 */
    public void setDepthMask(boolean flag) {
        this.event.saveDepthMask();
        GL11.glDepthMask(flag);
    }

    /** 设置深度测试函数。 */
    public void setDepthFunc(int func) {
        this.event.saveDepthFunc();
        GL11.glDepthFunc(func);
    }

    /** 设置深度清屏值。 */
    public void setClearDepth(double depth) {
        this.event.saveClearDepth();
        GL11.glClearDepth(depth);
    }

    // GL_ENABLE_BIT

    /** 启用纹理2D。 */
    public void enableTexture2D() {
        this.event.saveTexture2DEnabled();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /** 禁用纹理2D。 */
    public void disableTexture2D() {
        this.event.saveTexture2DEnabled();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /** 启用光照。 */
    public void enableLighting() {
        this.event.saveLightingEnabled();
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    /** 禁用光照。 */
    public void disableLighting() {
        this.event.saveLightingEnabled();
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    /** 启用面剔除。 */
    public void enableCullFace() {
        this.event.saveCullFaceEnabled();
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    /** 禁用面剔除。 */
    public void disableCullFace() {
        this.event.saveCullFaceEnabled();
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    /** 启用雾效。 */
    public void enableFog() {
        this.event.saveFogEnabled();
        GL11.glEnable(GL11.GL_FOG);
    }

    /** 禁用雾效。 */
    public void disableFog() {
        this.event.saveFogEnabled();
        GL11.glDisable(GL11.GL_FOG);
    }

    /** 启用裁剪测试。 */
    public void enableScissorTest() {
        this.event.saveScissorTestEnabled();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    /** 禁用裁剪测试。 */
    public void disableScissorTest() {
        this.event.saveScissorTestEnabled();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** 启用模板测试。 */
    public void enableStencilTest() {
        this.event.saveStencilTestEnabled();
        GL11.glEnable(GL11.GL_STENCIL_TEST);
    }

    /** 禁用模板测试。 */
    public void disableStencilTest() {
        this.event.saveStencilTestEnabled();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    /** 启用法线归一化。 */
    public void enableNormalize() {
        this.event.saveNormalizeEnabled();
        GL11.glEnable(GL11.GL_NORMALIZE);
    }

    /** 禁用法线归一化。 */
    public void disableNormalize() {
        this.event.saveNormalizeEnabled();
        GL11.glDisable(GL11.GL_NORMALIZE);
    }

    /** 启用法线重缩放。 */
    public void enableRescaleNormal() {
        this.event.saveRescaleNormalEnabled();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
    }

    /** 禁用法线重缩放。 */
    public void disableRescaleNormal() {
        this.event.saveRescaleNormalEnabled();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    }

    /** 启用多边形偏移（填充模式）。 */
    public void enablePolygonOffsetFill() {
        this.event.savePolygonOffsetFillEnabled();
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
    }

    /** 禁用多边形偏移（填充模式）。 */
    public void disablePolygonOffsetFill() {
        this.event.savePolygonOffsetFillEnabled();
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
    }

    /** 启用多边形偏移（线框模式）。 */
    public void enablePolygonOffsetLine() {
        this.event.savePolygonOffsetLineEnabled();
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
    }

    /** 禁用多边形偏移（线框模式）。 */
    public void disablePolygonOffsetLine() {
        this.event.savePolygonOffsetLineEnabled();
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);
    }

    /** 启用线条点画（stipple）。 */
    public void enableLineStipple() {
        this.event.saveLineStippleEnabled();
        GL11.glEnable(GL11.GL_LINE_STIPPLE);
    }

    /** 禁用线条点画（stipple）。 */
    public void disableLineStipple() {
        this.event.saveLineStippleEnabled();
        GL11.glDisable(GL11.GL_LINE_STIPPLE);
    }

    /** 启用线条平滑。 */
    public void enableLineSmooth() {
        this.event.saveLineSmoothEnabled();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }

    /** 禁用线条平滑。 */
    public void disableLineSmooth() {
        this.event.saveLineSmoothEnabled();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    /** 启用多边形点画（stipple）。 */
    public void enablePolygonStipple() {
        this.event.savePolygonStippleEnabled();
        GL11.glEnable(GL11.GL_POLYGON_STIPPLE);
    }

    /** 禁用多边形点画（stipple）。 */
    public void disablePolygonStipple() {
        this.event.savePolygonStippleEnabled();
        GL11.glDisable(GL11.GL_POLYGON_STIPPLE);
    }

    /** 启用多边形平滑。 */
    public void enablePolygonSmooth() {
        this.event.savePolygonSmoothEnabled();
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
    }

    /** 禁用多边形平滑。 */
    public void disablePolygonSmooth() {
        this.event.savePolygonSmoothEnabled();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }

    /** 启用点平滑。 */
    public void enablePointSmooth() {
        this.event.savePointSmoothEnabled();
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
    }

    /** 禁用点平滑。 */
    public void disablePointSmooth() {
        this.event.savePointSmoothEnabled();
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
    }

    // GL_FOG_BIT

    /** 设置雾效模式。 */
    public void setFogMode(int mode) {
        this.event.saveFogMode();
        GL11.glFogi(GL11.GL_FOG_MODE, mode);
    }

    /** 设置雾效密度。 */
    public void setFogDensity(float density) {
        this.event.saveFogDensity();
        GL11.glFogf(GL11.GL_FOG_DENSITY, density);
    }

    /** 设置雾效起始距离。 */
    public void setFogStart(float start) {
        this.event.saveFogStart();
        GL11.glFogf(GL11.GL_FOG_START, start);
    }

    /** 设置雾效结束距离。 */
    public void setFogEnd(float end) {
        this.event.saveFogEnd();
        GL11.glFogf(GL11.GL_FOG_END, end);
    }

    /** 设置雾效颜色。 */
    public void setFogColor(float r, float g, float b, float a) {
        this.event.saveFogColor();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        buffer.put(new float[] { r, g, b, a });
        buffer.flip();
        GL11.glFog(GL11.GL_FOG_COLOR, buffer);
    }

    // GL_HINT_BIT

    /** 设置透视校正提示。 */
    public void setPerspectiveCorrectionHint(int hint) {
        this.event.savePerspectiveCorrectionHint();
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, hint);
    }

    /** 设置点平滑提示。 */
    public void setPointSmoothHint(int hint) {
        this.event.savePointSmoothHint();
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, hint);
    }

    /** 设置线平滑提示。 */
    public void setLineSmoothHint(int hint) {
        this.event.saveLineSmoothHint();
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, hint);
    }

    /** 设置多边形平滑提示。 */
    public void setPolygonSmoothHint(int hint) {
        this.event.savePolygonSmoothHint();
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, hint);
    }

    /** 设置雾效提示。 */
    public void setFogHint(int hint) {
        this.event.saveFogHint();
        GL11.glHint(GL11.GL_FOG_HINT, hint);
    }

    // GL_LIGHTING_BIT

    /** 启用颜色材质。 */
    public void enableColorMaterial() {
        this.event.saveColorMaterialEnabled();
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
    }

    /** 禁用颜色材质。 */
    public void disableColorMaterial() {
        this.event.saveColorMaterialEnabled();
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
    }

    /** 设置颜色材质。 */
    public void setColorMaterial(int face, int mode) {
        this.event.saveColorMaterialFace();
        this.event.saveColorMaterialMode();
        GL11.glColorMaterial(face, mode);
    }

    /** 设置着色模型。 */
    public void setShadeModel(int mode) {
        this.event.saveShadeModel();
        GL11.glShadeModel(mode);
    }

    /** 设置材质环境光。 */
    public void setMaterialAmbient(int face, float r, float g, float b, float a) {
        this.event.saveMaterialAmbient();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        buffer.put(new float[] { r, g, b, a });
        buffer.flip();
        GL11.glMaterial(face, GL11.GL_AMBIENT, buffer);
    }

    /** 设置材质漫反射。 */
    public void setMaterialDiffuse(int face, float r, float g, float b, float a) {
        this.event.saveMaterialDiffuse();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        buffer.put(new float[] { r, g, b, a });
        buffer.flip();
        GL11.glMaterial(face, GL11.GL_DIFFUSE, buffer);
    }

    /** 设置材质镜面反射。 */
    public void setMaterialSpecular(int face, float r, float g, float b, float a) {
        this.event.saveMaterialSpecular();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        buffer.put(new float[] { r, g, b, a });
        buffer.flip();
        GL11.glMaterial(face, GL11.GL_SPECULAR, buffer);
    }

    /** 设置材质自发光。 */
    public void setMaterialEmission(int face, float r, float g, float b, float a) {
        this.event.saveMaterialEmission();
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        buffer.put(new float[] { r, g, b, a });
        buffer.flip();
        GL11.glMaterial(face, GL11.GL_EMISSION, buffer);
    }

    /** 设置材质光泽度。 */
    public void setMaterialShininess(int face, float shininess) {
        this.event.saveMaterialShininess();
        GL11.glMaterialf(face, GL11.GL_SHININESS, shininess);
    }

    // GL_LINE_BIT

    /** 设置线宽。 */
    public void setLineWidth(float width) {
        this.event.saveLineWidth();
        GL11.glLineWidth(width);
    }

    // GL_POINT_BIT

    /** 设置点大小。 */
    public void setPointSize(float size) {
        this.event.savePointSize();
        GL11.glPointSize(size);
    }

    // GL_POLYGON_BIT

    /** 设置面剔除模式。 */
    public void setCullFaceMode(int mode) {
        this.event.saveCullFaceMode();
        GL11.glCullFace(mode);
    }

    /** 设置正面朝向。 */
    public void setFrontFace(int mode) {
        this.event.saveFrontFace();
        GL11.glFrontFace(mode);
    }

    /** 设置多边形模式。 */
    public void setPolygonMode(int face, int mode) {
        this.event.savePolygonMode();
        GL11.glPolygonMode(face, mode);
    }

    /** 设置多边形偏移。 */
    public void setPolygonOffset(float factor, float units) {
        this.event.savePolygonOffset();
        GL11.glPolygonOffset(factor, units);
    }

    // GL_SCISSOR_BIT

    /** 设置裁剪区域。 */
    public void setScissor(int x, int y, int width, int height) {
        this.event.saveScissorBox();
        GL11.glScissor(x, y, width, height);
    }

    // GL_STENCIL_BUFFER_BIT

    /** 设置模板测试函数。 */
    public void setStencilFunc(int func, int ref, int mask) {
        this.event.saveStencilFunc();
        GL11.glStencilFunc(func, ref, mask);
    }

    /** 设置模板操作。 */
    public void setStencilOp(int sfail, int dpfail, int dppass) {
        this.event.saveStencilOp();
        GL11.glStencilOp(sfail, dpfail, dppass);
    }

    /** 设置模板写入掩码。 */
    public void setStencilMask(int mask) {
        this.event.saveStencilMask();
        GL11.glStencilMask(mask);
    }

    /** 设置模板清屏值。 */
    public void setClearStencil(int s) {
        this.event.saveClearStencil();
        GL11.glClearStencil(s);
    }

    // GL_TEXTURE_BIT

    /** 设置活动纹理单元。 */
    public void setActiveTexture(int texture) {
        this.event.saveActiveTexture();
        GL13.glActiveTexture(texture);
    }

    /** 绑定纹理2D。 */
    public void bindTexture2D(int texture) {
        this.event.saveTexture2DBinding();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    /** 设置纹理环境模式。 */
    public void setTexEnvMode(int mode) {
        this.event.saveTexEnvMode();
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, mode);
    }

    // GL_VIEWPORT_BIT

    /** 设置视口。 */
    public void setViewport(int x, int y, int width, int height) {
        this.event.saveViewport();
        GL11.glViewport(x, y, width, height);
    }

    /** 设置深度范围。 */
    public void setDepthRange(double near, double far) {
        this.event.saveDepthRange();
        GL11.glDepthRange(near, far);
    }
}
