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
install -o root -g root -m 644 "$source_jar" "$app_dir/app.jar.next"
printf '%s  %s\n' "$2" "$app_dir/app.jar.next" | sha256sum --check --status
# Never silently initialize an empty database during an update.
if [[ -f "$app_dir/app.jar" ]]; then test -s "$data_dir/servicehub.mv.db"; fi
systemctl stop "$service"
# Backup frio antes de qualquer alteração de esquema pelo novo JAR.
if [[ -f "$data_dir/servicehub.mv.db" ]]; then
  cold_backup pre-update
fi
if [[ -f "$app_dir/app.jar" ]]; then
  cp -p "$app_dir/app.jar" "$app_dir/app.jar.previous"
fi
mv -f "$app_dir/app.jar.next" "$app_dir/app.jar"
systemctl start "$service"
if wait_healthy; then
  echo 'ServiceHub saudável em 127.0.0.1:8083'; exit 0
fi
systemctl stop "$service"
echo 'Falha de saúde; serviço parado. Consulte journalctl -u servicehub. Restaure JAR e backup compatíveis; não reverta apenas o JAR após migração.' >&2
exit 1
