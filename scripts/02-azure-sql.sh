#!/usr/bin/env bash
set -euo pipefail

# Provisions the Azure SQL side of ArkIve: logical server, database, and firewall rules.
#
# Required in the current shell before running this script:
#   export SQL_ADMIN_PASSWORD="..."
#
# Optional in the current shell:
#   export MY_PUBLIC_IP="..."   # adds a firewall rule so this machine can reach
#                                # Azure SQL directly (e.g. for the professor's CRUD demo).

RESOURCE_GROUP="rg-arkive-rm561408"
LOCATION="chilecentral"

SQL_SERVER_NAME="sqlserver-arkive-rm561408"
SQL_DB_NAME="arkivedb"
SQL_ADMIN_USER="fiapadmin"

: "${SQL_ADMIN_PASSWORD:?SQL_ADMIN_PASSWORD is required}"

echo "Creating Azure SQL logical server '$SQL_SERVER_NAME'..."
az sql server create \
  --name "$SQL_SERVER_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --admin-user "$SQL_ADMIN_USER" \
  --admin-password "$SQL_ADMIN_PASSWORD" \
  --output none

echo "Creating Azure SQL database '$SQL_DB_NAME' (Basic service objective)..."
az sql db create \
  --resource-group "$RESOURCE_GROUP" \
  --server "$SQL_SERVER_NAME" \
  --name "$SQL_DB_NAME" \
  --service-objective Basic \
  --output none

echo "Creating firewall rule to allow other Azure services (App Service) to reach this server..."
az sql server firewall-rule create \
  --resource-group "$RESOURCE_GROUP" \
  --server "$SQL_SERVER_NAME" \
  --name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0 \
  --output none

if [[ -n "${MY_PUBLIC_IP:-}" ]]; then
  echo "MY_PUBLIC_IP is set; creating firewall rule 'AllowLocalMachine' for $MY_PUBLIC_IP..."
  az sql server firewall-rule create \
    --resource-group "$RESOURCE_GROUP" \
    --server "$SQL_SERVER_NAME" \
    --name AllowLocalMachine \
    --start-ip-address "$MY_PUBLIC_IP" \
    --end-ip-address "$MY_PUBLIC_IP" \
    --output none
  echo "Firewall rule 'AllowLocalMachine' created for $MY_PUBLIC_IP."
else
  echo "MY_PUBLIC_IP is not set; skipping the local-machine firewall rule."
fi

echo "Azure SQL server '$SQL_SERVER_NAME' and database '$SQL_DB_NAME' are ready."
