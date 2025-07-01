#!/bin/bash

# Script to reset Kotlin formatting consistently using ktlint via Nx
echo "🔧 Resetting Kotlin formatting with ktlint via Nx..."

# Format all Kotlin files in the project using Nx
echo "Formatting all Kotlin files..."
nx run-many --target=ktlintFormat --all

echo "✅ Formatting complete! Please review changes before committing."
echo "💡 Tip: Use 'git diff' to see what changed."
