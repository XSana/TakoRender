package moe.takochan.takorender.core;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.ecs.Layer;
import moe.takochan.takorender.api.ecs.World;
import moe.takochan.takorender.api.system.LifetimeSystem;

/**
 * Forge 渲染事件处理器
 *
 * <p>
 * 将 Forge 渲染事件路由到 ECS World 的分层渲染。
 * </p>
 *
 * <p>
 * <b>事件映射</b>:
 * </p>
 * <ul>
 * <li>RenderWorldLastEvent → WORLD_3D 层</li>
 * <li>RenderGameOverlayEvent.Post(ALL) → HUD 层</li>
 * <li>DrawScreenEvent.Post → GUI 层</li>
 * <li>ClientDisconnectionFromServerEvent → 存档退出清理</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class RenderEventHandler {

    private final World world;

    public RenderEventHandler(World world) {
        this.world = world;
    }

    /**
     * WORLD_3D 层渲染
     * <p>
     * 在 MC 渲染后、Angelica 后处理前触发。
     * 3D 内容会参与光影 Mod 的后处理。
     * </p>
     */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (world == null) {
            return;
        }

        // 同步当前维度
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            world.getSceneManager()
                .setActiveDimensionId(mc.theWorld.provider.dimensionId);
        }

        world.update(Layer.WORLD_3D, event.partialTicks);
        world.render(Layer.WORLD_3D);
    }

    /**
     * HUD 层渲染
     * <p>
     * 游戏内覆盖层（血条、状态图标、小地图等）。
     * 在所有后处理之后渲染，不受光影影响。
     * </p>
     */
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (world == null) {
            return;
        }

        // 只在 ALL 类型时渲染（避免重复渲染）
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        world.update(Layer.HUD, event.partialTicks);
        world.render(Layer.HUD);
    }

    /**
     * GUI 层渲染
     * <p>
     * 玩家打开的界面（背包、菜单、对话框等）。
     * 在 GUI 绘制完成后渲染。
     * </p>
     */
    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (world == null) {
            return;
        }

        world.update(Layer.GUI, event.renderPartialTicks);
        world.render(Layer.GUI);
    }

    /**
     * 存档退出事件
     * <p>
     * 清理 SESSION 类型的 Entity。
     * </p>
     */
    @SubscribeEvent
    public void onClientDisconnection(ClientDisconnectionFromServerEvent event) {
        if (world == null) {
            return;
        }

        TakoRenderMod.LOG.info("Client disconnected, cleaning up SESSION entities");

        LifetimeSystem lifetimeSystem = world.getSystem(LifetimeSystem.class);
        if (lifetimeSystem != null) {
            lifetimeSystem.destroySessionEntities();
        }
    }
}
