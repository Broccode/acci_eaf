# Formatting Guide

This guide explains how to use the comprehensive formatting setup that ensures your local
development environment matches CI exactly.

## 🎯 Overview

The formatting system is designed to prevent CI failures by ensuring your local environment can
catch and fix all the same formatting issues that CI checks.

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

## 🔧 What Gets Formatted

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

## 🛡️ Automatic Protection

### Pre-commit Hooks

Every commit automatically runs:

1. Comprehensive formatting check (`format:check:all`)
2. Lint-staged formatting for changed files
3. ESLint fixes for staged TypeScript/JavaScript files

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

### Lint-staged Configuration (`package.json`)

```json
{
  "lint-staged": {
    "*.{js,jsx,ts,tsx,json,css,scss,md,html}": ["prettier --write"],
    "*.{yml,yaml}": ["prettier --write"],
    "*.{ts,tsx,js,jsx}": ["eslint --fix"]
  }
}
```

## 🎯 Best Practices

1. **Always run `npm run format:local` before pushing**
2. **Use VS Code for automatic formatting during development**
3. **Don't disable pre-commit hooks** - they prevent CI failures
4. **When in doubt, run `npm run format:fix`** - it fixes everything
5. **Review formatting changes** before committing to understand what was wrong

## 🚨 Common Issues & Solutions

| Issue                             | Cause                              | Solution                          |
| --------------------------------- | ---------------------------------- | --------------------------------- |
| CI formatting fails, local passes | Different scope between commands   | Use `npm run format:local`        |
| YAML quotes wrong                 | Prettier config uses single quotes | Run `npm run format:fix`          |
| Line endings differ               | macOS vs Linux differences         | VS Code enforces LF automatically |
| Git hooks not working             | Husky not installed properly       | Run `npm run prepare`             |

## 📚 Related Documentation

- [Prettier Configuration Guide](https://prettier.io/docs/en/configuration.html)
- [ESLint Integration](https://prettier.io/docs/en/integrating-with-linters.html)
- [VS Code Prettier Extension](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode)
- [Husky Git Hooks](https://typicode.github.io/husky/)

---

💡 **Pro Tip**: Add `npm run format:local` to your daily development routine to catch issues early!
