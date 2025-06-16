#!/bin/bash

# CI Environment Debug Script
# This script helps debug CI failures by replicating CI environment locally

echo "🔍 CI Environment Debug Script"
echo "================================"
echo ""

# Set CI-like environment variables
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.caching=true"
export NODE_OPTIONS="--max_old_space_size=4096"

echo "📋 Setting CI-like environment:"
echo "  GRADLE_OPTS: $GRADLE_OPTS"
echo "  NODE_OPTIONS: $NODE_OPTIONS"
echo ""

# Stop any existing Gradle daemons
echo "🛑 Stopping existing Gradle daemons..."
./gradlew --stop

# Clean build directories
echo "🧹 Cleaning build directories..."
find . -name "build" -type d -path "*/kotlin/compile*" -exec rm -rf {} + 2>/dev/null || true
find . -name "caches-jvm" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name "*.kotlin_module" -delete 2>/dev/null || true

# Clear Nx cache
echo "🗑️  Clearing Nx cache..."
npx nx reset

echo ""
echo "🚀 Running affected tasks with CI environment..."
echo ""

# Use the same base/head as CI if provided, otherwise use defaults
if [ -n "$NX_BASE" ] && [ -n "$NX_HEAD" ]; then
    echo "Using provided NX_BASE: $NX_BASE"
    echo "Using provided NX_HEAD: $NX_HEAD"
    npx nx affected -t lint test build --skip-nx-cache
else
    echo "Using CI-like base/head detection..."
    NX_BASE=294d2f673eeae8be1cc94bd41cdfd467396c26af \
    NX_HEAD=799d6320c6344b8d6c490499f59961d6868b0fce \
    npx nx affected -t lint test build --skip-nx-cache
fi
