#!/bin/bash

# Optimized Test Runner for Distribution Engine API
# Uses Docker volumes to cache Gradle dependencies - MUCH faster on repeated runs!
# No image rebuilding, just runs tests directly in a container

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GRADLE_IMAGE="gradle:8.14-jdk21"
GRADLE_CACHE_VOLUME="distri-engine-api-gradle-cache"
PROJECT_NAME="distri-engine-api"

# Get absolute path to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "================================================="
echo "  Distribution Engine API - Fast Test Runner"
echo "  (Volume-cached strategy - super fast!)"
echo "================================================="
echo ""

# Function to print step headers
print_step() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

# Prerequisites check
print_step "Step 1: Checking Prerequisites"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker is not installed${NC}"
    echo "Install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi
echo -e "${GREEN}✓ Docker is installed${NC}"

if ! docker ps &> /dev/null; then
    echo -e "${RED}✗ Docker daemon is not running${NC}"
    echo "Please start Docker and try again"
    exit 1
fi
echo -e "${GREEN}✓ Docker daemon is running${NC}"

# Create Gradle cache volume if it doesn't exist
if ! docker volume inspect ${GRADLE_CACHE_VOLUME} &> /dev/null; then
    echo -e "${YELLOW}Creating Gradle cache volume (one-time setup)...${NC}"
    docker volume create ${GRADLE_CACHE_VOLUME}
    echo -e "${GREEN}✓ Gradle cache volume created${NC}"
else
    echo -e "${GREEN}✓ Gradle cache volume exists${NC}"
fi

# Run tests in Docker with mounted volumes
print_step "Step 2: Running Tests"
echo "Strategy: Mount source code + cache dependencies in volume"
echo "This approach:"
echo "  ✓ Caches Gradle dependencies (fast on 2nd+ runs)"
echo "  ✓ No image rebuilding required"
echo "  ✓ Incremental compilation (Gradle handles it)"
echo ""
echo -e "${YELLOW}Running test suite...${NC}"
echo ""

START_TIME=$(date +%s)

# Resolve GitHub Packages credentials: env vars take priority, then ~/.gradle/gradle.properties
GRADLE_PROPS="${HOME}/.gradle/gradle.properties"
if [ -z "${USERNAME_TOKEN}" ] && [ -f "${GRADLE_PROPS}" ]; then
    USERNAME_TOKEN=$(grep '^usernameToken=' "${GRADLE_PROPS}" | cut -d'=' -f2)
fi
if [ -z "${PASSWORD_TOKEN}" ] && [ -f "${GRADLE_PROPS}" ]; then
    PASSWORD_TOKEN=$(grep '^passwordToken=' "${GRADLE_PROPS}" | cut -d'=' -f2)
fi

# Run Gradle tests in container with volumes
# - Mount source code
# - Mount Gradle cache volume for dependencies
set +e  # Temporarily disable exit on error to capture exit code
docker run --rm \
    -v "${PROJECT_ROOT}:/app" \
    -v "${GRADLE_CACHE_VOLUME}:/home/gradle/.gradle" \
    -v "${HOME}/.gradle/gradle.properties:/home/gradle/.gradle/gradle.properties:ro" \
    -w /app \
    ${GRADLE_IMAGE} \
    gradle test --no-daemon

TEST_EXIT_CODE=$?
set -e  # Re-enable exit on error

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""

if [ ${TEST_EXIT_CODE} -eq 0 ]; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}✓ All Tests Passed!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "Execution time: ${DURATION} seconds"
    echo ""
    exit 0
else
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}✗ Tests Failed${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "Execution time: ${DURATION} seconds"
    echo "Check the output above for test failure details"
    echo ""
    exit 1
fi
