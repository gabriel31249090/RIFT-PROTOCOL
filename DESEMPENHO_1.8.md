# Desempenho do duelo — 1.8

Medição no ambiente de desenvolvimento Linux/OpenJDK 17.0.20. Dois renderizadores executaram simultaneamente, cada um em sua thread, com câmera própria e dois atores na arena Fenda. Resolução interna fixa 640 × 360; 50 quadros de aquecimento e 240 medidos por visão, sem limite de FPS.

| Visão | Tempo médio por quadro | Capacidade de render medida |
| --- | ---: | ---: |
| J1 | 4,30 ms | 232,3 FPS |
| J2 | 4,15 ms | 240,9 FPS |

Comando: `java -jar RiftProtocol.jar --duel-benchmark`.

São medidas do renderizador, sem janelas nativas, áudio físico, input USB ou comunicação pela internet. Não equivalem ao FPS do computador do jogador. Também não são comparação com o benchmark anterior de mapas grandes e dez bots, pois o cenário é diferente.

O modo jogável começa limitado a 60 FPS por janela, resolução interna AUTO e sombras desligadas. O menu principal deixa de renderizar enquanto a sessão está aberta. As duas visões compartilham o mapa estático, mas cada thread mantém sua própria paleta de textura. Um teste compara os pixels da renderização simultânea com a sequencial para detectar interferência entre caches.

O protocolo usa snapshots pequenos, filas de saída limitadas a um snapshot recente e canais de entrada que acumulam bordas de teclas/cliques. Um cliente lento não bloqueia a thread que simula o jogo. Nenhuma previsão de movimento ou compensação de latência foi implementada nesta versão: conexão lenta pode produzir atraso perceptível mesmo com FPS alto.
