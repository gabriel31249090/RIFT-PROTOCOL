# RIFT Protocol - roadmap após 1.9.2

A lista do projeto foi dividida por dependências. **Entregue** significa presente no pacote; **parcial** significa uma base funcional com limites; **planejado** ainda não existe. Versões futuras são propostas de escopo, sem datas prometidas.

## Sequência de atualizações

| Versão | Foco | Entrega |
| --- | --- | --- |
| 1.8.1 | Estabilidade | Entregue: conflitos resolvidos, pacote recompilado e base verificada. |
| 1.9 | Combate e movimento | Perfis das 15 armas, recuperação por tempo, dano gradual, penetração por arma, atrito e aceleração; algoritmos adaptados do ReGameDLL para Java. |
| 1.9.1 | Áudio e superfícies | Entregue: 144 WAVs originais, síntese de reserva, passos/pousos por material, impactos no modo solo e recargas por etapa; áudio posicional também no cliente 1v1. |
| 1.9.2 | Base visual, cenários e UI | Entregue: importação OBJ, materiais fotografados no CAIS-7, tema de UI compartilhado, ECHO importada/adaptada e armadura modular do Vértice. |
| 1.9.3 | Arsenal visual | Malhas e materiais de armas, mecanismos e animações em primeira pessoa e no mundo. |
| 1.9.4 | Personagens | Modelos com esqueleto; validar um agente completo antes de expandir. |
| 1.9.5 | Bots | Perfis configuráveis, rotas por risco, investigação de ruídos e coordenação por setores. |
| 2.0 | Multiplayer | Latência, interpolação e reconciliação; depois expandir para 2–4 jogadores. |

Antes dos cenários, definir materiais, símbolos, silhuetas e equipamentos do RIFT. Recursos convertidos do CS exigem verificação da licença correspondente antes da distribuição; a licença do ReGameDLL não concede automaticamente direitos sobre os modelos e texturas da instalação do jogo. Cada etapa exige testes do comportamento e comparação de tempo de quadro. As frentes abaixo detalham objetivos futuros, além dessa sequência.

O escopo visual foi ampliado em 07/10/2026 para combinar realismo militar, tecnologia sci-fi própria e estilização legível. Pesquisa e limites: [Plano visual da 1.9.2](PLANO_VISUAL_1.9.2.md). A primeira integração usa javagl/Obj, uma base CC0 da Quaternius, cinco texturas CC0 da Poly Haven e ícones Lucide. Blender, MPFB e MakeHuman continuam como candidatos de produção, não como ferramentas já instaladas.

## 1.9 — Combate e movimento

Perfis próprios por arma e precisão física comum a jogador/bots. Queda contínua de dano além do alcance, orçamento de penetração por arma e movimento com atrito/aceleração separados. Verificação da entrega: [VERIFICACAO.txt](VERIFICACAO.txt). Referências/licenças: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## 1.9.1 - Entregue: áudio e superfícies

Banco de 144 WAVs próprios com três variantes por evento e síntese de reserva. Concreto, madeira e metal têm passos, pousos e impactos distintos no modo solo; recargas usam um início curto e três etapas por categoria. Distância, panorâmica e oclusão por paredes ajustam os sons posicionais. No 1v1, os clientes observam snapshots para reproduzir passos, pousos e recargas; a rede ainda não informa os impactos em paredes. O protocolo, o balanceamento e o RNG de jogo permanecem inalterados em relação à 1.9.

Detalhes: [CHANGELOG_1.9.1.md](CHANGELOG_1.9.1.md). Medições: [DESEMPENHO_1.9.1.md](DESEMPENHO_1.9.1.md). Áudio em fones, dois conjuntos físicos e LAN real continuam pendentes de validação. Próxima etapa: **1.9.2**, identidade visual própria, texturas e importação de malhas estáticas.

## 1.9.2 - Entregue: cenário, modelos-piloto e UI

Parser OBJ com cache, UV e limites de arquivo/triângulos integrado ao renderizador e ao JAR offline. UI compartilhada renovada, CAIS-7 com materiais fotografados e objetos fora da área jogável, ECHO adaptada em primeira pessoa/coleção/mundo e três peças originais de armadura do Vértice seguindo a articulação existente. Colisão, navegação e balanceamento permanecem inalterados. Os demais modelos ficam para 1.9.3 e 1.9.4.

