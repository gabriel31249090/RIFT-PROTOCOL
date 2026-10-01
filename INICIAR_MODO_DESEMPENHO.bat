@echo off
setlocal
title RIFT Protocol - Modo desempenho
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
    echo Instale Java 17 ou superior e habilite Add to PATH.
    pause
    exit /b 1
)
if not exist "RiftProtocol.jar" (
    echo Extraia o ZIP inteiro antes de iniciar.
    pause
    exit /b 1
)
echo Iniciando com AUTO, escala rapida e menos efeitos decorativos.
echo Controles, colecao e progresso serao preservados.
java -Xms128m -Xmx768m -jar "RiftProtocol.jar" --performance
if errorlevel 1 pause
endlocal
