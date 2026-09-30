# Evidências de implantação AWS — 28/09/2026

> Evidências históricas da instalação privada. A preparação seguinte está em [PUBLIC-DEMO-VALIDACAO.md](PUBLIC-DEMO-VALIDACAO.md). A entrega 1.2.0 não modificou nem revalidou a EC2.

## Estado inicial

- Repositório clonado de `Gmerick/java-servicehub`, main `23532e1`, versão Maven 1.1.0, Java 17, Spring Boot 4.0.7.
- Pasta de trabalho inicial vazia, Git sem commits; clone em subpasta para preservar esse repositório. Nenhum AGENTS.md encontrado no repositório ou nos diretórios pais inspecionados.
- Branch `codex/aws-servicehub-dev`. Nenhuma mudança de HTML, CSS, JavaScript ou regras de negócio.
- CI existente executa testes Java, persistência, Playwright e empacotamento; release manual restrita à main. Novo artefato EC2 inclui JAR/checksum e scripts; nenhum deploy automático ou segredo AWS foi adicionado à CI.

## AWS realmente inspecionada

- Sessão existente do Chrome acessível; projeto exibido: Next Big Thing.
- Settings → Faturamento: plano Gratuito, US$ 100,00 restantes, US$ 0,00 devidos, 182 dias com término 28/03/2027. O widget EC2 exibiu 181 dias; registra-se a diferença entre telas sem interpretar como crédito mensal.
- Cost Explorer: custo exibido no mês US$ 0,00, excluindo créditos e reembolsos; sujeito a atraso de contabilização.
- `us-east-1`: console exibiu “Região Estados Unidos (Norte da Virgínia) indisponível” e exigiu “Ative recursos avançados”. Consultas EC2 retornaram acesso negado. Nenhuma ativação de recursos avançados ou alteração de plano realizada.
- Ohio (`us-east-2`): inventário anterior à criação confirmou nenhuma instância, volume ou chave; apenas o security group padrão. Provisionamento concluído sem ativar recursos avançados ou mudar o Free Plan.
- AWS Budgets não tinha orçamento. Criado e confirmado no console: `servicehub-dev-monthly-10-usd`, US$ 10 mensais recorrentes desde setembro/2026, custos não combinados, exclusão `Credit` e `Refund`, alertas ACTUAL acima de 50%, 80%, 100%, destinatário informado pelo usuário. Nenhuma ação automática associada. A entrega de e-mails ainda não foi testada.
- Criados uma EC2, um volume raiz, um par de chaves e um security group exclusivos, além do orçamento. VPC/subnet padrão reutilizadas; nenhuma IAM role, Elastic IP ou infraestrutura adicional.

## Testes executados localmente

| Verificação | Resultado real |
|---|---|
| Maven `clean verify` na base | 14 testes, zero falhas/erros/ignorados; build bem-sucedido |
| Persistência na base | Falhou: última peça não reapareceu após término abrupto do processo no Windows |
| Correção | URL padrão H2 passou a usar `WRITE_DELAY=0`; mesma configuração nos scripts EC2 |
| Maven `verify` após correção | 14 testes, zero falhas/erros/ignorados; JAR reconstruído |
| `python scripts/check_persistence.py` após correção | PASS: cliente, equipamento e peça preservados após reinício real do JAR |
| Playwright com Chrome local após correção | 8/8 passaram (23,5 s): ciclo completo, validação/duplicidade, cancelamento/estoque, erro de rede, CSV/reposição, desktop/móvel, injeção HTML e movimento reduzido |
| `bash -n` | Sintaxe dos três scripts de instalação, backup e atualização aprovada |
| `git diff --check` | Sem erros |

JAR: `target/app.jar`, SHA256 `d295dca4bc896f1927068a9671a15389d7c8eebf4fd6dda97fff41e7387574a7`.
Relatórios locais: `target/surefire-reports/`, `target/build-aws.log`, `playwright-report/` (ignorados pelo Git). As capturas geradas pelos testes foram revertidas para não trocar as imagens da documentação existente.


## Implantação e validação real em Ohio

