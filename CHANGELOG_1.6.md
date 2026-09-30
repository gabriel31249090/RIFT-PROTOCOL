# RIFT Protocol 1.6 — Impacto e identidade

## Combate e armas

- Sons próprios para as 15 armas: ataque inicial, corpo grave, ruído, mecanismo e cauda de reverberação sintetizados, com três pequenas variações por arma. Não utiliza samples de VALORANT.
- Sons de retirada do carregador, encaixe e ferrolho sincronizados às etapas da recarga.
- Impulso visual do modelo, peças móveis, clarão com transparência, partículas de impacto, cápsulas que colidem com o cenário e marcas nas superfícies atingidas. O limite de 32 cápsulas e 80 marcas controla o custo visual.
- Materiais bitmap aplicados às armas e acessórios. Echo recebeu carregador curvo e guarda de madeira; Helix, carregador traseiro; Wisp, coronha própria; Horizon, ferrolho móvel; Bastion, munição aparente.
- Mira alternável por clique direito como padrão nas armas com luneta. Soltar o botão mantém a mira; o segundo clique fecha. A troca de arma e a morte limpam o estado. Durante a recarga, a luneta fica temporariamente oculta.
- Configurações separadas para a mira da sniper e a das outras armas. Movimento de câmera reduzido também reduz o novo impulso visual. Efeitos cosméticos usam aleatoriedade separada da precisão dos tiros.

## Física de arremesso

A instrução de retorno linear da 1.5 foi substituída conforme a correção do pedido. A colisão reflete a componente normal da velocidade e aplica perda de energia e atrito; a componente tangencial continua no sentido físico do ricochete. A gravidade permanece ativa após o impacto. A varredura considera o raio do projétil, paredes, piso, rampas e barreiras para reduzir atravessamentos em alta velocidade. O pavio continua contando durante os ricochetes.

O clarão curvo abandona sua curva guiada ao bater e segue balística. Foguetes explodem ao contato. Véu Rápido abre ao tocar uma superfície ou ao concluir seu percurso rápido. São parâmetros próprios de um jogo, sem reprodução exata de um motor físico externo.

## Trama: fios

- Q inicia uma prévia entre duas paredes reais. A primeira parede deve estar até 18 m; o vão entre as pontas pode ter 0,6 a 12 m. A altura acompanha a mira, de 0,25 a 2,3 m.
- Clique esquerdo ou Q confirma. Direito, Esc ou troca de arma cancela. Abrir a prévia e apontar para uma posição inválida não consomem carga.
- O fio arma em 0,45 s. A detecção considera todo o deslocamento do personagem no quadro, inclusive cruzamentos rápidos.
- Ao cruzar, prende e revela; após 0,9 s, causa 15 de dano e 1,8 s de atordoamento. Destruir uma das âncoras de 45 PV durante o aviso impede o dano final.
- Saltar sobre um fio baixo e agachar sob um fio alto permitem escapar. A prévia termina se o jogador morrer, for suprimido ou ficar detido.

## Interfaces de habilidades

| Agente | Instrumento | Alcance e função |
|---|---|---|
| Bruma | Radar circular laranja, orientação da câmera fixada ao abrir | Q marca até três smokes em 26 m. X posiciona Ruptura em 36 m. |
| Cáustica | Painel químico retangular, norte fixo | E posiciona Manto Tóxico em 22 m, uma marca por uso. |
| Espectro | Instrumento astral violeta, norte fixo | E posiciona Eclipse em 30 m; X usa o mapa inteiro para Travessia. |
| Vértice | Lançamento direto | C continua sem painel e abre em até 0,42 s ou no primeiro contato. |

Cáustica E e Espectro E passam do lançamento ao posicionamento pelo painel. Seus efeitos conservam funções diferentes: nuvem tóxica oca e vórtice de redução de visão, respectivamente. Os instrumentos mostram a geometria real do cenário, preservam a correspondência entre clique e mundo e só gastam a habilidade ao confirmar um destino válido.

## Artes e gráficos

- Ilustração original de menu, onze retratos, 44 ícones de habilidades e quatro emblemas de reserva no atlas.
- Atlas de 16 materiais para paredes, pisos, coberturas, contêineres e modelos de armas; cartazes ilustrados no cenário.
- Quatro sprites com transparência para clarão de disparo, poeira, faíscas e vapor.
- Texturização com perspectiva, recorte próximo da câmera e cinco níveis de detalhe. Filtragem do piso próximo reduz blocos visíveis. Paletas pequenas evitam recriar texturas inteiras conforme a iluminação e a névoa mudam.
- Opções para desativar texturas de superfícies 3D e marcas/cápsulas. As artes da interface e o piso permanecem disponíveis. O menu não renderiza mais uma cena 3D por trás da ilustração.
- PNGs e fontes incluídos; os recursos também estão dentro do JAR. Não há download de conteúdo durante o jogo.

## Compatibilidade e verificação

Java 17 ou superior. O perfil existente continua sendo usado, incluindo contratos, coleção, sensibilidade e teclas. As opções novas recebem valores padrão quando não existem no perfil antigo.

O pacote continua sendo um protótipo desktop offline com bots, três mapas, onze agentes, quinze armas e três lâminas. Esta atualização não acrescenta servidores, mapas, agentes ou modos online. A lista de testes, medições e limitações está em `VERIFICACAO.txt`; o catálogo de artes está em `ARTES_1.6.md`.
