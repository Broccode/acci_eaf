#!/bin/bash

# ============================================================================
# Dependency Migration Script
# ============================================================================
# This script helps migrate existing modules to use centralized dependency versions
# Usage: ./gradle/scripts/migrate-dependencies.sh [module-path]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔧 EAF Dependency Migration Script${NC}"
echo "=================================================="

# Function to show usage
show_usage() {
    echo "Usage: $0 [module-path]"
    echo ""
    echo "Examples:"
    echo "  $0 apps/iam-service"
    echo "  $0 libs/eaf-core"
    echo "  $0 all  # Migrate all modules"
    echo ""
    exit 1
}

# Function to migrate a single module
migrate_module() {
    local module_path=$1
    local build_file="$module_path/build.gradle.kts"

    if [[ ! -f "$build_file" ]]; then
        echo -e "${RED}❌ Build file not found: $build_file${NC}"
        return 1
    fi

    echo -e "${YELLOW}🔄 Migrating: $module_path${NC}"

    # Create backup
    cp "$build_file" "$build_file.backup"
    echo -e "${BLUE}📋 Created backup: $build_file.backup${NC}"

    # Common replacements
    sed -i.tmp \
        -e 's/implementation("org\.springframework\.boot:spring-boot-starter:\([^"]*\)")/implementation("org.springframework.boot:spring-boot-starter:${rootProject.extra[\"springBootVersion\"]}")/g' \
        -e 's/implementation("org\.springframework\.boot:spring-boot-starter-web:\([^"]*\)")/implementation("org.springframework.boot:spring-boot-starter-web:${rootProject.extra[\"springBootVersion\"]}")/g' \
        -e 's/implementation("org\.springframework\.boot:spring-boot-starter-security:\([^"]*\)")/implementation("org.springframework.boot:spring-boot-starter-security:${rootProject.extra[\"springBootVersion\"]}")/g' \
        -e 's/implementation("org\.springframework\.boot:spring-boot-starter-data-jpa:\([^"]*\)")/implementation("org.springframework.boot:spring-boot-starter-data-jpa:${rootProject.extra[\"springBootVersion\"]}")/g' \
        -e 's/implementation("org\.springframework\.boot:spring-boot-starter-validation:\([^"]*\)")/implementation("org.springframework.boot:spring-boot-starter-validation:${rootProject.extra[\"springBootVersion\"]}")/g' \
        -e 's/testImplementation("org\.springframework\.boot:spring-boot-starter-test:\([^"]*\)")/testImplementation("org.springframework.boot:spring-boot-starter-test:${rootProject.extra[\"springBootVersion\"]}")/g' \
        -e 's/testImplementation("org\.springframework\.boot:spring-boot-testcontainers:\([^"]*\)")/testImplementation("org.springframework.boot:spring-boot-testcontainers:${rootProject.extra[\"springBootVersion\"]}")/g' \
        -e 's/testImplementation("org\.junit\.jupiter:junit-jupiter:\([^"]*\)")/testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.extra[\"junitVersion\"]}")/g' \
        -e 's/testImplementation("io\.mockk:mockk:\([^"]*\)")/testImplementation("io.mockk:mockk:${rootProject.extra[\"mockkVersion\"]}")/g' \
        -e 's/testImplementation("com\.ninja-squad:springmockk:\([^"]*\)")/testImplementation("com.ninja-squad:springmockk:${rootProject.extra[\"springMockkVersion\"]}")/g' \
        -e 's/testImplementation("org\.testcontainers:junit-jupiter:\([^"]*\)")/testImplementation("org.testcontainers:junit-jupiter:${rootProject.extra[\"testcontainersVersion\"]}")/g' \
        -e 's/testImplementation("org\.testcontainers:postgresql:\([^"]*\)")/testImplementation("org.testcontainers:postgresql:${rootProject.extra[\"testcontainersVersion\"]}")/g' \
        -e 's/testImplementation("org\.testcontainers:testcontainers:\([^"]*\)")/testImplementation("org.testcontainers:testcontainers:${rootProject.extra[\"testcontainersVersion\"]}")/g' \
        -e 's/testImplementation("com\.tngtech\.archunit:archunit-junit5:\([^"]*\)")/testImplementation("com.tngtech.archunit:archunit-junit5:${rootProject.extra[\"archunitVersion\"]}")/g' \
        -e 's/testImplementation("org\.assertj:assertj-core:\([^"]*\)")/testImplementation("org.assertj:assertj-core:${rootProject.extra[\"assertjVersion\"]}")/g' \
        -e 's/implementation("org\.postgresql:postgresql:\([^"]*\)")/implementation("org.postgresql:postgresql:${rootProject.extra[\"postgresqlVersion\"]}")/g' \
        -e 's/runtimeOnly("org\.postgresql:postgresql:\([^"]*\)")/runtimeOnly("org.postgresql:postgresql:${rootProject.extra[\"postgresqlVersion\"]}")/g' \
        -e 's/implementation("org\.flywaydb:flyway-core:\([^"]*\)")/implementation("org.flywaydb:flyway-core:${rootProject.extra[\"flywayVersion\"]}")/g' \
        -e 's/implementation("org\.flywaydb:flyway-database-postgresql:\([^"]*\)")/implementation("org.flywaydb:flyway-database-postgresql:${rootProject.extra[\"flywayVersion\"]}")/g' \
        -e 's/implementation("org\.jetbrains\.kotlinx:kotlinx-coroutines-core:\([^"]*\)")/implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.extra[\"kotlinCoroutinesVersion\"]}")/g' \
        -e 's/implementation("org\.jetbrains\.kotlinx:kotlinx-coroutines-reactor:\([^"]*\)")/implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${rootProject.extra[\"kotlinCoroutinesVersion\"]}")/g' \
        -e 's/testImplementation("org\.jetbrains\.kotlinx:kotlinx-coroutines-test:\([^"]*\)")/testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra[\"kotlinCoroutinesVersion\"]}")/g' \
        -e 's/implementation("jakarta\.servlet:jakarta\.servlet-api:\([^"]*\)")/implementation("jakarta.servlet:jakarta.servlet-api:${rootProject.extra[\"jakartaServletVersion\"]}")/g' \
        -e 's/implementation("com\.nimbusds:nimbus-jose-jwt:\([^"]*\)")/implementation("com.nimbusds:nimbus-jose-jwt:${rootProject.extra[\"nimbusJoseVersion\"]}")/g' \
        -e 's/implementation("org\.slf4j:slf4j-api:\([^"]*\)")/implementation("org.slf4j:slf4j-api:${rootProject.extra[\"slf4jVersion\"]}")/g' \
        "$build_file"

    # Remove temporary file
    rm -f "$build_file.tmp"

    echo -e "${GREEN}✅ Migration completed for: $module_path${NC}"

    # Show diff
    if command -v diff &> /dev/null; then
        echo -e "${BLUE}📊 Changes made:${NC}"
        diff -u "$build_file.backup" "$build_file" || true
        echo ""
    fi
}

# Function to migrate all modules
migrate_all() {
    echo -e "${YELLOW}🔄 Migrating all modules...${NC}"

    # Find all module directories with build.gradle.kts
    find . -name "build.gradle.kts" -not -path "./build.gradle.kts" -not -path "./gradle/*" | while read -r build_file; do
        module_path=$(dirname "$build_file" | sed 's|^\./||')
        migrate_module "$module_path"
    done
}

# Main script logic
if [[ $# -eq 0 ]]; then
    show_usage
fi

case "$1" in
    "all")
        migrate_all
        ;;
    "-h"|"--help")
        show_usage
        ;;
    *)
        if [[ -d "$1" ]]; then
            migrate_module "$1"
        else
            echo -e "${RED}❌ Directory not found: $1${NC}"
            exit 1
        fi
        ;;
esac

echo ""
echo -e "${GREEN}🎉 Migration completed!${NC}"
echo ""
echo -e "${BLUE}📋 Next Steps:${NC}"
echo "1. Review the changes in each module"
echo "2. Test the build: ./gradlew build"
echo "3. Run validation: ./gradlew validateDependencies"
echo "4. Commit changes if everything works"
echo ""
echo -e "${YELLOW}💡 Tip: Backup files (.backup) have been created for safety${NC}"