- EC2 `i-0d26611f9832a61c9`, nome `servicehub-dev`, IPv4 `18.224.63.142`, privada `172.31.14.16`, criada em 28/09/2026 às 23:01 UTC.
- Amazon Linux 2023 x86_64, AMI `ami-08be4b1b8afa29958`; t3.micro, CPU Standard, IMDSv2 obrigatório, sem monitoramento detalhado.
- EBS `vol-092b312ad13415b8a`: gp3 8 GiB, 3000 IOPS, 125 MiB/s, criptografado, DeleteOnTermination habilitado. Encerrar a instância exclui esse banco e os backups locais.
- SG `sg-0a68721ee7dc69a9d` (`servicehub-dev-ssh`): único ingresso TCP 22 de `201.74.182.206/32`, IP do computador confirmado pelo console. Nenhuma entrada 8083; egresso padrão.
- Chave ED25519 `servicehub-dev`, arquivo privado somente no computador, fora do Git e com ACL restrita. Host SSH conferido com o log do console: `SHA256:+Y7HhMFwRkUZqG6FG/4nfL0BTiAZCBClyAk+1FjE25Y`.
- Preço Ohio conferido no console EC2 e nas páginas oficiais EBS/IPv4: aproximadamente US$ 11,88 por 730 horas, antes de créditos e extras. US$ 10 é alerta antecipado, não limite de gasto.

### CI e artefato implantado

