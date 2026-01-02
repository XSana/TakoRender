# TakoRender

Modern ECS-based rendering framework for Minecraft 1.7.10

## Overview

TakoRender is a rendering framework designed for Minecraft 1.7.10 that provides:
- **Entity-Component-System (ECS)** architecture for clean separation of data and logic
- **OpenGL 3.3 Core Profile** rendering with modern shader support
- **GPU Particle System** with Compute Shader acceleration
- **Post-Processing** effects (Bloom, HDR)

## Features

### ECS Architecture
- `Entity` - ID container with component management
- `Component` - Pure data objects (Transform, Camera, Material, etc.)
- `GameSystem` - Logic processors with Phase (UPDATE/RENDER) separation
- `World` - ECS world manager with O(1) component indexing

### Rendering Systems
- **SpriteBatch** - 2D quad batch rendering
- **World3DBatch** - 3D primitive rendering in world space
- **World3DBatchLit** - 3D rendering with MC lighting support
- **MeshRenderSystem** - Automatic mesh rendering with material sorting
- **LineRenderSystem** - Debug line/wireframe rendering

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
│   └── particle/               # Particle internals (Buffer, Compute, Renderer)
└── proxy/                      # Client/Server proxies
```

## Usage Example

```java
// Create ECS world
World world = new World();

// Create camera entity
Entity camera = world.createEntity("MainCamera");
camera.addComponent(new TransformComponent().setPosition(0, 64, 0));
camera.addComponent(new CameraComponent().setActive(true).setFov(70));

// Create particle effect
Entity fire = world.createEntity("Fire");
fire.addComponent(new TransformComponent().setPosition(10, 65, 10));
ParticlePresets.applyFire(fire, 1.0f);

// Register systems
world.addSystem(new CameraSystem());
world.addSystem(new ParticleUpdateSystem());
world.addSystem(new ParticleRenderSystem());

// Game loop
world.update(deltaTime);  // UPDATE phase
world.render(deltaTime);  // RENDER phase
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
