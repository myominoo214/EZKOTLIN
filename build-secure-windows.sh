#!/bin/bash

# EZLedger - Secure Windows Build Script
# Copyright (c) 2024 YOTTA Systems
# Builds a secure, self-contained Windows executable with anti-tampering protection

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="EZLedger"
APP_VERSION="1.0.0"
DOCKER_IMAGE="yotta-secure-windows"
BUILD_DIR="secure-dist"
FAT_JAR="build/libs/${APP_NAME}-${APP_VERSION}-all.jar"
OBFUSCATED_JAR="build/libs/${APP_NAME}-${APP_VERSION}-obfuscated.jar"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}EZLedger - Secure Windows Builder${NC}"
echo -e "${BLUE}========================================${NC}"
echo

# Function to print status messages
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker is not running"
        exit 1
    fi
    
    # Check if we're in the right directory
    if [[ ! -f "build.gradle.kts" ]]; then
        print_error "build.gradle.kts not found. Please run this script from the project root."
        exit 1
    fi
    
    # Check if ProGuard configuration exists
    if [[ ! -f "proguard-rules.pro" ]]; then
        print_error "proguard-rules.pro not found. Please ensure ProGuard configuration is present."
        exit 1
    fi
    
    # Check if obfuscation dictionary exists
    if [[ ! -f "obfuscation-dictionary.txt" ]]; then
        print_error "obfuscation-dictionary.txt not found. Please ensure obfuscation dictionary is present."
        exit 1
    fi
    
    print_status "Prerequisites check passed"
}

# Function to clean previous builds
clean_previous_builds() {
    print_status "Cleaning previous builds..."
    
    # Remove previous build artifacts
    rm -rf "$BUILD_DIR"
    rm -rf "build/libs"
    
    # Clean Docker images
    if docker images | grep -q "$DOCKER_IMAGE"; then
        print_status "Removing previous Docker image..."
        docker rmi "$DOCKER_IMAGE" 2>/dev/null || true
    fi
    
    print_status "Cleanup completed"
}

# Function to build the secure application
build_secure_app() {
    print_status "Building secure Windows application..."
    
    # Build Docker image
    print_status "Building Docker image with security features..."
    docker build -f Dockerfile.secure-windows -t "$DOCKER_IMAGE" .
    
    if [[ $? -ne 0 ]]; then
        print_error "Docker build failed"
        exit 1
    fi
    
    print_status "Docker image built successfully"
}

# Function to extract build artifacts
extract_artifacts() {
    print_status "Extracting build artifacts..."
    
    # Create build directory
    mkdir -p "$BUILD_DIR"
    
    # Extract files from Docker image
    print_status "Extracting application files..."
    docker run --rm -v "$(pwd)/$BUILD_DIR:/output" "$DOCKER_IMAGE" sh -c '
        cp -r /app.jar /output/ &&
        cp -r /jre /output/ &&
        cp -r /EZLedger.class /output/ &&
        cp -r /EZLedger.bat /output/ &&
        cp -r /EZLedger.ps1 /output/ &&
        cp -r /install.bat /output/ &&
        cp -r /README.txt /output/ &&
        cp -r /EZLedger-Secure-Windows.zip /output/ &&
        cp -r /EZLedger-Secure-Windows.tar.gz /output/
    '
    
    if [[ $? -ne 0 ]]; then
        print_error "Failed to extract artifacts from Docker image"
        exit 1
    fi
    
    print_status "Artifacts extracted successfully"
}

