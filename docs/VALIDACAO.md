# Validação

## Evidências locais — 26/09/2026

- Java 17.0.20, Spring Boot 4.0.7 e Maven 3.9.16: `mvn verify`, 14 testes, zero falhas/erros.
- Testes HTTP reais: ciclo de atendimento, dinheiro decimal, estoque, rollback, concorrência, transições, referências, duplicidade, filtros, CSV e origem das requisições.
- `python scripts/check_persistence.py`: cliente, equipamento e peça preservados depois de parar e iniciar novamente o JAR com H2 em arquivo.
- `python scripts/package.py`: ZIP e SHA256 gerados, sem banco de dados.
- A execução local do Chrome foi bloqueada pelas permissões de socket deste ambiente. A validação de navegador foi concluída na CI do GitHub, sem remover proteções do ambiente.

## Verificação remota

[Execução aprovada 36278743816](https://github.com/Gmerick/java-servicehub/actions/runs/36278743816), commit `533a98078c5ea9f1a67dfa8aba41d435666cd723`: 14 testes Java, prova de persistência, 7 cenários de navegador e empacotamento aprovados. Capturas de tela e relatórios ficam no artefato `evidencias`; a distribuição fica em `ServiceHub-Windows`.

Os cenários de interface verificam cadastro de cliente/equipamento, orçamento, aprovação, execução e conclusão; duplicidade e recuperação; cancelamento; falha de rede; reposição e CSV; desktop/celular; e texto sem execução de HTML. As capturas aprovadas estão em `docs/screenshots/`.

Correções encontradas durante a validação: acesso ao JDBC pelo proxy Spring, seleção explícita da chave gerada no H2 e identificação acessível dos controles nos testes. Nenhum teste foi removido para obter aprovação. Alterações posteriores de documentação e capturas não mudam o comportamento da aplicação; a CI permanece obrigatória a cada push.

## Limites

Scripts Windows revisados, sem execução nativa em Windows neste ambiente. Testes de navegador usam Chromium; não há certificação para Safari/Firefox. Não foi feita auditoria externa de segurança ou acessibilidade. A aplicação é local e não possui autenticação.

Implementação, validação e autorrevisão realizadas pelo mesmo executor; não houve revisão independente.
