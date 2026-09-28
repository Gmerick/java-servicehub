param(
    [string]$Ec2Address = '18.224.63.142',
    [string]$KeyFile = "$env:USERPROFILE\.ssh\servicehub-dev.pem",
    [string]$KnownHostsFile = "$env:USERPROFILE\.ssh\servicehub-dev-known_hosts",
    [ValidateRange(1024,65535)][int]$LocalPort = 8083
)
$ErrorActionPreference = 'Stop'
if (!(Test-Path -LiteralPath $KeyFile -PathType Leaf)) { throw "Chave inexistente: $KeyFile" }
if (!(Test-Path -LiteralPath $KnownHostsFile -PathType Leaf)) { throw "Arquivo de hosts verificados inexistente: $KnownHostsFile. Confira a chave do host no console EC2 antes de cadastrar." }
Write-Host "Abra http://127.0.0.1:$LocalPort e mantenha este terminal aberto. Ctrl+C encerra o tunel."
& ssh -i $KeyFile -o "UserKnownHostsFile=$KnownHostsFile" -o StrictHostKeyChecking=yes -o ConnectTimeout=10 -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -N -L "127.0.0.1:${LocalPort}:127.0.0.1:8083" "ec2-user@$Ec2Address"
if ($LASTEXITCODE -ne 0) { throw "SSH terminou com codigo $LASTEXITCODE. Confira rede, IP, security group e porta local." }
