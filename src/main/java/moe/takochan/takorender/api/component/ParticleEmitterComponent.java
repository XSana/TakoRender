package moe.takochan.takorender.api.component;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.particle.CollisionMode;
import moe.takochan.takorender.api.particle.CollisionResponse;
import moe.takochan.takorender.api.particle.ColorOverLifetime;
import moe.takochan.takorender.api.particle.EmitterShape;
import moe.takochan.takorender.api.particle.ParticleEmitter;
import moe.takochan.takorender.api.particle.ParticleForce;
import moe.takochan.takorender.api.particle.RotationOverLifetime;
import moe.takochan.takorender.api.particle.SimulationSpace;
import moe.takochan.takorender.api.particle.SizeOverLifetime;
import moe.takochan.takorender.api.particle.VelocityOverLifetime;

/**
 * 粒子发射器组件 - 纯数据
 *
 * <p>
 * 存储粒子发射参数。逻辑由 ParticleEmitSystem 处理。
 * </p>
 */
@SideOnly(Side.CLIENT)
public class ParticleEmitterComponent extends Component {

    /** 发射形状 */
    private EmitterShape shape = EmitterShape.POINT;

    /** 形状参数 */
    private float shapeParam1 = 1.0f;
    private float shapeParam2 = 0;
    private float shapeParam3 = 0;

    /** 是否从表面发射 */
    private boolean emitFromSurface = false;

    /** 是否沿表面法线方向发射 */
    private boolean emitAlongNormal = true;

    /** 每秒发射数量 */
    private float emissionRate = 10;

    /** 一次性发射数量 */
    private int burstCount = 0;

    /** Burst 触发间隔 */
    private float burstInterval = 0;

    /** 是否启用发射 */
    private boolean emitting = true;

    /** 生命周期范围 */
    private float lifetimeMin = 1.0f;
    private float lifetimeMax = 1.0f;

    /** 速度 */
    private float velocityX = 0;
    private float velocityY = 0;
    private float velocityZ = 0;

    /** 径向速度 */
    private float speed = 0;

    /** 速度变化 */
    private float velocityVariation = 0;

    /** 大小范围 */
    private float sizeMin = 0.1f;
    private float sizeMax = 0.1f;

    /** 颜色 */
    private float colorR = 1;
    private float colorG = 1;
    private float colorB = 1;
    private float colorA = 1;

    /** 旋转范围 */
    private float rotationMin = 0;
    private float rotationMax = 0;

    /** 角速度范围 */
    private float angularVelocityMin = 0;
    private float angularVelocityMax = 0;

    /** 模拟空间 */
    private SimulationSpace simulationSpace = SimulationSpace.WORLD;

    /** 颜色生命周期曲线 */
    private ColorOverLifetime colorOverLifetime;

    /** 大小生命周期曲线 */
    private SizeOverLifetime sizeOverLifetime;

    /** 速度生命周期曲线 */
    private VelocityOverLifetime velocityOverLifetime;

    /** 旋转生命周期曲线 */
    private RotationOverLifetime rotationOverLifetime;

    /** 力场列表 */
    private final List<ParticleForce> forces = new ArrayList<>();

    /** 碰撞模式 */
    private CollisionMode collisionMode = CollisionMode.NONE;

    /** 碰撞响应 */
    private CollisionResponse collisionResponse = CollisionResponse.KILL;

    /** 碰撞参数 */
    private float bounciness = 0.5f;
    private float friction = 0.1f;
    private float bounceChance = 1.0f;
    private float bounceSpread = 0.0f;

    /** 碰撞平面 */
    private float collisionPlaneNX = 0;
    private float collisionPlaneNY = 1;
    private float collisionPlaneNZ = 0;
    private float collisionPlaneD = 0;

    /** 碰撞球体 */
    private float collisionSphereCenterX = 0;
    private float collisionSphereCenterY = 0;
    private float collisionSphereCenterZ = 0;
    private float collisionSphereRadius = 1;

    /** 碰撞盒 */
    private float collisionBoxMinX = -1;
    private float collisionBoxMinY = -1;
    private float collisionBoxMinZ = -1;
    private float collisionBoxMaxX = 1;
    private float collisionBoxMaxY = 1;
    private float collisionBoxMaxZ = 1;

    /** 子发射器 */
    private final List<ParticleEmitter.SubEmitterEntry> subEmitters = new ArrayList<>();

    public ParticleEmitterComponent() {}

