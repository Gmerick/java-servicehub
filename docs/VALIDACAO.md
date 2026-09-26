# Validação

## Evidências locais — 26/09/2026

- Java 17.0.20, Spring Boot 4.0.7 e Maven 3.9.16: `mvn verify`, 14 testes, zero falhas/erros.
- Testes HTTP reais: ciclo de atendimento, dinheiro decimal, estoque, rollback, concorrência, transições, referências, duplicidade, filtros, CSV e origem das requisições.
- `python scripts/check_persistence.py`: cliente, equipamento e peça preservados depois de parar e iniciar novamente o JAR com H2 em arquivo.
- `python scripts/package.py`: ZIP e SHA256 gerados, sem banco de dados.
- A execução local do Chrome foi bloqueada pelas permissões de socket deste ambiente. A validação de navegador será feita na CI do GitHub, sem remover proteções do ambiente.

## Verificação remota

Consulte a execução de CI associada ao commit em [GitHub Actions](https://github.com/Gmerick/java-servicehub/actions). O workflow executa os 14 testes Java, a prova de persistência, 7 cenários de navegador e o empacotamento. Capturas de tela e relatórios ficam no artefato `evidencias`; a distribuição fica em `ServiceHub-Windows`.

## Limites

Scripts Windows revisados, sem execução nativa em Windows neste ambiente. Testes de navegador usam Chromium; não há certificação para Safari/Firefox. Não foi feita auditoria externa de segurança ou acessibilidade. A aplicação é local e não possui autenticação.

Implementação, validação e autorrevisão realizadas pelo mesmo executor; não houve revisão independente.
