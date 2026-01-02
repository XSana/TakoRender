package moe.takochan.takorender.api.system;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.PostProcessComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.api.graphics.shader.ShaderType;
import moe.takochan.takorender.core.gl.FrameBuffer;
import moe.takochan.takorender.core.gl.GLStateContext;

/**
 * 后处理系统 - 执行后处理效果（Bloom、HDR、色调映射）
 *
 * <p>
 * PostProcessSystem 在 RENDER 阶段以最高优先级执行，负责：
 * </p>
 * <ol>
 * <li>提供场景捕获 FBO（供 MeshRenderSystem 渲染到）</li>
 * <li>执行亮度提取（Bloom 阈值）</li>
 * <li>执行高斯模糊（ping-pong）</li>
 * <li>合成最终画面（加法混合 + Alpha 混合到 MC 世界）</li>
 * </ol>
 *
 * <p>
 * <b>渲染流程</b>:
 * </p>
 *
 * <pre>
 * MeshRenderSystem:
 *   postProcess.beginCapture();
 *   // 渲染场景...
 *   postProcess.endCapture();
 *
 * PostProcessSystem.update():
 *   extractBrightness(sceneFbo -> brightFbo)
 *   gaussianBlur(brightFbo -> blurFbo1/2)
 *   composite(sceneFbo + blurFbo -> screen)
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class PostProcessSystem extends GameSystem {

    /** 场景渲染目标（全分辨率） */
    private FrameBuffer sceneFbo;

    /** 亮部提取目标（1/2 分辨率） */
    private FrameBuffer brightFbo;

    /** 模糊 ping-pong 目标 1（1/2 分辨率） */
    private FrameBuffer blurFbo1;

    /** 模糊 ping-pong 目标 2（1/2 分辨率） */
    private FrameBuffer blurFbo2;

    /** 全屏四边形 VAO/VBO/EBO */
    private int quadVao;
    private int quadVbo;
    private int quadEbo;

    /** 当前窗口尺寸 */
    private int currentWidth = 0;
    private int currentHeight = 0;

    /** 是否已初始化 */
    private boolean initialized = false;

    /** 是否启用后处理 */
    private boolean enabled = false;

    /** 是否正在捕获场景 */
    private boolean capturing = false;

    /** 保存的 MC FBO ID */
    private int savedMcFbo = 0;

    /** 保存的 MC 视口 */
    private final IntBuffer savedMcViewport = BufferUtils.createIntBuffer(16);

    @Override
    public Phase getPhase() {
        return Phase.RENDER;
    }

    @Override
    public int getPriority() {
        // 最高优先级（最后执行），在所有渲染完成后处理
        return 1000;
    }

    @Override
    public void onInit() {
        // 延迟初始化（需要等待 OpenGL 上下文可用）
    }

    @Override
    public void onDestroy() {
        cleanup();
    }

    /**
     * 初始化后处理系统
     *
     * @param width  宽度
     * @param height 高度
     * @return 是否初始化成功
     */
    public boolean initialize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return false;
        }

        try {
            currentWidth = width;
            currentHeight = height;

            // 创建 FBO
            sceneFbo = new FrameBuffer(width, height, true);
            brightFbo = new FrameBuffer(width / 2, height / 2, false);
            blurFbo1 = new FrameBuffer(width / 2, height / 2, false);
            blurFbo2 = new FrameBuffer(width / 2, height / 2, false);

            // 创建全屏四边形
            createFullscreenQuad();

            initialized = true;
            return true;
        } catch (Exception e) {
            cleanup();
            return false;
        }
    }

    /**
     * 清理所有资源
     */
    public void cleanup() {
        if (sceneFbo != null) {
            sceneFbo.close();
            sceneFbo = null;
        }
        if (brightFbo != null) {
            brightFbo.close();
            brightFbo = null;
        }
        if (blurFbo1 != null) {
            blurFbo1.close();
            blurFbo1 = null;
        }
        if (blurFbo2 != null) {
            blurFbo2.close();
            blurFbo2 = null;
        }

        if (quadVao != 0) {
            GL30.glDeleteVertexArrays(quadVao);
            quadVao = 0;
        }
        if (quadVbo != 0) {
            GL15.glDeleteBuffers(quadVbo);
            quadVbo = 0;
        }
        if (quadEbo != 0) {
            GL15.glDeleteBuffers(quadEbo);
            quadEbo = 0;
        }

        initialized = false;
    }

    /**
     * 检查并调整 FBO 尺寸
     */
    private void checkAndResizeFbos(int width, int height) {
        if (width != currentWidth || height != currentHeight) {
            currentWidth = width;
            currentHeight = height;

            if (sceneFbo != null) {
                sceneFbo.resize(width, height);
            }
            if (brightFbo != null) {
                brightFbo.resize(width / 2, height / 2);
            }
            if (blurFbo1 != null) {
                blurFbo1.resize(width / 2, height / 2);
            }
            if (blurFbo2 != null) {
                blurFbo2.resize(width / 2, height / 2);
            }
        }
    }

    /**
     * 开始捕获场景
     *
     * <p>
     * 绑定 sceneFbo 作为渲染目标，后续渲染将输出到此 FBO。
     * </p>
     */
    public void beginCapture() {
        if (!initialized || capturing) {
            return;
        }

        // 保存 MC 当前 FBO
        savedMcFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        savedMcViewport.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, savedMcViewport);

        // 绑定场景 FBO
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, sceneFbo.getFboId());
        GL11.glViewport(0, 0, sceneFbo.getWidth(), sceneFbo.getHeight());

        // 清屏（透明背景，用于 Overlay 模式）
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        capturing = true;
    }

    /**
     * 结束捕获场景
     *
     * <p>
     * 恢复 MC 原来的 FBO 绑定。
     * </p>
     */
    public void endCapture() {
        if (!capturing) {
            return;
        }

        // 恢复 MC FBO
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedMcFbo);
        GL11.glViewport(savedMcViewport.get(0), savedMcViewport.get(1), savedMcViewport.get(2), savedMcViewport.get(3));

        capturing = false;
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized || !enabled) {
            return;
        }

        // 查找活动相机的 PostProcessComponent
        Entity cameraEntity = findActiveCameraWithPostProcess();
        if (cameraEntity == null) {
            return;
        }

        PostProcessComponent postProcess = cameraEntity.getComponent(PostProcessComponent.class)
            .orElse(null);
        if (postProcess == null || !postProcess.hasAnyEffectEnabled()) {
            return;
        }

        // 检查尺寸变化
        int displayWidth = savedMcViewport.get(2);
        int displayHeight = savedMcViewport.get(3);
        if (displayWidth > 0 && displayHeight > 0) {
            checkAndResizeFbos(displayWidth, displayHeight);
        }

        // 执行后处理管线
        process(postProcess);
    }

    /**
     * 执行后处理管线
     */
    private void process(PostProcessComponent config) {
        try (GLStateContext ctx = GLStateContext.begin()) {
            ctx.disableDepthTest();
            ctx.enableBlend();
            ctx.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // 1. 亮度提取
            if (config.isBloomEnabled()) {
                extractBrightness(config);
            }

            // 2. 高斯模糊
            if (config.isBloomEnabled()) {
                gaussianBlur(config);
            }

            // 3. 合成输出
            composite(config);
        }
    }

    /**
     * 亮度提取（sceneFbo -> brightFbo）
     */
    private void extractBrightness(PostProcessComponent config) {
        ShaderProgram shader = ShaderType.POSTPROCESS_BRIGHTNESS.getOrNull();
        if (shader == null || !shader.isValid()) {
            return;
        }

        brightFbo.bind();
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        shader.use();
        shader.setUniformInt("uSceneTexture", 0);
        shader.setUniformFloat("uThreshold", config.getBloomThreshold());
        shader.setUniformFloat("uSoftKnee", config.getBloomSoftKnee());

        sceneFbo.bindTexture(0);
        drawFullscreenQuad();
        sceneFbo.unbindTexture();

        ShaderProgram.unbind();
        brightFbo.unbind();
    }

    /**
     * 高斯模糊 ping-pong（brightFbo -> blurFbo1/2）
     */
    private void gaussianBlur(PostProcessComponent config) {
        ShaderProgram shader = ShaderType.POSTPROCESS_BLUR.getOrNull();
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.use();
        shader.setUniformInt("uSourceTexture", 0);
        shader.setUniformFloat("uBlurScale", config.getBlurScale());

        float texelX = 1.0f / brightFbo.getWidth();
        float texelY = 1.0f / brightFbo.getHeight();
        shader.setUniformVec2("uTexelSize", texelX, texelY);

        boolean horizontal = true;
        FrameBuffer readFbo = brightFbo;
        FrameBuffer writeFbo = blurFbo1;

        for (int i = 0; i < config.getBlurIterations() * 2; i++) {
            writeFbo.bind();
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            shader.setUniformInt("uHorizontal", horizontal ? 1 : 0);

            readFbo.bindTexture(0);
            drawFullscreenQuad();
            readFbo.unbindTexture();

            writeFbo.unbind();

            // 交换 ping-pong
            horizontal = !horizontal;
            if (horizontal) {
                readFbo = blurFbo2;
                writeFbo = blurFbo1;
            } else {
                readFbo = blurFbo1;
                writeFbo = blurFbo2;
            }
        }

        ShaderProgram.unbind();
    }

    /**
     * 合成输出（sceneFbo + blurFbo -> screen）
     */
    private void composite(PostProcessComponent config) {
        ShaderProgram shader = ShaderType.POSTPROCESS_COMPOSITE.getOrNull();
        if (shader == null || !shader.isValid()) {
            return;
        }

        // 绑定到 MC 的 FBO（不是 0，而是保存的 FBO）
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedMcFbo);
        GL11.glViewport(savedMcViewport.get(0), savedMcViewport.get(1), savedMcViewport.get(2), savedMcViewport.get(3));

        // 不清屏，叠加到 MC 世界
        shader.use();
        shader.setUniformInt("uSceneTexture", 0);
        shader.setUniformInt("uBloomTexture", 1);
        shader.setUniformFloat("uBloomIntensity", config.isBloomEnabled() ? config.getBloomIntensity() : 0.0f);
        shader.setUniformFloat("uExposure", config.getExposure());
        shader.setUniformInt("uEnableTonemap", config.isTonemapEnabled() ? 1 : 0);
        shader.setUniformFloat("uBloomAlphaScale", config.getBloomAlphaScale());

        // 绑定纹理
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFbo.getTextureId());

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        // 使用最后一个模糊结果
        FrameBuffer finalBlur = (config.getBlurIterations() % 2 == 0) ? blurFbo2 : blurFbo1;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, finalBlur.getTextureId());

        drawFullscreenQuad();

        // 清理
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        ShaderProgram.unbind();
    }

    /**
     * 创建全屏四边形
     */
    private void createFullscreenQuad() {
        // 顶点数据: position(2) + texcoord(2)
        float[] vertices = {
            // 位置 // 纹理坐标
            -1.0f, 1.0f, 0.0f, 1.0f, // 左上
            -1.0f, -1.0f, 0.0f, 0.0f, // 左下
            1.0f, -1.0f, 1.0f, 0.0f, // 右下
            1.0f, 1.0f, 1.0f, 1.0f // 右上
        };

        int[] indices = { 0, 1, 2, 0, 2, 3 };

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices)
            .flip();

        ByteBuffer indexBuffer = BufferUtils.createByteBuffer(indices.length * 4);
        for (int index : indices) {
            indexBuffer.putInt(index);
        }
        indexBuffer.flip();

        quadVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(quadVao);

        quadVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        quadEbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, quadEbo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        // position (location = 0)
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0);

        // texcoord (location = 1)
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 2 * 4);

        GL30.glBindVertexArray(0);
    }

    /**
     * 绘制全屏四边形
     */
    private void drawFullscreenQuad() {
        GL30.glBindVertexArray(quadVao);
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * 查找带有 PostProcessComponent 的活动相机
     */
    private Entity findActiveCameraWithPostProcess() {
        for (Entity entity : getWorld().getEntitiesWith(CameraComponent.class, PostProcessComponent.class)) {
            CameraComponent camera = entity.getComponent(CameraComponent.class)
                .orElse(null);
            if (camera != null && camera.isActive()) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 启用/禁用后处理
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 检查是否启用
     */
    @Override
    public boolean isEnabled() {
        return super.isEnabled() && enabled;
    }

    /**
     * 获取场景 FBO（供 MeshRenderSystem 检测）
     */
    public FrameBuffer getSceneFbo() {
        return sceneFbo;
    }

    /**
     * 检查是否正在捕获
     */
    public boolean isCapturing() {
        return capturing;
    }
}
