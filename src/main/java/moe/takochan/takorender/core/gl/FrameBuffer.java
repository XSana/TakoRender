package moe.takochan.takorender.core.gl;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 帧缓冲对象 (FBO) 封装类 - 用于离屏渲染
 *
 * <p>
 * FrameBuffer 提供渲染到纹理的 API，是后处理效果（Bloom、HDR 等）的基础设施。
 * </p>
 *
 * <p>
 * <b>MC 双缓冲注意事项</b>:
 * </p>
 * <ul>
 * <li>FBO 0 是<b>显示帧</b>，不是渲染帧</li>
 * <li>MC 渲染到内部 FBO（或 Optifine/Angelica 的 FBO）</li>
 * <li>本类自动保存/恢复之前的 FBO 绑定</li>
 * </ul>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     FrameBuffer fbo = new FrameBuffer(1920, 1080, true);
 *
 *     // 渲染到 FBO
 *     fbo.bind();
 *     GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
 *     // ... 渲染场景 ...
 *     fbo.unbind();
 *
 *     // 使用 FBO 纹理采样
 *     fbo.bindTexture(0);
 *     // ... 在着色器中采样 ...
 *
 *     // 清理资源
 *     fbo.close();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class FrameBuffer implements AutoCloseable {

    private int fboId;
    private int colorTextureId;
    private int depthRboId;
    private int width;
    private int height;
    private final boolean useDepth;

    /** 绑定前保存的 FBO ID（处理 MC 双缓冲） */
    private int previousFbo;

    /** 绑定前保存的视口 */
    private final IntBuffer previousViewport = BufferUtils.createIntBuffer(16);

    private boolean disposed = false;

    /**
     * 创建带可选深度缓冲的帧缓冲
     *
     * @param width    宽度（像素）
     * @param height   高度（像素）
     * @param useDepth 是否创建深度缓冲附件
     */
    public FrameBuffer(int width, int height, boolean useDepth) {
        this.width = width;
        this.height = height;
        this.useDepth = useDepth;
        init();
    }

    /**
     * 创建无深度缓冲的帧缓冲
     *
     * @param width  宽度（像素）
     * @param height 高度（像素）
     */
    public FrameBuffer(int width, int height) {
        this(width, height, false);
    }

    private void init() {
        // 创建 FBO
        fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);

        // 创建颜色纹理附件
        colorTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTextureId);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            width,
            height,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (java.nio.ByteBuffer) null);

        // 纹理过滤和边缘处理
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        // 附加颜色纹理到 FBO
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT0,
            GL11.GL_TEXTURE_2D,
            colorTextureId,
            0);

        // 可选：创建深度缓冲
        if (useDepth) {
            depthRboId = GL30.glGenRenderbuffers();
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRboId);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height);
            GL30.glFramebufferRenderbuffer(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_RENDERBUFFER,
                depthRboId);
        }

        // 检查完整性
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete! Status: " + status);
        }

        // 解绑
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * 绑定此 FBO 为渲染目标
     *
     * <p>
     * 自动保存当前 FBO 绑定和视口，供 {@link #unbind()} 恢复。
     * </p>
     */
    public void bind() {
        if (disposed) {
            throw new IllegalStateException("FrameBuffer has been disposed");
        }

        // 保存当前 FBO（MC 双缓冲 - 可能不是 0！）
        previousFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        // 保存当前视口
        GL11.glGetInteger(GL11.GL_VIEWPORT, previousViewport);

        // 绑定此 FBO
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL11.glViewport(0, 0, width, height);
    }

    /**
     * 解绑此 FBO，恢复之前的 FBO 和视口
     *
     * <p>
     * <b>重要</b>: 恢复到之前绑定的 FBO，不是 FBO 0。
     * 这对 MC 双缓冲兼容性至关重要。
     * </p>
     */
    public void unbind() {
        // 恢复之前的 FBO（不是 0！）
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);

        // 恢复之前的视口
        GL11.glViewport(
            previousViewport.get(0),
            previousViewport.get(1),
            previousViewport.get(2),
            previousViewport.get(3));
    }

    /**
     * 绑定颜色纹理到纹理单元用于采样
     *
     * @param unit 纹理单元 (0-15)
     */
    public void bindTexture(int unit) {
        if (disposed) {
            throw new IllegalStateException("FrameBuffer has been disposed");
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTextureId);
    }

    /**
     * 解绑颜色纹理
     */
    public void unbindTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * 调整 FBO 尺寸
     *
     * <p>
     * 重新创建颜色纹理和深度缓冲以适应新尺寸。
     * </p>
     *
     * @param newWidth  新宽度（像素）
     * @param newHeight 新高度（像素）
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth == width && newHeight == height) {
            return;
        }
        if (newWidth <= 0 || newHeight <= 0) {
            return;
        }

        width = newWidth;
        height = newHeight;

        // 调整颜色纹理
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTextureId);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            width,
            height,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (java.nio.ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // 调整深度缓冲（如有）
        if (useDepth && depthRboId != 0) {
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRboId);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        }
    }

    /**
     * 获取 OpenGL FBO ID
     */
    public int getFboId() {
        return fboId;
    }

    /**
     * 获取颜色纹理 ID（用于采样）
     */
    public int getTextureId() {
        return colorTextureId;
    }

    /**
     * 获取当前宽度
     */
    public int getWidth() {
        return width;
    }

    /**
     * 获取当前高度
     */
    public int getHeight() {
        return height;
    }

    /**
     * 检查是否有深度缓冲
     */
    public boolean hasDepth() {
        return useDepth;
    }

    /**
     * 检查是否已释放
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * 释放所有 OpenGL 资源
     */
    @Override
    public void close() {
        if (disposed) {
            return;
        }
        disposed = true;

        if (fboId != 0) {
            GL30.glDeleteFramebuffers(fboId);
            fboId = 0;
        }
        if (colorTextureId != 0) {
            GL11.glDeleteTextures(colorTextureId);
            colorTextureId = 0;
        }
        if (depthRboId != 0) {
            GL30.glDeleteRenderbuffers(depthRboId);
            depthRboId = 0;
        }
    }
}
