#!/usr/bin/env bash
set -euo pipefail

# Creates the Azure Resource Group that will hold every ArkIve resource
# (Azure SQL server/database, App Service plan, Web App).

RESOURCE_GROUP="rg-arkive-rm561408"
LOCATION="chilecentral"

echo "Creating resource group '$RESOURCE_GROUP' in '$LOCATION'..."

az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output none

echo "Resource group '$RESOURCE_GROUP' created successfully in '$LOCATION'."
