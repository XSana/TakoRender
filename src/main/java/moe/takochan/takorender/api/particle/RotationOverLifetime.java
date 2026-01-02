package moe.takochan.takorender.api.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 旋转生命周期曲线
 *
 * <p>
 * 定义粒子旋转速度随生命周期（0-1）变化的效果。
 * 支持三个旋转轴（X/Y/Z）独立控制。
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
 *     RotationOverLifetime rot = RotationOverLifetime.spin();
 *     float[] xyz = rot.evaluate(0.5f); // 获取生命周期 50% 时的旋转速度
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class RotationOverLifetime {

    /** X 轴旋转速度曲线（弧度/秒） */
    private AnimationCurve curveX;

    /** Y 轴旋转速度曲线（弧度/秒） */
    private AnimationCurve curveY;

    /** Z 轴旋转速度曲线（弧度/秒） */
    private AnimationCurve curveZ;

    /** 是否分离 XYZ 分量 */
    private boolean separateAxes = false;

    /** 统一曲线（当 separateAxes = false 时使用，仅影响 Z 轴） */
    private AnimationCurve uniformCurve;

    /** 基础旋转速度（弧度/秒） */
    private float baseSpeed = 0;

    /**
     * 创建空的旋转生命周期曲线
     */
    public RotationOverLifetime() {
        this.uniformCurve = AnimationCurve.constant(0);
        this.curveX = new AnimationCurve();
        this.curveY = new AnimationCurve();
        this.curveZ = new AnimationCurve();
    }

    /**
     * 使用统一曲线创建旋转生命周期曲线
     *
     * @param curve 统一曲线（仅应用于 Z 轴，适用于 Billboard 粒子）
     */
    public RotationOverLifetime(AnimationCurve curve) {
        this.uniformCurve = curve != null ? curve : AnimationCurve.constant(0);
        this.curveX = new AnimationCurve();
        this.curveY = new AnimationCurve();
        this.curveZ = new AnimationCurve();
        this.separateAxes = false;
    }

    /**
     * 使用分离曲线创建旋转生命周期曲线
     *
     * @param curveX X 轴旋转曲线
     * @param curveY Y 轴旋转曲线
     * @param curveZ Z 轴旋转曲线
     */
    public RotationOverLifetime(AnimationCurve curveX, AnimationCurve curveY, AnimationCurve curveZ) {
        this.curveX = curveX != null ? curveX : new AnimationCurve();
        this.curveY = curveY != null ? curveY : new AnimationCurve();
        this.curveZ = curveZ != null ? curveZ : new AnimationCurve();
        this.uniformCurve = AnimationCurve.constant(0);
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
    public RotationOverLifetime setSeparateAxes(boolean separateAxes) {
        this.separateAxes = separateAxes;
        return this;
    }

    /**
     * 获取基础旋转速度
     *
     * @return 基础速度（弧度/秒）
     */
    public float getBaseSpeed() {
        return baseSpeed;
    }

    /**
     * 设置基础旋转速度
     *
     * @param speed 速度（弧度/秒）
     * @return this（链式调用）
     */
    public RotationOverLifetime setBaseSpeed(float speed) {
        this.baseSpeed = speed;
        return this;
    }

    /**
     * 设置基础旋转速度（度/秒）
     *
     * @param degreesPerSecond 速度（度/秒）
     * @return this（链式调用）
     */
    public RotationOverLifetime setBaseSpeedDegrees(float degreesPerSecond) {
        this.baseSpeed = (float) Math.toRadians(degreesPerSecond);
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
    public RotationOverLifetime setUniformCurve(AnimationCurve curve) {
        this.uniformCurve = curve != null ? curve : AnimationCurve.constant(0);
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
    public RotationOverLifetime setCurveX(AnimationCurve curve) {
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
    public RotationOverLifetime setCurveY(AnimationCurve curve) {
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
    public RotationOverLifetime setCurveZ(AnimationCurve curve) {
        this.curveZ = curve != null ? curve : new AnimationCurve();
        return this;
    }

    /**
     * 添加统一关键帧
     *
     * @param time  时间 (0-1)
     * @param value 旋转速度（弧度/秒）
     * @return this（链式调用）
     */
    public RotationOverLifetime addKey(float time, float value) {
        uniformCurve.addKey(time, value);
        separateAxes = false;
        return this;
    }

    /**
     * 添加统一关键帧（度/秒）
     *
     * @param time             时间 (0-1)
     * @param degreesPerSecond 旋转速度（度/秒）
     * @return this（链式调用）
     */
    public RotationOverLifetime addKeyDegrees(float time, float degreesPerSecond) {
        uniformCurve.addKey(time, (float) Math.toRadians(degreesPerSecond));
        separateAxes = false;
        return this;
    }

    /**
     * 添加分离轴关键帧
     *
     * @param time 时间 (0-1)
     * @param x    X 轴速度（弧度/秒）
     * @param y    Y 轴速度（弧度/秒）
     * @param z    Z 轴速度（弧度/秒）
     * @return this（链式调用）
     */
    public RotationOverLifetime addKey(float time, float x, float y, float z) {
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
    public RotationOverLifetime setSmooth(boolean smooth) {
        uniformCurve.setSmooth(smooth);
        curveX.setSmooth(smooth);
        curveY.setSmooth(smooth);
        curveZ.setSmooth(smooth);
        return this;
    }

    /**
     * 在指定生命周期采样旋转速度
     *
     * @param lifePercent 生命周期百分比 (0-1)
     * @return [x, y, z] 旋转速度数组（弧度/秒）
     */
    public float[] evaluate(float lifePercent) {
        if (separateAxes) {
            return new float[] { curveX.evaluate(lifePercent) + baseSpeed, curveY.evaluate(lifePercent) + baseSpeed,
                curveZ.evaluate(lifePercent) + baseSpeed };
        } else {
            float v = uniformCurve.evaluate(lifePercent) + baseSpeed;
            return new float[] { 0, 0, v };
        }
    }

    /**
     * 在指定生命周期采样 Z 轴旋转速度（用于 Billboard）
     *
     * @param lifePercent 生命周期百分比 (0-1)
     * @return Z 轴旋转速度（弧度/秒）
     */
    public float evaluateZ(float lifePercent) {
        if (separateAxes) {
            return curveZ.evaluate(lifePercent) + baseSpeed;
        } else {
            return uniformCurve.evaluate(lifePercent) + baseSpeed;
        }
    }

    /**
     * 转换为数组（用于上传到 GPU）
     *
     * @return 曲线数据数组
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
    public RotationOverLifetime clear() {
        uniformCurve.clear();
        curveX.clear();
        curveY.clear();
        curveZ.clear();
        return this;
    }

    /** 无旋转 */
    public static RotationOverLifetime none() {
        return new RotationOverLifetime(AnimationCurve.constant(0));
    }

    /** 恒定旋转（默认 180°/秒） */
    public static RotationOverLifetime constant() {
        return new RotationOverLifetime(AnimationCurve.constant((float) Math.PI));
    }

    /** 恒定旋转（指定速度） */
    public static RotationOverLifetime constant(float radiansPerSecond) {
        return new RotationOverLifetime(AnimationCurve.constant(radiansPerSecond));
    }

    /** 恒定旋转（度/秒） */
    public static RotationOverLifetime constantDegrees(float degreesPerSecond) {
        return new RotationOverLifetime(AnimationCurve.constant((float) Math.toRadians(degreesPerSecond)));
    }

    /** 缓慢旋转 - 90°/秒 */
    public static RotationOverLifetime slow() {
        return constantDegrees(90);
    }

    /** 快速旋转 - 360°/秒 */
    public static RotationOverLifetime fast() {
        return constantDegrees(360);
    }

    /** 加速旋转 - 从 0 到 360°/秒 */
    public static RotationOverLifetime accelerate() {
        return new RotationOverLifetime().addKeyDegrees(0, 0)
            .addKeyDegrees(1, 360);
    }

    /** 减速旋转 - 从 360°/秒 到 0 */
    public static RotationOverLifetime decelerate() {
        return new RotationOverLifetime().addKeyDegrees(0, 360)
            .addKeyDegrees(1, 0);
    }

    /** 旋转风暴 - 快速加速后减速 */
    public static RotationOverLifetime spin() {
        return new RotationOverLifetime().addKeyDegrees(0, 0)
            .addKeyDegrees(0.2f, 720)
            .addKeyDegrees(0.5f, 540)
            .addKeyDegrees(1, 180);
    }

    /** 摇摆效果 - 来回摆动 */
    public static RotationOverLifetime wobble() {
        return new RotationOverLifetime().addKeyDegrees(0, 90)
            .addKeyDegrees(0.25f, -90)
            .addKeyDegrees(0.5f, 90)
            .addKeyDegrees(0.75f, -90)
            .addKeyDegrees(1, 90);
    }

    /** 随机旋转感（通过变速模拟） */
    public static RotationOverLifetime erratic() {
        return new RotationOverLifetime().addKeyDegrees(0, 180)
            .addKeyDegrees(0.15f, 360)
            .addKeyDegrees(0.3f, 90)
            .addKeyDegrees(0.5f, 270)
            .addKeyDegrees(0.7f, 45)
            .addKeyDegrees(0.85f, 180)
            .addKeyDegrees(1, 0);
    }

    /** 叶片飘落效果 */
    public static RotationOverLifetime leaf() {
        return new RotationOverLifetime().addKeyDegrees(0, 45)
            .addKeyDegrees(0.3f, -30)
            .addKeyDegrees(0.5f, 60)
            .addKeyDegrees(0.7f, -45)
            .addKeyDegrees(1, 30);
    }

    /** 爆炸碎片效果 - 快速旋转 */
    public static RotationOverLifetime debris() {
        return new RotationOverLifetime().addKeyDegrees(0, 720)
            .addKeyDegrees(0.3f, 540)
            .addKeyDegrees(0.6f, 270)
            .addKeyDegrees(1, 90);
    }

    /** 平滑停止 */
    public static RotationOverLifetime smoothStop() {
        return new RotationOverLifetime().addKeyDegrees(0, 360)
            .addKeyDegrees(0.5f, 180)
            .addKeyDegrees(0.8f, 45)
            .addKeyDegrees(1, 0);
    }
}
