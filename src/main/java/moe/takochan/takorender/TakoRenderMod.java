package moe.takochan.takorender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import moe.takochan.takorender.proxy.CommonProxy;

@Mod(
    modid = Reference.MODID,
    name = Reference.NAME,
    version = Tags.VERSION,
    dependencies = "required-after:Forge@[10.13.4.1614,)",
    acceptedMinecraftVersions = "[1.7.10]")
public class TakoRenderMod {

    public static final Logger LOG = LogManager.getLogger(Reference.MODID);

    @Mod.Instance(Reference.MODID)
    public static TakoRenderMod instance;

    @SidedProxy(clientSide = Reference.CLIENT_PROXY, serverSide = Reference.COMMON_PROXY)
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("TakoRender preInit");
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOG.info("TakoRender init");
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOG.info("TakoRender postInit");
        proxy.postInit(event);
    }
}
