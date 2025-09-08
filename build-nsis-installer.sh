#!/bin/bash

# YOTTA Ledger NSIS Installer Build Script
# This script builds a Windows installer (.exe) using NSIS

set -e

echo "[INFO] EZLedger NSIS Installer Build Script"
echo "[INFO] =========================================="

# Configuration
PROJECT_NAME="EZLedger"
VERSION="1.0.0"
OUTPUT_DIR="installer-dist"
NSI_FILE="installer.nsi"
INSTALLER_FILE="EZLedger-Setup-1.0.0.exe"

# Check if secure distribution exists
if [ ! -d "secure-dist" ]; then
    echo "[ERROR] Secure distribution not found. Please run build-secure-windows.sh first."
    exit 1
fi

if [ ! -f "secure-dist/app.jar" ]; then
    echo "[ERROR] Application JAR not found in secure-dist directory."
    exit 1
fi

echo "[INFO] Checking NSIS availability..."

# Check if NSIS is available
if command -v makensis >/dev/null 2>&1; then
    echo "[INFO] NSIS found (native installation)"
    MAKENSIS_CMD="makensis"
elif command -v wine >/dev/null 2>&1; then
    echo "[INFO] Wine found, checking for NSIS in Wine environment"
    if wine makensis.exe /VERSION >/dev/null 2>&1; then
        echo "[INFO] NSIS found in Wine environment"
        MAKENSIS_CMD="wine makensis.exe"
    else
        echo "[WARNING] NSIS not found in Wine environment"
        echo "[INFO] Installing NSIS via Docker..."
        MAKENSIS_CMD="docker"
    fi
else
    echo "[INFO] Using Docker-based NSIS build..."
    MAKENSIS_CMD="docker"
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

if [ "$MAKENSIS_CMD" = "docker" ]; then
    echo "[INFO] Building installer using Docker with NSIS..."
    
    # Create Dockerfile for NSIS building
    cat > Dockerfile.nsis-builder << 'EOF'
FROM ubuntu:20.04

# Install NSIS and dependencies
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
    nsis \
    nsis-pluginapi \
    nsis-doc && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /build

# Copy source files
COPY . .

# Build installer
RUN makensis installer.nsi

EOF

    echo "[INFO] Building Docker image for NSIS compilation..."
    docker build -f Dockerfile.nsis-builder -t yotta-nsis-builder .
    
    # Extract installer from container
    echo "[INFO] Extracting installer..."
    docker create --name temp-nsis yotta-nsis-builder
    docker cp temp-nsis:/build/EZLedger-Setup-1.0.0.exe "$OUTPUT_DIR/"
    docker rm temp-nsis
    
    # Clean up
    rm -f Dockerfile.nsis-builder
    
else
    echo "[INFO] Building installer with NSIS..."
    $MAKENSIS_CMD "$NSI_FILE"
    
    # Move the installer to output directory
    if [ -f "$INSTALLER_FILE" ]; then
        mv "$INSTALLER_FILE" "$OUTPUT_DIR/"
    else
        echo "[ERROR] Installer file not found after build"
        exit 1
    fi
fi

if [ -f "$OUTPUT_DIR/$INSTALLER_FILE" ]; then
    echo "[INFO] NSIS installer created successfully: $OUTPUT_DIR/$INSTALLER_FILE"
    
    # Display installer information
    echo ""
    echo "[INFO] Installer Information:"
    echo "       File: $OUTPUT_DIR/$INSTALLER_FILE"
    echo "       Size: $(du -h "$OUTPUT_DIR/$INSTALLER_FILE" | cut -f1)"
    echo "       Product: $PROJECT_NAME"
    echo "       Version: $VERSION"
    echo ""
    echo "[INFO] The installer can be distributed to Windows users."
    echo "[INFO] Users can install by running the .exe file."
    echo "[INFO] Supports silent installation with /S parameter."
else
    echo "[ERROR] Failed to create installer"
    exit 1
fi

echo "[INFO] Build completed successfully!"