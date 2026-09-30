package rift;

import java.awt.*;
import java.awt.image.*;
import java.nio.file.*;
import java.util.*;
import static rift.Game.*;
import static rift.World.*;
import static rift.Tests.*;

final class UpdateTests {
    static Game arena(){Game g=fresh();g.start(true);g.player.x=42;g.player.z=72;g.player.y=0;g.player.yaw=Math.PI;g.player.bodyYaw=Math.PI;g.player.pitch=0;g.player.armor=0;for(Actor a:g.actors)if(a!=g.player){a.dead=true;a.respawn=999;}return g;}
    static Actor enemy(Game g,double x,double z){Actor e=g.actors.get(5);e.dead=false;e.hp=100;e.armor=0;e.x=x;e.y=0;e.z=z;e.yaw=0;e.invulnerable=0;return e;}
    static void run()throws Exception{
        check(Agent.values().length==11,"Elenco contém onze agentes");
        Set<String> roles=new HashSet<>();for(Agent a:Agent.values()){roles.add(a.role.replace("CONTROLADORA","CONTROLADOR"));check(new HashSet<>(java.util.List.of(a.q,a.c,a.e,a.x)).size()==4,"Quatro habilidades distintas: "+a.name);}check(roles.size()==4,"Quatro funções representadas");
        testMaps();testStepAndFall();testSmokes();testAbilities();testUtilities();testModes();testProgression();testAnimation();
    }
    static void testMaps(){
        for(int index=0;index<3;index++){
            Game g=arena();g.flow.mapIndex=index;g.start(false);World w=g.world;
            check(w.solids.size()>10&&w.ramps.size()>=3&&World.WIDTH*World.LENGTH>64*60*1.7,"Mapa maior e com alturas: "+World.NAMES[index]);
            for(Actor a:g.actors)check(!w.blocked(a.x,a.z,a.y,.31,1.78),"Spawn livre no mapa "+index+": "+a.id);
            check(!w.path(40,74,w.a.x(),w.a.z()).isEmpty()&&!w.path(40,74,w.b.x(),w.b.z()).isEmpty(),"Ambos os objetivos acessíveis no mapa "+index);
            for(Ramp r:w.ramps){
                double x=(r.x1()+r.x2())/2,z=r.reverse()?r.z2()+.5:r.z1()-.5,sign=r.reverse()?-1:1;
                g.player.x=x;g.player.z=z;g.player.y=0;g.player.grounded=true;for(int i=0;i<260;i++)g.move(g.player,0,sign*.07);
                check(g.player.y>=r.height()-.05,"Subida real de rampa "+index+" / "+r.x1());
                double topZ=r.reverse()?r.z1()-.3:r.z2()+.3;java.util.List<V> path=w.path(x,z,x,topZ);check(path.stream().anyMatch(v->v.y()>r.height()-.5),"Bots possuem rota com elevação "+index+" / "+r.x1());
                double middle=(r.z1()+r.z2())/2;double hit=w.ray(new V(x,8,middle),new V(0,-1,0),10);check(Math.abs(hit-(8-r.height()/2))<.001,"Tiro intercepta superfície inclinada "+index);
            }
        }
    }
    static void testStepAndFall(){
        Game g=arena();g.player.x=21.6;g.player.z=14;g.player.yaw=Math.PI/2;g.player.vx=5.18;
        g.controlPlayer(1/60.,Input.Frame.keys(java.awt.event.KeyEvent.VK_SPACE,java.awt.event.KeyEvent.VK_W));boolean climbed=false;
        for(int i=0;i<80;i++){g.controlPlayer(1/60.,Input.Frame.keys(java.awt.event.KeyEvent.VK_W));if(g.player.x>23&&g.player.x<24.8&&g.player.y>=1.19)climbed=true;}
        check(climbed,"Salto com degrau não prende o jogador dentro de caixas baixas");
        g.player.x=10-.1;g.player.z=14;g.player.y=2.4;g.player.grounded=true;g.move(g.player,.5,0);check(!g.player.grounded&&g.player.y>2,"Sair da plataforma inicia queda, sem teleportar ao chão");
    }
    static void testSmokes(){
        Game g=arena();g.castSlot(1);check(g.ui.equals("play")&&g.abilities.projectiles.size()==1&&g.smokes.isEmpty(),"Vértice lança C diretamente, sem mapa");
        for(int i=0;i<27;i++)g.abilities.tick(1/60.);check(g.smokes.size()==1&&g.abilities.projectiles.isEmpty(),"Smoke rápida chega em menos de meio segundo");Smoke s=g.smokes.get(0);s.tick(.2);check(s.radius()>2.5&&s.life<4.5,"Smoke rápida expande e tem duração curta");
        g.smokes.clear();s=new Smoke(42,72);g.smokes.add(s);check(!g.obscured(new V(41,1.6,72),new V(43,1.6,72)),"Dois alvos dentro da mesma smoke podem se ver");check(g.obscured(new V(42,1.6,65),new V(42,1.6,72)),"Borda da smoke bloqueia visão do exterior");
        Renderer r=new Renderer(320,180);r.camera(42,1.6,72,0,0,Math.PI/2);Arrays.fill(r.pixels,0xE53322);Arrays.fill(r.depth,1f);r.volumeSmoke(s,1);int inside=r.pixels[90*320+160];check((inside>>16&255)>200&&(inside>>8&255)<80,"Render preserva objetos próximos no interior oco");Arrays.fill(r.pixels,0xE53322);Arrays.fill(r.depth,.01f);r.volumeSmoke(s,1);check(r.pixels[90*320+160]!=inside,"Casca esconde objetos além da esfera");
    }
    static void testAbilities(){
        Game g=arena();Actor e=enemy(g,42,66);g.ability(Ability.FLASH);check(e.flash==0&&g.abilities.projectiles.size()==1,"Íon não cega instantaneamente ao apertar C");
        g.abilities.flash(new V(42,1.6,68));check(e.flash>2&&g.player.flash>1,"Explosão cega inimigo e também o usuário olhando para ela");e.yaw=Math.PI;e.flash=0;g.abilities.flash(new V(42,1.6,68));check(e.flash>0&&e.flash<1,"Virar de costas reduz a duração do clarão");
        g=arena();g.ability(Ability.CURVEFLASH);double old=g.abilities.projectiles.get(0).velocity.x();g.abilities.tick(.15);check(Math.abs(g.abilities.projectiles.get(0).velocity.x()-old)>2,"Clarão curvo muda a trajetória em voo");
        g=arena();g.ability(Ability.UPDRAFT);double max=0;for(int i=0;i<100;i++){g.controlPlayer(1/60.,Input.Frame.empty());max=Math.max(max,g.player.y);}check(max>3.8,"Ascensão alcança plataformas elevadas");
        g=arena();g.ability(Ability.STIM);g.abilities.tick(.2);check(g.player.stim>0,"Área aceleradora aplica buff de cadência e movimento");
        g=arena();e=enemy(g,42,69);g.ability(Ability.SUPPRESS);Abilities.Projectile p=g.abilities.projectiles.get(0);p.position=new V(42,1,69);g.abilities.detonate(p);check(e.emp>=5,"Disco suprime habilidades na área");g.player.emp=5;g.player.ult=6;g.castSlot(2);check(g.focus==0&&g.player.ult==6,"Supressão realmente bloqueia a suprema sem consumir pontos");
        g=arena();e=enemy(g,43,71);g.ability(Ability.TOXIC_DOME);double hp=e.hp;g.abilities.tick(.5);check(e.hp<hp&&e.revealed>0&&g.smokes.get(0).maxRadius==8,"Biosfera corrói e marca invasores próximos");
        g=arena();e=enemy(g,42,69);g.ability(Ability.TOXIC_WALL);check(g.obscured(new V(40,1.6,67),new V(44,1.6,67)),"Cortina tóxica bloqueia visão ao cruzá-la");g.abilities.tick(.5);check(e.hp<100&&e.slow>0,"Cortina afeta inimigos que a atravessam");
        g=arena();g.ability(Ability.BLINK);check(g.player.z==72&&g.player.teleportTime>0,"Teleporte curto tem canalização");g.abilities.tickActor(g.player,.5);check(g.player.z<65,"Passo sombrio chega ao destino válido");
        g=arena();g.abilities.teleport(g.player,new V(50,0,72),1.5);g.damage(g.player,5,g.actors.get(5),false);g.abilities.tickActor(g.player,2);check(g.player.x==42&&g.player.teleportTime==0,"Dano cancela teleporte longo");
        g=arena();g.ability(Ability.RETURN);g.player.x=47;g.damage(g.player,150,g.actors.get(5),false);check(!g.player.dead&&g.player.hp==100&&g.player.x==42&&g.player.returnTime==0,"Renascimento devolve o jogador vivo à marca");
        g=arena();g.ability(Ability.BARRIER);check(g.abilities.devices.size()==1&&!g.world.temporary.isEmpty(),"Barreira cria colisão física");Abilities.Device d=g.abilities.devices.get(0);g.move(g.player,0,-8);check(g.player.z>d.position.z(),"Jogador não atravessa barreira");Actor shooter=enemy(g,42,64);for(int i=0;i<12;i++)g.abilities.hitDevice(shooter,shooter.eye(),new V(0,0,1),15,55);g.abilities.tick(.02);check(g.world.temporary.isEmpty(),"Tiros destroem a barreira e removem colisão");
        g=arena();e=enemy(g,42,65);g.ability(Ability.TURRET);g.abilities.tick(.5);check(e.hp<100,"Torreta causa dano a alvo visível");
        g=arena();e=enemy(g,42,67);g.ability(Ability.LOCKDOWN);g.abilities.tick(7.3);check(e.detained==6,"Confinamento desarma após aviso de sete segundos");
        g=arena();e=enemy(g,42,67);g.ability(Ability.LOCKDOWN);g.abilities.devices.get(0).hp=0;g.abilities.tick(7.3);check(e.detained==0,"Destruir o dispositivo impede o confinamento");
        g=arena();Actor ally=g.actors.get(1);ally.x=43;ally.y=0;ally.z=70;g.ability(Ability.REVIVE);check(!ally.dead&&ally.hp==100,"Reanimação restaura aliado caído próximo");
        g=arena();g.ability(Ability.SATCHEL);check(g.player.vy>8&&g.dashTime>0,"Propulsor combina movimento vertical e horizontal");
        g=arena();g.abilities.ping();check(g.abilities.pingPoint!=null&&g.abilities.pingLife==7,"Ping registra posição no mundo");
    }
    static void testModes(){
        for(MatchFlow.Mode mode:MatchFlow.Mode.values()){
            Game g=fresh();g.flow.mode=mode;g.start(false);
            if(mode.respawn){check(g.phase==Phase.LIVE&&g.actors.stream().noneMatch(a->a.carrier),"Modo de abates sem Spike: "+mode.label);g.player.invulnerable=0;g.damage(g.player,1000,g.actors.get(5),false);g.flow.tickRespawn(3.1);check(!g.player.dead&&g.player.hp==100,"Respawn funciona em "+mode.label);if(mode==MatchFlow.Mode.DEATHMATCH)check(g.actors.stream().map(a->a.team).distinct().count()==10,"Mata-Mata tem dez equipes individuais");g.player.kills=mode.target;g.flow.tickRespawn(.01);check(g.phase==Phase.MATCH&&g.flow.matchWon,"Limite de abates encerra "+mode.label);}
            else {g.scoreBlue=mode.target-1;g.beginRound();g.finishRound(0,"Teste de fim");g.tick(5.1,Input.Frame.empty());check(g.phase==Phase.MATCH,"Condição de vitória própria: "+mode.label);if(mode==MatchFlow.Mode.PREMIER){check(g.ui.equals("tournament"),"Copa avança da semifinal à final");g.flow.nextTournament();g.tick(1.7,Input.Frame.empty());check(g.flow.seriesStage==1&&g.phase==Phase.BUY,"Final carrega outro mapa e nova partida");}}
        }
        Game g=fresh();g.flow.queue();g.tick(2.1,Input.Frame.empty());g.tick(15.1,Input.Frame.empty());check(g.ui.equals("modes"),"Aceite expira sem iniciar partida automaticamente");
    }
    static void testUtilities(){
        for(Ability type:new Ability[]{Ability.INCENDIARY,Ability.ACID,Ability.FRAG,Ability.ROCKET}){
            Game g=arena();Actor target=enemy(g,42,66);g.abilities.launch(g.player,type);Abilities.Projectile p=g.abilities.projectiles.get(0);p.position=new V(42,.1,66);g.abilities.detonate(p);g.abilities.projectiles.clear();g.abilities.tick(.45);check(target.hp<100,"Dano funcional da habilidade "+type);if(type==Ability.ACID)check(target.vulnerable>0,"Corrosão aplica vulnerabilidade");
        }
        Game g=arena();g.abilities.launch(g.player,Ability.SLOW);Abilities.Projectile p=g.abilities.projectiles.get(0);p.position=new V(42,.1,72);g.abilities.detonate(p);g.abilities.projectiles.clear();g.abilities.tick(.01);double start=g.player.z;for(int i=0;i<60;i++)g.controlPlayer(1/60.,Input.Frame.keys(java.awt.event.KeyEvent.VK_W));check(start-g.player.z<2.1&&start-g.player.z>1.5,"Campo de lentidão reduz o deslocamento real");
        g=arena();g.player.hp=30;g.abilities.zones.add(new Abilities.Zone(Ability.HEAL_FIRE,g.player,new V(42,0,72),4,5));g.abilities.tick(1);check(g.player.hp==48,"Fogo de Solar recupera vida do usuário");
        g=arena();Actor e=enemy(g,42,69);g.ability(Ability.TRAP);g.abilities.tick(.1);check(e.slow==4&&e.revealed==5,"Armadilha detecta e desacelera invasor");
        g=arena();e=enemy(g,42,65);e.moveSpeed=4;g.ability(Ability.SENSOR);g.abilities.tick(.1);check(e.revealed==2,"Sensor revela posição em pulso");
        g=arena();e=enemy(g,42,65);g.ability(Ability.HUNTER_BOT);for(int i=0;i<180;i++)g.abilities.tick(1/60.);check(e.hp<100,"Robô percorre rota e explode perto do alvo");
        g=arena();e=enemy(g,42,65);g.ability(Ability.BLIND_WAVE);for(int i=0;i<25;i++)g.abilities.tick(1/60.);check(e.nearSight>0,"Onda de visão reduzida atinge alvos à frente");
        g=arena();g.agent=Agent.ESPECTRO;g.player.agentIndex=4;g.castSlot(3);check(g.ui.equals("tactical")&&g.tacticalAbility==Ability.ECLIPSE,"E do Espectro abre sua visão astral para posicionar estrela");g.markTarget(42,68);g.deployTactical();check(g.abilities.zones.stream().anyMatch(zone->zone.type==Ability.ECLIPSE),"Confirmação astral cria o vórtice de visão reduzida");g.castSlot(2);g.markTarget(70,70);g.deployTactical();g.abilities.tickActor(g.player,1.6);check(g.player.x==70&&g.player.z==70,"Suprema do Espectro transporta ao ponto global escolhido");
        g=fresh();g.player.qCharges=0;g.player.credits=500;check(g.buyAbility(0)&&g.player.credits==300&&g.player.qCharges==1,"Compra de habilidade repõe carga e debita créditos");g.spawn(true);check(g.player.qCharges==1,"Próxima rodada preserva cargas compradas restantes");
    }
    static void testProgression()throws Exception{
        Path dir=Files.createTempDirectory("rift-profile-check");try{
            Path file=dir.resolve("profile.properties");Profile p=new Profile(file);check(p.wallet==1800&&!p.unlocked(8),"Contratos iniciais permitem liberar os seis novos agentes");check(p.unlock(8)&&p.wallet==1500,"Desbloqueio debita XP uma vez");p.unlock(8);check(p.wallet==1500,"Desbloqueio repetido não debita XP");Profile read=new Profile(file);check(read.unlocked(8)&&read.wallet==1500,"Contrato persiste após reabrir o perfil");
            Game g=fresh();g.flow.mode=MatchFlow.Mode.COMPETITIVE;g.player.kills=7;g.player.aces=1;p.reward(g,true);read=new Profile(file);check(read.xp==640&&read.wins==1&&read.aces==1&&read.rating==278&&read.history.size()==1,"XP, maestria, destaques e rank persistem");
        }finally{try(var files=Files.list(dir)){for(Path f:files.toList())Files.delete(f);}Files.delete(dir);}
    }
    static void testAnimation(){Actor a=new Actor(0,0,"RIG");a.moveSpeed=4.8;a.yaw=Math.PI/2;CharacterModel.animate(a,.016);check(a.animSpeed>0&&a.animSpeed<4.8&&a.bodyYaw>0&&a.bodyYaw<Math.PI/2,"Velocidade e rotação do corpo fazem transição gradual");java.util.List<Tri> idle=new ArrayList<>(),moving=new ArrayList<>();CharacterModel.add(idle,a,0,true);a.animSpeed=4.8;a.walk=1;CharacterModel.add(moving,a,.2,true);check(!idle.equals(moving)&&idle.size()>100,"Rig articulado muda vértices durante caminhada");}
    static void capture(Path dir)throws Exception{
        Game g=arena();View v=new View(g);g.settings.quality=2;g.ui="modes";shot(v,dir.resolve("22-modos.png"));g.flow.queue();g.tick(2.1,Input.Frame.empty());shot(v,dir.resolve("23-aceitar.png"));g.flow.accept();g.selectAgent(6);g.flow.lockAgent();g.flow.botLocks=7;shot(v,dir.resolve("24-travar.png"));
        for(int i=0;i<3;i++){g.flow.mapIndex=i;g.start(true);g.noticeTime=0;g.weaponEquip=0;World.Ramp ramp=g.world.ramps.get(0);g.player.x=(ramp.x1()+ramp.x2())/2;g.player.z=ramp.reverse()?ramp.z1()-1:ramp.z2()+1;g.player.y=ramp.height();g.player.yaw=i==0?-1.55:1.25;g.player.pitch=-.15;shot(v,dir.resolve("25-mapa-"+i+"-altura.png"));}
        g.flow.mapIndex=0;g.start(true);g.player.x=42;g.player.z=72;g.player.yaw=Math.PI;g.player.y=0;g.weaponEquip=0;g.smokes.clear();g.smokes.add(new Smoke(42,72));Actor near=g.actors.get(5);near.x=42;near.z=70.5;near.y=0;near.dead=false;g.noticeTime=0;shot(v,dir.resolve("26-smoke-por-dentro.png"));
        g.ui="profile";shot(v,dir.resolve("27-carreira.png"));
        BufferedImage sheet=new BufferedImage(1350,1050,BufferedImage.TYPE_INT_RGB);Graphics2D p=sheet.createGraphics();Renderer r=new Renderer(450,350);for(int i=0;i<9;i++){int x=i%3*450,y=i/3*350;p.drawImage(r.agentPreview(i,2.5),x,y,null);View.text(p,Agent.values()[i].name,x+20,y+36,23,new Color(Agent.values()[i].color),true);}p.dispose();javax.imageio.ImageIO.write(sheet,"png",dir.resolve("28-elenco.png").toFile());
    }
}
