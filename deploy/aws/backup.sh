#!/usr/bin/env bash
set -euo pipefail
[[ $EUID == 0 ]] || { echo 'Execute com sudo.' >&2; exit 1; }
source /usr/local/lib/servicehub/common.sh
exec 9>/run/lock/servicehub-deploy.lock
flock -n 9 || { echo 'Outra operação está em andamento.' >&2; exit 1; }
umask 077
check_database_config
was_active=false
systemctl is-active --quiet servicehub && was_active=true
trap 'if $was_active; then systemctl start servicehub && wait_healthy || exit 1; fi' EXIT
systemctl stop servicehub
cold_backup h2
