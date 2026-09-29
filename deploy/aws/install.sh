#!/usr/bin/env bash
set -euo pipefail
[[ $EUID == 0 ]] || { echo 'Execute com sudo.' >&2; exit 1; }
cd -- "$(dirname -- "$0")"
dnf install -y java-17-amazon-corretto-headless python3
id servicehub >/dev/null 2>&1 || useradd --system --home-dir /var/lib/servicehub --shell /sbin/nologin servicehub
install -d -o root -g root -m 755 /opt/servicehub
install -d -o servicehub -g servicehub -m 700 /var/lib/servicehub
install -d -o root -g root -m 700 /etc/servicehub /var/backups/servicehub
if [[ ! -e /etc/servicehub/servicehub.env ]]; then
  (umask 077; cat > /etc/servicehub/servicehub.env <<'ENV'
DB_URL=jdbc:h2:file:/var/lib/servicehub/servicehub;WRITE_DELAY=0
APP_DEMO=false
ENV
  )
fi
install -o root -g root -m 644 servicehub.service /etc/systemd/system/servicehub.service
install -d -o root -g root -m 755 /usr/local/lib/servicehub
install -o root -g root -m 644 common.sh /usr/local/lib/servicehub/common.sh
install -o root -g root -m 755 restore.sh /usr/local/sbin/servicehub-restore
install -o root -g root -m 755 backup.sh /usr/local/sbin/servicehub-backup
install -o root -g root -m 755 update.sh /usr/local/sbin/servicehub-update
systemctl daemon-reload
systemctl enable servicehub
echo 'Instalado. Envie o JAR e execute sudo servicehub-update /caminho/app.jar SHA256.'
