"""Pacote reproduzível de distribuição; não inclui banco de dados ou segredos."""
from pathlib import Path
import hashlib, zipfile
root = Path(__file__).resolve().parents[1]
jar = root / 'target/app.jar'
if not jar.exists():
    raise SystemExit('Execute mvn clean verify primeiro.')
out = root / 'dist'
out.mkdir(exist_ok=True)
with zipfile.ZipFile(jar) as built:
    metadata = dict(line.split('=', 1) for line in built.read('META-INF/build-info.properties').decode().splitlines() if line and not line.startswith('#'))
version = metadata['build.version']
archive = out / f'ServiceHub-{version}.zip'
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for source, destination in [(jar, 'app.jar'), (root/'scripts/INICIAR.cmd','INICIAR.cmd'),(root/'README.md','README.md'),(root/'docs/COMO-USAR.md','COMO-USAR.md'),(root/'LICENSE','LICENSE')]:
        z.write(source, 'ServiceHub/'+destination)
(out/'SHA256SUMS.txt').write_text(hashlib.sha256(archive.read_bytes()).hexdigest()+'  '+archive.name+'\n')
print(archive)
