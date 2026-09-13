#!/usr/bin/env bash
set -euo pipefail

# Creates the ArkIve Web App on the App Service plan and configures its
# mandatory startup settings (datasource + active profile).
#
# Required in the current shell before running this script:
#   export SQL_ADMIN_PASSWORD="..."
#
# Optional in the current shell (each adds its own extra "az webapp config
# appsettings set" call below; the app starts fine without any of them):
#   export ARKIVE_CLINICAL_ENGINE_URL="..."          # override the default clinical-engine URL
#   export AZURE_SPEECH_ENDPOINT="..."                # + AZURE_SPEECH_API_KEY, enables transcription
#   export AZURE_SPEECH_API_KEY="..."
#   export ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED="true"   # + NAME/LOGIN/PASSWORD below
#   export ARKIVE_BOOTSTRAP_SYSADMIN_NAME="..."
#   export ARKIVE_BOOTSTRAP_SYSADMIN_LOGIN="..."
#   export ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD="..."

RESOURCE_GROUP="rg-arkive-rm561408"
APP_SERVICE_PLAN="plan-arkive-rm561408"
WEBAPP_NAME="webapp-arkive-rm561408"

SQL_SERVER_NAME="sqlserver-arkive-rm561408"
SQL_DB_NAME="arkivedb"
SQL_ADMIN_USER="fiapadmin"

: "${SQL_ADMIN_PASSWORD:?SQL_ADMIN_PASSWORD is required}"

echo "Creating web app '$WEBAPP_NAME' (Linux, Java 17)..."
az webapp create \
  --resource-group "$RESOURCE_GROUP" \
  --plan "$APP_SERVICE_PLAN" \
  --name "$WEBAPP_NAME" \
  --runtime "JAVA:17-java17" \
  --output none

# Azure SQL JDBC URL; credentials are passed as separate app settings, never embedded here.
JDBC_URL="jdbc:sqlserver://${SQL_SERVER_NAME}.database.windows.net:1433;database=${SQL_DB_NAME};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"

echo "Applying mandatory application settings to '$WEBAPP_NAME'..."
az webapp config appsettings set \
  --resource-group "$RESOURCE_GROUP" \
  --name "$WEBAPP_NAME" \
  --settings \
    SPRING_PROFILES_ACTIVE="azure" \
    SPRING_DATASOURCE_URL="$JDBC_URL" \
    SPRING_DATASOURCE_USERNAME="$SQL_ADMIN_USER" \
    SPRING_DATASOURCE_PASSWORD="$SQL_ADMIN_PASSWORD" \
  --output none

# --- Optional settings below: each is only applied if already exported locally. ---

if [[ -n "${ARKIVE_CLINICAL_ENGINE_URL:-}" ]]; then
  echo "ARKIVE_CLINICAL_ENGINE_URL is set; overriding the default clinical-engine URL..."
  az webapp config appsettings set \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEBAPP_NAME" \
    --settings ARKIVE_CLINICAL_ENGINE_URL="$ARKIVE_CLINICAL_ENGINE_URL" \
    --output none
else
  echo "ARKIVE_CLINICAL_ENGINE_URL not set; the application keeps its built-in default URL."
fi

if [[ -n "${AZURE_SPEECH_ENDPOINT:-}" && -n "${AZURE_SPEECH_API_KEY:-}" ]]; then
  echo "AZURE_SPEECH_ENDPOINT and AZURE_SPEECH_API_KEY are set; enabling Azure Speech transcription..."
  az webapp config appsettings set \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEBAPP_NAME" \
    --settings \
      AZURE_SPEECH_ENDPOINT="$AZURE_SPEECH_ENDPOINT" \
      AZURE_SPEECH_API_KEY="$AZURE_SPEECH_API_KEY" \
    --output none
else
  echo "AZURE_SPEECH_ENDPOINT/AZURE_SPEECH_API_KEY not set; transcription stays disabled (app works fine without it)."
fi

if [[ "${ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED:-}" == "true" ]]; then
  : "${ARKIVE_BOOTSTRAP_SYSADMIN_NAME:?ARKIVE_BOOTSTRAP_SYSADMIN_NAME is required when ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED=true}"
  : "${ARKIVE_BOOTSTRAP_SYSADMIN_LOGIN:?ARKIVE_BOOTSTRAP_SYSADMIN_LOGIN is required when ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED=true}"
  : "${ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD:?ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD is required when ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED=true}"
  echo "ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED=true; enabling initial SysAdmin bootstrap..."
  az webapp config appsettings set \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEBAPP_NAME" \
    --settings \
      ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED="true" \
      ARKIVE_BOOTSTRAP_SYSADMIN_NAME="$ARKIVE_BOOTSTRAP_SYSADMIN_NAME" \
      ARKIVE_BOOTSTRAP_SYSADMIN_LOGIN="$ARKIVE_BOOTSTRAP_SYSADMIN_LOGIN" \
      ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD="$ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD" \
    --output none
else
  echo "ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED not set to 'true'; initial SysAdmin bootstrap stays disabled."
fi

echo "Web app '$WEBAPP_NAME' created and configured."
