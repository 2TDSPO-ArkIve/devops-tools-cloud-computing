# Copy this file to scripts/env.local.sh, fill in real values, then run:
#   source scripts/env.local.sh
# before running the provisioning/deploy scripts (01-05).
#
# scripts/env.local.sh is gitignored and must NEVER be committed.
# The provisioning scripts do NOT source this file automatically; the
# variables must already exist in your shell when you run them.

# --- Required ---

# Azure SQL logical server administrator password (used by 02-azure-sql.sh
# and 04-webapp.sh). Choose a password that meets Azure SQL's complexity rules.
export SQL_ADMIN_PASSWORD="<defina-localmente>"

# --- Optional ---

# Your current public IP address. When set, 02-azure-sql.sh creates an extra
# firewall rule ("AllowLocalMachine") so this machine can connect directly to
# Azure SQL (e.g. during the professor's live CRUD demonstration).
# Leave empty/unset to skip that rule.
export MY_PUBLIC_IP="<seu-ip-publico-opcional>"

# Enables 04-webapp.sh to configure the app to create the very first SYSADMIN
# account on startup. If ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED="true", the three
# variables below become required together.
export ARKIVE_BOOTSTRAP_SYSADMIN_ENABLED="false"
export ARKIVE_BOOTSTRAP_SYSADMIN_NAME="<nome-do-administrador>"
export ARKIVE_BOOTSTRAP_SYSADMIN_LOGIN="<login-ou-email>"
export ARKIVE_BOOTSTRAP_SYSADMIN_PASSWORD="<senha-segura>"

# Azure Speech transcription integration. The application starts and works
# fully without these; only the optional audio-transcription feature is
# disabled when they are absent.
export AZURE_SPEECH_ENDPOINT="<endpoint-opcional>"
export AZURE_SPEECH_API_KEY="<chave-opcional>"

# Overrides the application's built-in default clinical-engine URL.
# Only needed if you are pointing at a different clinical-engine deployment.
export ARKIVE_CLINICAL_ENGINE_URL="<url-opcional>"
