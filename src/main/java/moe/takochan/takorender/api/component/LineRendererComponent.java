package moe.takochan.takorender.api.component;

import org.joml.Vector3f;

import moe.takochan.takorender.api.ecs.Component;
import moe.takochan.takorender.api.ecs.RequiresComponent;
import moe.takochan.takorender.api.graphics.RenderQueue;

/**
 * 线条渲染组件 - 存储线条/线框渲染数据
 *
 * <p>
 * LineRendererComponent 用于渲染调试线条、边界框、辅助线等。
 * 支持多种预定义形状和自定义线条。
 * </p>
 *
 * <p>
 * <b>形状类型</b>:
 * </p>
 * <ul>
 * <li>{@link LineShape#LINE}: 单条线段</li>
 * <li>{@link LineShape#BOX}: 线框立方体</li>
 * <li>{@link LineShape#SPHERE}: 线框球体</li>
 * <li>{@link LineShape#CUSTOM}: 自定义顶点列表</li>
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
 *     Entity debugBox = world.createEntity();
 *     debugBox.addComponent(new TransformComponent(0, 64, 0));
 *     debugBox.addComponent(
 *         new LineRendererComponent().setShape(LineShape.BOX)
 *             .setSize(1.0f, 2.0f, 1.0f)
 *             .setColor(1.0f, 0.0f, 0.0f, 1.0f) // 红色
 *             .setLineWidth(2.0f));
 * }
 * </pre>
 */
@RequiresComponent(TransformComponent.class)
public class LineRendererComponent extends Component {

    /**
     * 线条形状枚举
     */
    public enum LineShape {
        /** 单条线段 */
        LINE,
        /** 线框立方体 */
        BOX,
        /** 线框球体 */
        SPHERE,
        /** 自定义顶点 */
        CUSTOM
    }

    private LineShape shape = LineShape.LINE;
    private RenderQueue renderQueue = RenderQueue.OVERLAY;

    private final Vector3f startPoint = new Vector3f(0, 0, 0);
    private final Vector3f endPoint = new Vector3f(1, 1, 1);

    private final Vector3f size = new Vector3f(1, 1, 1);

    private float colorR = 1.0f;
    private float colorG = 1.0f;
    private float colorB = 1.0f;
    private float colorA = 1.0f;

    /** 终点颜色（用于渐变） */
    private float endColorR = 1.0f;
    private float endColorG = 1.0f;
    private float endColorB = 1.0f;
    private float endColorA = 1.0f;

    /** 是否启用颜色渐变 */
    private boolean colorGradient = false;

    private float lineWidth = 1.0f;

    /** 终点线宽（用于宽度渐变） */
    private float endLineWidth = 1.0f;

    /** 是否启用宽度渐变 */
    private boolean widthGradient = false;

    private boolean depthTest = true;

    /**
     * 创建默认线条渲染组件
     */
    public LineRendererComponent() {}

    /**
     * 获取形状类型
     */
    public LineShape getShape() {
        return shape;
    }

    /**
     * 设置形状类型
     *
     * @param shape 形状类型
     * @return this（链式调用）
     */
    public LineRendererComponent setShape(LineShape shape) {
        this.shape = shape;
        return this;
    }

    /**
     * 获取渲染队列
     *
     * @return 渲染队列（默认 OVERLAY）
     */
    public RenderQueue getRenderQueue() {
        return renderQueue;
    }

    /**
     * 设置渲染队列
     *
     * <p>
     * 线条默认在 OVERLAY 层渲染（始终可见）。
     * 设置为 OPAQUE 或 TRANSPARENT 可使其参与正常深度排序。
     * </p>
     *
     * @param renderQueue 渲染队列
     * @return this（链式调用）
     */
    public LineRendererComponent setRenderQueue(RenderQueue renderQueue) {
        this.renderQueue = renderQueue != null ? renderQueue : RenderQueue.OVERLAY;
        return this;
    }

    /**
     * 获取线段起点
     */
    public Vector3f getStartPoint() {
        return new Vector3f(startPoint);
    }

    /**
     * 设置线段起点
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return this（链式调用）
     */
    public LineRendererComponent setStartPoint(float x, float y, float z) {
        this.startPoint.set(x, y, z);
        return this;
    }

    /**
     * 获取线段终点
     */
    public Vector3f getEndPoint() {
        return new Vector3f(endPoint);
    }

    /**
     * 设置线段终点
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return this（链式调用）
     */
    public LineRendererComponent setEndPoint(float x, float y, float z) {
        this.endPoint.set(x, y, z);
        return this;
    }

    /**
     * 设置线段起点和终点
     *
     * @param startX 起点 X
     * @param startY 起点 Y
     * @param startZ 起点 Z
     * @param endX   终点 X
     * @param endY   终点 Y
     * @param endZ   终点 Z
     * @return this（链式调用）
     */
    public LineRendererComponent setLine(float startX, float startY, float startZ, float endX, float endY, float endZ) {
        this.shape = LineShape.LINE;
        this.startPoint.set(startX, startY, startZ);
        this.endPoint.set(endX, endY, endZ);
        return this;
    }

    /**
     * 获取形状尺寸（用于 BOX/SPHERE）
     */
    public Vector3f getSize() {
        return new Vector3f(size);
    }

    /**
     * 设置形状尺寸
     *
     * @param width  宽度（X）
     * @param height 高度（Y）
     * @param depth  深度（Z）
     * @return this（链式调用）
     */
    public LineRendererComponent setSize(float width, float height, float depth) {
        this.size.set(width, height, depth);
        return this;
    }

