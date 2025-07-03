#!/bin/bash

# Script to run Control Plane tests in batches
# Usage: ./run-tests-batch.sh [batch_number]
# If no batch number is provided, it will run all batches

BATCH=$1

echo "Control Plane Test Runner - Batch Execution"
echo "==========================================="

# Function to run a specific batch of tests
run_batch() {
    local batch_num=$1
    shift
    local test_classes=("$@")

    echo ""
    echo "Running Batch $batch_num tests (${#test_classes[@]} test classes)..."
    echo "Test classes:"
    for test in "${test_classes[@]}"; do
        echo "  - $test"
    done
    echo ""

    # Build the Gradle test filter arguments
    local gradle_args=""
    for test in "${test_classes[@]}"; do
        gradle_args="$gradle_args --tests \"$test\""
    done

    # Run tests using gradlew directly
    echo "Executing: ../../gradlew :apps:acci-eaf-control-plane:test $gradle_args"
    eval "../../gradlew :apps:acci-eaf-control-plane:test $gradle_args"
}

# Batch 1: Core Domain Models (4 tests)
BATCH_1_TESTS=(
    "com.axians.eaf.controlplane.domain.model.user.UserTest"
    "com.axians.eaf.controlplane.domain.model.role.RoleTest"
    "com.axians.eaf.controlplane.domain.model.permission.PermissionTest"
    "com.axians.eaf.controlplane.domain.model.tenant.TenantTest"
)

# Batch 2: Domain Extensions (3 tests)
BATCH_2_TESTS=(
    "com.axians.eaf.controlplane.domain.model.invitation.UserInvitationTest"
    "com.axians.eaf.controlplane.domain.service.UserServiceTest"
    "com.axians.eaf.controlplane.domain.service.TenantServiceTest"
)

# Batch 3: Architecture Tests (3 tests)
BATCH_3_TESTS=(
    "com.axians.eaf.controlplane.architecture.ArchitectureComplianceTest"
    "com.axians.eaf.controlplane.CircularDependencyResolutionTest"
    "com.axians.eaf.controlplane.SimpleCircularDependencyTest"
)

# Batch 4: Basic Endpoints (4 tests)
BATCH_4_TESTS=(
    "com.axians.eaf.controlplane.infrastructure.adapter.input.HealthEndpointTest"
    "com.axians.eaf.controlplane.infrastructure.adapter.input.TenantManagementEndpointTest"
    "com.axians.eaf.controlplane.infrastructure.adapter.input.UserInfoEndpointTest"
    "com.axians.eaf.controlplane.infrastructure.adapter.outbound.health.EafHealthIndicatorsTest"
)

# Batch 5: Security & Integration (3 tests)
BATCH_5_TESTS=(
    "com.axians.eaf.controlplane.infrastructure.security.aspect.TenantSecurityAspectTest"
    "com.axians.eaf.controlplane.HillaIntegrationTest"
    "com.axians.eaf.controlplane.application.service.UserManagementIntegrationTest"
)

# Batch 6: SDK Unit Tests (2 tests)
BATCH_6_TESTS=(
    "com.axians.eaf.controlplane.infrastructure.sdk.layer1.EafEventingClientUnitTest"
    "com.axians.eaf.controlplane.infrastructure.sdk.layer1.EafIamClientUnitTest"
)

# Batch 7: Core Integration Tests (5 tests)
BATCH_7_TESTS=(
    "com.axians.eaf.controlplane.infrastructure.categories.DatabaseIntegrationTest"
    "com.axians.eaf.controlplane.infrastructure.categories.ErrorHandlingIntegrationTest"
    "com.axians.eaf.controlplane.infrastructure.categories.EventFlowIntegrationTest"
    "com.axians.eaf.controlplane.infrastructure.categories.HealthCheckIntegrationTest"
    "com.axians.eaf.controlplane.infrastructure.categories.SecurityIntegrationTest"
)

# Batch 8: Advanced Integration Tests (5 tests)
BATCH_8_TESTS=(
    "com.axians.eaf.controlplane.infrastructure.sdk.layer2.ContextPropagationIntegrationTest"
    "com.axians.eaf.controlplane.infrastructure.sdk.layer2.EafSdkIntegrationTest"
    "com.axians.eaf.controlplane.infrastructure.sdk.layer3.ControlPlaneE2EIntegrationTest"
    "com.axians.eaf.controlplane.infrastructure.security.audit.SecurityAuditTrailIntegrationTest"
    "com.axians.eaf.controlplane.integration.security.SecurityContextIntegrationTest"
)

case $BATCH in
    1)
        run_batch 1 "${BATCH_1_TESTS[@]}"
        ;;
    2)
        run_batch 2 "${BATCH_2_TESTS[@]}"
        ;;
    3)
        run_batch 3 "${BATCH_3_TESTS[@]}"
        ;;
    4)
        run_batch 4 "${BATCH_4_TESTS[@]}"
        ;;
    5)
        run_batch 5 "${BATCH_5_TESTS[@]}"
        ;;
    6)
        run_batch 6 "${BATCH_6_TESTS[@]}"
        ;;
    7)
        run_batch 7 "${BATCH_7_TESTS[@]}"
        ;;
    8)
        run_batch 8 "${BATCH_8_TESTS[@]}"
        ;;
    "")
        echo "No batch specified. Please specify a batch number (1-8)."
        echo ""
        echo "Usage: $0 [1-8]"
        echo ""
        echo "Batch 1: Core Domain Models (${#BATCH_1_TESTS[@]} tests)"
        echo "Batch 2: Domain Extensions (${#BATCH_2_TESTS[@]} tests)"
        echo "Batch 3: Architecture Tests (${#BATCH_3_TESTS[@]} tests)"
        echo "Batch 4: Basic Endpoints (${#BATCH_4_TESTS[@]} tests)"
        echo "Batch 5: Security & Integration (${#BATCH_5_TESTS[@]} tests)"
        echo "Batch 6: SDK Unit Tests (${#BATCH_6_TESTS[@]} tests)"
        echo "Batch 7: Core Integration Tests (${#BATCH_7_TESTS[@]} tests)"
        echo "Batch 8: Advanced Integration Tests (${#BATCH_8_TESTS[@]} tests)"
        echo ""
        echo "To run all tests at once, use: ../../gradlew :apps:acci-eaf-control-plane:test"
        exit 1
        ;;
    *)
        echo "Invalid batch number: $BATCH"
        echo "Usage: $0 [1-8]"
        echo ""
        echo "Batch 1: Core Domain Models (${#BATCH_1_TESTS[@]} tests)"
        echo "Batch 2: Domain Extensions (${#BATCH_2_TESTS[@]} tests)"
        echo "Batch 3: Architecture Tests (${#BATCH_3_TESTS[@]} tests)"
        echo "Batch 4: Basic Endpoints (${#BATCH_4_TESTS[@]} tests)"
        echo "Batch 5: Security & Integration (${#BATCH_5_TESTS[@]} tests)"
        echo "Batch 6: SDK Unit Tests (${#BATCH_6_TESTS[@]} tests)"
        echo "Batch 7: Core Integration Tests (${#BATCH_7_TESTS[@]} tests)"
        echo "Batch 8: Advanced Integration Tests (${#BATCH_8_TESTS[@]} tests)"
        echo ""
        echo "To run all tests at once, use: ../../gradlew :apps:acci-eaf-control-plane:test"
        exit 1
        ;;
esac

echo ""
echo "Test execution completed!"
