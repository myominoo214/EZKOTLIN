#!/bin/bash

# Hot reload development script for Kotlin Compose Desktop
# This script uses Gradle's continuous build feature to automatically
# recompile and restart the application when source files change

echo "Starting Kotlin Compose Desktop with Hot Reload..."
echo "The application will automatically restart when you make changes to source files."
echo "Press Ctrl+C to stop."
echo ""

# Use Gradle's continuous build feature
# The -t flag enables continuous build mode
# The --no-daemon flag ensures fresh builds
./gradlew run -t --no-daemon