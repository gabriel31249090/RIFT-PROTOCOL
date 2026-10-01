# RIFT Protocol 1.7 — Contato visual

## Bots

- Campo de visão horizontal de 110°, com bloqueio por paredes, fumaça, cegueira e visão reduzida. Alcance de 54 m, ou 72 m com arma de precisão; nearsight limita a 7 m.
- Giro da mira limitado a 105°/s, 180°/s ou 265°/s, conforme a dificuldade. A cabeça acompanha essa orientação e o corpo gira gradualmente. O disparo parte da direção da mira.
- Percepção a cada 190/140/110 ms, defasada por bot. A linha de visão é conferida novamente antes de atirar, para respeitar uma smoke ou parede recém-criada.
- Memória do último contato por 5–7 s. O bot procura a posição lembrada, olha ao redor e abandona pistas antigas; não acompanha a posição atual de um alvo oculto.
- Audição de corrida, pouso, tiro, recarga, lançamento de habilidade e plantio. Pistas sonoras têm erro de posição, alcance e atenuação por parede. O volume escolhido pelo jogador não altera a audição da IA.
- Strafe, pausas para disparo e agachamentos nos duelos. Dificuldade altera precisão, cadência de decisão, giro e disciplina ao atirar em movimento. Bots usam o padrão de recuo das armas com compensação imperfeita.
- Retirada para cobertura ao ficar com pouca vida ou recarregar. Troca para pistola quando o carregador principal acaba perto do inimigo, respeitando o tempo de saque. Depois da ameaça, retorna à principal para recarregar.
- Papéis básicos e escolha do site por sorteio ponderado: entrada, flanco, suporte e precisão. Personalidades agressiva, cautelosa e atiradora. O flanqueador usa a rota externa; o portador prioriza o objetivo. Execuções completas de flash + entrada ainda estão no roadmap.
- Bots se afastam de sobreposições próximas. A mira de deslocamento fica na altura dos olhos, sem mirar no chão ao alcançar um waypoint.
- O desarmador mantém seu canal quando outro aliado chega. Aliados se distribuem perto do objetivo para vigiar as entradas.

## Assistir bots

No menu, clique em **ASSISTIR BOTS**. Duas equipes autônomas disputam uma partida sem ranque, no mapa selecionado anteriormente na tela de modos. **Espaço** alterna entre os participantes vivos dos dois times; **Tab** mostra o placar; **Esc** pausa. O painel informa agente, papel, vida, arma, munição e estado de percepção. O minimapa do observador mostra ambos os times.

Este modo não concede XP, rank ou vitórias ao perfil. A câmera acompanha o POV; câmera livre, replay e ferramentas completas de observer ficam para uma atualização posterior.

## Penetração por material

Madeira e metal têm resistências diferentes. A espessura efetivamente atravessada cresce em impactos oblíquos. Duas superfícies compartilham o orçamento de penetração; o dano cai depois de cada superfície. Concreto estrutural bloqueia todos os tiros. As regras valem para jogador e bots.

| Arma / categoria | Orçamento de penetração |
| --- | ---: |
| Pistolas leves | 0,15 |
| Talon | 0,45 |
| SMGs | 0,28 |
| Fuzis | 0,60 |
| Ridge / Horizon | 0,90 |
| Bastion | 1,20 |
| Escopetas / Dart | 0 |

Custo = espessura em metros × resistência: madeira **1**, metal **3,5**. Um painel de madeira de 20 cm permite passagem de fuzil; metal de 20 cm exige precisão ou arma pesada. O painel da passagem leste, perto de x=116 / z=72, agora tem material e textura metálicos. As demais estruturas grossas permanecem sólidas.

## Desempenho e arquitetura

- No máximo duas buscas de rota por quadro, com temporizadores individuais.
- Navegação deixa de criar um array de vizinhos por célula visitada.
- Transformação dos triângulos sem textura evita três vetores temporários por face quando não há recorte no plano próximo.
- Peças dos personagens compartilham oito cantos transformados; deixam de construir uma lista e uma malha intermediárias para cada caixa.
- Mantidos resolução automática, escala rápida, preset de desempenho e otimizações de cenário da 1.6.1.
- Sistemas separados em `Bots`, `Economy`, `Rules`, `Spike` e `Loadout`. `Game` ainda coordena a simulação e contém entidades e dados; a modularização é incremental.
- Build só substitui o JAR depois de terminar de escrevê-lo.

Os resultados medidos e suas limitações estão em **DESEMPENHO_1.7.md** e **VERIFICACAO.txt**. Não há promessa de FPS no computador do jogador.

## Ferramentas incluídas

```sh
java Build.java
java -jar RiftProtocol.jar --self-test
java -jar RiftProtocol.jar --bot-test
java -jar RiftProtocol.jar --simulate-bots 6
java -jar RiftProtocol.jar --benchmark-bots
```

`--simulate-bots` aceita de 1 a 30 partidas, usa seeds fixas e alterna entre os três mapas. Mostra resultados por lado, plantios, desarmes, timeouts e tempo da simulação. É um teste de funcionamento; poucas partidas não demonstram equilíbrio competitivo.

## Escopo

Continuam disponíveis 11 agentes, 15 armas, 3 lâminas, 3 mapas e 6 modos locais. Esta versão não acrescenta multiplayer, um novo agente, novos mapas ou publicação no GitHub/itch.io. A lista completa de próximos trabalhos está em **ROADMAP.md**.
