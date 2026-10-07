package rift;

import java.util.*;
import static rift.Game.*;
import static rift.World.*;
import static rift.Tests.*;
import static java.awt.event.KeyEvent.*;

/** Exercises real gameplay events with an in-memory mixer queue, without opening an audio device. */
final class AudioSurfaceTests {
    static void run(){materials();position();movement();impacts();reloads();reloadCadence();}
    static Game arena(){
        Game game=UpdateTests.arena();clearWorld(game.world);game.audio.running=true;game.phase=Phase.LIVE;
        game.player.x=40;game.player.y=0;game.player.z=40;game.player.yaw=0;game.player.pitch=0;game.weaponEquip=0;
        game.settings.sound=true;game.settings.volume=1;game.settings.walkToggle=false;game.settings.crouchToggle=false;
        return game;
    }
    static void clearWorld(World world){world.solids.clear();world.temporary.clear();world.platforms.clear();world.ramps.clear();world.triangles.clear();world.penetrable.clear();world.coverMaterials.clear();world.surfaceMaterials.clear();}
    static boolean named(AudioEngine.Sound sound,String name){return sound!=null&&(sound.name().equals(name)||sound.name().startsWith(name+"#"));}
    static Input.Frame input(int... keys){BitSet held=new BitSet();for(int key:keys)held.set(key);return new Input.Frame(held,new BitSet(),false,false,false,0,0,0,0);}
    static void materials(){
        Game game=arena();try{
            World world=game.world;Actor actor=game.player;
            check(AudioSurface.ground(world,actor)==AudioSurface.Material.CONCRETE,"Audio: piso comum usa concreto");
            world.crate(39,39,2,2,1.2);actor.y=1.2;
            check(AudioSurface.ground(world,actor)==AudioSurface.Material.WOOD,"Audio: caixa de madeira usa material visual sem alterar penetracao");
            check(world.material(world.solids.get(0))==Ballistics.Material.CONCRETE,"Audio: registro visual nao muda resistencia balistica da caixa");
            actor.y=0;check(AudioSurface.ground(world,actor)==AudioSurface.Material.CONCRETE,"Audio: caixa acima dos pes nao troca material do piso");
            clearWorld(world);world.container(39,39,41,41,2,0xCB8062);actor.y=2;
            check(AudioSurface.ground(world,actor)==AudioSurface.Material.METAL,"Audio: conteiner usa passos de metal");
            clearWorld(world);world.platform(39,39,41,41,2,0x849D9B);
            check(AudioSurface.ground(world,actor)==AudioSurface.Material.CONCRETE,"Audio: plataforma usa textura real em vez de altura");
            clearWorld(world);world.ramp(39,39,41,43,2,false,0x849D9B);actor.y=.5;
            check(AudioSurface.ground(world,actor)==AudioSurface.Material.METAL,"Audio: rampa no nivel dos pes usa chapa metalica");
            actor.y=0;check(AudioSurface.ground(world,actor)==AudioSurface.Material.CONCRETE,"Audio: rampa mais alta nao muda som dos pes abaixo dela");
            clearWorld(world);world.panel(44,39,.12,2,Ballistics.Material.METAL);
            SurfaceHit hit=SurfaceHit.cast(world,new V(40,1,40),new V(1,0,0),10,0);
            check(hit!=null&&AudioSurface.material(world,hit)==AudioSurface.Material.METAL,"Audio: painel metalico resolve o material da face atingida");
            world.panel(46,39,.12,2,Ballistics.Material.WOOD);
            hit=SurfaceHit.cast(world,new V(45,1,40),new V(1,0,0),10,0);
            check(hit!=null&&AudioSurface.material(world,hit)==AudioSurface.Material.WOOD,"Audio: painel repintado usa textura de madeira");
            for(int texture:new int[]{Assets.TEAL,Assets.RUST,Assets.STEEL,Assets.BRASS,Assets.PLATE,Assets.ROOF})check(AudioSurface.material(texture)==AudioSurface.Material.METAL,"Audio: textura metalica "+texture);
            for(int map:new int[]{2,5}){World metallic=new World(map);Actor onFloor=new Actor(99,0,"floor");onFloor.x=70;onFloor.z=120;check(AudioSurface.ground(metallic,onFloor)==AudioSurface.Material.METAL,"Audio: piso metalico do mapa "+map);}
        }finally{game.close();}
    }
    static void position(){
        Game game=arena();try{
            V right=game.player.eye().add(new V(4,0,0));AudioSurface.play(game,"step_metal",right,1,19);
            AudioEngine.Sound sound=game.audio.queue.poll();check(named(sound,"step_metal")&&sound.pan()>.99,"Audio: fonte a direita tem pan a direita");double clear=sound.gain();
            AudioSurface.play(game,"step_metal",game.player.eye().add(new V(-4,0,0)),1,19);sound=game.audio.queue.poll();check(sound!=null&&sound.pan()<-.99,"Audio: fonte a esquerda tem pan a esquerda");
            AudioSurface.play(game,"step_metal",game.player.eye().add(new V(8,0,0)),1,19);sound=game.audio.queue.poll();check(sound!=null&&sound.gain()<clear,"Audio: distancia reduz ganho");
            game.world.solid(42,0,39,42.2,3,41,0x728694);AudioSurface.play(game,"step_metal",right,1,19);sound=game.audio.queue.poll();check(sound!=null&&Math.abs(sound.gain()-clear*.35)<1e-9,"Audio: cobertura atenua som sem eliminar evento");
            AudioSurface.play(game,"step_metal",game.player.eye().add(new V(19,0,0)),1,19);check(game.audio.queue.isEmpty(),"Audio: limite de alcance nao gera voz");
            AudioSurface.play(game,"step_metal",right,Double.NaN,19);AudioSurface.play(game,"step_metal",right,1,Double.NaN);AudioSurface.play(game,"step_metal",new V(Double.NaN,0,0),1,19);check(game.audio.queue.isEmpty(),"Audio: entrada espacial invalida nao gera voz");
            game.rng.setSeed(19);game.bots.random.setSeed(29);AudioSurface.play(game,"step_metal",right,1,19);AudioSurface.play(game,"impact_metal",right,1,19);
            check(game.rng.nextLong()==new Random(19).nextLong()&&game.bots.random.nextLong()==new Random(29).nextLong(),"Audio: eventos nao consomem RNG de combate ou bots");
        }finally{game.close();}
    }
    static void movement(){
        Game game=arena();try{
            for(int i=0;i<60;i++)game.controlPlayer(1./120,input(VK_W));
            check(game.audio.queue.stream().anyMatch(s->named(s,"step_concrete")),"Audio: corrida real gera passos de superficie");check(game.bots.sequence>0,"Audio: corrida preserva evento de audicao dos bots");
            game.audio.queue.clear();game.player.vx=game.player.vz=0;game.footstep=0;long sequence=game.bots.sequence;
            for(int i=0;i<60;i++)game.controlPlayer(1./120,input(VK_W,VK_SHIFT));
            check(game.audio.queue.isEmpty()&&game.bots.sequence==sequence,"Audio: caminhada nao gera voz nem ruido de IA");
            game.player.vx=game.player.vz=0;game.footstep=0;for(int i=0;i<60;i++)game.controlPlayer(1./120,input(VK_W,VK_CONTROL));
            check(game.audio.queue.isEmpty()&&game.bots.sequence==sequence,"Audio: agachar nao gera voz nem ruido de IA");
            Actor bot=game.actors.get(5);bot.dead=false;bot.x=game.player.x+2;bot.y=0;bot.z=game.player.z;bot.moveSpeed=5;bot.grounded=true;AudioSurface.step(game,bot);
            check(named(game.audio.queue.poll(),"step_concrete"),"Audio: passos de bot proximo usam a mesma superficie");
            bot.dead=true;AudioSurface.step(game,bot);check(game.audio.queue.isEmpty(),"Audio: ator morto nao emite passos");bot.dead=false;bot.crouch=true;AudioSurface.step(game,bot);check(game.audio.queue.isEmpty(),"Audio: bot agachado nao emite passos");bot.crouch=false;
            AudioSurface.land(game,bot,4);double light=game.audio.queue.poll().gain();AudioSurface.land(game,bot,10);double heavy=game.audio.queue.poll().gain();check(heavy>light,"Audio: velocidade de queda aumenta ganho do pouso");
            AudioSurface.land(game,bot,2);check(game.audio.queue.isEmpty(),"Audio: contato leve com piso nao vira pouso");bot.y=.04;bot.vy=-8;bot.grounded=false;game.actorGravity(bot,.01);
            check(named(game.audio.queue.poll(),"land_concrete")&&bot.grounded,"Audio: gravidade real do bot emite pouso");
            game.world.crate(game.player.x-1,game.player.z-1,2,2,1.2);game.player.y=1.24;game.player.vy=-8;game.player.vx=game.player.vz=0;game.player.grounded=false;
            game.controlPlayer(.01,input());
            check(named(game.audio.queue.poll(),"land_wood")&&game.player.grounded&&Math.abs(game.player.y-1.2)<1e-9,"Audio: pouso real do jogador usa material da plataforma apos corrigir altura");
            clearWorld(game.world);game.player.y=0;game.audio.queue.clear();game.footstep=0;sequence=game.bots.sequence;game.settings.sound=false;
            for(int i=0;i<60;i++)game.controlPlayer(1./120,input(VK_W));
            check(game.audio.queue.isEmpty()&&game.bots.sequence>sequence&&game.audio.caption.contains("PASSOS"),"Audio: mutar preserva audicao dos bots e legenda de passos");
        }finally{game.close();}
    }
    static void impacts(){
        Game game=arena();try{
            game.world.crate(45,39,2,2,2.2);game.player.yaw=Math.PI/2;game.settings.impactFX=false;
            game.shoot(game.player,new V(1,0,0),Weapon.SPARK);check(named(game.audio.queue.poll(),"impact_wood")&&game.shotFX.marks.isEmpty(),"Audio: impacto de madeira permanece com efeitos visuais desligados");
            game.settings.impactFX=true;game.shoot(game.player,new V(1,0,0),Weapon.SPARK);check(named(game.audio.queue.poll(),"impact_wood")&&game.audio.queue.isEmpty()&&game.shotFX.marks.size()==1,"Audio: impacto gera uma voz e uma marca sem duplicacao");
            clearWorld(game.world);game.shotFX.clear();game.world.panel(45,39,.12,2);game.world.solid(48,0,39,49,3,41,0x849D9B);
            game.shoot(game.player,new V(1,0,0),Weapon.ECHO);check(named(game.audio.queue.poll(),"impact_concrete")&&game.shotFX.marks.size()==1,"Audio: impacto final apos penetracao encontra a parede correta");
            clearWorld(game.world);game.shoot(game.player,new V(1,0,0),Weapon.SPARK);check(game.audio.queue.isEmpty(),"Audio: disparo sem superficie nao produz impacto");
            Actor enemy=UpdateTests.enemy(game,44,40);game.shoot(game.player,new V(1,0,0),Weapon.SPARK);check(enemy.hp<100&&game.audio.queue.stream().noneMatch(s->s.name().startsWith("impact_")),"Audio: acertar ator nao duplica impacto de parede");
            enemy.dead=true;game.audio.queue.clear();game.world.panel(45,39,.12,2,Ballistics.Material.METAL);game.shoot(game.player,new V(1,0,0),Weapon.SPARK);
            check(named(game.audio.queue.poll(),"impact_metal"),"Audio: disparo real contra chapa usa impacto metalico");
        }finally{game.close();}
    }
    static void reloads(){
        Game game=arena();try{
            for(Weapon weapon:Weapon.values()){
                game.audio.queue.clear();game.player.primary=new Gun(weapon);game.player.slot=2;game.player.gun().ammo=0;game.reload(game.player);
                String group=AudioSurface.group(weapon);check(named(game.audio.queue.poll(),"reload_"+group),"Audio: inicio de recarga agrupado para "+weapon);
                AudioSurface.reloadTick(game,game.player,weapon.reload);check(named(game.audio.queue.poll(),"magout_"+group)&&named(game.audio.queue.poll(),"magin_"+group)&&named(game.audio.queue.poll(),"bolt_"+group),"Audio: tres etapas de recarga para "+weapon);
                AudioSurface.reloadTick(game,game.player,weapon.reload);check(game.audio.queue.isEmpty(),"Audio: etapas de recarga nao repetem para "+weapon);
            }
            game.player.gun().reload=0;game.player.primary=new Gun(Weapon.ECHO);game.player.slot=2;game.player.gun().ammo=0;game.reload(game.player);game.combat.equip(1);game.audio.queue.clear();AudioSurface.reloadTick(game,game.player,3);
            check(game.audio.queue.isEmpty(),"Audio: trocar arma cancela as etapas de recarga");
            Actor bot=game.actors.get(5);bot.dead=false;bot.x=game.player.x+2;bot.y=0;bot.z=game.player.z;bot.slot=1;bot.pistol.ammo=0;game.reload(bot);
            check(named(game.audio.queue.poll(),"reload_pistol"),"Audio: recarga de bot proximo e posicional");AudioSurface.reloadTick(game,bot,bot.pistol.reloadTotal);check(game.audio.queue.size()==3,"Audio: bot proximo emite etapas de recarga");game.audio.queue.clear();bot.dead=true;bot.pistol.reloadStage=0;AudioSurface.reloadTick(game,bot,3);check(game.audio.queue.isEmpty(),"Audio: morte interrompe etapas de recarga");
        }finally{game.close();}
    }
    static void reloadCadence(){
        Game game=arena();try{
            game.player.primary=new Gun(Weapon.ECHO);game.player.slot=2;Gun gun=game.player.gun();gun.ammo=0;game.reload(game.player);game.audio.queue.clear();
            List<String> events=new ArrayList<>();List<Double> times=new ArrayList<>();double elapsed=0,dt=.01;
            while(gun.reload>0&&elapsed<10){AudioSurface.reloadTick(game,game.player,dt);gun.tick(dt);elapsed+=dt;AudioEngine.Sound sound;while((sound=game.audio.queue.poll())!=null){events.add(sound.name().split("#")[0]);times.add(elapsed);}}
            check(events.equals(List.of("magout_rifle","magin_rifle","bolt_rifle")),"Audio: clock de recarga real emite tres etapas uma vez e em ordem");
            check(gun.reload<=0&&gun.ammo==gun.kind.mag&&gun.reloadStage==3,"Audio: ultimo frame de recarga termina municao e todas as etapas");
            double[] thresholds={.14,.55,.82};boolean timely=times.size()==3;
            for(int i=0;i<times.size();i++)timely&=times.get(i)>=gun.reloadTotal*thresholds[i]&&times.get(i)<gun.reloadTotal*thresholds[i]+dt*2;
            check(timely,"Audio: mecanismos acompanham fases da recarga sem avancar no clock visual");
            game.shotFX.tick(5);check(game.audio.queue.isEmpty(),"Audio: tick visual isolado nao produz mecanismos de recarga");
            game.phase=Phase.TRAIN;gun.ammo=0;game.reload(game.player);game.audio.queue.clear();double remaining=gun.reload;game.ui="pause";game.tick(2,Input.Frame.empty());
            check(game.audio.queue.isEmpty()&&gun.reload==remaining,"Audio: pausa nao avanca recarga nem reproduz mecanismos");
            game.ui="play";events.clear();elapsed=0;
            while(gun.reload>0&&elapsed<10){game.tick(dt,Input.Frame.empty());elapsed+=dt;AudioEngine.Sound sound;while((sound=game.audio.queue.poll())!=null)events.add(sound.name().split("#")[0]);}
            check(events.equals(List.of("magout_rifle","magin_rifle","bolt_rifle"))&&gun.ammo==gun.kind.mag,"Audio: Game.tick integra etapas ao clock real da arma");
        }finally{game.close();}
    }
}