[CI manual 36495202552](https://github.com/Gmerick/java-servicehub/actions/runs/36495202552), branch `codex/aws-servicehub-dev`, commit `f7057e2`: **success**, concluída às 22:56:58 UTC em 28/09/2026. Foram aprovados 14 testes Java, persistência, 8 testes de interface, sintaxe Bash e empacotamento. Esta é a primeira implantação; atualizações posteriores estão registradas abaixo.

Artefato `ServiceHub-EC2` baixado dessa execução; SHA256 do JAR implantado: `56f8237ea2b30904889cab5bba14255f9be0575c43dcda1e57520cde42473a75`. Nenhuma compilação na EC2. A verificação em `update.sh` limita conexão a 2 s, chamada a 5 s e sondagem total a 120 s, mais 2 s de tolerância para encerramento forçado. Caminho de sucesso executado; expiração negativa completa não simulada.

### Operação, rede e dados

| Verificação executada | Resultado |
|---|---|
| systemd após instalação e restauração | active e enabled; Restart=on-failure |
| Identidade e permissões | usuário servicehub UID/GID 993, sem login ou grupo administrativo; dados 0700, banco 0600; JAR root |
| Escuta remota | Java somente `[::ffff:127.0.0.1]:8083` (loopback IPv4); SSH em 22 |
| Logs | Corretto 17, aplicação 1.1.0, Hikari no arquivo absoluto, inicialização normal em aproximadamente 55 s |
| Túnel no computador do usuário | SSH local 127.0.0.1:8083 → EC2 127.0.0.1:8083; health UP |
| Conexão pública a 8083 | não conectou no teste TCP de 5 s; ausência de regra confirmada no SG |
| Playwright contra EC2 pelo túnel | percurso completo aprovado, 1/1, 23,1 s |
| Fluxo fictício | cliente, notebook, OS 1, mão de obra 150, SSD 249,90; aprovação, execução, conclusão; total 399,90 e estoque de 8 para 7 |
| Reinício real | OS continuou COMPLETED e estoque 7 após systemctl restart |
| Backup frio real | serviço parado pelo script, arquivo fechado, SHA256 validado e reinício bem-sucedido |
| Restauração real | criado cliente marcador após backup; estado atual preservado antes da restauração; backup restaurado com serviço parado; marcador ausente e OS/estoque preservados após início |

Backup validado: `/var/backups/servicehub/h2-20260928T230852-26583.tar.gz`.
Cópia externa no computador: `C:\Users\SUPORTE\Documents\ServiceHub-backups\servicehub-backup-20260928.tar.gz`.
Ambas com SHA256 `d3c8a278b8f5472a57f835c8dc940a7182e1ca02a68b8d8f43b73c9be9fbcba2`.
Estado imediatamente anterior à restauração preservado em `/var/backups/servicehub/before-restore-20260928.tar.gz`, com checksum. Os dados fictícios de validação permanecem na aplicação.

## Limitações registradas

- Entrega dos alertas por e-mail não testada; orçamento não desliga recursos e contabilização pode atrasar.
- A limitação inicial de versão fixa no health foi corrigida na atualização documentada abaixo.
- IPv4 público automático e IP de origem podem mudar; atualize os comandos e o /32 conforme necessário.
- Nenhum merge na main, mudança de plano ou recurso fora do escopo realizado.


## Atualização final: versão Maven e reconexão — 28/09/2026

- Código aprovado: `4b9866b`. [CI manual 36497408914](https://github.com/Gmerick/java-servicehub/actions/runs/36497408914): success; 14 testes Java, versão HTTP igual ao metadado do JAR e ao POM, persistência após reinício, 8 testes de interface (17,5 s), sintaxe Bash e artefatos.
- A primeira execução `36497268911` falhou no teste por procurar o metadado na pasta errada do JAR. Corrigido para `META-INF/build-info.properties`; teste local e nova CI aprovados. Nenhum artefato reprovado foi implantado.
- Artefato ServiceHub-EC2 baixado da CI aprovada. SHA256 local, do manifesto e do JAR remoto iguais: `3a8ba544ec31ceb408405bef8a3b7ff05f1499d66f101330f9fd962e6bf662f8`.
- Backup frio prévio: `/var/backups/servicehub/h2-20260928T232015-27267.tar.gz`; checksum conferido e cópia externa em `C:\Users\SUPORTE\Documents\ServiceHub-backups\health-update-backup.tar.gz`. SHA256 das duas cópias: `0803551af54e330647d882ad4583f79752d303fee3aa60a7efde0d1ddcd966c1`.
- Atualização pelo `servicehub-update` existente, com backup adicional `/var/backups/servicehub/pre-update-20260928T232212-27495.tar.gz` e JAR anterior preservado. Sem alteração de infraestrutura ou plano.
- Após atualização: health `{"status":"UP","version":"1.1.0"}` igual ao metadado extraído do artefato da CI; JAR remoto com hash correspondente; systemd active, User=servicehub e NoNewPrivileges=yes. Logs mostram inicialização normal em 6,3 s.
- Comparação integral das respostas antes/depois: clientes, equipamentos, peças, lista de ordens, detalhe da OS 1 (incluindo itens/eventos) e movimentos da peça 1 idênticos. OS concluída e estoque 7 preservados. Nenhum dado fictício adicional criado nesta atualização.
- Escuta novamente confirmada somente em loopback 8083; teste TCP público de 5 s não conectou. Regras de rede não foram alteradas.
- `deploy/aws/open-tunnel.ps1` executado neste computador com LocalPort 18083: túnel abriu e respondeu ao health; usado também na comparação após atualização. Túnel original 8083 permanece disponível. Nenhuma chave privada incorporada ao script ou ao Git.
- Reconexão documentada em AWS.md. Reinício físico do computador não executado. Entrega dos alertas de orçamento por e-mail segue não testada. CI apresenta avisos de depreciação das actions v4; não impediram os testes.

## Revisão técnica do PR #1 — 29/09/2026

Diff completo revisado, arquivo por arquivo, incluindo os arquivos relacionados de empacotamento e release:

| Arquivos | Conclusão / correção |
|---|---|
| `.gitattributes`, `.gitignore`, `README.md` | LF nos scripts; chaves/bancos ignorados; referência de implantação correta. Busca no conteúdo rastreado não encontrou chaves privadas, tokens GitHub ou padrões de access keys AWS. |
| `pom.xml`, `HubController.java` | Maven é fonte da versão de aplicação; build-info gera metadado, health usa BuildProperties. Nenhum literal alternativo no endpoint. |
| `application.properties`, `servicehub.service` | H2 em arquivo; EC2 usa diretório separado do JAR. Loopback imposto por argumento CLI, usuário sem privilégios, escrita restrita e reinício limitado. `schema.sql` revisado: CREATE IF NOT EXISTS, sem DROP. |
| `install.sh`, `backup.sh`, `update.sh`, novo `common.sh` | Instalação da biblioteca comum e restore; checksum do JAR conferido também após copiar; atualização aborta com banco anterior ausente; backup frio com integridade gzip e checksum; health exige JSON UP e versão igual ao JAR, mantendo limites de tempo. |
| Novo `restore.sh` | Substitui receita manual sem fail-fast/trava: verifica hash, conteúdo e tipo do único membro, extrai em staging privado, preserva estado atual, usa troca por rename e falha de forma explícita. |
| `open-tunnel.ps1` | IP retirado do código, parâmetro/env obrigatório; validação de endereço e porta ocupada; BatchMode/IdentitiesOnly e limites de conexão; verificação de host mantida. IPs em documentação são evidências/exemplos da instalação, não credenciais. |
| `check_persistence.py`, `package.py` | Teste isola variáveis DB_URL/DB_PASSWORD herdadas e verifica versão também após reinício. ZIP deriva versão dos metadados, eliminando literal adicional. |
| Workflows CI/release | Release deriva tag/título do Maven. Actions oficiais fixadas por SHA; checkout/setup-java/setup-node v5 e upload-artifact v6; Ubuntu 24.04. Testes falham o job, sem continue-on-error. Publicação da release continua manual, só na main. |
| `docs/AWS.md`, este registro | Restauração e reconexão atualizadas; corrigida descrição histórica que dizia que todas as mudanças posteriores eram documentais. Limitações de proteção de branch e validação semântica explicitadas. |

### Testes e operação

- CI [36559627504](https://github.com/Gmerick/java-servicehub/actions/runs/36559627504), código `702e266`: aprovada. 14 testes Java, versão e persistência do JAR real, 8 testes de interface (21,2 s), empacotamento e testes novos de manutenção.
- `scripts/check_deploy.py` executa cópias dos scripts em diretórios temporários Linux; adapta caminhos, privilégios e prazo para 1 s, simula systemctl/curl, usa tar/SHA256/flock/timeout reais. Aprovados: backup, restauração e preservação do estado anterior, rejeição de checksum/caminhos/links, banco ausente, atualização sem alteração dos dados, versão incorreta encerrando por prazo e deixando serviço parado. Não equivale a novo teste de restauração H2 completo na EC2.
- Teste de persistência local com DB_URL herdado propositalmente divergente: aprovado, sem usar esse caminho; versão 1.1.0 antes/depois do reinício.
- Túnel revisado: endereço ausente, chave ausente e porta ocupada retornam erros claros; conexão real em 18084 respondeu UP/1.1.0. Túnel temporário encerrado; original 8083 preservado.
- Console Ohio: uma EC2 existente, running, 3/3 checks. SG `sg-0a68721ee7dc69a9d`: somente TCP22 de `201.74.182.206/32`; nenhuma entrada 8083. Novo teste público TCP8083 não conectou. SSH/ss confirmam Java em loopback; servicehub sem privilégios, NoNewPrivileges=yes; dados 0700/0600.
- Scripts de manutenção dessa CI instalados na EC2. Backup real novo `/var/backups/servicehub/h2-20260929T110845-49305.tar.gz`: checksum aprovado, retorno zero após saúde, todos os registros capturados antes/depois idênticos. Restore com checksum incorreto rejeitado antes de parar o serviço (Python 3.9 da EC2), que permaneceu active.
- JAR existente não substituído nesta revisão; SHA256 continua `3a8ba544ec31ceb408405bef8a3b7ff05f1499d66f101330f9fd962e6bf662f8`, health UP/1.1.0. Nenhuma mudança de infraestrutura, rede, plano ou capacidade de armazenamento.
- CI intermediária detectou codificação Windows inválida e depois requisito de ownership do teste em sandbox sem root; corrigidos antes da execução aprovada. O upload v5 ainda declarou Node20; trocado por v6 cujo action.yml declara Node24. Warnings internos de dependências das actions e API deprecated nos testes não foram suprimidos.

### Antes do merge

A API retornou `Branch not protected` para main e nenhum ruleset. O check `validate` precisa ser exigido em proteção de branch/ruleset para impedir merge com CI vermelha; essa configuração de governança não foi alterada. Confira a CI do commit final no PR. O novo restore tem testes de controle de falha em sandbox; a restauração H2 real documentada anteriormente foi feita pelo procedimento anterior. Restauração exige backup/JAR compatíveis e conferência dos registros; checksum não prova correção de negócio. Alertas por e-mail e reinício físico do computador seguem não testados. Nenhuma release publicada e nenhum merge realizado.

## Preparação final para merge — 29/09/2026

Esta seção atualiza as pendências de proteção e teste do novo restore mencionadas na revisão anterior.

- HEAD inicial confirmado: `447c8f9b051fffd8ad89fa93db8af4bb534e108d`, PR #1 OPEN, check `validate` SUCCESS. A única alteração subsequente deste passo é documental.
- Permissão administrativa confirmada pela API. Proteção clássica da `main` criada e relida: PR obrigatório, `validate` obrigatório vinculado ao GitHub Actions (app 15368), strict=true, enforce_admins=true, 0 aprovações obrigatórias, sem code owners/último push obrigatório, force-push=false e deletions=false. `allow_auto_merge=false` e PR sem autoMergeRequest. `gh pr checks 1 --required` confirmou validate/pass. Nenhum merge realizado.
- Checksum inválido novamente rejeitado pelo restore instalado antes de parar o serviço: MainPID e ExecMainStartTimestampMonotonic permaneceram iguais, serviço active.
- Teste válido controlado do novo restore executado na EC2: serviço explicitamente parado, sem timers/triggers; backup novo e fingerprint obtidos antes de qualquer reinício. O procedimento manteve trava durante preparação e transferiu execução ao restore instalado, que adquiriu sua própria trava. Nenhum backup antigo foi usado para restaurar.
- Backup novo: `/var/backups/servicehub/premerge-20260929T111949-50055.tar.gz`, SHA256 `f5356cba20e6f223d7c2bc76588d74a93cdb989e69ac44a67438bfd1de5b9ea6`. Hash do banco parado igual ao membro dentro do backup: `cb081e8c85d72c2ac39812a7909888bc0a07e7bb3dd9c1d87191b4d3197be0b3`.
- O restore preservou o estado anterior em `/var/backups/servicehub/before-restore-20260929T111950-50055.tar.gz`. Seu banco também tem exatamente o hash físico acima: comprova que não havia dados mais novos sendo substituídos.
- Restore retornou zero e confirmou saúde. Backup posterior consistente `/var/backups/servicehub/h2-20260929T112037-50288.tar.gz` usado para comparação offline. A biblioteca H2 do próprio JAR exportou snapshots privados com SCRIPT SIMPLE NOPASSWORDS NOSETTINGS. Nenhum dump de dados foi publicado no Git.
- Antes/depois: CUSTOMERS=1, ASSETS=1, PARTS=1, WORK_ORDERS=1, ORDER_ITEMS=2, ORDER_EVENTS=6, STOCK_MOVEMENTS=2. Hash SHA256 do dump lógico completo (sem comentários) idêntico: `3d1851ed18cc72bf2ede18b822dabad07f84cf93d05364cd5380976fd06ff1a0`. Conteúdo e contagens preservados, sem perda de dados. O hash físico após reabrir H2 pode mudar; a comparação lógica não depende desses metadados internos.
- Cópia externa do backup novo: `C:\Users\SUPORTE\Documents\ServiceHub-backups\premerge-backup-20260929.tar.gz`, checksum idêntico ao original validado.
- EC2 active em Ohio, health UP/1.1.0, User=servicehub, NoNewPrivileges=yes. JAR inalterado, SHA256 `3a8ba544ec31ceb408405bef8a3b7ff05f1499d66f101330f9fd962e6bf662f8`. Java somente em loopback 8083; novo teste público não conectou. Nenhuma regra de rede, infraestrutura, plano ou capacidade alterada.
- Documentação atualizada; CI do novo HEAD deve permanecer verde e seu resultado final fica vinculado ao PR. Não houve mudança de versão, frontend, arquitetura ou regra de negócio. Proteção não foi testada com tentativa destrutiva de push/delete: foi confirmada pela API e pelo check obrigatório.
- Pendências não bloqueantes: entrega de alertas de orçamento por e-mail e reinício físico do computador não testados. Permanecem cuidados operacionais com mudança de IP, backup externo e compatibilidade do JAR em futuras restaurações. Nenhuma configuração manual de proteção ficou pendente; aguarda revisão humana e autorização explícita para merge.
