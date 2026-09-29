#!/usr/bin/env bash
set -euo pipefail
[[ $EUID == 0 && $# == 2 ]] || { echo 'Uso: sudo servicehub-restore BACKUP SHA256_CONFIAVEL' >&2; exit 1; }
source /usr/local/lib/servicehub/common.sh
exec 9>/run/lock/servicehub-deploy.lock
flock -n 9 || { echo 'Outra operacao esta em andamento.' >&2; exit 1; }
check_database_config
[[ $2 =~ ^[a-fA-F0-9]{64}$ ]] || { echo 'SHA256 invalido.' >&2; exit 1; }
umask 077
stage=$(mktemp -d "$data_dir/.restore-XXXXXX")
trap 'rm -rf -- "$stage"' EXIT
# Validate a single regular member; never trust archive paths or links.
cp -- "$1" "$stage/input.tar.gz"
python3 - "$stage/input.tar.gz" "$2" "$stage/servicehub.mv.db" <<'PY'
import hashlib, pathlib, sys, tarfile, shutil
with open(sys.argv[1], 'rb') as payload:
    digest = hashlib.file_digest(payload, 'sha256').hexdigest() if hasattr(hashlib, 'file_digest') else None
    if digest is None:
        h = hashlib.sha256()
        for chunk in iter(lambda: payload.read(1024 * 1024), b''): h.update(chunk)
        digest = h.hexdigest()
    if digest != sys.argv[2].lower(): raise SystemExit('Checksum do backup divergente')
    payload.seek(0)
    with tarfile.open(fileobj=payload, mode='r:gz') as archive:
        members = archive.getmembers()
        if len(members) != 1 or members[0].name != 'servicehub.mv.db' or not members[0].isfile() or members[0].size == 0:
            raise SystemExit('Backup deve conter somente servicehub.mv.db regular e nao vazio')
        with archive.extractfile(members[0]) as source, open(sys.argv[3], 'wb') as destination:
            shutil.copyfileobj(source, destination)
PY
systemctl stop "$service"
# Failure after stopping leaves the service stopped and preserves the old database.
cold_backup before-restore
chown "$service:$service" "$stage/servicehub.mv.db"
chmod 600 "$stage/servicehub.mv.db"
mv -f "$stage/servicehub.mv.db" "$data_dir/servicehub.mv.db"
systemctl start "$service"
if wait_healthy; then
  echo 'Restaurado; confira os registros pela interface antes de retomar o uso.'
else
  systemctl stop "$service"
  echo 'Falha de saude; servico parado. Estado anterior no backup before-restore.' >&2
  exit 1
fi
