"""Exercise maintenance scripts in a disposable directory, never against systemd/data.
Only absolute paths, root/chown checks and readiness timing are adapted; systemctl
and curl are doubles. File copies, tar, SHA256, locks and timeout are real Linux tools.
"""
import hashlib, io, os, pathlib, subprocess, tarfile, tempfile, zipfile, sys
instance = sys.argv[1] if len(sys.argv) > 1 else 'private'
assert instance in ('private', 'demo')
service = 'servicehub-demo' if instance == 'demo' else 'servicehub'
root = pathlib.Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as directory:
    base = pathlib.Path(directory)
    paths = ['/var/lib/servicehub', '/var/backups/servicehub', '/opt/servicehub', '/etc/servicehub', '/usr/local/lib/servicehub', '/run/lock']
    for path in paths + [f'/var/lib/{service}', f'/var/backups/{service}', f'/opt/{service}', f'/etc/{service}']:
        (base / path.lstrip('/')).mkdir(parents=True, exist_ok=True)
    bin_dir = base / 'bin'; bin_dir.mkdir()
    def write(name, content):
        p = bin_dir / name; p.write_text(content); p.chmod(0o755)
    write('systemctl', '#!/bin/bash\ncase "$1" in\nis-active) test "$(cat "$STATE")" = active;;\nstop) echo stopped > "$STATE";;\nstart) echo active > "$STATE";;\nesac\n')
    write('curl', '#!/bin/bash\ncat "$HEALTH"\n')
    write('chown', '#!/bin/bash\nexit 0\n')
    env = dict(os.environ, SERVICEHUB_INSTANCE=instance, PATH=str(bin_dir)+':'+os.environ['PATH'], STATE=str(base/'state'), HEALTH=str(base/'health'))
    (base/'state').write_text('active'); (base/'health').write_text('{"status":"UP","version":"9.8.7"}')
    for source in (root/'deploy/aws').glob('*.sh'):
        text = source.read_text().replace('$EUID == 0', '1 == 1').replace('-o root -g root ', '')
        for path in paths: text = text.replace(path, str(base/path.lstrip('/')))
        text = text.replace('120s', '1s').replace('sleep 2', 'sleep 0.05')
        target = base/source.name; target.write_text(text)
        if source.name == 'common.sh': (base/'usr/local/lib/servicehub/common.sh').write_text(text)
    original = base/'var/lib/servicehub/servicehub.mv.db'; original.write_bytes(b'private-untouched')
    db = base/f'var/lib/{service}/servicehub.mv.db'; db.write_bytes(b'prior-state')
    backups = base/f'var/backups/{service}'
    (base/f'etc/{service}/servicehub.env').write_text(f'DB_URL=jdbc:h2:file:{base}/var/lib/{service}/servicehub;WRITE_DELAY=0\n')
    jar = base/f'opt/{service}/app.jar'
    with zipfile.ZipFile(jar, 'w') as z: z.writestr('META-INF/build-info.properties', 'build.version=9.8.7\n')
    candidate = base/'candidate.jar'; candidate.write_bytes(jar.read_bytes())
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    def run(script, *args, ok=True):
        result = subprocess.run(['bash', str(base/script), *map(str,args)], env=env, capture_output=True, text=True, timeout=10)
        assert (result.returncode == 0) == ok, (script, result.stdout, result.stderr)
        return result
    run('backup.sh')
    backup = next(backups.glob('h2-*.tar.gz'))
    run('restore.sh', backup, '0'*64, ok=False)
    assert db.read_bytes() == b'prior-state' and (base/'state').read_text().strip() == 'active'
    for member_name, kind in [('../escape', tarfile.REGTYPE), ('servicehub.mv.db', tarfile.SYMTYPE)]:
        bad = base/'bad.tar.gz'
        with tarfile.open(bad,'w:gz') as t:
            member=tarfile.TarInfo(member_name); member.type=kind; member.linkname='/etc/passwd'; member.size=3 if kind==tarfile.REGTYPE else 0
            t.addfile(member, io.BytesIO(b'bad') if member.size else None)
        run('restore.sh', bad, sha(bad), ok=False)
        assert db.read_bytes()==b'prior-state'
    db.write_bytes(b'newer-state')
    run('restore.sh', backup, sha(backup))
    assert db.read_bytes()==b'prior-state'
    previous=next(backups.glob('before-restore-*.tar.gz'))
    with tarfile.open(previous) as t: assert t.extractfile('servicehub.mv.db').read()==b'newer-state'
    run('update.sh',candidate,'0'*64,ok=False)
    db.unlink(); run('update.sh',candidate,sha(candidate),ok=False); db.write_bytes(b'prior-state')
    run('update.sh',candidate,sha(candidate))
    assert db.read_bytes()==b'prior-state'
    snapshot=next(backups.glob('pre-update-*.tar.gz'))
    assert pathlib.Path(str(snapshot)+'.sha256').exists()
    (base/'health').write_text('{"status":"UP","version":"wrong"}')
    run('update.sh',candidate,sha(candidate),ok=False)
    assert (base/'state').read_text().strip()=='stopped' and db.read_bytes()==b'prior-state'
    if instance == 'demo': assert original.read_bytes() == b'private-untouched'
    print(f'PASS ({instance}): backup, restore, previous state, checksum/path/link rejection, missing DB, update preservation and wrong-version deadline')
