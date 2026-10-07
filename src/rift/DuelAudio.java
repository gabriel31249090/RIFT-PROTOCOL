package rift;

import static rift.Game.*;

/** Client-only cues follow server snapshots, never the render clock or gameplay state. */
final class DuelAudio {
    final Game game;
    final Previous[] previous={new Previous(),new Previous()};
    long tick=-1;int epoch=-1;
    static final class Previous {
        double x,y,z,walk,vy,reload,stepDelay;
        int slot,ammo,stage;
        Weapon weapon;
        boolean grounded,dead;
        void copy(Actor actor,boolean reset){
            x=actor.x;y=actor.y;z=actor.z;walk=actor.walk;vy=actor.vy;grounded=actor.grounded;dead=actor.dead;
            Gun gun=actor.gun();slot=actor.slot;weapon=gun.kind;reload=gun.reload;ammo=gun.ammo;
            if(reset){stepDelay=.36;stage=stage(gun);}
        }
    }
    DuelAudio(Game game){this.game=game;}
    static int stage(Gun gun){
        if(gun.reload<=0||gun.reloadTotal<=0)return 0;
        double elapsed=1-gun.reload/gun.reloadTotal;
        return elapsed>.82?3:elapsed>.55?2:elapsed>.14?1:0;
    }
    void observe(DuelProtocol.State state){
        if(state.epoch==epoch&&state.tick<=tick)return;
        double dt=(state.tick-tick)/60.;
        boolean reset=tick<0||state.epoch!=epoch||dt<=0||dt>.5||state.phase!=DuelSimulation.LIVE;
        for(int i=0;i<Math.min(previous.length,game.actors.size());i++){
            Actor actor=game.actors.get(i);Previous old=previous[i];
            double distance=Math.hypot(actor.x-old.x,actor.z-old.z);
            boolean discontinuity=distance>Math.max(2,dt*12)||Math.abs(actor.y-old.y)>Math.max(2,dt*16)||actor.walk<old.walk-.01;
            if(reset||discontinuity||old.dead||actor.dead){old.copy(actor,true);continue;}
            old.stepDelay=Math.max(0,old.stepDelay-dt);
            if(actor.grounded&&old.grounded&&!actor.crouch&&actor.moveSpeed>3.3&&distance>.005&&actor.walk>old.walk+.005&&old.stepDelay<=0){
                AudioSurface.step(game,actor);old.stepDelay=.36;
            }
            if(actor.grounded&&!old.grounded&&old.vy<=0){
                double speed=Math.sqrt(Math.max(0,old.vy*old.vy+32*(old.y-actor.y)));
                AudioSurface.land(game,actor,speed);
            }
            Gun gun=actor.gun();
            if(actor.slot!=old.slot||gun.kind!=old.weapon||actor.melee())old.stage=stage(gun);
            else{
                if(gun.reload>0&&(old.reload<=0||gun.reload>old.reload+.05)){
                    old.stage=0;AudioSurface.reload(game,actor);
                }
                int next=stage(gun);
                // Ammo replenishment distinguishes completion from a cancelled reload.
                if(gun.reload<=0&&old.reload>0&&gun.ammo>old.ammo)next=3;
                while(old.stage<next)AudioSurface.reloadStage(game,actor,++old.stage);
                if(gun.reload<=0)old.stage=0;
            }
            old.copy(actor,false);
        }
        tick=state.tick;epoch=state.epoch;
    }
}
