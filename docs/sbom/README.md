# Software Bill of Materials (SBOM)

This document describes the generation of a CycloneDX SBOM for the ACCI EAF monorepo.

## Purpose

The SBOM provides full transparency of third-party dependencies and their versions, supporting supply-chain security and compliance requirements such as ISO 27001 or SOC2.

## Generation

```bash
# One-time installation (already in devDependencies)
pnpm install --frozen-lockfile

# Generate SBOM
pnpm generate:sbom
# Output: dist/sbom.json
```

## CI Pipeline

The GitLab CI pipeline contains a dedicated `sbom` stage that automatically produces the SBOM artefact for every merge request and push to `main`. The file is available as pipeline artefact for auditing purposes.

## Validation (Optional)

You can validate the generated SBOM against the CycloneDX 1.5 schema using `cyclonedx-cli`:

```bash
cyclonedx validate --input-file dist/sbom.json
```

## References

- <https://cyclonedx.org/>
- <https://github.com/CycloneDX/cyclonedx-npm>
