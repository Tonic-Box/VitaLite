#!/bin/bash

# Configuration
JDK_VERSION="11.0.19+7"
JDK_DIR="$HOME/.runelite/vitalite/jdk"
JAR_NAME="VitaLite.jar"  # Change this to your jar name

# Create directories if needed
mkdir -p "$JDK_DIR"

# Check if JDK already exists
if [ ! -f "$JDK_DIR/Contents/Home/bin/java" ]; then
    echo "JDK not found. Downloading JDK $JDK_VERSION..."
    
    # Detect architecture (Apple Silicon vs Intel)
    ARCH=$(uname -m)
    if [ "$ARCH" = "arm64" ]; then
        ARCH_URL="aarch64"
    else
        ARCH_URL="x64"
    fi
    
    # Download JDK
    JDK_URL="https://api.adoptium.net/v3/binary/version/jdk-11.0.19%2B7/mac/$ARCH_URL/jdk/hotspot/normal/eclipse"
    TEMP_FILE="$JDK_DIR/jdk-temp.tar.gz"
    
    echo "Downloading from: $JDK_URL"
    curl -L "$JDK_URL" -o "$TEMP_FILE" --progress-bar
    
    # Extract JDK
    echo "Extracting JDK..."
    tar -xzf "$TEMP_FILE" -C "$JDK_DIR" --strip-components=1
    rm "$TEMP_FILE"
    
    # On macOS, we might need to clear quarantine attributes
    xattr -r -d com.apple.quarantine "$JDK_DIR" 2>/dev/null || true
    
    echo "JDK installed successfully!"
else
    echo "JDK found at $JDK_DIR"
fi

# Launch the application
echo "Launching application..."
"$JDK_DIR/Contents/Home/bin/java" -jar "$JAR_NAME" "$@"
