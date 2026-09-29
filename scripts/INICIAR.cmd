@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java nao encontrado. Instale o JDK 17 ou superior e reabra este arquivo.
  pause
  exit /b 1
)
if not exist app.jar (
  echo app.jar nao encontrado. Extraia todo o ZIP antes de iniciar.
  pause
  exit /b 1
)
echo ServiceHub - http://localhost:8083
echo Aguarde a mensagem Started ServiceHubApplication e abra o endereco acima.
echo Para encerrar, pressione Ctrl+C. Seus dados ficam na pasta data.
if not defined SERVICEHUB_ADMIN_FILE (
  echo Configure SERVICEHUB_ADMIN_FILE com o caminho do admin.secrets.properties externo. Veja COMO-USAR.md.
  pause
  exit /b 1
)
java -jar app.jar "--spring.config.additional-location=file:%SERVICEHUB_ADMIN_FILE%"
pause