O piloto mantém LOD, carregador/ferrolho animados, skins, charms, movimentos e morte. Não entrega skinning genérico, novos corpos humanos completos, glTF animado nem PBR. Procedência em [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), entrega em [CHANGELOG_1.9.2.md](CHANGELOG_1.9.2.md). A próxima etapa é **1.9.3: expandir o padrão visual para o arsenal**, com validação por arma.

## 1.8 — Entregue: base de duelo entre pessoas

LAN/conexão direta pela internet, três arenas, dois jogadores com equipamentos separados, primeiro a sete e revanche. Duas janelas locais e ponte Raw Input Windows experimental. Próximas prioridades: validar dois conjuntos físicos no Windows; testar redes reais; adicionar previsão/reconciliação e tratamento de latência; depois sincronizar habilidades e expandir modos. Veja MULTIPLAYER.md.

## 1.7 — Entregue: percepção e combate dos bots

Visão de 110°, mira angular limitada, memória, audição, strafe/agachamento, recuo aplicado aos bots, retirada, troca para pistola, papéis básicos por sorteio, desarme estável com aliados em volta, modo Bots vs. Bots por POV, penetração por material e divisão de `Game` em cinco sistemas. Há testes automatizados, simulações completas e diagnóstico de combate com bots.

## 1.7.1 — Orçamento de quadro e diagnóstico

Prioridade antes de aumentar o custo visual: medir quadros com bots, várias smokes e efeitos ao mesmo tempo; adicionar histórico de frametime e percentis na UI; avaliar culling por portais; reduzir alocações restantes de modelos e efeitos; prototipar renderização paralela por faixas com buffers independentes e comparação de pixels. Só ativar paralelismo por padrão se ele superar a versão sequencial em hardware real. Criar preset mínimo explícito e orçamento por efeito.

Critério: comparação antes/depois em PCs de teste, mesmos cenários e configurações, sem perder triângulos, partículas ou precisão de colisão. O AUTO já desce até 384 px de largura interna e há preset de desempenho; isso não equivale a um renderizador paralelo pronto.

## Próxima frente — Identidade, kits e execução em equipe

- Revisar temas, nomes, silhuetas e efeitos dos 11 agentes para acentuar identidade própria. Criar um 12º agente com mecânica nova somente após testes de interação.
- Sinergias e contra-jogo explícitos: Cáustica + Silêncio, dispositivos destrutíveis, indicadores de área/duração, cancelamento uniforme e prévia de trajetória.
- Bot controlador fecha a entrada do site; iniciador avisa e lança flash; entrada espera o tempo correto; sentinela protege o flanco. Suprema exige oportunidade útil, não apenas pontos disponíveis.
- Economia coletiva: decisão de eco, force e full-buy pelo saldo do time; perfil de compra por papel; sugestão de compra para iniciantes; drop e compra para aliado.
- Tela de agente com pequenas demonstrações animadas e informações de contra-jogo.

Critério: quatro habilidades de cada kit têm comportamento verificável, efeitos legíveis e decisões úteis dos bots. Não basta reutilizar uma animação com outra cor.

## Frente futura — Mapas e treino

Um mapa compacto de interiores e outro maior com linhas de sniper. Um deles terá três sites e uma mecânica específica, como porta acionável. Rever ângulos, rotas, callouts, defesa pós-plantio e tempos de rotação antes de decorar.

Combate: hitboxes por membros, tabela de TTK das 15 armas por distância, melhoria dos impactos, flinch configurável, miras alternativas, disparos alternativos e protótipo de tempo de viagem da sniper. Movimento: novas superfícies além dos três materiais sonoros entregues na 1.9.1, queda opcional, cordas/escadas, leaning e percurso de bhop cronometrado.

Modos candidatos, nesta ordem: tutorial guiado, retake/1v1/2v2, Escalada e Sobrevivência. Replicação, regras customizadas e eventos vêm depois da infraestrutura de regras. Um editor simples começa com peças e spawns validados; workshop público exige distribuição e moderação.

Critério: partida completa em ambos os lados, sem locais inacessíveis, situações de sniper injustas ou habilidade sem contra-jogo. Amostra maior de bots registra resultados por mapa/lado, com revisão humana.

## 2.0 — Expansão do multiplayer

