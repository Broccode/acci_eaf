#!/bin/bash

# GitLab CI Environment Debug Script
# This script helps debug GitLab CI failures by replicating GitLab CI environment locally

echo "🔍 GitLab CI Environment Debug Script"
echo "====================================="
echo ""

# Set GitLab CI-like environment variables
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.caching=true"
export NODE_OPTIONS="--max_old_space_size=4096"
export GIT_DEPTH="0"
export DOCKER_DRIVER="overlay2"
export TESTCONTAINERS_RYUK_DISABLED="true"
export TESTCONTAINERS_HOST_OVERRIDE="localhost"

echo "📋 Setting GitLab CI-like environment:"
echo "  GRADLE_OPTS: $GRADLE_OPTS"
echo "  NODE_OPTIONS: $NODE_OPTIONS"
echo "  GIT_DEPTH: $GIT_DEPTH"
echo "  DOCKER_DRIVER: $DOCKER_DRIVER"
echo "  TESTCONTAINERS_RYUK_DISABLED: $TESTCONTAINERS_RYUK_DISABLED"
echo "  TESTCONTAINERS_HOST_OVERRIDE: $TESTCONTAINERS_HOST_OVERRIDE"
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
echo "🚀 Running affected tasks with GitLab CI environment..."
echo ""

# Simulate GitLab CI's base/head detection
if [ -n "$CI_MERGE_REQUEST_DIFF_BASE_SHA" ]; then
    NX_BASE="$CI_MERGE_REQUEST_DIFF_BASE_SHA"
    echo "Using GitLab MR base: $NX_BASE"
elif [ -n "$CI_COMMIT_BEFORE_SHA" ]; then
    NX_BASE="$CI_COMMIT_BEFORE_SHA"
    echo "Using GitLab commit before: $NX_BASE"
else
    # Fallback to main branch
    NX_BASE="main"
    echo "Using fallback base: $NX_BASE"
fi

if [ -n "$CI_COMMIT_SHA" ]; then
    NX_HEAD="$CI_COMMIT_SHA"
    echo "Using GitLab commit SHA: $NX_HEAD"
else
    NX_HEAD="HEAD"
    echo "Using fallback head: $NX_HEAD"
fi

echo ""
echo "🔍 Running GitLab CI equivalent commands:"
echo ""

# Step 1: Format check (like GitLab CI)
echo "📝 Step 1: Format check..."
if npx nx format:check --base=$NX_BASE --head=$NX_HEAD; then
    echo "✅ Format check passed"
else
    echo "❌ Format check failed"
    echo "💡 Run 'npm run format:fix' to fix formatting issues"
    exit 1
fi

echo ""

# Step 2: Affected lint, test, build (like GitLab CI)
echo "🔧 Step 2: Affected lint, test, build..."
if npx nx affected --base=$NX_BASE --head=$NX_HEAD -t lint test build --skip-nx-cache; then
    echo "✅ Lint, test, build passed"
else
    echo "❌ Lint, test, build failed"
    echo "💡 Check the error messages above for specific issues"
    exit 1
fi

echo ""

# Step 3: Kotlin formatting check (like GitLab CI)
echo "🎨 Step 3: Kotlin formatting check..."
if npx nx affected --base=$NX_BASE --head=$NX_HEAD -t ktlintCheck; then
    echo "✅ Kotlin formatting check passed"
else
    echo "❌ Kotlin formatting check failed"
    echo "💡 Run 'npx nx affected -t ktlintFormat' to fix Kotlin formatting"
    exit 1
fi

echo ""
echo "🎉 All GitLab CI checks passed successfully!"
echo ""
echo "💡 Your code should now pass GitLab CI pipeline!"
