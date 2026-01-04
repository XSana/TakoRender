package moe.takochan.takorender.core.debug;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import moe.takochan.takorender.api.ecs.GameSystem;

/**
 * 系统性能分析器
 *
 * <p>
 * SystemProfiler 统计每个 GameSystem 的执行时间，用于性能调优。
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
 *     SystemProfiler profiler = new SystemProfiler();
 *     profiler.setEnabled(true);
 *
 *     // 在 World 中集成
 *     profiler.beginFrame();
 *     for (GameSystem system : systems) {
 *         profiler.beginSystem(system);
 *         system.update(deltaTime);
 *         profiler.endSystem(system);
 *     }
 *     profiler.endFrame();
 *
 *     // 输出报告
 *     System.out.println(profiler.getReport());
 * }
 * </pre>
 */
public class SystemProfiler {

    /** 滑动窗口大小（用于计算平均值） */
    private static final int WINDOW_SIZE = 60;

    /** 是否启用 */
    private boolean enabled = false;

    /** 每个 System 的性能数据 */
    private final Map<Class<? extends GameSystem>, ProfileData> profiles = new ConcurrentHashMap<>();

    /** 当前帧开始时间 */
    private long frameStartNanos;

    /** 当前帧总耗时 */
    private long frameTotalNanos;

    /** 上一帧总耗时（报告使用，不会被下一帧覆盖） */
    private long lastFrameTotalNanos;

    /** 总帧数 */
    private long totalFrames;

    /** 更新阶段累计耗时（当前帧） */
    private long updatePhaseNanos;

    /** 上一帧更新阶段累计耗时 */
    private long lastUpdatePhaseNanos;

    /** 渲染阶段累计耗时（当前帧） */
    private long renderPhaseNanos;

    /** 上一帧渲染阶段累计耗时 */
    private long lastRenderPhaseNanos;

    /** 当前阶段开始时间 */
    private long phaseStartNanos;

    /** 是否在帧内 */
    private boolean inFrame;

    /** 当前正在计时的 System */
    private GameSystem currentSystem;
    private long currentSystemStartNanos;

    /**
     * 性能数据
     */
    public static class ProfileData {

        /** 上一帧耗时（纳秒） */
        private long lastFrameNanos;

        /** 平均耗时（纳秒，滑动窗口） */
        private long averageNanos;

        /** 最大耗时（纳秒） */
        private long maxNanos;

        /** 滑动窗口数据 */
        private final long[] window = new long[WINDOW_SIZE];
        private int windowIndex = 0;
        private int windowCount = 0;

        /** 总调用次数 */
        private long callCount;

        /**
         * 记录一次执行
         */
        void record(long nanos) {
            lastFrameNanos = nanos;
            callCount++;

            // 更新最大值
            if (nanos > maxNanos) {
                maxNanos = nanos;
            }

            // 滑动窗口
            window[windowIndex] = nanos;
            windowIndex = (windowIndex + 1) % WINDOW_SIZE;
            if (windowCount < WINDOW_SIZE) {
                windowCount++;
            }

            // 计算平均值
            long sum = 0;
            for (int i = 0; i < windowCount; i++) {
                sum += window[i];
            }
            averageNanos = sum / windowCount;
        }

        public long getLastFrameNanos() {
            return lastFrameNanos;
        }

        public long getAverageNanos() {
            return averageNanos;
        }

        public long getMaxNanos() {
            return maxNanos;
        }

        public long getCallCount() {
            return callCount;
        }

        /**
         * 获取上一帧耗时（毫秒）
         */
        public double getLastFrameMs() {
            return lastFrameNanos / 1_000_000.0;
        }

        /**
         * 获取平均耗时（毫秒）
         */
        public double getAverageMs() {
            return averageNanos / 1_000_000.0;
        }

        /**
         * 获取最大耗时（毫秒）
         */
        public double getMaxMs() {
            return maxNanos / 1_000_000.0;
        }

        /**
         * 重置统计数据
         */
        void reset() {
            lastFrameNanos = 0;
            averageNanos = 0;
            maxNanos = 0;
            windowIndex = 0;
            windowCount = 0;
            callCount = 0;
        }
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 开始新的一帧
     * <p>
     * 应在每个真实渲染帧开始时调用一次（而非每个图层）。
     * </p>
     */
    public void beginFrame() {
        if (!enabled) {
            return;
        }
        frameStartNanos = System.nanoTime();
        frameTotalNanos = 0;
        updatePhaseNanos = 0;
        renderPhaseNanos = 0;
        inFrame = true;
    }

    /**
     * 结束当前帧
     * <p>
     * 应在每个真实渲染帧结束时调用一次（而非每个图层）。
     * </p>
     */
    public void endFrame() {
        if (!enabled || !inFrame) {
            return;
        }
        frameTotalNanos = System.nanoTime() - frameStartNanos;
        lastFrameTotalNanos = frameTotalNanos;
        lastUpdatePhaseNanos = updatePhaseNanos;
        lastRenderPhaseNanos = renderPhaseNanos;
        totalFrames++;
        inFrame = false;
    }

