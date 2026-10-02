@echo off
setlocal
title RIFT Protocol
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
    echo.
    echo Java nao foi encontrado. Instale o Java 17 ou superior.
    echo Download oficial: https://adoptium.net/temurin/releases/
    echo Selecione Windows, x64, JDK 17 ou JDK 21.
    echo Ative a opcao Add to PATH no instalador e abra este arquivo novamente.
    echo.
    pause
    exit /b 1
)
if not exist "RiftProtocol.jar" (
    echo RiftProtocol.jar nao encontrado. Extraia o ZIP inteiro antes de jogar.
    pause
    exit /b 1
)
java -Xms128m -Xmx768m -jar "RiftProtocol.jar"
if errorlevel 1 (
    echo.
    echo Nao foi possivel iniciar. Confira se o comando java -version mostra 17 ou superior.
    echo Leia o arquivo LEIA_PRIMEIRO.txt para resolver problemas.
    pause
)
endlocal
