#!/bin/bash

# Script to fix Skiko UnsatisfiedLinkError issues
# This script helps resolve native library loading issues with Compose Desktop

echo "🔧 Fixing Skiko UnsatisfiedLinkError issues..."

# Clean Gradle cache and build directories
echo "📁 Cleaning Gradle cache and build directories..."
./gradlew clean
rm -rf ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.skiko/

# Clear Kotlin daemon
echo "🔄 Stopping Kotlin daemon..."
./gradlew --stop

# Rebuild the project
echo "🔨 Rebuilding project..."
./gradlew build

echo "✅ Skiko issue fix completed!"
echo "💡 If the issue persists, try:"
echo "   1. Restart your IDE"
echo "   2. Check Java version compatibility (Java 17+ recommended)"
echo "   3. Ensure you're using the correct Compose Desktop version"
echo "   4. Run: ./gradlew run"