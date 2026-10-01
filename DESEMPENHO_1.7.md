# Desempenho — RIFT Protocol 1.7

## Método

Medição local sem janela, OpenJDK 17.0.20 / Linux. Quadro de saída 1280×720, texturas e sombras ligadas, escala rápida, qualidade fixa. Cada qualidade usa 120 quadros de aquecimento e 240 medidos. O mesmo programa, seed, posição inicial da câmera e configuração são usados nos dois JARs.

São dez atores vivos, nove controlados por IA, equipados com Echo, com vida aumentada para manter combate durante a amostra. Habilidades são desativadas neste cenário para isolar tiros, movimentação, animação e render. Os bots das duas versões se movimentam de maneiras diferentes; a geometria visível e a quantidade de tiros podem variar. Não é uma comparação de quadros idênticos nem representa uma partida com múltiplas supremas.

O resultado soma simulação, cenário, personagens, efeitos, HUD e ampliação da imagem. Não inclui compositor da janela, driver da máquina do jogador ou captura de mouse. Os testes foram executados sequencialmente. Resultados curtos variam com aquecimento da JVM e carga do ambiente.

## Comparação com a 1.6.1

| Qualidade | Resolução interna | 1.6.1 média | 1.7 média | 1.7 p95 | Simulação 1.7 |
| --- | --- | --- | --- | --- | --- |
| Baixa | 640×360 | 12,95 ms / 77,2 FPS | 12,80 ms / 78,1 FPS | 16,78 ms | 0,129 ms |
| Média | 854×480 | 15,18 ms / 65,9 FPS | 15,03 ms / 66,5 FPS | 17,80 ms | 0,086 ms |
| Alta | 1066×599 | 19,19 ms / 52,1 FPS | 19,60 ms / 51,0 FPS | 24,70 ms | 0,079 ms |

Neste cenário, a 1.7 ficou no mesmo patamar da 1.6.1. A pequena diferença não demonstra ganho garantido; a qualidade alta ficou ligeiramente mais lenta nesta amostra. A nova IA ocupa uma fração pequena do tempo total, enquanto o render por software continua predominando. Não há promessa de recuperar 150 FPS em qualquer PC.

As otimizações finais de triângulos e peças dos personagens foram verificadas contra a transformação anterior. Os testes comparam pixels/profundidade em 90 casos com recorte e visão reduzida e a geometria de peças em três rotações. A redução de objetos não exige diminuir a resolução nem remover o detalhe dos modelos.

## Partidas completas sem render

Seis seeds fixas, duas partidas em cada mapa, dificuldade normal, 60 ticks por segundo simulado:

- 41 rounds concluídos, 38 plantios, oito desarmes e nenhum encerramento por tempo.
- Vitória do ataque em 73,2% dos rounds desta amostra.
- Simulação: média 0,013 ms/tick; p95 0,064 ms; p99 0,136 ms. Os percentis usam os primeiros 24.000 ticks amostrados após aquecimento.
- Nenhuma partida ultrapassou o limite de ticks ou produziu posição inválida, créditos negativos ou munição negativa.

O resultado serve para encontrar falhas e observar tendências. **41 rounds não comprovam equilíbrio**. O favorecimento do ataque é uma pendência de balanceamento para testes maiores por mapa, incluindo jogo humano. Tempo de simulação sem render não deve ser convertido em FPS do jogo.

## Reproduzir

```sh
java -Xms256m -Xmx768m -Djava.awt.headless=true -jar RiftProtocol.jar --benchmark-bots
java -Djava.awt.headless=true -jar RiftProtocol.jar --simulate-bots 6
java -Djava.awt.headless=true -jar RiftProtocol.jar --self-test
```

No Windows, `DIAGNOSTICO_IA_WINDOWS.bat` grava `DIAGNOSTICO_IA.txt`. O benchmark completo de treino da 1.6.1 também continua acessível com `--benchmark-full`.

Se estiver lento no computador do jogador, usar `INICIAR_MODO_DESEMPENHO.bat`. O perfil mantém texturas, usa escala rápida, desliga sombras/impactos e deixa a resolução no AUTO. Para comparar versões, manter a mesma qualidade, mapa, resolução da janela e situação de combate.
