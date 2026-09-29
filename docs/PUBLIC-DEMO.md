# Demonstração pública 1.2.0 — preparação, ainda sem publicação

A implantação privada em Ohio permanece na versão anterior, por túnel SSH. Esta entrega não atualiza a EC2, não muda o Free Plan e não abre portas. O domínio ainda será escolhido pelo proprietário. Não há domínio, senha administrativa ou endereço público padrão no código.

## Acesso e permissões

O login usa sessão Spring Security, BCrypt custo 12, token CSRF e logout POST que invalida a sessão. O ADMIN opera os cadastros e ordens existentes. “Entrar como visitante” cria uma sessão VISITANTE exclusivamente no modo demonstrativo: permite GET das consultas e CSV; qualquer gravação na API é negada pelo servidor, mesmo com token CSRF válido. Não existe cadastro público de administrador. O banner identifica dados fictícios.

A sessão expira após 30 minutos de inatividade e não sobrevive ao reinício do Java. Mensagens de falha não distinguem usuário inexistente de senha incorreta. O limitador admite 8 tentativas por IP/minuto, incluindo entrada de visitante e logins bem-sucedidos; existe também teto global de 60/minuto. O mapa tem capacidade máxima de 2.048 IPs e expira entradas, sem descartar bloqueios ativos. Retorna 429 + Retry-After. É memória local e reinicia com o processo: não é proteção contra DDoS; usuários atrás do mesmo NAT compartilham a cota. Caddy não deve confiar em proxies externos sem revisão, pois o IP determina essa cota.

No perfil `public`, cookies são Secure, HttpOnly e SameSite=Lax; acesso HTTP à aplicação é recusado, exceto `/api/health` sem dados. Caddy redireciona HTTP para HTTPS, e Tomcat só aceita informações de encaminhamento de loopback. `/api/csrf` é público e cria o token necessário ao login; não revela dados. Senhas não são registradas em logs. Não ative logs de corpo HTTP, depuração de segurança ou logs de credenciais.

## Criar credenciais somente no computador do administrador

Após `mvn clean verify`, em um terminal interativo com Python 3 e **JDK 17**:

```powershell
python scripts/create_admin.py target/app.jar "$env:USERPROFILE/admin.secrets.properties"
```

O utilitário usa a biblioteca BCrypt do próprio JAR, pede usuário/senha sem eco e grava somente o hash; não sobrescreve arquivos existentes. Nunca envie a senha no chat, no Git, em argumentos de processo ou no histórico do terminal. No Windows, guarde o arquivo em uma pasta privada do usuário e confira suas permissões NTFS; no POSIX o gerador cria com modo 600. Guarde a senha em um gerenciador. Para trocar a senha, gere outro arquivo, substitua o arquivo externo de configuração e reinicie o serviço (todas as sessões serão encerradas).

Para conferir localmente com um banco fictício separado:

```powershell
java -jar target/app.jar --spring.profiles.active=demo "--spring.config.additional-location=file:$env:USERPROFILE/admin.secrets.properties" --server.port=18085
```

Abra `http://localhost:18085`. O perfil `demo` grava em `./data-demo/servicehub.mv.db`; não usa `./data`. O perfil padrão mantém `./data`, não cria dados demonstrativos e exige credenciais. As variáveis externas ADMIN_USERNAME/ADMIN_PASSWORD_HASH também são suportadas, mas o arquivo evita expor o hash no histórico. A aplicação falha ao iniciar sem credenciais válidas. O arquivo aleatório `target/test-auth.json` é exclusivo dos testes e não é distribuído nem uma credencial de produção.

## Plano para aprovação antes da publicação

O proprietário deve informar um domínio/subdomínio já controlado e confirmar a configuração final abaixo após substituir os parâmetros. Não comprar domínio nem ativar plano pago.

