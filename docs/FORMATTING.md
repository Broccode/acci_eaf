# Formatting & Code Quality Guide

This guide explains how to use the comprehensive formatting and code quality setup that ensures your
local development environment matches CI exactly.

## 🎯 Overview

The formatting and code quality system is designed to prevent CI failures by ensuring your local
environment can catch and fix all the same formatting issues and code quality violations that CI
checks.

### Tools Used

- **Prettier**: Formats TypeScript, JavaScript, JSON, YAML, Markdown, and other frontend files
- **ESLint**: Lints TypeScript and JavaScript code
- **ktlint**: Formats and lints Kotlin code for style consistency
- **Detekt**: Static code analysis for Kotlin to detect code smells and potential bugs

## 🛠️ Available Commands

### Quick Commands

| Command                | Purpose                                        |
| ---------------------- | ---------------------------------------------- |
| `npm run format`       | Fix all formatting issues                      |
| `npm run format:check` | Check for formatting issues                    |
| `npm run format:local` | Run CI-equivalent formatting check             |
| `npm run format:fix`   | Fix all formatting issues with detailed output |

### Detailed Commands

| Command                         | Description                           |
| ------------------------------- | ------------------------------------- |
| `npm run format:all`            | Run both Nx and Prettier formatting   |
| `npm run format:check:all`      | Check both Nx and Prettier formatting |
| `npm run format:nx`             | Format files managed by Nx            |
| `npm run format:check:nx`       | Check Nx-managed files                |
| `npm run format:prettier`       | Format all files with Prettier        |
| `npm run format:check:prettier` | Check all files with Prettier         |
| `npm run format:ci`             | Run exact CI formatting check         |
| `npm run detekt`                | Run Detekt code quality analysis      |
| `npm run detekt:check`          | Run Detekt code quality analysis      |
| `npm run detekt:baseline`       | Create Detekt baseline files          |

## 🚀 Recommended Workflow

### Before Committing

1. **Run comprehensive check:**

   ```bash
   npm run format:local
   ```

2. **If issues are found, fix them:**

   ```bash
   npm run format:fix
   ```

3. **Verify everything is fixed:**

   ```bash
   npm run format:local
   ```

### Daily Development

VS Code is configured to automatically format files on save, but you can also:

```bash
# Quick format everything
npm run format

# Check if everything is properly formatted
npm run format:check
```

## 🔧 What Gets Formatted & Analyzed

### Nx Format (`npx nx format:write`)

- TypeScript/JavaScript files in the workspace
- Files managed by Nx projects

### Prettier Format (`prettier --write .`)

- **All** files in the repository including:
  - YAML files (`.yml`, `.yaml`)
  - Markdown files (`.md`)
  - JSON files (`.json`)
  - HTML files (`.html`)
  - CSS/SCSS files (`.css`, `.scss`)
  - GitHub workflow files
  - Configuration files

### Kotlin Tools

#### ktlint (`nx run-many --target=ktlintFormat --all`)

- Kotlin source files (`.kt`, `.kts`)
- Enforces Kotlin coding conventions
- Automatically fixes formatting issues

#### Detekt (`nx run-many --target=detekt --all`)

- Kotlin source files (`.kt`, `.kts`)
- Static code analysis for:
  - Code smells detection
  - Complexity analysis
  - Potential bugs
  - Performance issues
  - Style violations
  - Best practices enforcement

## 🛡️ Automatic Protection

### Pre-commit Hooks

Every commit automatically runs:

1. Comprehensive formatting check (`format:check:all`)
2. Lint-staged formatting for changed files
3. ESLint fixes for staged TypeScript/JavaScript files
4. ktlint formatting for staged Kotlin files
5. Detekt analysis for staged Kotlin files

### VS Code Integration

- **Format on Save**: Enabled for all supported file types
- **Format on Paste**: Enabled
- **Auto Fix on Save**: ESLint issues are automatically fixed
- **Line Endings**: Enforced to LF (matches CI)

## 🐛 Troubleshooting

### "Formatting check failed in CI but passes locally"

This usually happens when:

1. You're not running the comprehensive check
2. File types are not being formatted locally

**Solution:**

```bash
# Run the CI-equivalent check
npm run format:local

# If it fails, fix all issues
npm run format:fix
```

### "Pre-commit hook is failing"

The pre-commit hook runs the same checks as CI. If it fails:

```bash
# See what's wrong
npm run format:check:all

# Fix all issues
npm run format:fix

# Try committing again
git commit -m "your message"
```

### "YAML files have formatting issues"

YAML files are particularly sensitive to formatting:

