package rift;

import java.util.*;
import static rift.World.*;
import static rift.Game.*;

/** Ability simulation. Visuals and effects use the same positions, lifetimes and collision. */
final class Abilities {
    final Game g;
    final List<Projectile> projectiles=new ArrayList<>();
    final List<Zone> zones=new ArrayList<>();
    final List<Device> devices=new ArrayList<>();
    static final class Effect {final Ability type;final V position;final double yaw,duration;double age;Effect(Ability t,V p,double y,double d){type=t;position=p;yaw=y;duration=d;}}
    final List<Effect> effects=new ArrayList<>();
    V pingPoint;double pingLife;
    Abilities(Game game){g=game;}
    static final class Projectile {
        final Ability type;final Actor owner;V position,velocity;double fuse,age,curve;int bounces;boolean returning,resting;
        Projectile(Ability t,Actor a,V p,V v,double f){type=t;owner=a;position=p;velocity=v;fuse=f;}
    }
    static final class Zone {
        final Ability type;final Actor owner;final V position;final double radius,duration;double life,age,yaw;int pulses;
        Zone(Ability t,Actor a,V p,double radius,double life){type=t;owner=a;position=p;this.radius=radius;this.life=duration=life;}
        boolean wall(){return type==Ability.TOXIC_WALL||type==Ability.FIRE_WALL;}
    }
    static final class Device {
        final Ability type;final Actor owner;V position;double life,hp,cooldown,age,yaw;boolean active;Actor tethered;double tetherTime;V endpoint;Box collider;List<V> path=List.of();int pathIndex;
        Device(Ability type,Actor owner,V p,double life,double hp){this.type=type;this.owner=owner;position=p;this.life=life;this.hp=hp;}
        Box bounds(){if(type==Ability.TRIPWIRE)return new Box(position.x()-.16,position.y()-.16,position.z()-.16,position.x()+.16,position.y()+.16,position.z()+.16,0);return collider!=null?collider:new Box(position.x()-.42,position.y(),position.z()-.42,position.x()+.42,position.y()+.95,position.z()+.42,0);}
    }
    void removeOwned(Actor owner){devices.removeIf(d->{if(d.owner!=owner)return false;if(d.collider!=null)g.world.temporary.remove(d.collider);return true;});projectiles.removeIf(p->p.owner==owner);zones.removeIf(z->z.owner==owner);if(owner==g.player)g.sentinels.leave();}
    void clear(){effects.clear();g.sentinels.clear();projectiles.clear();zones.clear();devices.clear();g.world.temporary.clear();pingLife=0;}
    V ahead(Actor a,double distance){V dir=a.dir();double ray=g.world.ray(a.eye(),dir,distance);V p=a.eye().add(dir.mul(Math.max(.2,ray-.5)));return new V(p.x(),g.world.surfaceAt(p.x(),p.z()),p.z());}
    Actor healTarget(){Actor best=g.player;double dot=.93;for(Actor a:g.actors)if(a!=g.player&&!a.dead&&a.team==g.player.team&&a.hp<100&&g.player.distance(a)<18&&g.canSee(g.player,a)){double d=a.center().sub(g.player.eye()).unit().dot(g.player.dir());if(d>dot){dot=d;best=a;}}return best;}
    boolean cast(Ability type){return cast(g.player,type);}
    boolean cast(Actor a,Ability type){
        switch(type){
            case ECLIPSE,QUICK_SMOKE,POISON_CLOUD,ACID,INCENDIARY,SUPPRESS,FRAG,SLOW,ROCKET,CURVEFLASH -> launch(a,type);
            case UPDRAFT -> {a.vy=11.5;a.grounded=false;g.audio.play("dash");g.pulses.add(new Pulse(new V(a.x,a.y+.08,a.z),0xCDF8EE,3,.5));}
            case STIM -> zones.add(new Zone(type,a,new V(a.x,a.y,a.z),6,10));
            case HEAL_FIRE -> zones.add(new Zone(type,a,ahead(a,9),4,5));
            case TOXIC_WALL,FIRE_WALL -> {Zone z=new Zone(type,a,new V(a.x,a.y,a.z),type==Ability.FIRE_WALL?16:18,type==Ability.FIRE_WALL?8:10);z.yaw=a.yaw;zones.add(z);}
            case TOXIC_DOME -> {V at=new V(a.x,a.y,a.z);smoke(at,8,16,0xADCA73);zones.add(new Zone(type,a,at,8,16));}
            case BLINK -> {V dest=ahead(a,10);if(Math.hypot(dest.x()-a.x,dest.z()-a.z)<1||g.world.blocked(dest.x(),dest.z(),dest.y(),.34,1.78)){g.tell("Sem espaço para o passo",2);return false;}teleport(a,dest,.45);}
            case BLIND_WAVE -> {Zone z=new Zone(type,a,a.eye(),3,1.1);z.yaw=a.yaw;zones.add(z);}
            case RETURN -> {a.returnPoint=new V(a.x,a.y,a.z);a.returnYaw=a.yaw;a.returnTime=10;g.pulses.add(new Pulse(a.returnPoint.add(new V(0,.1,0)),0xFFC284,3,.6));}
            case SATCHEL -> {a.vy=9;a.grounded=false;g.dashTime=.4;g.dashX=Math.sin(a.yaw);g.dashZ=Math.cos(a.yaw);explode(a,new V(a.x,a.y,a.z),3,45);}
            case HUNTER_BOT,TURRET,TRAP,SENSOR,LOCKDOWN -> {V at=ahead(a,type==Ability.HUNTER_BOT?2.5:3);if(g.world.blocked(at.x(),at.z(),at.y(),.42,.9)){g.tell("Escolha piso livre",2);return false;}devices.add(new Device(type,a,at,type==Ability.LOCKDOWN?7.2:type==Ability.SENSOR?14:type==Ability.TRAP?30:24,type==Ability.LOCKDOWN?160:100));}
            case BARRIER -> {
                V at=ahead(a,4);boolean horizontal=Math.abs(Math.cos(a.yaw))>.7;double dx=horizontal?3.5:.55,dz=horizontal?.55:3.5;
                Box box=new Box(at.x()-dx,at.y(),at.z()-dz,at.x()+dx,at.y()+2.6,at.z()+dz,0x8ABCB4);
                for(Actor other:g.actors)if(!other.dead&&g.world.obstacle(box,other.x,other.z,other.y,.36,1.8)){g.tell("Barreira bloquearia um jogador",2);return false;}
                if(g.world.blocked(at.x(),at.z(),at.y(),.5,2.6)){g.tell("Sem espaço para a barreira",2);return false;}
                Device d=new Device(type,a,at,18,500);d.collider=box;devices.add(d);g.world.temporary.add(box);
            }
            case REVIVE -> {Actor target=null;double near=18;for(Actor other:g.actors)if(other.dead&&other!=a&&other.team==a.team&&a.distance(other)<near&&other.center().sub(a.eye()).unit().dot(a.dir())>.5&&g.world.visible(a.eye(),other.center())){target=other;near=a.distance(other);}
                if(target==null){g.tell("Nenhum aliado caído com visão em até 18 m",2);return false;}g.flow.revive(target,false);target.invulnerable=.8;g.pulses.add(new Pulse(target.center(),0xB4F7E0,4,1.2));}
            default -> {return g.sentinels.cast(a,type);}
        }
        g.audio.play(type==Ability.UPDRAFT||type==Ability.BLINK?"dash":"select");return true;
    }
    void launch(Actor a,Ability type){
        boolean direct=type==Ability.QUICK_SMOKE||type==Ability.ROCKET,curved=type==Ability.CURVEFLASH,under=a==g.player&&g.aiming;
        double speed=type==Ability.ROCKET?36:direct?29:under?8:16,fuse=type==Ability.QUICK_SMOKE?.42:type==Ability.ROCKET?2:curved?.65:under?.65:1.1;
        V velocity=a.dir().mul(speed).add(new V(0,direct||curved?0:2,0));
        SurfaceHit launchHit=SurfaceHit.cast(g.world,a.eye(),a.dir(),.32,.08);
        V start=a.eye().add(a.dir().mul(launchHit==null?.32:Math.max(0,launchHit.distance()-.01)));
        Projectile p=new Projectile(type,a,start,velocity,fuse);p.curve=curved?(under?-1:1)*3.6:0;projectiles.add(p);g.audio.play("equip");
    }
    void teleport(Actor a,V point,double delay){a.teleportPoint=point;a.teleportTime=delay;a.gun().burstLeft=0;g.pulses.add(new Pulse(a.center(),0xB2B1F0,2,delay));g.pulses.add(new Pulse(point.add(new V(0,.1,0)),0xB2B1F0,2,delay));}
    void returnActor(Actor a){V p=a.returnPoint;if(p==null)return;a.returnTime=0;a.hp=100;a.armor=0;a.x=p.x();a.y=p.y();a.z=p.z();a.yaw=a.returnYaw;a.vy=0;a.grounded=true;a.invulnerable=.6;a.teleportTime=0;g.pulses.add(new Pulse(a.center(),0xFFD5A3,4,1));if(a==g.player){g.weaponEquip=.4;g.tell("RENASCER  •  Você retornou à marca",3);}g.audio.play("heal");}
    void tickActor(Actor a,double dt){
        if(a.dead)return;
        if(a.teleportTime>0){a.teleportTime-=dt;if(a.teleportTime<=0&&a.teleportPoint!=null){V p=a.teleportPoint;if(!g.world.blocked(p.x(),p.z(),p.y(),.34,1.78)){a.x=p.x();a.y=p.y();a.z=p.z();a.vx=a.vz=a.vy=0;a.grounded=true;a.path=List.of();g.audio.play("dash");}if(a==g.player)g.weaponEquip=.35;}}
        if(a.returnTime>0){a.returnTime-=dt;if(a.returnTime<=0)returnActor(a);}
    }
    void smoke(V at,double radius,double duration,int color){Smoke s=new Smoke(at.x(),at.z(),0);s.y=at.y()+1.5;s.maxRadius=radius;s.life=duration;s.grow=.16;s.color=color;s.style=radius>6?Ability.TOXIC_DOME:color==0xC0E2DF?Ability.QUICK_SMOKE:Ability.POISON_CLOUD;g.smokes.add(s);g.audio.play("smoke");}
    void detonate(Projectile p){
        V at=p.position;double floor=g.world.groundAt(at.x(),at.z(),at.y());V ground=new V(at.x(),floor,at.z());
        switch(p.type){
            case ECLIPSE -> zones.add(new Zone(p.type,p.owner,ground,4,9));
            case QUICK_SMOKE -> smoke(new V(at.x(),Math.max(floor,at.y()-1.5),at.z()),2.6,4.5,0xC0E2DF);
            case POISON_CLOUD -> {smoke(ground,3.8,10,0xB4CD85);zones.add(new Zone(p.type,p.owner,ground,3.8,10));}
            case FLASH,CURVEFLASH -> flash(at);
            case SUPPRESS -> {g.pulses.add(new Pulse(at,0xB6ADFF,9,.8));for(Actor a:g.actors)if(!a.dead&&a.team!=p.owner.team&&a.center().sub(at).length()<9)a.emp=5;g.audio.play("scan");}
            case ROCKET -> explode(p.owner,at,6,180);
            case FRAG -> {explode(p.owner,at,5,65);zones.add(new Zone(p.type,p.owner,ground,5,.9));}
            case ACID,SLOW,INCENDIARY -> zones.add(new Zone(p.type,p.owner,ground,p.type==Ability.INCENDIARY?4:5,p.type==Ability.SLOW?7:p.type==Ability.ACID?5:6));
            default -> { }
        }
    }
    void flash(V at){
        g.flashCount=0;g.pulses.add(new Pulse(at,0xFFF7D4,3,.4));g.audio.play("flash");
        for(Actor a:g.actors)if(!a.dead){V to=at.sub(a.eye());double distance=to.length();if(distance<22&&g.world.visible(a.eye(),at)&&!g.obscured(a.eye(),at)){double facing=a.dir().dot(to.unit());a.flash=Math.max(a.flash,(facing>.35?2.4:.55)*Math.min(1,26/(distance+8)));if(a.team!=g.player.team)g.flashCount++;}}
        g.tell("CLARÃO  •  "+g.flashCount+" inimigo(s) atingido(s)",2);
    }
    void explode(Actor owner,V at,double radius,double damage){
        for(Actor a:g.actors)if(!a.dead&&a.team!=owner.team){double dist=a.center().sub(at).length();if(dist<radius&&g.world.visible(at,a.center()))g.damage(a,damage*(1-dist/radius*.75),owner,false);}
        g.pulses.add(new Pulse(at,0xFFC993,radius,.5));for(int i=0;i<22;i++)g.particle(at,new V((g.rng.nextDouble()-.5)*8,g.rng.nextDouble()*5,(g.rng.nextDouble()-.5)*8),.6,.08,0xFFC58B);g.audio.play("boom");
    }
    void tick(double dt){
        pingLife=Math.max(0,pingLife-dt);effects.removeIf(e->(e.age+=dt)>=e.duration);g.sentinels.tick(dt);
        for(Iterator<Projectile> it=projectiles.iterator();it.hasNext();){Projectile p=it.next();
            int steps=Math.max(1,(int)Math.ceil(dt*240));double step=dt/steps;
            for(int i=0;i<steps&&p.fuse>0;i++)advance(p,Math.min(step,p.fuse));
            if(p.fuse<=0){it.remove();effects.add(new Effect(p.type,p.position,0,.8));detonate(p);}
        }
        for(Iterator<Zone> it=zones.iterator();it.hasNext();){Zone z=it.next();z.life-=dt;z.age+=dt;if(z.life<=0){it.remove();continue;}
            if(z.type==Ability.FRAG){if(z.age>(z.pulses+1)*.3&&z.pulses<2){z.pulses++;explode(z.owner,z.position.add(new V((z.pulses==1?-1:1)*1.5,.4,0)),z.radius,45);}continue;}
            for(Actor a:g.actors)if(!a.dead){double dx=a.x-z.position.x(),dz=a.z-z.position.z(),distance=Math.hypot(dx,dz);boolean enemy=a.team!=z.owner.team;
                if(z.wall()){double along=dx*Math.sin(z.yaw)+dz*Math.cos(z.yaw),side=dx*Math.cos(z.yaw)-dz*Math.sin(z.yaw);if(along<0||along>z.radius||Math.abs(side)>1||Math.abs(a.y-z.position.y())>4)continue;}
                else if(z.type==Ability.BLIND_WAVE){V c=z.position.add(new V(Math.sin(z.yaw)*z.age*22,0,Math.cos(z.yaw)*z.age*22));if(a.center().sub(c).length()>3)continue;}
                else if(distance>z.radius||Math.abs(a.y-z.position.y())>4)continue;
                switch(z.type){
                    case STIM -> {if(!enemy){a.stim=.25;a.energy=.3;}}
                    case HEAL_FIRE -> {if(a==z.owner)a.hp=Math.min(100,a.hp+18*dt);else if(enemy)g.damage(a,26*dt,z.owner,false);}
                    case SLOW -> {a.slow=.3;}
                    case ACID -> {if(enemy){a.vulnerable=1.2;g.damage(a,22*dt,z.owner,false);}}
                    case INCENDIARY,FIRE_WALL -> {if(enemy)g.damage(a,30*dt,z.owner,false);}
                    case TOXIC_WALL,POISON_CLOUD,TOXIC_DOME -> {if(enemy){a.slow=.25;g.damage(a,(z.type==Ability.TOXIC_DOME?18:12)*dt,z.owner,false);if(z.type==Ability.TOXIC_DOME&&z.owner.distance(a)<8)a.revealed=.3;}}
                    case ECLIPSE -> {if(enemy){a.nearSight=.3;a.revealed=0;}}
                    case BLIND_WAVE -> {if(enemy)a.nearSight=3;}
                    default -> { }
                }
            }
        }
        for(Iterator<Device> it=devices.iterator();it.hasNext();){Device d=it.next();d.age+=dt;d.life-=dt;d.cooldown-=dt;
            if(d.hp<=0||d.life<=0){if(d.hp>0&&d.type==Ability.LOCKDOWN){for(Actor a:g.actors)if(!a.dead&&a.team!=d.owner.team&&a.center().sub(d.position).length()<25)a.detained=6;g.pulses.add(new Pulse(d.position,0xEACD92,25,1));}if(d.collider!=null)g.world.temporary.remove(d.collider);it.remove();continue;}
            Actor nearest=null;double range=d.type==Ability.TRAP?3:d.type==Ability.SENSOR?12:d.type==Ability.HUNTER_BOT?80:26;
            for(Actor a:g.actors)if(!a.dead&&a.team!=d.owner.team){double dist=a.center().sub(d.position).length();if((d.type!=Ability.SENSOR||a.moveSpeed>3.3||a.shotGlow>0)&&dist<range&&(d.type==Ability.HUNTER_BOT||d.type==Ability.SENSOR||g.world.visible(d.position.add(new V(0,.6,0)),a.center()))){range=dist;nearest=a;}}
            if(nearest==null)continue;
            if(d.type==Ability.TRAP){nearest.slow=4;nearest.revealed=5;d.life=0;g.pulses.add(new Pulse(d.position,0xF4D39C,3,.6));}
            if(d.type==Ability.SENSOR&&d.cooldown<=0){for(Actor a:g.actors)if(!a.dead&&a.team!=d.owner.team&&a.center().sub(d.position).length()<12&&(a.moveSpeed>3.3||a.shotGlow>0))a.revealed=2;d.cooldown=2;g.pulses.add(new Pulse(d.position,0xECD097,12,.6));}
            if(d.type==Ability.TURRET&&d.cooldown<=0&&!g.obscured(d.position.add(new V(0,.7,0)),nearest.center())){g.damage(nearest,12,d.owner,false);d.cooldown=.45;g.traces.add(new Trace(d.position.add(new V(0,.7,0)),nearest.center(),0xF9DD9A));}
            if(d.type==Ability.HUNTER_BOT){if(range<2){explode(d.owner,d.position.add(new V(0,.4,0)),4,70);d.life=0;}else{if(d.cooldown<=0){d.path=g.world.path(d.position.x(),d.position.z(),nearest.x,nearest.z);d.pathIndex=0;d.cooldown=.6;}if(d.pathIndex<d.path.size()){V goal=d.path.get(d.pathIndex),delta=goal.sub(d.position);if(delta.length()<.25)d.pathIndex++;else d.position=d.position.add(delta.unit().mul(Math.min(delta.length(),dt*6)));}}}
        }
    }
    // Reflection about the contact normal, with material-dependent restitution and friction.
    void advance(Projectile p,double dt){
        p.age+=dt;p.fuse-=dt;if(p.resting)return;
        if(p.curve!=0){double sin=Math.sin(p.curve*dt),cos=Math.cos(p.curve*dt);p.velocity=new V(p.velocity.x()*cos+p.velocity.z()*sin,p.velocity.y(),-p.velocity.x()*sin+p.velocity.z()*cos);}
        if(p.type!=Ability.QUICK_SMOKE&&p.type!=Ability.ROCKET&&p.type!=Ability.CURVEFLASH)p.velocity=p.velocity.add(new V(0,-9*dt,0));
        double remaining=dt;
        for(int contact=0;contact<4&&remaining>1e-7;contact++){
            double speed=p.velocity.length();if(speed<1e-5)break;V dir=p.velocity.mul(1/speed);
            SurfaceHit hit=SurfaceHit.cast(g.world,p.position,dir,speed*remaining,.075);
            if(hit==null){p.position=p.position.add(p.velocity.mul(remaining));break;}
            p.position=hit.point().add(hit.normal().mul(.002));remaining=Math.max(0,remaining-hit.distance()/speed);
            if(p.type==Ability.ROCKET||p.type==Ability.QUICK_SMOKE){p.fuse=0;return;}
            p.bounces++;p.curve=0;V n=hit.normal();double incoming=p.velocity.dot(n);
            double restitution=hit.material()==Assets.WOOD?.42:hit.material()==Assets.PLATE?.68:.56;
            V tangent=p.velocity.sub(n.mul(incoming));p.velocity=tangent.mul(n.y()>.5?.79:.94).sub(n.mul(incoming*restitution));
            if(p.bounces<=5)g.audio.playAt("bounce",0,Math.min(.5,speed/25));
            if(n.y()>.5&&Math.abs(p.velocity.dot(n))<.75)p.velocity=p.velocity.sub(n.mul(p.velocity.dot(n)));
            if(n.y()>.5&&p.velocity.length()<.4){p.velocity=new V(0,0,0);p.resting=true;break;}
        }
        if(p.type==Ability.ROCKET)for(Actor a:g.actors)if(!a.dead&&a.team!=p.owner.team&&a.center().sub(p.position).length()<.75)p.fuse=0;
    }
    boolean wallObscures(V from,V to){
        for(Zone z:zones)if(z.wall()){double nx=Math.cos(z.yaw),nz=-Math.sin(z.yaw),a=(from.x()-z.position.x())*nx+(from.z()-z.position.z())*nz,b=(to.x()-z.position.x())*nx+(to.z()-z.position.z())*nz;
            if(a*b<=0&&Math.abs(a-b)>.001){double t=a/(a-b);V p=from.add(to.sub(from).mul(t));double along=(p.x()-z.position.x())*Math.sin(z.yaw)+(p.z()-z.position.z())*Math.cos(z.yaw);if(along>=0&&along<=z.radius&&p.y()>z.position.y()&&p.y()<z.position.y()+3.6)return true;}}
        return false;
    }
    static double deviceHit(Device d,V origin,V dir){
        if(d.type==Ability.BULWARK){double c=Math.cos(d.yaw),s=Math.sin(d.yaw);V delta=origin.sub(d.position);return World.rayBox(new V(delta.x()*c-delta.z()*s,delta.y(),delta.x()*s+delta.z()*c),new V(dir.x()*c-dir.z()*s,dir.y(),dir.x()*s+dir.z()*c),new Box(-1.6,0,-.10,1.6,2.3,.10,0));}
        double distance=World.rayBox(origin,dir,d.bounds());
        if(d.type==Ability.TRIPWIRE&&d.endpoint!=null){V p=d.endpoint;double other=World.rayBox(origin,dir,new Box(p.x()-.15,p.y()-.15,p.z()-.15,p.x()+.15,p.y()+.15,p.z()+.15,0));if(other>=0&&(distance<0||other<distance))distance=other;}
        return distance;
    }
    double hitDevice(Actor shooter,V origin,V dir,double max,double amount){Device nearest=null;double best=max+.03;for(Device d:devices)if(d.hp>0&&d.life>0&&d.owner.team!=shooter.team){double t=deviceHit(d,origin,dir);if(t>=0&&t<best){best=t;nearest=d;}}if(nearest!=null){nearest.hp-=amount;return best;}return -1;}
    void ping(){V p=ahead(g.player,60);pingPoint=p;pingLife=7;g.tell("PING  •  "+g.world.callout(p.x(),p.z()),2);g.audio.play("mark");for(Actor a:g.actors)if(a!=g.player&&a.team==g.player.team&&!a.dead){a.repath=0;a.destX=p.x();a.destZ=p.z();}}
    void botCast(Actor a,Actor enemy){
        Agent kit=Agent.values()[a.agentIndex];Ability type=kit.c;
        if(a.cCharges<=0)return;
        switch(type){
            case FLASH,CURVEFLASH,ACID,INCENDIARY,FRAG,SLOW -> {launch(a,type);a.cCharges--;}
            case QUICK_SMOKE -> {if(a.hp<70){smoke(new V(a.x+Math.sin(a.yaw)*4,a.y,a.z+Math.cos(a.yaw)*4),2.6,4.5,0xC0E2DF);a.cCharges--;}}
            case FIRE_WALL,BLIND_WAVE,HUNTER_BOT,SENSOR -> {if(cast(a,type))a.cCharges--;}
            default -> { }
        }
        if(kit.e==Ability.HEAL&&a.eCharges>0&&a.hp<65){a.healing=5;a.healRate=11;a.eCharges--;}
        if(kit.e==Ability.SUPPRESS&&a.eCharges>0){launch(a,Ability.SUPPRESS);a.eCharges--;}
        if(kit.e==Ability.STIM&&a.eCharges>0){cast(a,Ability.STIM);a.eCharges--;}
    }
    static int color(Ability type){return switch(type){case TOXIC_WALL,POISON_CLOUD,ACID,TOXIC_DOME->0xB3D478;case INCENDIARY,HEAL_FIRE,FIRE_WALL,FRAG,ROCKET->0xE9AE78;case FLASH,CURVEFLASH->0xFFF3C1;case SLOW,BARRIER,HEAL,REVIVE->0x9BD9D5;case BLIND_WAVE,BLINK->0xA8A1DA;default->0xC8D6BB;};}
}
