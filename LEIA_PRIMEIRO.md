# RIFT Protocol 1.6

FPS tático 3D original em Java, com **onze agentes, três mapas táticos com alturas, 15 armas de fogo, três lâminas e seis modos locais contra bots**. JAR compilado, código-fonte, testes, capturas incluídas. Não usa internet nem dependências externas durante o jogo.

## Novidades da versão 1.6 — Impacto e identidade

- **Tiros mais presentes:** sons sintetizados em camadas diferentes para as 15 armas, cápsulas com física, marcas de impacto, partículas com transparência, clarão na boca do cano e impulso visual do modelo. Recargas têm sons por etapa e peças móveis.
- **Sniper alternável:** um clique direito abre a luneta; outro fecha. Funciona nas armas com luneta, inclusive Execução. A opção de segurar continua disponível em Configurações → Acessibilidade → Mira da sniper.
- **Ricochetes corrigidos:** arremessáveis refletem a velocidade na normal da superfície, conservam o sentido tangencial com atrito e perdem energia. A gravidade continua após a colisão. Foguetes detonam ao contato e Véu Rápido abre ao tocar uma superfície.
- **Fio da Trama reconstruído:** Q mostra a instalação entre paredes; mire na altura desejada e clique para confirmar. Detecta cruzamentos rápidos, prende e revela, com um aviso antes do dano e atordoamento. Destruir uma das âncoras permite escapar.
- **Três instrumentos de posicionamento:** radar alaranjado da Bruma, painel químico da Cáustica e visão astral do Espectro. As posições correspondem ao mapa real. Vértice continua lançando sua smoke diretamente pela mira.
- **Arte bitmap integrada:** menu ilustrado, retratos dos 11 agentes, 44 ícones de habilidade, 16 materiais para superfícies e armas, cartazes nos mapas e sprites de efeitos. Todos os PNGs estão incluídos e embutidos no JAR.
- **Opções gráficas:** interruptores para texturas 3D e marcas/cápsulas. O menu usa a ilustração sem renderizar um cenário 3D oculto. As texturas usam níveis de detalhe e paletas de cor em cache.

Continuam disponíveis os três mapas táticos, bhop, corpo a corpo, coleção de skins/pingentes, economia, progressão e seis modos locais da versão 1.5. Consulte `CHANGELOG_1.6.md` para os detalhes desta atualização.

## Jogar no Windows

1. Extraia o ZIP inteiro.
2. Tenha Java 17 ou superior instalado e disponível no PATH.
3. Abra **INICIAR_WINDOWS.bat**.
4. Clique em **JOGAR → escolha modo e mapa → CONFIRMAR → ACEITAR → escolha agente → TRAVAR**.
5. Aguarde os bots travarem e o mapa carregar.

Você também pode abrir **CAMPO DE TREINO**: todos os agentes e armas ficam disponíveis, sem custo. Na seleção, clique nos 11 cartões; as teclas 1 a 9 selecionam os nove primeiros. Enter confirma. Use G para trocar durante o treino ou a compra.

No Linux/macOS: `sh iniciar.sh`. É necessário um ambiente gráfico com o módulo `java.desktop`. Java não está incluído. Se o JAR não abrir com duplo clique, use o iniciador ou `java -jar RiftProtocol.jar`.

## Modos

| Modo | Regras locais |
|---|---|
| Competitivo | 5 × 5, alvo de 7 vitórias. Em 6:6 exige vantagem de duas; troca de lados a cada rodada extra, 5.000 créditos por rodada. Na rodada 19, morte súbita. Classificação local; troca normal após 6 rodadas. |
| Sem Ranque | 5 × 5, primeiro a 5. Troca de lados após 4 rodadas. |
| Disputa da Spike | Primeiro a 4, até 7 rodadas. Compra de 8 s, combate de 100 s e mesma arma para todos por rodada. Kits completos e supremas carregadas. |
| Mata-Mata | Você e nove bots, todos contra todos. Primeiro a 20 abates ou maior placar em 5 minutos. |
| Team Deathmatch | 5 × 5, primeiro a 40 abates por equipe ou maior placar em 5 minutos. |
| Premier | Copa local com semifinal e final. Cada partida vai até 4 vitórias; vencer a semifinal abre a final em outro mapa. |
| Campo de treino | Cinco alvos com respawn; armas, munição de reserva e habilidades livres. É preciso recarregar o pente. |

