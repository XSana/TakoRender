package moe.takochan.takorender.api.resource;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.TakoRenderMod;

/**
 * 纹理资源管理器
 *
 * <p>
 * TextureManager 负责管理 OpenGL 纹理的加载、缓存和卸载。
 * </p>
 *
 * <p>
 * <b>资源键格式</b>:
 * </p>
 * <ul>
 * <li>{@code "domain:path"} - 从 assets/{domain}/textures/{path}.png 加载</li>
 * <li>{@code "domain:path:nofilter"} - 不使用线性过滤（像素风格）</li>
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
 *     // 获取纹理（自动加载）
 *     ResourceHandle<Integer> texture = TextureManager.instance()
 *         .get("takorender:particle/fire");
 *
 *     // 使用纹理
 *     GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.get());
 *
 *     // 使用完毕释放
 *     texture.release();
 * }
 * </pre>
 *
 * <p>
 * <b>注意</b>: 返回的资源类型是 Integer（OpenGL 纹理 ID）
 * </p>
 */
@SideOnly(Side.CLIENT)
public class TextureManager extends ResourceManager<Integer> {

    private static final String TEXTURE_BASE_PATH = "textures/";

    private static TextureManager INSTANCE;

    private TextureManager() {
        super("TextureManager");
    }

    /**
     * 获取单例实例
     */
    public static TextureManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new TextureManager();
        }
        return INSTANCE;
    }

    @Override
    protected Integer loadResource(String key) {
        String[] parts = key.split(":");
        if (parts.length < 2) {
            TakoRenderMod.LOG.error("Invalid texture key format: {} (expected domain:path)", key);
            return null;
        }

        String domain = parts[0];
        String path = parts[1];
        boolean useFilter = parts.length < 3 || !parts[2].equals("nofilter");

        try {
            return loadTexture(domain, path, useFilter);
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to load texture: {}", key, e);
            return null;
        }
    }

    private Integer loadTexture(String domain, String path, boolean useFilter) {
        String fullPath = TEXTURE_BASE_PATH + path + ".png";
        ResourceLocation location = new ResourceLocation(domain, fullPath);

        try {
            InputStream stream = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(location)
                .getInputStream();

            BufferedImage image = ImageIO.read(stream);
            stream.close();

            if (image == null) {
                TakoRenderMod.LOG.error("Failed to read image: {}", location);
                return null;
            }

            int textureId = TextureUtil
                .uploadTextureImageAllocate(TextureUtil.glGenTextures(), image, useFilter, false);

            TakoRenderMod.LOG
                .debug("Loaded texture: {} ({}x{}, id={})", location, image.getWidth(), image.getHeight(), textureId);

            return textureId;

        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to load texture: {}", location, e);
            return null;
        }
    }

    @Override
    protected void unloadResource(Integer textureId) {
        if (textureId != null && textureId > 0) {
            GL11.glDeleteTextures(textureId);
        }
    }

    /**
     * 绑定纹理到指定纹理单元
     *
     * @param handle 纹理句柄
     * @param unit   纹理单元 (0-15)
     */
    public static void bind(ResourceHandle<Integer> handle, int unit) {
        if (handle != null && handle.isValid()) {
            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, handle.get());
        }
    }

    /**
     * 解绑当前纹理单元的纹理
     *
     * @param unit 纹理单元 (0-15)
     */
    public static void unbind(int unit) {
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * 重置单例（用于测试或重载）
     */
    public static void reset() {
        if (INSTANCE != null) {
            INSTANCE.dispose();
            INSTANCE = null;
        }
    }
}
