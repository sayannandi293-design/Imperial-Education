#!/usr/bin/env bash
# Manual encrypted backup from a trusted machine; not scheduled on Render.
set -euo pipefail
umask 077
: "${DB_URL:?Set DB_URL}"
: "${DB_USERNAME:?Set DB_USERNAME}"
: "${DB_PASSWORD:?Set DB_PASSWORD}"
: "${BACKUP_AGE_RECIPIENT:?Set an age public-key recipient}"
for bin in pg_dump gzip age; do command -v "$bin" >/dev/null || { echo "Missing $bin" >&2; exit 1; }; done
mkdir -p "${BACKUP_DIR:-./private-backups}"
dest="${BACKUP_DIR:-./private-backups}/imperial-$(date -u +%Y%m%dT%H%M%SZ).sql.gz.age"
uri="${DB_URL#jdbc:}"
if [[ "$uri" != postgresql://* ]]; then echo "Expected PostgreSQL JDBC URL" >&2; exit 1; fi
export PGPASSWORD="$DB_PASSWORD"
trap 'unset PGPASSWORD' EXIT
pg_dump --username="$DB_USERNAME" --dbname="$uri" --no-owner --no-acl --format=plain |
 gzip -9 | age -r "$BACKUP_AGE_RECIPIENT" > "$dest"
echo "Encrypted backup: $dest"
echo "Copy offsite, test restoration, and never commit backup files or private keys."
