<!-- Linear Issue: https://linear.app/acci/issue/ACCI-42/epic-1-story-9-documentation-and-sbom -->
# Epic-1 - Story-9

Documentation & SBOM

**As a** framework developer  
**I want** an initial set of framework documentation and automated SBOM generation  
**so that** I provide transparency, compliance readiness and an easier onboarding experience.

## Status

Completed

## Context

This story belongs to Epic-1 (Framework Core & Architecture) and focuses on two pillars:

1. Delivering the first iteration of the ACCI EAF documentation (setup guide, architecture overview, core concepts, how-tos).
2. Integrating Software Bill of Materials (SBOM) generation using CycloneDX via `@cyclonedx/cyclonedx-npm`.

A structured documentation foundation improves developer experience and knowledge sharing, while SBOM generation supports supply-chain security and compliance requirements (e.g. ISO 27001, SOC2).

## Estimation

Story Points: 3

## Acceptance Criteria

1. [x] Documentation skeletons exist under `docs/` with at least placeholder content:  
   • `docs/setup/` – Setup Guide  
   • `docs/architecture/` – Architecture Overview  
   • `docs/concept/` – Core Concepts  
   • `docs/how-tos/` – Task-oriented guides  
   • `docs/sbom/` – SBOM process description
2. [x] `nx generate-sbom` target creates `dist/sbom.json` without errors.
3. [x] CI pipeline stores `dist/sbom.json` as an artefact.
4. [x] `docs/setup/README.md` (or similar) explains local SBOM generation.
5. [x] This story file contains a valid Linear URL.

## Tasks

1. **Documentation Skeleton**
   1. [x] Create folder structure and placeholder `README.md` files.
   2. [x] Draft outline for each section (table of contents).
2. **SBOM Integration**
   1. [x] Add `@cyclonedx/cyclonedx-npm` as dev dependency.
   2. [x] Add Nx target `generate-sbom` executing `cyclonedx-npm` with proper output path.
   3. [x] Document target usage in root `README.md`.
3. **CI Pipeline**
   1. [x] Extend `.gitlab-ci.yml` with `sbom` stage to run `nx generate-sbom`.
   2. [x] Store `dist/sbom.json` as pipeline artefact.
4. **Validation (Optional)**
   1. [x] Evaluate `cyclonedx-cli` for schema validation of the generated SBOM.

## Constraints

- Documentation must be pure Markdown and live under `docs/`.
- SBOM generation must run offline-safe; use `--ignore-npm-errors`.
- Keep additional CI time below 30 seconds.
- All new scripts/targets must work on macOS and Linux.

## References

- PRD § "Features and Requirements" → FR-SBOM  
- CycloneDX v1.5: <https://cyclonedx.org/>  
- `@cyclonedx/cyclonedx-npm`: <https://github.com/CycloneDX/cyclonedx-npm>  
- Example documentation structure from Docusaurus (optional future work)

## Linear Issue

- **ID:** ACCI-42  
- **URL:** <https://linear.app/acci/issue/ACCI-42/epic-1-story-9-documentation-and-sbom>