Nos dois modos de abate, o respawn leva 3 s e B abre o arsenal gratuito durante o combate. A proteção de respawn dura 1,5 s e termina ao atirar. Não há Spike nesses modos.

Nos modos de rodadas: plantio com F por 3,2 s, desarme por 5 s e detonação após 40 s. Compra normal de 20 s e combate de 140 s. Enter adianta o início da rodada. A Spike armada mantém a rodada mesmo se os atacantes morrerem.

**A fila, a classificação e a copa são locais. Os outros participantes são bots.** Este pacote não implementa servidores multiplayer, matchmaking online ou chat de voz. O ping funciona no mundo, no minimapa e nas ordens dos aliados.

## Mapas e movimento

Os três mapas mantêm a área externa de 144 × 128 metros. Desde a versão 1.5, a área circulável se organiza entre massas sólidas de edifícios: aproximações de 7–8 m, conexões de rotação, cobertura e duas salas de objetivo. São layouts originais, não cópias de mapas oficiais.

- **Cais-7:** aproximações diretas pelos flancos, oficina ligada ao meio e docas elevadas.
- **Monte Aurora:** acessos em cotovelo que quebram as linhas longas, ligados à rota defensora.
- **Ferrovia:** conexões extras ao redor do meio e cobertura diferente no ponto B.

Há três plataformas por mapa: duas de 2,4 m e uma posição de precisão a 4,2 m. Rampas afetam a altura real, os tiros e a navegação dos bots. O HUD mostra o nome da área e o ping informa o callout correspondente.

### Bhop e air strafe

Comece correndo, salte, use A ou D e acompanhe suavemente com o mouse para orientar o movimento no ar. A aceleração depende da projeção da velocidade na direção desejada: virar demais ou manter a mesma direção não dá o mesmo ganho. Ao pousar, pressione Espaço novamente para conservar o impulso. É possível apertar até 100 ms antes do pouso. **Segurar Espaço não ativa salto automático.** Ficar no chão aplica frenagem. O limite é 11,5 m/s e acertar tiros no ar continua impreciso.

No treino, **T** mostra velocidade atual, melhor velocidade e saltos encadeados. Treine nos corredores; colisões continuam bloqueando o movimento.

Os personagens usam um esqueleto articulado: transição de velocidade, rotação gradual do corpo, joelhos, cotovelos, contrarrotação dos ombros, respiração, salto, agachamento, recarga, reação ao clarão e queda. As capturas atualizadas ficam na pasta `screenshots`.

## Agentes e teclas

