package moe.takochan.takorender.api.system;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.LightProbeComponent;
import moe.takochan.takorender.api.component.MeshRendererComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.graphics.Material;
import moe.takochan.takorender.api.graphics.Mesh;
import moe.takochan.takorender.api.graphics.RenderQueue;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.core.gl.GLStateContext;

/**
 * 网格渲染系统 - 负责渲染所有拥有 MeshRendererComponent 的实体
 *
 * <p>
 * MeshRenderSystem 在 RENDER 阶段执行，按 RenderQueue 分层渲染：
 * </p>
 * <ol>
 * <li>BACKGROUND - 天空盒等背景</li>
 * <li>OPAQUE - 不透明物体（前到后，Early-Z 优化）</li>
 * <li>TRANSPARENT - 透明物体（后到前，正确混合）</li>
 * <li>OVERLAY - 叠加层（UI、调试）</li>
 * </ol>
 *
 * <p>
 * <b>渲染优化</b>:
 * </p>
 * <ul>
 * <li>按材质排序减少状态切换</li>
 * <li>透明物体按深度排序确保正确混合</li>
 * <li>使用 GLStateContext 管理 GL 状态</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent(MeshRendererComponent.class)
public class MeshRenderSystem extends GameSystem {

    /** MVP 矩阵缓冲区（复用避免每帧分配） */
    private final FloatBuffer modelMatrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer viewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projMatrixBuffer = BufferUtils.createFloatBuffer(16);

    /** 临时矩阵和向量（复用避免每帧分配） */
    private final Matrix4f tempModelMatrix = new Matrix4f();
    private final Vector3f tempPosition = new Vector3f();
    private final Vector3f cameraPosition = new Vector3f();

    /** 按队列分组的渲染列表（复用避免每帧分配） */
    private final Map<RenderQueue, List<Entity>> queuedRenderables = new EnumMap<>(RenderQueue.class);

    public MeshRenderSystem() {
        for (RenderQueue queue : RenderQueue.values()) {
            queuedRenderables.put(queue, new ArrayList<>());
        }
    }

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
        TransformComponent cameraTransform = cameraEntity.getComponent(TransformComponent.class)
            .orElse(null);

        if (camera == null) {
            return;
        }

        // 获取相机位置（用于深度排序）
        if (cameraTransform != null) {
            cameraTransform.getWorldMatrix()
                .getTranslation(cameraPosition);
        } else {
            cameraPosition.set(0, 0, 0);
        }

        // 收集并分组可渲染实体
        collectAndGroupRenderables();

        // 检查是否有可渲染物体
        boolean hasRenderables = false;
        for (List<Entity> list : queuedRenderables.values()) {
            if (!list.isEmpty()) {
                hasRenderables = true;
                break;
            }
        }
        if (!hasRenderables) {
            return;
        }

        // 缓存相机矩阵
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projMatrix = camera.getProjectionMatrix();

        viewMatrixBuffer.clear();
        viewMatrix.get(viewMatrixBuffer);
        projMatrixBuffer.clear();
        projMatrix.get(projMatrixBuffer);

        // 获取后处理系统（如果存在）
        PostProcessSystem postProcess = getWorld().getSystem(PostProcessSystem.class);
        boolean usePostProcess = postProcess != null && postProcess.isEnabled() && postProcess.isInitialized();

        // 开始后处理捕获（如果启用）
        if (usePostProcess) {
            postProcess.beginCapture();
        }

        // 使用 GLStateContext 管理 GL 状态
        try (GLStateContext ctx = GLStateContext.begin()) {
            ctx.enableDepthTest();
            ctx.enableBlend();
            ctx.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ctx.enableCullFace();

            // 按队列顺序渲染
            renderQueue(RenderQueue.BACKGROUND, ctx);
            renderQueue(RenderQueue.OPAQUE, ctx);
            renderQueue(RenderQueue.TRANSPARENT, ctx);
            renderQueue(RenderQueue.OVERLAY, ctx);
        }

        // 结束后处理捕获（如果启用）
        if (usePostProcess) {
            postProcess.endCapture();
        }

        // 清空队列（下一帧重新收集）
        for (List<Entity> list : queuedRenderables.values()) {
            list.clear();
        }
    }

    /**
     * 收集并分组所有可渲染实体
     */
    private void collectAndGroupRenderables() {
        for (Entity entity : getRequiredEntities()) {
            MeshRendererComponent renderer = entity.getComponent(MeshRendererComponent.class)
                .orElse(null);

            if (renderer != null && renderer.isVisible()) {
                if (renderer.getMesh() != null && renderer.getMaterial() != null) {
                    RenderQueue queue = renderer.getRenderQueue();
                    queuedRenderables.get(queue)
                        .add(entity);
                }
            }
        }
    }

    /**
     * 渲染指定队列
     */
    private void renderQueue(RenderQueue queue, GLStateContext ctx) {
        List<Entity> entities = queuedRenderables.get(queue);
        if (entities.isEmpty()) {
            return;
        }

        // 根据队列类型设置 GL 状态
        configureQueueState(queue, ctx);

        // 排序
        sortEntities(entities, queue);

        // 渲染
        renderMeshes(entities);
    }

    /**
     * 根据队列类型配置 GL 状态
     */
    private void configureQueueState(RenderQueue queue, GLStateContext ctx) {
        switch (queue) {
            case BACKGROUND:
                // 背景：禁用深度写入，保持深度测试
                ctx.setDepthMask(false);
                ctx.enableDepthTest();
                break;

            case OPAQUE:
                // 不透明：启用深度写入和测试
                ctx.setDepthMask(true);
                ctx.enableDepthTest();
                break;

            case TRANSPARENT:
                // 透明：禁用深度写入，保持深度测试
                ctx.setDepthMask(false);
                ctx.enableDepthTest();
                ctx.enableBlend();
                ctx.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                break;

            case OVERLAY:
                // 叠加：禁用深度测试
                ctx.disableDepthTest();
                ctx.setDepthMask(false);
                break;
        }
    }

    /**
     * 排序实体
     */
    private void sortEntities(List<Entity> entities, RenderQueue queue) {
        if (queue.requiresDepthSorting()) {
            // 透明物体：从后往前排序（距离相机远的先渲染）
            entities.sort((e1, e2) -> {
                float dist1 = getDistanceToCamera(e1);
                float dist2 = getDistanceToCamera(e2);
                // 从远到近排序
                int distCompare = Float.compare(dist2, dist1);
                if (distCompare != 0) {
                    return distCompare;
                }
                // 相同距离按 sortingOrder
                return compareSortingOrder(e1, e2);
            });
        } else {
            // 非透明物体：按 sortingOrder 排序
            entities.sort(this::compareSortingOrder);
        }
    }

    /**
     * 比较 sortingOrder
     */
    private int compareSortingOrder(Entity e1, Entity e2) {
        int order1 = e1.getComponent(MeshRendererComponent.class)
            .map(MeshRendererComponent::getSortingOrder)
            .orElse(0);
        int order2 = e2.getComponent(MeshRendererComponent.class)
            .map(MeshRendererComponent::getSortingOrder)
            .orElse(0);
        return Integer.compare(order1, order2);
    }

    /**
     * 计算实体到相机的距离
     */
    private float getDistanceToCamera(Entity entity) {
        TransformComponent transform = entity.getComponent(TransformComponent.class)
            .orElse(null);
        if (transform == null) {
            return 0;
        }

        transform.getWorldMatrix()
            .getTranslation(tempPosition);
        return tempPosition.distanceSquared(cameraPosition);
    }

    /**
     * 渲染网格列表
     */
    private void renderMeshes(List<Entity> renderables) {
        Material lastMaterial = null;
        ShaderProgram currentShader = null;

        for (Entity entity : renderables) {
            MeshRendererComponent renderer = entity.getComponent(MeshRendererComponent.class)
                .orElse(null);
            TransformComponent transform = entity.getComponent(TransformComponent.class)
                .orElse(null);

            if (renderer == null || transform == null) {
                continue;
            }

            Mesh mesh = renderer.getMesh();
            Material material = renderer.getMaterial();

            if (mesh == null || material == null || mesh.isDisposed()) {
                continue;
            }

            // 材质切换优化：只在材质变化时重新绑定
            if (material != lastMaterial) {
                material.apply();
                currentShader = material.getShader();
                lastMaterial = material;

                // 设置相机矩阵（每次材质切换时）
                if (currentShader != null && currentShader.isValid()) {
                    viewMatrixBuffer.rewind();
                    currentShader.setUniformMatrix4("uView", false, viewMatrixBuffer);
                    projMatrixBuffer.rewind();
                    currentShader.setUniformMatrix4("uProjection", false, projMatrixBuffer);
                }
            }

            // 设置模型矩阵
            if (currentShader != null && currentShader.isValid()) {
                tempModelMatrix.set(transform.getWorldMatrix());
                modelMatrixBuffer.clear();
                tempModelMatrix.get(modelMatrixBuffer);
                currentShader.setUniformMatrix4("uModel", false, modelMatrixBuffer);

                // 设置光照 uniform（如果有 LightProbeComponent）
                LightProbeComponent probe = entity.getComponent(LightProbeComponent.class)
                    .orElse(null);
                if (probe != null) {
                    currentShader.setUniformFloat("uBlockLight", probe.getBlockLight());
                    currentShader.setUniformFloat("uSkyLight", probe.getSkyLight());
                    currentShader.setUniformFloat("uCombinedLight", probe.getCombinedLight());
                } else {
                    // 无光照探针时使用全亮度
                    currentShader.setUniformFloat("uBlockLight", 1.0f);
                    currentShader.setUniformFloat("uSkyLight", 1.0f);
                    currentShader.setUniformFloat("uCombinedLight", 1.0f);
                }
            }

            // 绘制网格
            mesh.bind();
            mesh.draw();
            mesh.unbind();
        }

        // 解绑着色器
        ShaderProgram.unbind();
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
