package rift;

import static rift.Game.*;
import static rift.World.*;

/** Local combat timing in seconds and metres. Input is never smoothed with the viewmodel. */
final class Combat {
    final Game g;
    double swing,elapsed,inspect,charmAngle,charmVelocity,charmTwist,previousSpeed;
    boolean heavy,connected,rightHeld;int combo,hops;double jumpBuffer,landingAge=1,bestSpeed;
    Combat(Game g){this.g=g;}
    void reset(){swing=elapsed=inspect=charmAngle=charmVelocity=charmTwist=previousSpeed=0;connected=rightHeld=false;jumpBuffer=0;landingAge=1;hops=0;bestSpeed=0;}
    void tick(double dt){
        inspect=Math.max(0,inspect-dt);
        if(swing>0){elapsed+=dt;swing=Math.max(0,swing-dt);if(!connected&&elapsed>=(heavy?.26:.13)){connected=true;if(g.player!=null&&!g.player.dead&&g.player.melee())strike();}}
        if(g.player!=null){double force=(g.player.moveSpeed-previousSpeed)*.65;previousSpeed=g.player.moveSpeed;charmVelocity+=Settings.clamp(force,-1.5,1.5);
            int n=Math.max(1,(int)Math.ceil(dt/.008));double step=dt/n;for(int i=0;i<n;i++){charmVelocity+=(-32*charmAngle-5.5*charmVelocity)*step;charmAngle=Settings.clamp(charmAngle+charmVelocity*step,-.9,.9);}charmTwist+=(g.swayX*12-charmTwist)*Math.min(1,dt*8);
        }
    }
    void movement(double dt,double speed){
        Actor a=g.player;int n=Math.max(1,(int)Math.ceil(dt*240));double step=dt/n;
        for(int i=0;i<n;i++){
            if(a.grounded){
                boolean moving=speed>0&&Math.hypot(a.intentX,a.intentZ)>.1;
                double velocity=Math.hypot(a.vx,a.vz),stopSpeed=moving?Math.min(1.2,speed*.25):6.2;
                if(velocity>0){
                    double remaining=Math.max(0,velocity-Math.max(velocity,stopSpeed)*(moving?6:14)*step);
                    a.vx*=remaining/velocity;a.vz*=remaining/velocity;
                }
                if(moving){
                    // Friction removes old momentum; acceleration only fills the requested projection.
                    double projection=a.vx*a.intentX+a.vz*a.intentZ;
                    double add=Math.min(Math.max(0,speed-projection),speed*12*step);
                    a.vx+=a.intentX*add;a.vz+=a.intentZ*add;
                    velocity=Math.hypot(a.vx,a.vz);
                    if(velocity>speed){a.vx*=speed/velocity;a.vz*=speed/velocity;}
                }
            }else if(speed>0&&Math.hypot(a.intentX,a.intentZ)>.1){
                // Projection-limited air acceleration allows orthogonal strafe to add speed.
                double projection=a.vx*a.intentX+a.vz*a.intentZ;
                double add=Math.min(Math.max(0,Math.min(2.8,speed)-projection),speed*9*step);
                a.vx+=a.intentX*add;a.vz+=a.intentZ*add;
                double velocity=Math.hypot(a.vx,a.vz),cap=a.tagTime>0?6:11.5;
                if(velocity>cap){a.vx*=cap/velocity;a.vz*=cap/velocity;}
            }
            double x=a.x,z=a.z;g.move(a,a.vx*step,a.vz*step);
            if(Math.abs(a.x-x)<1e-7)a.vx=0;if(Math.abs(a.z-z)<1e-7)a.vz=0;
        }
    }
    void jump(double dt,Input.Frame in){
        landingAge+=dt;jumpBuffer=Math.max(0,jumpBuffer-dt);
        if(in.pressed(java.awt.event.KeyEvent.VK_SPACE))jumpBuffer=.10;
        Actor a=g.player;
        if(a.grounded&&jumpBuffer>0&&a.teleportTime<=0&&a.detained<=0&&g.plantProgress<=0){
            hops=landingAge<.10?hops+1:1;a.vy=6;a.grounded=false;a.landRecovery=0;jumpBuffer=0;g.audio.play("step");
        }
        bestSpeed=Math.max(bestSpeed,Math.hypot(a.vx,a.vz));
    }
    void landed(){landingAge=0;}
    void equip(int slot){
        g.sentinels.cancelWire();
        Actor p=g.player;if(p.slot==slot||slot==2&&p.primary==null)return;
        Gun old=p.gun();old.burstLeft=0;old.reload=0;p.slot=slot;g.recoil=0;g.aiming=g.aimLatched=false;g.aimLerp=0;
        g.weaponEquip=slot==3?.30:p.gun().kind.scoped()?.62:p.gun().kind.sidearm()?.38:.48;
        swing=inspect=0;g.audio.play("equip");
    }
    void attack(boolean strong){
        if(swing>0||g.weaponEquip>0||g.dashTime>0||g.dashRecovery>0||g.player.detained>0||g.player.teleportTime>0||g.plantProgress>0||g.defuser==g.player)return;
        heavy=strong;elapsed=0;connected=false;swing=strong?.88:.46;combo++;inspect=0;g.player.invulnerable=0;g.audio.play(strong?"stab":"slash");
    }
    void strike(){
        Actor p=g.player;V origin=p.eye(),dir=p.dir();double reach=heavy?1.85:2.15,closest=reach;Actor victim=null;
        for(Actor e:g.actors)if(!e.dead&&e.team!=p.team){V point=new V(e.x,Settings.clamp(origin.y(),e.y+.25,e.y+(e.crouch?1.16:1.76)),e.z);V delta=point.sub(origin);double d=delta.length(),edge=Math.max(0,d-.29);
            if(edge<closest&&delta.unit().dot(dir)>(heavy?.94:.84)&&g.world.visible(origin,point)){victim=e;closest=edge;}}
        double damage=heavy?75:50;
        // Destructible gadgets in front of an actor intercept the attack.
        double obstruction=g.world.ray(origin,dir,closest);double device=g.abilities.hitDevice(p,origin,dir,obstruction,damage);
        if(device>=0){impact(origin.add(dir.mul(device)),false);return;}
        if(victim!=null){V behind=p.center().sub(victim.center()).unit();boolean back=behind.dot(new V(Math.sin(victim.yaw),0,Math.cos(victim.yaw)))<-.55;
            g.damage(victim,damage*(back?2:1),p,false);g.hitMarker=.20;g.hitHead=back?.25:0;g.trainingHits++;g.audio.play(back?"head":"hit");impact(victim.center(),true);
            if(back)g.tell("GOLPE PELAS COSTAS",1.1);
        }else {double wall=g.world.ray(origin,dir,reach);if(wall<reach-.001)impact(origin.add(dir.mul(wall)),false);}
        g.kickVelocity+=.45;
    }
    void impact(V at,boolean hit){g.audio.play(hit?"bladehit":"metal");for(int i=0;i<7;i++)g.particle(at,new V((g.rng.nextDouble()-.5)*2,g.rng.nextDouble()*2,(g.rng.nextDouble()-.5)*2),.25,.026,hit?0xF3D5B0:0xABD7DD);}
    static double recovery(Weapon w){return w.handling.recoveryDelay;}
    static void recover(Gun gun,double dt){
        double before=gun.shotAge;gun.shotAge+=dt;
        // Only time past the recovery delay counts, including frames that cross it.
        double active=Math.max(0,gun.shotAge-recovery(gun.kind))-Math.max(0,before-recovery(gun.kind));
        if(active>0){
            gun.sprayStep=0;double decay=Math.exp(-active*gun.kind.handling.recoveryRate);
            gun.pitchRecoil*=decay;gun.yawRecoil*=decay;gun.bloom*=decay;
        }
    }
    static double movementError(Actor p){double max=p.melee()?6.2:5.4*p.gun().kind.mobility;return Math.max(0,p.moveSpeed-max*.275);}
    static double spread(Actor actor,boolean aiming,boolean focus){
        Gun gun=actor.gun();Weapon w=gun.kind;WeaponHandling h=w.handling;
        double base=w.spread*(w.pellets>1?1:.30)*(aiming?.35:1)*(actor.crouch?.8:1);
        if(w==Weapon.HORIZON&&!aiming)base+=.035;
        base+=movementError(actor)*h.moveSpread+(actor.grounded?0:h.airSpread)+(actor.landRecovery>0?.018:0)+gun.bloom;
        return base*(focus?.18:1);
    }
    static void recoil(Gun gun,boolean crouch,boolean focus){
        gun.shotAge=0;int step=++gun.sprayStep;WeaponHandling h=gun.kind.handling;
        double strength=(crouch?.82:1)*(focus?.25:1);
        gun.pitchRecoil=Math.min(h.riseCap,h.rise+(step-1)*h.riseGrowth)*strength;
        int lateral=Math.max(0,step-h.sideStart);
        gun.yawRecoil=Math.sin(lateral*h.sideFrequency)*Math.min(h.sideCap,lateral*h.sideGrowth)*strength;
        gun.bloom=Math.min(h.bloomCap,Math.max(0,step-2)*h.bloomGrowth)*strength;
    }
}
