---
sidebar_position: 6
title: Troubleshooting
---

# IAM Client SDK Troubleshooting

Common issues and solutions when using the EAF IAM Client SDK.

## 🔑 Authentication Issues

### Invalid JWT Token

**Problem**: JWT token validation fails  
**Solution**: Check token format, expiration, and signing key

### Token Expired

**Problem**: Access token has expired  
**Solution**: Implement token refresh logic or re-authenticate

### Service Authentication Failed

**Problem**: Service-to-service authentication fails  
**Solution**: Verify client credentials and IAM service connectivity

## 🛡️ Authorization Issues

### Permission Denied

**Problem**: User lacks required permissions  
**Solution**: Check user roles and permission assignments in IAM

### Role Not Found

**Problem**: Required role doesn't exist  
**Solution**: Verify role definitions and user role assignments

### Authorization Cache Issues

**Problem**: Stale permission data in cache  
**Solution**: Clear cache or reduce cache duration

## 🏢 Multi-Tenancy Issues

### Tenant Not Found

**Problem**: Specified tenant doesn't exist  
**Solution**: Verify tenant ID and tenant registration

### Missing Tenant Context

**Problem**: No tenant information in request  
**Solution**: Ensure tenant header is properly set

### Cross-Tenant Access

**Problem**: Unauthorized access to other tenant's data  
**Solution**: Verify tenant isolation and access controls

## 🔌 Connectivity Issues

### IAM Service Unreachable

**Problem**: Cannot connect to IAM service  
**Solution**: Check network connectivity and service URL

### SSL/TLS Issues

**Problem**: SSL certificate validation fails  
**Solution**: Verify certificates or disable SSL verification for development

### Timeout Errors

**Problem**: Requests to IAM service timeout  
**Solution**: Increase timeout settings or check service performance

## 📋 Common Error Messages

### "Invalid client credentials"

Verify client ID and secret configuration.

### "Token signature verification failed"

Check JWT signing key configuration.

### "Insufficient privileges"

User needs additional permissions or roles.

### "Tenant context required"

Ensure tenant header is present in requests.

---

_Solutions for common issues with the EAF IAM Client SDK._