    /**
     * 从 ParticleEmitter 创建组件
     */
    public static ParticleEmitterComponent fromEmitter(ParticleEmitter emitter) {
        ParticleEmitterComponent c = new ParticleEmitterComponent();
        // 形状
        c.shape = emitter.getShape();
        c.shapeParam1 = emitter.getShapeParam1();
        c.shapeParam2 = emitter.getShapeParam2();
        c.shapeParam3 = emitter.getShapeParam3();
        c.emitFromSurface = emitter.isEmitFromSurface();
        c.emitAlongNormal = emitter.isEmitAlongNormal();
        // 发射控制
        c.emissionRate = emitter.getEmissionRate();
        c.burstCount = emitter.getBurstCount();
        c.burstInterval = emitter.getBurstInterval();
        c.emitting = emitter.isEmitting();
        // 生命周期
        c.lifetimeMin = emitter.getLifetimeMin();
        c.lifetimeMax = emitter.getLifetimeMax();
        // 速度
        c.velocityX = emitter.getVelocityX();
        c.velocityY = emitter.getVelocityY();
        c.velocityZ = emitter.getVelocityZ();
        c.speed = emitter.getSpeed();
        c.velocityVariation = emitter.getVelocityVariation();
        // 大小
        c.sizeMin = emitter.getSizeMin();
        c.sizeMax = emitter.getSizeMax();
        // 颜色
        c.colorR = emitter.getColorR();
        c.colorG = emitter.getColorG();
        c.colorB = emitter.getColorB();
        c.colorA = emitter.getColorA();
        // 旋转
        c.rotationMin = emitter.getRotationMin();
        c.rotationMax = emitter.getRotationMax();
        c.angularVelocityMin = emitter.getAngularVelocityMin();
        c.angularVelocityMax = emitter.getAngularVelocityMax();
        // 生命周期曲线
        c.colorOverLifetime = emitter.getColorOverLifetime();
        c.sizeOverLifetime = emitter.getSizeOverLifetime();
        // 力场
        c.forces.addAll(emitter.getForces());
        // 碰撞
        c.collisionMode = emitter.getCollisionMode();
        c.collisionResponse = emitter.getCollisionResponse();
        c.bounciness = emitter.getBounciness();
        c.friction = emitter.getFriction();
        c.bounceChance = emitter.getBounceChance();
        c.bounceSpread = emitter.getBounceSpread();
        c.collisionPlaneNX = emitter.getCollisionPlaneNX();
        c.collisionPlaneNY = emitter.getCollisionPlaneNY();
        c.collisionPlaneNZ = emitter.getCollisionPlaneNZ();
        c.collisionPlaneD = emitter.getCollisionPlaneD();
        // 子发射器
        c.subEmitters.addAll(emitter.getSubEmitters());
        return c;
    }

    // ==================== 形状 ====================

    public EmitterShape getShape() {
        return shape;
    }

    public ParticleEmitterComponent setShape(EmitterShape shape) {
        this.shape = shape;
        return this;
    }

    public ParticleEmitterComponent setShape(EmitterShape shape, float param1) {
        this.shape = shape;
        this.shapeParam1 = param1;
        return this;
    }

    public ParticleEmitterComponent setShape(EmitterShape shape, float param1, float param2) {
        this.shape = shape;
        this.shapeParam1 = param1;
        this.shapeParam2 = param2;
        return this;
    }

    public ParticleEmitterComponent setShape(EmitterShape shape, float param1, float param2, float param3) {
        this.shape = shape;
        this.shapeParam1 = param1;
        this.shapeParam2 = param2;
        this.shapeParam3 = param3;
        return this;
    }

    public float getShapeParam1() {
        return shapeParam1;
    }

    public float getShapeParam2() {
        return shapeParam2;
    }

    public float getShapeParam3() {
        return shapeParam3;
    }

    public boolean isEmitFromSurface() {
        return emitFromSurface;
    }

    public ParticleEmitterComponent setEmitFromSurface(boolean emitFromSurface) {
        this.emitFromSurface = emitFromSurface;
        return this;
    }

    public boolean isEmitAlongNormal() {
        return emitAlongNormal;
    }

    public ParticleEmitterComponent setEmitAlongNormal(boolean emitAlongNormal) {
        this.emitAlongNormal = emitAlongNormal;
        return this;
    }

    // ==================== 发射控制 ====================

    public float getEmissionRate() {
        return emissionRate;
    }

