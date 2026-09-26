# Estudo e apresentação

## Ordem de leitura

1. Execute e faça o percurso descrito no README; observe o estoque antes/depois.
2. Leia `schema.sql` e explique os relacionamentos e restrições.
3. Leia `Model.java` e identifique os campos, enums e validações.
4. Siga uma requisição de `HubController` para `HubService` e `HubRepository`.
5. Estude a transição de status: transação, bloqueio, soma de quantidades, baixa e rollback.
6. Leia `ServiceHubTest`: compare teste de concorrência entre duas ordens com aprovação repetida da mesma ordem.
7. Leia `app.js`: requisições, feedback, escape de texto e atualização da tela.
8. Execute Playwright e consulte `.github/workflows/ci.yml` e `release.yml`.

## Apresentação em 5–10 minutos

- **0–1 min:** problema de uma assistência técnica: evitar orçamento perdido, estoque inconsistente e atendimento sem histórico.
- **1–4 min:** painel, cadastro de equipamento, rascunho com peça/serviço, aprovação e conclusão.
- **4–6 min:** mostre o cancelamento com devolução e a mensagem de estoque insuficiente.
- **6–8 min:** abra a regra de transição e explique transação e bloqueio. Mostre o teste que prova que duas ordens não consomem a última peça.
- **8–10 min:** mostre CI verde, pacote gerado, guia de instalação e limites do produto.

## Perguntas para praticar

**Por que BigDecimal?** Valores monetários exigem precisão decimal; o total usa quantidade vezes preço do item.

**O que acontece se a segunda peça não tiver saldo?** A exceção reverte a transação inteira, sem baixa parcial.

**Como evita clique duplo?** O botão é desativado durante o envio, mas a proteção decisiva fica no servidor: bloqueio da ordem e validação da etapa atual.

**Por que H2?** Facilita demonstração local persistente. PostgreSQL e migrações seriam uma evolução para implantação multiusuário.

**Está pronto para internet?** O escopo entregue é local, sem autenticação. Publicar o código no GitHub não equivale a hospedar a aplicação para usuários externos.

## Exercícios de compreensão

- Acrescente um teste para uma regra antes de mudá-la.
- Explique por que o preço enviado pelo navegador não define o preço de uma peça.
- Localize a defesa de fórmula no CSV e o escape de HTML na interface.
- Modifique um texto ou cor e confira desktop e celular.

Apresente apenas o que você consegue demonstrar e explicar. O projeto recebeu apoio de IA; revisar, executar e compreender as decisões faz parte do uso responsável do portfólio.