| Agente / função | Q | E | C | X: suprema |
|---|---|---|---|---|
| **Vértice / Duelista** | Impulso: dash de 7 m em 0,40 s | Ascensão: impulso vertical | Véu Rápido: smoke pela mira, sem mapa | Foco: cadência, precisão, movimento e recarga melhores |
| **Bruma / Controladora** | Névoa: até 3 smokes pelo mapa | Acelerador: área de cadência e velocidade | Brasa: granada incendiária | Ruptura: bombardeio orbital escolhido no mapa |
| **Íon / Iniciador** | Pulso: revela inimigos | Silêncio: disco de supressão | Clarão: granada com ricochete balístico | Ressonância: cura e proteção para a equipe |
| **Cáustica / Controladora** | Cortina Ácida: parede tóxica de visão | Manto Tóxico: nuvem pelo painel químico | Corrosão: poça de dano e vulnerabilidade | Biosfera: grande smoke tóxica de 16 s |
| **Espectro / Controlador** | Passo Sombrio: teleporte curto canalizado | Eclipse: vórtice pela visão astral; reduz visão e apaga marcação | Pesadelo: onda que reduz visão | Travessia: teleporte global pelo mapa |
| **Solar / Duelista** | Centelha: clarão curvo | Reacender: fogo que cura o usuário | Labareda: parede de fogo | Renascer: retorna vivo à posição marcada |
| **Estopim / Duelista** | Propulsor: impulso explosivo | Estilhaço: granada com explosões sucessivas | Rastreador: robô perseguidor | Demolição: foguete de alto dano em área |
| **Bastilha / Sentinela** | Bastião: barreira física destrutível | Reparo: cura própria ou de aliado na mira | Geada: campo de lentidão | Reanimar: ressuscita aliado caído próximo à mira |
| **Vigia / Sentinela** | Torreta: defesa automática destrutível | Armadilha: lentidão e revelação | Sonar: detecta corrida e tiros | Confinamento: dispositivo desarma inimigos após aviso |
| **Áureo / Sentinela** | Veredito: pistola especial de 6 tiros | Âncora: posicionar e retornar até 22 m | Apólice: escudo frontal de tiros | Execução: fuzil especial de 5 tiros |
| **Trama / Sentinela** | Fio de Alarme: detecta cruzamento de passagem | Olho Remoto: câmera montada em parede | Rede Surda: gaiola de visão e alarme | Rastreamento: duas varreduras a partir de um corpo inimigo |

Vértice, Bruma, Íon, Áureo e Trama são gratuitos. Os seis agentes introduzidos na versão 1.3 custam 300 XP cada. Um perfil novo recebe **1.800 XP iniciais**, suficientes para liberar os seis sem precisar jogar antes. Clique no agente e em DESBLOQUEAR; depois, TRAVAR. No treino, todos são livres independentemente do contrato.

Cada suprema custa 6 pontos, obtidos com abates, plantio, desarme, orbes e rodadas. No treino são livres. Receber dano interrompe cura gradual e canalização de teleporte. Foco dura 12 s, melhora cadência em 30%, reduz dispersão em 82% e recarga em 40%; abates estendem o efeito em até 2 s, limitado a 12 s restantes.

### Smokes e posicionamento

| Agente / habilidade | Instrumento e orientação | Uso |
|---|---|---|
| Vértice / C | Lançamento direto pela mira | Chega em até 0,42 s ou abre no primeiro contato; dura 4,5 s. Não abre mapa. |
| Bruma / Q | Radar circular alaranjado; frente da câmera para cima, orientação fixada ao abrir | Marque até três posições em piso livre a até 26 m; cada smoke dura 12 s. |
| Cáustica / E | Painel químico retangular; norte fixo | Uma posição em piso livre a até 22 m; nuvem venenosa de 10 s, raio de 3,8 m. |
| Espectro / E | Visão astral circular; norte fixo | Uma posição em piso livre a até 30 m; vórtice de 9 s, raio de 4 m, que reduz a visão e apaga marcações. |

Nos painéis: **clique marca/remove; Enter confirma; direito desfaz; Esc cancela sem gastar**. A habilidade é cobrada ao confirmar uma posição válida. Ruptura (Bruma / X, 36 m) e Travessia (Espectro / X, mapa inteiro) também usam o instrumento do próprio agente. A partida continua enquanto o mapa está aberto.

O interior das smokes é oco: você pode ver personagens próximos dentro da mesma esfera. A casca bloqueia a visão entre interior e exterior, inclusive para bots. Tiros atravessam fumaças. Eclipse mantém sua função de redução de visão e não cria uma esfera de fumaça comum.

### Clarões

