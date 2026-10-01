package rift;

import static rift.Game.*;
import static rift.World.*;

/** Plant, recovery and defuse channels for the round objective. */
final class Spike {
    final Game g;
    Spike(Game game){g=game;}

    void updateSpike(double dt,boolean interact) {
        if(g.phase!=Phase.LIVE)return;
        if(!g.planted)  {
            boolean carrying=g.actors.stream().anyMatch(a->a.carrier&&!a.dead);
            if(!carrying)for(Actor a:g.actors)if(!a.dead&&a.team==g.attackTeam&&Math.hypot(a.x-g.spikeX,a.z-g.spikeZ)<1.7) {
                a.carrier=true;
                if(a==g.player)g.tell("Núcleo recuperado",2);
                break;
            }
            for(Actor a:g.actors)if(!a.dead&&a.carrier)  {
                boolean canPlant=g.world.site(a.x,a.z)!=null&&a.damageGlow<=0;
                if(a==g.player&&!g.observing) {
                    if(canPlant&&interact) {
                        g.plantProgress+=dt;
                        if(g.plantProgress>=3.2)g.plant(a);
                    }
                    else g.plantProgress=0;
                }
                else  {
                    if(canPlant&&a.moveSpeed<.1&&(a.target==null||a.distance(a.target)>15)) {
                        a.objective+=dt;
                        if(a.objective>=3.2)g.plant(a);
                    }
                    else a.objective=0;
                }
                break;
            }
        }
        else  {
            g.spikeTime-=dt;
            g.spikeBeep-=dt;
            if(g.spikeBeep<=0) {
                g.audio.play("beep");
                g.spikeBeep=g.spikeTime<10?.28:g.spikeTime<20?.6:1;
            }
            Actor candidate=eligible(g.defuser,interact)?g.defuser:null;
            for(Actor a:g.actors)if(!a.dead&&a.team!=g.attackTeam&&Math.hypot(a.x-g.spikeX,a.z-g.spikeZ)<2.5) {
                if(a==g.player&&!g.observing&&interact) {
                    candidate=a;
                    break;
                }
                if(candidate==null&&eligible(a,interact))candidate=a;
            }
            if(candidate!=g.defuser) {
                g.defuseProgress=0;
                g.defuser=candidate;
            }
            if(g.defuser!=null)g.defuseProgress+=dt;
            else g.defuseProgress=0;
            if(g.defuseProgress>=5) {
                g.defuser.credits=Math.min(9000,g.defuser.credits+300);
                g.defuser.ult=Math.min(6,g.defuser.ult+1);
                g.finishRound(1-g.attackTeam,"Núcleo desarmado");
                g.audio.play("defuse");
            }
            else if(g.spikeTime<=0) {
                g.finishRound(g.attackTeam,"Núcleo detonado");
                g.audio.play("boom");
            }
        }
    }

    boolean eligible(Actor a,boolean interact){
        if(a==null||a.dead||a.team==g.attackTeam||Math.hypot(a.x-g.spikeX,a.z-g.spikeZ)>=2.5)return false;
        if(a==g.player&&!g.observing)return interact;
        return (a.target==null||a.distance(a.target)>9)&&a.damageGlow<=0;
    }

    void plant(Actor a) {
        g.planted=true;
        g.spikeX=a.x;
        g.spikeZ=a.z;
        g.spikeTime=40;
        g.spikeBeep=0;
        a.carrier=false;
        g.plantProgress=0;
        a.credits=Math.min(9000,a.credits+300);
        a.ult=Math.min(6,a.ult+1);
        for(Actor b:g.actors) {
            b.repath=0;
            b.objective=0;
        }
        g.tell("NÚCLEO ARMADO  •  40 segundos",3);
        g.audio.play("plant");
        g.bots.noise(a,Bots.Noise.PLANT,90);
    }
}
