package rift;

import java.util.*;
import static rift.Game.*;
import static rift.World.*;
import static java.awt.event.KeyEvent.*;

/** The queue and all other participants are explicitly local simulations. No network dependency. */
final class MatchFlow {
    enum Mode {
        COMPETITIVE("COMPETITIVO","5 × 5 / 7 vitórias + prorrogação / rank local",7,6,false),
        UNRANKED("SEM RANQUE","5 × 5 / primeiro a 5 / economia por rodada",5,4,false),
        SPIKE_RUSH("DISPUTA DA SPIKE","5 × 5 / até 7 rodadas / armas iguais por rodada",4,3,false),
        DEATHMATCH("MATA-MATA","10 participantes / 20 abates / respawn",20,4,true),
        TEAM_DM("TEAM DEATHMATCH","5 × 5 / 40 abates por equipe / respawn",40,4,true),
        PREMIER("PREMIER","Copa local / semifinal + final / primeiro a 4",4,3,false);
        final String label,description;final int target,half;final boolean respawn;
        Mode(String label,String desc,int target,int half,boolean respawn){this.label=label;description=desc;this.target=target;this.half=half;this.respawn=respawn;}
    }
    final Game g;Mode mode=Mode.UNRANKED;int mapIndex,botLocks,seriesStage;double elapsed,lockDelay;boolean accepted,locked,rewarded,matchWon;
    MatchFlow(Game g){this.g=g;}
    void play(){g.ui="modes";g.phase=Phase.MENU;accepted=locked=false;seriesStage=0;}
    void queue(){g.pendingTraining=false;elapsed=0;accepted=locked=false;botLocks=0;g.ui="queue";g.audio.play("ui");}
    void accept(){if(!g.ui.equals("found"))return;accepted=true;locked=false;elapsed=0;g.openAgentSelect(false,true);g.agentReturn="modes";}
    void lockAgent(){if(locked)return;if(!g.profile.unlocked(g.settings.agent)){if(!g.profile.unlock(g.settings.agent)){g.tell("Você precisa de 300 XP para este contrato",3);return;}g.tell("Contrato desbloqueado",2);}locked=true;lockDelay=0;g.audio.play("select");}
    boolean tick(double dt,Input.Frame in){
        switch(g.ui){
            case "modes","profile" -> {if(in.pressed(VK_ESCAPE))g.ui="menu";return true;}
            case "queue" -> {elapsed+=dt;if(in.pressed(VK_ESCAPE)){g.ui="modes";return true;}if(elapsed>=2){g.ui="found";elapsed=0;g.audio.play("start");}return true;}
            case "found" -> {elapsed+=dt;if(in.pressed(VK_ENTER))accept();else if(in.pressed(VK_ESCAPE)||elapsed>=15){g.ui="modes";elapsed=0;}return true;}
            case "loading" -> {elapsed+=dt;if(elapsed>=1.6)g.start(false);return true;}
            case "tournament" -> {if(in.pressed(VK_ENTER))nextTournament();if(in.pressed(VK_ESCAPE))g.ui="menu";return true;}
            case "agents" -> {if(accepted){elapsed+=dt;botLocks=Math.min(9,(int)(elapsed*4));if(locked)lockDelay+=dt;if(locked&&lockDelay>=2&&botLocks==9){botLocks=9;g.ui="loading";elapsed=0;return true;}}}
            default -> { }
        }
        return false;
    }
    void startRespawn(){g.phase=Phase.LIVE;g.timer=300;g.planted=false;for(Actor a:g.actors){a.carrier=false;a.credits=9000;a.primary=new Gun(a.id%4==0?Weapon.ECHO:a.id%4==1?Weapon.SHADE:a.id%4==2?Weapon.WISP:Weapon.RIDGE);a.slot=2;revive(a,true);}g.tell(mode.label+"  •  B: arsenal livre  •  Respawn em 3 s",5);}
    void revive(Actor a,boolean relocate){
        if(relocate)g.abilities.removeOwned(a);a.nearSight=a.eRegen=0;a.specialPistol=a.specialRifle=null;if(a.slot>=4)a.slot=a.primary==null?1:2;
        a.vx=a.vz=a.tagTime=a.landRecovery=0;a.dead=false;a.hp=100;a.armor=mode.respawn?25:0;a.vy=0;a.grounded=true;a.deathAge=0;a.flash=a.healing=a.revealed=a.emp=a.slow=a.detained=a.vulnerable=0;a.eyeHeight=1.63;a.crouch=false;a.moveSpeed=0;a.animSpeed=0;a.path=List.of();a.repath=0;a.target=null;a.respawn=0;a.returnTime=a.teleportTime=0;a.invulnerable=1.5;
        a.pistol=new Gun(a.pistol.kind);if(a.primary!=null)a.primary=new Gun(a.primary.kind);Agent kit=Agent.values()[a.agentIndex];a.qCharges=kit.qs;a.cCharges=kit.cs;a.eCharges=kit.es;
        if(relocate){double best=-1;V location=new V(72,0,120);for(int i=0;i<36;i++){double x=4+g.rng.nextDouble()*(World.WIDTH-8),z=4+g.rng.nextDouble()*(World.LENGTH-8);double y=g.world.surfaceAt(x,z);if(g.world.blocked(x,z,y,.4,1.8))continue;double closest=100;for(Actor enemy:g.actors)if(enemy!=a&&!enemy.dead&&enemy.team!=a.team)closest=Math.min(closest,Math.hypot(enemy.x-x,enemy.z-z));if(closest>best){best=closest;location=new V(x,y,z);}}a.x=location.x();a.y=location.y();a.z=location.z();a.yaw=Math.atan2(World.WIDTH/2-a.x,World.LENGTH/2-a.z);a.bodyYaw=a.yaw;}
        if(a==g.player){g.combat.reset();g.weaponEquip=.4;g.focus=0;g.dashTime=0;g.ui="play";}
    }
    Actor roamTarget(Actor a){Actor best=g.player;double near=1e9;for(Actor other:g.actors)if(other!=a&&!other.dead&&other.team!=a.team){double d=a.distance(other);if(d<near){best=other;near=d;}}return best;}
    void tickRespawn(double dt){
        for(Actor a:g.actors)if(a.dead){a.respawn-=dt;if(a.respawn<=0)revive(a,true);}
        if(mode==Mode.TEAM_DM){g.scoreBlue=g.actors.stream().filter(a->a.team==0).mapToInt(a->a.kills).sum();g.scoreRed=g.actors.stream().filter(a->a.team!=0).mapToInt(a->a.kills).sum();}
        else {g.scoreBlue=g.player.kills;g.scoreRed=g.actors.stream().filter(a->a!=g.player).mapToInt(a->a.kills).max().orElse(0);}
        if(g.scoreBlue>=mode.target||g.scoreRed>=mode.target||g.timer<=0)complete(g.scoreBlue>g.scoreRed);
    }
    void complete(boolean win){
        g.combat.swing=g.combat.inspect=0;matchWon=win;g.phase=Phase.MATCH;g.ui="play";g.endTitle=win?"VITÓRIA":"DERROTA";g.endReason=mode.label+" / LOCAL";
        if(!rewarded){g.profile.reward(g,win);rewarded=true;}
        if(mode==Mode.PREMIER&&seriesStage==0&&win){g.ui="tournament";return;}
        if(mode==Mode.PREMIER&&seriesStage==1&&win)g.endTitle="CAMPEÃO DA COPA";
    }
    void nextTournament(){seriesStage=1;mapIndex=(mapIndex+1)%3;elapsed=0;g.ui="loading";}
}
