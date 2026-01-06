#!/bin/bash
# Build Miracle Client for all supported Minecraft versions

# Version groups (API changes between these groups)
declare -A VERSION_GROUPS=(
    ["1.21.4"]="1.21 1.21.1 1.21.2 1.21.3 1.21.4"
    ["1.21.8"]="1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10 1.21.11"
)

declare -A FABRIC_VERSIONS=(
    ["1.21.4"]="0.110.5+1.21.4"
    ["1.21.8"]="0.136.1+1.21.8"
)

declare -A LOADER_VERSIONS=(
    ["1.21.4"]="0.16.9"
    ["1.21.8"]="0.18.3"
)

# Create output directory
mkdir -p build/versions

# Build for each version group
for BUILD_VERSION in "${!VERSION_GROUPS[@]}"; do
    echo "Building for Minecraft $BUILD_VERSION (supports: ${VERSION_GROUPS[$BUILD_VERSION]})"
    
    # Update gradle.properties
    sed -i "s/minecraft_version=.*/minecraft_version=$BUILD_VERSION/" gradle.properties
    sed -i "s/yarn_mappings=.*/yarn_mappings=$BUILD_VERSION+build.1/" gradle.properties
    sed -i "s/loader_version=.*/loader_version=${LOADER_VERSIONS[$BUILD_VERSION]}/" gradle.properties
    sed -i "s/fabric_version=.*/fabric_version=${FABRIC_VERSIONS[$BUILD_VERSION]}/" gradle.properties
    
    # Clean and build
    ./gradlew.bat clean build
    
    # Copy JAR with version suffix
    cp build/libs/miracle-client-1.0.0.jar "build/versions/miracle-client-$BUILD_VERSION.jar"
    
    echo "Built: miracle-client-$BUILD_VERSION.jar"
done

echo "All versions built successfully!"
