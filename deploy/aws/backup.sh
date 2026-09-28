#!/usr/bin/env bash
set -euo pipefail
[[ $EUID == 0 ]] || { echo 'Execute com sudo.' >&2; exit 1; }
exec 9>/run/lock/servicehub-deploy.lock
flock -n 9 || { echo 'Outra operação está em andamento.' >&2; exit 1; }
umask 077
was_active=false
systemctl is-active --quiet servicehub && was_active=true
trap 'if $was_active; then systemctl start servicehub; fi' EXIT
systemctl stop servicehub
test -f /var/lib/servicehub/servicehub.mv.db
archive="/var/backups/servicehub/h2-$(date -u +%Y%m%dT%H%M%S)-$$.tar.gz"
tar -czf "$archive" -C /var/lib/servicehub servicehub.mv.db
sha256sum "$archive" > "$archive.sha256"
echo "$archive"
