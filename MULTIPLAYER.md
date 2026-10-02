# RIFT Protocol 1.9 — duelos com pessoas

O combate 1.9 usa o protocolo de rede 2. Todos na sala precisam desta atualização; clientes 1.8/1.8.1 são rejeitados para evitar regras de precisão e dano diferentes.

Esta é a primeira versão do multiplayer: **1v1 de armas**, com servidor autoritativo e três arenas próprias. Primeiro a sete vitórias; preparação de seis segundos; rodadas de até 60 segundos; os jogadores trocam de lado a cada rodada. Empate por tempo: vence quem tiver mais vida + escudo; valores iguais não dão ponto. Os dois confirmam uma revanche com Enter.

O duelo usa as 15 armas, três lâminas, recuo, wallbang, bhop e mira da sniper alternável. Cada rodada oferece uma principal, uma pistola e uma lâmina, com 100 de vida e 50 de escudo. A escolha é individual e gratuita; não há loja/economia no 1v1. **Habilidades de agentes ficam desativadas neste modo.** Os kits continuam disponíveis no jogo solo.

## Dois PCs na mesma rede

1. Os dois extraem a mesma versão e abrem `INICIAR_MULTIPLAYER_WINDOWS.bat`, ou o botão **1v1 / MULTIPLAYER** do menu.
2. O anfitrião escolhe **Criar sala**, informa nome, arena e porta (padrão TCP 27960).
3. A tela do anfitrião mostra endereço LAN, porta e código. Ele passa esses dados ao amigo.
4. O amigo usa **Entrar por endereço**, preenche os três dados e seu nome.
5. Cada um escolhe seu equipamento e aperta **Enter**. A contagem começa quando ambos estão prontos.

Se o Windows perguntar, permita que o Java receba conexões na rede em que vocês estão jogando. Não é necessário abrir nenhuma porta do roteador para dois computadores na mesma LAN. Redes de escola/hotel e algumas redes de convidados podem bloquear a comunicação entre dispositivos.

## Pela internet

O mesmo modo aceita um endereço público ou o IP de uma VPN entre os PCs. **É conexão direta, sem servidor público, contas ou busca automática de partida.** O código confirma a sala; ele não substitui o endereço.

Para conexão por IP público, a porta TCP escolhida precisa chegar ao PC anfitrião: encaminhamento no roteador + permissão no firewall. CGNAT ou regras da operadora podem impedir esse método; uma VPN que coloque os PCs na mesma rede virtual é uma alternativa. Não compartilhe senhas do roteador com o convidado.

O transporte inicial é TCP, a simulação roda a 60 Hz e os clientes recebem snapshots. A rotação da câmera responde localmente; movimento e tiros são confirmados pelo servidor. **Ainda não há previsão de movimento, compensação de latência, reconexão nem matchmaking.** Prefira uma conexão de baixa latência. O contador REDE mostra o tempo aproximado desde o envio do último comando confirmado, incluindo a espera pelo tick.

## Dois teclados e dois mouses no mesmo PC — experimental / Windows

1. Conecte os dois teclados e os dois mouses físicos.
2. Abra **Local / dois conjuntos**, selecione a arena e clique em **Abrir duas janelas**.
3. Com dois monitores, o jogo coloca uma janela em cada um. Com um monitor, divide o espaço em duas janelas; você também pode reposicioná-las.
4. Na janela **RIFT / Identificar teclados e mouses**, siga as quatro etapas: Enter no teclado do J1; clique no mouse do J1; Enter no teclado do J2; clique no mouse do J2.
5. A janela identifica cada dispositivo por número e, quando disponível, VID/PID. Não permite cadastrar o mesmo dispositivo para os dois jogadores. Na tela de jogo aparece qual conjunto pertence a cada pessoa.
6. Volte a uma janela do duelo. Ambos podem usar WASD e mirar ao mesmo tempo, mesmo com apenas uma janela tendo o foco do Windows.
7. Cada pessoa escolhe seu equipamento e aperta Enter no próprio teclado.

**F9** abre o cadastro novamente. **F12** encerra a sessão local. **Alt+Tab** para outro aplicativo libera o cursor e suspende a entrada dos dois jogadores. Ao voltar ao jogo, os controles são retomados. Teclas antigas são limpas para evitar movimento preso. Remover um dispositivo reabre o cadastro.

A ponte usa Raw Input do Windows, registra dispositivos pelo identificador físico e envia eventos por um canal privado ao processo Java. Não instala driver, serviço ou gancho global. A captura só é tratada quando o jogo ou a janela de cadastro tem o foco. Mouses relativos USB/Bluetooth são o alvo desta versão; tablets/touchscreens absolutos não são suportados.

