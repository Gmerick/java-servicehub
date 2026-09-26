@echo off
setlocal
cd /d "%~dp0"
if exist "%USERPROFILE%\ProjetosJavaJr\ferramentas" (
  for /d %%J in ("%USERPROFILE%\ProjetosJavaJr\ferramentas\*") do (
    if exist "%%J\bin\javac.exe" set "JAVA_HOME=%%J"
  )
  for /d %%M in ("%USERPROFILE%\ProjetosJavaJr\ferramentas\apache-maven-*") do set "PATH=%%M\bin;%PATH%"
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
where java >nul 2>nul
if errorlevel 1 goto missing
where mvn >nul 2>nul
if errorlevel 1 goto missing
call mvn -B -ntp clean verify
if errorlevel 1 (
  echo A compilacao falhou. Confira o erro acima.
  pause
  exit /b 1
)
echo Abra http://localhost:8083 quando o servidor terminar de iniciar.
java -jar target\app.jar
pause
exit /b
:missing
echo Instale Java JDK 17+ e Maven 3.6.3+ ou use o kit Java anterior.
pause
exit /b 1
