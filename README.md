# VitaLite

VitaLite is an enhanced RuneLite client loader that provides additional functionality, safety features, and customization options while maintaining compatibility with the core RuneLite experience.


## What is VitaLite?

VitaLite is a RuneLite client wrapper that adds extra features and safety options while keeping the familiar RuneLite experience. It provides additional customization, enhanced security features, and extended plugin support.

## General User Release
Grab latest version from releases and download the zip. Either use the provided scripts for launching, or launch jar manually. If you launch manually just keep in mind it requires jdk11 to run.

### Getting Started
1. **Download**: Get the latest VitaLite release
2. **Launch**: Run the appropriate script for your OS or double-click the JAR
3. **Configure**: Use in-client settings or command line arguments

### Requirements
- Java 11 (jdk)

## Key Features

### 🎮 Enhanced Gaming Experience
- **Plugin Support**: Load additional plugins and external extensions
- **Packet Logging**: Monitor game network traffic for analysis
- **Headless Mode**: Run the client without display for resource saving
- **Console Access**: Built-in logging and debugging tools

### 🛡️ Safety & Privacy Features
- **Incognito Mode**: Appears as regular RuneLite for privacy
- **Identity Protection**: Identified 1:1 as RuneLite itself
- **Detection Avoidance**: Enhanced safety protections

### 🎨 Interface Improvements
- **Enhanced Settings**: Additional configuration options
- **Console Integration**: Real-time logging display
- **Navigation Enhancements**: Improved UI components

## Command Line Options

VitaLite supports several command-line arguments for customization:

### Core Options

| Option       | Type    | Description                                          |
|--------------|---------|------------------------------------------------------|
| `--rsdump`   | String  | Path to dump the gamepack to (optional)              |
| `-noPlugins` | Boolean | Disables loading of core plugins                     |
| `-min`       | Boolean | Runs jvm with minimal alotted memory.                |
| `-noMusic`   | Boolean | Prevent the loading of music tracks                  |
| `-incognito` | Boolean | Visually display as 'RuneLite' instead of 'VitaLite' |
| `-help`      | Boolean | Displays help information about command line options |
| `-proxy`     | String  | Set a proxy server to use (e.g., ip:port or ip:port:username:password) |


### Usage Examples

```bash
# Launch VitaLite normally
java -jar VitaLite-shaded.jar

# Launch in incognito mode (appears as RuneLite)
java -jar VitaLite-shaded.jar -incognito

# Launch without core plugins
java -jar VitaLite-shaded.jar -noPlugins

# Dump gamepack and launch in incognito mode
java -jar VitaLite-shaded.jar --rsdump /path/to/dump -incognito

# Get help information
java -jar VitaLite-shaded.jar -help
```

### File Structure

```
~/.runelite/
└── sideloaded-plugins/    # External plugin JARs
```

## Building

### Prerequisites
- Java 11

### Build Commands

```bash
# First, sync the RuneLite API (required before building)
# Note: You may need to restart ide after running this
./gradlew syncRuneLiteAPI

# Clean and publish everything
./gradlew buildAndPublishAll
```

The main executable will be located at `build/libs/VitaLite-*-shaded.jar`.

### Running from IDE
```
1. Complete build steps above
2. Run the `com.tonic.VitaLite` main class
```

## Troubleshooting
1. Ensure your project jdk and build level are both set to java 11
2. Ensure your gradle version is 8.10 and set the java 11

## Plugin Support

VitaLite supports loading additional plugins:

- Place plugin JAR files in `~/.runelite/sideloaded-plugins/` directory
- Plugins will be automatically loaded on startup

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Disclaimer

VitaLite is a third-party loader for RuneLite. Use at your own risk. The developers are not responsible for any consequences resulting from the use of this software.

## [Buy me a coffee](https://ko-fi.com/tonicbox)