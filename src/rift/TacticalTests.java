package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import static rift.Game.*;
import static rift.World.*;
import static rift.Tests.*;
import static rift.UpdateTests.*;
import static java.awt.event.KeyEvent.*;

/** Behavioral contracts for the 1.5 systems, including their destructive counterplay. */
final class TacticalTests {
    static void run()throws Exception{unique();throwables();sentinels();bhop();settings();core();maps();}
    static void unique(){
        Set<Ability> kits=new HashSet<>();for(Agent a:Agent.values())for(int i=0;i<4;i++)check(kits.add(a.slot(i)),"Habilidade exclusiva no elenco: "+a.name+" / "+abilityName(a.slot(i)));
        Set<Integer> icons=new HashSet<>();for(Ability a:Ability.values()){BufferedImage image=new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();AbilityArt.icon(p,a,32,32,25,Color.WHITE);p.dispose();icons.add(Arrays.hashCode(image.getRGB(0,0,64,64,null,0,64)));}check(icons.size()==44,"44 ícones com desenhos distintos");
    }
    static void throwables(){ImpactTests.throwables();}
    static void sentinels(){
        Game g=arena();g.selectAgent(9);g.training=false;g.phase=Phase.LIVE;g.castSlot(0);check(g.player.slot==4&&g.player.specialPistol.ammo==6&&g.player.qCharges==0,"Veredito invoca seis tiros e consome carga");
        Actor e=enemy(g,42,68);g.shoot(g.player,e.center().sub(g.player.eye()).unit(),g.player.gun().kind);check(Math.abs(e.hp-30)<.01,"Veredito aplica 70 de dano no corpo");
        g.combat.equip(2);g.castSlot(0);check(g.player.slot==4&&g.player.qCharges==0,"Reequipar Veredito não cobra segunda carga");
        g.player.ult=6;g.castSlot(2);e.hp=100;e.armor=50;g.shoot(g.player,e.center().sub(g.player.eye()).unit(),g.player.gun().kind);check(e.dead&&g.player.specialRifle.ammo==5&&g.player.ult==1,"Execução elimina alvo com 150 totais e gasta suprema");
        g.castSlot(3);var anchor=g.sentinels.own(g.player,Ability.ANCHOR);double z=g.player.z;g.player.z-=4;g.castSlot(3);check(g.player.teleportTime>0&&anchor.life==0&&g.player.eCharges==0,"Reativar âncora consome dispositivo, não outra carga");g.abilities.tickActor(g.player,.4);check(Math.abs(g.player.z-z)<.001&&g.player.vx==0,"Âncora retorna ao local e zera velocidade");
        g.abilities.clear();g.ability(Ability.BULWARK);var shield=g.sentinels.own(g.player,Ability.BULWARK);e=enemy(g,42,66);double hp=g.player.hp;g.shoot(e,g.player.center().sub(e.eye()).unit(),Weapon.ECHO);check(g.player.hp==hp&&shield.hp<250,"Apólice intercepta tiro inimigo");g.move(g.player,0,-4);check(g.player.z<shield.position.z(),"Apólice permite passagem de jogadores");
        g=arena();g.selectAgent(10);g.player.x=42;g.player.z=72;g.player.yaw=Math.PI/2;g.player.pitch=Math.atan2(-.93,8);g.ability(Ability.TRIPWIRE);var wire=g.sentinels.own(g.player,Ability.TRIPWIRE);check(wire!=null&&wire.endpoint.sub(wire.position).length()<12,"Fio conecta paredes atingidas pela mira");wire.age=.5;e=enemy(g,46,72);e.y=1;g.abilities.tick(.01);check(wire.life>0&&e.detained==0,"Saltar por cima evita fio");e.y=0;g.abilities.tick(.01);check(e.detained>0&&e.revealed==4&&wire.tethered==e,"Cruzar fio prende e revela durante janela de contra-ataque");g.abilities.tick(1);check(wire.life<=0&&e.detained>1,"Fio conclui atordoamento após aviso");
        g=arena();g.selectAgent(10);g.ability(Ability.CAGE);var cage=g.sentinels.own(g.player,Ability.CAGE);check(!cage.active,"Gaiola espera reativação");g.castSlot(1);check(cage.active&&cage.life==7,"Reativação inicia sete segundos de gaiola");V p=cage.position.add(new V(0,1,0));check(g.obscured(p,p.add(new V(0,0,5)))&&!g.obscured(p,p.add(new V(0,0,1))),"Gaiola esconde através da borda e mantém interior oco");
        g=arena();g.selectAgent(10);g.player.yaw=Math.PI/2;g.castSlot(3);var camera=g.sentinels.own(g.player,Ability.SPYCAM);check(camera!=null,"Olho remoto instala em superfície atingida pelo raio");g.castSlot(3);check(g.sentinels.watching()&&g.cameraActor()==g.sentinels.eye,"Reativação troca a visão para câmera");e=enemy(g,42,72);V delta=e.center().sub(g.sentinels.eye.eye());g.sentinels.eye.yaw=Math.atan2(delta.x(),delta.z());g.sentinels.eye.pitch=Math.atan2(delta.y(),Math.hypot(delta.x(),delta.z()));g.sentinels.control(.01,trigger(true,true));check(e.revealed==5,"Dardo da câmera marca inimigo realmente visível");g.damage(g.player,1000,e,false);g.sentinels.tick(.01);check(!g.sentinels.watching(),"Morrer no corpo encerra controle remoto");
        g=arena();g.selectAgent(10);g.actors.get(5).x=100;g.actors.get(5).z=100;for(Actor a:g.actors)if(a.team==1){a.x=100;a.z=100;}check(!g.ability(Ability.NETWORK),"Rastreamento não ativa sem corpo próximo");g.actors.get(5).x=42;g.actors.get(5).z=71;e=g.actors.get(6);e.dead=false;e.revealed=0;check(g.ability(Ability.NETWORK)&&e.revealed==1.2,"Corpo próximo habilita primeira varredura global");e.revealed=0;g.sentinels.tick(3.1);check(e.revealed==1.2,"Segunda varredura ocorre após três segundos");
        g=arena();e=enemy(g,42,68);g.ability(Ability.SENSOR);g.abilities.tick(.1);check(e.revealed==0,"Sensor acústico não revela alvo parado");e.moveSpeed=4;g.abilities.tick(2.1);check(e.revealed>0,"Corrida ativa sensor acústico");
        g=arena();e=enemy(g,42,68);e.revealed=5;g.abilities.zones.add(new Abilities.Zone(Ability.ECLIPSE,g.player,e.center().sub(new V(0,1,0)),4,9));g.abilities.tick(.1);check(e.nearSight>0&&e.revealed==0,"Eclipse reduz visão e apaga revelação");g.close();
    }
    static void bhop(){
        Game g=arena();g.player.slot=3;Actor a=g.player;a.vx=6.2;a.vz=0;a.grounded=false;a.intentX=1;a.intentZ=0;g.combat.movement(.1,6.2);check(Math.abs(Math.hypot(a.vx,a.vz)-6.2)<.001,"Segurar frente no ar não concede aceleração gratuita");
        for(int i=0;i<240;i++){a.x=42;a.z=72;double heading=Math.atan2(a.vz,a.vx)+Math.acos(1.8/Math.hypot(a.vx,a.vz));a.intentX=Math.cos(heading);a.intentZ=Math.sin(heading);g.combat.movement(1/240.,6.2);}
        check(Math.hypot(a.vx,a.vz)>8&&Math.hypot(a.vx,a.vz)<=11.501,"Air strafe bem orientado ganha velocidade com limite de 11,5 m/s");
        double[] gain=new double[3];int[] rates={60,120,144};for(int k=0;k<rates.length;k++){a.vx=6.2;a.vz=0;a.grounded=false;for(int i=0;i<rates[k];i++){a.x=42;a.z=72;double angle=Math.atan2(a.vz,a.vx)+Math.acos(1.8/Math.hypot(a.vx,a.vz));a.intentX=Math.cos(angle);a.intentZ=Math.sin(angle);g.combat.movement(1./rates[k],6.2);}gain[k]=Math.hypot(a.vx,a.vz);}check(Arrays.stream(gain).max().orElse(0)-Arrays.stream(gain).min().orElse(0)<.25,"Ganho por strafe consistente entre taxas de atualização");
        g=arena();a=g.player;a.x=24;a.z=98;a.slot=3;a.vz=-7.5;a.vx=0;a.yaw=Math.PI;g.weaponEquip=0;
        for(int hop=0;hop<3;hop++){g.controlPlayer(1/120.,Input.Frame.keys(VK_SPACE));for(int i=0;i<150&&!a.grounded;i++)g.controlPlayer(1/120.,Input.Frame.empty());}
        check(g.combat.hops==3&&Math.hypot(a.vx,a.vz)>7.4&&a.z<84,"Três saltos sincronizados preservam impulso e deslocam pelo corredor");
        for(int i=0;i<30;i++)g.controlPlayer(1/120.,Input.Frame.empty());check(Math.hypot(a.vx,a.vz)<.01,"Permanecer no chão aplica atrito e encerra ganho de bhop");
        g=arena();var bits=new BitSet();bits.set(VK_SPACE);g.controlPlayer(1/120.,Input.Frame.keys(VK_SPACE));for(int i=0;i<240;i++)g.controlPlayer(1/120.,new Input.Frame(bits,new BitSet(),false,false,false,0,0,0,0));check(g.player.grounded&&g.combat.hops==1,"Segurar espaço sozinho não automatiza saltos");g.close();
    }
    static void settings()throws Exception{
        Path dir=Files.createTempDirectory("rift-settings-");Path path=dir.resolve("settings.properties");try{
            Settings s=new Settings(path);s.fov=105;s.volume=.8;s.effectsVolume=.2;s.musicVolume=.4;s.softFlash=true;s.lowMotion=true;s.abilityHold=true;s.renderDistance=100;s.agent=10;s.bind(Settings.Action.JUMP,VK_K);s.save();Settings loaded=new Settings(path);
            check(loaded.fov==105&&loaded.agent==10&&loaded.softFlash&&loaded.lowMotion&&loaded.renderDistance==100,"Preferências visuais, acessibilidade e agente persistem");check(loaded.effectsVolume==.2&&loaded.musicVolume==.4&&loaded.volume==.8,"Três volumes persistem independentemente");
            Input.Frame mapped=loaded.remap(Input.Frame.keys(VK_K));check(mapped.pressed(VK_SPACE)&&!mapped.pressed(VK_K)&&loaded.binds[Settings.Action.QUICKBUY.ordinal()]==VK_SPACE,"Remapeamento troca conflitos sem duplicar ações");check(!loaded.bind(Settings.Action.JUMP,VK_ESCAPE),"Tecla de saída permanece reservada");
            Files.writeString(path,"fov=999\nsensitivity=NaN\nvolume=-5\nagent=987\n");loaded=new Settings(path);check(loaded.fov==110&&Double.isFinite(loaded.sensitivity)&&loaded.volume==0&&loaded.agent==10,"Configuração inválida recupera limites seguros");
        }finally{Files.deleteIfExists(path);Files.delete(dir);}
        Game g=arena();double time=g.time;g.tick(.01,Input.Frame.keys(VK_F10));check(g.ui.equals("settings"),"F10 abre configurações durante a partida");g.tick(.5,Input.Frame.empty());check(g.time==time,"Configurações pausam simulação local");g.tick(.01,Input.Frame.keys(VK_F10));check(g.ui.equals("play"),"F10 retorna ao jogo");
        g.settings.crouchToggle=true;g.tick(.01,Input.Frame.keys(VK_CONTROL));g.tick(.02,Input.Frame.empty());check(g.player.crouch,"Agachar alternável permanece após soltar tecla");
        g.settings.aimToggle=true;g.weaponEquip=0;g.tick(.01,new Input.Frame(new BitSet(),new BitSet(),false,true,false,0,0,0,0));g.tick(.02,Input.Frame.empty());check(g.aiming,"ADS alternável permanece após soltar mouse");g.close();
    }
    static void core(){
        Game g=arena();g.player.x=74;g.player.z=62;Actor e=enemy(g,74,55);g.shoot(g.player,e.center().sub(g.player.eye()).unit(),Weapon.ECHO);check(e.hp<100&&e.hp>65,"Painel fino deixa passar fuzil com perda de dano");e.hp=100;g.shoot(g.player,e.center().sub(g.player.eye()).unit(),Weapon.SPARK);check(e.hp==100,"Pistola leve não atravessa painel acima da sua penetração");
        g=arena();e=enemy(g,42,68);g.shoot(g.player,new V(e.x,.35,e.z).sub(g.player.eye()).unit(),Weapon.ECHO);check(Math.abs(e.hp-73.75)<.01,"Dano nas pernas aplica multiplicador de 75%");
        g=fresh();g.flow.mode=MatchFlow.Mode.COMPETITIVE;g.scoreBlue=7;g.scoreRed=6;g.round=13;check(!g.matchFinished(),"Competitivo exige vantagem de duas rodadas na prorrogação");g.scoreBlue=8;check(g.matchFinished(),"Dois pontos de vantagem encerram prorrogação");g.scoreBlue=10;g.scoreRed=9;g.round=19;check(g.matchFinished(),"Rodada 19 funciona como morte súbita");g.scoreBlue=g.scoreRed=6;g.round=13;g.spawn(true);check(g.player.credits==5000&&g.attackTeam==0&&g.actors.stream().filter(a->a.carrier).count()==1,"Prorrogação concede 5.000 créditos e portador válido");g.round=14;g.spawn(true);check(g.attackTeam==1&&g.actors.stream().filter(a->a.carrier).count()==1,"Prorrogação alterna lados a cada rodada");
        g=fresh();g.player.credits=800;g.actors.get(1).credits=2500;g.requestFunds();check(g.player.credits==1400&&g.actors.get(1).credits==1900,"Pedido transfere 600 créditos de aliado local");g.requestFunds();check(g.player.credits==1400,"Só um pedido de créditos por rodada");
        g=arena();g.training=false;g.phase=Phase.BUY;g.player.credits=9000;g.profile.savedPrimary=Weapon.WISP;g.profile.savedPistol=Weapon.VEIL;g.player.primary=null;g.quickBuy();check(g.player.primary.kind==Weapon.WISP&&g.player.pistol.kind==Weapon.VEIL&&g.player.armor==50&&g.player.credits==5900,"Recompra respeita loadout e preços reais");
        g=arena();g.player.x=43;g.player.z=67;g.player.ult=2;g.player.moveSpeed=0;g.orbs.tick(.95,true);check(g.orbs.taken[0]&&g.player.ult==3,"Segurar interação coleta orbe compartilhado uma vez");g.orbs.tick(2,true);check(g.player.ult==3,"Orbe coletado não duplica pontos");g.orbs.reset();check(!g.orbs.taken[0],"Nova rodada repõe orbes");
        g=arena();g.player.eCharges=0;g.player.eRegen=29.99;g.tick(.02,Input.Frame.empty());check(g.player.eCharges==1,"Assinatura recarrega após 30 segundos");g.close();
    }
    static void maps(){for(int index=0;index<3;index++){World w=new World(index);int walkable=0;for(boolean n:w.nav)if(n)walkable++;check(walkable>World.NW*World.NH*.20&&walkable<World.NW*World.NH*.48,"Layout "+index+" dedica espaço a rotas controladas, sem planície aberta");for(Site site:java.util.List.of(w.a,w.b)){check(!w.path(68,120,site.x(),site.z()).isEmpty()&&!w.path(68,5,site.x(),site.z()).isEmpty(),"Ataque e defesa alcançam "+site.name()+" no layout "+index);}check(!w.visible(new V(68,1.6,120),new V(w.a.x(),1.6,w.a.z())),"Sem visão direta de base até site no layout "+index);}}
    static void capture(Path dir)throws Exception{
        Files.createDirectories(dir);Game g=arena();g.settings.quality=2;g.noticeTime=0;g.weaponEquip=0;View v=new View(g);g.ui="menu";shot(v,dir.resolve("01-menu.png"));
        g.ui="agents";g.pendingTraining=true;g.selectAgent(9);shot(v,dir.resolve("02-aureo.png"));g.selectAgent(10);shot(v,dir.resolve("03-trama.png"));
        g.ui="settings";for(int i=0;i<5;i++){v.settingsUI.tab=i;shot(v,dir.resolve("04-configuracoes-"+i+".png"));}
        for(int i=0;i<3;i++){g.flow.mapIndex=i;g.start(true);g.noticeTime=0;g.weaponEquip=0;g.player.x=136.5;g.player.z=17;g.player.y=4.2;g.player.yaw=-1.95;g.player.pitch=-.2;shot(v,dir.resolve("05-mapa-"+i+".png"));}
        g.flow.mapIndex=0;g.start(true);g.selectAgent(9);g.player.x=42;g.player.z=76;g.player.yaw=Math.PI;g.ability(Ability.VERDICT);g.weaponEquip=0;g.ability(Ability.ANCHOR);g.ability(Ability.BULWARK);g.noticeTime=0;g.abilities.tick(.5);shot(v,dir.resolve("06-aureo-jogando.png"));
        g.abilities.clear();g.selectAgent(10);g.player.x=23;g.player.z=96;g.player.yaw=Math.PI;g.ability(Ability.TRIPWIRE);g.ability(Ability.CAGE);g.castSlot(1);g.noticeTime=0;g.weaponEquip=0;shot(v,dir.resolve("07-vigilancia.png"));
        g.abilities.clear();g.player.x=42;g.player.z=72;g.player.yaw=Math.PI/2;g.ability(Ability.SPYCAM);g.castSlot(3);g.noticeTime=0;shot(v,dir.resolve("08-camera.png"));g.sentinels.leave();
        g.abilities.clear();g.player.x=74;g.player.z=69;g.player.yaw=Math.PI;g.player.pitch=-.02;g.bhopTrainer=true;g.combat.equip(3);g.weaponEquip=0;g.player.moveSpeed=7.8;g.combat.hops=4;g.combat.bestSpeed=8.2;g.player.vz=-7.8;shot(v,dir.resolve("09-bhop.png"));
        g.bhopTrainer=false;g.selectAgent(1);g.openTactical(Ability.SMOKE);g.markTarget(74,53);shot(v,dir.resolve("10-mapa-tatico.png"));
        g.ui="shop";shot(v,dir.resolve("11-arsenal.png"));v.collectionUI.open();shot(v,dir.resolve("12-colecao.png"));
        BufferedImage atlas=new BufferedImage(1440,1810,BufferedImage.TYPE_INT_RGB);Graphics2D p=atlas.createGraphics();p.setColor(new Color(0x0E2432));p.fillRect(0,0,1440,1810);View.text(p,"RIFT  /  11 AGENTES · 44 HABILIDADES",38,53,28,View.WHITE,true);
        for(Agent a:Agent.values()){int y=85+a.ordinal()*154;View.text(p,a.name,40,y+38,21,new Color(a.color),true);View.text(p,a.role,40,y+65,11,View.MUTED,true);for(int i=0;i<4;i++){Ability type=a.slot(new int[]{0,3,1,2}[i]);int x=260+i*290;AbilityArt.icon(p,type,x+20,y+25,23,new Color(a.color));View.text(p,abilityName(type),x+56,y+32,13,View.WHITE,true);View.wrap(p,abilityDescription(type),x,y+72,267,11,View.MUTED);}View.line(p,38,y+142,1400,y+142,new Color(0x34505F),1);}p.dispose();javax.imageio.ImageIO.write(atlas,"png",dir.resolve("13-kits.png").toFile());
        g.close();
    }
}
