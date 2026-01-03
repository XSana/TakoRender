package moe.takochan.takorender.api.system;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.LayerComponent;
import moe.takochan.takorender.api.component.SpriteRendererComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.component.VisibilityComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.GameSystem;
import moe.takochan.takorender.api.ecs.Layer;
import moe.takochan.takorender.api.ecs.Phase;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.graphics.batch.SpriteBatch;
import moe.takochan.takorender.core.gl.GLStateContext;

/**
 * 2D 精灵渲染系统 - 负责渲染所有拥有 SpriteRendererComponent 的实体
 *
 * <p>
 * SpriteRenderSystem 在 RENDER 阶段执行，使用 SpriteBatch 进行批量渲染。
 * 适用于 GUI 元素、2D 图形、HUD 等场景。
 * </p>
 *
 * <p>
 * <b>渲染特性</b>:
 * </p>
 * <ul>
 * <li>使用正交投影（屏幕坐标）</li>
 * <li>按 sortingOrder 排序渲染</li>
 * <li>禁用深度测试（2D 叠加渲染）</li>
 * <li>启用 Alpha 混合</li>
 * </ul>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     World guiWorld = RenderSystem.getWorld("gui");
 *     guiWorld.addSystem(new SpriteRenderSystem());
 *
 *     Entity panel = guiWorld.createEntity();
 *     panel.addComponent(new TransformComponent(50, 50, 0));
 *     panel.addComponent(
 *         new SpriteRendererComponent().setSize(200, 100)
 *             .setColor(0.2f, 0.2f, 0.2f, 0.8f));
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
@RequiresComponent(SpriteRendererComponent.class)
public class SpriteRenderSystem extends GameSystem {

    /** 批量渲染器（内部实现） */
    private SpriteBatch batch;

    /** 用于排序的临时列表（复用避免每帧分配） */
    private final List<Entity> sortedEntities = new ArrayList<>();

    /** 缓存的屏幕尺寸 */
    private int screenWidth = 0;
    private int screenHeight = 0;

    @Override
    public Phase getPhase() {
        return Phase.RENDER;
    }

    @Override
    public int getPriority() {
        // 在 LineRenderSystem (300) 之后
        return 400;
    }

    @Override
    public void update(float deltaTime) {
        List<Entity> entities = getRequiredEntities();
        if (entities.isEmpty()) {
            return;
        }

        // 延迟初始化批量渲染器
        if (batch == null) {
            batch = new SpriteBatch();
        }

        // 获取当前屏幕尺寸
        int width = GL11.glGetInteger(GL11.GL_VIEWPORT + 2);
        int height = GL11.glGetInteger(GL11.GL_VIEWPORT + 3);

        if (width <= 0 || height <= 0) {
            return;
        }

        // 更新投影矩阵（如果尺寸变化）
        if (width != screenWidth || height != screenHeight) {
            screenWidth = width;
            screenHeight = height;
        }

        // 收集并排序可见实体
        sortedEntities.clear();
        Layer currentLayer = getWorld().getCurrentLayer();

        for (Entity entity : entities) {
            // 检查可见性
            VisibilityComponent visibility = entity.getComponent(VisibilityComponent.class)
                .orElse(null);
            if (visibility != null && !visibility.shouldRender()) {
                continue;
            }

            // 检查 Layer 筛选（Sprite 通常是 HUD 或 GUI）
            if (currentLayer != null) {
                Layer entityLayer = entity.getComponent(LayerComponent.class)
                    .map(LayerComponent::getLayer)
                    .orElse(Layer.WORLD_3D);
                if (entityLayer != currentLayer) {
                    continue;
                }
            }

            SpriteRendererComponent sprite = entity.getComponent(SpriteRendererComponent.class)
                .orElse(null);
            if (sprite != null) {
                sortedEntities.add(entity);
            }
        }

        if (sortedEntities.isEmpty()) {
            return;
        }

        // 按 sortingOrder 排序（小的先渲染，在下层）
        sortedEntities.sort(
            Comparator.comparingInt(
                e -> e.getComponent(SpriteRendererComponent.class)
                    .map(SpriteRendererComponent::getSortingOrder)
                    .orElse(0)));

        // 使用 GLStateContext 管理 GL 状态
        try (GLStateContext ctx = GLStateContext.begin()) {
            ctx.disableDepthTest();
            ctx.enableBlend();
            ctx.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ctx.disableCullFace();
            ctx.disableLighting();

            // 设置正交投影
            batch.setProjectionOrtho(screenWidth, screenHeight);
            batch.begin();

            for (Entity entity : sortedEntities) {
                renderSprite(entity);
            }

            batch.end();
        }

        sortedEntities.clear();
    }

    /**
     * 渲染单个精灵实体
     */
    private void renderSprite(Entity entity) {
        SpriteRendererComponent sprite = entity.getComponent(SpriteRendererComponent.class)
            .orElse(null);
        TransformComponent transform = entity.getComponent(TransformComponent.class)
            .orElse(null);

        if (sprite == null || transform == null) {
            return;
        }

        // 获取屏幕坐标（使用 Transform 的位置作为左上角）
        float x = transform.getPosition().x;
        float y = transform.getPosition().y;
        float w = sprite.getWidth();
        float h = sprite.getHeight();

        float r = sprite.getColorR();
        float g = sprite.getColorG();
        float b = sprite.getColorB();
        float a = sprite.getColorA();

        // 绘制矩形
        batch.drawRect(x, y, w, h, r, g, b, a);
    }

    @Override
    public void onDestroy() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
    }
}
