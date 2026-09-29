# Evidências da preparação 1.2.0

Data: 2026-09-29. Branch `codex/servicehub-public-demo`, originada da main integrada `92789d1da46615468812042d717ebfa0fcb4c2ef`. Nenhum deploy, acesso ao banco privado, alteração de infraestrutura AWS, plano, DNS ou Security Group foi realizado nesta entrega.

## Executado localmente

- Maven `verify`: testes HTTP Java com Spring Security efetivo e credenciais aleatórias geradas em target, sem desativar filtros. Login válido/inválido; anônimo bloqueado; visitante GET permitido e POST/PUT/PATCH/DELETE proibidos; CSRF ausente/antigo rejeitado; logout; expiração real; limite HTTP429; cookies HttpOnly/SameSite/Secure; limite de memória; guarda de banco e HTTPS. Suíte de negócio continua autenticada como ADMIN.
- `scripts/check_persistence.py`: JAR real, login/CSRF reais, gravações em H2 temporário, processo parado e reiniciado, comparação de clientes/equipamentos/peças. Versão compara pom.xml, build-info.properties e /api/health.
- Playwright: 11 testes aprovados, incluindo fluxo completo ADMIN, estoque/cancelamento, validações, exportação, XSS, falha de rede, navegação e visitante desktop/móvel. Chamadas diretas de visitante com CSRF válido recebem403.
- Capturas reais: [login desktop](screenshots/demo-login-desktop.png), [login móvel](screenshots/demo-login-mobile.png), [visitante desktop](screenshots/demo-visitor-desktop.png), [visitante móvel](screenshots/demo-visitor-mobile.png).
- Caddy2.11.4 oficial Windowsamd64, arquivo comparado ao SHA512 publicado pela release: `caddy validate` aprovou o Caddyfile com domínio `.invalid` de validação. Não iniciou servidor nem emitiu certificado.

## Validação Linux na CI

O check `validate` executa build/testes Java, sintaxe shell, sandbox de manutenção para `private` e `demo`, persistência, Playwright e empacotamento. A sandbox usa tar, gzip, SHA256, flock, timeout e cópias reais, mas substitui systemctl/curl/chown; não equivale a um restore na EC2. Verifica checksum inválido antes de parar, arquivo malicioso/link, preservação prévia, banco ausente, atualização e versão incorreta com prazo limitado. No caso demo, o arquivo privado sentinela deve continuar intocado.

O resultado e link da execução final são registrados no PR. Não existe job de deploy automático. Artefatos não incluem target/test-auth.json, banco ou credenciais administrativas.

## Pendente antes de declarar publicação validada

- Escolha do domínio e aprovação da configuração final DNS/rede apresentada em PUBLIC-DEMO.md.
- Credenciais reais configuradas localmente pelo proprietário; não foram criadas para a EC2 nesta etapa.
- Backup privado recente, corte controlado para banco demo isolado e verificação de integridade do banco antigo parado.
- Instalação Linux e unidade Caddy, emissão/renovação ACME, HTTPS real, cookies no navegador e túnel TLS com domínio correto. Teste de atributo Secure e unidade de validação HTTPS não equivalem a essas provas externas.
- Repetir autenticação/permissões e manutenção real na EC2 após o corte autorizado; avaliar memória/CPU sob carga na t3.micro. Não houve teste de carga ou proteção distribuída contra abuso.

Estado AWS permanece o informado pelo proprietário: versão1.1.0 privada em Ohio. Não revalidada nem modificada por esta entrega de código. O PR não deve ser confundido com publicação concluída.
