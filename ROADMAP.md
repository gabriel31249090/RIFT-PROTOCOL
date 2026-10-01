# RIFT Protocol — roadmap após 1.8

A lista do projeto foi dividida por dependências. **Entregue** significa presente no pacote; **parcial** significa uma base funcional com limites; **planejado** ainda não existe. Versões futuras são propostas de escopo, sem datas prometidas.

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

## 1.9 — Mapas, combate e treino

Um mapa compacto de interiores e outro maior com linhas de sniper. Um deles terá três sites e uma mecânica específica, como porta acionável. Rever ângulos, rotas, callouts, defesa pós-plantio e tempos de rotação antes de decorar.

Combate: hitboxes por membros, tabela de TTK das 15 armas por distância, melhoria dos impactos, flinch configurável, miras alternativas, disparos alternativos e protótipo de tempo de viagem da sniper. Movimento: superfícies e sons distintos, pouso proporcional, queda opcional, cordas/escadas, leaning, rampas e percurso de bhop cronometrado.

Modos candidatos, nesta ordem: tutorial guiado, retake/1v1/2v2, Escalada e Sobrevivência. Replicação, regras customizadas e eventos vêm depois da infraestrutura de regras. Um editor simples começa com peças e spawns validados; workshop público exige distribuição e moderação.

Critério: partida completa em ambos os lados, sem locais inacessíveis, situações de sniper injustas ou habilidade sem contra-jogo. Amostra maior de bots registra resultados por mapa/lado, com revisão humana.

## 2.0 — Expansão do multiplayer

Expandir o 1v1 de armas entregue na 1.8 para Mata-Mata de 2–4 jogadores. A base já tem servidor autoritativo; os próximos itens são: comandos de entrada, snapshots, validação de velocidade, munição e dano, lobby por código, ping e chat de texto. Depois testar interpolação, compensação de latência e reconexão sob perda de pacotes. Rodadas completas e serviços públicos só avançam depois disso.

Amigos, parties, voz, regiões, matchmaking por MMR/RR, ranking global, temporadas, restrições de party, relatórios e anti-cheat de produção dependem de serviços online e operação. Os ranks e o Premier atuais são **locais**. A 1.8 inclui servidor direto de duelo. Leaderboard, conta online, relay e serviço hospedado ainda não existem.

## Trilhas que acompanham essas versões

| Área | Base existente / limite | Próximo trabalho |
| --- | --- | --- |
| Armas | 15 armas, dano por distância, cabeça/corpo/pernas, padrão de recuo e wallbang por material | Membros separados, TTK, drops, flinch, ópticas e modos alternativos |
| Movimento | Walk, crouch, jump, air strafe, bhop e tagging; treino básico | Pistas cronometradas, passos por material, queda, leaning, cordas e rampas |
| Habilidades | 44 habilidades de 11 agentes, cargas, assinaturas e orbes; bots usam parte dos kits | Execuções coordenadas, supremas táticas, contra-jogo, prévias e identidade |
| Mapas | 3 layouts de duas bases e dois sites, coberturas, rampas e alturas; grade de navegação de 1 m | Escalas distintas, três sites, interações, destruição e navmesh poligonal |
| Modos | Competitivo local, sem ranque, Spike Rush, DM, TDM, Premier local, treino e observador POV | Tutorial, retake, arenas, Escalada, Sobrevivência, Replicação e Custom |
| Progressão | XP, contratos, maestria, rank e histórico local básico; coleção cosmética | Precisão, HS%, KAST, ADR, resumo por rodada, missões, títulos e passe local |
| Áudio | Síntese de efeitos, volume de música/efeitos, panorâmica e alguma atenuação | Oclusão consistente, reverberação, vozes, callouts e volume separado de UI/voz |
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