O **C do Íon** arremessa uma granada que ricocheteia em paredes e no piso, sob gravidade, e detona após 1,1 s. Segure a mira ao lançar para fazer um arremesso curto de 0,65 s. O **Q de Solar** faz uma curva à direita; mirando, curva à esquerda. Pode cegar você e aliados. Paredes e smokes bloqueiam o clarão; virar de costas reduz seu efeito.

## Economia, armas e habilidades

Vitória: 3.000 créditos. Derrota: 1.900, aumentando em derrotas consecutivas até 2.900. Abate: 200. Plantio: 300 para o portador. Desarme: 300 para quem desarma. Limite: 9.000. Troca de lados reinicia a economia em 800.

Abra B, selecione uma arma e clique em COMPRAR. Pistola e principal ocupam slots diferentes. Sobreviver preserva as duas armas; morrer remove as armas compradas. Recomprar a mesma arma não repõe munição. Proteções de 25 e 50 custam 400 e 1.000 créditos.

A primeira rodada fornece um kit completo. Depois, Q e C mantêm as cargas restantes; E é renovada. A loja repõe cargas de habilidades por 200 créditos cada. Quando E está vazia, recupera uma carga em 30 s; âncora e câmera esperam o dispositivo anterior deixar de existir. Treino e Disputa da Spike renovam o kit completo. A troca de agente com G também fornece o kit escolhido, como conveniência desta edição local.

| Arma | Categoria / disparo | Custo | Pente / reserva | Dano corpo / cabeça, de perto |
|---|---|---:|---:|---:|
| Spark | Pistola semiautomática | 0 | 12 / 48 | 26 / 82 |
| Veil | Pistola semiautomática silenciada | 500 | 15 / 60 | 30 / 105 |
| Talon | Revólver semiautomático | 800 | 6 / 30 | 55 / 159 |
| Rush | Pistola automática | 450 | 20 / 80 | 22 / 70 |
| Dart | Pistola de dois canos, 12 projéteis | 350 | 2 / 20 | 11 / 18 por projétil |
| Wisp | Submetralhadora automática | 1.600 | 24 / 96 | 23 / 69 |
| Circuit | Submetralhadora automática de alta cadência | 1.200 | 32 / 96 | 21 / 63 |
| Echo | Fuzil automático | 2.700 | 25 / 75 | 35 / 156 |
| Shade | Fuzil automático silenciado | 2.900 | 30 / 90 | 31 / 124 |
| Helix | Fuzil com rajada de três tiros | 2.100 | 24 / 72 | 34 / 132 |
| Marrow | Escopeta por bombeamento, 9 projéteis | 1.850 | 8 / 24 | 13 / 24 por projétil |
| Breach | Escopeta automática, 10 projéteis | 2.200 | 7 / 28 | 10 / 19 por projétil |
| Ridge | Fuzil semiautomático com luneta | 2.250 | 12 / 36 | 65 / 195 |
| Horizon | Precisão pesada com luneta | 4.200 | 5 / 20 | 100 / 170 |
| Bastion | Metralhadora automática | 3.200 | 75 / 150 | 32 / 104 |

## Corpo a corpo e coleção

Abra **COLEÇÃO** no menu principal ou na pausa; na loja, use **SKINS E PINGENTES**. Selecione a arma, o acabamento e o pingente, confira a prévia 3D e clique em **EQUIPAR**. A opção **APLICAR ÀS 15 ARMAS** usa a mesma combinação em todas. Tudo é gratuito; não há compra com dinheiro nem sorteios.

Acabamentos: **Padrão, Carbono, Aurora, Pulso, Brasa e Regente**. Pingentes: **Cristal, Dado, Minibot, Estrela e Núcleo**, além da opção de remover. O pingente balança com aceleração, movimento do mouse e disparos, retornando gradualmente ao repouso. A seleção fica salva por arma e aparece no modelo da primeira pessoa e na prévia da loja. Cosméticos não alteram dano, precisão ou cadência.

