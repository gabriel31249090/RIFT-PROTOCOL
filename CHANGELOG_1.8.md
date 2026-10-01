# 1.8 — Duelo

- Multiplayer 1v1 entre duas pessoas, com criação de sala, entrada por endereço/porta/código, limite de dois jogadores e servidor sem janela opcional.
- Mesma conexão funciona em LAN e pela internet quando o anfitrião é alcançável. Não depende de um serviço hospedado.
- Servidor autoritativo de 60 Hz: controles enviados pelos clientes, movimento/colisão/dano/munição/placar calculados no servidor. Mensagens binárias limitadas; orientação não finita e equipamentos inválidos são rejeitados.
- Preparação, rodada de 60 s, troca de lados, primeiro a sete, resultado, revanche com dois prontos e saída de adversário.
- Três arenas: Fenda, Pátio Zero e Galeria. Spawns protegidos, rotas simétricas, coberturas e plataformas/rampas no Pátio.
- Vida, armadura, munição, recarga, mira, arma principal, pistola, lâmina, skin e pingente separados por jogador. Perfis do duelo separados do progresso solo.
- Duas janelas locais e distribuição por monitor. Ponte Raw Input do Windows com cadastro individual de teclados/mouses, recadastro, foco e limpeza de teclas. **Experimental: o auxiliar Windows ainda não foi validado em hardware real.**
- Cache de paletas separado por render thread. Duelo usa 60 FPS/AUTO e o menu não continua renderizando escondido.
- HUD de duelo, seleção de equipamento entre rodadas, indicador de rede e sons de disparos/acertos replicados.
- Testes de simulação, dois sockets reais, tiros pela rede, saída, dispositivos isolados e comparação de pixels com duas renderizações simultâneas.

## Escopo desta primeira versão

1v1 de armas; habilidades desativadas no duelo. Agentes têm aparência própria, mas não usam os kits nesse modo. Compra é substituída por loadout gratuito entre rodadas. Não inclui 5v5 online, matchmaking, relay automático, voz, duas saídas de áudio, lag compensation, previsão de movimento, reconexão ou ranking global. Os recursos solo da 1.7 permanecem no pacote.

Consulte MULTIPLAYER.md para instruções e limites de validação.
