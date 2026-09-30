# Atualização 1.2 — Visual e arsenal

## Correção do mapa das smokes

O mapa anterior espelhava a visão porque usava coordenadas do chão diretamente como coordenadas da tela. A projeção agora usa a mesma direção e o mesmo lado direito da câmera. A frente aparece acima; a direita aparece à direita. O mapa fixa essa orientação ao abrir, e o minimapa gira junto com a visão. Desenho, prévia e clique compartilham a mesma conversão, incluindo em janelas redimensionadas.

## Novo visual

- CAIS-7 recebeu fachadas com frisos, esquadrias, portas e equipamentos, contêineres com ferragens e guindastes no horizonte.
- Piso de maior resolução, variação de materiais, sinalização, grelhas e sombras pré-calculadas.
- Céu panorâmico com nuvens, sol e silhuetas distantes.
- Placas de A, B e dos edifícios usam texturas em perspectiva e respeitam a profundidade.
- Modelos de armas com bordas chanfradas, canos cilíndricos, tambores, carregadores, miras, silenciadores e detalhes metálicos. Recuo, movimento do carregador e bombeamento aparecem no modelo.
- Mãos com luvas e mangas em camadas; armaduras dos agentes com arestas chanfradas e armas de terceiros que refletem a categoria carregada.
- Loja refeita com filtros por categoria, atributos, imagens distintas e prévia 3D animada. A cena 3D do mapa deixa de ser renderizada enquanto essa loja opaca está aberta, reduzindo o trabalho desnecessário.

## 15 armas, sendo 5 pistolas

Novas: **Veil, Talon, Rush, Dart, Circuit, Shade, Helix, Breach, Ridge e Bastion**. Spark, Wisp, Echo, Marrow e Horizon também receberam os novos modelos.

- Pistolas e armas principais são compradas e preservadas em slots separados.
- Semiautomáticas disparam uma vez por clique; automáticas mantêm fogo; Helix dispara rajadas de três tiros.
- Cadência, recuo, precisão, mobilidade, dano por distância, quantidade de projéteis, recarga e som variam por arma.
- Bots usam um arsenal mais variado. Escopetas dos bots disparam múltiplos projéteis; Helix respeita as rajadas.
- O HUD informa arma secundária, principal e modo de disparo. O treino permite experimentar tudo sem custo.

## Validação

149 verificações automatizadas aprovadas, incluindo orientação e lançamento de smokes em seis direções, munição das 15 armas, cliques reais na loja, modos de tiro, habilidades, ACE e uma partida completa de bots. As 21 imagens em `screenshots` saem do renderizador do jogo, incluindo uma galeria com todos os modelos.

Os testes foram executados sem janela no Linux. Janela nativa, captura do mouse, tela cheia e saída de áudio no Windows ainda dependem de teste no computador do jogador. Veja `LEIA_PRIMEIRO.md` e `VERIFICACAO.txt`.
