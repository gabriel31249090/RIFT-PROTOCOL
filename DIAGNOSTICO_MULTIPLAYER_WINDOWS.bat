@echo off
setlocal
cd /d "%~dp0"
echo Verificando o duelo com duas conexoes locais...
java -Xmx1024m -Djava.awt.headless=true -jar "RiftProtocol.jar" --duel-test > DIAGNOSTICO_MULTIPLAYER.txt 2>&1
if errorlevel 1 (
  echo Falha. Envie DIAGNOSTICO_MULTIPLAYER.txt.
) else (
  echo Testes aprovados. Este teste nao verifica dispositivos fisicos nem a internet.
)
echo Resultado salvo em DIAGNOSTICO_MULTIPLAYER.txt
pause
endlocal
