# RIFT Protocol 1.6.1 — Correção de desempenho

Esta correção responde à queda de aproximadamente 150 para 13 FPS relatada após a versão 1.6. O perfil de execução identificou custo elevado na rasterização texturizada, criação de objetos e ampliação da imagem. As medições desta entrega incluem o HUD e a composição do quadro; os valores do computador do jogador ainda precisam ser confirmados.

## Renderização

- Cenário estático dividido em grupos espaciais, com descarte conservador dos grupos fora da câmera. Os grupos visíveis são desenhados dos mais próximos aos mais distantes. Colisões, tiros, navegação e conteúdo dos mapas permanecem independentes dessa organização visual.
- Rasterização por faixas horizontais dentro dos triângulos, evitando percorrer todos os pixels de seu retângulo envolvente. UVs continuam corrigidos pela perspectiva e há recorte junto à câmera.
- Caminho comum das texturas usa coordenadas primitivas, sem criar seis objetos por face. Paletas de cor têm armazenamento fixo reutilizável, eliminando a criação contínua de tabelas. As cores-base são quantizadas em passos de quatro níveis por canal para aumentar a reutilização.
- Terreno do minimapa pré-renderizado por mapa. Posição dos jogadores, pings e smokes continuam sendo atualizados em tempo real. Mudar de mapa refaz o cache.
- Ampliação RÁPIDA do cenário ativada por padrão. A opção SUAVE continua em Gráficos e custa mais CPU. Ilustrações, textos e controles da interface conservam seu tratamento próprio.

## Resposta a quedas de FPS

- AUTO considera simulação, cenário, HUD e ampliação, em vez de apenas o desenho 3D. A apresentação da janela pelo sistema operacional não está incluída nessa medição.
- Redução de resolução em poucos quadros sob carga; recuperação só após folga sustentada, para evitar oscilações constantes.
- Seis resoluções internas automáticas: 384 × 216, 480 × 270, 640 × 360, 768 × 432, 854 × 480 e 1066 × 599. As qualidades fixas mantêm seus tamanhos anteriores. A redução afeta o cenário; a interface permanece na resolução da janela.
- Contador em partida mostra FPS, tempo do quadro e resolução interna.
- Botão MODO DESEMPENHO em F10 → Gráficos e iniciador `INICIAR_MODO_DESEMPENHO.bat`. Aplicam AUTO, limite de 120 FPS, distância de 100 m, ampliação rápida e desligam sombras e marcas/cápsulas. Preservam texturas, controles, coleção e progressão. As preferências gráficas ficam salvas ao sair.

## Verificação e diagnóstico

- Novo `--benchmark-full`, incluindo simulação, cenário, interface e escala para 1280 × 720. Imprime média, percentil 95 e resolução interna, sem abrir janela nem alterar o perfil.
- `DIAGNOSTICO_FPS_WINDOWS.bat` grava os resultados em `DIAGNOSTICO_FPS.txt` para comparação no computador do jogador.
- Regressões de cobertura dos triângulos, oclusão, recorte espacial nos três mapas, invalidação do minimapa e recuperação adaptativa.
- Comparação antes/depois e limites da medição em `DESEMPENHO_1.6.1.md`. Saída integral dos testes em `VERIFICACAO.txt`.

Os arquivos de arte da 1.6 continuam incluídos. Esta correção não acrescenta agentes, mapas ou modos de jogo.
