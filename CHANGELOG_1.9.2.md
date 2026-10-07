# RIFT Protocol 1.9.2 - Primeiro Piloto Visual

## Importacao e Assets

- javagl/Obj 0.4.0 MIT fixado por checksum, compilacao offline e classes incluidas no JAR.
- Templates OBJ em cache, triangulacao, indices negativos, materiais persistentes,
  UVs e conversao de orientacao das faces para o motor. MTL nao abre caminhos externos.
- Limites: 4 MiB por OBJ, 16.384 vertices, 12.000 triangulos; coordenadas/UVs finitos.
- Registro de ate 64 materiais visuais; os 16 IDs de jogo permanecem intactos.
  Mapas de ate 4096 pixels por eixo/16 MiB, dimensoes verificadas antes do decode,
  cinco mipmaps de 256 a 16 pixels e paletas de tinta independentes por thread.
- Cinco fotografias diffuse CC0 da Poly Haven, com procedencia e hashes no pacote.

## CAIS-7

Concreto de piso/parede e metal com desgaste fotografado. Marcas de objetivos,
sombras pre-calculadas e grade de juntas preservadas. Refrigeradores OBJ originais
de 32 triangulos e gruas ficam fora da area jogavel, inclusive para atores no ar.
Nenhuma nova colisao, rota, cobertura balistica ou hitbox.

## ECHO e Vertice

- Receptor/guarda-mao adaptados de AssaultRifle_1 da Quaternius, CC0: 280 triangulos
  em primeira pessoa/colecao, 144 no receptor reduzido de terceira pessoa.
- Coronha, carregador curvo, mecanismos, mira, indicadores e equipamentos RIFT.
  Recarga, disparo, ADS, skins e charms mantidos; flash acompanha a nova ponta do cano.
- Vertice recebe capacete aberto, placa de peito e dois modulos dorsais originais,
  somando 88 triangulos OBJ, mais tecido/armadura de contraste controlado.
- A articulacao existente continua responsavel por andar, agachar, mirar, recarregar
  e morrer. Rotacoes preservam materiais e UVs; LOD distante permanece simplificado.

## UI

Tema grafite compartilhado entre menu, HUD solo/duelo, carreira, torneio, arsenal,
agentes, colecao e configuracoes. 15 icones Lucide rasterizados e licenciados,
switches, steppers numericos, swatches e opcoes segmentadas. Cliques/letterboxing
verificados em 1280x720 e 800x450. Aviso do arsenal nao cobre mais as habilidades.

## Verificacao e Limites

`--self-test` inclui os novos testes de modelos e UI. `--visual-test` roda apenas
essa frente; `--visual-capture pasta` produz capturas nativas reproduziveis.
`java tools/AuditAssets.java` compara todos os recursos do JAR com os fontes e
valida checksums/licencas. Evidencias e medicoes em [VERIFICACAO.txt](VERIFICACAO.txt).

Esta e uma entrega-piloto: os outros 14 modelos de armas, demais agentes e dois
mapas solo ainda nao foram renovados. Nao ha PBR, glTF animado, skinning generico,
novo motor GPU nem importacao de recursos proprietarios do Counter-Strike.
Blender/MPFB/MakeHuman nao foram instalados. Expansao do arsenal: 1.9.3.
