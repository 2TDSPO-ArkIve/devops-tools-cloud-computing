#!/usr/bin/env bash
set -euo pipefail

# Creates the Linux App Service Plan that will host the ArkIve Web App.

RESOURCE_GROUP="rg-arkive-rm561408"
LOCATION="chilecentral"
APP_SERVICE_PLAN="plan-arkive-rm561408"

echo "Creating Linux App Service plan '$APP_SERVICE_PLAN' (SKU F1) in '$LOCATION'..."

az appservice plan create \
  --name "$APP_SERVICE_PLAN" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --sku F1 \
  --is-linux \
  --output none

echo "App Service plan '$APP_SERVICE_PLAN' created successfully."
