#!/usr/bin/env bash
# Preparation only: does not stop/start services, install Caddy or open any port.
set -euo pipefail
[[ $EUID == 0 ]] || { echo 'Execute com sudo.' >&2; exit 1; }
cd -- "$(dirname -- "$0")"
exec 9>/run/lock/servicehub-deploy.lock
flock -n 9 || { echo 'Outra manutencao esta em andamento.' >&2; exit 1; }
id servicehub-demo >/dev/null 2>&1 || useradd --system --home-dir /var/lib/servicehub-demo --shell /sbin/nologin servicehub-demo
install -d -o root -g root -m 755 /opt/servicehub-demo
install -d -o servicehub-demo -g servicehub-demo -m 700 /var/lib/servicehub-demo
install -d -o root -g servicehub-demo -m 750 /etc/servicehub-demo
install -d -o root -g root -m 700 /var/backups/servicehub-demo
if [[ ! -e /etc/servicehub-demo/servicehub.env ]]; then
  (umask 077; printf '%s\n' 'DB_URL=jdbc:h2:file:/var/lib/servicehub-demo/servicehub;WRITE_DELAY=0' > /etc/servicehub-demo/servicehub.env)
fi
install -o root -g root -m 644 servicehub-demo.service /etc/systemd/system/servicehub-demo.service
install -d -o root -g root -m 755 /usr/local/lib/servicehub
install -o root -g root -m 644 ../aws/common.sh /usr/local/lib/servicehub/common.sh
for action in backup restore update; do
  install -o root -g root -m 755 "../aws/$action.sh" "/usr/local/sbin/servicehub-$action"
done
systemctl daemon-reload
echo 'Preparado sem ativar. Credenciais, JAR, Caddy, DNS, backup e autorizacao de publicacao ainda sao necessarios.'
