---
sidebar_position: 10
title: Performance Testing Baseline
---

# EAF Event Sourcing Performance Baseline

## Overview

This document defines the performance baseline assumptions, hardware requirements, and SLA
interpretations for the EAF Event Sourcing infrastructure performance tests.

## Performance SLA Targets

### Recalibrated Targets (Based on Real Hardware)

| Operation               | Target SLA | Safety Margin | Measured Baseline | Test Conditions                       |
| ----------------------- | ---------- | ------------- | ----------------- | ------------------------------------- |
| **Token Operations**    | &lt;5ms    | 2.5x          | ~2ms average      | 100 iterations, microsecond precision |
| **Aggregate Loading**   | &lt;100ms  | 2x            | ~47ms average     | 1000-event aggregate, 10 iterations   |
| **Event Streaming**     | &lt;200ms  | 4x            | ~50ms average     | 1000-event batch, 5 iterations        |
| **Single Event Append** | &lt;100ms  | -             | Variable          | Individual event writes               |
| **Batch Event Append**  | &lt;500ms  | -             | Variable          | 100-event batches                     |

### Hardware Baseline Assumptions

**Development/CI Environment:**

- **CPU**: Modern multi-core (4+ cores, 2.4GHz+)
- **Memory**: 8GB+ RAM available to JVM
- **Storage**: SSD storage (not spinning disk)
- **Database**: PostgreSQL 15+ with default tuning
- **Network**: Local/containerized (minimal latency)
- **JVM**: OpenJDK 21 with default GC settings

**Production Scaling Factors:**

- **Network Latency**: +10-50ms for remote database
- **Concurrent Load**: 2-5x degradation under high concurrency
- **Dataset Size**: Gradual degradation with &gt;10M events
- **Hardware Constraints**: Linear scaling with CPU/memory

## Test Data Characteristics

### Event Generation Patterns

```kotlin
// Realistic payload distribution
val payloadSizes = when {
    Random.nextDouble() < 0.7 -> 100-500 bytes   // 70% small events
    Random.nextDouble() < 0.95 -> 1-5KB          // 25% medium events
    else -> 5-10KB                                // 5% large events
}

// Multi-tenant distribution
val tenantDistribution = "80/20 rule" // 80% events in 20% of tenants

// Aggregate lifecycle patterns
val aggregatePatterns = listOf(
    "User: create → activate → update → deactivate",
    "Order: create → add_items → checkout → fulfill → complete",
    "Product: create → update_details → price_change → discontinue"
)
```

### Database Configuration

**Test Database Settings:**

```sql
-- Connection pooling
max_connections = 20-50
shared_buffers = 256MB (minimum)
work_mem = 4MB

-- Write optimization
wal_buffers = 16MB
checkpoint_completion_target = 0.9
max_wal_size = 1GB

-- Query optimization
effective_cache_size = 1GB
random_page_cost = 1.1 (for SSD)
```

## Performance Test Categories

### 1. CI Smoke Tests (Always Enabled)

**Purpose**: Catch major performance regressions in CI pipeline **Dataset**: 10-25 events per test
**Runtime**: &lt;30 seconds total **SLA**: 2x more lenient than full tests

```bash
# Enable in CI
-Dperformance.smoke.tests.enabled=true
-Dci.performance.enabled=true
```

### 2. Full Performance Tests (Manual/Nightly)

**Purpose**: Comprehensive performance validation **Dataset**: 1000-10000 events per test  
**Runtime**: 5-15 minutes per test **SLA**: Production-ready targets

```bash
# Enable full tests
# Uncomment @Disabled annotations in test classes
```

### 3. Large Dataset Tests (On-Demand)

**Purpose**: Scale testing with production-like data volumes **Dataset**: 1M+ events **Runtime**:
30+ minutes **SLA**: Throughput and memory efficiency

## Performance Monitoring

### Key Metrics to Track

1. **Latency Metrics**

   - Average response time
   - 95th percentile response time
   - Maximum response time

2. **Throughput Metrics**

   - Events per second (write)
   - Queries per second (read)
   - Concurrent user capacity

3. **Resource Metrics**

   - CPU utilization
   - Memory usage (heap + off-heap)
   - Database connection pool usage
   - Disk I/O patterns

4. **Database Metrics**
   - Query execution plans
   - Index usage statistics
   - Lock contention
   - Connection pool efficiency

### Alerting Thresholds

```yaml
# Example monitoring configuration
performance_alerts:
  token_operations:
    warning: 3ms
    critical: 5ms
  aggregate_loading:
    warning: 75ms
    critical: 100ms
  event_streaming:
    warning: 150ms
    critical: 200ms
```

## Troubleshooting Performance Issues

### Common Performance Problems

1. **Optimistic Locking Conflicts**

   ```
   Symptom: EventStoreException: Concurrency conflict detected
   Cause: Duplicate aggregate IDs or sequence numbers
   Fix: Ensure unique aggregate identifiers per test
   ```

2. **Database Index Misses**

   ```
   Symptom: Query plans show Seq Scan instead of Index Scan
   Cause: Missing or ineffective indexes
   Fix: Analyze EXPLAIN ANALYZE output, optimize indexes
   ```

3. **Connection Pool Exhaustion**

   ```
   Symptom: Timeouts during concurrent tests
   Cause: Too few database connections
   Fix: Increase pool size or reduce concurrency
   ```

4. **Memory Pressure**

   ```
   Symptom: GC overhead, OutOfMemoryError
   Cause: Large result sets, memory leaks
   Fix: Implement streaming, pagination, proper cleanup
   ```

### Performance Debugging Commands

```bash
# Run performance tests with profiling
nx test eaf-eventsourcing-sdk --args="--tests '*performance*' -Dperformance.profiling.enabled=true"

# Generate JMH benchmark reports
nx test eaf-eventsourcing-sdk --args="--tests '*runJmhBenchmarks*'"

# Database query analysis
psql -d eaf_eventstore_test -c "EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM domain_events WHERE tenant_id = 'test' LIMIT 1000;"
```

## Continuous Improvement

### Performance Regression Detection

1. **Baseline Tracking**: Store performance baselines in CI artifacts
2. **Trend Analysis**: Monitor performance changes over time
3. **Automated Alerts**: Flag significant performance degradations
4. **Root Cause Analysis**: Correlate performance changes with code changes

### Hardware Scaling Guidelines

| Environment | CPU Cores | RAM   | Storage        | Expected Throughput |
| ----------- | --------- | ----- | -------------- | ------------------- |
| Development | 4 cores   | 8GB   | SSD            | 1K events/sec       |
| Testing     | 8 cores   | 16GB  | NVMe SSD       | 5K events/sec       |
| Production  | 16+ cores | 32GB+ | Enterprise SSD | 10K+ events/sec     |

## References

- [JMH Benchmarking Best Practices](https://openjdk.org/projects/code-tools/jmh/)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