Na aba **LÂMINAS**, escolha **Fio** (faca), **Arco** (karambit) ou **Ruptor** (machadinha). As três possuem o mesmo alcance, dano e velocidade; o desenho é diferente. Use **3** em partida para equipar, **1** para pistola e **2** para principal.

| Golpe | Dano frontal / costas, antes da proteção | Alcance até a superfície do alvo | Contato / intervalo |
|---|---:|---:|---:|
| Esquerdo: corte | 50 / 100 | 2,15 m | 0,13 / 0,46 s |
| Direito: golpe forte | 75 / 150 | 1,85 m | 0,26 / 0,88 s |

O corte repete segurando o botão; o golpe forte exige um novo clique. Golpes exigem alvo no arco à frente e linha de visão, respeitam paredes e podem atingir dispositivos destrutíveis. Não consomem munição. Trocar de arma cancela um golpe pendente. Com lâmina, a corrida chega a 6,2 m/s; com armas de fogo, a base é 5,4 m/s multiplicada pela mobilidade de cada arma. Shift caminha silenciosamente; Ctrl agacha.

**V** inspeciona a arma ou lâmina. Atirar, mirar ou recarregar interrompe a inspeção. O treino mostra o estado da precisão, e o indicador vermelho junto à mira mostra a direção do dano recebido. As animações de inspeção, recuo, recarga e golpes continuam integradas ao modelo em primeira pessoa.

## Controles

| Entrada | Ação |
|---|---|
| WASD / Mouse | Mover / olhar |
| Botão esquerdo | Atirar / corte com lâmina; no mapa, marcar ou remover |
| Botão direito | Alternar luneta da sniper; segurar mira das demais armas; golpe forte com lâmina; no mapa, desfazer |
| R / 1 / 2 / 3 | Recarregar / pistola / principal / corpo a corpo |
| V | Inspecionar arma ou lâmina |
| Shift / Ctrl / Espaço | Andar / agachar / pular |
| Q / E / C / X | Três habilidades e suprema |
| F segurado | Plantar ou desarmar |
| B | Arsenal na compra, treino e modos de abate |
| G / H | Trocar agente; H simula dano no treino |
| Z | Ping de posição |
| T, no treino | Mostrar/ocultar indicadores de bhop |
| K / L, na compra | Recomprar loadout / pedir créditos |
| F10 / F1 | Configurações rápidas / guia |
| Enter | Aceitar, travar, confirmar mapa ou adiantar rodada |
| Tab / Esc | Placar / pausa ou voltar |
| F11 ou Alt+Enter | Tela cheia |
| F12 | Captura em `captures` |
| Setas | Alternativa ao mouse para olhar |

**Luneta:** clicar e soltar mantém a mira. Clique direito novamente para fechar. Trocar de arma ou morrer desativa a luneta; a recarga a oculta temporariamente. A opção de alternar para as outras armas é independente.

Semiautomáticas disparam uma vez por clique. Automáticas disparam enquanto o botão estiver pressionado. A Helix completa uma rajada de três tiros por clique. Pare de correr para melhorar a precisão; agachar e mirar reduzem dispersão. Disparos longos exigem compensar o recuo com o mouse. A frenagem atravessa um limiar de precisão a 27,5% da velocidade de corrida da arma; tiros no ar e logo após aterrissar recebem penalidade. O recuo se recupera ao pausar a rajada. Horizon tem dispersão maior sem usar a luneta.

## Progressão

CARREIRA LOCAL mostra XP, vitórias, abates, ACEs, maestria por agente e histórico competitivo. Cada partida concluída dá 300 XP + 20 por abate + 200 por vitória. Cada 800 XP no mesmo agente concede um nível de maestria.

Competitivo local: +28 pontos por vitória e −20 por derrota, com nove divisões de Ferro a Radiante. Não corresponde a um rank online. A copa recompensa cada etapa concluída. ACE exige os cinco inimigos diferentes na mesma rodada e não é concedido no treino nem nos modos com respawn.

