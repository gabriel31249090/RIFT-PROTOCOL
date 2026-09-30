# RIFT Protocol 1.4 — Movimento, impacto e coleção

## Combate

- Aceleração de 60 m/s², frenagem de 85 m/s² e inversão de direção de 100 m/s² no chão; controle aéreo reduzido, com integração em passos de até 1/240 s.
- Corrida de base 5,4 m/s com armas, multiplicada pela mobilidade; 6,2 m/s com lâmina. Caminhada e agachamento mantêm velocidades próprias.
- Precisão de movimento baseada em limiar de velocidade; penalidade ao correr, pular e aterrissar.
- Primeiro tiro mais preciso, sequência de recuo vertical/lateral por arma, dispersão acumulada e recuperação entre rajadas.
- Recuo acompanha a câmera e pode ser compensado com o mouse. A entrada do mouse permanece direta.
- Cadência automática preserva o excedente de tempo entre quadros para reduzir diferenças entre 60, 120 e 144 FPS.
- Troca de arma cancela recarga; tempos de equipar dependem da categoria.
- Dano recebido desacelera por 0,55 s e mostra direção. Treino exibe o estado de precisão.

## Lâminas e visuais

- Fio, Arco e Ruptor: três malhas originais, equipadas com 3.
- Corte e golpe forte possuem antecipação, contato, recuperação, alcance, arco e verificação de paredes. Dano dobrado pelas costas.
- Inspeção V para lâminas e armas; ações de combate interrompem a inspeção.
- Seis acabamentos, incluindo padrão, para as 15 armas e três lâminas.
- Cinco pingentes geométricos, com balanço amortecido ao mover, olhar e disparar.
- Tela COLEÇÃO com prévia 3D, equipamento individual e aplicação do visual a todas as armas.
- Preferências persistentes por arma, migração automática de perfis anteriores e validação de valores inválidos.
- Todos os cosméticos e lâminas são gratuitos; estatísticas não dependem da skin.

## Mapas

- Cais-7, Monte Aurora e Ferrovia passam de 88 × 80 para 144 × 128 m: área 2,62 vezes maior.
- Novos setores sul e leste, coberturas, ligações entre rotas, rampas e plataformas. A escala das estruturas anteriores foi mantida.
- Ponto B deslocado para o novo setor leste; bases, rotas dos bots e respawns adaptados.
- Combate normal: 140 s. Disputa da Spike: 100 s; continuam até sete rodadas.
- Mapas táticos, teleporte global, tiros e visibilidade adaptados à extensão maior.

## Verificação e limites

- Testes determinísticos de movimento, cadência, rajadas, recarga, corpo a corpo, coleção, persistência e rotas, além da regressão anterior.
- Capturas e GIF usam o renderizador e a simulação reais do jogo.
- Consulte VERIFICACAO.txt para resultados e desempenho medidos sem janela.
- Continua sendo uma edição Java por software, offline e contra bots. A sensação é uma aproximação própria de FPS tático; não usa o motor, assets ou balanceamento exatos da referência.
