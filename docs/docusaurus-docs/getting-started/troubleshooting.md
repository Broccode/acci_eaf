---
sidebar_position: 8
title: Troubleshooting
---

# Troubleshooting

Common issues and solutions for smooth EAF development. This guide helps you quickly resolve
problems and get back to productive development.

## 🔧 Environment Issues

### Java/JDK Problems

```bash
# Check Java version
java -version
# Should show version 17 or higher

# Check JAVA_HOME
echo $JAVA_HOME

# Fix JAVA_HOME on macOS
export JAVA_HOME=$(/usr/libexec/java_home)
```
