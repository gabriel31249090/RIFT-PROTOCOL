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

Nenhum recurso da instalacao de Counter-Strike foi incorporado a esta entrega. A licenca do codigo ReGameDLL nao licencia os recursos do jogo. Modificar um modelo ou uma textura nao substitui a verificacao dos direitos de uso e distribuicao. A proxima frente visual deve usar recursos proprios ou com licenca documentada.
