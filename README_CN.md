# TakoRender

[English](README.md) | 中文

Minecraft 1.7.10 现代化 ECS 渲染框架

## 概述

TakoRender 是为 Minecraft 1.7.10 设计的渲染框架，提供：
- **实体-组件-系统 (ECS)** 架构，数据与逻辑清晰分离
- **OpenGL 3.3 Core Profile** 渲染，支持现代着色器
- **GPU 粒子系统**，Compute Shader 加速
- **后处理效果**（Bloom、HDR）

## 功能特性

### ECS 架构
- `Entity` - ID 容器，管理组件集合
- `Component` - 纯数据对象（Transform、Camera、Material 等）
- `GameSystem` - 逻辑处理器，支持 Phase（UPDATE/RENDER）分离
- `World` - ECS 世界管理器，O(1) 组件索引

### 渲染系统
- **SpriteBatch** - 2D 四边形批量渲染
- **World3DBatch** - 世界空间 3D 图元渲染
- **World3DBatchLit** - 带 MC 光照的 3D 渲染
- **MeshRenderSystem** - 自动网格渲染，材质排序
- **LineRenderSystem** - 调试线框渲染

### 资源管理
- **ShaderManager** - 着色器加载、缓存、预加载
- **TextureManager** - 纹理加载，引用计数
- **ResourceHandle** - 智能资源引用，自动释放

### 粒子系统
- GPU 加速（OpenGL 4.3+ Compute Shader）
- CPU 回退兼容（OpenGL 3.3）
- 可定制发射器、力场、碰撞
- 颜色/大小/速度生命周期曲线

### 后处理
- Bloom 辉光，可配置阈值和强度
- 高斯模糊（ping-pong 缓冲）
- HDR 色调映射

### GL 状态管理
- `GLStateContext` - 自动状态保存/恢复（替代 glPushAttrib）
- 零 GL 栈消耗
- 线程安全状态追踪

## 项目结构

```
src/main/java/moe/takochan/takorender/
├── api/                        # 公共 API
│   ├── ecs/                    # ECS 核心（Entity, Component, GameSystem, World）
│   ├── component/              # 内置组件
│   ├── system/                 # 内置系统
│   ├── graphics/               # 渲染基础设施（Batch, Mesh, Shader, Material）
│   ├── particle/               # 粒子系统 API
│   └── resource/               # 资源管理（ShaderManager, TextureManager）
├── core/                       # 内部实现
│   ├── gl/                     # GL 工具（GLStateContext, FrameBuffer）
│   └── particle/               # 粒子内部实现（Buffer, Compute, Renderer）
└── proxy/                      # 客户端/服务端代理
```

## 使用示例

```java
// 创建 ECS 世界
World world = new World();

// 创建相机实体
Entity camera = world.createEntity("MainCamera");
camera.addComponent(new TransformComponent().setPosition(0, 64, 0));
camera.addComponent(new CameraComponent().setActive(true).setFov(70));

// 创建粒子效果
Entity fire = world.createEntity("Fire");
fire.addComponent(new TransformComponent().setPosition(10, 65, 10));
ParticlePresets.applyFire(fire, 1.0f);

// 注册系统
world.addSystem(new CameraSystem());
world.addSystem(new ParticleUpdateSystem());
world.addSystem(new ParticleRenderSystem());

// 游戏循环
world.update(deltaTime);  // UPDATE 阶段
world.render(deltaTime);  // RENDER 阶段
```

## 构建命令

```bash
./gradlew build              # 构建 Mod
./gradlew spotlessApply      # 格式化代码
./gradlew runClient          # 运行测试客户端
```

## 环境要求

- Minecraft 1.7.10 (GTNH 2.8.0+)
- Forge 10.13.4.1614+
- Java 8+（使用 Jabel 支持现代语法）
- OpenGL 3.3+（GPU 粒子需要 4.3+）

## 许可证

GNU General Public License v3.0 - 详见 [LICENSE](LICENSE)
