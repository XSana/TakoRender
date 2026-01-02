package moe.takochan.takorender.api.particle;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.component.ParticleBufferComponent;
import moe.takochan.takorender.api.component.ParticleEmitterComponent;
import moe.takochan.takorender.api.component.ParticleRenderComponent;
import moe.takochan.takorender.api.component.ParticleStateComponent;
import moe.takochan.takorender.api.component.TransformComponent;
import moe.takochan.takorender.api.ecs.Entity;
import moe.takochan.takorender.api.ecs.World;

/**
 * 粒子预设效果工厂
 *
 * <p>
 * 提供常用粒子效果的快速配置方法。使用 ECS 组件模式。
 * </p>
 *
 * <p>
 * <b>使用示例:</b>
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // 创建火焰效果
 *     Entity fireEntity = world.createEntity();
 *     fireEntity.addComponent(new TransformComponent().setPosition(x, y, z));
 *     ParticlePresets.applyFire(fireEntity, 1.0f);
 *
 *     // 创建爆炸效果（一次性）
 *     Entity explosion = world.createEntity();
 *     explosion.addComponent(new TransformComponent().setPosition(x, y, z));
 *     ParticlePresets.applyExplosion(explosion, 2.0f);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class ParticlePresets {

    private ParticlePresets() {}

    /**
     * 应用火焰效果
     *
     * @param entity    目标实体
     * @param intensity 强度 (0.5-2.0)
     */
    public static void applyFire(Entity entity, float intensity) {
        entity.addComponent(new ParticleBufferComponent((int) (5000 * intensity)));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.CIRCLE, 0.3f * intensity)
                .setRate(100 * intensity)
                .setLifetime(0.5f, 1.5f)
                .setVelocity(0, 3f * intensity, 0)
                .setVelocityVariation(0.3f)
                .setSize(0.1f, 0.3f)
                .setColorOverLifetime(ColorOverLifetime.fire())
                .setSizeOverLifetime(SizeOverLifetime.shrink())
                .setVelocityOverLifetime(VelocityOverLifetime.smoke())
                .addForce(ParticleForce.turbulence(2.0f, 1.0f * intensity))
                .addForce(ParticleForce.gravity(0, -2f, 0)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(0.9f)
                .setReceiveLighting(false));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用火把效果（小型火焰）
     *
     * @param entity 目标实体
     */
    public static void applyTorch(Entity entity) {
        entity.addComponent(new ParticleBufferComponent(500));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.POINT)
                .setRate(30)
                .setLifetime(0.3f, 0.8f)
                .setVelocity(0, 1.5f, 0)
                .setVelocityVariation(0.2f)
                .setSize(0.05f, 0.1f)
                .setColorOverLifetime(ColorOverLifetime.fire())
                .setSizeOverLifetime(SizeOverLifetime.shrink())
                .setVelocityOverLifetime(VelocityOverLifetime.smoke())
                .addForce(ParticleForce.turbulence(3.0f, 0.5f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(0.8f)
                .setReceiveLighting(false));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用烟雾效果
     *
     * @param entity  目标实体
     * @param density 密度 (0.5-2.0)
     */
    public static void applySmoke(Entity entity, float density) {
        entity.addComponent(new ParticleBufferComponent((int) (3000 * density)));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.CIRCLE, 0.2f)
                .setRate(50 * density)
                .setLifetime(2.0f, 4.0f)
                .setVelocity(0, 0.5f, 0)
                .setVelocityVariation(0.4f)
                .setSize(0.2f, 0.5f)
                .setColorOverLifetime(ColorOverLifetime.smoke())
                .setSizeOverLifetime(SizeOverLifetime.smoke())
                .setVelocityOverLifetime(VelocityOverLifetime.smoke())
                .addForce(ParticleForce.turbulence(1.0f, 0.3f))
                .addForce(ParticleForce.wind(0.5f, 0, 0, 0.3f, 0.2f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ALPHA)
                .setReceiveLighting(true));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用爆炸效果
     *
     * @param entity 目标实体
     * @param scale  规模 (0.5-3.0)
     */
    public static void applyExplosion(Entity entity, float scale) {
        entity.addComponent(new ParticleBufferComponent((int) (2000 * scale)));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.SPHERE, 0.1f * scale)
                .setBurst((int) (300 * scale))
                .setLifetime(0.3f, 1.5f)
                .setSpeed(12f * scale)
                .setVelocityVariation(0.4f)
                .setSize(0.2f * scale, 0.5f * scale)
                .setColorOverLifetime(ColorOverLifetime.explosion())
                .setSizeOverLifetime(SizeOverLifetime.explosion())
                .setVelocityOverLifetime(VelocityOverLifetime.explosion())
                .addForce(ParticleForce.drag(2.0f))
                .addForce(ParticleForce.gravity(0, -3f, 0)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(1.0f)
                .setReceiveLighting(false));

        entity.addComponent(
            new ParticleStateComponent().setLooping(false)
                .setDuration(3.0f));
    }

    /**
     * 应用能量球效果
     *
     * @param entity 目标实体
     * @param color  颜色 (0xRRGGBB)
     */
    public static void applyEnergyOrb(Entity entity, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        entity.addComponent(new ParticleBufferComponent(1000));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.SPHERE_SURFACE, 0.5f)
                .setRate(100)
                .setLifetime(0.5f, 1.0f)
                .setSpeed(-1.0f)
                .setSize(0.05f, 0.1f)
                .setColor(r, g, b, 1.0f)
                .setColorOverLifetime(ColorOverLifetime.energy())
                .setSizeOverLifetime(SizeOverLifetime.converge())
                .setRotationOverLifetime(RotationOverLifetime.spin())
                .addForce(ParticleForce.attractor(0, 0, 0, 5.0f, 2.0f))
                .addForce(ParticleForce.vortexY(0, 0, 0, 3.0f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(0.8f));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用传送门效果
     *
     * @param entity 目标实体
     */
    public static void applyPortal(Entity entity) {
        entity.addComponent(new ParticleBufferComponent(3000));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.RING, 1.0f, 0.8f)
                .setRate(200)
                .setLifetime(0.5f, 1.5f)
                .setVelocity(0, 0.5f, 0)
                .setSize(0.08f, 0.12f)
                .setColorOverLifetime(ColorOverLifetime.portal())
                .setSizeOverLifetime(SizeOverLifetime.pulse())
                .setRotationOverLifetime(RotationOverLifetime.slow())
                .addForce(ParticleForce.vortexY(0, 0, 0, 5.0f))
                .addForce(ParticleForce.curlNoise(2.0f, 0.5f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(0.7f));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用治愈效果
     *
     * @param entity 目标实体
     */
    public static void applyHealing(Entity entity) {
        entity.addComponent(new ParticleBufferComponent(500));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.CYLINDER, 0.5f, 0.1f)
                .setRate(30)
                .setLifetime(1.5f, 2.5f)
                .setVelocity(0, 1.5f, 0)
                .setVelocityVariation(0.2f)
                .setSize(0.1f, 0.2f)
                .setColorOverLifetime(ColorOverLifetime.healing())
                .setSizeOverLifetime(SizeOverLifetime.pulse())
                .setVelocityOverLifetime(VelocityOverLifetime.float_())
                .addForce(ParticleForce.turbulence(1.5f, 0.3f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(0.6f));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用下雨效果
     *
     * @param entity    目标实体
     * @param area      覆盖区域大小
     * @param intensity 强度 (0.5-2.0)
     */
    public static void applyRain(Entity entity, float area, float intensity) {
        entity.addComponent(new ParticleBufferComponent((int) (10000 * intensity)));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.RECTANGLE, area, area)
                .setRate(500 * intensity)
                .setLifetime(1.5f, 2.5f)
                .setVelocity(0, -15f, 0)
                .setVelocityVariation(0.1f)
                .setSize(0.02f, 0.15f)
                .setColor(0.7f, 0.8f, 1.0f, 0.5f)
                .setCollisionMode(CollisionMode.PLANE)
                .setCollisionResponse(CollisionResponse.KILL)
                .addForce(ParticleForce.wind(0.5f, 0, 0, 0.5f, 0.3f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ALPHA)
                .setRenderMode(RenderMode.STRETCHED_BILLBOARD));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用下雪效果
     *
     * @param entity    目标实体
     * @param area      覆盖区域大小
     * @param intensity 强度 (0.5-2.0)
     */
    public static void applySnow(Entity entity, float area, float intensity) {
        entity.addComponent(new ParticleBufferComponent((int) (5000 * intensity)));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.RECTANGLE, area, area)
                .setRate(100 * intensity)
                .setLifetime(5.0f, 10.0f)
                .setVelocity(0, -1.0f, 0)
                .setVelocityVariation(0.3f)
                .setSize(0.05f, 0.15f)
                .setColor(1.0f, 1.0f, 1.0f, 0.8f)
                .setAngularVelocity(-1.0f, 1.0f)
                .setRotationOverLifetime(RotationOverLifetime.leaf())
                .addForce(ParticleForce.turbulence(0.5f, 0.5f))
                .addForce(ParticleForce.wind(0.3f, 0, 0.2f, 0.3f, 0.2f)));

        entity.addComponent(new ParticleRenderComponent().setBlendMode(BlendMode.ALPHA));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用火花效果
     *
     * @param entity 目标实体
     */
    public static void applySparks(Entity entity) {
        entity.addComponent(new ParticleBufferComponent(500));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.CONE, 0.05f, 0.5f, 0.1f)
                .setRate(100)
                .setLifetime(0.3f, 0.8f)
                .setSpeed(6.0f)
                .setVelocityVariation(0.5f)
                .setSize(0.02f, 0.05f)
                .setColor(1.0f, 0.8f, 0.3f, 1.0f)
                .setSizeOverLifetime(SizeOverLifetime.spark())
                .setVelocityOverLifetime(VelocityOverLifetime.spark())
                .addForce(ParticleForce.gravity(0, -9.8f, 0))
                .addForce(ParticleForce.drag(0.3f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(0.9f));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用光环效果
     *
     * @param entity 目标实体
     * @param color  颜色 (0xRRGGBB)
     * @param radius 半径
     */
    public static void applyAura(Entity entity, int color, float radius) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        entity.addComponent(new ParticleBufferComponent(1000));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.SPHERE_SURFACE, radius)
                .setRate(50)
                .setLifetime(1.0f, 2.0f)
                .setSpeed(0)
                .setSize(0.05f, 0.1f)
                .setColor(r, g, b, 0.8f)
                .setColorOverLifetime(ColorOverLifetime.fadeOut())
                .setSizeOverLifetime(SizeOverLifetime.pulse())
                .setVelocityOverLifetime(VelocityOverLifetime.pulse())
                .addForce(ParticleForce.vortexY(0, 0, 0, 1.0f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(0.5f));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用水花效果
     *
     * @param entity 目标实体
     * @param scale  规模
     */
    public static void applyWaterSplash(Entity entity, float scale) {
        entity.addComponent(new ParticleBufferComponent((int) (500 * scale)));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.HEMISPHERE, 0.2f * scale)
                .setBurst((int) (100 * scale))
                .setLifetime(0.5f, 1.5f)
                .setSpeed(5.0f * scale)
                .setVelocityVariation(0.4f)
                .setSize(0.05f * scale, 0.15f * scale)
                .setColorOverLifetime(ColorOverLifetime.water())
                .setSizeOverLifetime(SizeOverLifetime.shrink())
                .setVelocityOverLifetime(VelocityOverLifetime.fastDecelerate())
                .addForce(ParticleForce.gravity(0, -9.8f, 0))
                .addForce(ParticleForce.drag(0.3f)));

        entity.addComponent(new ParticleRenderComponent().setBlendMode(BlendMode.ALPHA));

        entity.addComponent(
            new ParticleStateComponent().setLooping(false)
                .setDuration(2.0f));
    }

    /**
     * 应用气泡效果
     *
     * @param entity 目标实体
     */
    public static void applyBubbles(Entity entity) {
        entity.addComponent(new ParticleBufferComponent(200));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.CIRCLE, 0.3f)
                .setRate(20)
                .setLifetime(2.0f, 4.0f)
                .setVelocity(0, 1.0f, 0)
                .setVelocityVariation(0.3f)
                .setSize(0.05f, 0.15f)
                .setColor(0.8f, 0.9f, 1.0f, 0.5f)
                .setSizeOverLifetime(SizeOverLifetime.bubble())
                .setVelocityOverLifetime(VelocityOverLifetime.slowDecelerate())
                .addForce(ParticleForce.turbulence(1.0f, 0.3f)));

        entity.addComponent(new ParticleRenderComponent().setBlendMode(BlendMode.ALPHA));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用电击效果
     *
     * @param entity 目标实体
     */
    public static void applyElectric(Entity entity) {
        entity.addComponent(new ParticleBufferComponent(500));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.SPHERE, 0.3f)
                .setRate(150)
                .setLifetime(0.05f, 0.2f)
                .setSpeed(3.0f)
                .setVelocityVariation(0.8f)
                .setSize(0.02f, 0.05f)
                .setColorOverLifetime(ColorOverLifetime.lightning())
                .setSizeOverLifetime(SizeOverLifetime.flicker())
                .setRotationOverLifetime(RotationOverLifetime.erratic()));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ADDITIVE)
                .setEmissive(1.0f));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用蒸汽效果
     *
     * @param entity 目标实体
     */
    public static void applySteam(Entity entity) {
        entity.addComponent(new ParticleBufferComponent(1000));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.CIRCLE, 0.1f)
                .setRate(50)
                .setLifetime(1.0f, 2.5f)
                .setVelocity(0, 2.5f, 0)
                .setVelocityVariation(0.3f)
                .setSize(0.1f, 0.3f)
                .setColor(1.0f, 1.0f, 1.0f, 0.4f)
                .setSizeOverLifetime(SizeOverLifetime.smoke())
                .setVelocityOverLifetime(VelocityOverLifetime.smoke())
                .addForce(ParticleForce.turbulence(1.5f, 0.5f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ALPHA)
                .setReceiveLighting(true));

        entity.addComponent(new ParticleStateComponent().setLooping(true));
    }

    /**
     * 应用碎片爆炸效果
     *
     * @param entity 目标实体
     * @param scale  规模
     */
    public static void applyDebrisExplosion(Entity entity, float scale) {
        entity.addComponent(new ParticleBufferComponent((int) (300 * scale)));

        entity.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.SPHERE, 0.2f * scale)
                .setBurst((int) (100 * scale))
                .setLifetime(2.0f, 4.0f)
                .setSpeed(10f * scale)
                .setVelocityVariation(0.5f)
                .setSize(0.08f * scale, 0.2f * scale)
                .setColorOverLifetime(ColorOverLifetime.debris())
                .setAngularVelocity(-5.0f, 5.0f)
                .setVelocityOverLifetime(VelocityOverLifetime.decelerate())
                .setRotationOverLifetime(RotationOverLifetime.debris())
                .addForce(ParticleForce.gravity(0, -9.8f, 0))
                .addForce(ParticleForce.drag(0.2f)));

        entity.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ALPHA)
                .setRenderMode(RenderMode.MESH));

        entity.addComponent(
            new ParticleStateComponent().setLooping(false)
                .setDuration(5.0f));
    }

    /**
     * 创建多层火焰效果
     *
     * <p>
     * 包含三层: 火焰核心 + 烟雾 + 火星
     * </p>
     *
     * @param world     ECS World
     * @param position  位置
     * @param intensity 强度 (0.5-2.0)
     * @return 创建的实体列表
     */
    public static List<Entity> createLayeredFire(World world, Vector3f position, float intensity) {
        List<Entity> entities = new ArrayList<>();

        Entity fireCore = world.createEntity();
        TransformComponent fireTrans = new TransformComponent();
        fireTrans.setPosition(position.x, position.y, position.z);
        fireCore.addComponent(fireTrans);
        applyFire(fireCore, intensity);
        entities.add(fireCore);

        Entity smoke = world.createEntity();
        TransformComponent smokeTrans = new TransformComponent();
        smokeTrans.setPosition(position.x, position.y + 0.5f, position.z);
        smoke.addComponent(smokeTrans);
        applySmoke(smoke, intensity * 0.3f);
        entities.add(smoke);

        Entity sparks = world.createEntity();
        TransformComponent sparksTrans = new TransformComponent();
        sparksTrans.setPosition(position.x, position.y, position.z);
        sparks.addComponent(sparksTrans);
        applySparks(sparks);
        entities.add(sparks);

        return entities;
    }

    /**
     * 创建多层爆炸效果
     *
     * <p>
     * 包含三层: 爆炸闪光 + 碎片 + 烟雾
     * </p>
     *
     * @param world    ECS World
     * @param position 位置
     * @param scale    规模 (0.5-3.0)
     * @return 创建的实体列表
     */
    public static List<Entity> createLayeredExplosion(World world, Vector3f position, float scale) {
        List<Entity> entities = new ArrayList<>();

        Entity flash = world.createEntity();
        TransformComponent flashTrans = new TransformComponent();
        flashTrans.setPosition(position.x, position.y, position.z);
        flash.addComponent(flashTrans);
        applyExplosion(flash, scale);
        entities.add(flash);

        Entity debris = world.createEntity();
        TransformComponent debrisTrans = new TransformComponent();
        debrisTrans.setPosition(position.x, position.y, position.z);
        debris.addComponent(debrisTrans);
        applyDebrisExplosion(debris, scale);
        entities.add(debris);

        Entity smoke = world.createEntity();
        TransformComponent smokeTrans = new TransformComponent();
        smokeTrans.setPosition(position.x, position.y, position.z);
        smoke.addComponent(smokeTrans);
        smoke.addComponent(new ParticleBufferComponent((int) (1000 * scale)));
        smoke.addComponent(
            new ParticleEmitterComponent().setShape(EmitterShape.SPHERE, 0.5f * scale)
                .setRate(0)
                .setBurst((int) (50 * scale))
                .setLifetime(2.0f, 4.0f)
                .setVelocity(0, 1.0f, 0)
                .setVelocityVariation(0.5f)
                .setSize(0.3f * scale, 0.8f * scale)
                .setColorOverLifetime(ColorOverLifetime.smoke())
                .setSizeOverLifetime(SizeOverLifetime.smoke())
                .setVelocityOverLifetime(VelocityOverLifetime.smoke())
                .addForce(ParticleForce.turbulence(1.0f, 0.5f)));
        smoke.addComponent(
            new ParticleRenderComponent().setBlendMode(BlendMode.ALPHA)
                .setReceiveLighting(true));
        smoke.addComponent(
            new ParticleStateComponent().setLooping(false)
                .setDuration(5.0f));
        entities.add(smoke);

        return entities;
    }

    /**
     * 创建魔法光环效果
     *
     * <p>
     * 包含两层: 内层光圈 + 外层粒子
     * </p>
     *
     * @param world    ECS World
     * @param position 位置
     * @param color    颜色 (0xRRGGBB)
     * @param radius   半径
     * @return 创建的实体列表
     */
    public static List<Entity> createLayeredAura(World world, Vector3f position, int color, float radius) {
        List<Entity> entities = new ArrayList<>();

        Entity innerRing = world.createEntity();
        TransformComponent innerTrans = new TransformComponent();
        innerTrans.setPosition(position.x, position.y, position.z);
        innerRing.addComponent(innerTrans);
        applyEnergyOrb(innerRing, color);
        entities.add(innerRing);

        Entity outerParticles = world.createEntity();
        TransformComponent outerTrans = new TransformComponent();
        outerTrans.setPosition(position.x, position.y, position.z);
        outerParticles.addComponent(outerTrans);
        applyAura(outerParticles, color, radius);
        entities.add(outerParticles);

        return entities;
    }
}
