---
sidebar_position: 5
title: Troubleshooting
---

# Eventing SDK Troubleshooting

Common issues and solutions when using the EAF Eventing SDK.

## 🔌 Connection Issues

### NATS Connection Failed

**Problem**: Cannot connect to NATS server  
**Solution**: Verify NATS URL and server availability

### Connection Timeout

**Problem**: Connection timeouts during high load  
**Solution**: Increase connection timeout or connection pool size

## 📤 Publishing Issues

### Event Not Published

**Problem**: Events are not reaching subscribers  
**Solution**: Check subject naming and NATS connectivity

### Serialization Errors

**Problem**: Events cannot be serialized  
**Solution**: Ensure event classes are properly serializable

## 📥 Subscription Issues

### Handler Not Triggered

**Problem**: Event handlers are not being called  
**Solution**: Verify event handler registration and subject matching

### Duplicate Event Processing

**Problem**: Events are processed multiple times  
**Solution**: Implement idempotent event handlers

## 🔧 Performance Issues

### High Memory Usage

**Problem**: Memory consumption keeps growing  
**Solution**: Check for event handler memory leaks

### Slow Event Processing

**Problem**: Events are processed slowly  
**Solution**: Optimize handler code and consider async processing

## 📋 Common Error Messages

### "Subject not found"

Check subject naming conventions and registration.

### "Serialization failed"

Verify event class structure and serialization configuration.

### "Handler timeout"

Increase handler timeout or optimize processing logic.

---

_Solutions for common issues with the EAF Eventing SDK._
