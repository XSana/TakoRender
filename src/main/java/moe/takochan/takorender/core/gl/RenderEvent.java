package moe.takochan.takorender.core.gl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 渲染事件 - 追踪单次渲染作用域内的 GL 状态修改
 *
 * <p>
 * 使用 HashMap 去重机制确保每个状态在首次修改时只保存一次。
 * 按 LIFO（后进先出）顺序恢复状态，与修改顺序相反。
 * </p>
 *
 * <p>
 * <b>核心机制</b>:
 * </p>
 * <ol>
 * <li><b>去重保存</b>: 通过 HashMap.containsKey() 检查，避免重复保存同一状态</li>
 * <li><b>顺序记录</b>: List 按修改顺序记录 StateKey，用于 LIFO 恢复</li>
 * <li><b>懒加载</b>: 状态值仅在首次修改时从 OpenGL 查询，未修改的状态不查询</li>
 * <li><b>工厂模式</b>: 使用 RenderEventStateTrackers 创建 StateTracker 实例</li>
 * </ol>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     // 创建渲染事件
 *     RenderEvent event = new RenderEvent();
 *
 *     // 第一次调用 saveBlendEnabled() - 保存状态
 *     event.saveBlendEnabled(); // 查询 GL，保存 original=false
 *     GL11.glEnable(GL11.GL_BLEND);
 *
 *     // 第二次调用 saveBlendEnabled() - 跳过（已保存）
 *     event.saveBlendEnabled(); // containsKey() 返回 true，不执行
 *
 *     // 恢复所有状态（LIFO 顺序）
 *     event.restoreStates(); // GL11.glDisable(GL11.GL_BLEND)
 * }
 * </pre>
 *
 * <p>
 * <b>线程安全</b>: 此类不是线程安全的。由于 Minecraft 客户端渲染都在主线程执行，
 * GLStateContext 使用简单的静态栈管理实例，无需 ThreadLocal。
 * </p>
 *
 * @see GLStateContext
 * @see RenderEventStateTrackers
 * @see StateTracker
 * @see StateKey
 */
@SideOnly(Side.CLIENT)
class RenderEvent {

    /** 状态追踪器映射 - 防止重复保存同一状态 */
    private final Map<StateKey, StateTracker> savedStates = new HashMap<>();
    /** 修改顺序列表 - 用于 LIFO 恢复 */
    private final List<StateKey> modifiedOrder = new ArrayList<>();

    /**
     * 按 LIFO 顺序恢复所有修改过的状态
     *
     * <p>
     * 反向遍历 modifiedOrder 列表，按修改的逆序恢复状态（类似栈的 LIFO 行为）。
     * 这确保了嵌套状态修改能够正确恢复。
     * </p>
     *
     * <p>
     * <b>执行顺序</b>:
     * </p>
     *
     * <pre>
     * {@code
     * // 假设修改顺序: blend -> depth -> alpha
     * modifiedOrder = [BLEND_ENABLED, DEPTH_TEST_ENABLED, ALPHA_TEST_ENABLED]
     *
     * // 恢复顺序: alpha -> depth -> blend (LIFO)
     * restoreStates() {
     *     restore ALPHA_TEST_ENABLED   // i=2
     *     restore DEPTH_TEST_ENABLED   // i=1
     *     restore BLEND_ENABLED        // i=0
     * }
     * }
     * </pre>
     */
    void restoreStates() {
        // 反向遍历列表，按修改的逆序恢复（类似栈的 LIFO）
        for (int i = modifiedOrder.size() - 1; i >= 0; i--) {
            StateKey key = modifiedOrder.get(i);
            StateTracker tracker = savedStates.get(key);
            if (tracker != null) {
                tracker.restore();
            }
        }
    }