Preferências, coleção e progressão são salvas em `.rift-protocol` dentro da pasta do usuário do sistema. Não é necessário copiar os arquivos da versão anterior: a atualização utiliza a mesma pasta de preferências.

## Desempenho e problemas comuns

- A qualidade **AUTO** ajusta a resolução interna do cenário conforme o tempo de renderização. A interface permanece na resolução da janela.
- Configurações permitem qualidade baixa, média ou alta fixa e limite de 60, 120, 144 ou 240 FPS. O padrão é AUTO e 120 FPS. Distância: 60, 100, 160 ou 210 m; sombras podem ser desligadas.
- Se houver lentidão, escolha AUTO ou BAIXA e limite 60 FPS. Reduza a distância, desligue sombras, texturas de materiais e marcas/cápsulas se necessário. As ilustrações da interface e o piso continuam visíveis.
- O renderizador elimina faces escondidas antes de desenhá-las, usa menos cálculos por smoke e evita alocações temporárias nas interseções de tiro.
- O código não depende da GPU: é um renderizador 3D por software. Desempenho varia conforme CPU, resolução e sistema.
- Alt+Tab pausa a partida. Se o mouse não for capturado, clique na janela; as setas também permitem olhar.
- Configurações → CONTROLES permite remapear teclas. F1 abre o guia. Um erro inesperado é registrado em `rift-error.log`.

Os testes de desenvolvimento rodam sem janela. Janela nativa, áudio, captura do mouse e fullscreen precisam ser conferidos no seu sistema. Resultados e medições estão em `VERIFICACAO.txt`.

## Código e verificação

Para recompilar com um JDK 17+: `java Build.java`, `COMPILAR_WINDOWS.bat` ou `sh compilar.sh`.

```sh
java -jar RiftProtocol.jar --self-test
java -jar RiftProtocol.jar --benchmark
java -jar RiftProtocol.jar --impact-test
java -jar RiftProtocol.jar --impact-capture screenshots
```

O projeto usa apenas a biblioteca padrão Java. Os mapas, agentes, modelos e sons são próprios. As novas artes bitmap foram geradas para o projeto; formatos, mapeamento e especificações estão em `ARTES_1.6.md`. É um protótipo independente inspirado em FPS táticos, sem vínculo com Riot Games.