# Function to verify build
verify_build() {
    print_status "Verifying build artifacts..."
    
    local required_files=(
        "$BUILD_DIR/app.jar"
        "$BUILD_DIR/EZLedger.class"
        "$BUILD_DIR/EZLedger.bat"
        "$BUILD_DIR/EZLedger.ps1"
        "$BUILD_DIR/install.bat"
        "$BUILD_DIR/README.txt"
        "$BUILD_DIR/jre/bin/java.exe"
        "$BUILD_DIR/EZLedger-Secure-Windows.zip"
        "$BUILD_DIR/EZLedger-Secure-Windows.tar.gz"
    )
    
    local missing_files=()
    
    for file in "${required_files[@]}"; do
        if [[ ! -f "$file" && ! -d "$file" ]]; then
            missing_files+=("$file")
        fi
    done
    
    if [[ ${#missing_files[@]} -gt 0 ]]; then
        print_error "Missing required files:"
        for file in "${missing_files[@]}"; do
            echo "  - $file"
        done
        exit 1
    fi
    
    print_status "All required files present"
}

# Function to display build information
display_build_info() {
    echo
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}BUILD COMPLETED SUCCESSFULLY${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo
    
    # Get file sizes
    local app_size=$(du -h "$BUILD_DIR/app.jar" | cut -f1)
    local jre_size=$(du -sh "$BUILD_DIR/jre" | cut -f1)
    local total_size=$(du -sh "$BUILD_DIR" | cut -f1)
    local zip_size=$(du -h "$BUILD_DIR/EZLedger-Secure-Windows.zip" | cut -f1)
    local tar_size=$(du -h "$BUILD_DIR/EZLedger-Secure-Windows.tar.gz" | cut -f1)
    
    echo -e "${BLUE}Build Information:${NC}"
    echo "  Application Name: $APP_NAME"
    echo "  Version: $APP_VERSION"
    echo "  Build Date: $(date)"
    echo "  Build Directory: $BUILD_DIR"
    echo
    
    echo -e "${BLUE}File Sizes:${NC}"
    echo "  Obfuscated Application: $app_size"
    echo "  Embedded JRE: $jre_size"
    echo "  Total Application Size: $total_size"
    echo "  ZIP Distribution: $zip_size"
    echo "  TAR.GZ Distribution: $tar_size"
    echo
    
    echo -e "${BLUE}Security Features:${NC}"
    echo "  ✓ ProGuard code obfuscation"
    echo "  ✓ Anti-tampering protection"
    echo "  ✓ Debugger detection"
    echo "  ✓ Runtime integrity checks"
    echo "  ✓ Embedded JRE (no external Java dependency)"
    echo "  ✓ Security manager enforcement"
    echo "  ✓ Memory protection mechanisms"
    echo
    
    echo -e "${BLUE}Generated Files:${NC}"
    echo "  📁 $BUILD_DIR/"
    echo "    ├── app.jar                    (Obfuscated application)"
    echo "    ├── EZLedger.class            (Security wrapper)"
    echo "    ├── EZLedger.bat              (Windows batch launcher)"
    echo "    ├── EZLedger.ps1              (PowerShell launcher)"
    echo "    ├── install.bat               (Installation script)"
    echo "    ├── README.txt                (Documentation)"
    echo "    ├── jre/                      (Embedded Java Runtime)"
    echo "    ├── EZLedger-Secure-Windows.zip    ($zip_size)"
    echo "    └── EZLedger-Secure-Windows.tar.gz ($tar_size)"
    echo
    
    echo -e "${BLUE}Deployment Options:${NC}"
    echo "  1. ${YELLOW}Automatic Installation:${NC}"
    echo "     - Extract ZIP/TAR.GZ on target Windows machine"
    echo "     - Run install.bat as Administrator (system-wide) or User (user-only)"
    echo "     - Creates desktop and Start Menu shortcuts"
    echo "     - Registers with Windows Add/Remove Programs"
    echo
    
    echo "  2. ${YELLOW}Portable Mode:${NC}"
    echo "     - Extract files to any directory"
    echo "     - Run EZLedger.bat directly"
    echo "     - No installation required"
    echo
    
    echo "  3. ${YELLOW}PowerShell Deployment:${NC}"
    echo "     - Use EZLedger.ps1 for enhanced Windows integration"
    echo "     - Better error handling and argument processing"
    echo
    
    echo -e "${BLUE}Next Steps:${NC}"
    echo "  1. Test the application on a Windows machine"
    echo "  2. Verify security features are working"
    echo "  3. Create digital signatures for distribution"
    echo "  4. Set up secure distribution channels"
    echo "  5. Prepare user documentation and training materials"
    echo
    
    echo -e "${GREEN}Ready for secure deployment! 🚀${NC}"
    echo
}

# Main execution
main() {
    echo -e "${BLUE}Starting secure build process...${NC}"
    echo
    
    check_prerequisites
    clean_previous_builds
    build_secure_app
    extract_artifacts
    verify_build
    display_build_info
    
    echo -e "${GREEN}Secure Windows build completed successfully!${NC}"
}

# Handle script interruption
trap 'echo -e "\n${RED}Build interrupted!${NC}"; exit 1' INT TERM

# Run main function
main "$@"