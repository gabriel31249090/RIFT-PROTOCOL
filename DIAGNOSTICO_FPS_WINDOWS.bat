@echo off
setlocal
title RIFT Protocol - Diagnostico de FPS
cd /d "%~dp0"
echo Medindo o quadro completo. Aguarde cerca de um minuto.
echo Este teste nao abre janela e nao altera suas preferencias.
java -Xmx768m -Djava.awt.headless=true -jar "RiftProtocol.jar" --benchmark-full > "DIAGNOSTICO_FPS.txt" 2>&1
type "DIAGNOSTICO_FPS.txt"
echo.
echo Resultado salvo em DIAGNOSTICO_FPS.txt.
pause
endlocal