```bash
# Check YAML files specifically
find . -name "*.yml" -o -name "*.yaml" | grep -v node_modules | xargs npx prettier --check

# Fix YAML files
find . -name "*.yml" -o -name "*.yaml" | grep -v node_modules | xargs npx prettier --write
```

### "VS Code not formatting properly"

1. Ensure you have the Prettier extension installed
2. Check that `.prettierrc` exists in the project root
3. Reload VS Code window: `Cmd+Shift+P` → "Developer: Reload Window"

### "Detekt analysis failing"

Detekt performs static code analysis and may find issues that need to be addressed:

```bash
# Run detekt to see all issues
npm run detekt

# Create a baseline to suppress existing issues (for legacy code)
npm run detekt:baseline

# Run detekt on specific project
nx run iam-service:detekt
```

**Known Issue - Kotlin Version Compatibility:** Currently, Detekt 1.23.8 (built with Kotlin 2.0.21)
may show compatibility warnings when used with Kotlin 2.1.20. This is a known limitation. The
analysis will still work, but you may see warnings. This will be resolved when a compatible Detekt
version is released.

**Common Detekt Issues:**

- **Complexity violations**: Reduce method/class complexity
- **Magic numbers**: Extract constants for numeric literals
- **Long parameter lists**: Consider using data classes or builder patterns
- **Unused imports/variables**: Remove unused code
- **Naming conventions**: Follow Kotlin naming standards

### "Kotlin formatting vs. code quality issues"

- **ktlint**: Handles code formatting (spacing, indentation, etc.)
- **Detekt**: Handles code quality (complexity, smells, best practices)
- Both tools complement each other and should both pass

## 📝 Configuration Files

### Prettier Configuration (`.prettierrc`)

```json
{
  "semi": true,
  "trailingComma": "es5",
  "singleQuote": true,
  "printWidth": 80,
  "tabWidth": 2,
  "useTabs": false,
  "bracketSpacing": true,
  "bracketSameLine": false,
  "arrowParens": "avoid",
  "endOfLine": "lf"
}
```

### VS Code Settings (`.vscode/settings.json`)

Key settings:

- `"editor.formatOnSave": true`
- `"files.eol": "\n"` (LF line endings)
- `"editor.defaultFormatter": "esbenp.prettier-vscode"`

### Kotlin Configuration Files

#### ktlint Configuration (`.ktlint.conf`)

- Configures ktlint formatting rules
- Sets indentation, line length, and other style preferences

#### Detekt Configuration (`detekt.yml`)

- Configures static analysis rules
- Defines thresholds for complexity, naming conventions, etc.
- Can be customized per project needs
- Supports baseline files to suppress existing issues

### Lint-staged Configuration (`package.json`)

```json
{
  "lint-staged": {
    "*.{js,jsx,ts,tsx,json,css,scss,md,html}": ["prettier --write"],
    "*.{yml,yaml}": ["prettier --write"],
    "*.{ts,tsx,js,jsx}": ["eslint --fix"],
    "**/*.kt": [
      "nx affected --target=ktlintFormat --uncommitted",
      "nx affected --target=detekt --uncommitted"
    ]
  }
}
```

## 🎯 Best Practices

1. **Always run `npm run format:local` before pushing**
2. **Use VS Code for automatic formatting during development**
3. **Don't disable pre-commit hooks** - they prevent CI failures
4. **When in doubt, run `npm run format:fix`** - it fixes everything
5. **Review formatting changes** before committing to understand what was wrong
6. **Address Detekt issues promptly** - they indicate potential code quality problems
7. **Use baseline files sparingly** - only for legacy code, not new development
8. **Run `npm run detekt` regularly** during development to catch issues early

## 🚨 Common Issues & Solutions

| Issue                             | Cause                              | Solution                          |
| --------------------------------- | ---------------------------------- | --------------------------------- |
| CI formatting fails, local passes | Different scope between commands   | Use `npm run format:local`        |
| YAML quotes wrong                 | Prettier config uses single quotes | Run `npm run format:fix`          |
| Line endings differ               | macOS vs Linux differences         | VS Code enforces LF automatically |
| Git hooks not working             | Husky not installed properly       | Run `npm run prepare`             |
| Detekt analysis fails             | Code quality issues found          | Fix issues or create baseline     |
| Kotlin formatting inconsistent    | ktlint and IDE settings differ     | Use `npm run format:kotlin`       |

## 📚 Related Documentation

- [Prettier Configuration Guide](https://prettier.io/docs/en/configuration.html)
- [ESLint Integration](https://prettier.io/docs/en/integrating-with-linters.html)
- [VS Code Prettier Extension](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode)
- [Husky Git Hooks](https://typicode.github.io/husky/)

---

💡 **Pro Tip**: Add `npm run format:local` to your daily development routine to catch issues early!
