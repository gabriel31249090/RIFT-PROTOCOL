# Artes bitmap da versão 1.6

As imagens foram geradas para RIFT Protocol com o gerador de imagens integrado. As referências visuais enviadas na conversa orientaram a direção artística; não foram incluídas como assets. O pacote usa personagens, nomes, símbolos e cenários próprios. O código continua responsável pela geometria, colisões, interface e leitura dos atlas; o detalhe pictórico vem dos PNGs.

## Arquivos e uso

| Arquivo | Dimensões / canais | Uso |
|---|---|---|
| `assets/menu-keyart.png` | 1672 × 941 / RGB | Ilustração principal do menu e fundo da seleção. |
| `assets/materials.png` | 1254 × 1254 / RGB | Atlas 4 × 4 de superfícies para o cenário e modelos. |
| `assets/abilities.png` | 1448 × 1086 / RGB | Atlas 8 × 6: 44 habilidades e quatro emblemas de reserva. |
| `assets/agents.png` | 1448 × 1086 / RGB | Atlas 4 × 3: onze retratos e um emblema de equipe. |
| `assets/effects.png` | 1254 × 1254 / RGBA | Atlas 2 × 2 com transparência: disparo, poeira, faíscas e vapor. |

Os arquivos são embutidos em `RiftProtocol.jar` por `Build.java`; funcionam mesmo quando o JAR é iniciado fora da pasta do projeto. Nenhum serviço de imagens é chamado durante o jogo. Para modificar uma imagem, preserve o nome e a grade do atlas, depois execute `java Build.java` com JDK 17+.

## Materiais

Leitura da esquerda para a direita e de cima para baixo. Os índices abaixo começam em zero, como em `Assets.java`.

| Linha | Coluna 1 | Coluna 2 | Coluna 3 | Coluna 4 |
|---|---|---|---|---|
| 1 | 0: reboco | 1: concreto | 2: tijolo | 3: pedra |
| 2 | 4: chapa azul-petróleo | 5: chapa oxidada | 6: aço | 7: latão |
| 3 | 8: madeira | 9: piso de arenito | 10: asfalto | 11: chapa antiderrapante |
| 4 | 12: borracha | 13: tecido | 14: vidro | 15: cobertura de concreto |

O renderizador deriva cinco níveis de detalhe de cada célula e ajusta sua tonalidade à superfície ou skin. Uma paleta por luminância reduz o custo de coloração; não altera os PNGs de origem. A malha e os UVs continuam determinando a posição exata de paredes, rampas e armas.

## Retratos

Grade 4 × 3: Vértice, Bruma, Íon, Cáustica; Espectro, Solar, Estopim, Bastilha; Vigia, Áureo, Trama, emblema de reserva. Retratos também aparecem nos painéis de habilidades e em cartazes do cenário.

## Ícones

As primeiras 44 células seguem `Game.Ability.values()`, uma associação própria para cada habilidade. As células 45–48 são emblemas de reserva. A ordem do atlas não é a ordem das teclas Q/E/C/X do HUD.

