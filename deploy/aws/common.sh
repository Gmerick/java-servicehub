#!/usr/bin/env bash
# Callers are root and hold the deployment lock.
case "${SERVICEHUB_INSTANCE:-private}" in
  private) service=servicehub; data_dir=/var/lib/servicehub; backup_dir=/var/backups/servicehub; app_dir=/opt/servicehub; config_dir=/etc/servicehub ;;
  demo) service=servicehub-demo; data_dir=/var/lib/servicehub-demo; backup_dir=/var/backups/servicehub-demo; app_dir=/opt/servicehub-demo; config_dir=/etc/servicehub-demo ;;
  *) echo 'Instancia invalida (private ou demo).' >&2; exit 1 ;;
esac
check_database_config() {
  grep -Fxq "DB_URL=jdbc:h2:file:$data_dir/servicehub;WRITE_DELAY=0" "$config_dir/servicehub.env" || {
    echo 'DB_URL divergente: ajuste os procedimentos antes da manutencao.' >&2; return 1;
  }
}
cold_backup() {
  local archive="$backup_dir/$1-$(date -u +%Y%m%dT%H%M%S)-$$.tar.gz"
  test -s "$data_dir/servicehub.mv.db"
  tar -czf "$archive.partial" -C "$data_dir" servicehub.mv.db
  gzip -t "$archive.partial"
  mv "$archive.partial" "$archive"
  sha256sum "$archive" > "$archive.sha256"
  sha256sum --check --status "$archive.sha256"
  echo "$archive"
}
wait_healthy() {
  local expected
  expected=$(python3 - "$app_dir/app.jar" <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as jar:
    props = dict(line.split('=', 1) for line in jar.read('META-INF/build-info.properties').decode().splitlines() if line and not line.startswith('#'))
print(props['build.version'])
PY
  ) || return 1
  timeout --signal=TERM --kill-after=2s 120s bash -o pipefail -c '
    until curl --fail --silent --connect-timeout 2 --max-time 5 http://127.0.0.1:8083/api/health |
      python3 -c '\''import json,sys; h=json.load(sys.stdin); sys.exit(0 if h == {"status":"UP","version":sys.argv[1]} else 1)'\'' "$1" 2>/dev/null; do
      sleep 2
    done
  ' _ "$expected" || return 1
  systemctl is-active --quiet "$service"
}