    /**
     * 保存状态（如果尚未保存）
     *
     * <p>
     * 使用 HashMap 去重机制，确保每个状态只保存一次原始值。
     * 首次调用时，将 StateTracker 存入 HashMap 并记录到 List。
     * 后续调用同一 StateKey 时，containsKey() 返回 true，直接跳过。
     * </p>
     *
     * <p>
     * <b>去重示例</b>:
     * </p>
     *
     * <pre>
     * {@code
     * // 第一次调用
     * saveState(BLEND_ENABLED, tracker1);
     * // savedStates: {BLEND_ENABLED -> tracker1}
     * // modifiedOrder: [BLEND_ENABLED]
     *
     * // 第二次调用（跳过）
     * saveState(BLEND_ENABLED, tracker2);
     * // containsKey() 返回 true，不执行 put/add
     * // savedStates 和 modifiedOrder 保持不变
     * }
     * </pre>
     *
     * @param key     状态键，用于标识状态类型
     * @param tracker 状态追踪器，包含保存的原始值和恢复逻辑
     */
    private void saveState(StateKey key, StateTracker tracker) {
        if (!savedStates.containsKey(key)) {
            savedStates.put(key, tracker);
            modifiedOrder.add(key);
        }
    }

    // GL_COLOR_BUFFER_BIT

    /**
     * 保存混合开关状态
     *
     * <p>
     * 调用 RenderEventStateTrackers 工厂方法创建 StateTracker，
     * 捕获当前 GL_BLEND 状态并在恢复时还原。
     * </p>
     *
     * @see StateKey#BLEND_ENABLED
     */
    void saveBlendEnabled() {
        saveState(StateKey.BLEND_ENABLED, RenderEventStateTrackers.createBlendEnabledTracker());
    }

    /**
     * 保存混合函数参数
     *
     * @see StateKey#BLEND_FUNC
     */
    void saveBlendFunc() {
        saveState(StateKey.BLEND_FUNC, RenderEventStateTrackers.createBlendFuncTracker());
    }

    void saveAlphaTestEnabled() {
        saveState(StateKey.ALPHA_TEST_ENABLED, RenderEventStateTrackers.createAlphaTestEnabledTracker());
    }

    void saveAlphaTestFunc() {
        saveState(StateKey.ALPHA_TEST_FUNC, RenderEventStateTrackers.createAlphaTestFuncTracker());
    }

    void saveDitherEnabled() {
        saveState(StateKey.DITHER_ENABLED, RenderEventStateTrackers.createDitherEnabledTracker());
    }

    void saveLogicOpEnabled() {
        saveState(StateKey.LOGIC_OP_ENABLED, RenderEventStateTrackers.createLogicOpEnabledTracker());
    }

    void saveLogicOpMode() {
        saveState(StateKey.LOGIC_OP_MODE, RenderEventStateTrackers.createLogicOpModeTracker());
    }

    void saveColorMask() {
        saveState(StateKey.COLOR_MASK, RenderEventStateTrackers.createColorMaskTracker());
    }

    void saveClearColor() {
        saveState(StateKey.CLEAR_COLOR, RenderEventStateTrackers.createClearColorTracker());
    }

    // GL_CURRENT_BIT

    void saveColor() {
        saveState(StateKey.CURRENT_COLOR, RenderEventStateTrackers.createColorTracker());
    }

    void saveNormal() {
        saveState(StateKey.CURRENT_NORMAL, RenderEventStateTrackers.createNormalTracker());
    }

    void saveTexCoord() {
        saveState(StateKey.CURRENT_TEXCOORD, RenderEventStateTrackers.createTexCoordTracker());
    }

    // GL_DEPTH_BUFFER_BIT

    void saveDepthTestEnabled() {
        saveState(StateKey.DEPTH_TEST_ENABLED, RenderEventStateTrackers.createDepthTestEnabledTracker());
    }

    void saveDepthMask() {
        saveState(StateKey.DEPTH_MASK, RenderEventStateTrackers.createDepthMaskTracker());
    }

    void saveDepthFunc() {
        saveState(StateKey.DEPTH_FUNC, RenderEventStateTrackers.createDepthFuncTracker());
    }

    void saveClearDepth() {
        saveState(StateKey.CLEAR_DEPTH, RenderEventStateTrackers.createClearDepthTracker());
    }

    // GL_ENABLE_BIT

    void saveTexture2DEnabled() {
        saveState(StateKey.TEXTURE_2D_ENABLED, RenderEventStateTrackers.createTexture2DEnabledTracker());
    }

    void saveLightingEnabled() {
        saveState(StateKey.LIGHTING_ENABLED, RenderEventStateTrackers.createLightingEnabledTracker());
    }

