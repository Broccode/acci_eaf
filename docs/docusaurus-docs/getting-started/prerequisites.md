---
sidebar_position: 2
title: Prerequisites
---

# Prerequisites

Before diving into ACCI EAF development, ensure your system meets these requirements and has the
necessary tools installed.

## 📋 System Requirements

### Operating System

- **macOS** 12.0 or later
- **Windows** 10/11 with WSL2
- **Linux** Ubuntu 20.04+ or equivalent

### Hardware

- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space for tools and projects
- **CPU**: Any modern processor (Intel or Apple Silicon supported)

## ☕ Java Development Kit (JDK)

ACCI EAF requires **JDK 17 or higher** for Kotlin/Spring development.

### Installation Options

#### Option 1: SDKMAN (Recommended for macOS/Linux)

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install latest JDK 17
sdk install java 17.0.9-tem
sdk use java 17.0.9-tem

# Verify installation
java -version
```

#### Option 2: Direct Download

- **Oracle JDK**: [Download from Oracle](https://www.oracle.com/java/technologies/downloads/)
- **OpenJDK**: [Download from OpenJDK](https://jdk.java.net/17/)
- **Eclipse Temurin**: [Download from Adoptium](https://adoptium.net/)

### Verification

```bash
# Check Java version
java -version
# Should show: openjdk version "17.0.x" or similar

# Check JAVA_HOME
echo $JAVA_HOME
# Should point to your JDK installation
```

## 🎯 Kotlin

Kotlin is automatically managed by Gradle in EAF projects, but you may want the command-line
compiler for experimentation.

### Installation

```bash
# Using SDKMAN
sdk install kotlin

# Using Homebrew (macOS)
brew install kotlin

# Verify installation
kotlin -version
```

## 🟢 Node.js & npm

Required for frontend development and tooling.

### Installation

#### Option 1: Node Version Manager (Recommended)

```bash
# Install nvm (macOS/Linux)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# Restart terminal or source profile
source ~/.bashrc

# Install and use Node.js 18+
nvm install 18
nvm use 18

# Set as default
nvm alias default 18
```

#### Option 2: Direct Download

- Download from [nodejs.org](https://nodejs.org/) (LTS version recommended)

### Verification

```bash
# Check Node.js version
node --version
# Should show: v18.x.x or higher

# Check npm version
npm --version
# Should show: 9.x.x or higher
```

## 🐳 Docker & Docker Compose

Essential for running local infrastructure (NATS, PostgreSQL).

### Installation

#### macOS

```bash
# Download Docker Desktop
# https://www.docker.com/products/docker-desktop

# Or using Homebrew
brew install --cask docker
```

#### Windows

- Download Docker Desktop from [docker.com](https://www.docker.com/products/docker-desktop)
- Ensure WSL2 backend is enabled

#### Linux (Ubuntu)

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt-get update
sudo apt-get install docker-compose-plugin
```

### Verification

```bash
# Check Docker version
docker --version
# Should show: Docker version 24.x.x or higher

# Check Docker Compose version
docker compose version
# Should show: Docker Compose version v2.x.x or higher

# Test Docker installation
docker run hello-world
```

## ⚡ Nx CLI

Global installation for monorepo management.

### Installation

```bash
# Install globally
npm install -g nx

# Verify installation
nx --version
# Should show: 18.x.x or higher
```

## 🛠️ Recommended IDEs

### Backend Development (Kotlin/Spring)

#### IntelliJ IDEA (Strongly Recommended)

- **Community Edition**: Free, excellent Kotlin support
- **Ultimate Edition**: Enhanced Spring Boot support, database tools

**Download**: [jetbrains.com/idea](https://www.jetbrains.com/idea/)

**Essential Plugins**:

- Spring Boot (Ultimate only)
- Kotlin (bundled)
- Docker
- Database Tools (Ultimate only)

### Frontend Development (React/TypeScript)

#### Visual Studio Code (Recommended)

- Lightweight, excellent TypeScript support
- Rich extension ecosystem

**Download**: [code.visualstudio.com](https://code.visualstudio.com/)

**Essential Extensions**:

- ES7+ React/Redux/React-Native snippets
- TypeScript Hero
- Prettier - Code formatter
- ESLint
- Auto Rename Tag
- Bracket Pair Colorizer
- GitLens

## 🔧 Git Configuration

### Installation

Git is typically pre-installed on macOS and Linux. For Windows, download from
[git-scm.com](https://git-scm.com/).

### Configuration

```bash
# Set your identity
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Set default branch name
git config --global init.defaultBranch main

# Improve command line experience
git config --global color.ui auto
git config --global core.autocrlf input  # macOS/Linux
git config --global core.autocrlf true   # Windows
```

### SSH Keys (Recommended)

```bash
# Generate SSH key
ssh-keygen -t ed25519 -C "your.email@example.com"

# Start SSH agent
eval "$(ssh-agent -s)"

# Add key to agent
ssh-add ~/.ssh/id_ed25519

# Copy public key to clipboard
# macOS
pbcopy < ~/.ssh/id_ed25519.pub

# Linux
xclip -selection clipboard < ~/.ssh/id_ed25519.pub

# Windows
clip < ~/.ssh/id_ed25519.pub
```

Add the public key to your Git provider (GitHub, GitLab, etc.).

## 🧪 Verification Checklist

Run these commands to verify your setup:

```bash
# Java
echo "Java version:"
java -version

# Node.js
echo "Node.js version:"
node --version

# npm
echo "npm version:"
npm --version

# Docker
echo "Docker version:"
docker --version

# Docker Compose
echo "Docker Compose version:"
docker compose version

# Nx
echo "Nx version:"
nx --version

# Git
echo "Git version:"
git --version

# Kotlin (optional)
echo "Kotlin version:"
kotlin -version 2>/dev/null || echo "Kotlin not installed (optional)"
```

## 🎯 IDE Setup Verification

### IntelliJ IDEA

1. Create a new Kotlin project
2. Verify syntax highlighting works
3. Check that Spring Boot plugin is available (Ultimate edition)

### VS Code

1. Create a new TypeScript file
2. Verify syntax highlighting and IntelliSense work
3. Install recommended extensions from the list above

## 🚀 Next Steps

Once all prerequisites are installed and verified:

1. **Restart your terminal** to ensure all environment variables are loaded
2. **Test your setup** with the verification checklist above
3. **Proceed to [Local Setup](./local-setup.md)** to clone the repository and start development

## 🔧 Troubleshooting

### Common Issues

#### Java Issues

- **JAVA_HOME not set**: Add `export JAVA_HOME=$(/usr/libexec/java_home)` to your shell profile
- **Wrong Java version**: Use SDKMAN to switch versions: `sdk use java 17.0.9-tem`

#### Node.js Issues

- **Permission errors**: Use nvm instead of system Node.js installation
- **Old npm version**: Run `npm install -g npm@latest`

#### Docker Issues

- **Permission denied**: Add user to docker group: `sudo usermod -aG docker $USER`
- **Docker Desktop not starting**: Check system resources and restart Docker Desktop

#### Git Issues

- **SSH connection problems**: Test with `ssh -T git@github.com`
- **Credential issues**: Consider using SSH instead of HTTPS

### Getting Help

If you encounter issues:

1. Check the [Troubleshooting](./troubleshooting.md) guide
2. Search existing documentation
3. Reach out to the EAF team in our development channels

---

**Ready?** Continue to [Local Setup](./local-setup.md) to get your development environment running!
🚀
