# TakoRender 架构设计文档

> **版本**: 1.0
>
> **最后更新**: 2026-01-03
>
> **状态**: 核心框架完成（P0-P2）

---

## 目录

1. [框架概述](#一框架概述)
2. [Minecraft 集成](#二minecraft-集成)
3. [ECS 核心架构](#三ecs-核心架构)
4. [Component 参考](#四component-参考)
5. [System 参考](#五system-参考)
6. [资源管理系统](#六资源管理系统)
7. [渲染管线](#七渲染管线)
8. [使用指南](#八使用指南)
9. [注意事项](#九注意事项)

---

## 一、框架概述

### 1.1 定位

TakoRender 是 Minecraft 1.7.10 的现代化 ECS 渲染框架库，提供：

- **纯 ECS 架构**：Entity-Component-System 设计模式
- **现代 OpenGL**：基于 OpenGL 3.3 Core Profile
- **自动化渲染**：用户只需创建 Entity/Component，System 自动处理
- **MC 集成**：与 Minecraft 渲染管线无缝协作

### 1.2 技术规格

| 项目 | 规格 |
|------|------|
| OpenGL 版本 | 3.3 Core（基线），4.3+（可选高级特性） |
| GLSL 版本 | 330 core |
| 目标平台 | GTNH 2.8.0-rc1（Minecraft 1.7.10） |
| macOS 支持 | OpenGL 3.3（无 Compute Shader） |

### 1.3 架构分层

```
┌─────────────────────────────────────────────────────────┐
│ 用户代码                                                │
│ - 创建 Entity/Component                                 │
│ - 配置渲染参数                                          │
├─────────────────────────────────────────────────────────┤
│ ECS 层 (api/ecs/)                                       │
│ - World: 实体管理、系统调度                              │
│ - Entity: ID + Component 容器                           │
│ - Component: 纯数据对象                                  │
│ - GameSystem: 逻辑执行单元                               │
├─────────────────────────────────────────────────────────┤
│ 渲染层 (api/system/, core/system/)                      │
│ - MeshRenderSystem: 网格渲染                             │
│ - ParticleRenderSystem: 粒子渲染                         │
│ - PostProcessSystem: 后处理                              │
├─────────────────────────────────────────────────────────┤
│ 资源层 (api/resource/)                                  │
│ - ShaderManager: 着色器管理                              │
│ - TextureManager: 纹理管理                               │
│ - ModelManager: 模型管理                                 │
├─────────────────────────────────────────────────────────┤
│ GL 状态层 (core/gl/)                                    │
│ - GLStateContext: GL 状态隔离与恢复                      │
├─────────────────────────────────────────────────────────┤
│ OpenGL 驱动                                             │
└─────────────────────────────────────────────────────────┘
```

### 1.4 什么是 ECS

**ECS（Entity-Component-System）** 是一种数据驱动的架构模式，将游戏对象拆分为三个独立概念：

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ECS 三大核心概念                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Entity（实体）                                                       │
│  ────────────                                                        │
│  • 仅是一个唯一 ID（如 long 类型的数字）                               │
│  • 作为 Component 的容器，本身不包含任何数据或逻辑                     │
│  • 类比：数据库表的主键，只用于标识和关联                              │
│                                                                      │
│  Component（组件）                                                    │
│  ──────────────                                                      │
│  • 纯数据对象，只有字段和 getter/setter                               │
│  • 不包含任何业务逻辑或行为方法                                        │
│  • 类比：数据库表的一行数据                                           │
│  • 示例：TransformComponent 只存储 position、rotation、scale          │
│                                                                      │
│  System（系统）                                                       │
│  ────────────                                                        │
│  • 唯一的逻辑执行单元                                                 │
│  • 查询拥有特定 Component 组合的 Entity，批量处理                      │
│  • 类比：数据库的存储过程，处理所有匹配的数据                          │
│  • 示例：TransformSystem 遍历所有有 TransformComponent 的实体，        │
│          计算世界矩阵                                                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.5 ECS vs 传统 OOP

**传统面向对象（继承体系）**：
```java
// ❌ 传统 OOP：逻辑和数据耦合在对象中
class Particle extends GameObject {
    Vector3f position;
    Vector3f velocity;
    float lifetime;
    Texture texture;

    void update(float dt) {
        position.add(velocity.mul(dt));  // 物理逻辑
        lifetime -= dt;
    }

    void render() {
        // 渲染逻辑
        shader.use();
        texture.bind();
        drawQuad(position);
    }
}

// 问题：
// 1. 每个对象独立调用 update/render，无法批处理
// 2. 继承层次复杂时，修改困难
// 3. 内存布局分散，缓存不友好
```

**ECS 架构（数据驱动）**：
```java
// ✅ ECS：数据和逻辑完全分离

// Component - 只有数据
class TransformComponent extends Component {
    Vector3f position;
    Vector3f velocity;
}

class ParticleStateComponent extends Component {
    float lifetime;
    float elapsed;
}

// System - 只有逻辑，批量处理所有匹配实体
class ParticlePhysicsSystem extends GameSystem {
    void update(float dt) {
        for (Entity e : getEntitiesWith(TransformComponent.class)) {
            TransformComponent t = e.getComponent(TransformComponent.class);
            t.position.add(t.velocity.mul(dt));  // 批量处理
        }
    }
}

class ParticleRenderSystem extends GameSystem {
    void update(float dt) {
        // 批量收集所有粒子，一次 Draw Call 渲染
        for (Entity e : getEntitiesWith(ParticleRenderComponent.class)) {
            batch.add(e);  // 收集
        }
        batch.flush();     // 统一渲染
    }
}

// 优势：
// 1. System 批量处理，可优化为单次 Draw Call
// 2. 通过组合 Component 构建功能，无需继承
// 3. 数据连续存储，缓存友好
```

### 1.6 TakoRender ECS 执行流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                      每帧执行流程                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. Forge 事件触发                                                   │
│     └─ RenderWorldLastEvent / RenderGameOverlayEvent                │
│                                                                      │
│  2. RenderEventHandler 路由到对应 Layer                              │
│     └─ world.update(Layer.WORLD_3D, partialTicks)                   │
│     └─ world.render(Layer.WORLD_3D)                                 │
│                                                                      │
│  3. World.update() - UPDATE 阶段                                     │
│     │                                                                │
│     │  ┌─────────────────────────────────────────────────────────┐  │
│     │  │ TransformSystem (priority: -1000)                       │  │
│     │  │ • 遍历所有 TransformComponent                            │  │
│     │  │ • 计算 worldMatrix = T * R * S                          │  │
│     │  │ • 更新 forward/up/right 方向向量                         │  │
│     │  └─────────────────────────────────────────────────────────┘  │
│     │                         ↓                                      │
│     │  ┌─────────────────────────────────────────────────────────┐  │
│     │  │ CameraSystem (priority: 100)                            │  │
│     │  │ • 同步 MC 相机参数（如果 syncWithMinecraft=true）        │  │
│     │  │ • 计算 viewMatrix 和 projectionMatrix                   │  │
│     │  │ • 更新 viewProjectionMatrix                             │  │
│     │  └─────────────────────────────────────────────────────────┘  │
│     │                         ↓                                      │
│     │  ┌─────────────────────────────────────────────────────────┐  │
│     │  │ FrustumCullingSystem                                    │  │
│     │  │ • 用 viewProjection 构建视锥体                          │  │
│     │  │ • 测试每个 BoundsComponent，设置 culled 标记            │  │
│     │  └─────────────────────────────────────────────────────────┘  │
│     │                         ↓                                      │
│     │  ┌─────────────────────────────────────────────────────────┐  │
│     │  │ ParticleEmitSystem / ParticlePhysicsSystem              │  │
│     │  │ • 发射新粒子                                             │  │
│     │  │ • 更新粒子位置/速度/生命                                 │  │
│     │  └─────────────────────────────────────────────────────────┘  │
│     │                         ↓                                      │
│     │  ┌─────────────────────────────────────────────────────────┐  │
│     │  │ LifetimeSystem (priority: MAX)                          │  │
│     │  │ • 检查 TRANSIENT 类型是否过期                           │  │
│     │  │ • 销毁 markForDestroy 的 Entity                         │  │
│     │  └─────────────────────────────────────────────────────────┘  │
│     │                                                                │
│                                                                      │
│  4. World.render() - RENDER 阶段                                     │
│     │                                                                │
│     │  ┌─────────────────────────────────────────────────────────┐  │
│     │  │ MeshRenderSystem (priority: 0)                          │  │
│     │  │ • 收集所有 MeshRendererComponent                        │  │
│     │  │ • 按 RenderQueue 分组（OPAQUE → TRANSPARENT）           │  │
│     │  │ • 按 Material 排序，减少状态切换                        │  │
│     │  │ • 透明物体按深度排序（远→近）                           │  │
│     │  │ • 批量渲染                                               │  │
│     │  └─────────────────────────────────────────────────────────┘  │
│     │                         ↓                                      │
│     │  ┌─────────────────────────────────────────────────────────┐  │
│     │  │ LineRenderSystem / SpriteRenderSystem                   │  │
│     │  │ • 收集线条/精灵                                          │  │
│     │  │ • 批量渲染                                               │  │
│     │  └─────────────────────────────────────────────────────────┘  │
│     │                         ↓                                      │
│     │  ┌─────────────────────────────────────────────────────────┐  │
│     │  │ ParticleRenderSystem                                    │  │
│     │  │ • GPU 路径：SSBO + Compute Shader                       │  │
│     │  │ • CPU 路径：收集到 FloatBuffer + Instanced Draw         │  │
│     │  └─────────────────────────────────────────────────────────┘  │
│     │                         ↓                                      │
│     │  ┌─────────────────────────────────────────────────────────┐  │
│     │  │ PostProcessSystem                                       │  │
│     │  │ • Bloom 提取 → 模糊 → 合成                              │  │
│     │  │ • 色调映射                                               │  │
│     │  └─────────────────────────────────────────────────────────┘  │
│     │                                                                │
│                                                                      │
│  5. 返回 Forge，继续 MC 渲染流程                                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.7 与传统 MC 渲染方式对比

**传统 MC 渲染（手动管理）**：
```java
// ❌ 在 RenderWorldLastEvent 中手动渲染
@SubscribeEvent
public void onRenderWorldLast(RenderWorldLastEvent event) {
    // 手动获取相机
    Camera camera = RenderSystem.getWorldCamera();
    camera.syncFromMinecraft(event.partialTicks);

    // 手动开始批处理
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);  // 保存状态
    GL11.glEnable(GL11.GL_BLEND);

    World3DBatch batch = RenderSystem.getWorld3DBatch();
    batch.begin(GL11.GL_LINES);

    // 手动遍历渲染每个物体
    for (MyObject obj : myObjects) {
        batch.drawWireBox(obj.x, obj.y, obj.z, 1, 1, 1, 1, 0, 0, 1);
    }

    batch.end();
    GL11.glPopAttrib();  // 恢复状态

    // 问题：
    // 1. 用户需要管理 GL 状态（容易遗漏）
    // 2. 用户需要手动调用 begin/end
    // 3. 用户需要处理相机同步
    // 4. 代码分散，难以维护
}
```

**TakoRender ECS 方式（声明式）**：
```java
// ✅ 只需创建 Entity 和 Component，渲染自动进行
public void setupScene() {
    World world = TakoRender.getWorld();

    // 创建物体 - 只描述"是什么"，不关心"怎么渲染"
    Entity box = world.createEntity();
    box.addComponent(new TransformComponent().setPosition(x, y, z));
    box.addComponent(new LayerComponent().setLayer(Layer.WORLD_3D));
    box.addComponent(new LineRendererComponent()
        .setShape(LineShape.BOX)
        .setColor(1, 0, 0, 1));
    box.addComponent(new LifetimeComponent().setLifetime(Lifetime.SESSION));

    // 完成！不需要：
    // - 手动同步相机（CameraSystem 自动处理）
    // - 手动管理 GL 状态（GLStateContext 自动处理）
    // - 手动调用 render（LineRenderSystem 自动处理）
    // - 手动清理（LifetimeSystem 自动处理）
}
```

---

## 二、Minecraft 集成

### 2.1 框架已完成

TakoRender 已经处理的工作：

| 已完成 | 说明 |
|--------|------|
| ✅ ECS 初始化 | ClientProxy.init() 自动创建 World 并注册核心 System |
| ✅ 事件钩子注册 | 自动注册 Forge 渲染事件监听 |
| ✅ 分层渲染调度 | RenderEventHandler 将 Forge 事件路由到 Layer |
| ✅ 维度同步 | 自动同步 MC 当前维度 ID |
| ✅ 存档退出清理 | SESSION 类型 Entity 自动销毁 |
| ✅ 资源预加载 | postInit 阶段预编译 Shader |
| ✅ GL 状态隔离 | 渲染不影响 MC 的 GL 状态 |

### 2.2 用户需要做的

| 用户职责 | 说明 |
|----------|------|
| 创建 Entity | 调用 `TakoRender.getWorld().createEntity()` |
| 添加 Component | 配置 Transform、Mesh、Material 等 |
| 设置 Layer | 决定在哪个渲染阶段显示（WORLD_3D/HUD/GUI） |
| 设置生命周期 | 配置 LifetimeComponent 控制自动销毁 |
| 加载资源 | 准备 Mesh、Texture、自定义 Shader |

### 2.3 MC + Angelica 完整渲染管线

TakoRender 嵌入到 MC 渲染管线中的具体位置：

```
MC 主循环 (Minecraft.runGameLoop)
│
└── EntityRenderer.updateCameraAndRender()
    │
    ├── renderWorld()
    │   │
    │   ├── [Angelica] beginLevelRendering()
    │   │   └─ 初始化 GBuffer、准备阴影贴图
    │   │
    │   ├── [Angelica] renderShadows()
    │   │   └─ 渲染阴影贴图
    │   │
    │   ├── MC 原生渲染
    │   │   ├─ Sky（天空）
    │   │   ├─ Terrain（地形）
    │   │   ├─ Entities（实体）
    │   │   ├─ TileEntities（方块实体）
    │   │   └─ Particles（粒子）
    │   │
    │   ├── ★ RenderWorldLastEvent ←────────────────────────────┐
    │   │   │                                                    │
    │   │   └── TakoRender WORLD_3D 层                           │
    │   │       • world.update(WORLD_3D, partialTicks)           │
    │   │       • world.render(WORLD_3D)                         │
    │   │       • 渲染 3D 物体、粒子、线条                        │
    │   │                                                        │
    │   ├── Hand 渲染（第一人称手臂）                             │
    │   │                                                        │
    │   └── [Angelica] finalizeLevelRendering() ←────────────────┘
    │       └─ 后处理管线（Bloom、DOF、Color Grading...）
    │          WORLD_3D 的内容会被后处理影响
    │
    ├── theShaderGroup（MC 原生后处理，如 Super Secret Settings）
    │
    ├── renderGameOverlay()
    │   │
    │   └── ★ RenderGameOverlayEvent.Post(ALL) ←─────────────────┐
    │       │                                                     │
    │       └── TakoRender HUD 层                                 │
    │           • world.update(HUD, partialTicks)                 │
    │           • world.render(HUD)                               │
    │           • 血条、小地图、状态图标                           │
    │                                                             │
    │       不受光影后处理影响 ←──────────────────────────────────┘
    │
    └── GuiScreen.drawScreen()（如果有打开的 GUI）
        │
        └── ★ DrawScreenEvent.Post ←─────────────────────────────┐
            │                                                     │
            └── TakoRender GUI 层                                 │
                • world.update(GUI, partialTicks)                 │
                • world.render(GUI)                               │
                • 背包、菜单、对话框                               │
                                                                  │
            不受光影后处理影响 ←──────────────────────────────────┘
```

### 2.4 简化渲染流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                     Minecraft 渲染循环                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. MC 世界渲染                                                   │
│     └─ 地形、实体、粒子...                                        │
│                                                                  │
│  2. RenderWorldLastEvent ─────────────────────────┐              │
│     │                                              │              │
│     │  ┌─────────────────────────────────────┐    │              │
│     └──│ TakoRender WORLD_3D 层               │    │              │
│        │  • 同步维度 ID                        │    │              │
│        │  • world.update(WORLD_3D, partialTicks)│   │              │
│        │  • world.render(WORLD_3D)             │    │              │
│        │  • 3D 物体、粒子、线条等               │    │              │
│        └─────────────────────────────────────┘    │              │
│                                                    │              │
│  3. Angelica/OptiFine 后处理（光影）               │              │
│     └─ WORLD_3D 内容会被后处理影响 ◄───────────────┘              │
│                                                                  │
│  4. RenderGameOverlayEvent.Post ─────────────────┐               │
│     │                                             │               │
│     │  ┌─────────────────────────────────────┐   │               │
│     └──│ TakoRender HUD 层                    │   │               │
│        │  • world.update(HUD, partialTicks)   │   │               │
│        │  • world.render(HUD)                 │   │               │
│        │  • 血条、状态图标、小地图等           │   │               │
│        └─────────────────────────────────────┘   │               │
│                                                   │               │
│  5. GuiScreenEvent.DrawScreenEvent.Post ─────────┴─┐             │
│     │                                               │             │
│     │  ┌─────────────────────────────────────┐     │             │
│     └──│ TakoRender GUI 层                    │     │             │
│        │  • world.update(GUI, partialTicks)   │     │             │
│        │  • world.render(GUI)                 │     │             │
│        │  • 界面元素、对话框等                 │     │             │
│        └─────────────────────────────────────┘     │             │
│                                                     │             │
│  HUD 和 GUI 不受光影后处理影响 ◄────────────────────┘             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.5 Layer 与 Forge 事件对应

| Layer | Forge 事件 | 渲染时机 | 后处理影响 |
|-------|-----------|----------|-----------|
| WORLD_3D | RenderWorldLastEvent | MC 渲染后、光影前 | ✅ 受影响 |
| HUD | RenderGameOverlayEvent.Post(ALL) | 所有 HUD 元素后 | ❌ 不受影响 |
| GUI | DrawScreenEvent.Post | GUI 绘制后 | ❌ 不受影响 |

### 2.6 生命周期管理

```
┌───────────────────────────────────────────────────────────┐
│                     Entity 生命周期                        │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  创建方式                                                   │
│  ────────                                                  │
│  TakoRender.getWorld().createEntity()                      │
│      .addComponent(new TransformComponent())               │
│      .addComponent(new LifetimeComponent()                 │
│          .setLifetime(Lifetime.XXX));                      │
│                                                            │
│  生命周期类型                                               │
│  ────────────                                              │
│  TRANSIENT  ─── 定时销毁（例：粒子、临时特效）              │
│      └─ setDuration(5.0f) → 5 秒后自动销毁                 │
│                                                            │
│  SESSION ───── 存档退出时销毁（例：玩家相关效果）           │
│      └─ ClientDisconnectionFromServerEvent 触发清理        │
│                                                            │
│  VIEW ──────── 相机切换时销毁（例：视角特效）               │
│      └─ 活动相机变更时清理                                  │
│                                                            │
│  MANUAL ────── 手动销毁（例：长期存在的物体）               │
│      └─ 调用 lifetime.markForDestroy()                     │
│      └─ 或 world.removeEntity(id)                          │
│                                                            │
│  销毁流程                                                   │
│  ────────                                                  │
│  markForDestroy() → LifetimeSystem 检测 → 下一帧销毁       │
│                                                            │
│  资源清理                                                   │
│  ────────                                                  │
│  Entity 删除时，自动调用 Disposable Component 的 dispose() │
│                                                            │
└───────────────────────────────────────────────────────────┘
```

### 2.7 快速入门示例

```java
// 其他 Mod 中使用 TakoRender

// 1. 获取全局 World（TakoRender 已初始化）
World world = TakoRender.getWorld();

// 2. 创建 3D 世界中的物体
Entity cube = world.createEntity();
cube.addComponent(new TransformComponent()
    .setPosition(100, 64, 200));  // MC 世界坐标
cube.addComponent(new LayerComponent()
    .setLayer(Layer.WORLD_3D));   // 在 3D 世界渲染
cube.addComponent(new DimensionComponent()
    .setDimensionId(0));          // 主世界
cube.addComponent(new VisibilityComponent());
cube.addComponent(new MeshRendererComponent()
    .setMesh(myMesh)
    .setMaterial(myMaterial));
cube.addComponent(new LifetimeComponent()
    .setLifetime(Lifetime.SESSION));  // 退出存档时销毁

// 3. 创建 HUD 元素
Entity healthBar = world.createEntity();
healthBar.addComponent(new TransformComponent()
    .setPosition(10, 10, 0));     // 屏幕坐标
healthBar.addComponent(new LayerComponent()
    .setLayer(Layer.HUD));        // 在 HUD 渲染
healthBar.addComponent(new SpriteComponent()
    .setTexture(healthTexture));
healthBar.addComponent(new LifetimeComponent()
    .setLifetime(Lifetime.SESSION));

// 不需要手动调用 update/render
// TakoRender 的 RenderEventHandler 会自动处理
```

### 2.8 维度筛选

```java
// 只在主世界显示
entity.addComponent(new DimensionComponent().setDimensionId(0));

// 只在下界显示
entity.addComponent(new DimensionComponent().setDimensionId(-1));

// 只在末地显示
entity.addComponent(new DimensionComponent().setDimensionId(1));

// 不添加 DimensionComponent = 所有维度都显示
```

TakoRender 自动同步 MC 当前维度，只渲染匹配的 Entity。

---

## 三、ECS 核心架构

### 3.1 核心类

#### World（世界）

ECS 的核心容器，管理所有 Entity 和 System。

```java
World world = new World();

// 创建实体
Entity entity = world.createEntity();

// 添加系统
world.addSystem(new MeshRenderSystem());

// 每帧调用
world.update(deltaTime);  // UPDATE 阶段
world.render(deltaTime);  // RENDER 阶段
```

**主要方法**：

| 方法 | 说明 |
|------|------|
| `createEntity()` | 创建新实体 |
| `removeEntity(long id)` | 移除实体（自动清理 Disposable 资源） |
| `getEntitiesWith(Class<T>...)` | 查询拥有指定 Component 的实体 |
| `addSystem(GameSystem)` | 添加系统（按优先级排序） |
| `getSystem(Class<T>)` | 获取指定类型的系统 |
| `update(float deltaTime)` | 执行 UPDATE 阶段系统 |
| `render(float deltaTime)` | 执行 RENDER 阶段系统 |
| `update(Layer, float)` | 按层执行 UPDATE |
| `render(Layer)` | 按层执行 RENDER |
| `findActiveCamera()` | 查找活动相机实体 |
| `clearAllEntities()` | 清空所有实体（保留系统） |

#### Entity（实体）

仅作为 ID 和 Component 容器，不包含任何逻辑。

```java
Entity entity = world.createEntity();

// 链式添加组件
entity.addComponent(new TransformComponent().setPosition(0, 64, 0))
      .addComponent(new MeshRendererComponent().setMesh(mesh).setMaterial(material));

// 获取组件
Optional<TransformComponent> transform = entity.getComponent(TransformComponent.class);

// 检查组件
if (entity.hasComponent(MeshRendererComponent.class)) {
    // ...
}
```

#### Component（组件）

纯数据对象，只有 getter/setter，不包含业务逻辑。

```java
public class TransformComponent extends Component {
    private Vector3f position = new Vector3f();
    private float pitch, yaw, roll;
    private Vector3f scale = new Vector3f(1, 1, 1);

    // 只有 getter/setter
    public Vector3f getPosition() { return new Vector3f(position); }
    public TransformComponent setPosition(float x, float y, float z) {
        position.set(x, y, z);
        return this;
    }
}
```

#### GameSystem（系统）

唯一的逻辑执行单元，处理具有特定 Component 组合的 Entity。

```java
@RequiresComponent({ MeshRendererComponent.class, TransformComponent.class })
public class MeshRenderSystem extends GameSystem {

    @Override
    public Phase getPhase() {
        return Phase.RENDER;  // 渲染阶段执行
    }

    @Override
    public int getPriority() {
        return 0;  // 优先级（数字小的先执行）
    }

    @Override
    public void update(float deltaTime) {
        for (Entity entity : getRequiredEntities()) {
            // 渲染逻辑
        }
    }
}
```

### 3.2 执行阶段（Phase）

| 阶段 | 用途 | 示例系统 |
|------|------|----------|
| `UPDATE` | 逻辑更新 | TransformSystem、CameraSystem、ParticleEmitSystem |
| `RENDER` | 渲染绘制 | MeshRenderSystem、LineRenderSystem、PostProcessSystem |

### 3.3 渲染层（Layer）

| 层 | 用途 | 特点 |
|-----|------|------|
| `WORLD_3D` | 3D 世界空间 | 参与后处理、受维度筛选 |
| `HUD` | 游戏内覆盖 | 血条、状态栏 |
| `GUI` | 界面 | 菜单、背包 |

```java
// 按层渲染
world.update(Layer.WORLD_3D, deltaTime);
world.render(Layer.WORLD_3D);

world.update(Layer.HUD, deltaTime);
world.render(Layer.HUD);
```

### 3.4 资源清理（Disposable）

Component 可实现 `Disposable` 接口，Entity 删除时自动清理资源。

```java
public class ParticleBufferComponent extends Component implements Disposable {
    private ParticleBuffer gpuBuffer;

    @Override
    public void dispose() {
        if (gpuBuffer != null) {
            gpuBuffer.close();
            gpuBuffer = null;
        }
    }
}
```

---

## 四、Component 参考

### 4.1 核心组件

#### TransformComponent

位置、旋转、缩放变换。

| 字段 | 类型 | 说明 |
|------|------|------|
| position | Vector3f | 世界位置 |
| pitch/yaw/roll | float | 欧拉角旋转（度） |
| scale | Vector3f | 缩放 |
| worldMatrix | Matrix4f | 计算后的世界矩阵（只读） |
| forward/up/right | Vector3f | 方向向量（只读） |

```java
new TransformComponent()
    .setPosition(0, 64, 0)
    .setRotation(0, 45, 0)
    .setScale(2, 2, 2);
```

#### CameraComponent

相机投影与视图参数。**依赖**: TransformComponent

| 字段 | 类型 | 说明 |
|------|------|------|
| projectionType | PERSPECTIVE/ORTHOGRAPHIC | 投影类型 |
| vFov | float | 垂直视场角（度） |
| aspectRatio | float | 宽高比 |
| nearPlane/farPlane | float | 近/远裁剪面 |
| active | boolean | 是否为活动相机 |
| syncWithMinecraft | boolean | 是否与 MC 相机同步 |

```java
new CameraComponent()
    .setPerspective(70, 16f/9f, 0.1f, 1000f)
    .setActive(true);
```

### 4.2 渲染组件

#### MeshRendererComponent

网格渲染配置。**依赖**: TransformComponent

| 字段 | 类型 | 说明 |
|------|------|------|
| mesh | Mesh | 网格数据 |
| material | Material | 材质 |
| renderQueue | RenderQueue | 渲染队列（默认 OPAQUE） |
| sortingOrder | int | 排序顺序 |
| castShadows | boolean | 是否投射阴影 |

```java
new MeshRendererComponent()
    .setMesh(cubeMesh)
    .setMaterial(stoneMaterial)
    .setRenderQueue(RenderQueue.OPAQUE);
```

#### LineRendererComponent

线条/线框渲染。**依赖**: TransformComponent

| 字段 | 类型 | 说明 |
|------|------|------|
| shape | LINE/BOX/SPHERE/CUSTOM | 形状类型 |
| size | Vector3f | 形状尺寸 |
| colorR/G/B/A | float | 颜色 |
| lineWidth | float | 线宽 |
| colorGradient | boolean | 是否启用颜色渐变 |
| depthTest | boolean | 是否启用深度测试 |

```java
new LineRendererComponent()
    .setShape(LineShape.BOX)
    .setSize(1, 2, 1)
    .setColor(1, 0, 0, 1)
    .setLineWidth(2);
```

#### PostProcessComponent

后处理效果配置。**依赖**: CameraComponent

| 字段 | 类型 | 说明 |
|------|------|------|
| bloomEnabled | boolean | 启用 Bloom |
| bloomThreshold | float | Bloom 阈值（0-2） |
| bloomIntensity | float | Bloom 强度（0-5） |
| exposure | float | 曝光（0.1-10） |
| tonemapEnabled | boolean | 启用色调映射 |

```java
new PostProcessComponent()
    .setBloomEnabled(true)
    .setBloomThreshold(0.8f)
    .setBloomIntensity(1.5f)
    .setExposure(1.0f);
```

### 4.3 可见性组件

#### VisibilityComponent

控制实体可见性。

| 字段 | 类型 | 说明 |
|------|------|------|
| visible | boolean | 用户控制的可见性 |
| culled | boolean | 系统设置的剔除标记（只读） |

```java
visibility.setVisible(false);  // 隐藏
boolean shouldRender = visibility.shouldRender();  // visible && !culled
```

#### LayerComponent

渲染层标记。

```java
new LayerComponent().setLayer(Layer.HUD);
```

#### DimensionComponent

Minecraft 维度分组。

```java
new DimensionComponent().setDimensionId(0);  // 主世界
// -1 = 下界, 1 = 末地
```

### 4.4 生命周期组件

#### LifetimeComponent

自动生命周期管理。

| 类型 | 说明 |
|------|------|
| TRANSIENT | 定时销毁（duration 秒后） |
| VIEW | 切换相机时销毁 |
| SESSION | 退出存档时销毁 |
| MANUAL | 手动销毁 |

```java
new LifetimeComponent()
    .setLifetime(Lifetime.TRANSIENT)
    .setDuration(5.0f);  // 5 秒后自动销毁
```

### 4.5 粒子组件

| 组件 | 用途 |
|------|------|
| ParticleEmitterComponent | 发射参数（形状、速率、颜色等） |
| ParticleStateComponent | 运行时状态（时间、计数） |
| ParticleBufferComponent | GPU/CPU 缓冲区 |
| ParticleRenderComponent | 渲染配置（混合模式、纹理） |

---

## 五、System 参考

### 5.1 UPDATE 阶段系统

| 系统 | 优先级 | 职责 |
|------|--------|------|
| TransformSystem | -1000 | 计算世界矩阵、方向向量 |
| CameraSystem | 100 | 计算投影/视图矩阵 |
| LightProbeSystem | - | 采样 MC 光照 |
| ParticleEmitSystem | - | 粒子发射 |
| ParticlePhysicsSystem | - | 粒子物理模拟 |
| LifetimeSystem | - | 生命周期管理 |
| TrailSystem | - | 拖尾点记录 |
| FrustumCullingSystem | - | 视锥剔除 |
| LODSystem | - | LOD 级别切换 |

### 5.2 RENDER 阶段系统

| 系统 | 优先级 | 职责 |
|------|--------|------|
| MeshRenderSystem | 0 | 网格批量渲染 |
| InstancedRenderSystem | - | 实例化渲染 |
| LineRenderSystem | 100 | 线条渲染 |
| SpriteRenderSystem | - | 2D 精灵渲染 |
| ParticleRenderSystem | - | 粒子渲染 |
| PostProcessSystem | - | 后处理效果 |
| WorldSpaceUISystem | - | 3D→2D UI 投影 |
| DebugRenderSystem | 1000 | 调试可视化 |

---

## 六、资源管理系统

### 6.1 ShaderManager

着色器资源管理，单例模式。

```java
// 获取实例
ShaderManager manager = ShaderManager.instance();

// 获取内置 Shader
ResourceHandle<ShaderProgram> shader = manager.get(ShaderManager.SHADER_WORLD3D_LIT);

// 使用
shader.get().use();

// 释放引用
shader.release();
```

**内置 Shader 键**：

| 键 | 用途 |
|-----|------|
| `SHADER_WORLD3D` | 3D 世界渲染（无光照） |
| `SHADER_WORLD3D_LIT` | 3D 世界渲染（带光照） |
| `SHADER_LINE` | 线条渲染 |
| `SHADER_MODEL` | 模型渲染 |
| `SHADER_INSTANCED` | 实例化渲染 |
| `SHADER_PARTICLE` | 粒子渲染 |
| `SHADER_BRIGHTNESS_EXTRACT` | Bloom 亮度提取 |
| `SHADER_BLUR` | 高斯模糊 |
| `SHADER_COMPOSITE` | 后处理合成 |

### 6.2 资源引用计数

```java
ResourceHandle<ShaderProgram> handle = manager.get("myshader");
// refCount = 1

handle.release();
// refCount = 0, 资源可被清理

// 预加载（加载后立即释放，保持缓存）
manager.preload("myshader");
```

---

## 七、渲染管线

### 7.1 渲染队列（RenderQueue）

| 队列 | 优先级 | 特点 |
|------|--------|------|
| BACKGROUND | 1000 | 禁用深度写入 |
| OPAQUE | 2000 | 正常深度测试，前到后渲染 |
| TRANSPARENT | 3000 | 禁用深度写入，后到前排序 |
| OVERLAY | 4000 | 禁用深度测试 |

### 7.2 每帧渲染流程

```
1. UPDATE 阶段
   ├─ TransformSystem: 计算世界矩阵
   ├─ CameraSystem: 计算 VP 矩阵
   ├─ FrustumCullingSystem: 视锥剔除
   └─ 其他 UPDATE 系统...

2. RENDER 阶段
   ├─ MeshRenderSystem:
   │   ├─ 收集可渲染实体
   │   ├─ 按 RenderQueue 分组
   │   ├─ 按材质排序（减少状态切换）
   │   ├─ 透明物体按深度排序
   │   └─ 分队列渲染
   ├─ LineRenderSystem: 线条渲染
   ├─ ParticleRenderSystem: 粒子渲染
   └─ PostProcessSystem: 后处理
```

### 7.3 GLStateContext 使用

所有渲染必须在 GLStateContext 作用域内执行：

```java
try (var ctx = GLStateContext.begin()) {
    ctx.enableBlend();
    ctx.setBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    ctx.enableDepthTest();
    ctx.setDepthMask(true);

    // 渲染代码...

} // 自动恢复 GL 状态
```

---

## 八、使用指南

### 8.1 快速开始

```java
// 1. 创建 World
World world = new World();

// 2. 添加系统
world.addSystem(new TransformSystem());
world.addSystem(new CameraSystem());
world.addSystem(new MeshRenderSystem());

// 3. 创建相机
Entity camera = world.createEntity();
camera.addComponent(new TransformComponent().setPosition(0, 64, -10));
camera.addComponent(new CameraComponent()
    .setPerspective(70, 16f/9f, 0.1f, 1000f)
    .setActive(true));

// 4. 创建物体
Entity cube = world.createEntity();
cube.addComponent(new TransformComponent().setPosition(0, 64, 0));
cube.addComponent(new MeshRendererComponent()
    .setMesh(cubeMesh)
    .setMaterial(material));

// 5. 每帧调用
public void onRenderTick(float deltaTime) {
    world.update(deltaTime);
    world.render(deltaTime);
}
```

### 8.2 透明物体渲染

```java
// 设置透明材质
entity.addComponent(new MeshRendererComponent()
    .setMesh(mesh)
    .setMaterial(transparentMaterial)
    .setRenderQueue(RenderQueue.TRANSPARENT));  // 重要！
```

### 8.3 后处理效果

```java
// 给相机添加后处理
camera.addComponent(new PostProcessComponent()
    .setBloomEnabled(true)
    .setBloomThreshold(0.8f)
    .setBloomIntensity(1.5f));
```

### 8.4 粒子系统

```java
Entity particles = world.createEntity();
particles.addComponent(new TransformComponent().setPosition(0, 64, 0));
particles.addComponent(new ParticleEmitterComponent()
    .setShape(EmitterShape.SPHERE)
    .setEmissionRate(100)
    .setLifetime(1.0f, 2.0f)
    .setSpeed(5.0f));
particles.addComponent(new ParticleStateComponent());
particles.addComponent(new ParticleBufferComponent(10000));
particles.addComponent(new ParticleRenderComponent());
```

### 8.5 调试可视化

```java
// 添加调试系统
world.addSystem(new DebugRenderSystem());

// 运行时切换模式
DebugRenderSystem debug = world.getSystem(DebugRenderSystem.class);
debug.setMode(DebugMode.BOUNDING_BOX);  // 显示包围盒
debug.cycleMode();  // 切换到下一个模式
```

---

## 九、注意事项

### 9.1 ECS 原则

| 原则 | 说明 |
|------|------|
| Entity 无逻辑 | Entity 只是 ID + Component 容器 |
| Component 纯数据 | Component 只有 getter/setter，无业务逻辑 |
| System 唯一逻辑 | 所有逻辑都在 System 中实现 |

### 9.2 GL 状态管理

```java
// ✅ 正确：使用 GLStateContext
try (var ctx = GLStateContext.begin()) {
    ctx.enableBlend();
    // 渲染...
}

// ❌ 禁止：直接使用 glPushAttrib
GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);  // 会导致栈溢出
```

### 9.3 资源释放

```java
// ✅ 正确：使用 ResourceHandle
ResourceHandle<ShaderProgram> handle = manager.get("shader");
try {
    handle.get().use();
} finally {
    handle.release();  // 必须释放
}

// 或者使用 try-with-resources（如果实现了 AutoCloseable）
```

### 9.4 透明物体

- 必须设置 `RenderQueue.TRANSPARENT`
- 系统会自动按深度排序（远→近）
- 不应写入深度缓冲

### 9.5 Shader 编写

```glsl
#version 330 core  // 必须是 330 core

// ❌ 禁止中文注释
// ❌ 禁止全角符号

in vec3 aPosition;
out vec3 vPosition;

void main() {
    // 使用英文注释
    gl_Position = uViewProjection * vec4(aPosition, 1.0);
}
```

### 9.6 性能建议

| 建议 | 说明 |
|------|------|
| 材质排序 | 减少着色器和纹理切换 |
| 实例化渲染 | 对大量相同物体使用 StaticFlagsComponent.BATCHING |
| 视锥剔除 | 添加 BoundsComponent 启用自动剔除 |
| LOD | 使用 LODComponent 根据距离切换网格 |
| 资源预加载 | 使用 ShaderManager.preloadAll() |

### 9.7 常见问题

**Q: 物体不显示？**
1. 检查是否有 TransformComponent
2. 检查 VisibilityComponent.visible
3. 检查是否有活动相机
4. 检查 Layer 是否匹配

**Q: 透明物体顺序错误？**
1. 确保使用 RenderQueue.TRANSPARENT
2. 检查 sortingOrder 设置

**Q: 资源泄漏？**
1. 确保 ResourceHandle.release() 被调用
2. 实现 Disposable 接口的 Component 会自动清理

---

**文档版本**: 1.0
**适用版本**: TakoRender 当前 main 分支
