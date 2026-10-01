# Comparação de desempenho — RIFT 1.6.1

## Resultado no ambiente de desenvolvimento

Mesma cena de treino, trajetória de rotação da câmera, resolução interna e tamanho final de 1280 × 720. Java 17, mesma máquina Linux, execução sequencial, sem janela. Cinquenta quadros de aquecimento e 150 medidos por qualidade. Texturas, sombras e efeitos ativos nas duas versões. Sem gravação do profiler nestes números.

| Qualidade fixa | Resolução interna | 1.6, média | 1.6.1, média | 1.6, p95 | 1.6.1, p95 |
|---|---|---:|---:|---:|---:|
| Baixa | 640 × 360 | 35,3 FPS / 28,36 ms | 100,8 FPS / 9,93 ms | 37,05 ms | 13,01 ms |
| Média | 854 × 480 | 32,1 FPS / 31,20 ms | 85,1 FPS / 11,75 ms | 36,30 ms | 14,03 ms |
| Alta | 1066 × 599 | 25,8 FPS / 38,69 ms | 66,4 FPS / 15,06 ms | 45,47 ms | 19,67 ms |

O ganho do quadro completo foi de aproximadamente 2,6 a 2,9 vezes. A 1.6 usa ampliação bilinear; a 1.6.1 usa a nova opção RÁPIDA. Portanto, estes resultados combinam otimização do renderizador com uma troca de filtro visual. O filtro rápido deixa os pixels mais definidos; a opção SUAVE continua disponível.

Para separar os custos, o mesmo teste também mede o cenário 3D antes da composição: na qualidade média ele passou de 18,74 para 8,35 ms, com a mesma resolução e os mesmos materiais. Essa redução vem do descarte espacial, da rasterização e da reutilização de dados, independentemente da ampliação da janela.

**p95** é o tempo abaixo do qual ficaram 95% dos quadros medidos. Quanto menor, menores as demoras na maioria dos quadros.

## Teste completo incluído no jogo

O comando `java -jar RiftProtocol.jar --benchmark-full` usa 90 quadros de aquecimento e 240 medidos. Inclui o quadro completo uma vez por amostra. Seus resultados podem variar ligeiramente em relação à comparação acima, que intercala uma medição separada do renderizador.

| Configuração | Média | p95 | Resolução ao final |
|---|---:|---:|---|
| Baixa / rápida | 106,1 FPS / 9,42 ms | 12,43 ms | 640 × 360 |
| Média / rápida | 83,7 FPS / 11,95 ms | 14,86 ms | 854 × 480 |
| Alta / rápida | 64,6 FPS / 15,49 ms | 19,30 ms | 1066 × 599 |
| AUTO / rápida | 138,6 FPS / 7,21 ms | 10,25 ms | 384 × 216 |
| Média / suave | 43,6 FPS / 22,93 ms | 29,51 ms | 854 × 480 |

AUTO foi mais rápido porque reduziu a resolução. Esse valor não significa manter a qualidade média ou alta a 138 FPS. O benchmark mede capacidade sem esperar pelo limitador; a partida respeita o limite de FPS configurado.

## Como testar no seu PC

1. Extraia a nova versão em uma pasta própria.
2. Abra `INICIAR_MODO_DESEMPENHO.bat` para começar com o perfil leve.
3. Entre no mesmo mapa e modo em que observou a queda. Compare olhando na mesma direção e com condições semelhantes.
4. Confira o contador no canto superior direito. Ele informa FPS, ms e tamanho interno da imagem.
5. Se a queda continuar, rode `DIAGNOSTICO_FPS_WINDOWS.bat` e envie `DIAGNOSTICO_FPS.txt`, uma captura do contador e o modelo do processador. Isso permite distinguir custo da simulação, qualidade escolhida e apresentação da janela.

O modo de desempenho mantém texturas, reduz efeitos decorativos e salva as opções gráficas. Você pode reajustá-las em F10 → Gráficos. Para imagem mais suave, ative SUAVE sabendo que isso aumenta o custo por quadro.

## Limites

Não foi possível medir no hardware do jogador. Estes resultados não garantem recuperar os 150 FPS relatados. Os testes foram feitos com imagens em memória: não medem monitor, sincronização da janela, captura do mouse, drivers ou latência de entrada percebida. A cena de treino também não cobre toda combinação de bots e habilidades; a suíte funcional verifica o núcleo do jogo separadamente. A renderização continua por software na CPU.

## Ideias para depois da correção

Prioridades sugeridas, ainda não implementadas nesta versão:

- **Treino de recoil:** alvo que registra a rajada e compara o agrupamento, aproveitando os padrões de recuo existentes.
- **Retake local:** começar com a Spike plantada e praticar entrada, cobertura e desarme nos mapas atuais.
- **Caderno de lineups:** salvar posições de lançamento e visualizar a trajetória apenas no treino.
- **Relatório de combate:** mostrar dano por região, tiros acertados e distância no fim da rodada.

Essas propostas aproveitam os sistemas existentes e evitam adicionar muitas malhas e partículas antes de confirmar a estabilidade do FPS no computador do jogador.
