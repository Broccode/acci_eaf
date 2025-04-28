# ACCI EAF - CLI Commands

## Introduction

This document provides information about the available Command Line Interface (CLI) commands in the ACCI Enterprise Application Framework. These commands help with administrative tasks, bootstrapping, and management operations.

## Commands Overview

The CLI commands are implemented using [nest-commander](https://docs.nestjs.com/recipes/nest-commander), a package that integrates the NestJS framework with the Commander.js CLI library. This allows for the creation of robust, testable CLI commands that leverage NestJS dependency injection and module structure.

### Control Plane API CLI Commands

#### Setup Admin User

The `setup-admin` command is used to create the first administrative user for the Control Plane API. This command implements the bootstrapping mechanism described in ADR-007.

**Command Syntax:**

```bash
npx nx run control-plane-api:cli -- setup-admin --email <admin-email> --password <secure-password>
```

**Options:**

- `--email`, `-e`: The email address for the admin user (required)
- `--password`, `-p`: The password for the admin user (required, min 8 characters)

**Examples:**

```bash
# Create a new admin user
npx nx run control-plane-api:cli -- setup-admin --email admin@example.com --password securePassword123

# Using short options
npx nx run control-plane-api:cli -- setup-admin -e admin@example.com -p securePassword123
```

**Behavior:**

- If an admin with the specified email already exists, the command will not create a duplicate (idempotent operation)
- The command validates the email format and password length
- The password is securely hashed before storage
- On success, the command outputs a confirmation message with the email of the created admin

**Error Handling:**

- Invalid email format: The command will exit with an error if the email format is invalid
- Password too short: The command will exit with an error if the password is less than 8 characters
- Missing parameters: The command will exit with an error if either email or password is missing

## Running in Production Environments

In production environments (using the offline tarball deployment method), CLI commands should be run within the Docker container:

```bash
docker exec -it control-plane-api-container node /app/cli.js setup-admin --email admin@example.com --password securePassword123
```

Or using `docker-compose`:

```bash
docker-compose run --rm control-plane-api node /app/cli.js setup-admin --email admin@example.com --password securePassword123
```

## Security Considerations

- Always use strong passwords when creating admin users
- Consider using environment variables or a secure method to pass sensitive information like passwords
- CLI commands should only be accessible to authorized personnel
- In production environments, consider using a dedicated service account for running administrative commands

## Extending the CLI

The CLI framework allows for the creation of additional commands as needed. To create a new command:

1. Create a new command class that extends `CommandRunner` from nest-commander
2. Implement the required methods (`run` and option parsers)
3. Add the command to the appropriate CLI module

For more information, refer to the [nest-commander documentation](https://docs.nestjs.com/recipes/nest-commander).
