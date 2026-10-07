# Referencias e recursos de terceiros

## ReGameDLL_CS

Referencia fixada: [rehlds/ReGameDLL_CS, commit 4a50c42](https://github.com/rehlds/ReGameDLL_CS/tree/4a50c42e85fd3778c2b7d24731c7d30c83bbab01).

Na atualizacao 1.9, os seguintes algoritmos foram estudados e adaptados para a simulacao Java do RIFT:

- `regamedll/dlls/weapons.cpp`, `KickBack`: crescimento e limites do recuo vertical/lateral.
- `regamedll/dlls/wpn_shared/wpn_ak47.cpp`: precisao dependente de postura, movimento e sequencia de tiros.
- `regamedll/dlls/cbase.cpp`, `FireBullets3`: queda exponencial de dano e penetracao dependente da municao/material.
- `regamedll/pm_shared/pm_shared.cpp`, `PM_Friction` e `PM_Accelerate`: atrito e aceleracao pela projecao da velocidade.

Implementacao propria em `WeaponHandling`, `Combat`, `Game` e `Ballistics`, com parametros em metros/radianos/segundos ajustados para as armas do RIFT. Nao ha arquivo C++ transplantado, dependencia nativa do ReGameDLL nem copia dos valores de balanceamento do CS. O padrao lateral continua deterministico, em vez da troca aleatoria de direcao do `KickBack` original.

A copia da [licenca MIT da referencia](https://github.com/rehlds/ReGameDLL_CS/blob/4a50c42e85fd3778c2b7d24731c7d30c83bbab01/LICENSE) esta em `assets/licenses/ReGameDLL_CS.txt` e e incluida no JAR. Esta entrega nao usa os arquivos de bots/nav com avisos de licenca distintos.

## Modelos, texturas e sons de Counter-Strike

Nenhum recurso da instalacao de Counter-Strike foi incorporado a esta entrega. A licenca do codigo ReGameDLL nao licencia os recursos do jogo. Modificar um modelo ou uma textura nao substitui a verificacao dos direitos de uso e distribuicao. A frente visual usa recursos proprios ou com licenca documentada.

## Banco de audio original 1.9.1

Os 144 WAVs em `assets/audio` foram gerados para o RIFT por `src/rift/AudioAssets.java` e `src/rift/ShotAudio.java`, usando ruido deterministico, filtros, ressonadores e camadas mecanicas. Nao contem gravacoes, sons extraidos, conversoes ou downloads de Counter-Strike ou de outro jogo. A sintese de reserva usa as mesmas fontes originais.

Os arquivos PCM mono de 16 bits a 22050 Hz e seu manifesto `SHA256SUMS.txt` acompanham os fontes e sao incluidos no JAR. O gerador usa apenas Java Sound, da biblioteca padrao Java, sem adicionar biblioteca de terceiros. A origem e a reproducao do banco estao documentadas em `assets/audio/README.txt`; o escopo da entrega esta em [CHANGELOG_1.9.1.md](CHANGELOG_1.9.1.md).

## Biblioteca OBJ 1.9.2

[javagl/Obj](https://github.com/javagl/Obj), de Marco Hutter, versao 0.4.0, MIT.
O artefato fixado em `lib/obj-0.4.0.jar` e verificado por SHA-256 no build.
Suas classes sao incorporadas ao executavel, que permanece offline e sem JARs
externos. Licenca em `lib/obj-LICENSE.txt`, empacotada em
`META-INF/licenses/obj-LICENSE.txt`. Detalhes em [lib/README.md](lib/README.md).

## Base da ECHO

`AssaultRifle_1`, de [Quaternius Ultimate Guns](https://quaternius.com/packs/ultimategun.html),
CC0 1.0, obtido em 07/10/2026 pelo download oficial. A copia original e duas
derivacoes reduzidas estao em `assets/models/echo`: 280 triangulos no receptor
de primeira pessoa/colecao e 144 no receptor de terceira pessoa. Equipamento,
carregador, acabamento e animacoes adicionais sao proprios do RIFT.
Origem, transformacoes e hashes: [README](assets/models/echo/README.md).
Licenca local: `assets/models/echo/CC0-1.0.txt`, incluida no JAR.

## Texturas Fotografadas

Cinco mapas diffuse de 1K da [Poly Haven](https://polyhaven.com/license), CC0 1.0,
obtidos em 07/10/2026. Nao incluem previews do site, marcas, addon ou codigo AGPL/GPL.
Nao foram importados mapas de normal, displacement ou roughness.

- CAIS-7: `concrete_floor_worn_001`, `concrete_wall_003`, `blue_metal_plate`;
  [fontes, hashes e licenca](assets/models/cais7/PROVENANCE.md).
- ECHO/armadura: `metal_plate_02`, de Rob Tuytel;
  [fonte e hash](assets/models/echo/README.md).
- VERTICE/tecido: `fabric_leather_02`, de Rob Tuytel;
  [fonte e hash](assets/models/vertice/PROVENANCE.md).

O renderizador usa mipmaps limitados a 256x256 e tintas/paletas locais, nao PBR.
O refrigerador do CAIS e os tres modulos OBJ do VERTICE sao meshes originais
do RIFT, sob MIT; nao sao exports de MakeHuman/MPFB. Essas ferramentas nao foram
instaladas nem incorporadas.

## Icones da UI

15 icones do [Lucide, tag 0.468.0](https://github.com/lucide-icons/lucide/tree/0.468.0/icons).
SVGs originais e PNGs de 48x48 em `assets/ui/lucide`, com o aviso completo
ISC/MIT em `LICENSE.txt`. Rasterizacao offline com Apache Batik 1.18; Batik e
JavaScript nao sao dependencias do executavel. [Procedencia](assets/ui/lucide/README.txt).
