# RIFT Protocol · Java Edition

FPS tático em Java 17, com agentes, habilidades, economia por rodada e bots. A versão **1.9.1** acrescenta 144 WAVs originais, passos e pousos por material, impactos no modo solo e recargas por etapa, com distância, panorâmica e atenuação por paredes. Mantém o combate da 1.9, duelos 1v1 por LAN/conexão direta e uma opção experimental para dois teclados e dois mouses no mesmo Windows.

![Menu do RIFT Protocol](verificacao/duelo-1.9.1/menu-1.9.1.png)

## Jogar

Extraia o ZIP inteiro e abra **INICIAR_WINDOWS.bat**. Precisa de Java 17 ou superior. Para PC com dificuldade de desempenho, use **INICIAR_MODO_DESEMPENHO.bat**. Linux/macOS: `sh iniciar.sh`.

No menu, **JOGAR SOLO** abre o fluxo de modos e agentes; **CAMPO DE TREINO** abre o treino; **ASSISTIR BOTS** inicia uma partida autônoma. No observador, use Espaço para trocar de POV.

Para jogar com outra pessoa, use **1v1 / MULTIPLAYER**. Instruções de rede, internet e cadastro dos dispositivos estão em [MULTIPLAYER.md](MULTIPLAYER.md).

## Conteúdo

- 11 agentes com quatro habilidades por kit.
- 15 armas, três lâminas e coleção de cosméticos locais.
- Três mapas solo com sites e desníveis, mais três arenas de 1v1.
- Duelo primeiro a sete, troca de lados, revanche e equipamentos independentes.
- LAN/internet por endereço; duas janelas locais via Raw Input experimental no Windows.
- Seis modos locais, campo de treino e observação dos bots.
- Visão de 110°, mira gradual, audição, memória e decisões de duelo.
- Penetração de madeira/metal conforme arma, espessura e ângulo.
- Áudio próprio em WAV, três variantes por evento e síntese de reserva; concreto, madeira e metal têm sons distintos.
- Resolução interna automática, preset de desempenho e configurações de controles/acessibilidade.

## Desenvolvimento

```sh
java Build.java
java -jar RiftProtocol.jar --self-test
java -jar RiftProtocol.jar --combat-test
java -jar RiftProtocol.jar --audio-test
java -jar RiftProtocol.jar --audio-generate build/audio-original
java -jar RiftProtocol.jar --duel-test
java -jar RiftProtocol.jar --duel-benchmark
java -jar RiftProtocol.jar --simulate-bots 6
java -jar RiftProtocol.jar --benchmark-bots
```

A compilação usa o JDK 17 e não baixa dependências. O JAR contém as artes e os WAVs; seus arquivos-fonte também vêm no pacote. `--audio-test` verifica áudio sem placa de som e está incluído no `--self-test`. `--audio-generate` recria 144 WAVs e seu manifesto SHA256 no diretório indicado, sem modificar o JAR. As configurações e o perfil ficam em `.rift-protocol` na pasta do usuário; extraia cada atualização numa pasta nova para preservar o pacote anterior.

## Documentação

- [Como jogar e controles](LEIA_PRIMEIRO.md)
- [Multiplayer: instruções e limites](MULTIPLAYER.md)
- [Mudanças da 1.9.1](CHANGELOG_1.9.1.md)
- [Mudanças da 1.9](CHANGELOG_1.9.md)
- [Referências e licenças de terceiros](THIRD_PARTY_NOTICES.md)
- [Mudanças da 1.8.1](CHANGELOG_1.8.1.md)
- [Mudanças da 1.8](CHANGELOG_1.8.md)
- [Mudanças da 1.7](CHANGELOG_1.7.md)
- [Desempenho da 1.8.1 no Windows](DESEMPENHO_1.8.1.md)
- [Comparação de desempenho da 1.9](DESEMPENHO_1.9.md)
- [Desempenho da 1.9.1](DESEMPENHO_1.9.1.md)
- [Desempenho e limites das medições](DESEMPENHO_1.7.md)
- [Roadmap e funcionalidades ainda planejadas](ROADMAP.md)
- [Verificação do pacote](VERIFICACAO.txt)

O multiplayer inicial é um duelo de armas, com habilidades desativadas. Seus clientes reproduzem passos, pousos e recargas a partir dos snapshots; impactos em paredes ficam restritos ao solo, pois a rede não transmite esse evento. O protocolo continua na versão 2 da 1.9. Internet exige um anfitrião acessível; não há matchmaking/relay hospedado. O rank, a progressão e o Premier continuam locais. Áudio em fones, dois conjuntos físicos no Windows e partidas em LAN real ainda exigem validação; veja MULTIPLAYER.md e VERIFICACAO.txt.
