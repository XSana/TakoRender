package moe.takochan.takorender.api.system;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.TakoRenderMod;
import moe.takochan.takorender.api.component.CameraComponent;
import moe.takochan.takorender.api.component.LightProbeComponent;
import moe.takochan.takorender.api.component.ParticleBufferComponent;
import moe.takochan.takorender.api.component.ParticleRenderComponent;
import moe.takochan.takorender.api.component.ParticleStateComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.particle.BlendMode;
import moe.takochan.takorender.api.particle.RenderMode;
import moe.takochan.takorender.core.particle.ParticleBuffer;
import moe.takochan.takorender.core.particle.ParticleCPU;
import moe.takochan.takorender.core.particle.ParticleRenderer;

/**
 * 粒子渲染系统
 *
 * <p>
 * 负责渲染所有粒子系统。在 RENDER 阶段执行。
 * </p>
 *
 * <p>
 * <b>渲染路径</b>:
 * </p>
 * <ul>
 * <li>GPU 模式: 使用 SSBO 绑定粒子数据，Instanced Drawing 渲染 (OpenGL 4.3+)</li>
 * <li>CPU 模式: 每帧上传粒子数据到 VBO，动态渲染 (OpenGL 3.3)</li>
 * </ul>
 *
 * <p>
 * 两种模式使用相同的着色器，视觉效果一致。CPU 模式性能较低，建议限制粒子数。
 * </p>
 *
 * <p>
 * <b>后处理集成</b>:
 * </p>
 * <p>
 * 当 PostProcessSystem 启用时，粒子会渲染到 sceneFbo。
 * 带有 emissive > 0 的粒子会自动贡献到 Bloom 辉光效果。
 * </p>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent({ ParticleBufferComponent.class, ParticleStateComponent.class, TransformComponent.class })
public class ParticleRenderSystem extends GameSystem {

    /** 缓存的矩阵缓冲区 */
    private FloatBuffer viewMatrixBuffer;
    private FloatBuffer projMatrixBuffer;

    /** 缓存的矩阵数组 */
    private float[] viewMatrix = new float[16];
    private float[] projMatrix = new float[16];
    private float[] cameraPos = new float[3];

    /** 粒子渲染器（延迟初始化） */
    private ParticleRenderer renderer;

    @Override
    public Phase getPhase() {
        return Phase.RENDER;
    }

    @Override
    public int getPriority() {
        // 优先级 201: 在 MeshRenderSystem (200) 之后渲染
        // MeshRenderSystem 会调用 beginCapture/endCapture，但我们需要自己处理
        return 201;
    }

    @Override
    public void onInit() {
        viewMatrixBuffer = BufferUtils.createFloatBuffer(16);
        projMatrixBuffer = BufferUtils.createFloatBuffer(16);
        TakoRenderMod.LOG.info("ParticleRenderSystem: Initialized");
    }

    @Override
    public void update(float deltaTime) {
        CameraComponent camera = findActiveCamera();
        boolean useMinecraftCamera = (camera == null);

        if (!useMinecraftCamera) {
            extractCameraData(camera);
        } else {
            extractMinecraftCamera();
        }

        PostProcessSystem postProcess = getWorld() != null ? getWorld().getSystem(PostProcessSystem.class) : null;
        boolean usePostProcess = postProcess != null && postProcess.isEnabled() && postProcess.isInitialized();

        if (usePostProcess) {
            postProcess.beginCapture();
        }

        for (Entity entity : getRequiredEntities()) {
            renderParticleEntity(entity, useMinecraftCamera, deltaTime);
        }

        if (usePostProcess) {
            postProcess.endCapture();
        }
    }

    private void renderParticleEntity(Entity entity, boolean useMinecraftCamera, float partialTicks) {
        ParticleBufferComponent buffer = entity.getComponent(ParticleBufferComponent.class)
            .orElse(null);
        ParticleStateComponent state = entity.getComponent(ParticleStateComponent.class)
            .orElse(null);
        TransformComponent transform = entity.getComponent(TransformComponent.class)
            .orElse(null);

        if (buffer == null || state == null) {
            return;
        }

        if (!buffer.isInitialized() || state.getAliveCount() <= 0) {
            return;
        }

        ParticleRenderComponent renderComp = entity.getComponent(ParticleRenderComponent.class)
            .orElse(null);
        LightProbeComponent lightProbe = entity.getComponent(LightProbeComponent.class)
            .orElse(null);

        ensureRendererInitialized();
        if (renderer == null) {
            return;
        }

        int textureId = 0;
        if (renderComp != null) {
            configureRenderer(renderComp, transform, lightProbe);
            textureId = renderComp.getTextureId();
        }

        if (buffer.isUseCPUFallback()) {
            renderCPU(buffer, state, textureId);
        } else {
            renderGPU(buffer, state, textureId);
        }
    }

    private void ensureRendererInitialized() {
        if (renderer != null) {
            return;
        }

        try {
            renderer = new ParticleRenderer();
            if (!renderer.initialize()) {
                TakoRenderMod.LOG.warn("ParticleRenderSystem: Failed to initialize renderer");
                renderer = null;
            }
        } catch (Exception e) {
            TakoRenderMod.LOG.error("ParticleRenderSystem: Error initializing renderer", e);
            renderer = null;
        }
    }

    private void configureRenderer(ParticleRenderComponent renderComp, TransformComponent transform,
        LightProbeComponent lightProbe) {
        renderer.setBlendMode(convertBlendMode(renderComp.getBlendMode()));
        renderer.setRenderMode(convertRenderMode(renderComp.getRenderMode()));
        renderer.setSoftParticles(renderComp.isSoftParticles(), renderComp.getSoftDistance());
        renderer.setDepthWrite(renderComp.isDepthWrite());

        // 纹理动画参数
        renderer.setTextureAnimation(
            renderComp.getTextureTilesX(),
            renderComp.getTextureTilesY(),
            renderComp.getAnimationSpeed(),
            renderComp.getAnimationMode());

        // 光照参数：优先使用 LightProbeComponent，否则回退到直接查询
        float blockLight = 1.0f;
        float skyLight = 1.0f;

        if (renderComp.isReceiveLighting()) {
            if (lightProbe != null) {
                blockLight = lightProbe.getBlockLight();
                skyLight = lightProbe.getSkyLight();
            } else if (transform != null) {
                float[] lightLevels = queryMCLighting(transform);
                blockLight = lightLevels[0];
                skyLight = lightLevels[1];
            }
        }

        renderer.setLighting(blockLight, skyLight, renderComp.getEmissive(), renderComp.isReceiveLighting());
        renderer.setMinBrightness(renderComp.getMinBrightness());
    }

    /**
     * 查询 MC 世界光照（按发射器位置查询）
     *
     * @param transform 发射器位置
     * @return [blockLight, skyLight] 范围 0-1
     */
    private float[] queryMCLighting(TransformComponent transform) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            World world = mc.theWorld;
            if (world == null) {
                return new float[] { 1.0f, 1.0f };
            }

            Vector3f pos = transform.getPosition();
            int x = MathHelper.floor_double(pos.x);
            int y = MathHelper.floor_double(pos.y);
            int z = MathHelper.floor_double(pos.z);

            int blockLightLevel = world.getSavedLightValue(EnumSkyBlock.Block, x, y, z);
            int skyLightLevel = world.getSavedLightValue(EnumSkyBlock.Sky, x, y, z);

            return new float[] { blockLightLevel / 15.0f, skyLightLevel / 15.0f };
        } catch (Exception e) {
            return new float[] { 1.0f, 1.0f };
        }
    }

    /**
     * 将 api.particle.BlendMode 转换为 ParticleRenderer.BlendMode
     */
    private ParticleRenderer.BlendMode convertBlendMode(BlendMode mode) {
        if (mode == null) {
            return ParticleRenderer.BlendMode.ALPHA;
        }
        switch (mode) {
            case ALPHA:
                return ParticleRenderer.BlendMode.ALPHA;
            case ADDITIVE:
            case SOFT_ADDITIVE:
                return ParticleRenderer.BlendMode.ADDITIVE;
            case MULTIPLY:
                return ParticleRenderer.BlendMode.MULTIPLY;
            case PREMULTIPLIED:
                return ParticleRenderer.BlendMode.PREMULTIPLIED;
            case OPAQUE:
                return ParticleRenderer.BlendMode.ALPHA;
            default:
                return ParticleRenderer.BlendMode.ALPHA;
        }
    }

    /**
     * 将 api.particle.RenderMode 转换为 ParticleRenderer.RenderMode
     */
    private ParticleRenderer.RenderMode convertRenderMode(RenderMode mode) {
        if (mode == null) {
            return ParticleRenderer.RenderMode.BILLBOARD_QUAD;
        }
        switch (mode) {
            case BILLBOARD_QUAD:
                return ParticleRenderer.RenderMode.BILLBOARD_QUAD;
            case HORIZONTAL_BILLBOARD:
            case VERTICAL_BILLBOARD:
                return ParticleRenderer.RenderMode.BILLBOARD_QUAD;
            case STRETCHED_BILLBOARD:
                return ParticleRenderer.RenderMode.STRETCHED_BILLBOARD;
            case MESH:
                return ParticleRenderer.RenderMode.MESH;
            default:
                return ParticleRenderer.RenderMode.BILLBOARD_QUAD;
        }
    }

    private void renderGPU(ParticleBufferComponent buffer, ParticleStateComponent state, int textureId) {
        ParticleBuffer gpuBuffer = buffer.getGpuBuffer();
        if (gpuBuffer == null || !gpuBuffer.isValid()) {
            return;
        }

        renderer.render(gpuBuffer, viewMatrix, projMatrix, cameraPos, textureId, state.getAliveCount());
    }

    /**
     * CPU 渲染路径（回退模式）
     *
     * <p>
     * 每帧上传粒子数据到 VBO，兼容 OpenGL 3.3。
     * </p>
     */
    private void renderCPU(ParticleBufferComponent buffer, ParticleStateComponent state, int textureId) {
        ParticleCPU cpuBuffer = buffer.getCpuBuffer();
        if (cpuBuffer == null) {
            return;
        }

        renderer.renderCPU(cpuBuffer, viewMatrix, projMatrix, cameraPos, textureId, state.getAliveCount());
    }

    private CameraComponent findActiveCamera() {
        if (getWorld() == null) {
            return null;
        }

        for (Entity entity : getWorld().getEntitiesWith(CameraComponent.class)) {
            CameraComponent camera = entity.getComponent(CameraComponent.class)
                .orElse(null);
            if (camera != null && camera.isActive()) {
                return camera;
            }
        }
        return null;
    }

    private void extractCameraData(CameraComponent camera) {
        viewMatrixBuffer.clear();
        camera.getViewMatrix()
            .get(viewMatrixBuffer);
        viewMatrixBuffer.get(viewMatrix);

        projMatrixBuffer.clear();
        camera.getProjectionMatrix()
            .get(projMatrixBuffer);
        projMatrixBuffer.get(projMatrix);

        Entity cameraEntity = camera.getEntity();
        if (cameraEntity != null) {
            TransformComponent transform = cameraEntity.getComponent(TransformComponent.class)
                .orElse(null);
            if (transform != null) {
                Vector3f pos = transform.getPosition();
                cameraPos[0] = pos.x;
                cameraPos[1] = pos.y;
                cameraPos[2] = pos.z;
            }
        }
    }

    private void extractMinecraftCamera() {
        viewMatrixBuffer.clear();
        projMatrixBuffer.clear();

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, viewMatrixBuffer);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projMatrixBuffer);

        viewMatrixBuffer.get(viewMatrix);
        projMatrixBuffer.get(projMatrix);

        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                cameraPos[0] = (float) mc.thePlayer.posX;
                cameraPos[1] = (float) mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
                cameraPos[2] = (float) mc.thePlayer.posZ;
            } else {
                cameraPos[0] = 0;
                cameraPos[1] = 0;
                cameraPos[2] = 0;
            }
        } catch (Exception e) {
            cameraPos[0] = 0;
            cameraPos[1] = 0;
            cameraPos[2] = 0;
        }
    }

    @Override
    public void onDestroy() {
        if (renderer != null) {
            renderer.cleanup();
            renderer = null;
        }
        viewMatrixBuffer = null;
        projMatrixBuffer = null;
        TakoRenderMod.LOG.info("ParticleRenderSystem: Destroyed");
    }
}
