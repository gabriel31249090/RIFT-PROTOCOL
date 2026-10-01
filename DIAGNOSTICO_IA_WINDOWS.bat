@echo off
setlocal
title RIFT Protocol - Diagnostico de IA
cd /d "%~dp0"
echo Medindo combate com bots. Aguarde.
echo O teste nao abre janela nem altera seu perfil.
java -Xms256m -Xmx768m -Djava.awt.headless=true -jar "RiftProtocol.jar" --benchmark-bots > "DIAGNOSTICO_IA.txt" 2>&1
type "DIAGNOSTICO_IA.txt"
echo Resultado salvo em DIAGNOSTICO_IA.txt.
pause
endlocal