Na primeira utilização, o jogo prepara um pequeno auxiliar usando o compilador do **.NET Framework 4.x** do Windows. Se esse componente estiver ausente, o jogo apresenta uma mensagem com o motivo. O código do auxiliar está incluído no JAR e em `assets/native/RawInputBridge.cs` no pacote de fontes.

**Limite da validação:** o protocolo de dispositivos e o isolamento de comandos passaram em testes automatizados no Linux. A ponte C#/Win32 não pôde ser compilada nem executada em Windows neste ambiente; dois teclados/mouses reais e dois monitores ainda precisam de teste físico. Por isso este modo está identificado como experimental. A conexão por rede Java não depende dessa ponte.

## Controles e equipamentos

| Controle | Ação |
| --- | --- |
| Enter | Pronto / aceitar revanche |
| F5 / F6 | Próxima arma principal / próxima pistola |
| F7 | Trocar aparência do agente |
| F8 | Trocar acabamento |
| Home / End | Trocar pingente / lâmina |
| B | Exibir/ocultar equipamento entre rodadas |
| WASD, Shift, Ctrl, Espaço | Mover, andar, agachar, saltar/bhop |
| Mouse esquerdo / direito | Atirar / mirar; lâmina: corte / golpe forte |
| 1 / 2 / 3 | Pistola / principal / lâmina |
| R / V | Recarregar / inspecionar |
| Esc | Suspender os próprios controles; a partida em rede continua |
| F12 | Sair do duelo |

As mudanças de equipamento são aceitas somente na espera, preparação ou tela de revanche. O servidor controla movimento, colisão, cadência, munição, dano e resultado. Um terceiro cliente é recusado. Uma conexão interrompida solta os comandos após 350 ms; a desconexão encerra a partida em andamento.

## Itens e preferências separados

Vida, armadura, munição, recarga, slot, mira, arma principal, pistola e cosméticos são próprios de cada jogador. Não há um inventário coletivo.

Os perfis de duelo são separados do progresso solo. O PC local guarda `duel/player-1` e `duel/player-2`; a conexão entre PCs usa `duel/online`, dentro da pasta `.rift-protocol` do usuário. As preferências começam como uma cópia das configurações solo; depois são independentes. As escolhas de arma e cosméticos são salvas ao sair. Duelo não altera XP/rank/Premier do jogo solo.

O áudio local usa uma única saída do sistema, com a perspectiva do J1 para evitar sons duplicados. Não há seleção de dois headsets nesta versão.

## Arenas e desempenho

- **Fenda:** três rotas, bloco central e coberturas para peeks.
- **Pátio Zero:** duas plataformas com rampas e disputa de altura.
- **Galeria:** corredores mais definidos para precisão e controle de ângulos.

Cada arena ocupa 48 × 48 metros, com spawns sem visão direta um do outro. As dimensões são de uma arena de duelo; os três mapas grandes do solo continuam disponíveis.

As janelas de duelo iniciam com limite de 60 FPS, resolução automática e sombras desligadas. O menu principal suspende a renderização enquanto o duelo está aberto. Cada janela possui cache de paletas independente; o mapa pode ser compartilhado sem duplicar a geometria. Não há promessa de FPS em um PC específico.

## Servidor sem janela e diagnósticos

```sh
java -jar RiftProtocol.jar --duel-server 27960 0 MEUCODIGO
java -Djava.awt.headless=true -jar RiftProtocol.jar --duel-test
java -Djava.awt.headless=true -jar RiftProtocol.jar --duel-benchmark
```

Arenas: `0` Fenda, `1` Pátio Zero, `2` Galeria. Código: 4–24 letras/números ou hífen. Omitir o código gera um automaticamente. Encerre o servidor com Ctrl+C. Os dois jogadores entram como clientes.

O diagnóstico de rede usa apenas o próprio computador e conexões de loopback. Não testa o encaminhamento do roteador nem a rota pela internet. Envie o arquivo produzido por `DIAGNOSTICO_MULTIPLAYER_WINDOWS.bat` se houver problema.

## Referências da integração Windows

- [Microsoft: Raw Input Overview](https://learn.microsoft.com/en-us/windows/win32/inputdev/about-raw-input)
- [Microsoft: RAWMOUSE](https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-rawmouse)
- [Microsoft: RAWKEYBOARD](https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-rawkeyboard)
