package moe.takochan.takorender.core.system;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.DimensionComponent;
import moe.takochan.takorender.api.component.LODComponent;
import moe.takochan.takorender.api.component.LayerComponent;
import moe.takochan.takorender.api.component.MeshRendererComponent;
import moe.takochan.takorender.api.component.StaticFlagsComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.component.VisibilityComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Layer;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.ecs.StaticFlags;
import moe.takochan.takorender.api.graphics.Material;
import moe.takochan.takorender.api.graphics.Mesh;
import moe.takochan.takorender.api.graphics.RenderQueue;
import moe.takochan.takorender.api.graphics.mesh.BaseMesh;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.core.gl.GLStateContext;
import moe.takochan.takorender.core.render.BatchKey;
import moe.takochan.takorender.core.render.InstanceBuffer;

/**
 * 实例化渲染系统
 *
 * <p>
 * InstancedRenderSystem 负责渲染标记为 {@link StaticFlags#BATCHING} 的实体。
 * 相同 Mesh+Material 的实体会合并为单次 Instanced Draw Call。
 * </p>
 *
 * <p>
 * <b>合批条件</b>:
 * </p>
 * <ul>
 * <li>拥有 StaticFlagsComponent 且标记为 BATCHING</li>
 * <li>拥有 MeshRendererComponent（有效的 Mesh 和 Material）</li>
 * <li>VisibilityComponent.shouldRender() == true</li>
 * <li>位于当前活动 Layer 和 Dimension</li>
 * </ul>
 *
 * <p>
 * <b>执行阶段</b>: RENDER（在 MeshRenderSystem 之前）
 * </p>
 *
 * <p>
 * <b>Shader 要求</b>:
 * </p>
 * <ul>
 * <li>顶点属性 0-2: 网格数据（position, normal, uv）</li>
 * <li>顶点属性 3-6: 实例变换矩阵（mat4，4个 vec4）</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent({ MeshRendererComponent.class, StaticFlagsComponent.class })
public class InstancedRenderSystem extends GameSystem {

    /** 实例矩阵属性的起始位置（location 3-6） */
    private static final int INSTANCE_MATRIX_LOCATION = 3;

    /** 批次数据：BatchKey -> Entity 列表 */
    private final Map<BatchKey, List<Entity>> batches = new HashMap<>();

    /** 实例缓冲区（复用） */
    private InstanceBuffer instanceBuffer;

    /** MVP 矩阵缓冲区（复用） */
    private final FloatBuffer viewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projMatrixBuffer = BufferUtils.createFloatBuffer(16);

    @Override
    public Phase getPhase() {
        return Phase.RENDER;
    }

    @Override
    public int getPriority() {
        // 在 MeshRenderSystem (0) 之前执行
        return -100;
    }

    @Override
    public void onInit() {
        instanceBuffer = new InstanceBuffer(256);
    }

    @Override
    public void onDestroy() {
        if (instanceBuffer != null) {
            instanceBuffer.dispose();
            instanceBuffer = null;
        }
    }

    @Override
    public void update(float deltaTime) {
        // 获取活动相机
        Entity cameraEntity = findActiveCamera();
        if (cameraEntity == null) {
            return;
        }

        CameraComponent camera = cameraEntity.getComponent(CameraComponent.class)
            .orElse(null);
        if (camera == null) {
            return;
        }

        // 收集并分组可合批实体
        collectBatchableEntities();

        if (batches.isEmpty()) {
            return;
        }

        // 缓存相机矩阵
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projMatrix = camera.getProjectionMatrix();

        viewMatrixBuffer.clear();
        viewMatrix.get(viewMatrixBuffer);
        projMatrixBuffer.clear();
        projMatrix.get(projMatrixBuffer);

        // 渲染所有批次
        try (GLStateContext ctx = GLStateContext.begin()) {
            ctx.enableDepthTest();
            ctx.setDepthMask(true);
            ctx.enableCullFace();
            ctx.enableBlend();
            ctx.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            renderBatches();
        }

        // 清空批次（下一帧重新收集）
        for (List<Entity> list : batches.values()) {
            list.clear();
        }
        batches.clear();
    }

    /**
     * 收集可合批的实体并按 BatchKey 分组
     */
    private void collectBatchableEntities() {
        Layer currentLayer = getWorld().getCurrentLayer();
        int activeDimension = getWorld().getSceneManager()
            .getActiveDimensionId();

        for (Entity entity : getRequiredEntities()) {
            // 检查 BATCHING 标记
            StaticFlagsComponent flags = entity.getComponent(StaticFlagsComponent.class)
                .orElse(null);
            if (flags == null || !flags.isBatchable()) {
                continue;
            }

            // 检查可见性
            VisibilityComponent visibility = entity.getComponent(VisibilityComponent.class)
                .orElse(null);
            if (visibility != null && !visibility.shouldRender()) {
                continue;
            }

            // 检查 Layer 筛选
            if (currentLayer != null) {
                Layer entityLayer = entity.getComponent(LayerComponent.class)
                    .map(LayerComponent::getLayer)
                    .orElse(Layer.WORLD_3D);
                if (entityLayer != currentLayer) {
                    continue;
                }
            }

            // 检查维度筛选（仅 WORLD_3D 需要）
            if (currentLayer == Layer.WORLD_3D) {
                DimensionComponent dimension = entity.getComponent(DimensionComponent.class)
                    .orElse(null);
                if (dimension != null && dimension.getDimensionId() != activeDimension) {
                    continue;
                }
            }

            // 获取 Mesh 和 Material
            MeshRendererComponent renderer = entity.getComponent(MeshRendererComponent.class)
                .orElse(null);
            if (renderer == null) {
                continue;
            }

            // 优先使用 LOD Mesh
            Mesh mesh = null;
            LODComponent lod = entity.getComponent(LODComponent.class)
                .orElse(null);
            if (lod != null && lod.getLevelCount() > 0) {
                mesh = lod.getActiveMesh();
            }
            if (mesh == null) {
                mesh = renderer.getMesh();
            }

            Material material = renderer.getMaterial();

            if (mesh == null || material == null || mesh.isDisposed()) {
                continue;
            }

            // 只处理 OPAQUE 队列（透明物体需要排序，不适合合批）
            if (renderer.getRenderQueue() != RenderQueue.OPAQUE) {
                continue;
            }

            // 分组到批次
            BatchKey key = new BatchKey(mesh, material);
            batches.computeIfAbsent(key, k -> new ArrayList<>())
                .add(entity);
        }
    }

    /**
     * 渲染所有批次
     */
    private void renderBatches() {
        for (Map.Entry<BatchKey, List<Entity>> entry : batches.entrySet()) {
            BatchKey key = entry.getKey();
            List<Entity> entities = entry.getValue();

            if (entities.isEmpty()) {
                continue;
            }

            renderBatch(key.getMesh(), key.getMaterial(), entities);
        }
    }

    /**
     * 渲染单个批次
     */
    private void renderBatch(Mesh mesh, Material material, List<Entity> entities) {
        // 填充实例缓冲区
        instanceBuffer.begin();
        for (Entity entity : entities) {
            TransformComponent transform = entity.getComponent(TransformComponent.class)
                .orElse(null);
            if (transform != null) {
                instanceBuffer.addInstance(transform.getWorldMatrix());
            }
        }
        instanceBuffer.end();

        if (instanceBuffer.isEmpty()) {
            return;
        }

        // 应用材质
        material.apply();
        ShaderProgram shader = material.getShader();

        if (shader == null || !shader.isValid()) {
            return;
        }

        // 设置相机矩阵
        viewMatrixBuffer.rewind();
        shader.setUniformMatrix4("uView", false, viewMatrixBuffer);
        projMatrixBuffer.rewind();
        shader.setUniformMatrix4("uProjection", false, projMatrixBuffer);

        // 绑定网格
        mesh.bind();

        // 绑定实例属性
        instanceBuffer.bindAttributes(INSTANCE_MATRIX_LOCATION);

        // 绘制（使用实例化渲染）
        if (mesh instanceof BaseMesh) {
            BaseMesh baseMesh = (BaseMesh) mesh;
            int indexCount = baseMesh.getIndexCount();
            if (indexCount > 0) {
                GL31.glDrawElementsInstanced(
                    baseMesh.getDrawMode(),
                    indexCount,
                    GL11.GL_UNSIGNED_INT,
                    0,
                    instanceBuffer.getInstanceCount());
            }
        }

        // 解绑
        instanceBuffer.unbindAttributes(INSTANCE_MATRIX_LOCATION);
        mesh.unbind();
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
