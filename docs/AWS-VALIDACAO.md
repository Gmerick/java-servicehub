# Evidências de implantação AWS — 28/09/2026

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

[CI manual 36495202552](https://github.com/Gmerick/java-servicehub/actions/runs/36495202552), branch `codex/aws-servicehub-dev`, commit `f7057e2`: **success**, concluída às 22:56:58 UTC em 28/09/2026. Foram aprovados 14 testes Java, persistência, 8 testes de interface, sintaxe Bash e empacotamento. Mudanças posteriores neste registro são documentais.

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
