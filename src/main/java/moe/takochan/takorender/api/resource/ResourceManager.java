package moe.takochan.takorender.api.resource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import moe.takochan.takorender.TakoRenderMod;

/**
 * 资源管理器基类 - 提供资源缓存、引用计数和自动卸载
 *
 * <p>
 * ResourceManager 是资源管理的核心抽象类，子类需要实现：
 * </p>
 * <ul>
 * <li>{@link #loadResource(String)} - 加载资源</li>
 * <li>{@link #unloadResource(Object)} - 卸载资源</li>
 * </ul>
 *
 * <p>
 * <b>特性</b>:
 * </p>
 * <ul>
 * <li>资源缓存：相同 key 返回相同资源</li>
 * <li>引用计数：自动追踪资源使用情况</li>
 * <li>自动卸载：引用计数归零时可选择立即卸载或延迟卸载</li>
 * <li>线程安全：使用 ConcurrentHashMap</li>
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
 *     public class ShaderManager extends ResourceManager<ShaderProgram> {
 * 
 *         &#64;Override
 *         protected ShaderProgram loadResource(String key) {
 *             return new ShaderProgram().loadFromResources(key);
 *         }
 *
 *         &#64;Override
 *         protected void unloadResource(ShaderProgram resource) {
 *             resource.close();
 *         }
 *     }
 * }
 * </pre>
 *
 * @param <T> 资源类型
 */
public abstract class ResourceManager<T> {

    /** 资源缓存 */
    protected final Map<String, ResourceHandle<T>> cache = new ConcurrentHashMap<>();

    /** 是否在引用计数归零时立即卸载 */
    protected boolean immediateUnload = false;

    /** 管理器名称（用于日志） */
    protected final String name;

    protected ResourceManager(String name) {
        this.name = name;
    }

    /**
     * 获取资源句柄
     *
     * <p>
     * 如果资源已缓存，增加引用计数并返回；否则加载新资源。
     * </p>
     *
     * @param key 资源键
     * @return 资源句柄
     */
    public ResourceHandle<T> get(String key) {
        ResourceHandle<T> handle = cache.get(key);

        if (handle != null && handle.isValid()) {
            handle.acquire();
            return handle;
        }

        // 加载新资源
        synchronized (this) {
            // 双重检查
            handle = cache.get(key);
            if (handle != null && handle.isValid()) {
                handle.acquire();
                return handle;
            }

            T resource = loadResource(key);
            if (resource == null) {
                TakoRenderMod.LOG.warn("{}: Failed to load resource: {}", name, key);
                return null;
            }

            handle = new ResourceHandle<>(key, resource, this::onHandleReleased);
            cache.put(key, handle);
            TakoRenderMod.LOG.debug("{}: Loaded resource: {}", name, key);
            return handle;
        }
    }

    /**
     * 预加载资源（不返回句柄）
     *
     * @param key 资源键
     * @return 是否加载成功
     */
    public boolean preload(String key) {
        ResourceHandle<T> handle = get(key);
        if (handle != null) {
            handle.release();
            return true;
        }
        return false;
    }

    /**
     * 检查资源是否已缓存
     */
    public boolean isCached(String key) {
        ResourceHandle<T> handle = cache.get(key);
        return handle != null && handle.isValid();
    }

    /**
     * 获取缓存的资源数量
     */
    public int getCachedCount() {
        return (int) cache.values()
            .stream()
            .filter(ResourceHandle::isValid)
            .count();
    }

    /**
     * 清理所有未使用的资源（引用计数为0）
     *
     * @return 清理的资源数量
     */
    public int cleanup() {
        int cleaned = 0;
        for (Map.Entry<String, ResourceHandle<T>> entry : cache.entrySet()) {
            ResourceHandle<T> handle = entry.getValue();
            if (handle.getRefCount() <= 0) {
                unloadAndRemove(entry.getKey(), handle);
                cleaned++;
            }
        }
        if (cleaned > 0) {
            TakoRenderMod.LOG.info("{}: Cleaned up {} unused resources", name, cleaned);
        }
        return cleaned;
    }

    /**
     * 强制卸载所有资源
     */
    public void dispose() {
        for (Map.Entry<String, ResourceHandle<T>> entry : cache.entrySet()) {
            ResourceHandle<T> handle = entry.getValue();
            try {
                T resource = handle.get();
                handle.invalidate();
                unloadResource(resource);
            } catch (Exception e) {
                TakoRenderMod.LOG.error("{}: Error unloading resource: {}", name, entry.getKey(), e);
            }
        }
        cache.clear();
        TakoRenderMod.LOG.info("{}: Disposed all resources", name);
    }

    /**
     * 设置是否在引用计数归零时立即卸载
     */
    public void setImmediateUnload(boolean immediate) {
        this.immediateUnload = immediate;
    }

    /**
     * 资源句柄释放回调
     */
    private void onHandleReleased(ResourceHandle<T> handle) {
        if (immediateUnload && handle.getRefCount() <= 0) {
            unloadAndRemove(handle.getKey(), handle);
        }
    }

    /**
     * 卸载并移除资源
     */
    private void unloadAndRemove(String key, ResourceHandle<T> handle) {
        try {
            T resource = handle.get();
            handle.invalidate();
            cache.remove(key);
            unloadResource(resource);
            TakoRenderMod.LOG.debug("{}: Unloaded resource: {}", name, key);
        } catch (Exception e) {
            handle.invalidate();
            cache.remove(key);
            TakoRenderMod.LOG.error("{}: Error unloading resource: {}", name, key, e);
        }
    }

    /**
     * 加载资源（子类实现）
     *
     * @param key 资源键
     * @return 加载的资源，失败返回 null
     */
    protected abstract T loadResource(String key);

    /**
     * 卸载资源（子类实现）
     *
     * @param resource 要卸载的资源
     */
    protected abstract void unloadResource(T resource);
}