    /**
     * 设置统一尺寸
     *
     * @param size 尺寸
     * @return this（链式调用）
     */
    public LineRendererComponent setSize(float size) {
        this.size.set(size, size, size);
        return this;
    }

    /**
     * 获取颜色 R 分量（0-1）
     */
    public float getColorR() {
        return colorR;
    }

    /**
     * 获取颜色 G 分量（0-1）
     */
    public float getColorG() {
        return colorG;
    }

    /**
     * 获取颜色 B 分量（0-1）
     */
    public float getColorB() {
        return colorB;
    }

    /**
     * 获取颜色 A 分量（0-1）
     */
    public float getColorA() {
        return colorA;
    }

    /**
     * 设置颜色
     *
     * @param r 红色（0-1）
     * @param g 绿色（0-1）
     * @param b 蓝色（0-1）
     * @param a 透明度（0-1）
     * @return this（链式调用）
     */
    public LineRendererComponent setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
        return this;
    }

    /**
     * 设置颜色（不透明）
     *
     * @param r 红色（0-1）
     * @param g 绿色（0-1）
     * @param b 蓝色（0-1）
     * @return this（链式调用）
     */
    public LineRendererComponent setColor(float r, float g, float b) {
        return setColor(r, g, b, 1.0f);
    }

    /**
     * 获取终点颜色 R 分量（0-1）
     */
    public float getEndColorR() {
        return endColorR;
    }

    /**
     * 获取终点颜色 G 分量（0-1）
     */
    public float getEndColorG() {
        return endColorG;
    }

    /**
     * 获取终点颜色 B 分量（0-1）
     */
    public float getEndColorB() {
        return endColorB;
    }

    /**
     * 获取终点颜色 A 分量（0-1）
     */
    public float getEndColorA() {
        return endColorA;
    }

    /**
     * 设置终点颜色
     *
     * @param r 红色（0-1）
     * @param g 绿色（0-1）
     * @param b 蓝色（0-1）
     * @param a 透明度（0-1）
     * @return this（链式调用）
     */
    public LineRendererComponent setEndColor(float r, float g, float b, float a) {
        this.endColorR = r;
        this.endColorG = g;
        this.endColorB = b;
        this.endColorA = a;
        return this;
    }

    /**
     * 设置终点颜色（不透明）
     *
     * @param r 红色（0-1）
     * @param g 绿色（0-1）
     * @param b 蓝色（0-1）
     * @return this（链式调用）
     */
    public LineRendererComponent setEndColor(float r, float g, float b) {
        return setEndColor(r, g, b, 1.0f);
    }

    /**
     * 设置颜色渐变
     *
     * <p>
     * 线条从起点颜色渐变到终点颜色。
     * </p>
     *
     * @param startR 起点红色
     * @param startG 起点绿色
     * @param startB 起点蓝色
     * @param startA 起点透明度
     * @param endR   终点红色
     * @param endG   终点绿色
     * @param endB   终点蓝色
     * @param endA   终点透明度
     * @return this（链式调用）
     */
    public LineRendererComponent setColorGradient(float startR, float startG, float startB, float startA, float endR,
        float endG, float endB, float endA) {
        setColor(startR, startG, startB, startA);
        setEndColor(endR, endG, endB, endA);
        this.colorGradient = true;
        return this;
    }

    /**
     * 检查是否启用颜色渐变
     */
    public boolean isColorGradient() {
        return colorGradient;
    }

    /**
     * 设置是否启用颜色渐变
     *
     * @param colorGradient 是否启用
     * @return this（链式调用）
     */
    public LineRendererComponent setColorGradient(boolean colorGradient) {
        this.colorGradient = colorGradient;
        return this;
    }

    /**
     * 获取线宽
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * 设置线宽
     *
     * @param lineWidth 线宽（像素）
     * @return this（链式调用）
     */
    public LineRendererComponent setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    /**
     * 获取终点线宽
     */
    public float getEndLineWidth() {
        return endLineWidth;
    }

    /**
     * 设置终点线宽
     *
     * @param endLineWidth 终点线宽（像素）
     * @return this（链式调用）
     */
    public LineRendererComponent setEndLineWidth(float endLineWidth) {
        this.endLineWidth = endLineWidth;
        return this;
    }

    /**
     * 设置线宽渐变
     *
     * <p>
     * 线条从起点宽度渐变到终点宽度，用于创建拖尾效果。
     * </p>
     *
     * @param startWidth 起点线宽
     * @param endWidth   终点线宽
     * @return this（链式调用）
     */
    public LineRendererComponent setWidthGradient(float startWidth, float endWidth) {
        this.lineWidth = startWidth;
        this.endLineWidth = endWidth;
        this.widthGradient = true;
        return this;
    }

    /**
     * 检查是否启用宽度渐变
     */
    public boolean isWidthGradient() {
        return widthGradient;
    }

    /**
     * 设置是否启用宽度渐变
     *
     * @param widthGradient 是否启用
     * @return this（链式调用）
     */
    public LineRendererComponent setWidthGradient(boolean widthGradient) {
        this.widthGradient = widthGradient;
        return this;
    }

    /**
     * 检查是否启用深度测试
     */
    public boolean isDepthTest() {
        return depthTest;
    }

    /**
     * 设置是否启用深度测试
     *
     * @param depthTest 是否启用
     * @return this（链式调用）
     */
    public LineRendererComponent setDepthTest(boolean depthTest) {
        this.depthTest = depthTest;
        return this;
    }
}
