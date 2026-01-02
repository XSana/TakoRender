package moe.takochan.takorender.api.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 速度生命周期曲线
 *
 * <p>
 * 定义粒子速度随生命周期（0-1）变化的效果。
 * 支持三个分量（X/Y/Z）独立控制。
 * </p>
 *
 * <p>
 * <b>使用示例</b>:
 * </p>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     VelocityOverLifetime vel = VelocityOverLifetime.decelerate();
 *     float[] xyz = vel.evaluate(0.5f); // 获取生命周期 50% 时的速度倍率
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class VelocityOverLifetime {

    /** X 轴速度曲线 */
    private AnimationCurve curveX;

    /** Y 轴速度曲线 */
    private AnimationCurve curveY;

    /** Z 轴速度曲线 */
    private AnimationCurve curveZ;

    /** 是否分离 XYZ 分量 */
    private boolean separateAxes = false;

    /** 统一曲线（当 separateAxes = false 时使用） */
    private AnimationCurve uniformCurve;

    /**
     * 创建空的速度生命周期曲线（统一模式）
     */
    public VelocityOverLifetime() {
        this.uniformCurve = AnimationCurve.constant(1.0f);
        this.curveX = new AnimationCurve();
        this.curveY = new AnimationCurve();
        this.curveZ = new AnimationCurve();
    }

    /**
     * 使用统一曲线创建速度生命周期曲线
     *
     * @param curve 统一曲线（应用于所有轴）
     */
    public VelocityOverLifetime(AnimationCurve curve) {
        this.uniformCurve = curve != null ? curve : AnimationCurve.constant(1.0f);
        this.curveX = new AnimationCurve();
        this.curveY = new AnimationCurve();
        this.curveZ = new AnimationCurve();
        this.separateAxes = false;
    }

    /**
     * 使用分离曲线创建速度生命周期曲线
     *
     * @param curveX X 轴曲线
     * @param curveY Y 轴曲线
     * @param curveZ Z 轴曲线
     */
    public VelocityOverLifetime(AnimationCurve curveX, AnimationCurve curveY, AnimationCurve curveZ) {
        this.curveX = curveX != null ? curveX : new AnimationCurve();
        this.curveY = curveY != null ? curveY : new AnimationCurve();
        this.curveZ = curveZ != null ? curveZ : new AnimationCurve();
        this.uniformCurve = AnimationCurve.constant(1.0f);
        this.separateAxes = true;
    }

    /**
     * 是否分离轴控制
     *
     * @return true 分离控制，false 统一控制
     */
    public boolean isSeparateAxes() {
        return separateAxes;
    }

    /**
     * 设置是否分离轴控制
     *
     * @param separateAxes 是否分离
     * @return this（链式调用）
     */
    public VelocityOverLifetime setSeparateAxes(boolean separateAxes) {
        this.separateAxes = separateAxes;
        return this;
    }

    /**
     * 获取统一曲线
     *
     * @return 统一曲线
     */
    public AnimationCurve getUniformCurve() {
        return uniformCurve;
    }

    /**
     * 设置统一曲线（禁用分离轴）
     *
     * @param curve 曲线
     * @return this（链式调用）
     */
    public VelocityOverLifetime setUniformCurve(AnimationCurve curve) {
        this.uniformCurve = curve != null ? curve : AnimationCurve.constant(1.0f);
        this.separateAxes = false;
        return this;
    }

    /**
     * 获取 X 轴曲线
     */
    public AnimationCurve getCurveX() {
        return curveX;
    }

    /**
     * 设置 X 轴曲线
     */
    public VelocityOverLifetime setCurveX(AnimationCurve curve) {
        this.curveX = curve != null ? curve : new AnimationCurve();
        return this;
    }

    /**
     * 获取 Y 轴曲线
     */
    public AnimationCurve getCurveY() {
        return curveY;
    }

    /**
     * 设置 Y 轴曲线
     */
    public VelocityOverLifetime setCurveY(AnimationCurve curve) {
        this.curveY = curve != null ? curve : new AnimationCurve();
        return this;
    }

    /**
     * 获取 Z 轴曲线
     */
    public AnimationCurve getCurveZ() {
        return curveZ;
    }

    /**
     * 设置 Z 轴曲线
     */
    public VelocityOverLifetime setCurveZ(AnimationCurve curve) {
        this.curveZ = curve != null ? curve : new AnimationCurve();
        return this;
    }

    /**
     * 添加统一关键帧
     *
     * @param time  时间 (0-1)
     * @param value 速度倍率
     * @return this（链式调用）
     */
    public VelocityOverLifetime addKey(float time, float value) {
        uniformCurve.addKey(time, value);
        separateAxes = false;
        return this;
    }

    /**
     * 添加分离轴关键帧
     *
     * @param time 时间 (0-1)
     * @param x    X 轴倍率
     * @param y    Y 轴倍率
     * @param z    Z 轴倍率
     * @return this（链式调用）
     */
    public VelocityOverLifetime addKey(float time, float x, float y, float z) {
        curveX.addKey(time, x);
        curveY.addKey(time, y);
        curveZ.addKey(time, z);
        separateAxes = true;
        return this;
    }

    /**
     * 设置是否使用平滑插值
     *
     * @param smooth true 平滑，false 线性
     * @return this（链式调用）
     */
    public VelocityOverLifetime setSmooth(boolean smooth) {
        uniformCurve.setSmooth(smooth);
        curveX.setSmooth(smooth);
        curveY.setSmooth(smooth);
        curveZ.setSmooth(smooth);
        return this;
    }

    /**
     * 在指定生命周期采样速度倍率
     *
     * @param lifePercent 生命周期百分比 (0-1)
     * @return [x, y, z] 速度倍率数组
     */
    public float[] evaluate(float lifePercent) {
        if (separateAxes) {
            return new float[] { curveX.evaluate(lifePercent), curveY.evaluate(lifePercent),
                curveZ.evaluate(lifePercent) };
        } else {
            float v = uniformCurve.evaluate(lifePercent);
            return new float[] { v, v, v };
        }
    }

    /**
     * 在指定生命周期采样统一倍率
     *
     * @param lifePercent 生命周期百分比 (0-1)
     * @return 速度倍率（统一模式）
     */
    public float evaluateUniform(float lifePercent) {
        return uniformCurve.evaluate(lifePercent);
    }

    /**
     * 转换为数组（用于上传到 GPU）
     *
     * @return 如果统一模式返回 [time0, val0, ...]，分离模式返回 [tx, vx, ty, vy, tz, vz, ...]
     */
    public float[] toArray() {
        if (separateAxes) {
            float[] arrX = curveX.toArray();
            float[] arrY = curveY.toArray();
            float[] arrZ = curveZ.toArray();
            float[] result = new float[arrX.length + arrY.length + arrZ.length];
            System.arraycopy(arrX, 0, result, 0, arrX.length);
            System.arraycopy(arrY, 0, result, arrX.length, arrY.length);
            System.arraycopy(arrZ, 0, result, arrX.length + arrY.length, arrZ.length);
            return result;
        } else {
            return uniformCurve.toArray();
        }
    }

    /**
     * 清除所有关键帧
     *
     * @return this（链式调用）
     */
    public VelocityOverLifetime clear() {
        uniformCurve.clear();
        curveX.clear();
        curveY.clear();
        curveZ.clear();
        return this;
    }

    /** 恒定速度 */
    public static VelocityOverLifetime constant() {
        return new VelocityOverLifetime(AnimationCurve.constant(1.0f));
    }

    /** 线性减速 - 从 1 到 0 */
    public static VelocityOverLifetime decelerate() {
        return new VelocityOverLifetime(AnimationCurve.linearDecay());
    }

    /** 线性加速 - 从 0 到 1 */
    public static VelocityOverLifetime accelerate() {
        return new VelocityOverLifetime(AnimationCurve.linear());
    }

    /** 快速减速 - 指数衰减 */
    public static VelocityOverLifetime fastDecelerate() {
        return new VelocityOverLifetime().addKey(0, 1.0f)
            .addKey(0.2f, 0.5f)
            .addKey(0.5f, 0.2f)
            .addKey(1, 0);
    }

    /** 缓慢减速 - 平滑过渡 */
    public static VelocityOverLifetime slowDecelerate() {
        return new VelocityOverLifetime().addKey(0, 1.0f)
            .addKey(0.5f, 0.8f)
            .addKey(0.8f, 0.4f)
            .addKey(1, 0);
    }

    /** 弹跳效果 - 速度反弹 */
    public static VelocityOverLifetime bounce() {
        return new VelocityOverLifetime().addKey(0, 1.0f)
            .addKey(0.25f, 0.3f)
            .addKey(0.35f, 0.6f)
            .addKey(0.5f, 0.2f)
            .addKey(0.6f, 0.4f)
            .addKey(0.75f, 0.1f)
            .addKey(1, 0);
    }

    /** 脉冲效果 - 速度脉动 */
    public static VelocityOverLifetime pulse() {
        return new VelocityOverLifetime().addKey(0, 1.0f)
            .addKey(0.25f, 0.5f)
            .addKey(0.5f, 1.0f)
            .addKey(0.75f, 0.5f)
            .addKey(1, 1.0f);
    }

    /** 爆炸效果 - 快速扩散后减速 */
    public static VelocityOverLifetime explosion() {
        return new VelocityOverLifetime().addKey(0, 2.0f)
            .addKey(0.1f, 1.5f)
            .addKey(0.3f, 0.8f)
            .addKey(0.6f, 0.3f)
            .addKey(1, 0);
    }

    /** 烟雾效果 - 缓慢上升 */
    public static VelocityOverLifetime smoke() {
        return new VelocityOverLifetime().addKey(0, 0.5f)
            .addKey(0.3f, 0.8f)
            .addKey(0.7f, 0.6f)
            .addKey(1, 0.3f);
    }

    /** 火花效果 - 快速衰减 */
    public static VelocityOverLifetime spark() {
        return new VelocityOverLifetime().addKey(0, 1.5f)
            .addKey(0.1f, 1.0f)
            .addKey(0.3f, 0.4f)
            .addKey(1, 0);
    }

    /** 漂浮效果 - Y 轴正向偏移 */
    public static VelocityOverLifetime float_() {
        VelocityOverLifetime v = new VelocityOverLifetime();
        v.separateAxes = true;
        v.curveX = AnimationCurve.constant(0);
        v.curveY = new AnimationCurve();
        v.curveY.addKey(0, 0.5f);
        v.curveY.addKey(0.5f, 1.0f);
        v.curveY.addKey(1, 0.5f);
        v.curveZ = AnimationCurve.constant(0);
        return v;
    }

    /** 下落效果 - Y 轴负向加速 */
    public static VelocityOverLifetime fall() {
        VelocityOverLifetime v = new VelocityOverLifetime();
        v.separateAxes = true;
        v.curveX = AnimationCurve.constant(0);
        v.curveY = new AnimationCurve();
        v.curveY.addKey(0, 0);
        v.curveY.addKey(0.5f, -0.5f);
        v.curveY.addKey(1, -1.0f);
        v.curveZ = AnimationCurve.constant(0);
        return v;
    }

    /** 螺旋效果 - XZ 平面旋转（需要配合角度使用） */
    public static VelocityOverLifetime spiral() {
        VelocityOverLifetime v = new VelocityOverLifetime();
        v.separateAxes = true;
        v.curveX = new AnimationCurve();
        v.curveX.addKey(0, 1.0f);
        v.curveX.addKey(0.5f, 0);
        v.curveX.addKey(1, -1.0f);
        v.curveY = AnimationCurve.constant(0.5f);
        v.curveZ = new AnimationCurve();
        v.curveZ.addKey(0, 0);
        v.curveZ.addKey(0.5f, 1.0f);
        v.curveZ.addKey(1, 0);
        return v;
    }
}
