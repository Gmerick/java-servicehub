# Evidências de preparação AWS — 28/09/2026

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
- `us-east-2`: lista EC2 mostrou “Nenhuma instância”. Não foi concluído inventário dos demais tipos de recurso; não presumir ausência de volumes, chaves ou SGs ao retomar.
- AWS Budgets não tinha orçamento. Criado e confirmado no console: `servicehub-dev-monthly-10-usd`, US$ 10 mensais recorrentes desde setembro/2026, custos não combinados, exclusão `Credit` e `Refund`, alertas ACTUAL acima de 50%, 80%, 100%, destinatário informado pelo usuário. Nenhuma ação automática associada. A entrega de e-mails ainda não foi testada.
- **Nenhuma EC2, volume, chave SSH, SG, IAM role ou outro recurso de infraestrutura criado nesta etapa.** Somente o orçamento foi criado.

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

## Pendente — não validado

1. Decisão do proprietário sobre Ohio (`us-east-2`) em lugar de Norte da Virgínia, sem mudança de plano.
2. Conferir inventário restante, preço regional atual e elegibilidade; criar uma única EC2 conforme `AWS.md`.
3. Confirmar IP público do computador do usuário, configurar SSH `/32`, chave fora do repositório e ausência de ingresso 8083.
4. Executar instalação/systemd no Amazon Linux 2023; verificar logs, usuário, permissões, memória e escuta em loopback.
5. Implantar JAR, executar fluxo fictício remoto e verificar dados após reinício do serviço.
6. Validar túnel no computador e teste negativo da porta pública; não há comando com IP real enquanto não existir instância.
7. Executar backup e restauração reais na EC2. Scripts têm sintaxe verificada; isso não equivale a validação operacional Linux.

O trabalho local e o orçamento estão concluídos; **a aplicação ainda não está implantada na AWS**.
