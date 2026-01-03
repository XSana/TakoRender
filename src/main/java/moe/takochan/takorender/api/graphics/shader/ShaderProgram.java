package moe.takochan.takorender.api.graphics.shader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLContext;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takorender.TakoRenderMod;

/**
 * Shader Program 封装类
 *
 * <p>
 * 负责加载、编译、链接着色器，并提供 uniform 设置接口。
 * 实现 AutoCloseable 以支持 try-with-resources。
 * </p>
 *
 * <p>
 * <b>版本支持</b>:
 * </p>
 * <ul>
 * <li>基础着色器: GLSL 330 core (OpenGL 3.3)</li>
 * <li>Geometry Shader: GLSL 330+ (OpenGL 3.2)</li>
 * <li>Compute Shader: GLSL 430 core (OpenGL 4.3)</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class ShaderProgram implements AutoCloseable {

    private int programId = 0;
    private int vertexShaderId = 0;
    private int fragmentShaderId = 0;
    private int geometryShaderId = 0;
    private int computeShaderId = 0;

    private boolean isComputeProgram = false;

    private final Map<String, Integer> uniformCache = new HashMap<>();
    private final Map<String, Integer> blockIndexCache = new HashMap<>();
    private final Map<String, Integer> ssboIndexCache = new HashMap<>();

    private static Boolean geometryShaderSupported = null;
    private static Boolean computeShaderSupported = null;
    private static Boolean ssboSupported = null;
    private static int[] maxWorkGroupSize = null;
    private static int maxWorkGroupInvocations = -1;

    /**
     * 检查当前系统是否支持 Shader
     */
    public static boolean isSupported() {
        return OpenGlHelper.shadersSupported;
    }

    /**
     * 检查是否支持 Geometry Shader (GL32)
     */
    public static boolean isGeometryShaderSupported() {
        if (geometryShaderSupported == null) {
            try {
                geometryShaderSupported = GLContext.getCapabilities().OpenGL32
                    || GLContext.getCapabilities().GL_ARB_geometry_shader4;
            } catch (Exception e) {
                geometryShaderSupported = false;
            }
        }
        return geometryShaderSupported;
    }

    /**
     * 检查是否支持 Compute Shader (GL43)
     */
    public static boolean isComputeShaderSupported() {
        if (computeShaderSupported == null) {
            try {
                var caps = GLContext.getCapabilities();
                boolean gl43 = caps.OpenGL43;
                boolean arbCompute = caps.GL_ARB_compute_shader;
                computeShaderSupported = gl43 || arbCompute;
                TakoRenderMod.LOG.info(
                    "Compute Shader support: OpenGL43={}, ARB_compute_shader={}, result={}",
                    gl43,
                    arbCompute,
                    computeShaderSupported);
            } catch (Exception e) {
                TakoRenderMod.LOG.error("Failed to check compute shader support", e);
                computeShaderSupported = false;
            }
        }
        return computeShaderSupported;
    }

    /**
     * 检查是否支持 SSBO (Shader Storage Buffer Object, GL43)
     */
    public static boolean isSSBOSupported() {
        if (ssboSupported == null) {
            try {
                var caps = GLContext.getCapabilities();
                boolean gl43 = caps.OpenGL43;
                boolean arbSsbo = caps.GL_ARB_shader_storage_buffer_object;
                ssboSupported = gl43 || arbSsbo;
            } catch (Exception e) {
                ssboSupported = false;
            }
        }
        return ssboSupported;
    }

    /**
     * 获取 Compute Shader 最大工作组大小
     *
     * @return [maxX, maxY, maxZ] 数组
     */
    public static int[] getMaxWorkGroupSize() {
        if (maxWorkGroupSize == null && isComputeShaderSupported()) {
            maxWorkGroupSize = new int[3];
            maxWorkGroupSize[0] = GL30.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0);
            maxWorkGroupSize[1] = GL30.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1);
            maxWorkGroupSize[2] = GL30.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2);
        }
        return maxWorkGroupSize != null ? maxWorkGroupSize : new int[] { 0, 0, 0 };
    }

    /**
     * 获取单个工作组最大调用数
     */
    public static int getMaxWorkGroupInvocations() {
        if (maxWorkGroupInvocations < 0 && isComputeShaderSupported()) {
            maxWorkGroupInvocations = GL11.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS);
        }
        return maxWorkGroupInvocations > 0 ? maxWorkGroupInvocations : 0;
    }

    /**
     * 私有默认构造函数（用于静态工厂方法）
     */
    private ShaderProgram() {}

    /**
     * 从资源文件创建 Shader Program
     *
     * @param domain       资源域 (mod id)
     * @param vertexPath   顶点着色器路径
     * @param fragmentPath 片元着色器路径
     */
    public ShaderProgram(String domain, String vertexPath, String fragmentPath) {
        if (!isSupported()) {
            TakoRenderMod.LOG.warn("Shaders not supported on this system");
            return;
        }

        try {
            String vertexSource = loadShaderSource(domain, vertexPath);
            String fragmentSource = loadShaderSource(domain, fragmentPath);

            if (vertexSource == null || fragmentSource == null) {
                TakoRenderMod.LOG.error("Failed to load shader sources: {} / {}", vertexPath, fragmentPath);
                return;
            }

            createProgram(vertexSource, null, fragmentSource);

        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to create shader program", e);
            cleanup();
        }
    }

    /**
     * 从源码直接创建 Shader Program
     *
     * @param vertexSource   顶点着色器源码
     * @param fragmentSource 片元着色器源码
     */
    public ShaderProgram(String vertexSource, String fragmentSource) {
        if (!isSupported()) {
            TakoRenderMod.LOG.warn("Shaders not supported on this system");
            return;
        }

        try {
            createProgram(vertexSource, null, fragmentSource);
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to create shader program", e);
            cleanup();
        }
    }

    /**
     * 从资源文件创建带 Geometry Shader 的程序
     *
     * @param domain       资源域 (mod id)
     * @param vertexPath   顶点着色器路径
     * @param geometryPath 几何着色器路径 (可为 null)
     * @param fragmentPath 片元着色器路径
     */
    public ShaderProgram(String domain, String vertexPath, String geometryPath, String fragmentPath) {
        if (!isSupported()) {
            TakoRenderMod.LOG.warn("Shaders not supported on this system");
            return;
        }

        String effectiveGeometryPath = geometryPath;
        if (geometryPath != null && !isGeometryShaderSupported()) {
            TakoRenderMod.LOG.warn("Geometry shaders not supported, falling back to vertex/fragment only");
            effectiveGeometryPath = null;
        }

        try {
            String vertexSource = loadShaderSource(domain, vertexPath);
            String geometrySource = effectiveGeometryPath != null ? loadShaderSource(domain, effectiveGeometryPath)
                : null;
            String fragmentSource = loadShaderSource(domain, fragmentPath);

            if (vertexSource == null || fragmentSource == null) {
                TakoRenderMod.LOG.error("Failed to load shader sources: {} / {}", vertexPath, fragmentPath);
                return;
            }

            createProgram(vertexSource, geometrySource, fragmentSource);

        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to create shader program with geometry shader", e);
            cleanup();
        }
    }

    /**
     * 从源码创建带 Geometry Shader 的程序（静态工厂方法）
     */
    public static ShaderProgram createFromSource(String vertexSource, String geometrySource, String fragmentSource) {
        ShaderProgram program = new ShaderProgram();

        if (!isSupported()) {
            TakoRenderMod.LOG.warn("Shaders not supported on this system");
            return program;
        }

        String effectiveGeometrySource = geometrySource;
        if (geometrySource != null && !isGeometryShaderSupported()) {
            TakoRenderMod.LOG.warn("Geometry shaders not supported, ignoring geometry shader");
            effectiveGeometrySource = null;
        }

        try {
            program.createProgram(vertexSource, effectiveGeometrySource, fragmentSource);
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to create shader program", e);
            program.cleanup();
        }

        return program;
    }

    /**
     * 从资源文件创建 Compute Shader 程序
     *
     * @param domain      资源域 (mod id)
     * @param computePath 计算着色器路径
     * @return 创建的 ShaderProgram，如果失败则返回无效的程序
     */
    public static ShaderProgram createCompute(String domain, String computePath) {
        ShaderProgram program = new ShaderProgram();

        if (!isComputeShaderSupported()) {
            TakoRenderMod.LOG.warn("Compute shaders not supported on this system");
            return program;
        }

        try {
            String computeSource = program.loadShaderSource(domain, computePath);
            if (computeSource == null) {
                TakoRenderMod.LOG.error("Failed to load compute shader source: {}", computePath);
                return program;
            }
            program.createComputeProgram(computeSource);
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to create compute shader program", e);
            program.cleanup();
        }

        return program;
    }

    /**
     * 从源码创建 Compute Shader 程序
     */
    public static ShaderProgram createComputeFromSource(String computeSource) {
        ShaderProgram program = new ShaderProgram();

        if (!isComputeShaderSupported()) {
            TakoRenderMod.LOG.warn("Compute shaders not supported on this system");
            return program;
        }

        try {
            program.createComputeProgram(computeSource);
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to create compute shader program", e);
            program.cleanup();
        }

        return program;
    }

    private void createComputeProgram(String computeSource) {
        computeShaderId = compileShader(GL43.GL_COMPUTE_SHADER, computeSource);

        if (computeShaderId == 0) {
            cleanup();
            return;
        }

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, computeShaderId);
        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(programId, 1024);
            TakoRenderMod.LOG.error("Failed to link compute shader program:\n{}", log);
            GL20.glDeleteProgram(programId);
            programId = 0;
            return;
        }

        GL20.glDetachShader(programId, computeShaderId);
        GL20.glDeleteShader(computeShaderId);
        computeShaderId = 0;

        isComputeProgram = true;
        validateProgram();
    }

    private void createProgram(String vertexSource, String geometrySource, String fragmentSource) {
        vertexShaderId = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        fragmentShaderId = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);

        if (vertexShaderId == 0 || fragmentShaderId == 0) {
            cleanup();
            return;
        }

        if (geometrySource != null && isGeometryShaderSupported()) {
            geometryShaderId = compileShader(GL32.GL_GEOMETRY_SHADER, geometrySource);
            if (geometryShaderId == 0) {
                TakoRenderMod.LOG.warn("Geometry shader compilation failed, continuing without it");
            }
        }

        programId = linkProgram(vertexShaderId, geometryShaderId, fragmentShaderId);

        if (programId != 0) {
            validateProgram();
        }
    }

    private String loadShaderSource(String domain, String path) {
        try {
            ResourceLocation location = new ResourceLocation(domain, path);
            TakoRenderMod.LOG.debug("Loading shader from: {}", location);

            InputStream stream = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(location)
                .getInputStream();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String source = reader.lines()
                    .collect(Collectors.joining("\n"));
                TakoRenderMod.LOG.debug("Loaded shader {} ({} chars)", path, source.length());
                if (source.isEmpty()) {
                    TakoRenderMod.LOG.error("Shader source is empty: {}", path);
                }
                return source;
            }
        } catch (Exception e) {
            TakoRenderMod.LOG.error("Failed to load shader: {}:{} - {}", domain, path, e.getMessage());
            return null;
        }
    }

    private int compileShader(int type, String source) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shaderId, 1024);
            String typeName = getShaderTypeName(type);
            TakoRenderMod.LOG.error("Failed to compile {} shader:\n{}", typeName, log);
            GL20.glDeleteShader(shaderId);
            return 0;
        }

        return shaderId;
    }

    private static String getShaderTypeName(int type) {
        if (type == GL20.GL_VERTEX_SHADER) return "vertex";
        if (type == GL20.GL_FRAGMENT_SHADER) return "fragment";
        if (type == GL32.GL_GEOMETRY_SHADER) return "geometry";
        if (type == GL43.GL_COMPUTE_SHADER) return "compute";
        return "unknown";
    }

    private int linkProgram(int vertexId, int geometryId, int fragmentId) {
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexId);
        if (geometryId != 0) {
            GL20.glAttachShader(program, geometryId);
        }
        GL20.glAttachShader(program, fragmentId);

        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 1024);
            TakoRenderMod.LOG.error("Failed to link shader program:\n{}", log);
            GL20.glDeleteProgram(program);
            return 0;
        }

        GL20.glDetachShader(program, vertexId);
        GL20.glDeleteShader(vertexId);
        vertexShaderId = 0;

        if (geometryId != 0) {
            GL20.glDetachShader(program, geometryId);
            GL20.glDeleteShader(geometryId);
            geometryShaderId = 0;
        }

        GL20.glDetachShader(program, fragmentId);
        GL20.glDeleteShader(fragmentId);
        fragmentShaderId = 0;

        return program;
    }

    private void validateProgram() {
        GL20.glValidateProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(programId, 1024);
            TakoRenderMod.LOG.warn("Shader program validation warning (ID={}):\n{}", programId, log);
        }
    }

    /**
     * 使用此着色器程序
     */
    public void use() {
        if (programId != 0) {
            GL20.glUseProgram(programId);
        }
    }

    /**
     * 解绑着色器程序（使用固定管线）
     */
    public static void unbind() {
        GL20.glUseProgram(0);
    }

    /**
     * 调度 Compute Shader
     */
    public void dispatch(int numGroupsX, int numGroupsY, int numGroupsZ) {
        if (!isComputeProgram || programId == 0) {
            TakoRenderMod.LOG.warn("Cannot dispatch non-compute shader program");
            return;
        }
        GL43.glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
    }

    public void dispatch(int numGroupsX) {
        dispatch(numGroupsX, 1, 1);
    }

    public static void memoryBarrier(int barriers) {
        if (isComputeShaderSupported()) {
            GL42.glMemoryBarrier(barriers);
        }
    }

    public static void memoryBarrierSSBO() {
        memoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
    }

    public static void memoryBarrierAll() {
        memoryBarrier(GL42.GL_ALL_BARRIER_BITS);
    }

    public boolean isComputeProgram() {
        return isComputeProgram;
    }

    public int getProgram() {
        return programId;
    }

    public boolean isValid() {
        return programId != 0;
    }

    @Override
    public void close() {
        cleanup();
    }

    public void delete() {
        cleanup();
    }

    private void cleanup() {
        if (vertexShaderId != 0) {
            GL20.glDeleteShader(vertexShaderId);
            vertexShaderId = 0;
        }
        if (geometryShaderId != 0) {
            GL20.glDeleteShader(geometryShaderId);
            geometryShaderId = 0;
        }
        if (fragmentShaderId != 0) {
            GL20.glDeleteShader(fragmentShaderId);
            fragmentShaderId = 0;
        }
        if (computeShaderId != 0) {
            GL20.glDeleteShader(computeShaderId);
            computeShaderId = 0;
        }
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
            programId = 0;
        }
        uniformCache.clear();
        blockIndexCache.clear();
        ssboIndexCache.clear();
        isComputeProgram = false;
    }

    public int getUniformLocation(String name) {
        if (!isValid()) {
            return -1;
        }

        return uniformCache.computeIfAbsent(name, n -> {
            int loc = GL20.glGetUniformLocation(programId, n);
            if (loc == -1) {
                TakoRenderMod.LOG.warn("Uniform '{}' not found in shader program (ID = {})", n, programId);
            }
            return loc;
        });
    }

    public boolean setUniformInt(String name, int value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform1i(loc, value);
        return true;
    }

    public boolean setUniformBool(String name, boolean value) {
        return setUniformInt(name, value ? 1 : 0);
    }

    public boolean setUniformFloat(String name, float value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform1f(loc, value);
        return true;
    }

    public boolean setUniformVec2(String name, float x, float y) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform2f(loc, x, y);
        return true;
    }

    public boolean setUniformVec3(String name, float x, float y, float z) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform3f(loc, x, y, z);
        return true;
    }

    public boolean setUniformVec4(String name, float x, float y, float z, float w) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform4f(loc, x, y, z, w);
        return true;
    }

    public boolean setUniformMatrix3(String name, boolean transpose, FloatBuffer matrix) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniformMatrix3(loc, transpose, matrix);
        return true;
    }

    public boolean setUniformMatrix4(String name, boolean transpose, FloatBuffer matrix) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniformMatrix4(loc, transpose, matrix);
        return true;
    }

    public int getUniformBlockIndex(String blockName) {
        if (!isValid()) {
            return -1;
        }

        return blockIndexCache.computeIfAbsent(blockName, name -> {
            int index = GL31.glGetUniformBlockIndex(programId, name);
            if (index == GL31.GL_INVALID_INDEX) {
                TakoRenderMod.LOG.warn("Uniform block '{}' not found in shader program (ID = {})", name, programId);
                return -1;
            }
            return index;
        });
    }

    public boolean bindUniformBlock(String blockName, int bindingPoint) {
        int blockIndex = getUniformBlockIndex(blockName);
        if (blockIndex == -1) {
            return false;
        }
        GL31.glUniformBlockBinding(programId, blockIndex, bindingPoint);
        return true;
    }

    private static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;

    public int getShaderStorageBlockIndex(String blockName) {
        if (!isValid() || !isSSBOSupported()) {
            return -1;
        }

        return ssboIndexCache.computeIfAbsent(blockName, name -> {
            int index = GL43.glGetProgramResourceIndex(programId, GL43.GL_SHADER_STORAGE_BLOCK, name);
            if (index == GL31.GL_INVALID_INDEX) {
                TakoRenderMod.LOG
                    .warn("Shader storage block '{}' not found in shader program (ID = {})", name, programId);
                return -1;
            }
            return index;
        });
    }

    public boolean bindShaderStorageBlock(String blockName, int bindingPoint) {
        int blockIndex = getShaderStorageBlockIndex(blockName);
        if (blockIndex == -1) {
            return false;
        }
        GL43.glShaderStorageBlockBinding(programId, blockIndex, bindingPoint);
        return true;
    }

    public static void bindSSBO(int ssboId, int bindingPoint) {
        if (isSSBOSupported()) {
            GL30.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindingPoint, ssboId);
        }
    }

    public static void unbindSSBO(int bindingPoint) {
        if (isSSBOSupported()) {
            GL30.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindingPoint, 0);
        }
    }
}
