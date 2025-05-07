# License Validation in ACCI EAF

Version: 1.0
Date: 2025-05-07
Status: Published

## Introduction

The ACCI EAF incorporates a license validation mechanism to control the usage of software built upon the framework, aligning with business requirements and ensuring compliance. This document describes the conceptual approach to license validation in V1, as primarily guided by ADR-003.

## Core Objective

To provide a reliable and reasonably secure method for validating that an instance of an EAF-based application is being used in accordance with the terms of its license.

## Key Concepts (ADR-003)

### 1. Hybrid Validation Approach

ACCI EAF V1 employs a hybrid license validation strategy:

- **Offline File Check (Mandatory):**
  - A digitally signed license file (e.g., JSON or XML format) is provided to the tenant/customer.
  - This file contains license entitlements and constraints (e.g., `tenantId`, `expiresAt`, feature flags, `maxCpuCores`).
  - The application validates the signature of this file using a public key embedded within the application or securely retrieved.
  - It then checks the constraints defined within the file against the current environment and usage.
  - This is the primary and mandatory check, ensuring the application can function in air-gapped environments.

- **Online Check (Optional but Recommended):**
  - The application can optionally connect to a central Axians License Server to re-validate the license or fetch updates.
  - This allows for more dynamic license management, such as remote deactivation or updates to entitlements.
  - The specifics of the online server API and its operation are outside the scope of EAF V1 development but the framework provides hooks for this interaction.
  - Policy on network failure during an online check (fail-open vs. fail-closed) can be configurable based on business requirements.

### 2. License File

- **Format:** Typically JSON or XML, containing key-value pairs for license attributes.
- **Content:** Common attributes include:
  - `licenseId`: Unique identifier for the license.
  - `tenantId`: The specific tenant this license is issued for (critical for multi-tenant setups).
  - `issuedAt`: Date and time when the license was issued.
  - `expiresAt`: Expiration date and time of the license. After this, the application might operate in a degraded mode or cease to function, depending on policy.
  - `featureFlags`: A list or map of features enabled by this license.
  - `constraints`: Usage limits, e.g., `maxUsers`, `maxDataVolume`, `maxCpuCores` (measurement method for CPU cores needs careful consideration and may depend on the environment).
- **Digital Signature:** The license file is cryptographically signed (e.g., using RSA or ECDSA) to ensure its integrity and authenticity. The application holds the corresponding public key to verify the signature.

### 3. Validation Logic (`libs/licensing`)

- A dedicated module/service within `libs/licensing` encapsulates the license validation logic.
- **Startup Validation:** License validation is typically performed when the application starts up. If validation fails, the application might refuse to start or operate in a restricted mode.
- **Periodic Re-validation (Optional):** The application may be configured to re-validate the license periodically during runtime.
- **Constraint Enforcement:** The licensing service provides methods for other parts of the application to check if specific features are enabled or if usage limits are being respected.

### 4. Security Considerations

- **Tamper Resistance:** The digital signature protects the license file from unauthorized modifications.
- **Bypass Prevention:** The validation logic should be robust against simple bypass attempts. Code obfuscation or other hardening techniques might be considered for the licensing module in highly sensitive scenarios, though this is not a primary focus for V1.
- **Secure Key Management:** The private key used for signing licenses must be kept highly secure. The public key embedded in the application needs to be protected from easy replacement.

## Integration with the Framework

- The licensing service can be injected into other services that need to check license status or feature enablement.
- For instance, specific modules or API endpoints might be guarded based on feature flags present in the validated license.

## Graceful Degradation / Enforcement Policy

- The behavior of the application upon license expiry or validation failure (e.g., hard stop, read-only mode, warning messages) is a business decision and should be configurable or clearly defined.
- For V1, the primary goal is to establish the validation mechanism; sophisticated enforcement policies can be built upon it.

## Related ADRs

- **ADR-003: License Validation:** This is the primary ADR defining the hybrid approach and core attributes of the license.

## Future Considerations

- More sophisticated online interactions (e.g., dynamic feature provisioning).
- Floating licenses or concurrent user licensing models.
- Detailed usage reporting to the license server (if implemented).

This document provides a conceptual overview of license validation in ACCI EAF. For detailed implementation, refer to `libs/licensing` and ADR-003.
