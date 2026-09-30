# Publicação HTTPS — evidências de 30/09/2026

A demonstração está acessível em https://servicehub.leadopssender.com.br. Autenticação, permissões, fluxo administrativo, persistência e manutenção foram validados no endereço público. Escolha **Explorar como visitante** para consulta de dados fictícios, sem senha e sem permissão de gravação.

## Artefato e infraestrutura

- PR #2 integrado sem bypass: `160d648c51a4540992763901b3c4f9c54f60e77e`.
- [CI main aprovada](https://github.com/Gmerick/java-servicehub/actions/runs/36668875868): Java, scripts Linux, persistência privada/demo, interface e empacotamento. Artefato `ServiceHub-EC2` obtido dessa execução.
- JAR SHA256 verificado localmente e na EC2: `e760f84a1f80289ba6268607a41bcefb95757f3874dbcca2a7616ba849ee7362`.
- Mesma EC2 `i-0d26611f9832a61c9`, `servicehub-dev`, Ohio `us-east-2`, t3.micro. Nenhum novo recurso AWS. Free Plan confirmado no console; saldo exibido na verificação atual: US$119,51, 181 dias. Valores não são créditos mensais nem garantia de duração.
- IPv4/DNS `18.224.63.142`; A confirmado nos três autoritativos CajuHost e nos resolvedores 1.1.1.1/8.8.8.8. Nenhum outro registro alterado nesta publicação.
- Caddy oficial 2.11.4 Linux amd64, SHA512 conferido contra a release. Configuração validada, usuário `caddy`; Java como `servicehub-demo` em 127.0.0.1:8083, API Caddy em 127.0.0.1:2019.
- SG `sg-0a68721ee7dc69a9d`: TCP22 de 201.74.182.206/32; TCP80 e443 de0.0.0.0/0. Nenhuma regra8083/2019 ou IPv6. Regras novas `sgr-058b43e0c480ebe8e` (80) e `sgr-0c1f28090f8b473b7` (443), salvas após confirmação explícita.
- Banco demo `/var/lib/servicehub-demo/servicehub.mv.db`, modo600, proprietário servicehub-demo. Credenciais externas root:servicehub-demo640, diretório750; criadas interativamente pelo proprietário, sem senha ou hash no Git/logs.
- `servicehub` privado parado/desabilitado; `servicehub-demo` e `caddy` ativos/habilitados. 5,8 GiB livres após preparação.

## Verificações públicas reais

- `/api/health`: UP /1.2.0; versão do build Maven.
- HTTP retorna308 para HTTPS. TLS1.3 verificado com cadeia confiável e hostname, sem ignorar certificados. Let's Encrypt YE1; SAN servicehub.leadopssender.com.br, validade até29/12/2026 10:38:33UTC. Renovação automática configurada no Caddy; uma renovação futura ainda não foi observada.
- Acesso anônimo a clientes/equipamentos/estoque/ordens/painel retorna401.
- Login inválido401; login/entrada visitante sem CSRF403.
- Visitante autenticado lê dados; gravações POST de cadastros, ordens, status e reposição retornam403 mesmo com CSRF válido. PUT/PATCH/DELETE também403.
- Cookie de sessão Secure, HttpOnly e SameSite=Lax verificado no HTTPS real.
- Logout sem CSRF403; logout correto200; sessão anterior sem acesso401.
- Conexões externas80/443 aceitas;8083/2019 inacessíveis. `ss` confirmou Java e API administrativa Caddy somente em loopback.
- Login público abriu normalmente no Chrome. Login ADMIN efetuado privadamente pelo proprietário e fluxo real executado no Chrome: cliente Oficina Boreal (demo HTTPS), equipamento, ordem #4, serviço R$50 + peça R$229, aprovação, execução e conclusão. Total R$279, seis eventos no histórico, estoque da Fonte ATX reduzido de6 para5. Logout retornou à tela de login. Expiração por inatividade e limitação foram testadas na CI; não foi aguardada a expiração de30 minutos no ambiente público.

## Backup, restauração e preservação

Backup privado frio completo antes do corte: `/var/backups/servicehub/private-pre-public-20260930T113334.tar.gz`, inclui JAR, configuração, banco e unidade systemd. Cópia externa no computador, na pasta privada `.servicehub/backups/private-20260930T113334.tar.gz`. SHA256 nos dois destinos: `0bb3d34a19bdc9d7a82c9bd7b0e09e9ca3c2f1b171a46826a0b62afe261a690a`.

Banco privado parado: SHA256 `f26a8bae7d9caa6d7d89974a2e39096bf4bbc07811e77dae910bb360fcf481a5`; JAR privado `3a8ba544ec31ceb408405bef8a3b7ff05f1499d66f101330f9fd962e6bf662f8`. Ambos conferidos após o corte e novamente após manutenção demo, sem alteração.

Teste real demo com Caddy parado durante toda a janela para impedir gravações públicas: backup frio recém-criado, checksum validado, tentativa com checksum incorreto rejeitada antes de parar o serviço (PID manteve-se), seguida de restore válido. O restore guardou o estado anterior em `before-restore-20260930T113845-96319.tar.gz`, reiniciou Java e verificou saúde/versão. Caddy reaberto apenas ao terminar.

Backup demo: `/var/backups/servicehub-demo/h2-20260930T113838-96234.tar.gz`, SHA256 `d8db69836381d441923283588292e02907958b515bd77661ccc9f5004f0236be`. Cópia externa validada em `.servicehub/backups/demo-validated-backup.tar.gz`.

Comparação lógica completa pela API HTTPS antes/depois: 2 clientes,2 equipamentos,3 peças,3 ordens com itens/eventos e movimentações das3 peças. JSON canônico SHA256 idêntico `0c364eff722a5303a6b1f241a4fa9e53e3eb42666d19636d3fd32a38473e6e88`. Persistência confirmada após os reinícios do backup/restore. Nenhum dado privado foi copiado para demo ou publicado.

## Operação e limites

Use os procedimentos de atualização, backup e rollback em [PUBLIC-DEMO.md](PUBLIC-DEMO.md), sempre selecionando `SERVICEHUB_INSTANCE=demo`. Backups locais da EC2 não substituem a cópia externa. Mantenha credenciais e certificados fora do Git. TúnelHTTP8083 não oferece acesso à demonstração; preserve os cookies Secure e use HTTPS público ou túnelTLS documentado.

O Free Plan foi preservado. OrçamentoUS$10 é alerta, não trava; operação contínua e tráfego podem consumi-lo. Sem teste de carga, sem alta disponibilidade e sem prova de renovação futura do certificado. IP pode mudar após parar/iniciar a EC2; nesse caso conferir DNS. Em falha bloqueante, fechar80/443, parar/desabilitar Caddy/demo e reativar serviço privado com seus arquivos preservados.

Após o fluxo ADMIN, novo snapshot e reinício explícito do Java confirmaram dados completos idênticos: 3 clientes,3 equipamentos,3 peças,4 ordens, respectivos itens/eventos/movimentações. SHA256 lógico antes/depois `0a30d5f0fd18fe042410d85c92bfe2202bf26538fa036121a13b9f63f157b207`. A ordem #4 permanece concluída e a baixa de estoque persistiu.

Capturas reais: [login público](screenshots/public-login.png), [fluxo administrativo](screenshots/public-admin-completed.png), [visitante](screenshots/public-visitor.png).

Visitante também validado no Chrome desktop e viewport390×844: navegação e abertura da ordem/histórico em consulta, sem controles de gravação. [Captura móvel](screenshots/public-visitor-mobile.png). Pendência visual não bloqueante: a coluna de descrição no [orçamento móvel](screenshots/public-order-mobile.png) quebra textos longos em linhas muito estreitas; melhorar em mudança posterior. Nenhuma alteração visual foi feita durante a publicação.
