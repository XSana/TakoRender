package moe.takochan.takorender.api.system;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.MeshRendererComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;

/**
 * 网格渲染系统 - 负责渲染所有拥有 MeshRendererComponent 的实体
 *
 * <p>
 * MeshRenderSystem 在 RENDER 阶段执行，收集所有可见的网格渲染组件，
 * 按材质和排序顺序进行批量渲染。
 * </p>
 *
 * <p>
 * <b>渲染流程</b>:
 * </p>
 * <ol>
 * <li>查找活动相机</li>
 * <li>收集所有可见的 MeshRendererComponent</li>
 * <li>按材质和 sortingOrder 排序</li>
 * <li>批量渲染（减少状态切换）</li>
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
 *     World world = new World();
 *     world.addSystem(new CameraSystem());
 *     world.addSystem(new MeshRenderSystem());
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent(MeshRendererComponent.class)
public class MeshRenderSystem extends GameSystem {

    @Override
    public Phase getPhase() {
        return Phase.RENDER;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void update(float deltaTime) {
        Entity cameraEntity = findActiveCamera();
        if (cameraEntity == null) {
            return;
        }

        CameraComponent camera = cameraEntity.getComponent(CameraComponent.class)
            .orElse(null);
        if (camera == null) {
            return;
        }

        List<Entity> renderables = collectRenderables();

        renderables.sort(
            Comparator.comparingInt(
                e -> e.getComponent(MeshRendererComponent.class)
                    .map(MeshRendererComponent::getSortingOrder)
                    .orElse(0)));

        renderMeshes(camera, renderables);
    }

    /**
     * 收集所有可渲染的实体
     */
    private List<Entity> collectRenderables() {
        List<Entity> result = new ArrayList<>();

        for (Entity entity : getRequiredEntities()) {
            MeshRendererComponent renderer = entity.getComponent(MeshRendererComponent.class)
                .orElse(null);

            if (renderer != null && renderer.isVisible()) {
                result.add(entity);
            }
        }

        return result;
    }

    /**
     * 渲染所有网格
     *
     * <p>
     * TODO: 实际渲染逻辑待实现（需要 Mesh、Material、Shader 等基础设施）
     * </p>
     */
    private void renderMeshes(CameraComponent camera, List<Entity> renderables) {
        for (Entity entity : renderables) {
            MeshRendererComponent renderer = entity.getComponent(MeshRendererComponent.class)
                .orElse(null);
            TransformComponent transform = entity.getComponent(TransformComponent.class)
                .orElse(null);

            if (renderer == null || transform == null) {
                continue;
            }

            // TODO: 实际渲染逻辑
            // 1. 获取 Mesh 和 Material
            // 2. 绑定 Shader
            // 3. 设置 uniform（MVP 矩阵等）
            // 4. 绘制网格
        }
    }

    /**
     * 查找活动相机
     */
    private Entity findActiveCamera() {
        for (Entity entity : getWorld().getEntitiesWith(CameraComponent.class)) {
            CameraComponent camera = entity.getComponent(CameraComponent.class)
                .orElse(null);
            if (camera != null && camera.isActive()) {
                return entity;
            }
        }
        return null;
    }
}