Expandir o 1v1 de armas entregue na 1.8 para Mata-Mata de 2–4 jogadores. A base já tem servidor autoritativo; os próximos itens são: comandos de entrada, snapshots, validação de velocidade, munição e dano, lobby por código, ping e chat de texto. Depois testar interpolação, compensação de latência e reconexão sob perda de pacotes. Rodadas completas e serviços públicos só avançam depois disso.

Amigos, parties, voz, regiões, matchmaking por MMR/RR, ranking global, temporadas, restrições de party, relatórios e anti-cheat de produção dependem de serviços online e operação. Os ranks e o Premier atuais são **locais**. A 1.8 inclui servidor direto de duelo. Leaderboard, conta online, relay e serviço hospedado ainda não existem.

## Trilhas que acompanham essas versões

| Área | Base existente / limite | Próximo trabalho |
| --- | --- | --- |
| Armas | 15 armas, dano por distância, cabeça/corpo/pernas, padrão de recuo e wallbang por material | Membros separados, TTK, drops, flinch, ópticas e modos alternativos |
| Movimento | Walk, crouch, jump, air strafe, bhop e tagging; treino básico e passos/pousos por material | Pistas cronometradas, novos materiais, queda, leaning e cordas |
| Habilidades | 44 habilidades de 11 agentes, cargas, assinaturas e orbes; bots usam parte dos kits | Execuções coordenadas, supremas táticas, contra-jogo, prévias e identidade |
| Mapas | 3 layouts de duas bases e dois sites, coberturas, rampas e alturas; grade de navegação de 1 m | Escalas distintas, três sites, interações, destruição e navmesh poligonal |
| Modos | Competitivo local, sem ranque, Spike Rush, DM, TDM, Premier local, treino e observador POV | Tutorial, retake, arenas, Escalada, Sobrevivência, Replicação e Custom |
| Progressão | XP, contratos, maestria, rank e histórico local básico; coleção cosmética | Precisão, HS%, KAST, ADR, resumo por rodada, missões, títulos e passe local |
| Áudio | 144 WAVs originais com síntese de reserva, sons por material, recargas por etapa, distância, panorâmica e oclusão | Validação física, eventos de impacto no 1v1, reverberação, vozes, callouts e volume separado de UI/voz |
| Gráficos | Texturas bitmap, artes de menu/agentes, materiais, efeitos e animações articuladas | Iluminação pré-calculada, sombras, FXAA, clima, inspeções/vitória, mortes e ragdoll |
| Câmeras | POV ao morrer e observação dos bots | Câmera livre, killcam, gravação de eventos e replay por rodada |
| Acessibilidade | FOV, sensibilidade ADS, binds, crosshair, modos de segurar/alternar, movimento reduzido e legendas básicas | Paletas daltônicas completas, perfis de controle, gamepad, inglês/espanhol |
| UI | Fluxo jogar → modo → aceitar → agente → travar; minimapa, placar, ping e compra rápida | Pings por tipo, rádio, resumos, ADR/HS%, ícones no killfeed e HUD editável |
| Social | Partidas locais com bots | Amigos, party, voz, chat, bloqueio/report, clãs/LFG e reputação após rede |
| Arquitetura | Bots/Economy/Rules/Spike/Loadout separados; núcleo ainda acoplado ao Game | Dados externos de armas/agentes, eventos gerais, dependências menores, logging e formatação do legado |
| Build | JAR Java 17, compilação sem downloads, self-test, simulações e benchmarks | CI no repositório confirmado; instalador Windows com runtime; versionamento e changelog automatizados |
| Publicação | README, imagens, fontes, licença e ZIP preparados | Confirmar repositório correto, criar releases, itch.io, trailer e página no portfólio |
| Comunidade | Código e pacote local compartilháveis | Formulário de feedback, mapas da comunidade e distribuição de mods |
| Universo | Nome próprio e agentes com temas próprios | Facções, histórias, falas, eventos e narrativa com identidade consistente |

## Feedback que ajuda a escolher o próximo patch

Registrar mapa, modo, dificuldade, versão, arma/agente, passos para reproduzir e comportamento esperado. Para FPS, informar CPU, GPU, resolução da janela, qualidade e anexar `DIAGNOSTICO_IA.txt`. Capturas ou clipes devem mostrar o contador de desempenho quando possível. O formulário para amigos ainda não foi publicado nem enviado.
