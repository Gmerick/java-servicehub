# ServiceHub de desenvolvimento na EC2

> Histórico da implantação privada 1.1.0. Para a preparação 1.2.0 com autenticação e banco demonstrativo separado, siga [PUBLIC-DEMO.md](PUBLIC-DEMO.md). Não houve deploy neste PR; não atualizar o JAR antigo sem preparar credenciais externas. O perfil público não aceita HTTP pelo túnel8083.

Uma EC2, Java 17, H2 em arquivo e acesso por túnel SSH. Sem autenticação: **nunca abra 8083, 80 ou 443 no security group**. O serviço força `127.0.0.1:8083`. A interface permanece a mesma.

## Antes de provisionar

Confira o plano, créditos, região habilitada e recursos existentes (instâncias, volumes, pares de chaves e security groups). Não execute novamente o assistente sem verificar uma criação anterior. A conta usada em 28/09/2026 bloqueou `us-east-1` pedindo ativação de recursos avançados; não ative nem migre o plano automaticamente. O proprietário autorizou Ohio (`us-east-2`) em 28/09/2026; essa é a região efetiva.

Configuração proposta: nome `servicehub-dev`, Amazon Linux 2023 oficial Amazon, x86_64, `t3.micro` On-Demand, CPU **Standard**, um EBS raiz `gp3` de **8 GiB criptografado**, 3000 IOPS e 125 MiB/s, sem monitoramento detalhado, sem Elastic IP, sem IAM instance profile. Exigir IMDSv2. Use VPC/subnet padrão com rota para internet, se já existir. Não crie RDS, NAT, balanceador ou Kubernetes.

O único ingresso deve ser TCP 22 da rede pública **do computador do usuário**, `/32`. No console aberto nesse computador use “Meu IP”; não use o endereço de um runner ou CloudShell. Mudança de rede/VPN exige atualizar esse `/32`. Não use `0.0.0.0/0` nem `::/0` em regras de entrada. Egresso padrão permite instalar Java e atualizações; não expõe a aplicação. Guarde a chave privada fora do repositório, em diretório restrito ao usuário. Nunca crie chaves de acesso root.

Estimativa confirmada para Ohio (`us-east-2`) em 28/09/2026, 730 horas/mês, antes dos créditos: t3.micro Linux US$ 0,0104/h = US$ 7,592; IPv4 público US$ 0,005/h = US$ 3,65; gp3 8 × US$ 0,08 = US$ 0,64. **Total aproximado US$ 11,88/mês**, mais tráfego excedente, tributos e qualquer serviço adicional. Reconfirme preço/eligibilidade na região efetiva antes de criar. CPU Standard evita cobrança de créditos excedentes de Unlimited, mas limita desempenho quando os créditos acabam. Os US$ 100 são um saldo promocional, não mensal, e sua duração não é garantida.

