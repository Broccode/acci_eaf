# ADR-007: Control Plane Bootstrapping

* **Status:** Accepted
* **Date:** 2025-04-21
* **Stakeholders:** [Names or roles]

## Context and Problem Statement

The Control Plane API requires at least one initial system administrator user (and possibly an initial tenant) to be usable after installation/first startup. This initial setup process needs to be defined, especially for the offline deployment scenario.

## Considered Options

1. **Manual Database Entries:** Admin must manually execute SQL commands after installation.
    * *Pros:* Simplest implementation within the EAF.
    * *Cons:* Error-prone, cumbersome for the customer/admin, not idempotent.
2. **Automatic Check on Startup:** The API checks on startup if admins exist and creates a default one if necessary.
    * *Pros:* Automated.
    * *Cons:* "Magical", password management difficult, when exactly should it run?
3. **Dedicated CLI Command:** A command-line command (part of the Control Plane API) for initial setup.
    * *Pros:* Explicit, controllable, can be implemented idempotently, can securely accept parameters (password), easily scriptable (for setup/update scripts).
    * *Cons:* Requires executing a separate command after installation.

## Decision

We choose **Option 3: Dedicated CLI Command**. The Control Plane API will provide a command like `setup-admin --email <email> --password <secure-password>` (executable e.g., via `npm run cli -- ...` in the container). This command will check if an admin already exists and, if not, securely create the first admin user.

## Consequences

* Setup/installation scripts in the tarball must call this command after the initial container startup.
* The command must be idempotent (doing nothing or reporting completion on subsequent calls).
* Process must be clearly documented in the setup guide.
