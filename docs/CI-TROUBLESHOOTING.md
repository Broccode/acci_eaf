# CI Troubleshooting Guide

This guide helps you diagnose and fix CI failures, especially build and test issues that work
locally but fail in CI.

## 🚨 **Common CI Failure Patterns**

### **1. "Works Locally, Fails in CI"**

This is the most common issue. Here's why it happens and how to fix it:

#### **Root Causes:**

| **Difference**       | **Local**         | **CI**                                 | **Impact**                               |
| -------------------- | ----------------- | -------------------------------------- | ---------------------------------------- |
| **Operating System** | macOS/Windows     | Ubuntu Linux                           | File path differences, case sensitivity  |
| **Node.js Version**  | Latest (v24+)     | Fixed (v20)                            | API differences, package compatibility   |
| **Gradle Daemon**    | Enabled           | Disabled (`-Dorg.gradle.daemon=false`) | Memory management, compilation stability |
| **Cache State**      | Warm, incremental | Cold, fresh                            | Missing dependencies, cache corruption   |
| **Memory Limits**    | System default    | Constrained (4GB)                      | Out of memory errors                     |
| **Testcontainers**   | Native Docker     | Docker-in-Docker                       | Network/container issues                 |

#### **Solutions:**

1. **Replicate CI Environment Locally:**

   ```bash
   ./scripts/ci-debug.sh
   ```

2. **Check Environment-Specific Issues:**

   ```bash
   # Test with CI-like Node.js version
   nvm use 20
   npm ci
   npx nx affected -t lint test build
   ```

3. **Test with Gradle Daemon Disabled:**
   ```bash
   export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.caching=true"
   npx nx affected -t build
   ```

### **2. Kotlin Compilation Failures**

#### **Common Error Patterns:**

- `OutOfMemoryError` during compilation
- `KotlinCompilerException`
- `CompilationException`
- Gradle daemon crashes

#### **Solutions:**

1. **Memory Optimization:**

   ```bash
   # Increase memory allocation
   export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs='-Xmx4g -XX:MaxMetaspaceSize=1g'"
   ```

2. **Clean Compilation State:**

   ```bash
   # Clean all Kotlin compilation artifacts
   find . -name "build" -type d -path "*/kotlin/compile*" -exec rm -rf {} + 2>/dev/null || true
   find . -name "caches-jvm" -type d -exec rm -rf {} + 2>/dev/null || true
   ./gradlew clean
   ```

3. **Reduce Parallelism:**
   ```bash
   # Build with single thread for stability
   npx nx affected -t build --parallel=1
   ```

### **3. Test Failures**

#### **Common Causes:**

- Testcontainers networking issues
- Database connection problems
- Timing-sensitive tests
- Resource cleanup issues

#### **Solutions:**

1. **Testcontainers Configuration:**

   ```bash
   # Set CI-compatible Testcontainers environment
   export DOCKER_HOST=tcp://localhost:2375
   export TESTCONTAINERS_RYUK_DISABLED=true
   export TESTCONTAINERS_HOST_OVERRIDE=localhost
   ```

2. **Test Isolation:**

   ```bash
   # Run tests with better isolation
   npx nx affected -t test --parallel=1
   ```

3. **Debug Specific Test:**
   ```bash
   # Run specific failing test project
   npx nx run test-service:test --verbose
   ```

## 🛠️ **Diagnostic Tools**

### **1. Local CI Simulation**

```bash
# Simulate exact CI environment
./scripts/ci-debug.sh
```

### **2. Manual Diagnostics**

```bash
# Check system resources
free -h
df -h

# Check Java/Node versions
java -version
node --version

# Check Gradle status
./gradlew --version
./gradlew --status

# Check Docker connectivity
docker version
docker ps
```

### **3. Nx Cloud Analysis**

- Visit the Nx Cloud links provided in CI failure logs
- Check detailed task execution logs
- Compare successful vs failed runs

## 🔧 **CI Configuration Improvements**

### **1. Enhanced Error Handling**

Your CI already includes:

- Automatic retry on failure
- Comprehensive cache cleanup
- Detailed diagnostic logging
- Artifact upload for debugging

### **2. Memory Optimization**

```yaml
env:
  GRADLE_OPTS:
    -Dorg.gradle.daemon=false -Dorg.gradle.caching=true -Dorg.gradle.jvmargs="-Xmx4g
    -XX:MaxMetaspaceSize=1g"
  NODE_OPTIONS: --max_old_space_size=4096
```

### **3. Parallel Execution Tuning**

```bash
# Conservative parallelism for stability
npx nx affected -t build --parallel=2
npx nx affected -t test --parallel=1
```

## 📊 **Monitoring & Alerts**

### **Failure Patterns to Watch:**

1. **Frequent Kotlin compilation failures** → Memory issues
2. **Intermittent test failures** → Timing/networking issues
3. **Cache-related build failures** → Dependency issues
4. **Container startup failures** → Docker/Testcontainers issues

### **Success Metrics:**

- Build success rate > 95%
- Average build time < 10 minutes
- Cache hit rate > 80%

## 🚀 **Best Practices**

### **1. Local Development**

1. **Test with CI environment before pushing:**

   ```bash
   npm run format:local  # Check formatting
   ./scripts/ci-debug.sh # Test build with CI settings
   ```

2. **Use consistent Node.js version:**

   ```bash
   nvm use 20  # Match CI Node.js version
   ```

3. **Clean build state regularly:**
   ```bash
   npx nx reset
   ./gradlew clean
   ```

### **2. CI Optimization**

1. **Use enhanced CI workflow** (`.github/workflows/ci-improved.yml`)
2. **Monitor build times and cache effectiveness**
3. **Tune parallelism based on failure patterns**

### **3. Debugging Workflow**

1. **Check CI logs** for specific error messages
2. **Download diagnostic artifacts** for detailed analysis
3. **Reproduce locally** using CI debug script
4. **Incremental fixes** - test one change at a time

## 🆘 **Emergency Procedures**

### **When CI is Completely Broken:**

1. **Disable parallelism temporarily:**

   ```yaml
   run: npx nx affected -t build --parallel=1 --skip-nx-cache
   ```

2. **Use fallback build strategy:**

   ```bash
   # Build each project individually
   npx nx run eaf-core:build
   npx nx run iam-service:build
   # ... etc
   ```

3. **Emergency cache clear:**
   ```yaml
   - name: Nuclear cache clear
     run: |
       rm -rf ~/.gradle/caches
       rm -rf ~/.gradle/daemon
       npx nx reset
   ```

## 📚 **Additional Resources**

- [Nx Troubleshooting Guide](https://nx.dev/troubleshooting/troubleshoot-nx-install-issues)
- [Gradle Performance Guide](https://docs.gradle.org/current/userguide/performance.html)
- [Testcontainers CI Guide](https://www.testcontainers.org/ci/)
- [GitHub Actions Best Practices](https://docs.github.com/en/actions/learn-github-actions/usage-limits-billing-and-administration)

---

💡 **Pro Tip**: Most CI failures are environment-related. When in doubt, run `./scripts/ci-debug.sh`
to simulate CI conditions locally!
