# TakoRender

Minecraft 1.7.10 现代化 ECS 渲染框架

## 概述

TakoRender 是为 Minecraft 1.7.10 设计的渲染框架，提供现代化的实体-组件-系统（ECS）架构来管理渲染逻辑。该框架旨在使复杂的渲染任务更易于管理和维护。

## 特性

- **ECS 架构**：使用实体-组件-系统模式清晰分离数据和逻辑
- **灵活的组件系统**：易于扩展的基于组件的架构
- **现代 Java 语法**：使用 Jabel 启用现代 Java 语法，同时仍针对 JVM 8

## 项目结构

```
src/main/java/moe/takochan/takorender/
├── TakoRenderMod.java          # Mod 主类
├── Reference.java              # 常量和引用
├── api/                        # 公共 API
│   ├── ecs/                    # ECS 核心类
│   │   ├── Entity.java         # 实体实现
│   │   ├── Component.java      # 组件基类
│   │   ├── GameSystem.java     # 系统基类
│   │   └── World.java          # ECS 世界管理器
│   └── component/              # 内置组件
│       ├── TransformComponent.java  # 位置、旋转、缩放
│       └── CameraComponent.java     # 相机属性
├── core/                       # 内部实现（预留）
└── proxy/                      # 客户端/服务端代理类
    ├── CommonProxy.java
    └── ClientProxy.java
```

## 构建命令

```bash
# 构建 Mod
./gradlew build

# 运行 Minecraft 客户端测试
./gradlew runClient

# 使用指定用户名运行
./gradlew runClient --username=YourName

# 检查代码格式（Spotless）
./gradlew spotlessCheck

# 应用代码格式化
./gradlew spotlessApply
```

## 环境要求

- Minecraft 1.7.10
- Forge 10.13.4.1614+
- Java 8+

## 开发依赖

以下 Mod 用于开发/测试：
- NotEnoughItems (NEI)
- Waila
- Angelica

## 许可证

本项目采用 GNU General Public License v3.0 许可证 - 详见 [LICENSE](LICENSE) 文件。
