#!/usr/bin/env bash
set -euo pipefail

# Cleanup script: deletes the entire ArkIve resource group (Azure SQL server/database,
# App Service plan, Web App and anything else created inside it). Requires an explicit
# interactive confirmation; nothing is deleted if the confirmation does not match.

RESOURCE_GROUP="rg-arkive-rm561408"

echo "This will PERMANENTLY delete resource group '$RESOURCE_GROUP' and every resource inside it"
echo "(Azure SQL server, Azure SQL database, App Service plan, Web App)."
echo "This action cannot be undone."
echo
read -r -p "Type DELETE (all caps) to confirm: " CONFIRMATION

if [[ "$CONFIRMATION" != "DELETE" ]]; then
  echo "Confirmation did not match 'DELETE'. Aborting; nothing was deleted."
  exit 1
fi

echo "Deleting resource group '$RESOURCE_GROUP'..."
az group delete \
  --name "$RESOURCE_GROUP" \
  --yes

echo "Resource group '$RESOURCE_GROUP' deleted."
