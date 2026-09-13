#!/usr/bin/env bash
set -euo pipefail

# Manual build + manual Azure CLI deployment (this is NOT CI/CD; no pipeline
# triggers this script, it is run by hand from the local machine).

RESOURCE_GROUP="rg-arkive-rm561408"
WEBAPP_NAME="webapp-arkive-rm561408"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Building ArkIve with the Maven Wrapper (tests included)..."
cd "$PROJECT_ROOT"
./mvnw clean package

# Locate the repackaged Spring Boot executable JAR, excluding the plain
# pre-repackage artifact that spring-boot-maven-plugin keeps as *.jar.original.
mapfile -t CANDIDATE_JARS < <(find "$PROJECT_ROOT/target" -maxdepth 1 -type f -name "*.jar" \
  ! -name "*.original" ! -name "*-sources.jar" ! -name "*-javadoc.jar" | sort)

if [[ ${#CANDIDATE_JARS[@]} -eq 0 ]]; then
  echo "ERROR: no deployable JAR found under target/. Aborting." >&2
  exit 1
fi

if [[ ${#CANDIDATE_JARS[@]} -gt 1 ]]; then
  echo "ERROR: expected exactly one deployable JAR under target/, found ${#CANDIDATE_JARS[@]}:" >&2
  printf '  %s\n' "${CANDIDATE_JARS[@]}" >&2
  exit 1
fi

JAR_PATH="${CANDIDATE_JARS[0]}"
echo "Deploying $JAR_PATH to web app '$WEBAPP_NAME'..."

az webapp deploy \
  --resource-group "$RESOURCE_GROUP" \
  --name "$WEBAPP_NAME" \
  --src-path "$JAR_PATH" \
  --type jar \
  --output none

echo "Deployment finished."
echo "Application URL: https://${WEBAPP_NAME}.azurewebsites.net"
