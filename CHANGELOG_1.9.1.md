# RIFT Protocol 1.9.1 - audio e superficies

## Banco original

- 144 WAVs proprios, com tres variantes por evento: 45 disparos, 27 sons de superficie e 72 mecanismos de recarga.
- PCM mono de 16 bits, little-endian, a 22050 Hz. Arquivos em `assets/audio`, tambem incluidos como recursos no JAR.
- Carregamento WAV com limites de tamanho/duracao e conversao para o formato do mixer; recurso ausente ou invalido usa sintese original de reserva.
- Mistura estereo com fila e polifonia limitadas. Legendas de efeitos continuam disponiveis com som desativado.
- Nenhuma gravacao, modelo, textura ou som da instalacao de Counter-Strike foi importado.

## Superficies e recargas

- Passos, pousos e impactos no modo solo distinguem concreto, madeira e metal; caixas, conteineres e rampas seguem os materiais do mapa.
- Eventos posicionais usam atenuacao por distancia, panorama estereo e reducao de volume quando ha uma parede entre fonte e ouvinte.
- Recarga inicial curta, sem sequencia completa embutida. Retirada, insercao e ferrolho sao eventos separados, alinhados ao progresso da recarga.
- Seis categorias de mecanismos: pistola, submetralhadora, fuzil, escopeta, precisao e pesada.
- Audio de impactos nao depende de marcas/capsulas estarem ativadas nas opcoes graficas.

## Duelo e compatibilidade

Clientes 1v1 reproduzem passos, pousos e recargas a partir dos snapshots; os disparos existentes usam as variantes WAV. Impactos em paredes ficam restritos ao solo: o protocolo nao transmite a causa desse evento, e o cliente nao a inventa a partir de um tiro.

O protocolo continua na versao 2 da 1.9, sem alteracao do contrato de rede, balanceamento ou RNG de jogo. Preferencias e perfis locais permanecem compativeis. A audicao simulada dos bots continua separada da reproducao de som.

## Geracao e verificacao

```sh
java -jar RiftProtocol.jar --audio-test
java -jar RiftProtocol.jar --self-test
java -jar RiftProtocol.jar --audio-generate build/audio-original
```

`--audio-test` verifica o banco, a mistura e os eventos sem exigir placa de som; tambem integra `--self-test`. `--audio-generate` cria os 144 WAVs e `SHA256SUMS.txt` no diretorio indicado, sem modificar o JAR. Fontes e origem: [assets/audio/README.txt](assets/audio/README.txt).

Resultados e limites: [VERIFICACAO.txt](VERIFICACAO.txt). Medicoes: [DESEMPENHO_1.9.1.md](DESEMPENHO_1.9.1.md). Testes sem janela nao validam a saida fisica: audio em fones, dois conjuntos reais de teclado/mouse e partidas em LAN real continuam pendentes. Reverberacao, vozes e callouts falados nao fazem parte desta entrega.

Proxima etapa planejada: **1.9.2**, identidade visual propria, texturas e importacao de malhas estaticas. Veja [ROADMAP.md](ROADMAP.md).