    /**
     * 开始更新阶段计时
     */
    public void beginUpdatePhase() {
        if (!enabled || !inFrame) {
            return;
        }
        phaseStartNanos = System.nanoTime();
    }

    /**
     * 结束更新阶段计时
     */
    public void endUpdatePhase() {
        if (!enabled || !inFrame) {
            return;
        }
        updatePhaseNanos += System.nanoTime() - phaseStartNanos;
    }

    /**
     * 开始渲染阶段计时
     */
    public void beginRenderPhase() {
        if (!enabled || !inFrame) {
            return;
        }
        phaseStartNanos = System.nanoTime();
    }

    /**
     * 结束渲染阶段计时
     */
    public void endRenderPhase() {
        if (!enabled || !inFrame) {
            return;
        }
        renderPhaseNanos += System.nanoTime() - phaseStartNanos;
    }

    /**
     * 开始计时 System
     */
    public void beginSystem(GameSystem system) {
        if (!enabled) {
            return;
        }
        currentSystem = system;
        currentSystemStartNanos = System.nanoTime();
    }

    /**
     * 结束计时 System
     */
    public void endSystem(GameSystem system) {
        if (!enabled || currentSystem != system) {
            return;
        }

        long elapsed = System.nanoTime() - currentSystemStartNanos;
        ProfileData data = profiles.computeIfAbsent(system.getClass(), k -> new ProfileData());
        data.record(elapsed);

        currentSystem = null;
    }

    /**
     * 获取指定 System 的性能数据
     */
    public ProfileData getProfile(Class<? extends GameSystem> systemClass) {
        return profiles.get(systemClass);
    }

    /**
     * 获取所有性能数据
     */
    public Map<Class<? extends GameSystem>, ProfileData> getAllProfiles() {
        return new LinkedHashMap<>(profiles);
    }

    /**
     * 获取上一完成帧总耗时（纳秒）
     */
    public long getFrameTotalNanos() {
        return lastFrameTotalNanos;
    }

    /**
     * 获取上一完成帧总耗时（毫秒）
     */
    public double getFrameTotalMs() {
        return lastFrameTotalNanos / 1_000_000.0;
    }

    /**
     * 获取上一完成帧更新阶段总耗时（纳秒）
     */
    public long getUpdatePhaseNanos() {
        return lastUpdatePhaseNanos;
    }

    /**
     * 获取上一完成帧更新阶段总耗时（毫秒）
     */
    public double getUpdatePhaseMs() {
        return lastUpdatePhaseNanos / 1_000_000.0;
    }

    /**
     * 获取上一完成帧渲染阶段总耗时（纳秒）
     */
    public long getRenderPhaseNanos() {
        return lastRenderPhaseNanos;
    }

    /**
     * 获取上一完成帧渲染阶段总耗时（毫秒）
     */
    public double getRenderPhaseMs() {
        return lastRenderPhaseNanos / 1_000_000.0;
    }

    /**
     * 获取总帧数
     */
    public long getTotalFrames() {
        return totalFrames;
    }

    /**
     * 重置所有统计数据
     */
    public void reset() {
        profiles.values()
            .forEach(ProfileData::reset);
        frameTotalNanos = 0;
        lastFrameTotalNanos = 0;
        lastUpdatePhaseNanos = 0;
        lastRenderPhaseNanos = 0;
        totalFrames = 0;
    }

    /**
     * 生成文本报告
     */
    public String getReport() {
        if (profiles.isEmpty()) {
            return "No profiling data available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== System Profiler Report ===\n");
        sb.append(String.format("Total Frames: %d\n", totalFrames));
        sb.append(
            String.format(
                "Frame Time: %.2f ms (Update: %.2f ms, Render: %.2f ms)\n\n",
                getFrameTotalMs(),
                getUpdatePhaseMs(),
                getRenderPhaseMs()));

        sb.append(String.format("%-40s %10s %10s %10s %10s\n", "System", "Last(ms)", "Avg(ms)", "Max(ms)", "Calls"));
        sb.append(String.format("%-40s %10s %10s %10s %10s\n", "------", "-------", "------", "------", "-----"));

        // 按平均耗时排序（降序）
        profiles.entrySet()
            .stream()
            .sorted(
                Comparator.<Map.Entry<Class<? extends GameSystem>, ProfileData>>comparingDouble(
                    e -> e.getValue()
                        .getAverageNanos())
                    .reversed())
            .forEach(entry -> {
                String name = entry.getKey()
                    .getSimpleName();
                ProfileData data = entry.getValue();
                sb.append(
                    String.format(
                        "%-40s %10.3f %10.3f %10.3f %10d\n",
                        name,
                        data.getLastFrameMs(),
                        data.getAverageMs(),
                        data.getMaxMs(),
                        data.getCallCount()));
            });

        return sb.toString();
    }
}
