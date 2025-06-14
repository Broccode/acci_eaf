---
sidebar_position: 4
title: Configuration
---

# IAM Client SDK Configuration

Detailed configuration options for the EAF IAM Client SDK.

## ⚙️ Basic Configuration

```yaml
eaf:
  iam:
    base-url: 'https://iam.acci.com'
    client-id: 'my-service'
    client-secret: '${IAM_CLIENT_SECRET}'
```

## 🔧 Advanced Configuration

### Authentication Settings

```yaml
eaf:
  iam:
    jwt:
      public-key-url: '${IAM_PUBLIC_KEY_URL}'
      audience: 'eaf-services'
      issuer: 'acci-iam'
    token:
      cache-duration: PT30M
      refresh-threshold: PT5M
```

### Authorization Settings

```yaml
eaf:
  iam:
    authorization:
      cache-enabled: true
      cache-size: 1000
      cache-duration: PT15M
      default-deny: true
```

### Multi-Tenancy Settings

```yaml
eaf:
  iam:
    tenant:
      header-name: 'X-Tenant-ID'
      default-tenant: 'default'
      required: true
      validation-enabled: true
```

## 🌍 Environment Variables

Override configuration using environment variables:

- `IAM_BASE_URL`
- `IAM_CLIENT_ID`
- `IAM_CLIENT_SECRET`
- `IAM_PUBLIC_KEY_URL`

## 🔒 Security Configuration

### HTTPS Settings

```yaml
eaf:
  iam:
    security:
      ssl-verification: true
      timeout: PT30S
      max-retries: 3
```

### Rate Limiting

```yaml
eaf:
  iam:
    rate-limit:
      enabled: true
      requests-per-second: 100
      burst-size: 200
```

---

_Complete configuration guide for the EAF IAM Client SDK._
