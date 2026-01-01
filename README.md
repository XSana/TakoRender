# TakoRender

Modern ECS-based rendering framework for Minecraft 1.7.10

## Overview

TakoRender is a rendering framework designed for Minecraft 1.7.10 that provides a modern Entity-Component-System (ECS) architecture for managing rendering logic. This framework aims to make complex rendering tasks more manageable and maintainable.

## Features

- **ECS Architecture**: Clean separation of data and logic using Entity-Component-System pattern
- **Flexible Component System**: Easily extensible component-based architecture
- **Modern Java Syntax**: Uses Jabel to enable modern Java syntax while targeting JVM 8

## Project Structure

```
src/main/java/moe/takochan/takorender/
├── TakoRenderMod.java          # Main mod class
├── Reference.java              # Constants and references
├── api/                        # Public API
│   ├── ecs/                    # ECS core classes
│   │   ├── Entity.java         # Entity implementation
│   │   ├── Component.java      # Base component class
│   │   ├── GameSystem.java     # Base system class
│   │   └── World.java          # ECS world manager
│   └── component/              # Built-in components
│       ├── TransformComponent.java  # Position, rotation, scale
│       └── CameraComponent.java     # Camera properties
├── core/                       # Internal implementation (reserved)
└── proxy/                      # Sided proxy classes
    ├── CommonProxy.java
    └── ClientProxy.java
```

## Build Commands

```bash
# Build the mod
./gradlew build

# Run Minecraft client for testing
./gradlew runClient

# Run with specific username
./gradlew runClient --username=YourName

# Check code formatting (Spotless)
./gradlew spotlessCheck

# Apply code formatting
./gradlew spotlessApply
```

## Requirements

- Minecraft 1.7.10
- Forge 10.13.4.1614+
- Java 8+

## Development Dependencies

The following mods are included for development/testing:
- NotEnoughItems (NEI)
- Waila
- Angelica

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
