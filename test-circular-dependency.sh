#!/bin/bash

echo "Testing circular dependency fix..."
echo "======================================="

echo "1. Checking compilation..."
./gradlew :apps:acci-eaf-control-plane:compileKotlin --quiet
if [ $? -eq 0 ]; then
    echo "✓ Main compilation successful"
else
    echo "✗ Main compilation failed"
    exit 1
fi

echo "2. Checking test compilation..."
./gradlew :apps:acci-eaf-control-plane:compileTestKotlin --quiet
if [ $? -eq 0 ]; then
    echo "✓ Test compilation successful"
else
    echo "✗ Test compilation failed"
    exit 1
fi

echo "3. Running a simple unit test..."
./gradlew :apps:acci-eaf-control-plane:test --tests="*HealthEndpointTest" --quiet
if [ $? -eq 0 ]; then
    echo "✓ Simple unit test passed"
else
    echo "✗ Simple unit test failed"
    exit 1
fi

echo "4. Running integration test (this will reveal circular dependency)..."
./gradlew :apps:acci-eaf-control-plane:test --tests="*HillaIntegrationTest" --quiet
if [ $? -eq 0 ]; then
    echo "✓ Integration test passed - circular dependency is FIXED!"
    echo ""
    echo "SUCCESS: The circular dependency issue has been resolved! 🎉"
else
    echo "✗ Integration test failed - circular dependency might still exist"
    echo ""
    echo "The circular dependency fix may need further adjustments."
fi

echo ""
echo "Done."
