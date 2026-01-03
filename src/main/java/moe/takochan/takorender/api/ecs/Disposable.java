package moe.takochan.takorender.api.ecs;

/**
 * 可释放资源接口
 *
 * <p>
 * Component 可选实现此接口以支持资源清理。
 * 当 Entity 被从 World 移除时，World 会自动调用实现此接口的 Component 的 dispose 方法。
 * </p>
 *
 * <p>
 * <b>使用场景</b>:
 * </p>
 * <ul>
 * <li>持有 GPU 资源的 Component（如 ParticleBufferComponent）</li>
 * <li>持有需要显式释放的原生资源的 Component</li>
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
 *     public class ParticleBufferComponent extends Component implements Disposable {
 * 
 *         private ParticleBuffer gpuBuffer;
 *
 *         &#64;Override
 *         public void dispose() {
 *             if (gpuBuffer != null) {
 *                 gpuBuffer.close();
 *                 gpuBuffer = null;
 *             }
 *         }
 *     }
 * }
 * </pre>
 */
public interface Disposable {

    /**
     * 释放此对象持有的资源
     *
     * <p>
     * 此方法由 World 在移除 Entity 时自动调用。
     * 实现应该释放所有需要显式清理的资源（如 GPU 缓冲区）。
     * </p>
     *
     * <p>
     * <b>注意</b>: 此方法可能被多次调用，实现应该是幂等的。
     * </p>
     */
    void dispose();
}