Fontes: [EC2 T3](https://aws.amazon.com/ec2/instance-types/t3/), [EBS](https://aws.amazon.com/ebs/pricing/), [IPv4](https://aws.amazon.com/vpc/pricing/).

Orçamento recomendado: `servicehub-dev-monthly-10-usd`, custo mensal recorrente de US$ 10, `UnblendedCost`, excluir tipos `Credit` e `Refund`, todos os serviços, alertas de custo real em 50%, 80% e 100%. E-mail definido pelo proprietário no console, sem publicar endereço no código. **Alertas não desligam recursos nem impõem um teto** e há atraso de atualização dos custos.

## Construir e instalar

Na máquina de desenvolvimento ou CI (não na t3.micro):

```sh
mvn -B -ntp clean verify
python scripts/check_persistence.py
npm ci
npx playwright install chromium
npm run test:ui
```

Use `target/app.jar`. Registre o SHA256 (`Get-FileHash target/app.jar` no PowerShell; `sha256sum target/app.jar` no Linux). Em PowerShell, defina variáveis com valores reais obtidos no console:

```powershell
$Ec2Address = 'IP_PUBLICO_DA_EC2'
$KeyFile = "$env:USERPROFILE\.ssh\servicehub-dev.pem"
scp -i $KeyFile -r deploy/aws "ec2-user@${Ec2Address}:~/servicehub-deploy"
scp -i $KeyFile target/app.jar "ec2-user@${Ec2Address}:~/app.jar"
ssh -i $KeyFile "ec2-user@$Ec2Address"
```

Compare a impressão digital da chave do host SSH por canal confiável (console/log da EC2) antes de aceitar. Não desative a verificação de host. Na EC2:

```sh
sudo bash ~/servicehub-deploy/install.sh
sudo servicehub-update /home/ec2-user/app.jar SHA256_DO_JAR_LOCAL
sudo systemctl status servicehub --no-pager
sudo journalctl -u servicehub -n 100 --no-pager
curl --fail http://127.0.0.1:8083/api/health
sudo ss -lntp
```

`install.sh` instala apenas o runtime Java, usuário de sistema sem login `servicehub`, unidade e ferramentas. Não substitui configuração existente. JAR em `/opt/servicehub/app.jar` pertence a root; dados em `/var/lib/servicehub/servicehub.mv.db` pertencem ao usuário `servicehub`, com diretório 0700. Configuração em `/etc/servicehub/servicehub.env`, 0600. `APP_DEMO=false` evita povoar a instância automaticamente; cadastre somente dados fictícios na validação. H2 usa `WRITE_DELAY=0` para reduzir a janela de perda de commits em encerramento abrupto ([H2](https://h2database.com/html/commands.html#set_write_delay)). Isso não substitui backups.

O systemd reinicia falhas, limita a heap a 384 MiB, restringe escrita ao diretório de dados e impede privilégios adicionais. Cinco falhas em 120 segundos interrompem tentativas; corrija a causa e use `sudo systemctl reset-failed servicehub`.

## Túnel no computador do usuário

```powershell
ssh -i $KeyFile -N -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -L 127.0.0.1:8083:127.0.0.1:8083 "ec2-user@$Ec2Address"
```

Mantenha a janela aberta e acesse **http://127.0.0.1:8083**. `Ctrl+C` fecha o túnel. Se 8083 estiver ocupada localmente, use `-L 127.0.0.1:18083:127.0.0.1:8083` e abra a porta 18083. O destino remoto permanece 8083. Não utilize `-g` nem escuta local `0.0.0.0`.

Valide cadastro de cliente/equipamento, ordem, mão de obra, aprovação, execução e conclusão. Reinicie com `sudo systemctl restart servicehub`; confirme os mesmos registros pelo túnel. Confira na EC2 que `ss` mostra somente `127.0.0.1:8083` e no console que o SG só recebe 22 do `/32`. Do computador, `Test-NetConnection $Ec2Address -Port 8083` deve falhar enquanto a interface via túnel funciona.

## Atualizar o JAR

Repita build/testes e envie apenas o novo JAR. Execute `sudo servicehub-update /home/ec2-user/app.jar SHA256`. O script verifica hash, obtém trava de manutenção, para o serviço, faz backup frio do H2 com checksum, guarda `app.jar.previous`, troca o JAR e aguarda saúde por até 120 segundos (conexão: 2 s; chamada: 5 s; encerramento forçado após mais 2 s, se necessário). Não sobrescreve o diretório de dados. Em falha o serviço fica parado para investigação; não faz rollback automático de esquema. Mantenha também o JAR correspondente a cada backup. Planeje espaço: o volume de 8 GiB não comporta retenção ilimitada.

## Backup consistente e restauração

```sh
sudo servicehub-backup
```

Há breve indisponibilidade. O script serializa manutenção, para a aplicação, copia o arquivo fechado para `/var/backups/servicehub/h2-DATA-PID.tar.gz`, valida o arquivo comprimido, gera e confere SHA256 e reinicia somente se estava ativa, verificando status e versão. Não copie o `.mv.db` enquanto o processo estiver usando o banco. Transfira uma cópia para seu computador via SCP: primeiro copie explicitamente o arquivo escolhido para a home de `ec2-user` com permissão 0600 e dono `ec2-user`; após o download e verificação de hash, remova a cópia intermediária. Um backup no mesmo EBS não protege contra exclusão ou falha do volume. Não foi contratado armazenamento adicional.

Para restaurar, use o comando instalado, um backup confiável e o SHA256 registrado fora da instância:

```sh
sudo servicehub-restore /var/backups/servicehub/h2-DATA-PID.tar.gz SHA256_CONFIAVEL
```

O comando obtém a mesma trava de atualização/backup, copia o arquivo para uma área privada, valida o checksum e exige exatamente um arquivo regular não vazio chamado `servicehub.mv.db` (rejeita links e outros caminhos). Só então para o serviço, salva o estado atual com checksum e substitui o banco por renomeação no mesmo volume. Erros interrompem a operação; após a parada, não há reinício automático em caso de falha. Antes de restaurar banco de outra versão, disponibilize o JAR compatível. O teste de saúde compara status e versão com o JAR; ele não comprova a semântica de todos os registros. Confira dados pela interface após a restauração e retenha o estado anterior.

Os comandos recusam configuração DB_URL diferente do caminho suportado. Um novo deploy não copia arquivos para o diretório do banco; se já houver JAR instalado e o banco estiver ausente/vazio, a atualização aborta para evitar inicializar silenciosamente outro banco. Migrações de esquema pelo próprio aplicativo continuam exigindo backup e compatibilidade.

## Diagnóstico, parada e remoção

- SSH expira: confira estado EC2, IPv4 atual, IP público local, regra TCP 22 `/32`, subnet/rota e chave correta. Nunca abra 22 para o mundo para “testar”.
- Túnel recusa conexão: confira `systemctl status`, `journalctl`, saúde e porta local ocupada.
- Serviço falha: confira Java 17, SHA256, proprietário/permissões do banco, `df -h`, `free -m` e erros no journal. Não apague o banco como correção.
- Pare a aplicação com `sudo systemctl stop servicehub`; isso **não para cobrança da EC2**.
- Pare a instância pelo console para suspender computação. EBS continua cobrado; snapshots e Elastic IP, se existirem, também podem continuar. IPv4 automático é liberado e pode mudar no próximo início. Atualize túnel/SCP com o novo IP.
- Antes de encerrar/excluir a instância, copie e teste backup externo e guarde configuração/JAR. Verifique `DeleteOnTermination`: volume raiz pode ser excluído junto com a instância, destruindo H2 e backups locais.
- Após encerramento, confira volumes/snapshots remanescentes, Elastic IPs e SG/par de chaves exclusivos. Exclua somente recursos comprovadamente deste projeto e sem uso, com autorização para exclusões definitivas. Não remova VPC padrão compartilhada. Confira Billing após o atraso de contabilização. O orçamento pode permanecer como acompanhamento.

## Instância efetiva e acesso validado — 28/09/2026

Implantação em Ohio (`us-east-2`): `servicehub-dev`, instância `i-0d26611f9832a61c9`, IPv4 automático `18.224.63.142`. Configuração proposta acima efetivamente aplicada. Orçamento criado com os três alertas. Consulte [evidências completas](AWS-VALIDACAO.md) para CI, rede, persistência e restauração.

Neste computador, a chave e o arquivo de hosts verificados estão em `C:\Users\SUPORTE\.ssh`. Para reabrir o túnel quando não houver outro escutando em 8083:

```powershell
ssh -i C:\Users\SUPORTE\.ssh\servicehub-dev.pem -o UserKnownHostsFile=C:\Users\SUPORTE\.ssh\servicehub-dev-known_hosts -o StrictHostKeyChecking=yes -o ConnectTimeout=10 -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -N -L 127.0.0.1:8083:127.0.0.1:8083 ec2-user@18.224.63.142
```

Abra http://127.0.0.1:8083. Esse acesso foi validado no computador, inclusive com fluxo completo na EC2. A origem SSH autorizada é `201.74.182.206/32`; se mudar, ajuste somente essa regra no SG `sg-0a68721ee7dc69a9d`.

O backup externo testado está em `C:\Users\SUPORTE\Documents\ServiceHub-backups\servicehub-backup-20260928.tar.gz`. O volume `vol-092b312ad13415b8a` é excluído ao encerrar a EC2; preserve a cópia externa e o JAR compatível. Com a instância parada, o EBS de 8 GiB continua custando aproximadamente US$ 0,64/mês antes de créditos; não há Elastic IP ou snapshot criado nesta implantação.

## Reconectar com PowerShell

Na raiz do repositório, execute:

```powershell
.\deploy\aws\open-tunnel.ps1 -Ec2Address '18.224.63.142'
```

Mantenha o terminal aberto e acesse http://127.0.0.1:8083. Fechar o terminal, pressionar Ctrl+C ou reiniciar o computador encerra o túnel; execute novamente o mesmo comando para reconectar. A aplicação e os dados continuam na EC2. Se já houver um túnel em 8083, use o existente ou escolha outra porta:

```powershell
.\deploy\aws\open-tunnel.ps1 -Ec2Address '18.224.63.142' -KeyFile "$env:USERPROFILE\.ssh\servicehub-dev.pem" -KnownHostsFile "$env:USERPROFILE\.ssh\servicehub-dev-known_hosts" -LocalPort 18083
```

Nesse caso abra http://127.0.0.1:18083. O endereço não está embutido no script: informe `-Ec2Address` ou a variável `SERVICEHUB_EC2_ADDRESS`. Porta ocupada, chave/known_hosts ausentes e endereço inválido causam erro antes de conectar. O modo BatchMode evita prompts indefinidos; chaves com senha precisam estar carregadas no ssh-agent. Falhas de SSH retornam erro, sem alterar regras de rede. O script exige host previamente verificado e não contém chave privada. Após mudança do IPv4 da EC2, informe o endereço novo e confira a chave do host no console antes de atualizar o arquivo known_hosts. Após mudança do IP público do computador, atualize a origem /32 do SSH. Não abra 8083 no security group.

## CI e decisão de merge

A CI falha se build, testes Java, versão/persistência, manutenção em sandbox ou interface falharem. Em 29/09/2026 foi configurada e relida pela API a proteção clássica da `main`: PR obrigatório, check `validate` do GitHub Actions obrigatório e branch atualizada, regras aplicadas também a administradores, force-push e deleção bloqueados. Não há aprovação obrigatória de reviewers (repositório pessoal). Auto-merge permanece desativado. A configuração fica em Settings → Branches → Branch protection rules → main. Antes de decidir pelo merge, confira o check verde do HEAD atual; o merge depende de autorização humana explícita.

## Versão do artefato

O Maven executa `spring-boot:build-info` e gera `META-INF/build-info.properties`. `/api/health` usa `BuildProperties`, sem versão fixa ou fallback. Ao iniciar pela IDE, execute antes `mvn generate-resources` para gerar esse metadado. A CI abre o JAR, compara `build.version` com a versão do POM e consulta a versão HTTP do próprio artefato durante o teste de persistência.

O nome do ZIP e a tag/título da release também derivam do Maven/metadado do JAR. Versões em notas históricas documentam releases passadas; package.json identifica apenas as ferramentas privadas de teste da interface. Checkout/setup-java/setup-node foram migradas para v5 e upload-artifact para v6 (Node 24), fixadas por SHA, e o runner para Ubuntu 24.04.
