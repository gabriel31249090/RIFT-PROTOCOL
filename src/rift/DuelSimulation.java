package rift;

import java.util.*;
import static rift.Game.*;
import static rift.World.*;

/** Server-owned duel. Each seat has its own controller; only the arena and two actors are shared. */
final class DuelSimulation implements AutoCloseable {
    static final int WAITING=0,PREP=1,LIVE=2,RESULT=3,FINISHED=4;
    static final int TARGET=7;
    final World world;
    final Game[] seats=new Game[2];
    final Actor[] players=new Actor[2];
    final DuelProtocol.Choice[] choices={DuelProtocol.Choice.defaults(),DuelProtocol.Choice.defaults()};
    final String[] names={"Jogador 1","Jogador 2"};
    final boolean[] connected=new boolean[2],ready=new boolean[2];
    final int[] scores=new int[2];
    int phase=WAITING,round=1,winner=-1,epoch=1;
    long tick;
    double timer;
    String message="Aguardando dois jogadores";
    DuelSimulation(int map){
        world=new World(3+Math.floorMod(map,3));world.setShadows(false);
        for(int i=0;i<2;i++){players[i]=new Actor(i,i,names[i]);Settings s=new Settings(false);s.shadows=false;seats[i]=new Game(s,false,1800+i,world);seats[i].duel=true;seats[i].player=players[i];seats[i].ui="play";}
        for(Game g:seats)g.actors.addAll(Arrays.asList(players));
        resetActors();
    }
    void connect(int seat,String name,int options){
        connected[seat]=true;names[seat]=name;ready[seat]=false;
        Settings s=seats[seat].settings;s.sniperToggle=(options&1)!=0;s.aimToggle=(options&2)!=0;s.crouchToggle=(options&4)!=0;s.walkToggle=(options&8)!=0;
        if(connected[0]&&connected[1]&&phase==WAITING)message="Escolham o equipamento e apertem ENTER";
    }
    void disconnect(int seat){
        connected[seat]=false;ready[seat]=false;
        if(phase!=WAITING){winner=1-seat;phase=FINISHED;message=names[seat]+" saiu da sala";}
    }
    void resetActors(){
        for(int i=0;i<2;i++){
            Actor a=players[i];V spawn=DuelMaps.spawn((i+round-1)%2);
            a.x=spawn.x();a.z=spawn.z();a.y=world.surfaceAt(a.x,a.z);a.yaw=a.z>64?Math.PI:0;a.pitch=0;
            a.hp=100;a.armor=50;a.dead=false;a.deathAge=0;a.grounded=true;a.crouch=false;a.vx=a.vz=a.vy=a.walk=a.moveSpeed=0;
            a.eyeHeight=1.63;a.tagTime=a.landRecovery=a.damageGlow=a.shotGlow=0;a.carrier=false;a.roundVictims.clear();
            Game g=seats[i];g.combat.reset();g.aimLatched=g.lastAim=g.aiming=g.walkLatched=g.crouchLatched=false;
            g.aimLerp=g.landing=g.viewKick=g.kickVelocity=g.weaponEquip=g.hitMarker=g.hitHead=g.killToast=0;g.traces.clear();g.particles.clear();g.shotFX.clear();
            equip(i);a.bodyYaw=a.yaw;a.animCrouch=a.animSpeed=a.lean=a.animTurn=0;
        }
        epoch++;
    }
    void equip(int seat){
        Actor a=players[seat];DuelProtocol.Choice c=choices[seat];
        a.primary=new Gun(Weapon.values()[c.primary()]);a.pistol=new Gun(Weapon.values()[c.pistol()]);a.slot=2;a.agentIndex=c.agent();
        Game g=seats[seat];g.agent=Agent.values()[a.agentIndex];g.profile.melee=c.blade();g.profile.meleeSkin=c.skin();
        Arrays.fill(g.profile.skins,c.skin());Arrays.fill(g.profile.charms,c.charm());
        g.aimLatched=g.aiming=false;g.aimLerp=0;
    }
    void step(DuelProtocol.Command[] input){
        double dt=1/60.;tick++;
        for(int i=0;i<2;i++){
            DuelProtocol.Command c=input[i];
            if(c!=null&&connected[i]&&(phase==WAITING||phase==PREP||phase==FINISHED)){
                if(!choices[i].equals(c.choice())){choices[i]=c.choice();equip(i);}
                if(c.ready())ready[i]=true;
            }
        }
        if((phase==WAITING||phase==FINISHED)&&connected[0]&&connected[1]&&ready[0]&&ready[1]){
            scores[0]=scores[1]=0;round=1;for(Actor a:players){a.kills=a.deaths=0;}
            for(Game g:seats){g.trainingHits=g.trainingShots=0;}beginPrep();
        }
        for(Actor a:players){a.pistol.tick(dt);a.primary.tick(dt);a.tagTime=Math.max(0,a.tagTime-dt);a.landRecovery=Math.max(0,a.landRecovery-dt);a.shotGlow=Math.max(0,a.shotGlow-dt);a.damageGlow=Math.max(0,a.damageGlow-dt);if(a.dead)a.deathAge+=dt;}
        for(int turn=0;turn<2;turn++){
            int i=(int)((tick+turn)&1);
            Game g=seats[i];g.time+=dt;g.visualTime+=dt;
            g.phase=phase==LIVE?Phase.LIVE:phase==FINISHED?Phase.MATCH:Phase.BUY;
            g.weaponEquip=Math.max(0,g.weaponEquip-dt);g.landing=Math.max(0,g.landing-dt*4);g.hitMarker=Math.max(0,g.hitMarker-dt);g.hitHead=Math.max(0,g.hitHead-dt);g.killToast=Math.max(0,g.killToast-dt);
            g.kickVelocity+=(-95*g.viewKick-18*g.kickVelocity)*dt;g.viewKick+=g.kickVelocity*dt;
            g.traces.removeIf(t->(t.life-=dt)<=0);g.particles.removeIf(p->p.tick(dt));g.shotFX.tick(dt);
            if(phase==LIVE&&!players[i].dead){
                DuelProtocol.Command c=input[i];
                if(c!=null){players[i].yaw=c.yaw();players[i].pitch=c.pitch();}
                g.combat.tick(dt);g.controlPlayer(dt,c==null?Input.Frame.empty():c.frame());
            }
            if(players[i].dead){g.aiming=g.aimLatched=false;g.aimLerp=0;}
            CharacterModel.animate(players[i],dt);
        }
        if(phase==PREP){timer-=dt;if(timer<=0){phase=LIVE;timer=60;message="DUELO";}}
        else if(phase==LIVE){
            timer-=dt;
            if(players[0].dead||players[1].dead)finish(players[0].dead?(players[1].dead?-1:1):0,"Eliminação");
            else if(timer<=0){double h0=players[0].hp+players[0].armor,h1=players[1].hp+players[1].armor;finish(Math.abs(h0-h1)<.01?-1:h0>h1?0:1,"Tempo esgotado • vida + escudo");}
        }else if(phase==RESULT){timer-=dt;if(timer<=0){if(Math.max(scores[0],scores[1])>=TARGET){phase=FINISHED;message=names[winner]+" venceu a partida";ready[0]=ready[1]=false;}else{round++;beginPrep();}}}
    }
    void beginPrep(){phase=PREP;timer=6;winner=-1;ready[0]=ready[1]=false;resetActors();message="Prepare seu equipamento";}
    void finish(int winner,String reason){this.winner=winner;if(winner>=0)scores[winner]++;phase=RESULT;timer=3;message=(winner<0?"Empate":names[winner]+" venceu a rodada")+" • "+reason;for(Game g:seats){g.combat.swing=0;g.player.gun().burstLeft=0;}}
    @Override public void close(){for(Game g:seats)g.close();}
}
