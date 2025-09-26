# Graphene

**Version 4.4.3**

Graphene is a graph visualization tool for LMNtal (Language for Modeling with Nested membranes). It provides an interactive interface for visualizing and manipulating graph structures, with support for real-time graph rewriting and force-based layout algorithms.

## Features

- Interactive graph visualization with drag-and-drop node manipulation
- Force-based layout algorithms for automatic graph arrangement
- Real-time graph rewriting visualization
- Multi-selection support with Shift+click
- Depth-based node coloring
- Heat-up functionality to untangle link crossings
- Anti-aliasing and multi-core processing support

## Requirements

### System Requirements
- **Java**: JDK version >= 11 (Java 17 recommended for best performance)
- **sbt**: Scala Build Tool (See https://www.scala-sbt.org/download/)
  - sbt will automatically download Scala 2.13.13 as specified in build.sbt
- **Platform**: Windows, macOS, Linux

### For M1 Mac Users
This project includes `.jvmopts` configuration for optimal compatibility with Apple Silicon Macs.

## Development Setup

### Quick Start
```bash
# Clone the repository
git clone <repository-url>
cd Graphene

# Build the project
sbt assembly
```

### Recommended Java Version
For the best experience, especially on M1 Macs, use Java 17:
```bash
# Install Java 17 (macOS with Homebrew)
brew install openjdk@17

# Set JAVA_HOME for current session
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

### Development Environment
```bash
# Clean build
sbt clean compile

# Run tests (if available)
sbt test

# Create assembly JAR
sbt assembly
```

## Build

### Standard Build
```bash
sbt assembly
```

### Troubleshooting Build Issues

**If you encounter `sbt.internal.ServerAlreadyBootingException`:**
```bash
sbt --batch -Dsbt.server.forcestart=true assembly
```

**For M1 Mac warning issues:**
The project includes a `.jvmopts` file that suppresses common M1 Mac JVM warnings. If you still encounter issues, ensure you're using Java 17.

**For memory issues:**
```bash
export SBT_OPTS="-Xmx2G -XX:+UseConcMarkSweepGC"
sbt assembly
```

## Integration with LaViT

Graphene is designed to be integrated with LaViT (LMNtal Visual Tool) and is not intended for standalone use. The built JAR file should be placed in the appropriate LaViT directory structure.

### Controls
- **Node Movement**: Drag nodes to move them
- **Screen Movement**: Drag empty space to pan the view
- **Multi-Selection**: Shift+click to select multiple nodes
- **Depth Coloring**: Right-click selected nodes to color by depth
- **Zooming**: Use mouse wheel to zoom in/out

## Release Process

```bash
# Update version
vim build.sbt # バージョン番号を更新
vim template/README.md # 更新情報を記述

# Clean build
rm -rf target
sbt assembly
./pack.sh

# Release
# target/graphene-x.x.x.zipをアップロード
git checkout develop
git merge release/vx.x.x
git tag -a vx.x.x -m "Version x.x.x"
```

**Release Guidelines:**
- リリース時にはbuild.sbtのバージョンを更新し、git tagでタグ付けをする
- バージョンはx.x.xの形式にし、前から順にmajor、minor、patchとする
- 詳しくは[Semantic Versioning](http://semver.org/spec/v2.0.0.html)を参考にする

## Technical Documentation

### Coordinate Systems
The application uses two coordinate systems:
- **Screen Coordinates**: Corresponds to `java.awt.Graphics` coordinate system
- **World Coordinates**: The coordinate system where nodes exist, which can be transformed (position and scale) to map to screen coordinates

### Recent Updates
- **Scala 2.13 Upgrade**: Upgraded from Scala 2.11.12 to 2.13.13 for improved performance and modern language features
- **Warning Reduction**: Significantly reduced compilation warnings from 100+ to 10
- **M1 Mac Compatibility**: Added JVM options for optimal performance on Apple Silicon
- **Dependency Updates**: Updated all libraries to Scala 2.13 compatible versions

For detailed information about the Scala upgrade, see [SCALA_UPGRADE_SUMMARY.md](SCALA_UPGRADE_SUMMARY.md).

## Configuration

### JVM Options
The project includes a `.jvmopts` file with optimized settings for:
- M1 Mac compatibility
- Warning suppression
- Memory management

### Properties
Application settings are stored in `.properties` files:
- SLIM path configuration
- UI preferences
- Performance settings

## Troubleshooting

### Common Issues

**Build fails with "Unknown Scala version"**
```bash
# Ensure you're using a compatible sbt version
sbt about
# Should show sbt 1.11.6 and Scala 2.13.13
```

**M1 Mac shows many JVM warnings**
```bash
# Ensure Java 17 is being used
java -version
# Should show version 17.x.x

# Verify .jvmopts is present
ls -la .jvmopts
```

**SLIM path errors**
- Configure SLIM path in LaViT settings
- Default path is set for LaViT installations

## Contributing

### Development Workflow
1. Ensure Java 17 and sbt are installed
2. Clone the repository
3. Run `sbt compile` to verify setup
4. Make changes and test with `sbt assembly`
5. Follow the existing code style and patterns

### Code Quality
- The project maintains zero deprecation warnings
- Use `sbt compile` to check for warnings before committing
- Follow Scala 2.13 best practices

## License

[Add license information here]

## Links

- [LMNtal Official Site](https://www.ueda.info.waseda.ac.jp/lmntal/)
- [Scala Documentation](https://docs.scala-lang.org/)
- [sbt Documentation](https://www.scala-sbt.org/documentation.html)