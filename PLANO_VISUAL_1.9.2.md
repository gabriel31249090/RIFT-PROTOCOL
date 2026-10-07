# RIFT Protocol - plano visual ampliado

Pesquisa e primeira integracao: 07/10/2026. Estado: piloto implementado com OBJ, UI compartilhada, CAIS-7, ECHO e armadura do Vertice. As ferramentas de producao listadas nao foram instaladas. Escopo efetivo: [CHANGELOG_1.9.2.md](CHANGELOG_1.9.2.md).

## Direcao visual

Combinar as tres preferencias indicadas: proporcoes e materiais militares criveis, tecnologia sci-fi propria do RIFT e estilizacao para leitura em combate. Nao reproduzir a identidade de CS nem trocar todos os elementos por recursos genericos de um pacote.

- Personagens: anatomia melhor, silhuetas por agente, roupas/equipamentos com volume e materiais distintos; preservar reconhecimento das equipes e animacoes.
- Armas: proporcoes consistentes, detalhes de metal/polimero, carregador e ferrolho separados, maos e encaixes coerentes em primeira pessoa.
- Cenarios: escala realista de materiais, concreto/metal/madeira com desgaste localizado, iluminacao pre-calculada e objetos industriais bem escolhidos.
- UI: contraste e hierarquia melhores, tipografia consistente, espacamento, icones e selecoes legiveis; compartilhar o tema entre solo e duelo.

## Repositorios selecionados

