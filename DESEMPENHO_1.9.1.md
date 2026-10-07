# Desempenho - 1.9.1

Medicao em 07/10/2026, Windows informado pelo Java como Windows 10, Temurin/OpenJDK 17.0.12 e quatro CPUs logicas. Os JARs 1.9 e 1.9.1 foram executados em sequencia, sem outra suite/benchmark Java em andamento, com `-Xmx1g`, `-Dfile.encoding=UTF-8` e `-Djava.awt.headless=true`.

Base 1.9: commit `e28f65a`, SHA256 `581c362579eb5befcb8f8f188758c2ece6ce48be2119fb99022bbd27edb8af37`. Executavel novo: [SHA256SUMS.txt](SHA256SUMS.txt).

## Combate com bots

`--benchmark-bots`: dez atores vivos, nove bots, mesma seed/camera/equipamentos/entrada; saida 1280x720; 120 quadros de aquecimento e 240 medidos por resolucao. Inclui simulacao, render, HUD e escala. Sem janela, thread de mixer ou placa de som; os eventos posicionais e legendas continuam sendo processados.

| Resolucao interna | 1.9: media / p95 | 1.9.1: media / p95 |
| --- | --- | --- |
| 640x360 | 34.23 / 57.40 ms | 35.17 / 67.37 ms |
| 854x480 | 41.32 / 62.73 ms | 28.63 / 32.76 ms |
| 1066x599 | 52.83 / 78.55 ms | 38.87 / 48.31 ms |

IA/simulacao media: 1.9 = 0.326 / 0.189 / 0.155 ms; 1.9.1 = 0.415 / 0.178 / 0.134 ms. Alocacao por tick: 5844 / 5706 / 5408 bytes na base e 6008 / 5849 / 5563 na 1.9.1. Os eventos novos acrescentam aproximadamente 143 a 164 bytes/tick neste cenario.

Logs: [base 1.9](verificacao/benchmark-bots-1.9-comparacao-1.9.1.log) e [1.9.1](verificacao/benchmark-bots-1.9.1.log). O cabecalho "RIFT 1.7" e legado do diagnostico; versao e hash dos JARs foram conferidos separadamente.

## Duelo

`--duel-benchmark`: dois renderizadores simultaneos, arena Fenda, 640x360 por jogador; 50 quadros de aquecimento e 240 medidos. Mede apenas renderizacao, nao o observador de snapshots nem o mixer.

| Visao | 1.9 | 1.9.1 |
| --- | --- | --- |
| J1 | 8.66 ms/quadro | 8.56 ms/quadro |
| J2 | 8.04 ms/quadro | 8.17 ms/quadro |

Logs: [base 1.9](verificacao/duel-benchmark-1.9-comparacao-1.9.1.log) e [1.9.1](verificacao/duel-benchmark-1.9.1.log).

## Limites

Uma execucao por versao, com variacao elevada entre os cenarios. A resolucao menor teve media/p95 maiores na 1.9.1; as demais ficaram menores. Isso nao demonstra ganho causado pelo update. Sistema, JIT e carga externa afetam a amostra; o renderizador nao recebeu uma nova otimizacao nesta etapa.

Os resultados do duelo ficaram proximos. Nao sao garantia de FPS com janela, latencia de rede ou custo de reproducao fisica de audio. O carregamento/cache, limites de vozes/fila e mistura foram validados funcionalmente em [VERIFICACAO.txt](VERIFICACAO.txt), sem dispositivo de som. Saida em fones/alto-falantes ainda exige teste presencial.
