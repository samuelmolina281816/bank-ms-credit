#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"
VAULT_EXE="${VAULT_EXE:-/c/Dev/Software/vault/vault.exe}"
SECRET_PATH="secret/banca-project/bank-ms-credit"

if [[ ! -f "$ENV_FILE" ]]; then
  printf 'No existe %s. Copialo desde .env.example y completa las credenciales locales.\n' "$ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$VAULT_EXE" ]]; then
  printf 'No se encontro el ejecutable de Vault: %s\nConfigura VAULT_EXE con su ruta correcta.\n' "$VAULT_EXE" >&2
  exit 1
fi

set -a
# .env must contain trusted local KEY=value assignments.
source "$ENV_FILE"
set +a

: "${CREDIT_DB_USERNAME:?Define CREDIT_DB_USERNAME en .env}"
: "${CREDIT_DB_PASSWORD:?Define CREDIT_DB_PASSWORD en .env}"
: "${VAULT_TOKEN:?Define VAULT_TOKEN en .env}"
export VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"

"$VAULT_EXE" status >/dev/null

"$VAULT_EXE" kv put "$SECRET_PATH" \
  "CREDIT_DB_USERNAME=$CREDIT_DB_USERNAME" \
  "CREDIT_DB_PASSWORD=$CREDIT_DB_PASSWORD"

"$VAULT_EXE" kv get -field=CREDIT_DB_USERNAME "$SECRET_PATH" >/dev/null
printf '\n--- Secreto Credit creado en %s ---\n' "$SECRET_PATH"
