---
sidebar_position: 5
title: Troubleshooting
---

# Event Sourcing SDK Troubleshooting

Common issues and solutions when using the EAF Event Sourcing SDK.

## 🗄️ Database Issues

### Event Store Schema Not Found

**Problem**: Table "domain_events" does not exist  
**Solution**: Run database migrations or create the event store schema

### Connection Pool Exhausted

**Problem**: Too many concurrent database connections  
**Solution**: Increase pool size or optimize query patterns

## 🔄 Concurrency Issues

### OptimisticLockingException

**Problem**: Concurrent modifications to the same aggregate  
**Solution**: Implement retry logic or conflict resolution strategy

### Event Ordering Issues

**Problem**: Events appear out of order  
**Solution**: Check event versioning and database transaction isolation

## 📸 Snapshot Issues

### Snapshot Deserialization Failed

**Problem**: Cannot deserialize stored snapshots  
**Solution**: Implement snapshot migration or versioning strategy

### Large Snapshot Size

**Problem**: Snapshots consume too much storage  
**Solution**: Optimize snapshot content or increase snapshot frequency

## 🚀 Performance Issues

### Slow Event Loading

**Problem**: High latency when loading aggregates  
**Solution**: Optimize database indexes or implement event caching

### Memory Leaks

**Problem**: Memory usage keeps growing  
**Solution**: Check for unreleased aggregate references or event accumulation

## 📋 Common Error Messages

### "Aggregate not found"

Verify aggregate ID and check event store connectivity.

### "Event version mismatch"

Handle optimistic locking conflicts with proper retry logic.

### "Serialization failed"

Ensure event classes are properly serializable and versioned.

---

_Solutions for common issues with the EAF Event Sourcing SDK._