| Célula | Identificador no código | Habilidade |
|---|---|---|
| 1 | DASH | IMPULSO |
| 2 | SMOKE | NÉVOA |
| 3 | HEAL | REPARO |
| 4 | SCAN | PULSO |
| 5 | FLASH | CLARÃO |
| 6 | FOCUS | FOCO |
| 7 | ORBITAL | RUPTURA |
| 8 | SURGE | RESSONÂNCIA |
| 9 | QUICK_SMOKE | VÉU RÁPIDO |
| 10 | UPDRAFT | ASCENSÃO |
| 11 | STIM | ACELERADOR |
| 12 | INCENDIARY | BRASA |
| 13 | SUPPRESS | SILÊNCIO |
| 14 | TOXIC_WALL | CORTINA ÁCIDA |
| 15 | POISON_CLOUD | MANTO TÓXICO |
| 16 | ACID | CORROSÃO |
| 17 | TOXIC_DOME | BIOSFERA |
| 18 | BLINK | PASSO SOMBRIO |
| 19 | BLIND_WAVE | PESADELO |
| 20 | GLOBAL_TELEPORT | TRAVESSIA |
| 21 | CURVEFLASH | CENTELHA |
| 22 | HEAL_FIRE | REACENDER |
| 23 | FIRE_WALL | LABAREDA |
| 24 | RETURN | RENASCER |
| 25 | SATCHEL | PROPULSOR |
| 26 | FRAG | ESTILHAÇO |
| 27 | HUNTER_BOT | RASTREADOR |
| 28 | ROCKET | DEMOLIÇÃO |
| 29 | BARRIER | BASTIÃO |
| 30 | SLOW | GEADA |
| 31 | REVIVE | REANIMAR |
| 32 | TURRET | TORRETA |
| 33 | TRAP | ARMADILHA |
| 34 | SENSOR | SONAR |
| 35 | LOCKDOWN | CONFINAMENTO |
| 36 | ECLIPSE | ECLIPSE |
| 37 | VERDICT | VEREDITO |
| 38 | BULWARK | APÓLICE |
| 39 | ANCHOR | ÂNCORA |
| 40 | RAIL | EXECUÇÃO |
| 41 | TRIPWIRE | FIO DE ALARME |
| 42 | CAGE | REDE SURDA |
| 43 | SPYCAM | OLHO REMOTO |
| 44 | NETWORK | RASTREAMENTO |

## Direção de geração e prompts de reconstrução

Modo: criação de assets raster com o gerador integrado. Os textos abaixo resumem as especificações usadas e servem como prompts de reconstrução; não são uma transcrição literal das chamadas. As imagens finais são a referência visual entregue nesta versão.

**Menu:** “Create an original premium tactical FPS key art, wide 16:9 composition. Keep the left 40 percent dark navy and quiet for menu typography. Place three adult original operatives on the right: a female blue intelligence specialist with a half mask and cyan optical lens, a brown-skinned gold-accented marksman, and a white-haired mint-accented wind runner. Industrial harbor at sunset, dramatic angular painterly illustration, controlled mint, gold and navy palette, detailed costume materials. No text, no logos, no existing characters.”

**Materiais:** “Create a flat orthographic albedo texture atlas in an exact equal 4 by 4 grid, no borders, no labels, no perspective. Row one: plaster, concrete, brick, stone. Row two: teal corrugated metal, rusty corrugated metal, brushed steel, brass. Row three: timber, sandstone paving, asphalt, diamond plate. Row four: rubber, woven cloth, smoked glass, concrete roof. Subtle believable wear, readable medium-scale detail, balanced neutral lighting, suitable for an original stylized tactical game.”

**Habilidades:** “Create an exact equal 8 by 6 icon atlas on deep navy #10232f. Each cell contains one distinct illustrated tactical ability symbol with clear small-scale silhouette and theme-colored lighting. Use the 44 abilities in the mapping table in order, then four original crests. Depict the appropriate wind, smoke, heal, scan, flash, fire, chemical, spectral, explosive, ice, machinery, golden weapon or blue surveillance object. No lettering, no labels, no visible cell borders.”

**Agentes:** “Create an exact equal 4 by 3 atlas of cohesive original adult tactical-agent portraits. Row one: mint wind runner, dark green smoke controller, violet suppression specialist, yellow-green chemical specialist. Row two: violet masked spectral operative, orange fire duelist, red explosive specialist, cyan ice guardian. Row three: golden mechanical sentinel, elegant gold marksman, blue intelligence operative with a half mask, and an original team crest. Painterly angular shapes, detailed clothing, unique faces, consistent portrait scale, no text or labels.”

**Efeitos:** “Create a 2 by 2 VFX sprite atlas on a truly transparent background: warm muzzle flash upper left, soft gray impact dust upper right, hot orange-white sparks lower left, cyan vapor lower right. Separate effects centered inside their equal cells, soft alpha edges, no frames, no lettering, no opaque background.”

## Limites visuais

As ilustrações detalhadas do menu não são modelos 3D. Os personagens e armas em partida continuam usando malhas geométricas leves, agora com materiais e efeitos adicionais. Placas, callouts, linhas de interface, geometria exata dos mapas de habilidade e alguns efeitos animados continuam desenhados por código para manter legibilidade e precisão.
