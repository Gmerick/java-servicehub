# ServiceHub · Gestão de assistência técnica

[![CI](https://github.com/Gmerick/java-servicehub/actions/workflows/ci.yml/badge.svg)](https://github.com/Gmerick/java-servicehub/actions/workflows/ci.yml)

Aplicação de portfólio em **Java 17 + Spring Boot**, com interface web em português, banco persistente e fluxo de ordens de serviço. Organiza clientes, equipamentos, orçamento, execução e estoque em uma aplicação local.

![Painel ServiceHub](docs/screenshots/desktop.png)

[Ver a interface no celular](docs/screenshots/mobile.png). Capturas reais geradas pelos testes de navegador.

## O que é possível fazer

- Cadastrar clientes, equipamentos e peças; consultar os cadastros pela interface.
- Abrir ordens, incluir serviços e peças, acompanhar prazo e prioridade.
- Aprovar o orçamento, iniciar execução, concluir com resolução ou cancelar com justificativa.
- Baixar estoque ao aprovar e devolver ao cancelar uma ordem aprovada/em execução.
- Repor peças e consultar movimentações; visualizar alertas de estoque mínimo.
- Filtrar e paginar ordens, exportar CSV e acompanhar indicadores no painel.
- Usar a interface em desktop ou celular, com estados vazios, mensagens de erro e recuperação.

O valor das ordens concluídas representa **orçamentos concluídos**, não recebimentos financeiros. A versão 1.2.0 inclui login por sessão, ADMIN e VISITANTE com autorização no backend, CSRF e demonstração em banco separado. Não inclui pagamentos ou notas fiscais. A publicação HTTPS está preparada, mas depende de domínio e corte autorizado; a instalação AWS anterior permanece privada.

[Login desktop](docs/screenshots/demo-login-desktop.png) · [Login móvel](docs/screenshots/demo-login-mobile.png) · [Consulta de visitante](docs/screenshots/demo-visitor-desktop.png) · [Visitante móvel](docs/screenshots/demo-visitor-mobile.png)

## Executar em poucos minutos

Requisitos de desenvolvimento: **JDK 17+ e Maven 3.6.3+**. Node é necessário somente para os testes de navegador.

```bash
git clone https://github.com/Gmerick/java-servicehub.git
cd java-servicehub
mvn clean verify
python scripts/create_admin.py target/app.jar /caminho/privado/admin.secrets.properties
java -jar target/app.jar --spring.profiles.active=demo --spring.config.additional-location=file:/caminho/privado/admin.secrets.properties
```

Substitua o caminho por uma pasta privada fora do repositório; o gerador pede a senha no terminal e exige Python 3 + JDK 17. Abra **http://localhost:8083** e escolha visitante ou entre como ADMIN. O perfil `demo` cria dados fictícios em `data-demo`, separados do banco `data` existente. Sem esse perfil, não há visitante nem carga fictícia. Feche com `Ctrl+C`. No Windows, `INICIAR-DESENVOLVIMENTO.cmd` usa o perfil padrão e exige a variável `SERVICEHUB_ADMIN_FILE` apontando para o arquivo privado. Nunca configure senhas em arquivos Git.

**Pacote pronto:** em [Actions → CI](https://github.com/Gmerick/java-servicehub/actions/workflows/ci.yml), baixe `ServiceHub-Windows` de uma execução verde e extraia o ZIP interno da versão Maven. Siga COMO-USAR.md para criar as credenciais externas antes de executar `INICIAR.cmd`. A execução requer Java17; a geração local inicial do hash requer JDK17 e Python3. Maven e Node não são necessários no pacote. Releases continuam sendo manuais.

## Demonstração em 5 minutos

1. Entre como ADMIN para demonstrar operações, ou como visitante para consultar. Explore o painel e abra **Ordens de serviço**.
2. Abra a ordem de upgrade, confira peça + mão de obra e o total.
3. Selecione **Aprovada**, escreva uma observação e confirme; confira a redução do SSD em estoque.
4. Retorne à ordem, avance para **Em execução** e depois **Concluída**, registrando a resolução.
5. Abra outra ordem com peça e demonstre o cancelamento após aprovação: estoque retorna uma única vez.
6. Exporte o CSV e confira o histórico da ordem.

## Qualidade e regras

Os valores usam `BigDecimal` e `DECIMAL`. Operações de orçamento e estoque têm transações; bloqueios por ordem e peça impedem aprovação duplicada e venda acima do estoque. Chaves estrangeiras, validação de entrada e restrições no banco protegem os relacionamentos. O orçamento fica imutável após a aprovação.

Há testes HTTP de integração cobrindo o ciclo completo, rollback, concorrência real, validação, CSV e restrições de origem. Os testes Playwright percorrem a interface real, erros recuperáveis e layout móvel. A CI executa ambas as suítes e empacota o JAR. O workflow de release repete as verificações antes de publicar uma versão.

```bash
mvn clean verify
npm ci
npx playwright install --with-deps chromium
npm run test:ui
python scripts/package.py
python scripts/check_persistence.py
```

## Documentação

- [Autenticação, demonstração separada e plano de publicação HTTPS](docs/PUBLIC-DEMO.md)
- [Evidências e limites da validação 1.2.0](docs/PUBLIC-DEMO-VALIDACAO.md)

- [Implantação de desenvolvimento na AWS, túnel SSH e operação do H2](docs/AWS.md)
- [Como usar, instalar, fazer backup e resolver problemas](docs/COMO-USAR.md)
- [Arquitetura, regras e API com exemplos](docs/ARQUITETURA.md)
- [Roteiro de estudo e apresentação em entrevista](docs/ESTUDO.md)
- [Escopo e critérios de aceite](docs/CONTRATO.md)
- [Evidências e limitações da validação](docs/VALIDACAO.md)

## Estrutura

```text
src/main/java/io/github/gmerick/servicehub/  API, regras, persistência
src/main/resources/static/                Interface HTML/CSS/JavaScript
src/main/resources/schema.sql             Esquema relacional H2
src/test/                                 Testes Java via HTTP
e2e/                                      Testes de navegador
scripts/                                  Inicialização e distribuição
.github/workflows/                        CI e release
```

Licença MIT. Projeto desenvolvido com apoio de IA; o roteiro de estudo orienta a revisão do código e a explicação das decisões técnicas.

## Visual 1.1

Bancada com navegação horizontal, atendimentos em linhas abertas e estoque em uma coluna assimétrica. Azul gelo, lilás e ciano em superfícies leves; transições de tela e modal, respostas ao cursor e um detalhe orbital animado. A preferência de movimento reduzido do sistema desativa as animações. Veja as instruções de atualização preservando a pasta `data` em [Como usar](docs/COMO-USAR.md).