    public ParticleEmitterComponent setRate(float rate) {
        this.emissionRate = rate;
        return this;
    }

    public int getBurstCount() {
        return burstCount;
    }

    public ParticleEmitterComponent setBurst(int count) {
        this.burstCount = count;
        return this;
    }

    public float getBurstInterval() {
        return burstInterval;
    }

    public ParticleEmitterComponent setBurstInterval(float interval) {
        this.burstInterval = interval;
        return this;
    }

    public boolean isEmitting() {
        return emitting;
    }

    public ParticleEmitterComponent setEmitting(boolean emitting) {
        this.emitting = emitting;
        return this;
    }

    // ==================== 生命周期 ====================

    public float getLifetimeMin() {
        return lifetimeMin;
    }

    public float getLifetimeMax() {
        return lifetimeMax;
    }

    public ParticleEmitterComponent setLifetime(float lifetime) {
        this.lifetimeMin = lifetime;
        this.lifetimeMax = lifetime;
        return this;
    }

    public ParticleEmitterComponent setLifetime(float min, float max) {
        this.lifetimeMin = min;
        this.lifetimeMax = max;
        return this;
    }

    // ==================== 速度 ====================

    public float getVelocityX() {
        return velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public float getVelocityZ() {
        return velocityZ;
    }

    public ParticleEmitterComponent setVelocity(float x, float y, float z) {
        this.velocityX = x;
        this.velocityY = y;
        this.velocityZ = z;
        return this;
    }

    public float getSpeed() {
        return speed;
    }

    public ParticleEmitterComponent setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    public float getVelocityVariation() {
        return velocityVariation;
    }

    public ParticleEmitterComponent setVelocityVariation(float variation) {
        this.velocityVariation = Math.max(0, Math.min(1, variation));
        return this;
    }

    // ==================== 大小 ====================

    public float getSizeMin() {
        return sizeMin;
    }

    public float getSizeMax() {
        return sizeMax;
    }

    public ParticleEmitterComponent setSize(float size) {
        this.sizeMin = size;
        this.sizeMax = size;
        return this;
    }

    public ParticleEmitterComponent setSize(float min, float max) {
        this.sizeMin = min;
        this.sizeMax = max;
        return this;
    }

    // ==================== 颜色 ====================

    public float getColorR() {
        return colorR;
    }

    public float getColorG() {
        return colorG;
    }

    public float getColorB() {
        return colorB;
    }

    public float getColorA() {
        return colorA;
    }

    public ParticleEmitterComponent setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
        return this;
    }

    // ==================== 旋转 ====================

    public float getRotationMin() {
        return rotationMin;
    }

    public float getRotationMax() {
        return rotationMax;
    }

    public ParticleEmitterComponent setRotation(float min, float max) {
        this.rotationMin = min;
        this.rotationMax = max;
        return this;
    }

    public float getAngularVelocityMin() {
        return angularVelocityMin;
    }

    public float getAngularVelocityMax() {
        return angularVelocityMax;
    }

    public ParticleEmitterComponent setAngularVelocity(float min, float max) {
        this.angularVelocityMin = min;
        this.angularVelocityMax = max;
        return this;
    }

    // ==================== 模拟空间 ====================

    public SimulationSpace getSimulationSpace() {
        return simulationSpace;
    }

    public ParticleEmitterComponent setSimulationSpace(SimulationSpace space) {
        this.simulationSpace = space;
        return this;
    }

    // ==================== 生命周期曲线 ====================

    public ColorOverLifetime getColorOverLifetime() {
        return colorOverLifetime;
    }

    public ParticleEmitterComponent setColorOverLifetime(ColorOverLifetime curve) {
        this.colorOverLifetime = curve;
        return this;
    }

    public SizeOverLifetime getSizeOverLifetime() {
        return sizeOverLifetime;
    }

    public ParticleEmitterComponent setSizeOverLifetime(SizeOverLifetime curve) {
        this.sizeOverLifetime = curve;
        return this;
    }

    public VelocityOverLifetime getVelocityOverLifetime() {
        return velocityOverLifetime;
    }

    public ParticleEmitterComponent setVelocityOverLifetime(VelocityOverLifetime curve) {
        this.velocityOverLifetime = curve;
        return this;
    }

    public RotationOverLifetime getRotationOverLifetime() {
        return rotationOverLifetime;
    }

    public ParticleEmitterComponent setRotationOverLifetime(RotationOverLifetime curve) {
        this.rotationOverLifetime = curve;
        return this;
    }

