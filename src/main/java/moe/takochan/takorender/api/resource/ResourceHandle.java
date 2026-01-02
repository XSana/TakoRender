package moe.takochan.takorender.api.resource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 资源句柄 - 封装资源引用和引用计数
 *
 * <p>
 * ResourceHandle 是对资源的智能引用，提供引用计数管理。
 * 当引用计数降为 0 时，资源可被 ResourceManager 回收。
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
 *     ResourceHandle<ShaderProgram> shader = ShaderManager.get("sprite");
 *     shader.get()
 *         .use();
 *     // 使用完毕后释放
 *     shader.release();
 * }
 * </pre>
 *
 * <p>
 * <b>生命周期</b>:
 * </p>
 * <ul>
 * <li>acquire() - 增加引用计数</li>
 * <li>release() - 减少引用计数，当计数为0时通知管理器</li>
 * <li>isValid() - 检查资源是否仍然有效</li>
 * </ul>
 *
 * @param <T> 资源类型
 */
public class ResourceHandle<T> {

    private final String key;
    private final T resource;
    private final AtomicInteger refCount;
    private final Consumer<ResourceHandle<T>> onRelease;
    private volatile boolean valid = true;

    /**
     * 创建资源句柄（由 ResourceManager 内部调用）
     *
     * @param key       资源键
     * @param resource  资源对象
     * @param onRelease 释放回调（引用计数归零时调用）
     */
    public ResourceHandle(String key, T resource, Consumer<ResourceHandle<T>> onRelease) {
        this.key = key;
        this.resource = resource;
        this.refCount = new AtomicInteger(1);
        this.onRelease = onRelease;
    }

    /**
     * 获取资源对象
     *
     * @return 资源对象
     * @throws IllegalStateException 如果资源已失效
     */
    public T get() {
        if (!valid) {
            throw new IllegalStateException("ResourceHandle is no longer valid: " + key);
        }
        return resource;
    }

    /**
     * 获取资源键
     */
    public String getKey() {
        return key;
    }

    /**
     * 检查资源是否有效
     */
    public boolean isValid() {
        return valid && refCount.get() > 0;
    }

    /**
     * 获取当前引用计数
     */
    public int getRefCount() {
        return refCount.get();
    }

    /**
     * 增加引用计数
     *
     * @return this（链式调用）
     */
    public ResourceHandle<T> acquire() {
        if (!valid) {
            throw new IllegalStateException("Cannot acquire invalid resource: " + key);
        }
        refCount.incrementAndGet();
        return this;
    }

    /**
     * 减少引用计数
     *
     * <p>
     * 当引用计数降为 0 时，调用 onRelease 回调通知管理器。
     * </p>
     */
    public void release() {
        int count = refCount.decrementAndGet();
        if (count == 0 && onRelease != null) {
            onRelease.accept(this);
        } else if (count < 0) {
            refCount.set(0);
        }
    }

    /**
     * 标记资源为无效（由 ResourceManager 调用）
     */
    void invalidate() {
        valid = false;
    }

    @Override
    public String toString() {
        return String.format("ResourceHandle[key=%s, refCount=%d, valid=%s]", key, refCount.get(), valid);
    }
}
