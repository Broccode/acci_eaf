# Control Plane Test Batching Guide

This guide explains how to run Control Plane tests in batches instead of running all 29 tests at
once.

## Overview

The Control Plane project contains 29 test classes organized into 3 batches:

- **Batch 1**: Domain and Architecture Tests (10 tests)
- **Batch 2**: Infrastructure and Endpoint Tests (9 tests)
- **Batch 3**: Integration and E2E Tests (10 tests)

## Using the Test Batch Script

### Basic Usage

```bash
# From the workspace root
./apps/acci-eaf-control-plane/run-tests-batch.sh [batch_number]
```

### Examples

```bash
# Run only domain and architecture tests (Batch 1)
./apps/acci-eaf-control-plane/run-tests-batch.sh 1

# Run only infrastructure and endpoint tests (Batch 2)
./apps/acci-eaf-control-plane/run-tests-batch.sh 2

# Run only integration and E2E tests (Batch 3)
./apps/acci-eaf-control-plane/run-tests-batch.sh 3
```

## Batch Contents

### Batch 1: Domain and Architecture Tests

- Domain model tests (User, Role, Permission, Tenant, UserInvitation)
- Domain service tests (UserService, TenantService)
- Architecture compliance tests
- Circular dependency resolution tests

### Batch 2: Infrastructure and Endpoint Tests

- REST endpoint tests
- Health indicator tests
- Security aspect tests
- SDK unit tests
- Hilla integration test

### Batch 3: Integration and E2E Tests

- Database integration tests
- Error handling tests
- Event flow tests
- Security integration tests
- Context propagation tests
- Full E2E tests

## Alternative: Running Specific Tests Manually

You can also run specific tests using Nx with Gradle test filters:

```bash
# Run a single test class
nx test acci-eaf-control-plane --args="--tests 'com.axians.eaf.controlplane.domain.model.user.UserTest'"

# Run multiple specific test classes
nx test acci-eaf-control-plane --args="--tests 'com.axians.eaf.controlplane.domain.model.user.UserTest' --tests 'com.axians.eaf.controlplane.domain.service.UserServiceTest'"
```

## Running All Tests

To run all tests at once (not in batches):

```bash
nx test acci-eaf-control-plane
```

## Tips

1. Run domain tests first (Batch 1) as they are typically faster and don't require external
   dependencies
2. Integration tests (Batch 3) may take longer due to database setup and container initialization
3. If a batch fails, you can run individual tests from that batch to isolate issues
4. The script shows which test classes are included in each batch before running them
