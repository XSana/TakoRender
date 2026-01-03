package moe.takochan.takorender.api;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.ecs.World;
import moe.takochan.takorender.api.system.CameraSystem;
import moe.takochan.takorender.api.system.LifetimeSystem;
import moe.takochan.takorender.api.system.LineRenderSystem;
import moe.takochan.takorender.api.system.MeshRenderSystem;
import moe.takochan.takorender.api.system.ParticleEmitSystem;
import moe.takochan.takorender.api.system.ParticlePhysicsSystem;
import moe.takochan.takorender.api.system.ParticleRenderSystem;
import moe.takochan.takorender.api.system.SpriteRenderSystem;
import moe.takochan.takorender.api.system.TransformSystem;

/**
 * TakoRender 公共 API 入口
 *
 * <p>
 * 提供全局 ECS World 实例和基础设施访问。
 * </p>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // 获取全局 World
 *     World world = TakoRender.getWorld();
 *
 *     // 创建 Entity
 *     Entity entity = world.createEntity();
 *     entity.addComponent(new TransformComponent(x, y, z));
 *     entity.addComponent(new LayerComponent(Layer.WORLD_3D));
 *     entity.addComponent(new VisibilityComponent());
 *     entity.addComponent(
 *         new MeshRendererComponent().setMesh(mesh)
 *             .setMaterial(material));
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class TakoRender {

    private static World world;
    private static boolean initialized = false;

    private TakoRender() {}

    /**
     * 初始化 TakoRender
     * <p>
     * 由 ClientProxy 在 init 阶段调用。
     * </p>
     */
    public static void initialize() {
        if (initialized) {
            TakoRenderMod.LOG.warn("TakoRender already initialized");
            return;
        }

        TakoRenderMod.LOG.info("Initializing TakoRender ECS...");

        world = new World();

        // 注册核心 System（按优先级排序）
        world.addSystem(new CameraSystem());
        world.addSystem(new TransformSystem());
        world.addSystem(new MeshRenderSystem());
        world.addSystem(new LineRenderSystem());
        world.addSystem(new SpriteRenderSystem());
        world.addSystem(new ParticleEmitSystem());
        world.addSystem(new ParticlePhysicsSystem());
        world.addSystem(new ParticleRenderSystem());
        world.addSystem(new LifetimeSystem());

        initialized = true;
        TakoRenderMod.LOG.info("TakoRender ECS initialized with {} systems", world.getSystemCount());
    }

    /**
     * 获取全局 ECS World
     *
     * @return 全局 World 实例
     * @throws IllegalStateException 如果未初始化
     */
    public static World getWorld() {
        if (!initialized) {
            throw new IllegalStateException("TakoRender not initialized. Call TakoRender.initialize() first.");
        }
        return world;
    }

    /**
     * 检查是否已初始化
     *
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 清理 TakoRender
     * <p>
     * 由 ClientProxy 在游戏退出时调用。
     * </p>
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }

        TakoRenderMod.LOG.info("Shutting down TakoRender ECS...");

        if (world != null) {
            world.clear();
            world = null;
        }

        initialized = false;
        TakoRenderMod.LOG.info("TakoRender ECS shutdown complete");
    }
}
