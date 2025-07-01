#!/bin/bash
echo "Simple compilation test..."
./gradlew :apps:acci-eaf-control-plane:compileKotlin > compile.log 2>&1
if [ $? -eq 0 ]; then
    echo "SUCCESS: Compilation worked!"
    rm compile.log
else
    echo "FAILED: Compilation failed - check compile.log"
    tail -10 compile.log
fi
