#!/bin/bash

# CI & Formatting Tools Summary
# This script shows you all the CI debugging and formatting tools available

echo "🎯 Comprehensive CI & Formatting Tools - Quick Reference"
echo "========================================================"
echo ""

echo "📝 FORMATTING COMMANDS:"
echo ""
echo "🚀 Main Commands (Use these!):"
echo "  npm run format              → Fix all formatting issues"
echo "  npm run format:check        → Check for formatting issues"
echo "  npm run format:local        → Run CI-equivalent formatting check"
echo "  npm run format:fix          → Fix all issues with detailed output"
echo ""

echo "🔧 Detailed Commands:"
echo "  npm run format:all          → Run both Nx and Prettier formatting"
echo "  npm run format:check:all    → Check both Nx and Prettier formatting"
echo "  npm run format:nx           → Format files managed by Nx"
echo "  npm run format:prettier     → Format all files with Prettier"
echo ""

echo "🚨 CI DEBUGGING COMMANDS:"
echo ""
echo "🐙 GitHub Actions:"
echo "  npm run ci:debug            → Simulate GitHub Actions environment"
echo "  npm run ci:local            → Same as above (alias)"
echo "  ./scripts/ci-debug.sh       → Direct script execution"
echo ""

echo "🦊 GitLab CI:"
echo "  npm run gitlab:debug        → Simulate GitLab CI environment"
echo "  npm run gitlab:local        → Same as above (alias)"
echo "  ./scripts/gitlab-ci-debug.sh → Direct script execution"
echo ""

echo "📚 DOCUMENTATION & GUIDES:"
echo ""
echo "  docs/FORMATTING.md           → Complete formatting guide"
echo "  docs/CI-TROUBLESHOOTING.md   → CI troubleshooting guide"
echo ""

echo "🔄 PRE-COMMIT HOOKS:"
echo ""
echo "  Husky pre-commit hook        → Automatically runs formatting checks"
echo "  npm run precommit            → Manual pre-commit check"
echo ""

echo "⚡ QUICK WORKFLOWS:"
echo ""
echo "🔍 Before pushing code:"
echo "  1. npm run format:local      # Check formatting"
echo "  2. npm run ci:debug          # Test GitHub Actions"
echo "  3. npm run gitlab:debug      # Test GitLab CI"
echo ""

echo "🚨 When CI fails:"
echo "  1. Check CI logs for specific errors"
echo "  2. Run appropriate debug script locally"
echo "  3. Fix issues and test again"
echo "  4. Push fixed code"
echo ""

echo "🛠️  Emergency fixes:"
echo "  npm run format:fix           # Fix all formatting issues"
echo "  npx nx reset                 # Clear Nx cache"
echo "  ./gradlew clean              # Clean Gradle build"
echo ""

echo "💡 TIPS:"
echo ""
echo "• Use Node.js v20 to match CI environments"
echo "• Run debug scripts before pushing major changes"
echo "• Check both GitHub Actions and GitLab CI if using both"
echo "• Pre-commit hooks prevent most CI failures"
echo ""

echo "📊 SUCCESS METRICS TO AIM FOR:"
echo ""
echo "• Build success rate > 95%"
echo "• Average build time < 10 minutes"
echo "• Cache hit rate > 80%"
echo "• Zero formatting failures"
echo ""

echo "🆘 NEED HELP?"
echo ""
echo "• Read docs/CI-TROUBLESHOOTING.md for detailed guidance"
echo "• Check CI logs for specific error patterns"
echo "• Use debug scripts to reproduce issues locally"
echo ""

echo "✅ System Status Check:"
echo ""
echo "Node.js version: $(node --version)"
echo "NPM version: $(npm --version)"
echo "Java version: $(java -version 2>&1 | head -1)"
echo "Git version: $(git --version)"
echo ""

if command -v docker &> /dev/null; then
    echo "Docker version: $(docker --version)"
else
    echo "Docker: Not available"
fi

echo ""
echo "🎉 You're all set! Happy coding! 🚀"
