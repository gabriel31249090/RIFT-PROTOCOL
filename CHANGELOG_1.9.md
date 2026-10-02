# RIFT Protocol 1.9 - combate e movimento

## Combate

- Perfis proprios para as 15 armas: crescimento/limites do recuo, dispersao acumulada, recuperacao e penalidades de movimento/ar.
- Primeiro tiro preciso; rajadas longas aumentam erro; pausa recupera o recuo sem depender da divisao do tempo em quadros.
- Precisao fisica compartilhada por jogador e bots, incluindo dispersao acumulada, ar, pouso e escopetas. A dificuldade dos bots altera apenas erro de mira e compensacao do recuo.
- Queda exponencial e continua de dano alem do alcance nominal, com retencao por arma. Dano proximo, precos, municao e cadencia permanecem inalterados.
- Orcamento de penetracao e quantidade de coberturas por arma; espessura e material consomem o mesmo orcamento ao longo do tiro.
- Coberturas sobrepostas e alvos parcialmente dentro de paineis usam a espessura realmente percorrida; paredes, barreiras temporarias, rampas e chao dentro do painel nao sao ignorados.

## Movimento

- Atrito separado da aceleracao no solo, com aceleracao pela projecao da velocidade na direcao pedida.
- Frenagem, contra-direcao e limite de velocidade no solo verificados; salto manual, air strafe e bhop preservados.
- Solo e servidor de duelo continuam usando o mesmo controlador e subpassos de movimento.

## Compatibilidade

O protocolo de duelo agora e a versao 2. Cliente/servidor 1.8 e 1.8.1 sao rejeitados antes de entrar na sala: todos devem usar 1.9. Perfis/configuracoes locais permanecem compativeis.

## Referencias

Algoritmos do ReGameDLL_CS foram adaptados para Java com balanceamento proprio, sem importar modelos, texturas ou sons do CS. Veja [referencias e licencas](THIRD_PARTY_NOTICES.md). Esta etapa nao adiciona audio WAV, malhas ou animacoes novas; o proximo passo e 1.9.1, audio e superficies.

## Verificacao

`--combat-test` executa os novos cenarios focados. `--self-test` inclui esses testes, todas as regressoes anteriores e conexoes TCP reais entre clientes de duelo. Resultados e limites da entrega ficam em [VERIFICACAO.txt](VERIFICACAO.txt).
