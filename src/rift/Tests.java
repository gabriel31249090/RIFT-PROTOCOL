package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import static rift.Game.*;
import static rift.World.*;

/** Deterministic checks exercise real simulation and rendering, without a display server. */
final class Tests {
    static int passed;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);passed++;System.out.println("PASS  "+message);}
    static Game fresh(){Game g=new Game(new Settings(false),false,42);g.start(false);return g;}
    static void run()throws Exception {
        Game g=fresh();World w=g.world;
        check(w.path(68,120,w.a.x(),w.a.z()).size()>10,"Rota do ataque ao ponto A");
        check(w.path(68,120,w.b.x(),w.b.z()).size()>10,"Rota do ataque ao ponto B");
        check(w.path(68,5,w.a.x(),w.a.z()).size()>10,"Rota da defesa ao ponto A");
        check(w.path(68,5,w.b.x(),w.b.z()).size()>10,"Rota da defesa ao ponto B");
        for(Actor a:g.actors)check(!w.blocked(a.x,a.z,0,.31,1.78),"Spawn válido: "+a.name);
        check(g.actors.stream().filter(a->a.carrier).count()==1,"Um único portador do núcleo");
        g.player.x=6;g.player.z=37;g.move(g.player,9,0);check(g.player.x<9.7,"Colisão impede atravessar edifício, inclusive com impulso");
        check(w.visible(new V(15,1.6,30),new V(20,1.6,30)),"Linha de visão livre");
        check(!w.visible(new V(10,1.6,30),new V(10,1.6,48)),"Parede bloqueia a linha de visão");
        g=fresh();g.player.credits=5000;check(g.buyWeapon(Weapon.ECHO)&&g.player.credits==2300&&g.player.gun().kind==Weapon.ECHO,"Compra desconta créditos e equipa arma");
        check(g.buyArmor(50)&&g.player.armor==50&&g.player.credits==1300,"Compra da proteção completa");
        check(!g.buyWeapon(Weapon.HORIZON)&&g.player.credits==1300,"Créditos insuficientes não alteram inventário");
        g.beginRound();check(!g.buyWeapon(Weapon.WISP),"Compra bloqueada durante combate");
        Actor enemy=g.actors.get(5);g.damage(g.player,100,enemy,false);check(Math.abs(g.player.hp-50)<.01&&g.player.armor==0,"Absorção de dano não cria vida nem proteção negativa");
        g.damage(g.player,500,g.actors.get(1),false);check(g.player.hp==50,"Fogo amigo não causa dano");
        Gun gun=g.player.gun();gun.ammo=2;gun.reserve=8;g.reload(g.player);gun.tick(3);check(gun.ammo==10&&gun.reserve==0&&gun.reload<=0,"Recarga transfere somente a munição disponível");
        g=fresh();g.beginRound();g.player.x=16;g.player.z=19;g.player.carrier=true;g.plant(g.player);for(Actor a:g.actors)if(a.team==g.attackTeam)a.dead=true;g.checkRules();check(g.phase==Phase.LIVE&&g.planted,"Núcleo armado mantém rodada após eliminação do ataque");
        for(Actor a:g.actors)if(a.team!=g.attackTeam){a.x=16;a.z=19;a.target=null;}
        for(int i=0;i<302;i++)g.updateSpike(1/60.,false);
        check(g.phase==Phase.END&&g.scoreRed==1&&g.endReason.contains("desarmado"),"Bots concluem o desarme em 5 segundos");
        g=fresh();g.beginRound();g.player.x=16;g.player.z=19;g.plant(g.player);for(Actor a:g.actors)if(a.team==1){a.x=60;a.z=50;}
        g.spikeTime=.01;g.updateSpike(.02,false);check(g.phase==Phase.END&&g.scoreBlue==1,"Detonação concede a rodada ao ataque");
        g=fresh();g.round=5;g.spawn(true);check(g.attackTeam==1&&!g.player.carrier&&g.player.credits==800,"Troca de lados e reinício econômico após quatro rodadas");
        g.beginRound();g.timer=.01;g.timer-=.02;g.checkRules();check(g.roundWinner==0,"Tempo esgotado dá vitória à defesa");
        g=fresh();g.beginRound();double timer=g.timer;g.ui="pause";g.tick(1,Input.Frame.empty());check(g.timer==timer,"Pausa interrompe o relógio");
        g=fresh();g.smokes.add(new Smoke(32,30));check(g.obscured(new V(32,1.6,20),new V(32,1.6,40)),"Fumaça bloqueia a percepção dos bots");
        testShooting();testTraining();testInputs();testTactical();testMapOrientation();testWeapons();testUltimates();testAce();testMatch();testMenus();UpdateTests.run();FeelTests.run();TacticalTests.run();ImpactTests.run();
        CombatTests.run();MovementTests.run();BallisticsTests.run();
        PerformanceTests.run();BotTests.run();DuelTests.run();System.out.println("\n"+passed+" checks passed.");
    }
    static void testShooting(){
        Game g=fresh();g.beginRound();for(Actor a:g.actors)if(a!=g.player)a.dead=true;
        Actor e=g.actors.get(5);e.dead=false;e.hp=100;e.armor=0;e.x=20;e.z=29;e.y=0;
        g.player.x=15;g.player.z=29;V target=new V(20,1.62,29);g.shoot(g.player,target.sub(g.player.eye()).unit(),Weapon.ECHO);
        check(e.dead&&g.player.kills==1,"Tiro real na cabeça elimina e contabiliza abate");
        e.dead=false;e.hp=100;e.x=10;e.z=48;g.player.x=10;g.player.z=30;
        g.shoot(g.player,e.center().sub(g.player.eye()).unit(),Weapon.HORIZON);check(e.hp==100,"Tiro não atravessa uma parede");
    }
    static void testTraining(){Game g=fresh();g.start(true);Actor e=g.actors.get(5);g.damage(e,1000,g.player,false);for(int i=0;i<125;i++)g.tick(1/60.,Input.Frame.empty());check(!e.dead&&e.hp==100,"Alvo de treino reaparece");g.player.credits=9999;g.buyWeapon(Weapon.HORIZON);check(g.player.credits==9999,"Arsenal de treino sem custo");}
    static void testInputs(){
        Game g=fresh();g.start(true);g.player.x=20;g.player.z=30;g.player.yaw=Math.PI;double z=g.player.z;
        for(int i=0;i<30;i++)g.tick(1/60.,Input.Frame.keys(java.awt.event.KeyEvent.VK_W));
        check(g.player.z<z-2,"W move o jogador no sentido da câmera");
        g.player.x=20;g.player.z=30;g.player.y=0;g.player.vy=0;g.player.grounded=true;
        g.tick(1/60.,Input.Frame.keys(java.awt.event.KeyEvent.VK_SPACE));double maxY=0;for(int i=0;i<90;i++){g.tick(1/60.,Input.Frame.empty());maxY=Math.max(maxY,g.player.y);}
        check(maxY>.9&&g.player.grounded&&g.player.y==0,"Pulo aplica gravidade e retorna ao piso");
        g.player.x=20;g.player.z=30;g.player.intentX=-1;g.player.intentZ=0;g.ability(Ability.DASH);
        check(g.player.x==20&&g.dashTime>.39,"Impulso inicia uma animação, sem teleportar");
        g.advanceDash(.1);check(g.player.x<20&&g.player.x>17,"Dash percorre apenas parte da distância após 100 ms");
        for(int i=0;i<18;i++)g.advanceDash(1/60.);check(g.player.x<14&&Math.abs(g.player.z-30)<.1,"Impulso completa a distância na direção do movimento");
        g.player.x=4;g.player.z=37;g.player.intentX=1;g.player.intentZ=0;g.dashTime=0;g.ability(Ability.DASH);for(int i=0;i<25&&g.dashTime>0;i++)g.advanceDash(1/60.);check(g.player.x<9.7&&g.dashTime==0,"Dash animado para ao encontrar um edifício");
        g.player.hp=30;g.ability(Ability.HEAL);for(int i=0;i<300;i++)g.tick(1/60.,Input.Frame.empty());check(g.player.hp>84&&g.player.hp<86,"Reparo recupera 55 de vida gradualmente");
        g.player.ult=6;g.tick(1/60.,Input.Frame.keys(java.awt.event.KeyEvent.VK_X));check(g.focus>9,"Suprema do duelista ativa o foco");
        int charges=g.player.qCharges;g.player.x=20;g.player.z=30;g.player.yaw=Math.PI;g.tick(1/60.,Input.Frame.keys(java.awt.event.KeyEvent.VK_Q));check(g.player.qCharges==charges,"Treino mantém habilidades disponíveis sem inflar cargas");
        g=fresh();g.beginRound();for(Actor a:g.actors)if(a!=g.player)a.dead=true;g.player.x=16;g.player.z=19;
        for(int i=0;i<194;i++){g.controlPlayer(1/60.,Input.Frame.keys(java.awt.event.KeyEvent.VK_F));g.updateSpike(1/60.,true);}
        check(g.planted,"Segurar F planta o núcleo pela entrada real do jogador");
        g=fresh();g.round=5;g.spawn(true);g.beginRound();Actor carrier=g.actors.get(5);carrier.x=16;carrier.z=19;g.plant(carrier);for(Actor a:g.actors)if(a!=g.player)a.dead=true;g.player.x=16;g.player.z=20;
        for(int i=0;i<302;i++)g.updateSpike(1/60.,true);
        check(g.roundWinner==0&&g.endReason.contains("desarmado"),"Segurar F conclui o desarme pelo jogador");
        g=fresh();g.beginRound();g.player.x=18;g.player.z=28;g.damage(g.player,1000,g.actors.get(5),false);Actor ally=g.actors.get(1);ally.x=18;ally.z=28;g.updateSpike(.01,false);
        check(ally.carrier,"Aliado recupera o núcleo de um jogador eliminado");
    }
    static void testTactical(){
        Game g=fresh();g.settings.agent=1;g.start(false);g.beginRound();g.player.x=20;g.player.z=30;
        g.castSlot(0);check(g.ui.equals("tactical")&&g.smokes.isEmpty()&&g.player.qCharges==3,"Q da Bruma abre o mapa sem gastar fumaças");
        check(!g.markTarget(60,5)&&g.tacticalTargets.isEmpty(),"Mapa rejeita ponto fora do alcance");
        check(!g.markTarget(10,37)&&g.tacticalTargets.isEmpty(),"Mapa rejeita ponto dentro de edifício");
        check(g.markTarget(16,19)&&g.markTarget(5,18)&&g.tacticalTargets.size()==2,"Mapa permite marcar dois pontos válidos");
        check(g.markTarget(16,19)&&g.tacticalTargets.size()==1,"Clicar outra vez remove a marcação");
        double timer=g.timer;g.tick(1/60.,Input.Frame.empty());check(g.timer<timer&&g.ui.equals("tactical"),"Relógio e partida continuam enquanto o mapa está aberto");
        g.cancelTactical();check(g.player.qCharges==3&&g.smokes.isEmpty(),"Cancelar não consome cargas");
        g.castSlot(0);g.markTarget(16,19);g.markTarget(5,18);check(g.deployTactical()&&g.smokes.size()==2&&g.player.qCharges==1&&g.ui.equals("play"),"Confirmar lança as fumaças e debita somente as marcações");
        Smoke smoke=g.smokes.get(0);check(smoke.radius()==0,"Fumaça começa com atraso de chegada");smoke.tick(.7);check(smoke.radius()>0&&smoke.radius()<3.3,"Fumaça se expande progressivamente");smoke.tick(.6);check(smoke.radius()>3.2,"Fumaça atinge o raio completo");
        smoke.life=.3;check(smoke.radius()<1.1,"Fumaça retrai antes de desaparecer");
        g.castSlot(0);g.tick(1/60.,Input.Frame.keys(java.awt.event.KeyEvent.VK_Q));check(g.ui.equals("play"),"Repetir a tecla fecha o mapa sem reabri-lo no mesmo quadro");
        g.castSlot(0);g.markTarget(16,19);g.damage(g.player,1000,g.actors.get(5),false);check(g.ui.equals("play")&&g.tacticalTargets.isEmpty()&&g.player.qCharges==1,"Morrer fecha o mapa e cancela alvos pendentes sem custo");
    }
    static void testMapOrientation(){
        Game g=fresh();g.settings.agent=1;g.start(true);View view=new View(g);BufferedImage canvas=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=canvas.createGraphics();
        for(double yaw:new double[]{0,Math.PI/2,Math.PI,Math.PI*1.5,.61,2.29}){
            g.player.yaw=yaw;g.openTactical(Ability.SMOKE);MapProjection m=view.tacticalMap();double px=m.x(g.player.x,g.player.z),py=m.y(g.player.x,g.player.z);
            double fx=g.player.x+Math.sin(yaw)*4,fz=g.player.z+Math.cos(yaw)*4,rx=g.player.x+Math.cos(yaw)*4,rz=g.player.z-Math.sin(yaw)*4;
            check(Math.abs(m.x(fx,fz)-px)<.0001&&m.y(fx,fz)<py&&m.x(rx,rz)>px&&Math.abs(m.y(rx,rz)-py)<.0001,"Mapa alinha frente/acima e direita/direita em "+Math.round(Math.toDegrees(yaw))+" graus");
            view.render(graphics,1280,720);view.click((int)Math.round(m.x(16,19)),(int)Math.round(m.y(16,19)));check(g.tacticalTargets.size()==1&&Math.hypot(g.tacticalTargets.get(0).x()-16,g.tacticalTargets.get(0).z()-19)<.20,"Clique inverso preserva o local real ao girar o mapa");
            int before=g.smokes.size();g.deployTactical();Smoke smoke=g.smokes.get(before);check(Math.hypot(smoke.x-16,smoke.z-19)<.20,"Smoke surge no ponto escolhido, sem espelhamento");g.smokes.clear();
        }
        graphics.dispose();
    }
    static Input.Frame trigger(boolean down,boolean click){return new Input.Frame(new java.util.BitSet(),new java.util.BitSet(),down,false,click,0,0,0,0);}
    static void testWeapons(){
        Game g=fresh();g.start(true);for(Actor a:g.actors)if(a!=g.player)a.dead=true;
        for(Weapon w:Weapon.values()){
            g.buyWeapon(w);g.weaponEquip=0;g.traces.clear();g.firePlayer();
            check(g.player.gun().kind==w&&g.player.slot==(w.sidearm()?1:2)&&g.player.gun().ammo==w.mag-1&&g.traces.size()==w.pellets,"Arma funcional: "+w.label+" / slot, munição e projéteis");
            Gun gun=g.player.gun();gun.ammo=1;gun.reserve=3;g.reload(g.player);gun.tick(5);check(gun.ammo==Math.min(w.mag,4)&&gun.reserve==Math.max(0,4-w.mag),"Recarga conserva munição: "+w.label);
        }
        g.buyWeapon(Weapon.TALON);g.buyWeapon(Weapon.ECHO);check(g.player.pistol.kind==Weapon.TALON&&g.player.primary.kind==Weapon.ECHO,"Pistola e principal usam inventários independentes");
        g.spawn(true);check(g.player.pistol.kind==Weapon.TALON&&g.player.primary.kind==Weapon.ECHO,"Sobrevivência preserva ambas as armas na rodada seguinte");
        g.player.dead=true;g.spawn(true);check(g.player.pistol.kind==Weapon.SPARK&&g.player.primary==null,"Morte restaura pistola gratuita e remove armas compradas");
        g.start(true);g.buyWeapon(Weapon.VEIL);g.weaponEquip=0;int shots=g.trainingShots;g.tick(1/60.,trigger(true,true));for(int i=0;i<40;i++)g.tick(1/60.,trigger(true,false));check(g.trainingShots-shots==1,"Pistola semiautomática dispara uma vez ao segurar o mouse");
        g.tick(1/60.,trigger(false,false));g.tick(1/60.,trigger(true,true));check(g.trainingShots-shots==2,"Novo clique dispara o segundo tiro da pistola");
        g.buyWeapon(Weapon.RUSH);g.weaponEquip=0;shots=g.trainingShots;for(int i=0;i<35;i++)g.tick(1/60.,trigger(true,i==0));check(g.trainingShots-shots>=6,"Pistola automática mantém fogo ao segurar o mouse");
        g.buyWeapon(Weapon.HELIX);g.weaponEquip=0;shots=g.trainingShots;g.tick(1/60.,trigger(true,true));for(int i=0;i<38;i++)g.tick(1/60.,trigger(false,false));check(g.trainingShots-shots==3&&g.player.gun().burstLeft==0,"Helix conclui três disparos após um único clique");
        g.player.gun().ammo=2;shots=g.trainingShots;g.tick(1/60.,trigger(true,true));for(int i=0;i<30;i++)g.tick(1/60.,trigger(false,false));check(g.trainingShots-shots==2&&g.player.gun().ammo==0,"Rajada respeita a munição restante");
        check(Weapon.DART.damageAt(35,false)<Weapon.DART.damageAt(5,false)*.3&&Weapon.TALON.damageAt(5,true)>150,"Escopeta curta perde dano à distância e revólver causa alto impacto");
        g=fresh();g.player.credits=500;g.buyWeapon(Weapon.VEIL);g.player.pistol.ammo=3;check(!g.buyWeapon(Weapon.VEIL)&&g.player.pistol.ammo==3&&g.player.credits==0,"Recomprar arma já possuída não repõe munição ou desconta créditos");
        check(!g.buyWeapon(Weapon.TALON)&&g.player.pistol.kind==Weapon.VEIL,"Compra negada preserva a pistola anterior");
    }
    static void testUltimates(){
        Game g=fresh();g.start(true);g.player.moveSpeed=4.8;g.recoil=.8;double spread=g.playerSpread();g.castSlot(2);
        check(g.focus==12&&g.playerSpread()<spread*.19,"Foco reduz a dispersão total, incluindo movimento e recuo");
        g.firePlayer();check(Math.abs(g.player.gun().cooldown-Weapon.ECHO.interval/1.3)<.00001,"Foco aumenta a cadência em 30%");
        g.player.gun().ammo=10;g.reload(g.player);check(Math.abs(g.player.gun().reload-Weapon.ECHO.reload*.6)<.00001,"Foco reduz o tempo de recarga em 40%");
        g=fresh();g.settings.agent=1;g.start(true);g.player.ult=6;g.castSlot(2);g.markTarget(20,19);check(g.tacticalAbility==Ability.ORBITAL&&g.deployTactical()&&g.orbitals.size()==1,"Suprema da Bruma exige escolha e confirmação no mapa");
        Actor target=g.actors.get(7);double hp=target.hp;g.tickOrbitals(1);check(target.hp==hp,"Ataque orbital respeita o aviso antes de causar dano");g.tickOrbitals(.7);check(target.hp<hp-30&&target.hp>0,"Ataque orbital causa dano durante sua janela ativa");g.tickOrbitals(1.1);check(target.dead,"Ataque orbital pode eliminar um alvo que permaneça na área");
        g=fresh();g.settings.agent=2;g.start(false);g.beginRound();g.player.hp=25;g.player.armor=0;Actor ally=g.actors.get(1);ally.hp=20;ally.armor=0;ally.x=g.player.x+2;ally.z=g.player.z;g.player.ult=6;g.castSlot(2);
        check(g.player.hp==60&&g.player.armor==25&&ally.hp==55&&ally.armor==25&&g.player.ult==0,"Ressonância recupera vida e proteção de aliados próximos e consome a suprema");
        g=fresh();g.start(true);g.player.hp=20;g.agent=Agent.BRUMA;g.ability(Ability.HEAL);g.damage(g.player,5,g.actors.get(5),false);check(g.player.healing==0,"Receber dano interrompe reparo");
        g.ability(Ability.SCAN);check(g.scanCount>0&&g.actors.stream().anyMatch(a->a.team==1&&a.revealed>=5),"Pulso revela alvos e informa a quantidade");
        g.ability(Ability.FLASH);check(g.abilities.projectiles.size()==1&&g.actors.stream().noneMatch(a->a.flash>0),"Clarão lança uma granada e respeita tempo de detonação");
    }
    static void testAce(){
        Game g=fresh();g.beginRound();for(int i=5;i<9;i++)g.damage(g.actors.get(i),1000,g.player,false);
        check(g.player.aces==0&&g.aceTime==0,"Quatro eliminações ainda não são ACE");
        g.damage(g.actors.get(9),1000,g.player,true);check(g.player.aces==1&&g.aceTime==6&&g.aceName.equals("VOCÊ"),"Cinco adversários distintos na mesma rodada geram ACE");
        g.damage(g.actors.get(9),1000,g.player,true);check(g.player.aces==1,"Um inimigo já eliminado não duplica o ACE");
        g.spawn(true);check(g.player.roundVictims.isEmpty()&&g.player.aces==1,"Nova rodada limpa a sequência e preserva o total de ACEs");
        g=fresh();g.start(true);Actor target=g.actors.get(5);for(int i=0;i<8;i++){target.dead=false;target.hp=100;g.damage(target,1000,g.player,false);}check(g.player.aces==0,"Alvos que reaparecem no treino não geram ACE falso");
    }
    static void testMatch(){
        Game g=fresh();g.beginRound();g.player.dead=true;int ticks=0,plants=0,lastRound=1;boolean wasPlanted=false;
        while(g.phase!=Phase.MATCH&&ticks<72000){
            if(g.phase==Phase.BUY){g.beginRound();g.player.dead=true;if(g.player.carrier){g.player.carrier=false;g.spikeX=g.player.x;g.spikeZ=g.player.z;}}
            g.tick(1/60.,Input.Frame.empty());if(g.planted&&!wasPlanted)plants++;wasPlanted=g.planted;
            if(g.round!=lastRound){System.out.println("SIM   rodada "+lastRound+" concluída, placar "+g.scoreBlue+":"+g.scoreRed);lastRound=g.round;}
            ticks++;
        }
        check(g.phase==Phase.MATCH&&(g.scoreBlue==5||g.scoreRed==5),"Partida autônoma completa sem travar ("+ticks+" ticks, "+plants+" plantios)");
        check(g.round<=9,"Partida termina em até nove rodadas");
    }
    static void testMenus(){
        Game g=new Game(new Settings(false),false,42);View v=new View(g);BufferedImage img=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D gg=img.createGraphics();
        v.render(gg,1280,720);v.click(200,415);check(g.ui.equals("modes")&&g.player==null,"Jogar abre escolha de modo, sem iniciar partida");
        v.render(gg,1280,720);v.click(600,193);v.click(1060,667);check(g.ui.equals("queue"),"Confirmar inicia fila local");
        g.tick(2.1,Input.Frame.empty());check(g.ui.equals("found"),"Fila local exige aceite ao encontrar partida");
        v.render(gg,1280,720);v.click(630,574);check(g.ui.equals("agents")&&g.player==null,"Aceitar leva à seleção de agentes");
        v.render(gg,1280,720);v.click(329,214);check(g.agent==Agent.ION,"Terceiro cartão seleciona Íon");v.render(gg,1280,720);v.click(1060,672);check(g.flow.locked&&g.player==null,"Travar aguarda os bots antes de iniciar");
        g.tick(2.7,Input.Frame.empty());g.tick(1.7,Input.Frame.empty());check(g.phase==Phase.BUY&&g.player.agentIndex==2,"Carregamento inicia a partida com o agente travado");
        g.tick(.016,Input.Frame.keys(java.awt.event.KeyEvent.VK_G));g.tick(.016,Input.Frame.keys(java.awt.event.KeyEvent.VK_2));g.tick(.016,Input.Frame.keys(java.awt.event.KeyEvent.VK_ENTER));check(g.ui.equals("play")&&g.agent==Agent.BRUMA&&g.player.qCharges==3,"Troca de agente disponível durante compra");
        g.ui="shop";g.player.credits=9000;v.render(gg,1280,720);v.click(1080,594);check(g.player.primary!=null&&g.player.primary.kind==Weapon.ECHO,"Botão do arsenal compra o fuzil");
        v.render(gg,1280,720);v.click(182,181);v.render(gg,1280,720);v.click(430,260);v.render(gg,1280,720);v.click(1080,594);check(g.player.pistol.kind==Weapon.TALON&&g.player.primary.kind==Weapon.ECHO,"Filtro e compra de pistola preservam o fuzil");
        g.beginRound();g.player.x=20;g.player.z=30;g.castSlot(0);v.render(gg,1280,720);MapProjection map=v.tacticalMap();v.click((int)Math.round(map.x(16,19)),(int)Math.round(map.y(16,19)));v.render(gg,1280,720);v.click(640,630);check(g.smokes.size()==1&&g.player.qCharges==2,"Mapa circular converte clique e confirma lançamento");
        g.ui="menu";v.render(gg,960,540);v.click(150,360);v.render(gg,960,540);v.click(850,500);check(g.phase==Phase.TRAIN,"Menu e seleção funcionam em janela redimensionada");
        gg.dispose();check(img.getRGB(350,250)!=0,"Renderização completa sem janela");
    }
    static void capture(String directory)throws Exception {
        Path dir=Path.of(directory);Files.createDirectories(dir);Game g=new Game(new Settings(false),false,47);g.settings.quality=2;View v=new View(g);
        shot(v,dir.resolve("01-menu.png"));g.ui="agents";shot(v,dir.resolve("02-agentes.png"));g.ui="help";shot(v,dir.resolve("03-controles.png"));
        g.start(true);g.noticeTime=0;g.weaponEquip=0;g.player.x=19;g.player.z=30;g.player.yaw=Math.PI-.24;g.player.pitch=-.015;shot(v,dir.resolve("04-jogo.png"));
        g.ui="shop";shot(v,dir.resolve("05-arsenal.png"));g.ui="settings";g.backUi="pause";shot(v,dir.resolve("06-configuracoes.png"));
        g.start(false);g.beginRound();g.noticeTime=0;g.player.primary=new Gun(Weapon.ECHO);g.player.slot=2;g.player.x=20;g.player.z=28;g.player.yaw=Math.PI+.25;
        for(Actor a:g.actors)if(a.team==0&&a!=g.player){a.x=4+a.id*3;a.z=28-a.id*2;a.yaw=Math.PI;}
        g.actors.get(5).x=18;g.actors.get(5).z=20;g.actors.get(5).yaw=0;
        shot(v,dir.resolve("07-combate.png"));g.scoreboard=true;shot(v,dir.resolve("08-placar.png"));
        g.settings.agent=1;g.start(true);g.weaponEquip=0;g.player.x=20;g.player.z=30;g.player.yaw=Math.PI+.20;
        g.castSlot(0);g.markTarget(16,19);g.markTarget(5,18);g.noticeTime=0;MapProjection map=v.tacticalMap();v.pointer((int)map.x(18,19),(int)map.y(18,19));shot(v,dir.resolve("09-mapa-smokes.png"));
        g.deployTactical();for(int i=0;i<72;i++)g.tick(1/60.,Input.Frame.empty());g.noticeTime=0;shot(v,dir.resolve("10-smokes.png"));
        g.castSlot(2);g.markTarget(18,19);g.deployTactical();for(int i=0;i<96;i++)g.tick(1/60.,Input.Frame.empty());g.noticeTime=0;shot(v,dir.resolve("11-ruptura.png"));
        g.settings.agent=0;g.start(true);g.player.yaw=Math.PI;g.castSlot(2);for(int i=0;i<30;i++)g.tick(1/60.,Input.Frame.empty());g.noticeTime=0;shot(v,dir.resolve("12-foco.png"));
        g.focus=0;g.castSlot(0);for(int i=0;i<10;i++)g.tick(1/60.,Input.Frame.empty());g.noticeTime=0;shot(v,dir.resolve("13-dash.png"));
        g.start(false);g.beginRound();g.player.x=20;g.player.z=30;g.player.yaw=Math.PI+.15;g.player.primary=new Gun(Weapon.ECHO);g.player.slot=2;g.weaponEquip=0;
        for(int i=5;i<10;i++)g.damage(g.actors.get(i),1000,g.player,false);for(int i=0;i<26;i++)g.tick(1/60.,Input.Frame.empty());g.noticeTime=0;shot(v,dir.resolve("14-ace.png"));
        g.settings.agent=2;g.start(true);g.weaponEquip=0;g.player.hp=25;g.castSlot(2);for(int i=0;i<20;i++)g.tick(1/60.,Input.Frame.empty());g.castSlot(0);g.noticeTime=0;shot(v,dir.resolve("15-ion-pulso.png"));
        g.ui="shop";v.shopCategory=Category.PISTOL;v.shopWeapon=Weapon.TALON;shot(v,dir.resolve("16-pistolas.png"));
        g.ui="play";g.buyWeapon(Weapon.TALON);g.weaponEquip=0;g.noticeTime=0;g.ultimateBurst=0;g.scan=0;for(Actor a:g.actors)a.revealed=0;g.pulses.clear();g.player.hp=100;g.player.healing=0;shot(v,dir.resolve("17-revolver.png"));
        g.buyWeapon(Weapon.BASTION);g.weaponEquip=0;g.noticeTime=0;shot(v,dir.resolve("18-metralhadora.png"));
        g.player.x=51;g.player.z=30;g.player.yaw=Math.PI;g.buyWeapon(Weapon.SHADE);g.weaponEquip=0;g.noticeTime=0;shot(v,dir.resolve("19-terminal-b.png"));
        g.settings.agent=1;g.start(true);g.player.yaw=Math.PI/2;g.castSlot(0);g.markTarget(16,19);g.noticeTime=0;shot(v,dir.resolve("20-mapa-leste.png"));
        weaponSheet(dir.resolve("21-modelos-arsenal.png"));UpdateTests.capture(dir);FeelTests.capture(dir);
        System.out.println("Capturas salvas em "+dir.toAbsolutePath());
    }
    static void weaponSheet(Path path)throws Exception{
        BufferedImage sheet=new BufferedImage(1750,735,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();Renderer renderer=new Renderer(350,195);g.setColor(new Color(0x102937));g.fillRect(0,0,1750,735);
        for(Weapon w:Weapon.values()){int x=w.ordinal()%5*350,y=w.ordinal()/5*245;g.drawImage(renderer.preview(w,2),x,y,null);g.setColor(new Color(w.color));g.setFont(new Font("SansSerif",Font.BOLD,19));g.drawString(w.label,x+17,y+220);g.setFont(new Font("SansSerif",Font.PLAIN,10));g.setColor(new Color(0xB5C5C7));g.drawString(w.mode.label,x+135,y+219);g.setColor(new Color(0x305363));g.drawRect(x,y,349,244);}
        g.dispose();ImageIO.write(sheet,"png",path.toFile());
    }
    static void shot(View v,Path path)throws Exception {BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();v.render(g,1280,720);g.dispose();ImageIO.write(image,"png",path.toFile());}
    static void benchmark(){
        Game g=fresh();g.beginRound();g.player.primary=new Gun(Weapon.ECHO);g.player.slot=2;Renderer r=new Renderer(854,480);
        for(int i=0;i<30;i++){g.tick(1/60.,Input.Frame.empty());r.render(g);}
        long start=System.nanoTime();for(int i=0;i<180;i++){g.tick(1/60.,Input.Frame.empty());g.player.yaw+=.015;r.render(g);}
        double ms=(System.nanoTime()-start)/1e6/180;System.out.printf(Locale.ROOT,"854x480 | 180 quadros | %.2f ms/quadro | %.1f FPS de render+simulação (sem janela)%n",ms,1000/ms);
        g.start(true);g.player.yaw=Math.PI;g.smokes.add(new Smoke(18,23));g.smokes.add(new Smoke(20,19));g.smokes.add(new Smoke(6,21));
        start=System.nanoTime();for(int i=0;i<180;i++){g.visualTime+=1/60.;r.render(g);}ms=(System.nanoTime()-start)/1e6/180;System.out.printf(Locale.ROOT,"854x480 | 3 smokes com interior oco | %.2f ms/quadro | %.1f FPS de render (sem janela)%n",ms,1000/ms);
    }
}
