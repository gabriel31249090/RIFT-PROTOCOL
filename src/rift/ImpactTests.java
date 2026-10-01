package rift;

import java.awt.*;
import java.awt.image.*;
import java.nio.file.*;
import java.util.*;
import static rift.Game.*;
import static rift.World.*;
import static rift.Tests.*;
import static rift.UpdateTests.*;
import static java.awt.event.KeyEvent.*;

/** Regressions for the user-reported 1.6 interactions and packaged bitmap renderer. */
final class ImpactTests {
    static Input.Frame aim(boolean down){return new Input.Frame(new BitSet(),new BitSet(),false,down,false,0,0,0,0);}
    static void run()throws Exception{aiming();wire();maps();art();effects();}
    static void throwables(){
        Game g=arena();
        for(V incoming:java.util.List.of(new V(12,3,6),new V(12,-4,-7))){var p=new Abilities.Projectile(Ability.FLASH,g.player,new V(49.8,1.6,72),incoming,3);g.abilities.advance(p,.025);check(p.bounces==1&&p.velocity.x()<0&&Math.signum(p.velocity.z())==Math.signum(incoming.z()),"Impacto oblíquo reflete normal e preserva sentido tangencial");check(p.velocity.length()<incoming.length(),"Ricochete perde energia, sem amplificar velocidade");double vy=p.velocity.y();g.abilities.advance(p,.02);check(p.velocity.y()<vy,"Gravidade continua agindo após o ricochete");}
        var floor=new Abilities.Projectile(Ability.ACID,g.player,new V(42,.15,72),new V(5,-10,2),3);g.abilities.advance(floor,.02);check(floor.bounces==1&&floor.velocity.y()>0&&floor.velocity.x()>0&&floor.velocity.z()>0,"Piso reflete só a componente vertical e aplica atrito");
        var curve=new Abilities.Projectile(Ability.CURVEFLASH,g.player,new V(49.8,1.6,72),new V(12,0,2),2);curve.curve=3.6;g.abilities.advance(curve,.02);check(curve.bounces==1&&curve.curve==0&&curve.fuse>0,"Clarão curvo passa a trajetória balística após colisão");
        var rocket=new Abilities.Projectile(Ability.ROCKET,g.player,new V(49.8,1.6,72),new V(36,0,0),2);g.abilities.advance(rocket,.02);check(rocket.fuse<=0&&rocket.bounces==0,"Foguete detona no impacto sem ricochetear");
        var quick=new Abilities.Projectile(Ability.QUICK_SMOKE,g.player,new V(49.8,1.6,72),new V(29,0,0),.42);g.abilities.advance(quick,.02);check(quick.fuse<=0,"Véu rápido abre ao tocar a parede");
        var fast=new Abilities.Projectile(Ability.FLASH,g.player,new V(48,1.6,72),new V(700,0,0),2);g.abilities.advance(fast,.01);check(fast.bounces>0&&fast.position.x()<50,"Varredura impede atravessar parede em alta velocidade");
        V[] end=new V[3];int[] rates={60,120,144};for(int k=0;k<3;k++){g.abilities.clear();var p=new Abilities.Projectile(Ability.FLASH,g.player,new V(48,1.6,72),new V(8,2,3),2);g.abilities.projectiles.add(p);for(int i=0;i<rates[k]/2;i++)g.abilities.tick(1./rates[k]);end[k]=p.position;}check(end[0].sub(end[1]).length()<.04&&end[0].sub(end[2]).length()<.04,"Ricochetes consistentes a 60 / 120 / 144 FPS");g.close();
    }
    static void aiming()throws Exception{
        Game g=arena();g.player.primary=new Gun(Weapon.HORIZON);g.player.slot=2;g.weaponEquip=0;
        g.controlPlayer(.02,aim(true));g.controlPlayer(.02,aim(false));check(g.aiming&&g.aimLatched,"Sniper mantém a mira após soltar o botão direito");g.controlPlayer(.02,aim(true));check(!g.aiming,"Segundo clique desativa luneta");g.controlPlayer(.02,aim(false));g.controlPlayer(.02,aim(true));g.controlPlayer(.02,aim(true));check(g.aiming,"Segurar direito não alterna repetidamente");
        g.combat.equip(1);g.weaponEquip=0;g.controlPlayer(.02,aim(false));check(!g.aiming&&!g.aimLatched,"Troca de arma cancela a luneta travada");g.controlPlayer(.02,aim(true));g.controlPlayer(.02,aim(false));check(!g.aiming,"Pistola continua usando segurar com configuração padrão");
        g.player.slot=2;g.settings.sniperToggle=false;g.controlPlayer(.02,aim(true));g.controlPlayer(.02,aim(false));check(!g.aiming,"Sniper respeita opção de segurar quando escolhida");
        Path dir=Files.createTempDirectory("rift16-");try{Settings s=new Settings(dir.resolve("prefs"));s.sniperToggle=false;s.textures=false;s.save();Settings restored=new Settings(dir.resolve("prefs"));check(!restored.sniperToggle&&!restored.textures,"Preferências novas persistem separadamente");}finally{try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}g.close();
    }
    static void wire(){
        Game g=arena();g.selectAgent(10);g.training=false;g.phase=Phase.LIVE;g.player.yaw=Math.PI/2;g.player.pitch=Math.atan2(-.93,8);int charges=g.player.qCharges;
        g.castSlot(0);check(g.sentinels.placingWire&&g.sentinels.preview!=null&&g.player.qCharges==charges,"Q mostra fio válido sem gastar carga");g.sentinels.cancelWire();check(g.player.qCharges==charges,"Cancelar prévia preserva carga");g.castSlot(0);g.sentinels.confirmWire();var d=g.sentinels.own(g.player,Ability.TRIPWIRE);check(d!=null&&g.player.qCharges==charges-1,"Confirmar instala o fio na superfície e cobra uma carga");
        Actor e=enemy(g,46,75);e.motionStart=new V(46,0,69);d.age=.5;g.sentinels.tick(.016);check(d.tethered==e&&e.revealed>0,"Cruzamento rápido dispara mesmo terminando além do fio");double hp=e.hp;d.hp=0;g.abilities.tick(1);check(e.hp==hp,"Destruir a âncora durante o aviso impede o dano final");
        e.motionStart=new V(46,1.1,69);e.y=1.1;check(!Sentinels.crossed(e,d.position,d.endpoint),"Salto sobre fio não dispara a varredura");e.y=0;e.motionStart=new V(46,0,69);e.crouch=true;check(!Sentinels.crossed(e,new V(40,1.5,72),new V(50,1.5,72)),"Agachar permite passar sob fio alto");
        g.player.pitch=1.3;g.castSlot(0);g.sentinels.confirmWire();check(g.player.qCharges==charges-1,"Posição inválida não consome carga");
        g.combat.equip(1);check(!g.sentinels.placingWire,"Trocar para arma cancela prévia do fio");g.castSlot(0);g.player.dead=true;g.sentinels.tick(.02);check(!g.sentinels.placingWire,"Morrer cancela instalação pendente do fio");g.close();
    }
    static void maps(){
        Game g=arena();View v=new View(g);Set<Integer> visual=new HashSet<>();for(int agent:new int[]{1,3,4}){g.selectAgent(agent);g.training=false;g.phase=Phase.LIVE;int slot=agent==1?0:3;int charge=g.charges(slot);g.castSlot(slot);check(g.ui.equals("tactical")&&g.charges(slot)==charge,"Abrir painel não cobra habilidade do agente "+agent);
            MapProjection m=v.tacticalMap();V at=m.world(m.x(42,68),m.y(42,68));check(Math.hypot(at.x()-42,at.z()-68)<1e-8,"Clique e mapa coincidem no painel "+agent);check(!g.markTarget(130,112),"Painel rejeita posição fora do alcance "+agent);check(g.markTarget(42,68),"Painel aceita piso dentro do alcance "+agent);
            BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();v.render(p,1280,720);p.dispose();visual.add(Arrays.hashCode(image.getRGB(360,64,560,550,null,0,560)));
            check(g.deployTactical()&&g.charges(slot)==charge-1,"Confirmar aplica efeito e cobra exatamente uma carga "+agent);g.abilities.clear();g.smokes.clear();g.training=true;g.phase=Phase.TRAIN;}
        check(visual.size()==3,"Três instrumentos de posicionamento visualmente distintos");g.selectAgent(0);g.castSlot(1);check(g.ui.equals("play")&&!g.abilities.projectiles.isEmpty(),"Vértice mantém smoke instantânea sem abrir mapa");g.close();
    }
    static void art(){
        check(Assets.HERO.getWidth()>1200&&Assets.portraits.length==12&&Assets.icons.length==48,"Artes reais carregadas dos recursos do pacote");for(int i=0;i<16;i++){int[] t=Assets.tiles[i][0];check(Arrays.stream(t).distinct().limit(20).count()>15,"Material bitmap com detalhe: "+i);}
        Game g=arena();Renderer r=new Renderer(640,360);g.player.yaw=Math.PI/2;g.player.pitch=-.1;g.settings.textures=true;int textured=Arrays.hashCode(r.render(g).getRGB(0,0,640,360,null,0,640));g.settings.textures=false;int flat=Arrays.hashCode(r.render(g).getRGB(0,0,640,360,null,0,640));check(textured!=flat,"Texturas alteram a cena renderizada, não apenas o menu");
        g.settings.textures=true;g.player.x=49.77;r.render(g);check(r.triangleCount>0,"Texturas atravessando o near plane renderizam sem falha");g.close();
    }
    static void effects(){
        Set<Integer> hashes=new HashSet<>();for(Weapon w:Weapon.values()){float[] a=ShotAudio.render(w,1);double energy=0;for(float n:a){checkSample(n);energy+=n*n;}check(energy>1,"Disparo em camadas com energia audível: "+w);hashes.add(Arrays.hashCode(a));}check(hashes.size()==15,"As quinze armas têm sons diferentes");
        Game g=arena();g.weaponEquip=0;g.firePlayer();check(!g.shotFX.casings.isEmpty(),"Disparo ejeta cápsula com simulação própria");g.shotFX.hit(g.player.eye(),new V(1,0,0),8);check(!g.shotFX.marks.isEmpty(),"Tiro deixa marca na superfície realmente atingida");for(int i=0;i<180;i++)g.shotFX.fired(g.player);check(g.shotFX.casings.size()<=32,"Cápsulas têm limite para controlar custo visual");g.close();
    }
    static void checkSample(float n){if(!Float.isFinite(n)||Math.abs(n)>1)throw new AssertionError("Amostra de áudio inválida");}
    static void capture(Path dir)throws Exception{
        Files.createDirectories(dir);Game g=arena();g.settings.quality=2;g.weaponEquip=0;g.noticeTime=0;View v=new View(g);g.ui="menu";shot(v,dir.resolve("01-menu.png"));g.ui="agents";g.pendingTraining=true;g.selectAgent(10);shot(v,dir.resolve("02-agentes.png"));
        for(int map=0;map<3;map++){g.flow.mapIndex=map;g.start(true);g.player.x=136.5;g.player.z=17;g.player.y=4.2;g.player.yaw=-1.95;g.player.pitch=-.2;g.weaponEquip=0;g.noticeTime=0;shot(v,dir.resolve("03-mapa-"+map+".png"));}
        g.flow.mapIndex=0;g.start(true);g.player.x=42;g.player.z=72;g.player.y=0;g.player.yaw=Math.PI/2;g.player.pitch=-.07;g.weaponEquip=0;g.noticeTime=0;g.player.primary=new Gun(Weapon.ECHO);g.player.slot=2;shot(v,dir.resolve("04-arma-texturizada.png"));g.firePlayer();g.player.gun().shotAge=.025;shot(v,dir.resolve("05-disparo.png"));
        for(int agent:new int[]{1,3,4}){g.selectAgent(agent);g.player.yaw=Math.PI;g.castSlot(agent==1?0:3);g.markTarget(42,68);g.noticeTime=0;shot(v,dir.resolve("06-painel-"+agent+".png"));g.cancelTactical();}
        g.shotFX.clear();g.particles.clear();g.traces.clear();g.player.shotGlow=0;g.audio.caption="";
        g.selectAgent(10);g.player.yaw=Math.PI/2;g.player.pitch=Math.atan2(-.93,8);g.castSlot(0);g.noticeTime=0;shot(v,dir.resolve("07-previa-fio.png"));g.sentinels.confirmWire();var wire=g.sentinels.own(g.player,Ability.TRIPWIRE);if(wire!=null){wire.age=.6;Actor e=enemy(g,46,72);e.motionStart=new V(46,0,71);g.sentinels.tick(.02);g.player.yaw=Math.PI/2;g.player.pitch=0;g.noticeTime=0;shot(v,dir.resolve("08-fio-acionado.png"));}
        g.sentinels.cancelWire();g.ui="settings";v.settingsUI.tab=4;shot(v,dir.resolve("09-configuracoes.png"));g.ui="shop";shot(v,dir.resolve("10-arsenal.png"));g.close();
    }
}
