# Como usar o ServiceHub

## Instalação no Windows

1. Tenha Java 17 ou superior instalado. No terminal, `java -version` deve funcionar.
2. Baixe o pacote gerado pela CI do repositório. Extraia os dois ZIPs, quando houver o ZIP externo de artefato do GitHub.
3. Dentro da pasta `ServiceHub`, dê dois cliques em `INICIAR.cmd`.
4. Espere a mensagem `Started ServiceHubApplication` e abra http://localhost:8083.
5. Mantenha o terminal aberto enquanto utiliza o sistema. Para parar, pressione `Ctrl+C`.

Para desenvolver, instale também JDK e Maven, clone o repositório e execute `INICIAR-DESENVOLVIMENTO.cmd`. O script reconhece ferramentas na pasta `%USERPROFILE%\ProjetosJavaJr\ferramentas` do kit anterior ou no PATH.

No Linux/macOS: `mvn clean verify` e `java -jar target/app.jar`. Execute os comandos a partir da raiz do projeto.

## Primeiro atendimento

**Clientes:** clique em Novo cliente e informe nome, e-mail e telefone. O e-mail é único.

**Equipamentos:** clique em Novo equipamento, escolha o cliente e informe nome e série. Uma série não pode se repetir.

**Peças e estoque:** cadastre peça, SKU, preço, saldo inicial e mínimo. O SKU é único. A ação Repor registra quantidade e motivo; o histórico identifica as movimentações.

**Ordens de serviço:** clique em Nova ordem. Escolha equipamento, título, descrição, prioridade e prazo de hoje em diante. A ordem começa em rascunho.

**Orçamento:** adicione serviços digitando descrição e valor, ou selecione uma peça do catálogo. Para peças, o servidor utiliza o preço do catálogo. A quantidade deve ser positiva. Você pode remover itens enquanto a ordem está em rascunho.

**Aprovação:** selecione Aprovada e explique a mudança. É necessário pelo menos um item; peças precisam ter saldo suficiente. Se uma peça faltar, nenhuma baixa parcial será mantida.

**Execução e entrega:** avance para Em execução. Depois selecione Concluída e registre o que foi resolvido. Essa informação aparece no encerramento.

**Cancelamento:** permitido antes da conclusão. Um rascunho não altera o estoque. Uma ordem aprovada ou em execução devolve as peças reservadas; repetir a operação é recusado. Ordens concluídas/canceladas são finais.

## Consulta e exportação

No painel, veja ordens abertas, atrasadas, concluídas e valor dos orçamentos concluídos. Atraso significa prazo anterior ao dia atual e ordem ainda aberta. O painel também mostra peças no mínimo ou abaixo dele.

Na lista de ordens, busque título, cliente ou ID exato, selecione status e clique Filtrar. A exportação CSV inclui todas as ordens, independentemente do filtro atual. Abra no Excel/LibreOffice usando UTF-8 e separador ponto e vírgula. Textos que poderiam virar fórmulas são neutralizados.

## Dados, backup e restauração

O banco fica em `data/servicehub.mv.db`, relativo à pasta em que a aplicação é iniciada. Os scripts sempre mudam para a pasta correta. Não execute duas instâncias sobre o mesmo arquivo.

Para backup: pare a aplicação e copie a pasta `data` para um local seguro. Para restaurar: pare a aplicação, preserve uma cópia do banco atual e substitua `data` pela cópia desejada. Reinicie. Não envie bancos com dados pessoais ao GitHub; `data/` está ignorada pelo Git.

Para iniciar uma base vazia, use uma pasta de dados nova e:

```bash
java -jar target/app.jar --app.demo=false --spring.datasource.url=jdbc:h2:file:./data-vazia/servicehub
```

Desativar a demonstração não apaga os registros existentes. Para recomeçar a demonstração, use outra pasta de banco. Não é necessário excluir seu banco atual.

## Configuração

| Variável | Padrão | Uso |
|---|---|---|
| PORT | 8083 | Porta HTTP |
| SERVER_ADDRESS | 127.0.0.1 | Interface de rede local |
| DB_URL | jdbc:h2:file:./data/servicehub | Endereço do banco H2 |
| DB_PASSWORD | vazio | Senha do banco |
| APP_DEMO | true | Carga fictícia se não houver clientes |

Também é possível usar argumentos Spring, por exemplo `--server.port=8084`. Nesse caso abra http://localhost:8084. A aplicação não tem login e foi concebida para uso local; a implantação multiusuário requer autenticação, autorização, HTTPS e revisão operacional.

## Problemas frequentes

- **Java não encontrado:** configure Java 17+ no PATH e reabra o terminal.
- **UnsupportedClassVersionError:** a versão de Java é antiga; confira `java -version`.
- **Porta ocupada:** feche a instância anterior ou use `--server.port=8084`.
- **Banco em uso:** encerre outra instância que esteja usando a mesma pasta `data`.
- **Duplicidade:** use e-mail, série ou SKU ainda não cadastrado.
- **Estoque insuficiente:** reponha a peça com motivo ou ajuste o rascunho antes de aprovar.
- **Servidor indisponível:** confira o terminal e clique Tentar novamente após reiniciar o servidor.
- **Maven não baixa dependências:** confira a conexão/proxy e repita a compilação. A primeira execução precisa de internet.

O executável `.cmd` foi revisado, mas sua execução precisa ser confirmada em um PC Windows. O JAR, os fluxos e o pacote são validados no Linux.

## Atualizar da versão 1.0 para a 1.1

Pare o programa e copie a pasta `data` para backup. Extraia a nova distribuição em outra pasta e copie `data` para dentro da nova pasta `ServiceHub`, ao lado de `app.jar`. Inicie a nova versão. As regras e o banco são compatíveis; não execute as duas versões simultaneamente.

A visão geral agora funciona como uma bancada: atendimentos recentes à esquerda e estoque/atalhos à direita. No celular, os blocos seguem uma coluna e a navegação pode ser deslizada horizontalmente. Ative “reduzir movimento” no sistema para desativar as animações.
