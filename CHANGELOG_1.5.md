# RIFT Protocol 1.5 — Rotas e vigilância

## Sete pedidos implementados

1. Arremessáveis agora invertem o vetor de velocidade completo ao colidir, inclusive contra o piso e em impactos oblíquos. A volta é reta, sem gravidade/curvatura e sem ricochetes repetidos. Foguete continua explodindo ao contato.
2. Elenco com 44 habilidades exclusivas. Eclipse substitui a smoke duplicada do Espectro. Pesadelo aplica visão reduzida, não clarão branco. Sonar detecta ruído em vez de repetir o pulso de presença.
3. Áureo: pistola especial, escudo frontal, âncora e fuzil de suprema. Trama: fio entre paredes, gaiola, câmera controlável com dardo e duas varreduras por corpo inimigo. Ambos gratuitos, com dispositivos destrutíveis.
4. Biblioteca visual nova: 44 ícones próprios, granadas com silhuetas diferentes, dispositivos detalhados, efeitos temáticos, materiais animados das smokes e armas especiais originais. Personagens novos têm acessórios próprios.
5. Bhop com aceleração por projeção no ar, buffer de salto de 100 ms, preservação de velocidade no pouso e limite de 11,5 m/s. Segurar espaço não automatiza saltos. T mostra os indicadores no treino.
6. Três layouts reconstruídos com corredores, entradas de sites, rotas de ataque/defesa/rotação, cobertura penetrável e posições a 2,4/4,2 m. Callouts no HUD e no ping.
7. Configurações em cinco abas, 25 teclas remapeáveis, sensibilidade ADS, qualidade/sombras/distância/FPS, três volumes, FOV/mira e acessibilidade. F10 funciona como acesso rápido.

## Núcleo local

- Dano de pernas a 75%, cabeça/corpo preservados.
- Penetração em até dois painéis de madeira, condicionada à arma e espessura, com redução de dano. Paredes estruturais não são penetráveis.
- Salvar loadout e recomprar com K. L pede 600 créditos a um bot aliado que tenha saldo, uma vez por rodada.
- Desarme recompensa 300 créditos e um ponto de suprema.
- Assinaturas vazias recuperam uma carga em 30 s; dispositivos reativáveis aguardam o anterior acabar.
- Dois orbes de suprema compartilhados por rodada, coleta com F parado por 0,9 s.
- Competitivo em 6:6 entra em prorrogação. Troca de lado e 5.000 créditos a cada rodada extra. Vantagem de duas decide; rodada 19 é morte súbita.
- Disparos dos bots com panorama estéreo, distância e atenuação por paredes. Trilha ambiente sintetizada exclusiva dos menus. Legendas de efeitos.
- Trocar agente no treino e respawn limpam as armas especiais anteriores.

## Compatibilidade

Java 17+, sem dependências externas. Salvos da versão 1.4 continuam sendo carregados; maestria agora comporta onze agentes. Armas normais, lâminas, skins e pingentes mantidos. A interface exibe a edição local contra bots.

## Validação e limites

Testes determinísticos de gameplay, menus, física, colisão, navegação, dispositivos, progressão, preferências, economia e renderização. Capturas reais geradas pelo mesmo renderizador. Detalhes em VERIFICACAO.txt.

Janela nativa, dispositivo de áudio, mouse e fullscreen não foram exercitados no ambiente de testes sem tela. Desempenho depende da CPU e da resolução. Não há servidores, multiplayer online, anti-cheat, sistema social, replays, Workshop ou versão mobile nesta entrega. O roteiro desses sistemas está no final de LEIA_PRIMEIRO.md.
