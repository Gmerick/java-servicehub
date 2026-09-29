param(
    [string]$Ec2Address = $env:SERVICEHUB_EC2_ADDRESS,
    [string]$KeyFile = "$env:USERPROFILE\.ssh\servicehub-dev.pem",
    [string]$KnownHostsFile = "$env:USERPROFILE\.ssh\servicehub-dev-known_hosts",
    [ValidateRange(1024,65535)][int]$LocalPort = 8083
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Ec2Address) -or $Ec2Address -notmatch '^[a-zA-Z0-9][a-zA-Z0-9.-]*$') { throw 'Informe -Ec2Address com o IPv4/DNS atual da EC2 ou defina SERVICEHUB_EC2_ADDRESS.' }
$null = Get-Command ssh -ErrorAction Stop
if (!(Test-Path -LiteralPath $KeyFile -PathType Leaf)) { throw "Chave inexistente: $KeyFile" }
if (!(Test-Path -LiteralPath $KnownHostsFile -PathType Leaf)) { throw "Arquivo de hosts verificados inexistente: $KnownHostsFile. Confira a chave do host no console EC2 antes de cadastrar." }
$probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $LocalPort)
try { $probe.Start() } catch { throw "Porta local $LocalPort ocupada. Use o tunel existente ou informe -LocalPort com outra porta." } finally { $probe.Stop() }
Write-Host "Conectando: quando o SSH estabelecer a conexao, abra http://127.0.0.1:$LocalPort. Mantenha este terminal aberto; Ctrl+C encerra o tunel."
& ssh -i $KeyFile -o "UserKnownHostsFile=`"$KnownHostsFile`"" -o BatchMode=yes -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10 -o ConnectionAttempts=1 -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -N -L "127.0.0.1:${LocalPort}:127.0.0.1:8083" "ec2-user@$Ec2Address"
if ($LASTEXITCODE -ne 0) { throw "SSH terminou com codigo $LASTEXITCODE. Confira rede, IP, security group e porta local." }