| Item | Configuração proposta, não aplicada nesta entrega |
|---|---|
| DNS | Registro A do subdomínio escolhido para o IPv4 **atual** da EC2 existente em us-east-2; conferir o IP antes de salvar. Sem AAAA enquanto não houver IPv6 configurado |
| EC2 | Mesma servicehub-dev, t3.micro, CPU Standard, EBS existente, sem infraestrutura adicional |
| Security Group | TCP 22 somente do IP público atual do proprietário /32; TCP 80 e 443 públicos somente no corte aprovado; nenhuma regra para 8083 ou 2019 |
| Java | 127.0.0.1:8083, usuário servicehub-demo sem privilégios, banco /var/lib/servicehub-demo/servicehub.mv.db |
| Caddy | Portas 80/443, proxy para loopback, API administrativa apenas localhost:2019, sem proxy externo/CDN |
| Serviço privado | Parado durante a demonstração; JAR, configuração e banco originais preservados. Não executar dois Java na mesma porta nem simultaneamente na t3.micro |

O IPv4 pode mudar após parar/iniciar a EC2: atualizar DNS nessa situação. Não alocar Elastic IP nesta entrega. Caddy no mesmo host não cria um serviço AWS adicional, mas tráfego público e uso de CPU podem aumentar consumo dos créditos. O orçamento de US$ 10 continua sendo alerta, não um limitador. Conferir plano, saldo e estimativa antes da publicação; não prometer gratuidade permanente.

## Procedimento futuro de corte (não executado)

1. Obter aprovação do domínio, DNS/rede e janela de manutenção. Baixar o artefato **ServiceHub-EC2** de uma CI aprovada, verificar SHA256. Não compilar na EC2. Confirmar espaço livre para JAR, banco e backups.
2. Criar backup frio do serviço privado com `sudo servicehub-backup`, validar o arquivo `.sha256` e copiar o backup para armazenamento privado fora da EC2, já disponível ao proprietário. Registrar contagens/hash lógico pela API privada antes de parar. Não enviar dados ou backup ao Git/artefatos da CI.
3. Enviar os arquivos desta versão; executar `sudo bash deploy/public-demo/prepare.sh`. Isso prepara usuário, diretórios, unidade e scripts, mas não inicia serviços, não instala Caddy nem abre portas. O padrão dos scripts continua sendo `private`; a seleção `demo` é explícita.
4. Enviar o arquivo administrativo por SCP, instalar com `sudo install -o root -g servicehub-demo -m 640 CAMINHO/admin.secrets.properties /etc/servicehub-demo/admin.secrets.properties` e remover somente a cópia temporária após conferir a instalação. Nunca exibir seu conteúdo. O serviço precisa ler esse arquivo; o diretório tem modo 750.
5. Instalar Caddy da distribuição oficial compatível com Amazon Linux 2023, verificando checksum/assinatura do binário Linux amd64; usar serviço dedicado `caddy` sem root, diretório de certificados privado e CAP_NET_BIND_SERVICE. Não usar instalador remoto encadeado com shell. A configuração foi validada com Caddy 2.11.4; conferir a versão efetiva antes do corte. Criar `/etc/caddy/servicehub.env` com DEMO_DOMAIN e ACME_EMAIL (sem aspas de placeholder); configurar EnvironmentFile no serviço Caddy. Instalar `deploy/public-demo/Caddyfile`, validar com `caddy validate` sob essas mesmas variáveis. Não iniciar Caddy antes da aprovação de rede.
6. Parar **e desabilitar** `servicehub` antes de iniciar a demonstração: `sudo systemctl disable --now servicehub`. Confirmar que 8083 ficou livre. Não copiar seu banco para a demonstração. Executar `sudo SERVICEHUB_INSTANCE=demo servicehub-update CAMINHO/app.jar SHA256`. O primeiro início cria dados fictícios apenas no diretório demonstrativo; o perfil público rejeita outro caminho de banco antes da inicialização do H2. Confirmar `systemctl status servicehub-demo`, logs sem erros e `curl --fail http://127.0.0.1:8083/api/health` com versão do artefato. Depois habilitar `servicehub-demo` para reinício do host.
7. Somente com autorização: aplicar DNS e regras TCP 80/443, iniciar Caddy e validar certificado/redirect. Conferir `ss -lntp`: Java somente127.0.0.1:8083; SG sem 8083/2019. Testar anon401, login, visitante sem escrita, ADMIN, CSRF, logout e cookies no endereço HTTPS. Confirmar que clientes mostrados são fictícios, comparar o hash do banco privado parado com o registrado após sua parada e testar reinício/backup/restore do banco demo atual.
8. Registrar evidências reais em PUBLIC-DEMO-VALIDACAO.md. A publicação só estará concluída após essas verificações.