| Projeto | Utilidade | Licenca e limite |
| --- | --- | --- |
| [Blender](https://github.com/blender/blender) | Modelar armas, equipamentos e props; UV, reducao de malha, animacoes e baking de texturas. | Ferramenta GPLv3; nao e um pacote de modelos. Usar na producao offline, sem transplantar seu codigo para o RIFT. [Licenca](https://github.com/blender/blender/blob/main/COPYING). |
| [MPFB2](https://github.com/makehumancommunity/mpfb2) | Criar corpos/rostos humanos e rigs no Blender, depois roupas/equipamentos proprios. | Codigo GPLv3; assets incluidos CC0. Exige Blender 4.2+. Verificar separadamente cada recurso de terceiros. [Licenca e exports](https://github.com/makehumancommunity/mpfb2/blob/master/LICENSE.md). |
| [MakeHuman](https://github.com/makehumancommunity/makehuman) | Alternativa standalone para gerar a base dos personagens. | Codigo AGPL; assets incluidos CC0, com ressalvas para assets externos. Compatibilidade local ainda nao testada. [Licenca](https://github.com/makehumancommunity/makehuman/blob/master/LICENSE.md). |
| [javagl/Obj](https://github.com/javagl/Obj) | Leitura OBJ/MTL em Java, UVs, normais e triangulacao. Melhor candidato para a primeira importacao no motor atual. | MIT. Biblioteca de parsing, nao banco de modelos ou renderizador. [Licenca](https://github.com/javagl/Obj/blob/master/LICENSE.txt). |
| [Quaternius Ultimate Guns](https://github.com/Quaternius/quaternius.github.io/blob/main/packs/ultimategun.html) | 40 armas-base em OBJ/FBX/Blend para estudar proporcoes ou retrabalhar. | Pack anunciado como CC0, sem texturas/animacoes. O GitHub hospeda o site; os downloads dos modelos ficam fora dele. Nao entrega realismo pronto. |
| [Poly Haven Assets](https://github.com/Poly-Haven/polyhavenassets) | Ferramenta de acesso a materiais e props realistas do Poly Haven. | Addon GPLv3, com distribuicao oficial do ZIP via compra/Patreon; nao e necessario instala-lo. Os [assets oficiais](https://polyhaven.com/license) sao CC0 e podem ser obtidos diretamente, separados do addon. |
| [Lucide](https://github.com/lucide-icons/lucide) | Icones consistentes para ferramentas e controles da UI. | Preservar os avisos ISC/MIT descritos na [licenca](https://github.com/lucide-icons/lucide/blob/main/LICENSE). Preparar bitmaps offline, sem adicionar runtime JavaScript ao jogo. |

Ferramentas, bibliotecas e assets sao categorias distintas. Os recursos efetivamente importados possuem URL, versao/origem, autor, licenca, hash e modificacoes registrados em THIRD_PARTY_NOTICES.md e nas pastas assets/models e assets/ui. A pesquisa continua mais ampla que a entrega.

## Candidatos para depois

- [javagl/JglTF](https://github.com/javagl/JglTF): loader/modelo glTF/GLB MIT para a futura etapa de esqueletos e animacoes; nao fornece skinning/PBR pronto ao renderer do RIFT. [Licenca](https://github.com/javagl/JglTF/blob/master/LICENSE).
- [Khronos glTF-Blender-IO](https://github.com/KhronosGroup/glTF-Blender-IO): exportador/importador Apache-2.0, incluido no Blender. Ferramenta de exportacao, nao pack de modelos. [Licenca](https://github.com/KhronosGroup/glTF-Blender-IO/blob/main/LICENSE.txt).
- [TripoSR](https://github.com/VAST-AI-Research/TripoSR): imagem para malha, codigo/pesos MIT; geracao opcional de objetos e acessorios, nao personagem animado pronto. O padrao usa cerca de 6 GB de VRAM. A existencia de fallback CPU nao comprova tempo aceitavel nesta maquina. [README](https://github.com/VAST-AI-Research/TripoSR/blob/main/README.md).

## Encaixe no motor atual

O RIFT desenha triangulos na CPU em BufferedImage. World.Tri tem posicoes, cor, material e UV; nao tem juntas/pesos, normais de vertice, tangentes ou mapas PBR. Assets mantem os 16 materiais de jogo e agora aceita ate 64 materiais visuais com mipmaps; TextureRaster ja tem UV em perspectiva.

Assim, a primeira integracao proposta e OBJ triangulado + texturas PNG com cor e sombras de contato pre-calculadas. Mapas de normal/roughness de um asset externo nao serao exibidos automaticamente. glTF animado requer uma etapa propria de esqueleto, skinning e clips.

Pontos principais: World/EnvironmentArt para malhas visuais; WeaponModel para pecas de arma; CharacterModel para articulacao; View/DuelView e UIs separadas para o tema. SceneMesh possui convencao de orientacao das faces que o importador precisa respeitar.

Manter os colisores, rampas, navegacao, hitboxes e parametros de combate existentes. Malha visual nao deve passar a definir a colisao competitiva sem uma atualizacao dedicada.

## Sequencia de implementacao

1. Importacao de um prop pequeno com biblioteca OBJ, UV, escala/eixos corretos, limites de arquivo/triangulos e assets incluidos no JAR.
2. Tema compartilhado da UI: menu, HUD e duelo; depois compra, selecao de agentes, colecao e configuracoes. Validar cliques e textos nas resolucoes suportadas.
3. Atualizar CAIS-7: materiais e poucos props industriais, preservando rotas, coberturas e linhas de tiro.
4. Prototipo da ECHO: primeira pessoa, colecao e mundo; carregador/ferrolho separados, ADS, recarga, inspecao e clarao alinhados ao modelo.
5. Prototipo de um agente, inicialmente Vertice: malha otimizada e animacoes de andar, agachar, mirar, recarregar e morrer.
6. Comparar capturas, tempos de quadro e testes. Expandir para o arsenal na 1.9.3 e para os demais agentes na 1.9.4 somente apos validar os prototipos.

## Desempenho e validacao

A maquina consultada tem i5-3470, quatro CPUs logicas e Intel HD Graphics; nao foi encontrada GPU NVIDIA. Blender nao foi encontrado no PATH, o que nao comprova ausencia de instalacao. MPFB/Blender recente exigem verificacao de compatibilidade local antes de uma instalacao. [Requisitos oficiais do Blender](https://www.blender.org/download/requirements/).

Nao exigir ferramentas de criacao para executar o jogo. A geracao de assets pode ocorrer offline/em outra maquina; o JAR precisa continuar autocontido.

A [medicao da 1.9.1](DESEMPENHO_1.9.1.md) nao demonstra folga para malhas pesadas. Usar LOD (malhas simplificadas a distancia), cache, texturas dimensionadas e budget por ativo; medir com dez atores, smokes e dois renderizadores. Nao prometer realismo fotorealista ou ganho de FPS com essa base.

Verificar UV/culling/recorte, ausencia de superficies sumidas, equipar/ADS/recarregar, silhuetas dos agentes, clicks da UI, isolamento do duelo e recursos carregados apenas do JAR. A primeira entrega preserva as animacoes da articulacao existente: os modulos do Vertice sao rigidos, nao um corpo MakeHuman com skinning. Evidencias da 1.9.2 estao em VERIFICACAO.txt e verificacao/visual-1.9.2.
