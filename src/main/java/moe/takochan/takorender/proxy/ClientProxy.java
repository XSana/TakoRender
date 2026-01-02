package moe.takochan.takorender.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.resource.ShaderManager;
import moe.takochan.takorender.api.resource.TextureManager;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        preloadResources();
    }

    /**
     * 预加载渲染资源（着色器、纹理）
     *
     * <p>
     * 在 postInit 阶段调用，批量编译着色器和加载纹理，避免首帧卡顿。
     * </p>
     */
    private void preloadResources() {
        TakoRenderMod.LOG.info("Preloading rendering resources...");

        try {
            ShaderManager.instance()
                .preloadAll();
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to preload shaders", e);
        }

        try {
            TextureManager.instance()
                .preloadAll();
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to preload textures", e);
        }

        TakoRenderMod.LOG.info("Resource preloading complete");
    }
}
