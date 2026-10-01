package rift;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import static rift.Game.*;
import static rift.World.*;
import static rift.Tests.check;

/** Regressions for observer knowledge, human aim limits, traversal and real wallbangs. */
final class BotTests {
    static Game arena() {
        Game g = new Game(new Settings(false), false, 1717);
        g.start(false); g.beginRound();
        g.world.solids.clear(); g.world.temporary.clear(); g.world.ramps.clear();
        g.world.penetrable.clear(); g.world.coverMaterials.clear();
        Arrays.fill(g.world.nav, true); Arrays.fill(g.world.navHeight, 0);
        for (Actor a : g.actors) { a.dead = true; a.carrier = false; }
        Actor bot = g.actors.get(1), enemy = g.actors.get(5);
        bot.dead = enemy.dead = false;
        bot.x = enemy.x = 50; bot.z = 60; enemy.z = 70;
        bot.y = enemy.y = 0; bot.yaw = 0; bot.pitch = 0;
        bot.primary = new Gun(Weapon.ECHO); bot.slot = 2; bot.skillCooldown = 9999;
        bot.armor = enemy.armor = 0;
        g.bots.reset(bot);
        return g;
    }
    static void run() throws Exception {
        perception(); aiming(); movement(); hearing(); penetration(); scheduling(); observer();
    }
    static void perception() {
        Game g = arena(); Actor a = g.actors.get(1), e = g.actors.get(5);
        check(g.bots.visible(a,e), "IA enxerga alvo na frente com linha de visão");
        e.z = 50; g.bots.perceive(a);
        check(a.target == null, "IA não enxerga inimigo atrás, mesmo sem parede");
        for (double degrees : new double[]{54.9,55.1,-54.9,-55.1}) {
            double angle = Math.toRadians(degrees); e.x = a.x + Math.sin(angle)*10; e.z = a.z + Math.cos(angle)*10;
            check(g.bots.inView(a,e) == (Math.abs(degrees)<55), "Limite de visão horizontal: "+degrees+" graus");
        }
        e.x=50; e.z=70;
        Box wall = new Box(48,0,64,52,3,65,0); g.world.solids.add(wall);
        check(!g.bots.visible(a,e), "Parede bloqueia aquisição, mesmo dentro do cone");
        g.world.solids.clear(); g.smokes.add(new Smoke(50,65));
        check(!g.bots.visible(a,e), "Smoke bloqueia aquisição e não apenas renderização");
        g.smokes.clear(); a.flash=1;
        check(!g.bots.visible(a,e), "Bot cegado não adquire alvo");
        a.flash=0; a.nearSight=2;
        check(!g.bots.visible(a,e), "Visão reduzida limita percepção a sete metros");
        a.nearSight=0; g.bots.perceive(a); double rememberedX=a.mind.x, rememberedZ=a.mind.z;
        e.x=80; e.z=50; g.time+=.2; g.bots.perceive(a);
        check(a.target==null && a.mind.memory==Bots.Memory.SEEN && a.mind.x==rememberedX && a.mind.z==rememberedZ,
            "Último local visto não acompanha inimigo fora da visão");
        g.time+=8; g.bots.perceive(a);
        check(a.mind.memory==Bots.Memory.NONE, "Memória visual expira e encerra perseguição antiga");
        e.x=50; e.z=70; g.bots.perceive(a); a.react=0; int ammo=a.gun().ammo;
        g.bots.look(a,a.mind.x,a.mind.y,a.mind.z,1);
        g.smokes.add(new Smoke(50,65)); g.bots.fire(a);
        check(a.gun().ammo==ammo && a.target==null, "Smoke nova invalida visão em cache antes do tiro");
        g.close();
    }
    static void aiming() {
        Game g=arena(); Actor a=g.actors.get(1),e=g.actors.get(5);
        for(int difficulty=0;difficulty<3;difficulty++) {
            g.settings.difficulty=difficulty; a.yaw=a.pitch=0;
            g.bots.look(a,60,a.eyeHeight,60,.1);
            check(Math.abs(a.yaw-Bots.AIM_SPEED[difficulty]*.1)<1e-8,
                "Dificuldade "+difficulty+": giro limitado por velocidade angular, sem snap");
        }
        g.settings.difficulty=1; a.yaw=a.pitch=0;
        for(int i=0;i<15;i++)g.bots.look(a,60,a.eyeHeight,60,1/60.);
        double angle60=a.yaw; a.yaw=a.pitch=0;
        for(int i=0;i<30;i++)g.bots.look(a,60,a.eyeHeight,60,1/120.);
        check(Math.abs(a.yaw-angle60)<1e-8, "Mira gira igualmente a 60 e 120 ticks/s");
        a.yaw=0; e.x=60; e.z=70; g.bots.perceive(a); a.react=0; int ammo=a.gun().ammo;
        g.bots.fire(a);
        check(a.gun().ammo==ammo,"Bot não dispara na direção do inimigo antes de alinhar a cabeça");
        g.bots.look(a,a.mind.x,a.mind.y,a.mind.z,1);g.bots.fire(a);
        check(a.gun().ammo==ammo-1,"Tiro é liberado quando mira e linha de visão estão alinhadas");
        check(a.gun().sprayStep==1&&a.gun().pitchRecoil>0,"Bots usam o mesmo padrão de recuo das armas");
        g.close();
    }
    static void movement() {
        Game g=arena();Actor a=g.actors.get(1),e=g.actors.get(5);
        g.bots.perceive(a);a.mind.actionTime=10;a.mind.holding=false;a.mind.strafe=1;
        double x=a.x;for(int i=0;i<30;i++)g.bots.duel(a,1/60.);
        check(Math.abs(a.x-x)>1,"Bot faz strafe em duelo a menos de 19 metros");
        boolean crouched=false,stood=false;
        for(int i=0;i<80;i++){g.time+=1;a.mind.actionTime=0;g.bots.duel(a,.016);crouched|=a.crouch;stood|=!a.crouch;}
        check(crouched&&stood,"Duelo alterna agachamento, parada para tiro e deslocamento");
        a.primary.ammo=0;a.pistol.ammo=12;a.slot=2;a.target=e;g.bots.manageWeapon(a,7);
        check(a.slot==1&&a.mind.equip==.38&&a.primary.reload==0,"Sem balas e inimigo perto: sacar pistola tem prioridade");
        int ammo=a.pistol.ammo;a.react=0;g.bots.fire(a);
        check(a.pistol.ammo==ammo,"Tempo de saque impede tiro instantâneo ao trocar de arma");
        a.slot=1;a.target=null;a.mind.expires=0;g.bots.manageWeapon(a,40);
        check(a.slot==2&&a.primary.reload>0,"Sem ameaça próxima: bot volta da pistola e recarrega a arma principal");
        g.bots.remember(a,50,1,75,Bots.Memory.SEEN,5);a.x=50;a.z=60;a.hp=20;
        g.world.solids.add(new Box(48,0,57,52,3,58,0));a.mind.nextCover=0;
        g.bots.retreat(a,.016);
        check(a.mind.hasCover&&!g.world.visible(new V(50,1,75),new V(a.mind.coverX,1.3,a.mind.coverZ)),
            "Retirada procura cobertura que realmente bloqueia a ameaça conhecida");
        a.mind.memory=Bots.Memory.NONE;a.target=null;a.x=50;a.z=60;a.yaw=a.pitch=0;a.repath=5;
        a.destX=50.5;a.destZ=60.5;a.path=List.of(new V(50.5,0,60.5));a.pathIndex=0;g.bots.travel(a,50.5,60.5,.016);
        check(Math.abs(a.pitch)<1e-8,"Bot mantém mira na altura dos olhos ao seguir waypoint próximo");
        g.close();
    }
    static void hearing() {
        Game g=arena();Actor a=g.actors.get(1),e=g.actors.get(5);e.z=53;
        g.bots.perceive(a);check(a.mind.memory==Bots.Memory.NONE,"Silêncio não entrega inimigo atrás do bot");
        g.bots.noise(e,Bots.Noise.STEP,19);g.bots.perceive(a);
        check(a.target==null&&a.mind.memory==Bots.Memory.HEARD&&Math.abs(a.mind.z-e.z)<1,
            "Passos geram pista aproximada mesmo com motor de áudio desligado");
        double last=a.mind.z;e.z=40;g.time+=.2;g.bots.perceive(a);
        check(a.mind.z==last,"Pista sonora não rastreia a posição atual do emissor");
        g.bots.reset(a);g.bots.noise(e,Bots.Noise.RELOAD,13);g.bots.perceive(a);
        check(a.mind.memory==Bots.Memory.NONE,"Recarga distante não revela inimigo fora do alcance acústico");
        e.z=50;g.world.solids.add(new Box(48,0,54,52,3,55,0));g.bots.noise(e,Bots.Noise.STEP,19);g.bots.perceive(a);
        check(a.mind.memory==Bots.Memory.NONE,"Parede atenua audição de passos");
        g.bots.noise(e,Bots.Noise.SHOT,64);g.bots.perceive(a);
        check(a.mind.memory==Bots.Memory.HEARD,"Tiro forte ainda pode ser ouvido através da parede");
        g.bots.reset(a);e.team=a.team;g.bots.noise(e,Bots.Noise.SHOT,64);g.bots.perceive(a);
        check(a.mind.memory==Bots.Memory.NONE,"Tiro de aliado não vira pista de inimigo");g.close();
    }
    static void penetration() {
        Game g=arena();World w=g.world;V origin=new V(50,1,60),dir=new V(0,0,1);
        Box panel=new Box(48,0,65,52,3,65.2,0);w.solids.add(panel);w.penetrable.add(panel);
        w.coverMaterials.put(panel,Ballistics.Material.WOOD);
        Ballistics.Shot wood=Ballistics.trace(w,origin,dir,Weapon.ECHO);
        check(wood.distance()>20&&wood.factorAt(10)<1&&wood.factorAt(3)==1,"Madeira fina permite wallbang com perda de dano só depois da superfície");
        w.coverMaterials.put(panel,Ballistics.Material.METAL);
        check(Ballistics.trace(w,origin,dir,Weapon.ECHO).distance()<6,"Metal da mesma espessura segura fuzil comum");
        Ballistics.Shot metal=Ballistics.trace(w,origin,dir,Weapon.HORIZON);
        check(metal.distance()>20&&metal.factorAt(10)<wood.factorAt(10),"Sniper atravessa metal fino com maior perda de dano");
        w.coverMaterials.put(panel,Ballistics.Material.WOOD);
        check(Ballistics.trace(w,origin,dir,Weapon.SPARK).distance()<6,"Pistola leve não atravessa painel de madeira de 20 cm");
        check(Ballistics.trace(w,origin,dir,Weapon.MARROW).distance()<6,"Pellets de escopeta não atravessam cobertura");
        w.coverMaterials.put(panel,Ballistics.Material.CONCRETE);
        check(Ballistics.trace(w,origin,dir,Weapon.BASTION).distance()<6,"Concreto estrutural bloqueia até arma pesada");
        w.coverMaterials.put(panel,Ballistics.Material.WOOD);
        Box second=new Box(48,0,66,52,3,66.5,0);w.solids.add(second);w.penetrable.add(second);
        check(Ballistics.trace(w,origin,dir,Weapon.ECHO).distance()<7,"Duas superfícies compartilham orçamento de penetração");
        w.solids.remove(second);w.penetrable.remove(second);
        V angled=new V(.97,0,.243).unit();
        Box wide=new Box(0,0,65,144,3,65.2,0);w.solids.clear();w.penetrable.clear();w.solids.add(wide);w.penetrable.add(wide);
        check(Ballistics.trace(w,origin,angled,Weapon.ECHO).passages().isEmpty(),"Impacto oblíquo percorre mais material e pode parar a bala");
        w.solids.clear();w.penetrable.clear();w.solids.add(panel);w.penetrable.add(panel);
        Actor shooter=g.actors.get(1),target=g.actors.get(5);shooter.yaw=0;target.hp=100;target.z=70;
        g.shoot(shooter,target.center().sub(shooter.eye()).unit(),Weapon.ECHO);
        check(target.hp<100&&target.hp>65,"Wallbang real aplica dano reduzido à vítima através da madeira");
        g.close();
    }
    static void scheduling() {
        Game g=arena();Set<Site> sites=new HashSet<>();Set<Bots.Role> roles=new HashSet<>();
        for(int i=0;i<20;i++){g.bots.beginRound();sites.add(g.bots.attackSite);roles.add(g.actors.get(1).mind.role);}
        check(sites.size()==2&&roles.size()>=2,"Mesmo número de rodada produz estratégias e papéis variados por sorteio");
        g.bots.beginFrame();long start=g.bots.paths;
        for(Actor a:g.actors){a.repath=0;g.bots.travel(a,20,20,.016);}
        check(g.bots.paths-start==Bots.PATHS_PER_FRAME,"Busca de rotas tem orçamento global por quadro");
        Actor a=g.actors.get(1);a.mind.nextThink=g.time+1;g.actors.get(5).dead=true;
        long before=g.bots.sightRays;for(int i=0;i<6;i++)g.bots.tick(a,.016);
        check(g.bots.sightRays==before,"Sem disparo nem nova percepção, IA reutiliza conhecimento entre quadros");g.close();
    }
    static void observer() throws Exception {
        Game g=new Game(new Settings(false),false,17);View v=new View(g);
        BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();
        v.render(p,1280,720);v.click(130,628);
        check(g.observing&&g.phase==Phase.LIVE&&g.actors.size()==10,"Menu inicia Bots vs. Bots com dez participantes ativos");
        Actor camera=g.cameraActor();g.tick(.016,Input.Frame.keys(java.awt.event.KeyEvent.VK_SPACE));
        check(g.cameraActor()!=camera,"Espaço alterna POV no observador");
        int matches=g.profile.matches;g.flow.complete(true);
        check(g.profile.matches==matches,"Observar partida não concede XP, rank ou vitória ao perfil");
        v.render(p,1280,720);g.start(true);check(!g.observing,"Sair para treino devolve controle ao jogador");
        p.dispose();g.close();
    }
    static void capture(Path dir) throws Exception {
        Files.createDirectories(dir);Game g=new Game(new Settings(false),false,1717);g.settings.quality=1;View v=new View(g);
        save(v,dir.resolve("01-menu-1.7.png"));g.startBots();
        for(int i=0;i<1200;i++)g.tick(1/60.,Input.Frame.empty());
        g.noticeTime=0;save(v,dir.resolve("02-bots-observador.png"));
        g.ui="menu";g.close();
    }
    static void save(View v,Path file) throws Exception {
        BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();
        v.render(p,1280,720);p.dispose();ImageIO.write(image,"png",file.toFile());
    }
}
