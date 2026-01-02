package moe.takochan.takorender.api.system;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.joml.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.LightProbeComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 光照探针系统 - 查询并更新实体的 MC 光照数据
 *
 * <p>
 * LightProbeSystem 在 UPDATE 阶段运行，遍历所有拥有
 * {@link LightProbeComponent} 的实体，根据其位置查询 MC 世界光照，
 * 并更新组件中的光照值。
 * </p>
 *
 * <p>
 * <b>光照查询</b>:
 * </p>
 * <ul>
 * <li>blockLight - 方块光照（火把、熔岩、红石灯等）</li>
 * <li>skyLight - 天空光照（受时间和遮挡影响）</li>
 * </ul>
 *
 * <p>
 * <b>执行顺序</b>:
 * </p>
 * <p>
 * Priority 50，在 TransformSystem (10) 之后、渲染系统之前执行，
 * 确保位置数据已更新且光照数据在渲染前可用。
 * </p>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent({ LightProbeComponent.class, TransformComponent.class })
public class LightProbeSystem extends GameSystem {

    @Override
    public Phase getPhase() {
        return Phase.UPDATE;
    }

    @Override
    public int getPriority() {
        // 在 TransformSystem (10) 之后，渲染系统之前
        return 50;
    }

    @Override
    public void update(float deltaTime) {
        World mcWorld = getMCWorld();
        if (mcWorld == null) {
            return;
        }

        for (Entity entity : getRequiredEntities()) {
            updateEntityLighting(entity, mcWorld);
        }
    }

    /**
     * 更新单个实体的光照数据
     */
    private void updateEntityLighting(Entity entity, World mcWorld) {
        LightProbeComponent probe = entity.getComponent(LightProbeComponent.class)
            .orElse(null);
        TransformComponent transform = entity.getComponent(TransformComponent.class)
            .orElse(null);

        if (probe == null || transform == null) {
            return;
        }

        // 不接收光照的实体跳过查询
        if (!probe.isReceiveLighting()) {
            probe.setCombinedLight(1.0f);
            return;
        }

        Vector3f pos = transform.getPosition();
        int x = MathHelper.floor_double(pos.x);
        int y = MathHelper.floor_double(pos.y);
        int z = MathHelper.floor_double(pos.z);

        // 查询 MC 光照
        int blockLight = mcWorld.getSavedLightValue(EnumSkyBlock.Block, x, y, z);
        int skyLight = mcWorld.getSavedLightValue(EnumSkyBlock.Sky, x, y, z);

        // 更新组件
        probe.updateLighting(blockLight, skyLight);
    }

    /**
     * 获取 MC 客户端世界
     */
    private World getMCWorld() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            return mc.theWorld;
        } catch (Exception e) {
            return null;
        }
    }
}