Referências de design consultadas: [notas oficiais 0.50, de 2020](https://playvalorant.com/en-gb/news/game-updates/valorant-patch-notes-0-50/) e [3.0, de 2021](https://playvalorant.com/en-us/news/game-updates/valorant-patch-notes-3-0/), sobre recuperação entre disparos e precisão em movimento. Os parâmetros e o balanceamento de RIFT são próprios; essas referências históricas não representam uma reprodução exata da versão atual de VALORANT.

## Como usar os sentinelas Áureo e Trama

**Áureo:** Q invoca Veredito (70 corpo / 160 cabeça); X invoca Execução (150 / 300). Ambos têm munição limitada, sem recarga, inclusive no treino. Repetir Q ou X reequipa a arma existente sem consumir carga ou suprema enquanto houver tiros. Usar 1, 2 ou 3 volta ao inventário normal. As armas de habilidade não recebem skins ou pingentes. C posiciona um escudo de 250 PV por 12 s: jogadores atravessam, tiros inimigos atingem o escudo. E posiciona uma âncora de 80 PV; E novamente retorna até 22 m após 0,35 s. Receber dano interrompe a canalização. Destruir a âncora impede o retorno.

**Trama:** Q abre a prévia. Mire numa parede até 18 m: a outra ponta procura uma parede oposta, com vão entre 0,6 e 12 m. A altura acompanha a mira entre 0,25 e 2,3 m. Clique esquerdo ou Q instala; direito ou Esc cancela sem custo. Trocar de arma também cancela. O fio arma em 0,45 s e dura até 180 s, com 45 PV. Cruzar prende e revela por 4 s; após um aviso de 0,9 s, causa 15 de dano e atordoa por 1,8 s. Saltar por cima ou agachar sob um fio alto evita a ativação; destruir qualquer ponta durante o aviso evita o dano final. C posiciona um emissor; C novamente ativa os emissores pendentes por 7 s. A gaiola cilíndrica tem interior oco, bloqueia visão pela borda e alerta quando um inimigo cruza. E instala uma câmera na superfície apontada, até 18 m. E novamente entra na câmera: mova o mouse e clique para marcar um inimigo visível por 5 s, com intervalo de 3 s. E ou Esc sai. O corpo permanece no lugar e pode morrer. X requer um corpo inimigo até 12 m; revela globalmente por 1,2 s, repete após 3 s. Não transmite a posição continuamente.

## Configurações e acessibilidade

Cinco abas: Controles, Gráficos, Áudio, Gameplay e Acessibilidade. Teclas repetidas trocam de função para evitar conflitos. Esc, Enter e F1/F10/F11/F12 são reservadas. O mouse mantém esquerdo para disparo e direito para mira. F10 abre/fecha preferências pausando a partida local.

Sensibilidade normal e multiplicador ADS são separados. FOV horizontal vai de 70° a 110°. A mira permite tamanho, abertura, espessura, quatro cores, modo fixo/dinâmico e ponto central. Mapa de habilidade e câmera podem alternar com a tecla ou funcionar enquanto ela estiver segurada; no mapa, soltar confirma os pontos marcados.

Acessibilidade: ADS, caminhada e agachamento alternáveis; câmera com menos balanço; clarão escuro com a mesma perda de visão; legendas de efeitos; HUD com alto contraste; inversão vertical. Volumes master, efeitos e música são independentes. A trilha ambiente sintetizada toca somente nos menus. Disparos de bots usam panorama estéreo, distância e atenuação por paredes; isso não é um sistema acústico completo de áudio 3D.

## Economia e tiros

Na loja, SALVAR LOADOUT guarda pistola e arma principal. K tenta recomprá-las e proteção completa dentro do saldo disponível; não concede itens sem pagar. L solicita 600 créditos de um aliado que tenha saldo, uma vez por rodada. A transferência é entre os participantes locais, sem dinheiro externo.

Dano nas pernas equivale a 75% do dano corporal. Painéis finos de madeira podem ser atravessados conforme a arma e a espessura atravessada; o tiro perde dano e passa no máximo por dois painéis. Paredes estruturais, rampas e barreiras continuam sólidas. Isso permite counterplay contra alguém escondido atrás da cobertura leve, sem tornar qualquer parede penetrável.

Há dois orbes compartilhados por rodada, na Oficina e na Rota Defensora. Pare perto de um e segure F por 0,9 s para ganhar um ponto de suprema. Receber dano ou sair interrompe a coleta. Cada orbe só pode ser coletado uma vez por rodada.

## Escopo e próximos sistemas

A versão 1.6 melhora impacto, mira, ricochetes, fios e apresentação visual sobre o núcleo local já existente. Ainda não inclui servidores dedicados, netcode, matchmaking online, anti-cheat, amigos/party/voz, MMR de jogadores reais, temporadas online, replays, editor de mapas, Workshop, crossplay, APK ou monetização. Modos como Retake, Wingman, Escalation e Replication também ficam para futuras versões. O rank, a fila e o Premier existentes são simulações locais, identificadas assim na interface.

As habilidades compartilham infraestrutura de colisão, efeitos e dano, mas cada um dos 44 slots tem uma mecânica própria no elenco. Não há reprodução exata do balanceamento, dos modelos ou da física de VALORANT. O bhop usa uma implementação original de aceleração por projeção, inspirada no gênero Source/Quake, sem incorporar código desses motores.
