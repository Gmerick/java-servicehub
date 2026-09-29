#!/usr/bin/env bash
set -euo pipefail
[[ $EUID == 0 && $# == 2 ]] || { echo 'Uso: sudo servicehub-update /caminho/app.jar SHA256' >&2; exit 1; }
source /usr/local/lib/servicehub/common.sh
umask 077
source_jar=$(realpath -- "$1")
[[ $2 =~ ^[a-fA-F0-9]{64}$ ]] || { echo 'SHA256 inválido.' >&2; exit 1; }
printf '%s  %s\n' "$2" "$source_jar" | sha256sum --check --status
exec 9>/run/lock/servicehub-deploy.lock
flock -n 9 || { echo 'Outra operação está em andamento.' >&2; exit 1; }
check_database_config
install -o root -g root -m 644 "$source_jar" /opt/servicehub/app.jar.next
printf '%s  %s\n' "$2" /opt/servicehub/app.jar.next | sha256sum --check --status
# Never silently initialize an empty database during an update.
if [[ -f /opt/servicehub/app.jar ]]; then test -s "$data_dir/servicehub.mv.db"; fi
systemctl stop servicehub
# Backup frio antes de qualquer alteração de esquema pelo novo JAR.
if [[ -f /var/lib/servicehub/servicehub.mv.db ]]; then
  cold_backup pre-update
fi
if [[ -f /opt/servicehub/app.jar ]]; then
  cp -p /opt/servicehub/app.jar /opt/servicehub/app.jar.previous
fi
mv -f /opt/servicehub/app.jar.next /opt/servicehub/app.jar
systemctl start servicehub
if wait_healthy; then
  echo 'ServiceHub saudável em 127.0.0.1:8083'; exit 0
fi
systemctl stop servicehub
echo 'Falha de saúde; serviço parado. Consulte journalctl -u servicehub. Restaure JAR e backup compatíveis; não reverta apenas o JAR após migração.' >&2
exit 1