    void saveCullFaceEnabled() {
        saveState(StateKey.CULL_FACE_ENABLED, RenderEventStateTrackers.createCullFaceEnabledTracker());
    }

    void saveFogEnabled() {
        saveState(StateKey.FOG_ENABLED, RenderEventStateTrackers.createFogEnabledTracker());
    }

    void saveScissorTestEnabled() {
        saveState(StateKey.SCISSOR_TEST_ENABLED, RenderEventStateTrackers.createScissorTestEnabledTracker());
    }

    void saveStencilTestEnabled() {
        saveState(StateKey.STENCIL_TEST_ENABLED, RenderEventStateTrackers.createStencilTestEnabledTracker());
    }

    void saveNormalizeEnabled() {
        saveState(StateKey.NORMALIZE_ENABLED, RenderEventStateTrackers.createNormalizeEnabledTracker());
    }

    void saveRescaleNormalEnabled() {
        saveState(StateKey.RESCALE_NORMAL_ENABLED, RenderEventStateTrackers.createRescaleNormalEnabledTracker());
    }

    void savePolygonOffsetFillEnabled() {
        saveState(
            StateKey.POLYGON_OFFSET_FILL_ENABLED,
            RenderEventStateTrackers.createPolygonOffsetFillEnabledTracker());
    }

    void savePolygonOffsetLineEnabled() {
        saveState(
            StateKey.POLYGON_OFFSET_LINE_ENABLED,
            RenderEventStateTrackers.createPolygonOffsetLineEnabledTracker());
    }

    void saveLineStippleEnabled() {
        saveState(StateKey.LINE_STIPPLE_ENABLED, RenderEventStateTrackers.createLineStippleEnabledTracker());
    }

    void saveLineSmoothEnabled() {
        saveState(StateKey.LINE_SMOOTH_ENABLED, RenderEventStateTrackers.createLineSmoothEnabledTracker());
    }

    void savePolygonStippleEnabled() {
        saveState(StateKey.POLYGON_STIPPLE_ENABLED, RenderEventStateTrackers.createPolygonStippleEnabledTracker());
    }

    void savePolygonSmoothEnabled() {
        saveState(StateKey.POLYGON_SMOOTH_ENABLED, RenderEventStateTrackers.createPolygonSmoothEnabledTracker());
    }

    void savePointSmoothEnabled() {
        saveState(StateKey.POINT_SMOOTH_ENABLED, RenderEventStateTrackers.createPointSmoothEnabledTracker());
    }

    // GL_FOG_BIT

    void saveFogMode() {
        saveState(StateKey.FOG_MODE, RenderEventStateTrackers.createFogModeTracker());
    }

    void saveFogDensity() {
        saveState(StateKey.FOG_DENSITY, RenderEventStateTrackers.createFogDensityTracker());
    }

    void saveFogStart() {
        saveState(StateKey.FOG_START, RenderEventStateTrackers.createFogStartTracker());
    }

    void saveFogEnd() {
        saveState(StateKey.FOG_END, RenderEventStateTrackers.createFogEndTracker());
    }

    void saveFogColor() {
        saveState(StateKey.FOG_COLOR, RenderEventStateTrackers.createFogColorTracker());
    }

    // GL_HINT_BIT

    void savePerspectiveCorrectionHint() {
        saveState(
            StateKey.PERSPECTIVE_CORRECTION_HINT,
            RenderEventStateTrackers.createPerspectiveCorrectionHintTracker());
    }

    void savePointSmoothHint() {
        saveState(StateKey.POINT_SMOOTH_HINT, RenderEventStateTrackers.createPointSmoothHintTracker());
    }

    void saveLineSmoothHint() {
        saveState(StateKey.LINE_SMOOTH_HINT, RenderEventStateTrackers.createLineSmoothHintTracker());
    }

    void savePolygonSmoothHint() {
        saveState(StateKey.POLYGON_SMOOTH_HINT, RenderEventStateTrackers.createPolygonSmoothHintTracker());
    }

    void saveFogHint() {
        saveState(StateKey.FOG_HINT, RenderEventStateTrackers.createFogHintTracker());
    }

    // GL_LIGHTING_BIT

