#!/bin/bash

# Local formatting script that replicates CI behavior exactly
# This script runs the same formatting checks as the CI pipeline

set -e  # Exit on any error

echo "🚀 Running comprehensive formatting checks (CI-equivalent)..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}📋 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
    print_error "Error: package.json not found. Please run this script from the project root."
    exit 1
fi

# 1. Run Nx format check (same as CI)
print_status "Step 1: Running Nx format check..."
if npx nx format:check; then
    print_success "Nx format check passed"
else
    print_error "Nx format check failed"
    print_warning "Run 'npx nx format:write' to fix Nx-managed files"
    exit 1
fi

echo ""

# 2. Run Prettier check on all files (comprehensive)
print_status "Step 2: Running comprehensive Prettier check..."
if npx prettier --check .; then
    print_success "Prettier format check passed"
else
    print_error "Prettier format check failed"
    print_warning "Run 'npx prettier --write .' to fix formatting issues"
    exit 1
fi

echo ""

# 3. Check for specific file types that commonly cause CI issues
print_status "Step 3: Checking specific problematic file patterns..."

# Check YAML files specifically
if find . -name "*.yml" -o -name "*.yaml" | grep -v node_modules | xargs npx prettier --check; then
    print_success "YAML files formatting check passed"
else
    print_error "YAML files formatting check failed"
    exit 1
fi

# Check markdown files
if find . -name "*.md" | grep -v node_modules | xargs npx prettier --check; then
    print_success "Markdown files formatting check passed"
else
    print_error "Markdown files formatting check failed"
    exit 1
fi

# Check GitHub workflow files
if [ -d ".github" ]; then
    if find .github -name "*.yml" -o -name "*.yaml" | xargs npx prettier --check; then
        print_success "GitHub workflow files formatting check passed"
    else
        print_error "GitHub workflow files formatting check failed"
        exit 1
    fi
fi

echo ""
print_success "🎉 All formatting checks passed! Your code is ready for CI."
echo ""
print_status "Summary:"
echo "  ✅ Nx format check: OK"
echo "  ✅ Prettier comprehensive check: OK"
echo "  ✅ YAML files: OK"
echo "  ✅ Markdown files: OK"
echo "  ✅ GitHub workflows: OK"
echo ""
print_status "Your local formatting now matches CI requirements exactly! 🚀"
