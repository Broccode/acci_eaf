#!/bin/bash

# Local formatting fix script that applies the same formatting as CI expects
# This script fixes all formatting issues that would cause CI to fail

set -e  # Exit on any error

echo "🔧 Fixing all formatting issues (CI-equivalent)..."
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}📋 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
    echo "❌ Error: package.json not found. Please run this script from the project root."
    exit 1
fi

# 1. Run Nx format write
print_status "Step 1: Running Nx format write..."
npx nx format:write
print_success "Nx format write completed"

echo ""

# 2. Run Prettier write on all files
print_status "Step 2: Running comprehensive Prettier format..."
npx prettier --write .
print_success "Prettier format completed"

echo ""

# 3. Fix specific file types that commonly cause CI issues
print_status "Step 3: Fixing specific problematic file patterns..."

# Fix YAML files specifically
print_status "Fixing YAML files..."
find . -name "*.yml" -o -name "*.yaml" | grep -v node_modules | xargs npx prettier --write

# Fix markdown files
print_status "Fixing Markdown files..."
find . -name "*.md" | grep -v node_modules | xargs npx prettier --write

# Fix GitHub workflow files
if [ -d ".github" ]; then
    print_status "Fixing GitHub workflow files..."
    find .github -name "*.yml" -o -name "*.yaml" | xargs npx prettier --write
fi

# Fix JSON files
print_status "Fixing JSON files..."
find . -name "*.json" | grep -v node_modules | xargs npx prettier --write

print_success "All specific file types formatted"

echo ""

# 4. Run the check script to verify everything is fixed
print_status "Step 4: Verifying fixes with comprehensive check..."
if [ -f "scripts/format-local.sh" ]; then
    chmod +x scripts/format-local.sh
    ./scripts/format-local.sh
else
    # Fallback verification
    print_status "Running fallback verification..."
    npx nx format:check && npx prettier --check .
    print_success "Verification completed"
fi

echo ""
print_success "🎉 All formatting issues have been fixed!"
print_success "Your code is now ready for CI and should pass all formatting checks."
echo ""
print_status "Files that were formatted:"
echo "  • TypeScript/JavaScript files (via Nx)"
echo "  • All files (via Prettier)"
echo "  • YAML files (.github/, infra/, etc.)"
echo "  • Markdown files"
echo "  • JSON configuration files"
echo ""
print_status "Next steps:"
echo "  1. Review the changes with: git diff"
echo "  2. Commit the formatting fixes: git add . && git commit -m 'fix: Apply comprehensive formatting'"
echo "  3. Push to trigger CI: git push"