Fonte da configuração: [proxy reverso do Caddy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy) e [HTTPS automático](https://caddyserver.com/docs/automatic-https). O Caddy define X-Forwarded-For/Proto de forma segura por padrão; não acrescentar `trusted_proxies` para internet aberta.

A unidade `deploy/public-demo/caddy.service` acompanha a configuração. Após conferir o binário oficial em `/usr/local/bin/caddy`, criar usuário de sistema `caddy` sem login, `/var/lib/caddy` como caddy:caddy modo700 e `/etc/caddy` como root:caddy modo750. Instalar Caddyfile como root:caddy640 e servicehub.env como root:root600 (lido por systemd), e a unidade em `/etc/systemd/system/caddy.service`. Executar daemon-reload, validar configuração com as duas variáveis e somente no corte aprovado `systemctl enable --now caddy`. Não executar em paralelo com uma unidade Caddy de pacote já existente. Certificados ficam no diretório privado persistente `/var/lib/caddy`, nunca no Git. Uma mudança no EnvironmentFile exige restart, não apenas reload.

## Túnel e HTTPS

O script `deploy/aws/open-tunnel.ps1` permanece válido para a instalação privada anterior via HTTP. Ele encaminha 8083 e **não** é uma alternativa HTTP para o perfil público. Após o corte, não desligar Secure ou a exigência HTTPS para facilitar o túnel.

Para diagnóstico HTTPS com túnel após o certificado existir, encaminhe a porta Caddy, preservando o domínio TLS:

```powershell
ssh -N -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -i CAMINHO_DA_CHAVE -L 127.0.0.1:8443:127.0.0.1:443 ec2-user@IP_ATUAL_EC2
# Em outro terminal; substituir DOMINIO pelo domínio real do certificado:
curl.exe --resolve DOMINIO:8443:127.0.0.1 https://DOMINIO:8443/api/health
```

Para navegador, resolver temporariamente DOMINIO para127.0.0.1 no computador e acessar `https://DOMINIO:8443`; retirar essa entrada ao terminar. Não usar `-k`, ignorar erros de certificado ou acessar `https://localhost` com certificado de outro nome. Fechar o terminal encerra o túnel; execute novamente o mesmo comando após fechar/reiniciar. Se a porta local estiver ocupada, usar outra porta livre. O Host completo, incluindo porta, deve ser preservado pelo proxy para validação de Origin.

## Atualização, backup e recuperação

Todos os comandos abaixo são futuros, após o corte. Nenhum deles altera o banco privado quando selecionado `demo`:

```bash
sudo SERVICEHUB_INSTANCE=demo servicehub-backup
sudo sha256sum --check /var/backups/servicehub-demo/ARQUIVO.tar.gz.sha256
sudo SERVICEHUB_INSTANCE=demo servicehub-update /caminho/app.jar SHA256_DO_JAR
sudo SERVICEHUB_INSTANCE=demo servicehub-restore /var/backups/servicehub-demo/ARQUIVO.tar.gz SHA256_DO_BACKUP
```

O backup para o serviço antes de copiar o H2 e o reinicia; não copiar o `.mv.db` com Java ativo. Restore exige checksum, trava de manutenção, rejeita caminhos/links no tar, preserva estado anterior e verifica saúde/versão. Restaure apenas backup compatível e conscientemente selecionado: restaurar backup antigo reverte alterações posteriores. Backup não inclui credenciais; guarde-as separadamente. Atualização troca somente o JAR, nunca o diretório do banco, e cria backup prévio. Em falha de saúde, manter parado e recuperar par JAR/banco compatível; não reverter só o JAR depois de uma mudança de esquema. Consultar `journalctl -u servicehub-demo` e `journalctl -u caddy`.

Para voltar à instalação privada: fechar 80/443 no SG, parar/desabilitar Caddy e servicehub-demo, confirmar 8083 livre e reativar `servicehub` com seu JAR e banco originais. Não mover o banco demonstrativo para o privado. O acesso volta pelo túnel anterior. Parar a EC2 não remove EBS/backups e pode deixar custos de armazenamento; usar as instruções de encerramento em AWS.md. Nenhuma remoção de recursos é feita por este PR.
