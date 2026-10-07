# Desempenho - Piloto Visual 1.9.2

Medicoes em 07/10/2026, Temurin/OpenJDK 17.0.12, Windows, i5-3470 e quatro CPUs
logicas. Renderizacao CPU, sem janela/audio fisico. JARs executados em sequencia
com `-Xmx1g -Dfile.encoding=UTF-8 -Djava.awt.headless=true`, sem outra suite Java
em andamento. A base 1.9.1 tem SHA256
`2a87e96e9471dc968b3e1356e14abe96c3eabf4711f23854f079dcadaad3d75e`;
o JAR final esta registrado em [SHA256SUMS.txt](SHA256SUMS.txt).

## Dez Atores

`--benchmark-bots`: nove bots, ECHO equipada, mesma seed/camera/input, vida alta,
120 quadros de aquecimento e 240 medidos. Saida 1280x720, incluindo simulacao,
render, HUD e escala. Comparacao com a repeticao da base e o build final:

| Resolucao interna | 1.9.1 media / p95 | 1.9.2 media / p95 |
| --- | --- | --- |
| 640x360 | 22.35 / 25.47 ms | 20.12 / 22.83 ms |
| 854x480 | 27.25 / 30.03 ms | 25.25 / 28.21 ms |
| 1066x599 | 33.67 / 36.67 ms | 32.18 / 35.26 ms |

IA/simulacao media: 1.9.1 = 0.220 / 0.164 / 0.144 ms;
1.9.2 = 0.222 / 0.157 / 0.135 ms. Alocacao medida apenas no tick de simulacao:
6003 / 5849 / 5563 versus 5989 / 5883 / 5562 bytes. Nao mede a alocacao do render.

Logs: [base repetida](verificacao/benchmark-bots-1.9.1-comparacao-1.9.2-repeticao.log)
e [build final](verificacao/benchmark-bots-1.9.2.log).
A [primeira base](verificacao/benchmark-bots-1.9.1-comparacao-1.9.2.log)
foi muito mais lenta: 39.09 / 44.04 / 55.34 ms de media. Essa variacao de sistema,
JIT e carga externa impede atribuir toda diferenca ao update ou prometer ganho de FPS.

## Dois Renderizadores

`--duel-benchmark`: dois renderizadores paralelos, 640x360 por jogador, arena Fenda;
50 quadros de aquecimento e 240 medidos. Somente render, sem HUD/rede/mixer.

| Visao | 1.9.1 repetida | 1.9.2 final |
| --- | --- | --- |
| J1 | 7.05 ms | 6.89 ms |
| J2 | 6.62 ms | 6.56 ms |

Logs: [base repetida](verificacao/duel-benchmark-1.9.1-comparacao-1.9.2-repeticao.log)
e [final](verificacao/duel-benchmark-1.9.2.log). A
[primeira base](verificacao/duel-benchmark-1.9.1-comparacao-1.9.2.log)
registrou 8.42 / 7.98 ms. As amostras finais ficaram proximas; nao validam FPS
com duas janelas reais, latencia de entrada ou saida de audio.

## Orcamento e Limites

CAIS-7 acrescenta 1176 triangulos estaticos no maximo, fora da area jogavel.
O receptor ECHO tem 280 triangulos em primeira pessoa e 144 no mundo; equipamento
de terceira pessoa soma 168, totalizando 312. Os tres modulos OBJ do Vertice
somam 88. O detalhamento de atores respeita a distancia de LOD ja existente.
As cinco fotografias de 1K viram mipmaps de ate 256 pixels; cache de modelos e
paletas evita reler/recolorir bitmaps por instancia. Rotacao de armas reutiliza
seno/cosseno, com equivalencia de posicoes/inversa coberta por testes.

O resultado permite manter o piloto no motor atual, nao demonstra folga para
malhas densas, skinning/PBR ou renovar todo o arsenal sem novos benchmarks.
Presets/AUTO continuam necessarios nesta CPU. Testes fisicos e maquinas distintas
permanecem pendentes; a expansao 1.9.3 deve manter orcamento por arma.
