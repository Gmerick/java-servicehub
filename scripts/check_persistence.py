"""Prova de reinício usando o JAR real e um banco temporário em arquivo."""
import http.cookiejar, json, os, pathlib, socket, subprocess, tempfile, time, urllib.request, urllib.parse, zipfile, sys, xml.etree.ElementTree as ET
demo = len(sys.argv) > 1 and sys.argv[1] == 'demo'
root = pathlib.Path(__file__).resolve().parents[1]
jar = root / 'target/app.jar'
with zipfile.ZipFile(jar) as archive:
    metadata = dict(line.split('=', 1) for line in archive.read('META-INF/build-info.properties').decode().splitlines() if line and not line.startswith('#'))
expected_version = ET.parse(root / 'pom.xml').getroot().find('{http://maven.apache.org/POM/4.0.0}version').text
assert metadata['build.version'] == expected_version, 'Metadados do JAR divergem do Maven'
with tempfile.TemporaryDirectory(prefix='servicehub-persistence-') as directory:
    private = pathlib.Path(directory)/'data/servicehub.mv.db'
    if demo:
        private.parent.mkdir()
        private.write_bytes(b'original-private-database-sentinel')
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    base = f'http://127.0.0.1:{port}/api'
    auth = json.loads((root / 'target/test-auth.json').read_text())
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def request(path, data=None):
        payload = None if data is None else json.dumps(data).encode()
        headers = {'Content-Type': 'application/json'}
        if data is not None: headers['X-CSRF-TOKEN'] = request('/csrf')['token']
        req = urllib.request.Request(base + path, payload, headers)
        with opener.open(req, timeout=3) as response:
            return json.load(response)
    def launch():
        log = open(pathlib.Path(directory)/'server.log', 'ab')
        profile = '--spring.profiles.active=demo' if demo else '--app.demo=false'
        process = subprocess.Popen(['java', '-jar', str(jar), f'--server.port={port}', profile], cwd=directory, env={k:v for k,v in os.environ.items() if k not in ('DB_URL','DB_PASSWORD','DEMO_DB_URL','APP_DEMO','SERVER_ADDRESS','PORT')} | {'ADMIN_USERNAME':auth['username'],'ADMIN_PASSWORD_HASH':auth['hash']}, stdout=log, stderr=log)
        log.close()
        for _ in range(120):
            if process.poll() is not None:
                raise RuntimeError((pathlib.Path(directory)/'server.log').read_text())
            try:
                request('/health')
                token=request('/csrf')['token']
                login=urllib.request.Request(f'http://127.0.0.1:{port}/login', urllib.parse.urlencode({'username':auth['username'],'password':auth['password']}).encode(), {'X-CSRF-TOKEN':token, 'Content-Type':'application/x-www-form-urlencoded'})
                with opener.open(login,timeout=5) as response: assert response.status==200
                if demo and len(request('/customers')) < 2:
                    time.sleep(.25)
                    continue
                return process
            except (OSError, ValueError):
                time.sleep(.25)
        process.terminate()
        process.wait(timeout=15)
        raise RuntimeError('Servidor não iniciou em 30 segundos')
    def stop(process):
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
    process = launch()
    try:
        assert request('/health') == {'status': 'UP', 'version': metadata['build.version']}
        print(f'PASS: /api/health corresponde ao artefato Maven: {expected_version}')
        customer = request('/customers', {'name':'Persistência', 'email':'persistencia@example.com', 'phone':'11'})
        asset = request('/assets', {'customerId':customer['id'], 'name':'Notebook persistente', 'serial':'PERSIST-01'})
        part = request('/parts', {'name':'Peça persistente', 'sku':'PERSIST-01', 'price':10.50, 'stock':5, 'minimum':1})
        snapshot = {path: request(path) for path in ['/customers','/assets','/parts','/orders']}
    finally:
        stop(process)
    process = launch()
    try:
        assert request('/health') == {'status': 'UP', 'version': metadata['build.version']}
        assert {path: request(path) for path in snapshot} == snapshot
        if demo:
            assert private.read_bytes() == b'original-private-database-sentinel'
            assert (pathlib.Path(directory)/'data-demo/servicehub.mv.db').stat().st_size > 0
        print(f'PASS ({"demo" if demo else "private"}): dados completos preservados após reinício real do JAR; banco separado quando demo.')
    finally:
        stop(process)
