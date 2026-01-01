package moe.takochan.takorender.core.gl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * GL 状态追踪器工厂类
 *
 * <p>
 * 提供静态工厂方法为所有 GL 状态创建 StateTracker 实例。
 * 每个工厂方法捕获当前 GL 状态并返回一个能够恢复该状态的 StateTracker。
 * </p>
 *
 * <p>
 * <b>设计模式</b>: 静态工厂方法模式
 * </p>
 *
 * <p>
 * <b>职责</b>:
 * </p>
 * <ul>
 * <li>封装所有 GL 状态的查询逻辑（glGetInteger, glGetFloat, glIsEnabled 等）</li>
 * <li>创建匿名 StateTracker 实例，捕获当前状态值</li>
 * <li>实现 restore() 方法，调用相应的 GL 函数恢复状态</li>
 * </ul>
 *
 * <p>
 * <b>优势</b>:
 * </p>
 * <ul>
 * <li>集中管理：所有状态捕获逻辑集中在一个类中</li>
 * <li>易于维护：添加新状态只需添加一个工厂方法</li>
 * <li>类型安全：每个方法返回明确的 StateTracker 接口</li>
 * <li>懒加载：状态值仅在工厂方法调用时查询</li>
 * </ul>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * {@code
 * // RenderEvent 中使用工厂方法创建 tracker
 * void saveBlendEnabled() {
 *     StateTracker tracker = RenderEventStateTrackers.createBlendEnabledTracker();
 *     // tracker 已捕获当前 GL_BLEND 状态
 *     saveState(StateKey.BLEND_ENABLED, tracker);
 * }
 * }
 * </pre>
 *
 * <p>
 * <b>实现统计</b>: 67 个工厂方法，覆盖 OpenGL 1.3 固定管线所有状态
 * </p>
 *
 * <p>
 * <b>访问控制</b>: 包私有（package-private），不可实例化
 * </p>
 *
 * @see StateTracker
 * @see RenderEvent
 * @see StateKey
 */
@SideOnly(Side.CLIENT)
final class RenderEventStateTrackers {

    /**
     * 私有构造函数 - 防止实例化
     *
     * <p>
     * 此类仅包含静态工厂方法，不应被实例化。
     * </p>
     */
    private RenderEventStateTrackers() {
        // 防止实例化
    }

    // GL_COLOR_BUFFER_BIT

