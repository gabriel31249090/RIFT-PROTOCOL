# Desempenho - 1.9

Medicao em 02/10/2026, Windows informado pelo Java como Windows 10, Temurin/OpenJDK 17.0.12 e quatro CPUs logicas. Os JARs 1.8.1 e 1.9 foram executados isoladamente, em sequencia, com `-Xmx1g`, `-Dfile.encoding=UTF-8` e `-Djava.awt.headless=true`. Nenhuma outra suite/benchmark Java estava em execucao.

## Combate com bots

`--benchmark-bots`: dez atores vivos, nove bots, mesma seed/camera/equipamentos/entrada; saida 1280x720; 120 quadros de aquecimento e 240 medidos por resolucao interna. Inclui simulacao, render, HUD e escala, sem janela/audio fisico.

| Resolucao interna | 1.8.1: media / p95 | 1.9: media / p95 |
| --- | --- | --- |
| 640x360 | 21.60 / 23.80 ms | 21.90 / 23.88 ms |
| 854x480 | 26.55 / 28.90 ms | 26.63 / 28.99 ms |
| 1066x599 | 37.18 / 59.63 ms | 33.38 / 36.08 ms |

Alocacao da simulacao: 1.8.1 = 5533 / 5381 / 5110 bytes por tick; 1.9 = 5835 / 5706 / 5433. O pequeno aumento acompanha os novos eventos de penetracao. Media de IA/simulacao: 0.196 / 0.143 / 0.136 ms na base, 0.207 / 0.152 / 0.123 ms na 1.9.

Logs: `verificacao/benchmark-bots-1.8.1-comparacao-1.9.log` e `verificacao/benchmark-bots-1.9.log`. Os cabecalhos antigos do diagnostico ainda dizem "RIFT 1.7"; a identificacao dos JARs comparados foi conferida pelo manifesto/hash, nao por esse texto.

## Duelo

`--duel-benchmark`: dois renderizadores simultaneos, 640x360 por jogador, arena Fenda; 50 quadros de aquecimento e 240 medidos por visao.

| Visao | 1.8.1 | 1.9 |
| --- | --- | --- |
| J1 | 6.91 ms/quadro | 6.81 ms/quadro |
| J2 | 6.58 ms/quadro | 6.48 ms/quadro |

Logs: `verificacao/duel-benchmark-1.8.1-comparacao-1.9.log` e `verificacao/duel-benchmark-1.9.log`.

## Limites

Esta etapa altera combate/movimento, nao otimiza o renderizador. Os resultados ficaram proximos nas duas resolucoes menores e no duelo; a variacao da resolucao maior nao demonstra ganho causado pelo update. Sistema/JIT e trajetorias diferentes dos bots afetam os tempos. Sao amostras locais sem janela, nao uma garantia de FPS ou de latencia de rede.

As seis partidas completas de `--simulate-bots 6` deram media de 0.024 ms/tick, p95 de 0.099 e p99 de 0.220 (sem render, amostra inicial de 24000 ticks). Sao outro cenario e nao devem ser comparadas diretamente com os tempos do quadro acima. Nao validam equilibrio competitivo.