    // ==================== 力场 ====================

    public List<ParticleForce> getForces() {
        return forces;
    }

    public ParticleEmitterComponent addForce(ParticleForce force) {
        if (force != null) {
            forces.add(force);
        }
        return this;
    }

    public ParticleEmitterComponent clearForces() {
        forces.clear();
        return this;
    }

    // ==================== 碰撞 ====================

    public CollisionMode getCollisionMode() {
        return collisionMode;
    }

    public ParticleEmitterComponent setCollisionMode(CollisionMode mode) {
        this.collisionMode = mode;
        return this;
    }

    public CollisionResponse getCollisionResponse() {
        return collisionResponse;
    }

    public ParticleEmitterComponent setCollisionResponse(CollisionResponse response) {
        this.collisionResponse = response;
        return this;
    }

    public float getBounciness() {
        return bounciness;
    }

    public ParticleEmitterComponent setBounciness(float bounciness) {
        this.bounciness = bounciness;
        return this;
    }

    public float getFriction() {
        return friction;
    }

    public ParticleEmitterComponent setFriction(float friction) {
        this.friction = friction;
        return this;
    }

    public float getBounceChance() {
        return bounceChance;
    }

    public ParticleEmitterComponent setBounceChance(float chance) {
        this.bounceChance = chance;
        return this;
    }

    public float getBounceSpread() {
        return bounceSpread;
    }

    public ParticleEmitterComponent setBounceSpread(float spread) {
        this.bounceSpread = spread;
        return this;
    }

    public float getCollisionPlaneNX() {
        return collisionPlaneNX;
    }

    public float getCollisionPlaneNY() {
        return collisionPlaneNY;
    }

    public float getCollisionPlaneNZ() {
        return collisionPlaneNZ;
    }

    public float getCollisionPlaneD() {
        return collisionPlaneD;
    }

    public ParticleEmitterComponent setCollisionPlane(float nx, float ny, float nz, float d) {
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0001f) {
            this.collisionPlaneNX = nx / len;
            this.collisionPlaneNY = ny / len;
            this.collisionPlaneNZ = nz / len;
        }
        this.collisionPlaneD = d;
        return this;
    }

    public float getCollisionSphereCenterX() {
        return collisionSphereCenterX;
    }

    public float getCollisionSphereCenterY() {
        return collisionSphereCenterY;
    }

    public float getCollisionSphereCenterZ() {
        return collisionSphereCenterZ;
    }

    public float getCollisionSphereRadius() {
        return collisionSphereRadius;
    }

    public ParticleEmitterComponent setCollisionSphere(float cx, float cy, float cz, float radius) {
        this.collisionSphereCenterX = cx;
        this.collisionSphereCenterY = cy;
        this.collisionSphereCenterZ = cz;
        this.collisionSphereRadius = radius;
        return this;
    }

    public float getCollisionBoxMinX() {
        return collisionBoxMinX;
    }

    public float getCollisionBoxMinY() {
        return collisionBoxMinY;
    }

    public float getCollisionBoxMinZ() {
        return collisionBoxMinZ;
    }

    public float getCollisionBoxMaxX() {
        return collisionBoxMaxX;
    }

    public float getCollisionBoxMaxY() {
        return collisionBoxMaxY;
    }

    public float getCollisionBoxMaxZ() {
        return collisionBoxMaxZ;
    }

    public ParticleEmitterComponent setCollisionBox(float minX, float minY, float minZ, float maxX, float maxY,
        float maxZ) {
        this.collisionBoxMinX = minX;
        this.collisionBoxMinY = minY;
        this.collisionBoxMinZ = minZ;
        this.collisionBoxMaxX = maxX;
        this.collisionBoxMaxY = maxY;
        this.collisionBoxMaxZ = maxZ;
        return this;
    }

    public List<ParticleEmitter.SubEmitterEntry> getSubEmitters() {
        return subEmitters;
    }

    public ParticleEmitterComponent addSubEmitter(ParticleEmitter.SubEmitterEntry entry) {
        if (entry != null) {
            subEmitters.add(entry);
        }
        return this;
    }

    @Override
    public String toString() {
        return String.format(
            "ParticleEmitterComponent[shape=%s, rate=%.1f, lifetime=%.1f-%.1f]",
            shape,
            emissionRate,
            lifetimeMin,
            lifetimeMax);
    }
}