    static StateTracker createBlendEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_BLEND);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_BLEND);
                else GL11.glDisable(GL11.GL_BLEND);
            }
        };
    }

    static StateTracker createBlendFuncTracker() {
        return new StateTracker() {

            private final int srcFactor = GL11.glGetInteger(GL11.GL_BLEND_SRC);
            private final int dstFactor = GL11.glGetInteger(GL11.GL_BLEND_DST);

            @Override
            public void restore() {
                GL11.glBlendFunc(srcFactor, dstFactor);
            }
        };
    }

    static StateTracker createAlphaTestEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_ALPHA_TEST);
                else GL11.glDisable(GL11.GL_ALPHA_TEST);
            }
        };
    }

    static StateTracker createAlphaTestFuncTracker() {
        return new StateTracker() {

            private final int func = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
            private final float ref = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);

            @Override
            public void restore() {
                GL11.glAlphaFunc(func, ref);
            }
        };
    }

    static StateTracker createDitherEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_DITHER);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_DITHER);
                else GL11.glDisable(GL11.GL_DITHER);
            }
        };
    }

    static StateTracker createLogicOpEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
                else GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
            }
        };
    }

    static StateTracker createLogicOpModeTracker() {
        return new StateTracker() {

            private final int mode = GL11.glGetInteger(GL11.GL_LOGIC_OP_MODE);

            @Override
            public void restore() {
                GL11.glLogicOp(mode);
            }
        };
    }

    static StateTracker createColorMaskTracker() {
        return new StateTracker() {

            private final IntBuffer buffer = BufferUtils.createIntBuffer(4);
            private final boolean[] mask;

            {
                GL11.glGetInteger(GL11.GL_COLOR_WRITEMASK, buffer);
                mask = new boolean[] { buffer.get(0) != 0, buffer.get(1) != 0, buffer.get(2) != 0, buffer.get(3) != 0 };
            }

            @Override
            public void restore() {
                GL11.glColorMask(mask[0], mask[1], mask[2], mask[3]);
            }
        };
    }

    static StateTracker createClearColorTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
            private final float[] color;

            {
                GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, buffer);
                color = new float[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                GL11.glClearColor(color[0], color[1], color[2], color[3]);
            }
        };
    }

    // GL_CURRENT_BIT

    static StateTracker createColorTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
            private final float[] color;

            {
                GL11.glGetFloat(GL11.GL_CURRENT_COLOR, buffer);
                color = new float[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                GL11.glColor4f(color[0], color[1], color[2], color[3]);
            }
        };
    }

    static StateTracker createNormalTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(3);
            private final float[] normal;

            {
                GL11.glGetFloat(GL11.GL_CURRENT_NORMAL, buffer);
                normal = new float[] { buffer.get(0), buffer.get(1), buffer.get(2) };
            }

            @Override
            public void restore() {
                GL11.glNormal3f(normal[0], normal[1], normal[2]);
            }
        };
    }

    static StateTracker createTexCoordTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
            private final float[] texCoord;

            {
                GL11.glGetFloat(GL11.GL_CURRENT_TEXTURE_COORDS, buffer);
                texCoord = new float[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                GL11.glTexCoord4f(texCoord[0], texCoord[1], texCoord[2], texCoord[3]);
            }
        };
    }

    // GL_DEPTH_BUFFER_BIT

    static StateTracker createDepthTestEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_DEPTH_TEST);
                else GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
        };
    }

    static StateTracker createDepthMaskTracker() {
        return new StateTracker() {

            private final boolean mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

            @Override
            public void restore() {
                GL11.glDepthMask(mask);
            }
        };
    }

    static StateTracker createDepthFuncTracker() {
        return new StateTracker() {

            private final int func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

            @Override
            public void restore() {
                GL11.glDepthFunc(func);
            }
        };
    }

    static StateTracker createClearDepthTracker() {
        return new StateTracker() {

            private final double depth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);

            @Override
            public void restore() {
                GL11.glClearDepth(depth);
            }
        };
    }

    // GL_ENABLE_BIT

    static StateTracker createTexture2DEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_TEXTURE_2D);
                else GL11.glDisable(GL11.GL_TEXTURE_2D);
            }
        };
    }

    static StateTracker createLightingEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_LIGHTING);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_LIGHTING);
                else GL11.glDisable(GL11.GL_LIGHTING);
            }
        };
    }

    static StateTracker createCullFaceEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_CULL_FACE);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_CULL_FACE);
                else GL11.glDisable(GL11.GL_CULL_FACE);
            }
        };
    }

    static StateTracker createFogEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_FOG);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_FOG);
                else GL11.glDisable(GL11.GL_FOG);
            }
        };
    }

    static StateTracker createScissorTestEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_SCISSOR_TEST);
                else GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        };
    }

    static StateTracker createStencilTestEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_STENCIL_TEST);
                else GL11.glDisable(GL11.GL_STENCIL_TEST);
            }
        };
    }

    static StateTracker createNormalizeEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_NORMALIZE);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_NORMALIZE);
                else GL11.glDisable(GL11.GL_NORMALIZE);
            }
        };
    }

    static StateTracker createRescaleNormalEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                else GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            }
        };
    }

    static StateTracker createPolygonOffsetFillEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                else GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }
        };
    }

    static StateTracker createPolygonOffsetLineEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_LINE);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
                else GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);
            }
        };
    }

    static StateTracker createLineStippleEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_LINE_STIPPLE);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_LINE_STIPPLE);
                else GL11.glDisable(GL11.GL_LINE_STIPPLE);
            }
        };
    }

    static StateTracker createLineSmoothEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_LINE_SMOOTH);
                else GL11.glDisable(GL11.GL_LINE_SMOOTH);
            }
        };
    }

    static StateTracker createPolygonStippleEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_POLYGON_STIPPLE);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_POLYGON_STIPPLE);
                else GL11.glDisable(GL11.GL_POLYGON_STIPPLE);
            }
        };
    }

    static StateTracker createPolygonSmoothEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_POLYGON_SMOOTH);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
                else GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
            }
        };
    }

    static StateTracker createPointSmoothEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_POINT_SMOOTH);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_POINT_SMOOTH);
                else GL11.glDisable(GL11.GL_POINT_SMOOTH);
            }
        };
    }

    // GL_FOG_BIT

    static StateTracker createFogModeTracker() {
        return new StateTracker() {

            private final int mode = GL11.glGetInteger(GL11.GL_FOG_MODE);

            @Override
            public void restore() {
                GL11.glFogi(GL11.GL_FOG_MODE, mode);
            }
        };
    }

    static StateTracker createFogDensityTracker() {
        return new StateTracker() {

            private final float density = GL11.glGetFloat(GL11.GL_FOG_DENSITY);

            @Override
            public void restore() {
                GL11.glFogf(GL11.GL_FOG_DENSITY, density);
            }
        };
    }

    static StateTracker createFogStartTracker() {
        return new StateTracker() {

            private final float start = GL11.glGetFloat(GL11.GL_FOG_START);

            @Override
            public void restore() {
                GL11.glFogf(GL11.GL_FOG_START, start);
            }
        };
    }

    static StateTracker createFogEndTracker() {
        return new StateTracker() {

            private final float end = GL11.glGetFloat(GL11.GL_FOG_END);

            @Override
            public void restore() {
                GL11.glFogf(GL11.GL_FOG_END, end);
            }
        };
    }

    static StateTracker createFogColorTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
            private final float[] color;

            {
                GL11.glGetFloat(GL11.GL_FOG_COLOR, buffer);
                color = new float[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                FloatBuffer buf = BufferUtils.createFloatBuffer(4);
                buf.put(color);
                buf.flip();
                GL11.glFog(GL11.GL_FOG_COLOR, buf);
            }
        };
    }

    // GL_HINT_BIT

    static StateTracker createPerspectiveCorrectionHintTracker() {
        return new StateTracker() {

            private final int hint = GL11.glGetInteger(GL11.GL_PERSPECTIVE_CORRECTION_HINT);

            @Override
            public void restore() {
                GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, hint);
            }
        };
    }

    static StateTracker createPointSmoothHintTracker() {
        return new StateTracker() {

            private final int hint = GL11.glGetInteger(GL11.GL_POINT_SMOOTH_HINT);

            @Override
            public void restore() {
                GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, hint);
            }
        };
    }

    static StateTracker createLineSmoothHintTracker() {
        return new StateTracker() {

            private final int hint = GL11.glGetInteger(GL11.GL_LINE_SMOOTH_HINT);

            @Override
            public void restore() {
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, hint);
            }
        };
    }

    static StateTracker createPolygonSmoothHintTracker() {
        return new StateTracker() {

            private final int hint = GL11.glGetInteger(GL11.GL_POLYGON_SMOOTH_HINT);

            @Override
            public void restore() {
                GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, hint);
            }
        };
    }

    static StateTracker createFogHintTracker() {
        return new StateTracker() {

            private final int hint = GL11.glGetInteger(GL11.GL_FOG_HINT);

            @Override
            public void restore() {
                GL11.glHint(GL11.GL_FOG_HINT, hint);
            }
        };
    }

    // GL_LIGHTING_BIT

    static StateTracker createColorMaterialEnabledTracker() {
        return new StateTracker() {

            private final boolean original = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);

            @Override
            public void restore() {
                if (original) GL11.glEnable(GL11.GL_COLOR_MATERIAL);
                else GL11.glDisable(GL11.GL_COLOR_MATERIAL);
            }
        };
    }

    static StateTracker createColorMaterialFaceTracker() {
        return new StateTracker() {

            private final int face = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_FACE);
            private final int mode = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_PARAMETER);

            @Override
            public void restore() {
                GL11.glColorMaterial(face, mode);
            }
        };
    }

    static StateTracker createColorMaterialModeTracker() {
        return new StateTracker() {

            private final int mode = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_PARAMETER);
            private final int face = GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_FACE);

            @Override
            public void restore() {
                GL11.glColorMaterial(face, mode);
            }
        };
    }

    static StateTracker createShadeModelTracker() {
        return new StateTracker() {

            private final int model = GL11.glGetInteger(GL11.GL_SHADE_MODEL);

            @Override
            public void restore() {
                GL11.glShadeModel(model);
            }
        };
    }

    static StateTracker createMaterialAmbientTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
            private final float[] color;
            private final int face = GL11.GL_FRONT_AND_BACK;

            {
                GL11.glGetMaterial(face, GL11.GL_AMBIENT, buffer);
                color = new float[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                FloatBuffer buf = BufferUtils.createFloatBuffer(4);
                buf.put(color);
                buf.flip();
                GL11.glMaterial(face, GL11.GL_AMBIENT, buf);
            }
        };
    }

    static StateTracker createMaterialDiffuseTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
            private final float[] color;
            private final int face = GL11.GL_FRONT_AND_BACK;

            {
                GL11.glGetMaterial(face, GL11.GL_DIFFUSE, buffer);
                color = new float[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                FloatBuffer buf = BufferUtils.createFloatBuffer(4);
                buf.put(color);
                buf.flip();
                GL11.glMaterial(face, GL11.GL_DIFFUSE, buf);
            }
        };
    }

    static StateTracker createMaterialSpecularTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
            private final float[] color;
            private final int face = GL11.GL_FRONT_AND_BACK;

            {
                GL11.glGetMaterial(face, GL11.GL_SPECULAR, buffer);
                color = new float[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                FloatBuffer buf = BufferUtils.createFloatBuffer(4);
                buf.put(color);
                buf.flip();
                GL11.glMaterial(face, GL11.GL_SPECULAR, buf);
            }
        };
    }

    static StateTracker createMaterialEmissionTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
            private final float[] color;
            private final int face = GL11.GL_FRONT_AND_BACK;

            {
                GL11.glGetMaterial(face, GL11.GL_EMISSION, buffer);
                color = new float[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                FloatBuffer buf = BufferUtils.createFloatBuffer(4);
                buf.put(color);
                buf.flip();
                GL11.glMaterial(face, GL11.GL_EMISSION, buf);
            }
        };
    }

    static StateTracker createMaterialShininessTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(1);
            private final float shininess;

            {
                GL11.glGetMaterial(GL11.GL_FRONT, GL11.GL_SHININESS, buffer);
                shininess = buffer.get(0);
            }

            @Override
            public void restore() {
                GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, shininess);
            }
        };
    }

    // GL_LINE_BIT

    static StateTracker createLineWidthTracker() {
        return new StateTracker() {

            private final float width = GL11.glGetFloat(GL11.GL_LINE_WIDTH);

            @Override
            public void restore() {
                GL11.glLineWidth(width);
            }
        };
    }

    // GL_POINT_BIT

    static StateTracker createPointSizeTracker() {
        return new StateTracker() {

            private final float size = GL11.glGetFloat(GL11.GL_POINT_SIZE);

            @Override
            public void restore() {
                GL11.glPointSize(size);
            }
        };
    }

    // GL_POLYGON_BIT

    static StateTracker createCullFaceModeTracker() {
        return new StateTracker() {

            private final int mode = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);

            @Override
            public void restore() {
                GL11.glCullFace(mode);
            }
        };
    }

    static StateTracker createFrontFaceTracker() {
        return new StateTracker() {

            private final int mode = GL11.glGetInteger(GL11.GL_FRONT_FACE);

            @Override
            public void restore() {
                GL11.glFrontFace(mode);
            }
        };
    }

    static StateTracker createPolygonModeTracker() {
        return new StateTracker() {

            private final IntBuffer buffer = BufferUtils.createIntBuffer(2);
            private final int frontMode;
            private final int backMode;

            {
                GL11.glGetInteger(GL11.GL_POLYGON_MODE, buffer);
                frontMode = buffer.get(0);
                backMode = buffer.get(1);
            }

            @Override
            public void restore() {
                GL11.glPolygonMode(GL11.GL_FRONT, frontMode);
                GL11.glPolygonMode(GL11.GL_BACK, backMode);
            }
        };
    }

    static StateTracker createPolygonOffsetTracker() {
        return new StateTracker() {

            private final float factor = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR);
            private final float units = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);

            @Override
            public void restore() {
                GL11.glPolygonOffset(factor, units);
            }
        };
    }

    // GL_SCISSOR_BIT

    static StateTracker createScissorBoxTracker() {
        return new StateTracker() {

            private final IntBuffer buffer = BufferUtils.createIntBuffer(4);
            private final int[] box;

            {
                GL11.glGetInteger(GL11.GL_SCISSOR_BOX, buffer);
                box = new int[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                GL11.glScissor(box[0], box[1], box[2], box[3]);
            }
        };
    }

    // GL_STENCIL_BUFFER_BIT

    static StateTracker createStencilFuncTracker() {
        return new StateTracker() {

            private final int func = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
            private final int ref = GL11.glGetInteger(GL11.GL_STENCIL_REF);
            private final int mask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);

            @Override
            public void restore() {
                GL11.glStencilFunc(func, ref, mask);
            }
        };
    }

    static StateTracker createStencilOpTracker() {
        return new StateTracker() {

            private final int sfail = GL11.glGetInteger(GL11.GL_STENCIL_FAIL);
            private final int dpfail = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL);
            private final int dppass = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS);

            @Override
            public void restore() {
                GL11.glStencilOp(sfail, dpfail, dppass);
            }
        };
    }

    static StateTracker createStencilMaskTracker() {
        return new StateTracker() {

            private final int mask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);

            @Override
            public void restore() {
                GL11.glStencilMask(mask);
            }
        };
    }

    static StateTracker createClearStencilTracker() {
        return new StateTracker() {

            private final int s = GL11.glGetInteger(GL11.GL_STENCIL_CLEAR_VALUE);

            @Override
            public void restore() {
                GL11.glClearStencil(s);
            }
        };
    }

    // GL_TEXTURE_BIT

    static StateTracker createActiveTextureTracker() {
        return new StateTracker() {

            private final int texture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

            @Override
            public void restore() {
                GL13.glActiveTexture(texture);
            }
        };
    }

    static StateTracker createTexture2DBindingTracker() {
        return new StateTracker() {

            private final int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

            @Override
            public void restore() {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
            }
        };
    }

    static StateTracker createTexEnvModeTracker() {
        return new StateTracker() {

            private final int mode = GL11.glGetTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE);

            @Override
            public void restore() {
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, mode);
            }
        };
    }

    // GL_VIEWPORT_BIT

    static StateTracker createViewportTracker() {
        return new StateTracker() {

            private final IntBuffer buffer = BufferUtils.createIntBuffer(4);
            private final int[] viewport;

            {
                GL11.glGetInteger(GL11.GL_VIEWPORT, buffer);
                viewport = new int[] { buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3) };
            }

            @Override
            public void restore() {
                GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            }
        };
    }

    static StateTracker createDepthRangeTracker() {
        return new StateTracker() {

            private final FloatBuffer buffer = BufferUtils.createFloatBuffer(2);
            private final double near;
            private final double far;

            {
                GL11.glGetFloat(GL11.GL_DEPTH_RANGE, buffer);
                near = buffer.get(0);
                far = buffer.get(1);
            }

            @Override
            public void restore() {
                GL11.glDepthRange(near, far);
            }
        };
    }
}
