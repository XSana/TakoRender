# TakoRender

[![JitPack](https://jitpack.io/v/XSana/TakoRender.svg)](https://jitpack.io/#XSana/TakoRender)

English | [中文](README_CN.md)

Modern ECS-based rendering framework for Minecraft 1.7.10

## Installation

Add to your `build.gradle`:

```gradle
repositories {
    maven {
        url 'https://jitpack.io'
        content {
            includeGroupByRegex 'com\\.github\\..+'
        }
    }
}

dependencies {
    implementation 'com.github.XSana:TakoRender:VERSION'
}
```

Replace `VERSION` with the latest version shown in the badge above.

## Overview

TakoRender is a rendering framework designed for Minecraft 1.7.10 that provides:
- **Entity-Component-System (ECS)** architecture for clean separation of data and logic
- **OpenGL 3.3 Core Profile** rendering with modern shader support
- **GPU Particle System** with Compute Shader acceleration
- **Post-Processing** effects (Bloom, HDR)
- **Performance Optimizations** (Frustum Culling, LOD, Instanced Rendering)

## Features

### ECS Architecture
- `Entity` - ID container with component management
- `Component` - Pure data objects (Transform, Camera, Material, etc.)
- `GameSystem` - Logic processors with Phase (UPDATE/RENDER) separation
- `World` - ECS world manager with O(1) component indexing

### Rendering Systems
- **MeshRenderSystem** - Automatic mesh rendering with material sorting
- **InstancedRenderSystem** - Batch rendering for static objects (same Mesh+Material)
- **LineRenderSystem** - Debug line/wireframe rendering
- **SpriteRenderSystem** - 2D sprite rendering for UI layers

### Batch Rendering
- **SpriteBatch** - 2D quad batch rendering
- **World3DBatch** - 3D primitive rendering in world space
- **World3DBatchLit** - 3D rendering with MC lighting support

### Resource Management
- **ShaderManager** - Shader loading, caching, and preloading
- **TextureManager** - Texture loading with reference counting
- **ResourceHandle** - Smart resource references with auto-release

### Particle System
- GPU-accelerated with Compute Shaders (OpenGL 4.3+)
- CPU fallback for compatibility (OpenGL 3.3)
- Customizable emitters, forces, and collision
- Color/Size/Velocity over lifetime curves

### Post-Processing
- Bloom with configurable threshold and intensity
- Gaussian blur with ping-pong buffers
- HDR tone mapping

### Performance Optimizations
- **Frustum Culling** - Skip rendering objects outside camera view
- **LOD System** - Distance-based mesh switching with hysteresis
- **Instanced Rendering** - Single draw call for multiple identical objects
- **BoundsComponent** - AABB bounding boxes for culling

### Debug Tools
- **SystemProfiler** - Per-system timing statistics
- **EntityInspector** - Runtime entity/component inspection
- **DebugRenderSystem** - Wireframe, bounding box, LOD level visualization

### GL State Management
- `GLStateContext` - Automatic state save/restore (replaces glPushAttrib)
- Zero GL stack consumption
- Thread-safe state tracking

## Project Structure

```
src/main/java/moe/takochan/takorender/
├── api/                        # Public API
│   ├── ecs/                    # ECS core (Entity, Component, GameSystem, World)
│   ├── component/              # Built-in components
│   ├── system/                 # Built-in systems
│   ├── graphics/               # Rendering primitives (Batch, Mesh, Shader, Material)
│   ├── particle/               # Particle system API
│   └── resource/               # Resource management (ShaderManager, TextureManager)
├── core/                       # Internal implementation
│   ├── gl/                     # GL utilities (GLStateContext, FrameBuffer)
│   ├── debug/                  # Debug tools (Profiler, Inspector)
│   ├── render/                 # Render utilities (BatchKey, InstanceBuffer)
│   ├── system/                 # Core systems (Frustum, LOD, Instanced)
│   └── particle/               # Particle internals (Buffer, Compute, Renderer)
└── proxy/                      # Client/Server proxies
```

## Usage Example

```java
// Create ECS world
World world = new World();

// Create camera entity
Entity camera = world.createEntity();
camera.addComponent(new TransformComponent().setPosition(0, 64, 0));
camera.addComponent(new CameraComponent().setActive(true).setFov(70));

// Create renderable entity with LOD
Entity tree = world.createEntity();
tree.addComponent(new TransformComponent().setPosition(10, 64, 10));
tree.addComponent(new MeshRendererComponent()
    .setMesh(highDetailMesh)
    .setMaterial(treeMaterial));
tree.addComponent(new LODComponent()
    .addLevel(20.0f, highDetailMesh)
    .addLevel(50.0f, mediumDetailMesh)
    .addLevel(100.0f, lowDetailMesh));
tree.addComponent(new BoundsComponent().setLocalBounds(treeBounds));
tree.addComponent(new VisibilityComponent());

// Mark for instanced rendering (optional)
tree.addComponent(new StaticFlagsComponent(StaticFlags.BATCHING));

// Register systems
world.addSystem(new CameraSystem());
world.addSystem(new TransformSystem());
world.addSystem(new LODSystem());
world.addSystem(new FrustumCullingSystem());
world.addSystem(new InstancedRenderSystem());
world.addSystem(new MeshRenderSystem());

// Enable profiling (optional)
world.getProfiler().setEnabled(true);

// Game loop
world.update(Layer.WORLD_3D, deltaTime);  // UPDATE phase
world.render(Layer.WORLD_3D);              // RENDER phase

// Print profiler report
System.out.println(world.getProfiler().getReport());
```

## Build Commands

```bash
./gradlew build              # Build the mod
./gradlew spotlessApply      # Format code
./gradlew runClient          # Run test client
```

## Requirements

- Minecraft 1.7.10 (GTNH 2.8.0+)
- Forge 10.13.4.1614+
- Java 8+ (compiled with Jabel for modern syntax)
- OpenGL 3.3+ (4.3+ for GPU particles)

## License

GNU General Public License v3.0 - see [LICENSE](LICENSE)
