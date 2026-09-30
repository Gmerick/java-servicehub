# DNS da demonstração — 30/09/2026

Etapa autorizada: criar somente o DNS e preparar configuração. Nenhum merge, deploy, credencial administrativa, certificado ou abertura de porta foi executado.

## Estado conferido

- PR #2 aberto, HEAD inicial 6285c558d2ad0570742253c27b1eb72f62c7648f, check validate aprovado. Esta atualização acrescenta apenas configuração e documentação à mesma branch.
- EC2 existente servicehub-dev, i-0d26611f9832a61c9, us-east-2 (Ohio), executando. IPv4 público **18.224.63.142**, confirmado no console atualizado e reconfirmado às 01:10 de 30/09/2026 (America/Sao_Paulo). IP atribuído automaticamente, sem Elastic IP e sem IPv6.
- Security Group servicehub-dev-ssh: única entrada TCP 22 de 201.74.182.206/32. Nenhuma entrada 80, 443, 8083 ou 2019. Nenhuma regra foi alterada; conferir o IP atual do proprietário antes de qualquer ajuste futuro do SSH.

## Registro criado

Provedor autoritativo: **CajuHost / CajuHosting**, administrado pelo Zone Editor do cPanel autenticado. Não é Cloudflare; o A aponta diretamente para a EC2, sem proxy.

| Campo | Valor |
|---|---|
| Zona | leadopssender.com.br |
| Nome | servicehub |
| FQDN | servicehub.leadopssender.com.br. |
| Tipo | A |
| Destino | 18.224.63.142 |
| TTL | 300 segundos |

Antes da inclusão, os três autoritativos responderam NXDOMAIN e o filtro do cPanel não encontrou registros para servicehub: não houve substituição de serviço existente. Após salvar, o painel mostrou exatamente o registro acima. A comparação de todas as linhas das duas páginas da zona mostrou **zero remoções/alterações e uma inclusão**: os 139 registros anteriores permanecem, totalizando 140. Domínio principal, www, MX, SPF, DKIM, DMARC e nameservers foram preservados. Nenhum AAAA foi criado.

## Resolução e CAA

Consulta direta DNS às04:09:14 UTC (01:09:14 -03:00), após salvar:

| Servidor consultado | Resposta A |
|---|---|
| ns1.cajuhost.net.br (177.53.142.141), autoritativo | 18.224.63.142, TTL 300, NOERROR/AA |
| ns2.cajuhost.net.br (108.61.203.124), autoritativo | 18.224.63.142, TTL 300, NOERROR/AA |
| ns3.cajuhost.net.br (177.53.142.108), autoritativo | 18.224.63.142, TTL 300, NOERROR/AA |
| Google Public DNS (8.8.8.8) | 18.224.63.142, NOERROR |
| Cloudflare DNS (1.1.1.1), apenas resolvedor | 18.224.63.142, NOERROR |

Propagação confirmada nesses cinco servidores, sem pendência observada neles. Isso não garante que todos os caches de terceiros já expiraram. As consultas AAAA retornaram NOERROR sem resposta: não há IPv6 publicado.

Não foram encontrados CAA no subdomínio ou no domínio base (autoritativos e resolvedores públicos). Também não houve CAA em com.br/br nas consultas aos resolvedores públicos. Nenhuma restrição CAA foi identificada nessa consulta e nenhum CAA foi alterado. Reconsultar antes de emitir o certificado; DNS válido não comprova emissão ACME ou HTTPS.

## Configuração pronta, ainda não instalada

`deploy/public-demo/servicehub.env.example` contém `DEMO_DOMAIN=servicehub.leadopssender.com.br`. O Caddyfile permanece parametrizado. ACME_EMAIL será definido externamente na publicação, após confirmar o contato. Nenhuma senha/hash administrativo foi gerado nesta etapa. `caddy validate` (2.11.4) aprovou a configuração local com esse domínio e um contato `.invalid` exclusivo da validação; nenhum servidor Caddy foi iniciado nem certificado emitido.

## Plano final para a próxima autorização

1. Conferir HEAD/CI do PR #2 e obter aprovação explícita para merge (se desejado), implantação e abertura 80/443. A autorização DNS não inclui essas ações. Confirmar saldo/Free Plan, IPv4/DNS e contato ACME.
2. Gerar credenciais administrativas somente no terminal privado do proprietário, fora do Git/chat, conforme PUBLIC-DEMO.md. Separar o artefato aprovado e seu checksum; não compilar na EC2.
3. Fazer backup frio imediato da instalação privada, validar SHA256, guardar cópia privada fora da EC2 e registrar contagens/hash. Preservar JAR, configuração e banco originais.
4. Preparar servicehub-demo sem privilégios e seus diretórios exclusivos: JAR em /opt/servicehub-demo, H2 em /var/lib/servicehub-demo, backups em /var/backups/servicehub-demo. Não copiar nem publicar o banco privado. Parar/desabilitar o serviço privado no corte e confirmar 8083 livre.
5. Implantar o JAR aprovado pelo procedimento de manutenção selecionando demo. Validar serviço, logs, versão 1.2.0, dados fictícios e Java em 127.0.0.1:8083. Instalar e validar Caddy da origem oficial com o domínio acima e cookies Secure/HttpOnly/SameSite intactos.
6. Somente após aprovação e verificações de autenticação/banco: adicionar TCP 80 e 443 de 0.0.0.0/0 para Caddy. Preservar 22 restrito ao IP público do proprietário/32. Nunca abrir 8083 ou 2019; não adicionar IPv6/AAAA. Não criar EC2, EIP, balanceador ou serviço pago adicional.
7. Emitir certificado, confirmar redirecionamento HTTPS, login/visitante/ADMIN/CSRF/logout, cookies, túnel TLS e persistência. Repetir backup/restore real da demo atual com preservação de estado e conferir integridade do banco privado parado. Registrar evidências reais antes de declarar publicação concluída.
8. Se falhar, fechar 80/443, parar/desabilitar Caddy e demo e reativar serviço privado com seu JAR e banco preservados, pelo túnel original. Procedimentos detalhados em [PUBLIC-DEMO.md](PUBLIC-DEMO.md).

**O subdomínio resolve, mas ainda não abre o ServiceHub:** a instalação pública e o HTTPS não foram ativados, e 80/443 continuam fechadas. A instalação privada segue pelo túnel SSH. O IP pode mudar se a EC2 for parada/iniciada; nesse caso atualizar apenas este A após confirmar o novo endereço.
