package moe.takochan.takorender.api.system;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.MeshRendererComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.graphics.Material;
import moe.takochan.takorender.api.graphics.Mesh;
import moe.takochan.takorender.api.graphics.shader.ShaderProgram;
import moe.takochan.takorender.core.gl.GLStateContext;

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
 * <li>使用 GLStateContext 管理 GL 状态</li>
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

    /** MVP 矩阵缓冲区（复用避免每帧分配） */
    private final FloatBuffer modelMatrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer viewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projMatrixBuffer = BufferUtils.createFloatBuffer(16);

    /** 临时矩阵对象（复用避免每帧分配） */
    private final Matrix4f tempModelMatrix = new Matrix4f();

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
        if (renderables.isEmpty()) {
            return;
        }

        // 按 sortingOrder 排序
        renderables.sort(
            Comparator.comparingInt(
                e -> e.getComponent(MeshRendererComponent.class)
                    .map(MeshRendererComponent::getSortingOrder)
                    .orElse(0)));

        // 缓存相机矩阵
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projMatrix = camera.getProjectionMatrix();

        viewMatrixBuffer.clear();
        viewMatrix.get(viewMatrixBuffer);
        projMatrixBuffer.clear();
        projMatrix.get(projMatrixBuffer);

        // 使用 GLStateContext 管理 GL 状态
        try (GLStateContext ctx = GLStateContext.begin()) {
            ctx.enableDepthTest();
            ctx.enableBlend();
            ctx.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ctx.enableCullFace();

            renderMeshes(renderables);
        }
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
                // 检查 Mesh 和 Material 是否存在
                if (renderer.getMesh() != null && renderer.getMaterial() != null) {
                    result.add(entity);
                }
            }
        }

        return result;
    }

    /**
     * 渲染所有网格
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
