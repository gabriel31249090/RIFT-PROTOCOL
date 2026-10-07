package rift;

import java.util.*;
import static rift.Game.*;
import static rift.Tests.check;

/** Snapshot-driven audio checks use only an in-memory queue, without a device or mixer thread. */
final class DuelAudioTests {
    static final class Fixture implements AutoCloseable {
        final Game game;
        final DuelProtocol.State state=new DuelProtocol.State();
        final DuelAudio audio;
        Fixture(){
            World world=new World(3);world.solids.clear();world.temporary.clear();world.ramps.clear();world.platforms.clear();world.triangles.clear();
            game=DuelTests.replica(world,0);game.settings.sound=true;game.settings.volume=1;game.audio.running=true;
            for(int i=0;i<2;i++){Actor actor=game.actors.get(i);actor.x=60+i;actor.y=0;actor.z=60;actor.slot=2;}
            state.epoch=1;state.phase=DuelSimulation.LIVE;audio=new DuelAudio(game);
        }
        void snapshot(int ticks){state.tick+=ticks;audio.observe(state);}
        List<AudioEngine.Sound> sounds(){List<AudioEngine.Sound> sounds=new ArrayList<>();game.audio.queue.drainTo(sounds);return sounds;}
        @Override public void close(){game.close();}
    }
    static boolean named(AudioEngine.Sound sound,String name){return sound!=null&&(sound.name().equals(name)||sound.name().startsWith(name+"#"));}
    static void run()throws Exception{movement();landing();reloads();boundaries();replication();}
    static void movement(){
        try(Fixture f=new Fixture()){
            Actor actor=f.game.player;actor.moveSpeed=5;actor.walk=1;actor.primary.reload=1;actor.primary.reloadTotal=2;
            f.snapshot(1);check(f.sounds().isEmpty(),"Duelo audio: primeiro snapshot nao reproduz eventos antigos");actor.primary.reload=0;
            for(int i=0;i<20;i++){actor.z+=.25;actor.walk+=.25;f.snapshot(3);}
            List<AudioEngine.Sound> sounds=f.sounds();check(sounds.size()==2&&sounds.stream().allMatch(s->named(s,"step_concrete")),"Duelo audio: corrida tem cadencia de passos por superficie");
            f.audio.observe(f.state);check(f.sounds().isEmpty(),"Duelo audio: snapshot repetido nao gera passos");
            actor.crouch=true;for(int i=0;i<10;i++){actor.z+=.25;actor.walk+=.25;f.snapshot(3);}check(f.sounds().isEmpty(),"Duelo audio: agachado nao emite passos mesmo com velocidade elevada");
            actor.crouch=false;actor.moveSpeed=2.45;for(int i=0;i<10;i++){actor.z+=.12;actor.walk+=.12;f.snapshot(3);}check(f.sounds().isEmpty(),"Duelo audio: caminhada silenciosa permanece silenciosa");
            actor.moveSpeed=5;f.snapshot(30);check(f.sounds().isEmpty(),"Duelo audio: velocidade sem deslocamento nao inventa passo");
            actor.grounded=false;actor.z+=.25;actor.walk+=.25;f.snapshot(3);check(f.sounds().isEmpty(),"Duelo audio: movimento no ar nao gera passos");
        }
        try(Fixture f=new Fixture()){
            Actor remote=f.game.actors.get(1);f.game.world.crate(60.5,59.5,1,1,1.2);remote.y=1.2;remote.moveSpeed=5;f.snapshot(1);
            remote.x+=.1;remote.walk+=.1;f.snapshot(24);AudioEngine.Sound sound=f.game.audio.queue.poll();
            check(named(sound,"step_wood")&&sound.pan()>.4,"Duelo audio: passos remotos usam material e pan da posicao replicada");
        }
    }
    static void landing(){
        try(Fixture f=new Fixture()){
            Actor actor=f.game.player;actor.y=1;actor.vy=-6;actor.grounded=false;f.snapshot(1);
            actor.y=0;actor.vy=0;actor.grounded=true;f.snapshot(3);check(named(f.game.audio.queue.poll(),"land_concrete"),"Duelo audio: contato de queda replicado emite pouso");
            f.snapshot(3);check(f.sounds().isEmpty(),"Duelo audio: ficar no chao nao repete pouso");
            actor.y=.02;actor.vy=-1;actor.grounded=false;f.snapshot(1);actor.y=0;actor.vy=0;actor.grounded=true;f.snapshot(1);check(f.sounds().isEmpty(),"Duelo audio: contato leve nao vira pouso");
            actor.y=.02;actor.vy=-1;actor.grounded=false;f.snapshot(1);actor.y=0;actor.vy=0;actor.grounded=true;f.snapshot(24);check(f.sounds().isEmpty(),"Duelo audio: atraso de snapshot nao amplifica contato leve");
            actor.y=.5;actor.vy=-5;actor.grounded=false;f.snapshot(1);actor.y=0;actor.vy=0;actor.grounded=true;actor.dead=true;f.snapshot(1);check(f.sounds().isEmpty(),"Duelo audio: morte nao dispara pouso");
        }
    }
    static void reloads(){
        try(Fixture f=new Fixture()){
            Actor actor=f.game.player;Gun gun=actor.primary;gun.ammo=0;gun.reloadTotal=1;f.snapshot(1);
            gun.reload=1;f.snapshot(1);check(named(f.game.audio.queue.poll(),"reload_rifle"),"Duelo audio: inicio de recarga e reproduzido uma vez");
            for(double remaining:new double[]{.85,.44,.15}){gun.reload=remaining;f.snapshot(1);}
            List<AudioEngine.Sound> sounds=f.sounds();check(sounds.size()==3&&named(sounds.get(0),"magout_rifle")&&named(sounds.get(1),"magin_rifle")&&named(sounds.get(2),"bolt_rifle"),"Duelo audio: mecanismos acompanham as tres etapas replicadas");
            f.snapshot(2);f.audio.observe(f.state);check(f.sounds().isEmpty(),"Duelo audio: recarga congelada e snapshots repetidos nao repetem mecanismos");
            gun.reload=0;gun.ammo=gun.kind.mag;f.snapshot(1);check(f.sounds().isEmpty(),"Duelo audio: conclusao apos etapa final nao duplica ferrolho");
            gun.ammo=0;gun.reload=1;f.snapshot(1);f.sounds();gun.reload=.4;f.snapshot(1);f.sounds();gun.reload=0;gun.ammo=gun.kind.mag;f.snapshot(1);
            check(named(f.game.audio.queue.poll(),"bolt_rifle"),"Duelo audio: conclusao entre snapshots preserva ultima etapa");
            gun.ammo=0;gun.reload=1;f.snapshot(1);f.sounds();gun.reload=0;f.snapshot(1);check(f.sounds().isEmpty(),"Duelo audio: cancelar recarga nao gera mecanismos restantes");
            gun.reload=1;f.snapshot(1);f.sounds();actor.slot=1;actor.pistol.reloadTotal=1;actor.pistol.reload=.8;f.snapshot(1);check(f.sounds().isEmpty(),"Duelo audio: trocar arma nao inventa inicio ou etapas antigas");
            actor.pistol.reload=.1;f.snapshot(1);sounds=f.sounds();check(sounds.size()==2&&named(sounds.get(0),"magin_pistol")&&named(sounds.get(1),"bolt_pistol"),"Duelo audio: etapas futuras da nova arma seguem grupo correto");
            actor.pistol.reload=0;f.snapshot(1);actor.pistol.reload=1;f.snapshot(1);f.sounds();actor.dead=true;actor.pistol.reload=.1;f.snapshot(1);check(f.sounds().isEmpty(),"Duelo audio: morte interrompe mecanismos de recarga");
        }
        try(Fixture f=new Fixture()){
            Actor remote=f.game.actors.get(1);remote.slot=1;remote.pistol.ammo=0;remote.pistol.reloadTotal=1;f.snapshot(1);remote.pistol.reload=1;f.snapshot(1);
            AudioEngine.Sound sound=f.game.audio.queue.poll();check(named(sound,"reload_pistol")&&sound.pan()>.99&&sound.gain()>0,"Duelo audio: recarga remota tem pan e atenuacao espacial");
        }
    }
    static void boundaries(){
        try(Fixture f=new Fixture()){
            Actor actor=f.game.player;f.snapshot(1);actor.moveSpeed=5;actor.z+=10;actor.walk+=10;actor.primary.reload=.1;f.snapshot(1);
            check(f.sounds().isEmpty(),"Duelo audio: deslocamento brusco nao reproduz eventos intermediarios");
            actor.z+=.5;actor.walk+=.5;actor.primary.reload=2;f.state.epoch++;f.snapshot(24);check(f.sounds().isEmpty(),"Duelo audio: nova rodada estabelece base silenciosa");
            actor.z+=.5;actor.walk+=.5;actor.primary.reload=.01;f.snapshot(60);check(f.sounds().isEmpty(),"Duelo audio: longa interrupcao descarta eventos atrasados");
            f.state.phase=DuelSimulation.PREP;actor.primary.reload=2;actor.z+=.5;actor.walk+=.5;f.snapshot(24);check(f.sounds().isEmpty(),"Duelo audio: preparacao nao produz eventos de combate");
            f.state.phase=DuelSimulation.LIVE;actor.primary.reload=0;actor.dead=true;f.snapshot(1);actor.dead=false;actor.z+=.5;actor.walk+=.5;actor.primary.reload=.1;f.snapshot(24);check(f.sounds().isEmpty(),"Duelo audio: primeira observacao apos respawn e silenciosa");
            f.game.rng.setSeed(22);f.game.bots.random.setSeed(33);actor.primary.reload=.01;actor.primary.reloadStage=7;f.snapshot(1);f.sounds();
            check(actor.primary.reloadStage==7&&f.game.rng.nextLong()==new Random(22).nextLong()&&f.game.bots.random.nextLong()==new Random(33).nextLong(),"Duelo audio: observador nao altera arma ou RNG de combate");
            check(f.game.audio.thread==null&&f.game.audio.line==null,"Duelo audio: testes nao abrem dispositivo ou thread de audio");
        }
        try(Fixture f=new Fixture()){
            f.snapshot(100);f.state.tick=99;f.game.player.primary.reload=1;f.game.player.z+=.5;f.game.player.walk+=.5;f.game.player.moveSpeed=5;f.audio.observe(f.state);
            check(f.sounds().isEmpty(),"Duelo audio: snapshot anterior ao tick observado nao reproduz eventos");
            f.state.tick=101;f.audio.observe(f.state);check(named(f.game.audio.queue.poll(),"reload_rifle"),"Duelo audio: pacote antigo nao substitui a base do proximo evento");
        }
    }
    static void replication()throws Exception{
        try(DuelSimulation server=new DuelSimulation(0)){
            server.phase=DuelSimulation.LIVE;server.connected[0]=server.connected[1]=true;server.timer=60;
            Game replica=DuelTests.replica(server.world,0);replica.audio.running=true;DuelProtocol.State state=new DuelProtocol.State();DuelAudio audio=new DuelAudio(replica);
            try{
                DuelProtocol.snapshot(DuelProtocol.snapshot(server,0,0),replica,state,0);audio.observe(state);
                DuelProtocol.Command running=DuelTests.command(1,1,0,0,(float)Math.PI,DuelProtocol.Choice.defaults(),false);
                for(int i=0;i<60;i++){server.step(new DuelProtocol.Command[]{running,null});if(i%3==2){DuelProtocol.snapshot(DuelProtocol.snapshot(server,0,0),replica,state,0);audio.observe(state);}}
                check(replica.audio.queue.stream().anyMatch(s->named(s,"step_concrete")),"Duelo audio: snapshots binarios de movimento real chegam ao banco de passos");
                check(server.seats[0].audio.queue.isEmpty()&&server.seats[1].audio.queue.isEmpty(),"Duelo audio: servidor nao reproduz vozes de cliente");
                replica.audio.queue.clear();server.players[0].primary.ammo=0;server.seats[0].reload(server.players[0]);server.tick++;
                DuelProtocol.snapshot(DuelProtocol.snapshot(server,0,0),replica,state,0);audio.observe(state);
                check(named(replica.audio.queue.poll(),"reload_rifle"),"Duelo audio: recarga autoritativa chega ao cliente pelo protocolo existente");
            }finally{replica.close();}
        }
    }
}
