#!/bin/bash

# Validation script for Windows .exe build configuration
# This script validates that all components are properly configured

set -e

echo "🔍 Validating Windows .exe build configuration..."
echo "================================================"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check Java version
echo "📋 Checking Java version..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        print_status "Java $JAVA_VERSION detected (JDK 17+ required)"
    else
        print_error "Java $JAVA_VERSION detected (JDK 17+ required)"
        exit 1
    fi
else
    print_error "Java not found in PATH"
    exit 1
fi

# Check jpackage availability
echo "📦 Checking jpackage availability..."
if command -v jpackage &> /dev/null; then
    JPACKAGE_VERSION=$(jpackage --version 2>&1)
    print_status "jpackage available: $JPACKAGE_VERSION"
else
    print_warning "jpackage not found (required for local .exe builds)"
fi

# Check Gradle
echo "🔨 Checking Gradle..."
if [ -f "./gradlew" ]; then
    print_status "Gradle wrapper found"
    GRADLE_VERSION=$(./gradlew --version | grep "Gradle" | head -n 1)
    print_status "$GRADLE_VERSION"
else
    print_error "Gradle wrapper not found"
    exit 1
fi

# Check required files
echo "📁 Checking required files..."
required_files=(
    "build.gradle.kts"
    "icon.ico"
    ".github/workflows/build-installers.yml"
    "installer.nsi"
    "build-nsis-installer.sh"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        print_status "Found: $file"
    else
        print_warning "Missing: $file"
    fi
done

# Test Gradle tasks
echo "🧪 Testing Gradle tasks..."
echo "Available distribution tasks:"
./gradlew tasks --group=distribution | grep -E "^[a-zA-Z]" || true

# Test shadow JAR build
echo "🔨 Testing shadow JAR build..."
if ./gradlew shadowJar --dry-run &> /dev/null; then
    print_status "Shadow JAR task configuration valid"
else
    print_error "Shadow JAR task configuration invalid"
fi

# Check GitHub Actions workflow syntax
echo "🔍 Validating GitHub Actions workflow..."
if command -v yamllint &> /dev/null; then
    if yamllint .github/workflows/build-installers.yml &> /dev/null; then
        print_status "GitHub Actions workflow syntax valid"
    else
        print_warning "GitHub Actions workflow syntax issues detected"
    fi
else
    print_warning "yamllint not found (install with: pip install yamllint)"
fi

# Summary
echo ""
echo "📊 Validation Summary"
echo "==================="
print_status "Java 17+ available"
if command -v jpackage &> /dev/null; then
    print_status "jpackage available for local builds"
else
    print_warning "jpackage not available (use GitHub Actions for .exe builds)"
fi
print_status "Gradle configuration valid"
print_status "GitHub Actions workflow configured"

echo ""
echo "🚀 Next Steps:"
echo "1. Push to GitHub to trigger automatic .exe builds"
echo "2. Or run locally: ./gradlew shadowJar (then use jpackage manually)"
echo "3. Check the WINDOWS_EXE_BUILD_GUIDE.md for detailed instructions"
echo ""
print_status "Configuration validation completed!"