"""Interactive local BCrypt generator using the dependency already inside the JAR.
Requires Python 3 and JDK 17; no network, password arguments or third-party Python packages.
"""
import pathlib, subprocess, sys, tempfile, zipfile
if len(sys.argv) != 3:
    raise SystemExit('Uso: python scripts/create_admin.py CAMINHO/app.jar CAMINHO/admin.secrets.properties')
with tempfile.TemporaryDirectory() as tmp, zipfile.ZipFile(sys.argv[1]) as jar:
    names = [n for n in jar.namelist() if n.startswith('BOOT-INF/lib/spring-security-crypto-') and n.endswith('.jar')]
    if len(names) != 1: raise SystemExit('Biblioteca BCrypt não encontrada no artefato.')
    library = pathlib.Path(tmp) / 'crypto.jar'
    library.write_bytes(jar.read(names[0]))
    result = subprocess.run(['java', '--class-path', str(library), str(pathlib.Path(__file__).with_name('AdminCredentials.java')), sys.argv[2]])
    raise SystemExit(result.returncode)
