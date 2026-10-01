@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Instale Java 17 ou superior e extraia o ZIP inteiro.
  pause
  exit /b 1
)
java -Xms128m -Xmx1024m -jar "RiftProtocol.jar" --multiplayer
if errorlevel 1 pause
endlocal