    void saveColorMaterialEnabled() {
        saveState(StateKey.COLOR_MATERIAL_ENABLED, RenderEventStateTrackers.createColorMaterialEnabledTracker());
    }

    void saveColorMaterialFace() {
        saveState(StateKey.COLOR_MATERIAL_FACE, RenderEventStateTrackers.createColorMaterialFaceTracker());
    }

    void saveColorMaterialMode() {
        saveState(StateKey.COLOR_MATERIAL_MODE, RenderEventStateTrackers.createColorMaterialModeTracker());
    }

    void saveShadeModel() {
        saveState(StateKey.SHADE_MODEL, RenderEventStateTrackers.createShadeModelTracker());
    }

    void saveMaterialAmbient() {
        saveState(StateKey.MATERIAL_AMBIENT, RenderEventStateTrackers.createMaterialAmbientTracker());
    }

    void saveMaterialDiffuse() {
        saveState(StateKey.MATERIAL_DIFFUSE, RenderEventStateTrackers.createMaterialDiffuseTracker());
    }

    void saveMaterialSpecular() {
        saveState(StateKey.MATERIAL_SPECULAR, RenderEventStateTrackers.createMaterialSpecularTracker());
    }

    void saveMaterialEmission() {
        saveState(StateKey.MATERIAL_EMISSION, RenderEventStateTrackers.createMaterialEmissionTracker());
    }

    void saveMaterialShininess() {
        saveState(StateKey.MATERIAL_SHININESS, RenderEventStateTrackers.createMaterialShininessTracker());
    }

    // GL_LINE_BIT

    void saveLineWidth() {
        saveState(StateKey.LINE_WIDTH, RenderEventStateTrackers.createLineWidthTracker());
    }

    // GL_POINT_BIT

    void savePointSize() {
        saveState(StateKey.POINT_SIZE, RenderEventStateTrackers.createPointSizeTracker());
    }

    // GL_POLYGON_BIT

    void saveCullFaceMode() {
        saveState(StateKey.CULL_FACE_MODE, RenderEventStateTrackers.createCullFaceModeTracker());
    }

    void saveFrontFace() {
        saveState(StateKey.FRONT_FACE, RenderEventStateTrackers.createFrontFaceTracker());
    }

    void savePolygonMode() {
        saveState(StateKey.POLYGON_MODE, RenderEventStateTrackers.createPolygonModeTracker());
    }

    void savePolygonOffset() {
        saveState(StateKey.POLYGON_OFFSET, RenderEventStateTrackers.createPolygonOffsetTracker());
    }

    // GL_SCISSOR_BIT

    void saveScissorBox() {
        saveState(StateKey.SCISSOR_BOX, RenderEventStateTrackers.createScissorBoxTracker());
    }

    // GL_STENCIL_BUFFER_BIT

    void saveStencilFunc() {
        saveState(StateKey.STENCIL_FUNC, RenderEventStateTrackers.createStencilFuncTracker());
    }

    void saveStencilOp() {
        saveState(StateKey.STENCIL_OP, RenderEventStateTrackers.createStencilOpTracker());
    }

    void saveStencilMask() {
        saveState(StateKey.STENCIL_MASK, RenderEventStateTrackers.createStencilMaskTracker());
    }

    void saveClearStencil() {
        saveState(StateKey.CLEAR_STENCIL, RenderEventStateTrackers.createClearStencilTracker());
    }

    // GL_TEXTURE_BIT

    void saveActiveTexture() {
        saveState(StateKey.ACTIVE_TEXTURE, RenderEventStateTrackers.createActiveTextureTracker());
    }

    void saveTexture2DBinding() {
        saveState(StateKey.TEXTURE_2D_BINDING, RenderEventStateTrackers.createTexture2DBindingTracker());
    }

    void saveTexEnvMode() {
        saveState(StateKey.TEX_ENV_MODE, RenderEventStateTrackers.createTexEnvModeTracker());
    }

    // GL_VIEWPORT_BIT

    void saveViewport() {
        saveState(StateKey.VIEWPORT, RenderEventStateTrackers.createViewportTracker());
    }

    void saveDepthRange() {
        saveState(StateKey.DEPTH_RANGE, RenderEventStateTrackers.createDepthRangeTracker());
    }
}
