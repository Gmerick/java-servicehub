"""Prova de reinício usando o JAR real e um banco temporário em arquivo."""
import json, os, pathlib, socket, subprocess, tempfile, time, urllib.request
root = pathlib.Path(__file__).resolve().parents[1]
jar = root / 'target/app.jar'
with tempfile.TemporaryDirectory(prefix='servicehub-persistence-') as directory:
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    base = f'http://127.0.0.1:{port}/api'
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    def request(path, data=None):
        payload = None if data is None else json.dumps(data).encode()
        req = urllib.request.Request(base + path, payload, {'Content-Type': 'application/json'})
        with opener.open(req, timeout=3) as response:
            return json.load(response)
    def launch():
        log = open(pathlib.Path(directory)/'server.log', 'ab')
        process = subprocess.Popen(['java', '-jar', str(jar), f'--server.port={port}', '--app.demo=false'], cwd=directory, stdout=log, stderr=log)
        log.close()
        for _ in range(120):
            if process.poll() is not None:
                raise RuntimeError((pathlib.Path(directory)/'server.log').read_text())
            try:
                request('/health')
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
        customer = request('/customers', {'name':'Persistência', 'email':'persistencia@example.com', 'phone':'11'})
        asset = request('/assets', {'customerId':customer['id'], 'name':'Notebook persistente', 'serial':'PERSIST-01'})
        part = request('/parts', {'name':'Peça persistente', 'sku':'PERSIST-01', 'price':10.50, 'stock':5, 'minimum':1})
    finally:
        stop(process)
    process = launch()
    try:
        assert request('/customers')[0]['id'] == customer['id']
        assert request('/assets')[0]['id'] == asset['id']
        assert request('/parts')[0]['stock'] == 5
        print('PASS: clientes, equipamentos e peças preservados após reinício real do JAR.')
    finally:
        stop(process)
