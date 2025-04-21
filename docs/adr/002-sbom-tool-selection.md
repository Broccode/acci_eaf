# ADR-002: Selection of CycloneDX for SBOM Generation

* **Status:** Accepted
* **Date:** 2025-04-25
* **Authors:** [Security Architect]

## Context

Software Bill of Materials (SBOM) is becoming an essential part of software development, especially for enterprise applications that require compliance with security standards like ISO 27001 and SOC2. An SBOM provides a comprehensive inventory of all components, libraries, and modules used in software, which is valuable for:

1. Vulnerability management
2. License compliance
3. Supply chain risk assessment
4. Regulatory compliance
5. Security audits

The ACCI EAF needs to incorporate SBOM generation into its build process to support these use cases. Several SBOM standards and tools exist:

* **CycloneDX**: An OWASP Foundation project, focused on application security.
* **SPDX**: Developed by the Linux Foundation, more focused on license compliance.
* **SWID**: Software Identification Tags, an international standard (ISO/IEC 19770-2).
* **Custom format**: Developing our own SBOM format.

For tooling, several options were considered:

* **@cyclonedx/bom**: Official JavaScript/TypeScript SDK for CycloneDX
* **spdx-sbom-generator**: Linux Foundation tool for SPDX
* **syft**: An SBOM generator supporting multiple formats
* **custom implementation**: Building our own SBOM generation tool

## Decision

We will use **CycloneDX** as the SBOM format and **@cyclonedx/bom** as the primary tool for SBOM generation in the ACCI EAF.

## Rationale

CycloneDX was selected for the following reasons:

1. **Security Focus**: CycloneDX was specifically designed with application security in mind, which aligns with our security requirements and compliance goals.

2. **Format Comprehensiveness**: CycloneDX supports all the component types we need to document (libraries, frameworks, applications) and includes fields for vulnerabilities, licenses, and other metadata.

3. **JavaScript/TypeScript Support**: The official @cyclonedx/bom library provides native TypeScript support, which integrates well with our technology stack.

4. **Active Development**: CycloneDX is actively maintained by the OWASP Foundation with regular updates and improvements.

5. **Tool Ecosystem**: There's a growing ecosystem of tools that support CycloneDX format, including vulnerability scanners and SBOM analyzers.

6. **Adoption Rate**: CycloneDX is widely adopted in the industry, especially in sectors concerned with security compliance.

7. **Automation Capabilities**: The @cyclonedx/bom library is designed to be easily integrated into build pipelines, supporting our CI/CD requirements.

8. **Standards Compliance**: CycloneDX is aligned with various standards and regulations related to software supply chain security.

The other options were rejected for the following reasons:

* **SPDX**: While comprehensive, it has a stronger focus on license compliance rather than security, and has less robust JavaScript/TypeScript tooling.
* **SWID**: Less adoption in modern web application development and fewer available tools for our specific technology stack.
* **Custom format**: Would require significant development effort and would not be compatible with industry-standard tools and processes.

## Consequences

### Positive

* Streamlined compliance with security standards and regulations
* Better visibility into dependencies and potential vulnerabilities
* Integration with existing security tools that support CycloneDX
* Reduced effort in manual inventory tracking
* Improved due diligence for security audits

### Negative

* Additional build-time overhead for SBOM generation
* Need to maintain SBOM generation as part of the CI/CD pipeline
* Potential false positives in vulnerability reports based on SBOM data
* Need to keep the SBOM tooling updated

### Mitigations

* Optimize SBOM generation to minimize build time impact
* Implement careful review processes for vulnerability reports
* Automate the update process for SBOM tooling
* Provide clear documentation on how to interpret and use SBOM data

## Implementation Notes

* SBOM generation will be integrated into the build process using the @cyclonedx/bom library
* SBOMs will be generated at both the library level (for `libs/`) and the application level (for `apps/`)
* SBOM artifacts will be stored with build artifacts for traceability
* We'll implement a process to periodically validate SBOMs against vulnerability databases
* Documentation will be provided on how to analyze SBOM data for security purposes

## Alternatives Considered

### SPDX with spdx-sbom-generator

SPDX is a well-established standard, particularly for license compliance. However:

* The JavaScript/TypeScript tooling is less mature compared to CycloneDX
* It's more focused on license compliance than security vulnerabilities
* Integration with security tools is less developed

### Syft

Syft is a powerful SBOM generator supporting multiple formats:

* It's primarily designed for container images rather than source code projects
* It would introduce an additional external dependency
* It doesn't offer the same level of native TypeScript integration

### Custom Implementation

Building our own SBOM generation tool:

* Would require significant development effort
* Would not be compatible with industry-standard tools
* Would create maintenance overhead
* Would lack the community validation of established standards

## References

* [CycloneDX Specification](https://cyclonedx.org/specification/overview/)
* [CycloneDX JavaScript/TypeScript Library](https://github.com/CycloneDX/cyclonedx-node-module)
* [SPDX Specification](https://spdx.dev/specifications/)
* [Software Supply Chain Security](https://www.cisa.gov/resources-tools/resources/software-supply-chain-best-practices)
* [ISO/IEC 19770-2 (SWID Tags)](https://www.iso.org/standard/65666.html)
