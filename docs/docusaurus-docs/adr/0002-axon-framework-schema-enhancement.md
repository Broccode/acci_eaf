---
sidebar_position: 2
title: ADR-0002 - Axon Framework Schema Enhancement
---

# ADR-0002: Axon Framework Schema Enhancement

## Status

Accepted

## Context

The EAF (Enterprise Application Framework) currently uses a custom event sourcing implementation
with a PostgreSQL-based event store. As part of Epic 4.1, we need to integrate Axon Framework 4.11.2
to leverage its mature event sourcing capabilities, command/query separation, and saga management.

The existing EAF Event Store schema was designed for our custom implementation and needs
enhancements to support Axon Framework's requirements while maintaining backward compatibility with
existing EAF SDK operations.

### Current Schema Limitations

1. **Missing Axon-specific metadata**: Axon Framework expects certain metadata fields for optimal
   performance
2. **Suboptimal indexing**: Current indexes are not optimized for Axon's query patterns
3. **Event type handling**: Axon distinguishes between event types and payload types for better
   serialization
4. **Version management**: No explicit payload versioning for event schema evolution

### Axon Framework Requirements

Axon Framework 4.11.2 expects the following from an event store:

- **Payload Type**: Separate field for event payload type (for serialization)
- **Payload Revision**: Version information for event schema evolution
- **Optimized Indexes**: Specific index patterns for TrackingEventProcessor and aggregate loading
- **Multi-tenant Support**: Efficient tenant isolation in all queries

## Decision

We will enhance the existing EAF Event Store schema to support Axon Framework 4.11.2 while
maintaining full backward compatibility with existing EAF SDK operations.

### Schema Enhancements

1. **Add New Columns**:

   - `payload_type` VARCHAR(255): Event payload type for Axon serialization
   - `payload_revision` VARCHAR(10): Event schema version

2. **Create Performance Indexes**:

   - `idx_events_tenant_sequence`: Optimized for TrackingEventProcessor
   - `idx_events_aggregate_enhanced`: Fast aggregate loading
   - `idx_events_tracking`: Event processor resume scenarios
   - `idx_events_payload_type`: Event type filtering

3. **Enhance Existing Indexes**:

   - Add tenant isolation to all indexes
   - Optimize column ordering for multi-tenant queries
   - Add partial indexes for high-frequency scenarios

4. **Add Data Integrity Constraints**:
   - Ensure payload_type is not empty
   - Validate payload_revision format (semantic versioning)

### Migration Strategy

1. **Backward Compatibility First**: New columns are initially nullable
2. **Data Population**: Populate existing events with appropriate defaults
3. **Constraint Addition**: Add constraints after data population
4. **Index Optimization**: Create new indexes and enhance existing ones
5. **Statistics Update**: Refresh query planner statistics

## Consequences

### Positive

1. **Axon Framework Compatibility**: Full support for Axon Framework 4.11.2 features
2. **Performance Improvement**: Optimized indexes for both EAF SDK and Axon queries
3. **Backward Compatibility**: Existing EAF SDK operations continue unchanged
4. **Future-Proofing**: Schema supports event evolution and versioning
5. **Multi-tenant Optimization**: All indexes optimized for tenant isolation

### Negative

1. **Storage Overhead**: ~15-25% increase in index storage
2. **Migration Complexity**: Requires careful migration planning and testing
3. **Maintenance Overhead**: Additional indexes require monitoring and maintenance

### Neutral

1. **Development Impact**: Minimal changes required to existing applications
2. **Performance Trade-offs**: Slight write performance impact offset by read improvements

## Implementation Details

### Migration File Location

```
libs/eaf-eventsourcing-sdk/src/main/resources/db/migration/V3__enhance_eventstore_for_axon.sql
```

### Key Design Decisions

1. **Column Naming**: Used `payload_type` and `payload_revision` to align with Axon conventions
2. **Index Strategy**: Tenant-first indexing for optimal multi-tenant performance
3. **Constraint Timing**: Added after data population to prevent migration failures
4. **Partial Indexes**: Used for high-frequency scenarios to minimize storage impact

### Testing Strategy

1. **Migration Testing**: Comprehensive tests for V1→V3 and V2→V3 migrations
2. **Compatibility Testing**: Verify existing EAF SDK operations continue working
3. **Performance Testing**: Benchmark query performance improvements
4. **Data Integrity Testing**: Validate constraint enforcement and data consistency

## Alternatives Considered

### Alternative 1: Separate Axon Event Store

- **Pros**: Clean separation, no migration complexity
- **Cons**: Data duplication, synchronization complexity, operational overhead
- **Rejected**: Violates single source of truth principle

### Alternative 2: Axon-Only Migration

- **Pros**: Simpler schema, optimal for Axon
- **Cons**: Breaking change for existing applications, high migration risk
- **Rejected**: Unacceptable backward compatibility impact

### Alternative 3: Dual Schema Approach

- **Pros**: Separate optimization for each use case
- **Cons**: Complex synchronization, doubled storage, operational complexity
- **Rejected**: Operational overhead outweighs benefits

## Success Metrics

1. **Compatibility**: 100% backward compatibility with existing EAF SDK operations
2. **Performance**:
   - TrackingEventProcessor queries: &gt;50% improvement
   - Aggregate loading: &gt;30% improvement
   - Write performance: &lt;10% degradation
3. **Migration**: Zero data loss during migration
4. **Adoption**: Successful integration with Axon Framework components

## Monitoring and Maintenance

1. **Index Usage Monitoring**: Regular analysis of index effectiveness
2. **Query Performance**: Monitor query execution plans and performance
3. **Storage Growth**: Track storage impact of new indexes
4. **Constraint Violations**: Monitor for data integrity issues

## Timeline

- **Analysis Phase**: Completed
- **Implementation**: Current sprint
- **Testing**: Next sprint
- **Deployment**: Following sprint
- **Monitoring**: Ongoing

## Related ADRs

- [ADR-0001: Hilla Kotlin Nullability Workaround](./0001-hilla-kotlin-nullability-workaround.md)

## References

- [Axon Framework 4.11.2 Documentation](https://docs.axoniq.io/axon-framework-reference/4.11/)
- [PostgreSQL Index Optimization](https://www.postgresql.org/docs/current/indexes.html)
