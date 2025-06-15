# Document Migration Task

## Purpose

Migrate BMAD-METHOD documents to match V4 template structure exactly, preserving all content while enforcing strict template compliance.

## Task Requirements

1. **Input**: User MUST specify the document to migrate (e.g., `docs/prd.md`)
2. **Template**: User MUST specify the V4 template to use (e.g., `.bmad-core/templates/prd-tmpl.md`)
3. **Validation**: Verify document and template are compatible types
4. **Output**: Creates migrated document with original name, backs up original with `.bak` extension

[[LLM: VALIDATION REQUIREMENTS:

- Both input document and template paths MUST be provided by the user
- Do NOT assume or select templates automatically
- Validate that document type matches template type (e.g., PRD → PRD template)
- Reject incompatible migrations (e.g., PRD → architecture template)

]]

## Migration Rules

### Strict Template Compliance

[[LLM: CRITICAL RULES:

1. The ONLY Level 2 headings (##) allowed are EXACTLY those in the V4 template
2. No additions, no removals, no modifications to Level 2 headings
3. All user content must be preserved and placed under appropriate template sections
4. Remove any existing table of contents
5. Never delete user content - find the most appropriate section
6. DO NOT add any LLM prompts, placeholders, or "TBD" content
7. Leave empty sections empty - no instructions or guidance text

]]

### Content Migration Process

1. **Read Template Structure**

   - Extract all Level 2 headings from the V4 template
   - These are the ONLY Level 2 headings allowed in the final document

2. **Analyze Original Document**

   - Identify all content blocks
   - Note original section organization
   - Flag any custom sections

3. **Create Backup First**

   - Rename original file: `mv filename.md filename.md.bak`
   - This preserves the original completely

4. **Create New File**

   - Create new `filename.md` from scratch
   - Start with template structure (all Level 2 headings)
   - For each content block from original:
     - Find the most appropriate V4 template section
     - If original had Level 2 heading not in template, demote to Level 3
     - Preserve all text, lists, code blocks, diagrams, tables
     - Remove only table of contents sections
   - **IMPORTANT**: Do NOT add any LLM prompts, placeholders, or instructions
   - If a template section has no matching content, leave it empty

5. **Validate Content Preservation**
   - For each major content block from original (excluding headings):
     - Use grep or search to verify it exists in new file
     - Report any content that couldn't be verified
   - This ensures no accidental content loss

## Example Migration

```markdown
Original (prd.md):

## Executive Summary

[content...]

## Custom Feature Section

[content...]

## Table of Contents

[toc...]

Template (prd-tmpl.md):

## Goals and Background Context

## Functional Requirements

## Success Metrics and KPIs

Result (prd.md):

## Goals and Background Context

[executive summary content moved here]

### Custom Feature Section

[content preserved but demoted to Level 3]

## Functional Requirements

## Success Metrics and KPIs
```

## Execution Instructions

[[LLM: When executing this task:

1. Ask user for BOTH: input file path AND template path (do not assume)
2. Validate compatibility:
   - Check document title/type (PRD, Architecture, etc.)
   - Ensure template matches document type
   - REJECT if types don't match (e.g., "Cannot migrate PRD to architecture template")
3. Read both files completely
4. Rename original to .bak: `mv original.md original.md.bak`
5. Extract Level 2 headings from template - these are MANDATORY
6. Create NEW file with original name
7. Build new content:
   - Start with all template Level 2 sections
   - Map original content to appropriate sections
   - Demote any non-template Level 2 headings to Level 3
   - Leave empty sections empty (no placeholders or instructions)
8. Validate content preservation:
   - Extract key content snippets from .bak file
   - Use grep to verify each exists in new file
   - Report any missing content
9. Report migration summary:
   - Sections moved/demoted
   - Content validation results
   - Any manual review needed

]]

### Document Type Detection

[[LLM: Detect document type by examining:

- File name (prd.md, architecture.md, etc.)
- Main title (# Product Requirements Document, # Architecture, etc.)
- Content patterns (user stories → PRD, technology stack → Architecture)

Template compatibility:

- prd-tmpl.md → Only for PRD documents
- architecture-tmpl.md → Only for backend/single architecture
- full-stack-architecture-tmpl.md → Only for architecture documents (can merge multiple)

]]

## Common Migrations

Valid migrations:

- `prd.md` → `.bmad-core/templates/prd-tmpl.md`
- `architecture.md` → `.bmad-core/templates/architecture-tmpl.md`
- `architecture.md` + `front-end-architecture.md` → `.bmad-core/templates/full-stack-architecture-tmpl.md`

Invalid migrations (MUST REJECT):

- `prd.md` → `.bmad-core/templates/architecture-tmpl.md`
- `architecture.md` → `.bmad-core/templates/prd-tmpl.md`
- `ux-ui-spec.md` → `.bmad-core/templates/prd-tmpl.md`

## Validation

After migration verify:

- [ ] All Level 2 headings match template exactly
- [ ] All original content is preserved (use grep validation)
- [ ] No table of contents remains
- [ ] Backup file exists with .bak extension
- [ ] Custom sections demoted to Level 3 or lower

### Content Validation Example

[[LLM: Example validation approach:

1. Extract significant text blocks from .bak file (>20 words)
2. For each block, grep in new file:
   ```bash
   grep -F "first 10-15 words of content block" newfile.md
   ```
3. Track validation results:
   - ✓ Found: content successfully migrated
   - ✗ Missing: needs investigation
4. Report any content that couldn't be found

]]
