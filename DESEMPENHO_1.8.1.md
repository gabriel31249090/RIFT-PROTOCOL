# Desempenho - 1.8.1

Medicao em 02/10/2026, na mesma maquina Windows, Temurin/OpenJDK 17.0.12 e 4 CPUs logicas disponiveis. Os processos Java rodaram sequencialmente, sem outra suite ou benchmark Java concorrente. Ambos os JARs usaram `-Xmx1g`, `-Dfile.encoding=UTF-8` e `-Djava.awt.headless=true` na comparacao abaixo.

## Combate com bots

Comando: `java -Xmx1g "-Djava.awt.headless=true" -jar RiftProtocol.jar --benchmark-bots`.

Dez atores vivos, nove bots, mesma seed, camera, equipamentos e entrada. Saida 1280x720, 120 quadros de aquecimento e 240 medidos por qualidade. Inclui simulacao, cenario, HUD e escala da imagem, sem janela nativa ou audio.

| Resolucao interna | 1.8 original: media / p95 | 1.8.1: media / p95 |
| --- | --- | --- |
| 640x360 | 34.91 / 58.36 ms | 31.94 / 48.85 ms |
| 854x480 | 39.74 / 57.16 ms | 40.19 / 59.45 ms |
| 1066x599 | 51.28 / 77.50 ms | 49.48 / 67.81 ms |

Logs: `verificacao/benchmark-bots-1.8-windows-repeat.log` e `verificacao/benchmark-bots-1.8.1.log`.

A primeira execucao do JAR original produziu medias de 122.42, 157.50 e 75.74 ms. A forte variacao levou a repetir a medicao; o log inicial foi preservado em `verificacao/benchmark-bots-1.8-windows.log`. A tabela usa essa repeticao. As amostras ficam na mesma ordem de grandeza, mas nao isolam a variacao do sistema/JIT nem demonstram ganho causado pela atualizacao.

## Duelo

Comando: `java -Xmx1g "-Djava.awt.headless=true" -jar RiftProtocol.jar --duel-benchmark`.

Dois renderizadores simultaneos, arena Fenda, 640x360 por jogador, 50 quadros de aquecimento e 240 medidos por visao, sem limite de FPS.

| Visao | 1.8 original | 1.8.1 |
| --- | --- | --- |
| J1 | 12.58 ms/quadro | 10.10 ms/quadro |
| J2 | 12.05 ms/quadro | 9.43 ms/quadro |

Logs: `verificacao/duel-benchmark-1.8-windows.log` e `verificacao/duel-benchmark-1.8.1.log`.

## Interpretacao

A 1.8.1 restaura os fontes da base 1.8 e altera apenas a identificacao da versao no menu e no manifesto. Nao ha nova otimizacao de renderizacao. Os resultados servem como referencia local para os proximos updates; nao garantem FPS com janela, dispositivos fisicos ou rede. As medidas Linux da entrega 1.8 permanecem como historico em `DESEMPENHO_1.8.md` e nao sao uma comparacao equivalente com este Windows.
