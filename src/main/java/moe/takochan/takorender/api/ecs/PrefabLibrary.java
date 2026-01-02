package moe.takochan.takorender.api.ecs;

import moe.takochan.takorender.api.component.LineRendererComponent;
import moe.takochan.takorender.api.component.LineRendererComponent.LineShape;
import moe.takochan.takorender.api.component.ParticleBufferComponent;
import moe.takochan.takorender.api.component.ParticleEmitterComponent;
import moe.takochan.takorender.api.component.ParticleRenderComponent;
import moe.takochan.takorender.api.component.ParticleStateComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.particle.BlendMode;
import moe.takochan.takorender.api.particle.ColorOverLifetime;
import moe.takochan.takorender.api.particle.ParticleForce;
import moe.takochan.takorender.api.particle.SizeOverLifetime;

/**
 * 预制件库 - 常用实体模板集合
 *
 * <p>
 * PrefabLibrary 提供了常用实体的预定义模板，
 * 可直接使用或作为自定义 Prefab 的参考。
 * </p>
 *
 * <p>
 * <b>使用方式</b>:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // 直接实例化
 *     Entity fire = PrefabLibrary.FIRE.instantiate(world);
 *
 *     // 设置位置
 *     fire.getComponent(TransformComponent.class)
 *         .ifPresent(t -> t.setPosition(100, 64, 200));
 * }
 * </pre>
 */
public final class PrefabLibrary {

    private PrefabLibrary() {}

    /**
     * 调试方块 - 红色线框立方体
     *
     * <p>
     * 用于可视化位置、碰撞盒等调试信息。
     * </p>
     */
    public static final Prefab DEBUG_BOX = new Prefab("DebugBox").add(TransformComponent::new)
        .add(
            () -> new LineRendererComponent().setShape(LineShape.BOX)
                .setSize(1.0f)
                .setColor(1.0f, 0.0f, 0.0f, 1.0f)
                .setLineWidth(2.0f));

    /**
     * 调试球体 - 绿色线框球体
     */
    public static final Prefab DEBUG_SPHERE = new Prefab("DebugSphere").add(TransformComponent::new)
        .add(
            () -> new LineRendererComponent().setShape(LineShape.SPHERE)
                .setSize(1.0f)
                .setColor(0.0f, 1.0f, 0.0f, 1.0f)
                .setLineWidth(1.5f));

    /**
     * 坐标轴 - RGB 坐标轴线条
     *
     * <p>
     * X=红色, Y=绿色, Z=蓝色。用于可视化方向。
     * </p>
     */
    public static final Prefab AXIS_HELPER = new Prefab("AxisHelper").add(TransformComponent::new)
        .add(
            () -> new LineRendererComponent().setShape(LineShape.LINE)
                .setLine(0, 0, 0, 1, 0, 0)
                .setColor(1.0f, 0.0f, 0.0f, 1.0f)
                .setLineWidth(2.0f));

    /**
     * 火焰粒子效果
     *
     * <p>
     * 预配置的火焰粒子系统，带有颜色渐变和正确的物理参数。
     * </p>
     */
    public static final Prefab FIRE_PARTICLE = new Prefab("FireParticle").add(TransformComponent::new)
        .add(() -> new ParticleBufferComponent(3000))
        .add(() -> {
            ParticleEmitterComponent emitter = new ParticleEmitterComponent();
            emitter.setRate(80);
            emitter.setLifetime(0.5f, 1.2f);
            emitter.setVelocity(0, 1.5f, 0);
            emitter.setVelocityVariation(0.3f);
            emitter.setSize(0.08f, 0.2f);
            emitter.setColorOverLifetime(ColorOverLifetime.fire());
            emitter.setSizeOverLifetime(SizeOverLifetime.shrink());
            emitter.addForce(ParticleForce.gravity(0, -3.0f, 0));
            emitter.addForce(ParticleForce.turbulence(1.5f, 0.8f));
            return emitter;
        })
        .add(
            () -> new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(0.9f)
                .setReceiveLighting(false))
        .add(() -> new ParticleStateComponent().setLooping(true));

    /**
     * 烟雾粒子效果
     *
     * <p>
     * 缓慢上升的灰色烟雾，接收世界光照。
     * </p>
     */
    public static final Prefab SMOKE_PARTICLE = new Prefab("SmokeParticle").add(TransformComponent::new)
        .add(() -> new ParticleBufferComponent(2000))
        .add(() -> {
            ParticleEmitterComponent emitter = new ParticleEmitterComponent();
            emitter.setRate(30);
            emitter.setLifetime(1.5f, 3.0f);
            emitter.setVelocity(0, 0.8f, 0);
            emitter.setVelocityVariation(0.2f);
            emitter.setSize(0.15f, 0.4f);
            emitter.setColorOverLifetime(ColorOverLifetime.smoke());
            emitter.setSizeOverLifetime(SizeOverLifetime.grow());
            emitter.addForce(ParticleForce.gravity(0, 0.5f, 0));
            emitter.addForce(ParticleForce.turbulence(0.5f, 0.3f));
            return emitter;
        })
        .add(
            () -> new ParticleRenderComponent().setBlendMode(BlendMode.ALPHA)
                .setReceiveLighting(true))
        .add(() -> new ParticleStateComponent().setLooping(true));

    /**
     * 火花粒子效果
     *
     * <p>
     * 快速闪烁的明亮火花，适用于焊接、碰撞等效果。
     * </p>
     */
    public static final Prefab SPARK_PARTICLE = new Prefab("SparkParticle").add(TransformComponent::new)
        .add(() -> new ParticleBufferComponent(1000))
        .add(() -> {
            ParticleEmitterComponent emitter = new ParticleEmitterComponent();
            emitter.setRate(50);
            emitter.setLifetime(0.2f, 0.5f);
            emitter.setVelocity(0, 3.0f, 0);
            emitter.setVelocityVariation(0.8f);
            emitter.setSize(0.02f, 0.05f);
            emitter.setColorOverLifetime(ColorOverLifetime.lightning());
            emitter.setSizeOverLifetime(SizeOverLifetime.shrink());
            emitter.addForce(ParticleForce.gravity(0, -15.0f, 0));
            return emitter;
        })
        .add(
            () -> new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(1.0f))
        .add(() -> new ParticleStateComponent().setLooping(true));

    /**
     * 创建自定义颜色的调试方块
     *
     * @param r 红色（0-1）
     * @param g 绿色（0-1）
     * @param b 蓝色（0-1）
     * @return 新的 Prefab
     */
    public static Prefab debugBox(float r, float g, float b) {
        return new Prefab("DebugBox").add(TransformComponent::new)
            .add(
                () -> new LineRendererComponent().setShape(LineShape.BOX)
                    .setSize(1.0f)
                    .setColor(r, g, b, 1.0f)
                    .setLineWidth(2.0f));
    }

    /**
     * 创建自定义尺寸的调试方块
     *
     * @param width  宽度
     * @param height 高度
     * @param depth  深度
     * @param r      红色（0-1）
     * @param g      绿色（0-1）
     * @param b      蓝色（0-1）
     * @return 新的 Prefab
     */
    public static Prefab debugBox(float width, float height, float depth, float r, float g, float b) {
        return new Prefab("DebugBox").add(TransformComponent::new)
            .add(
                () -> new LineRendererComponent().setShape(LineShape.BOX)
                    .setSize(width, height, depth)
                    .setColor(r, g, b, 1.0f)
                    .setLineWidth(2.0f));
    }
}
