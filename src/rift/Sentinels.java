package rift;

import static rift.Game.*;
import static rift.World.*;
import static java.awt.event.KeyEvent.*;
import rift.Abilities.Device;

/** Stateful gadgets: reactivation never consumes a second charge. */
final class Sentinels {
    record Wire(V a,V b){}
    boolean placingWire;Wire preview;
    final Game g;Device camera;final Actor eye=new Actor(-1,0,"OLHO REMOTO");double networkTime=-1;Actor networkOwner;
    Sentinels(Game g){this.g=g;}
    void clear(){camera=null;networkTime=-1;cancelWire();}
    boolean watching(){return camera!=null;}
    void leave(){camera=null;g.weaponEquip=.3;}
    Device own(Actor a,Ability t){for(Device d:g.abilities.devices)if(d.owner==a&&d.type==t&&d.hp>0&&d.life>0)return d;return null;}
    boolean recast(Ability type){
        Actor a=g.player;
        if(type==Ability.VERDICT&&a.specialPistol!=null&&a.specialPistol.ammo>0){g.combat.equip(4);return true;}
        if(type==Ability.RAIL&&a.specialRifle!=null&&a.specialRifle.ammo>0){g.combat.equip(5);return true;}
        Device d=own(a,type);if(d==null)return false;
        if(type==Ability.ANCHOR){if(a.center().sub(d.position).length()>22){g.tell("Âncora fora do alcance de 22 m",2);return true;}g.abilities.teleport(a,d.position,.35);d.life=0;return true;}
        if(type==Ability.SPYCAM){camera=d;eye.team=a.team;eye.x=d.position.x();eye.y=d.position.y();eye.z=d.position.z();eye.eyeHeight=.25;eye.yaw=d.yaw;eye.pitch=0;g.aiming=false;g.tell("CÂMERA  •  Clique marca  •  E / Esc sai",3);return true;}
        if(type==Ability.CAGE){boolean activated=false;for(Device cage:g.abilities.devices)if(cage.owner==a&&cage.type==type&&!cage.active&&cage.life>0){cage.active=true;cage.life=7;activated=true;g.audio.play("smoke");}return activated;}
        return false;
    }
    boolean cast(Actor a,Ability type){
        switch(type){
            case VERDICT,RAIL -> {Gun gun=new Gun(type==Ability.VERDICT?Weapon.TALON:Weapon.HORIZON);gun.reserve=0;if(type==Ability.VERDICT)a.specialPistol=gun;else a.specialRifle=gun;if(a==g.player)g.combat.equip(type==Ability.VERDICT?4:5);else a.slot=type==Ability.VERDICT?4:5;}
            case ANCHOR -> {V p=new V(a.x,a.y,a.z);if(g.world.blocked(p.x(),p.z(),p.y(),.35,1.8))return false;add(type,a,p,90,80);}
            case BULWARK -> {V p=g.abilities.ahead(a,3);if(g.world.blocked(p.x(),p.z(),p.y(),.35,1.8))return false;Device d=add(type,a,p,12,250);d.yaw=a.yaw;double dx=Math.abs(Math.cos(a.yaw))*1.6+.1,dz=Math.abs(Math.sin(a.yaw))*1.6+.1;d.collider=new Box(p.x()-dx,p.y(),p.z()-dz,p.x()+dx,p.y()+2.3,p.z()+dz,0xD8B56D);}
            case TRIPWIRE -> {
                Wire wire=wire(a);if(wire==null){g.tell("Mire numa parede de corredor: vão de até 12 m",2);return false;}
                Device d=add(type,a,wire.a(),180,45);d.endpoint=wire.b();
            }
            case CAGE -> add(type,a,g.abilities.ahead(a,6),90,45);
            case SPYCAM -> {
                V dir=a.dir();double distance=g.world.ray(a.eye(),dir,18);
                if(distance>=18||distance<.8){g.tell("Câmera: mire em parede até 18 m",2);return false;}
                V p=a.eye().add(dir.mul(distance-.3));Device d=add(type,a,p,90,50);d.yaw=a.yaw+Math.PI;
            }
            case NETWORK -> {
                boolean corpse=false;for(Actor e:g.actors)if(e.dead&&e.team!=a.team&&a.distance(e)<12)corpse=true;
                if(!corpse){g.tell("Rastreamento precisa de um corpo inimigo até 12 m",3);return false;}
                networkOwner=a;networkTime=3;reveal(a);
            }
            default -> {return false;}
        }
        g.audio.play("select");return true;
    }
    Device add(Ability t,Actor a,V p,double life,double hp){Device d=new Device(t,a,p,life,hp);d.yaw=a.yaw;g.abilities.devices.add(d);return d;}
    void reveal(Actor owner){for(Actor a:g.actors)if(!a.dead&&a.team!=owner.team)a.revealed=1.2;g.abilities.effects.add(new Abilities.Effect(Ability.NETWORK,owner.center(),0,1.2));g.audio.play("scan");}
    void tick(double dt){
        if(placingWire&&(g.player.dead||g.player.emp>0||g.player.detained>0))cancelWire();
        if(networkTime>=0){networkTime-=dt;if(networkTime<0)reveal(networkOwner);}
        if(camera!=null&&(camera.hp<=0||camera.life<=0||g.player.dead||g.player.emp>0))leave();
        for(Device d:g.abilities.devices){if(d.hp<=0||d.life<=0)continue;
            if(d.type==Ability.TRIPWIRE&&d.endpoint!=null&&d.age>=.45){
                if(d.tethered==null){for(Actor a:g.actors)if(!a.dead&&a.team!=d.owner.team&&crossed(a,d.position,d.endpoint)){d.tethered=a;d.tetherTime=0;a.detained=.15;a.revealed=4;g.audio.play("wire");if(d.owner==g.player)g.tell("FIO ACIONADO  /  "+g.world.callout(a.x,a.z),3);break;}}
                else if(d.tethered.dead){d.life=0;}
                else{Actor a=d.tethered;d.tetherTime+=dt;a.detained=Math.max(a.detained,.15);a.revealed=Math.max(a.revealed,1);
                    V ab=d.endpoint.sub(d.position);double t=Settings.clamp(a.center().sub(d.position).dot(ab)/ab.dot(ab),0,1);V nearest=d.position.add(ab.mul(t));double dist=Math.hypot(a.x-nearest.x(),a.z-nearest.z());
                    if(dist>1.1){double pull=Math.min(dist-1.1,dt*18);g.move(a,(nearest.x()-a.x)/dist*pull,(nearest.z()-a.z)/dist*pull);}
                    if(d.tetherTime>=.9){g.damage(a,15,d.owner,false);a.detained=1.8;a.revealed=4;d.life=0;g.audio.play("scan");}
                }
            }
            if(d.type==Ability.CAGE&&d.active)for(Actor a:g.actors)if(!a.dead&&a.team!=d.owner.team&&Math.abs(Math.hypot(a.x-d.position.x(),a.z-d.position.z())-3)<.4&&d.cooldown<=0){a.revealed=.6;d.cooldown=.8;g.audio.play("scan");}
        }
    }
    void beginWire(){placingWire=true;preview=wire(g.player);g.aiming=g.aimLatched=false;g.combat.inspect=0;g.tell("Mire na parede • Clique instala • Direito / Esc cancela",3);}
    void cancelWire(){placingWire=false;preview=null;}
    void confirmWire(){
        preview=wire(g.player);if(preview==null){g.tell("Posição inválida: procure paredes opostas até 12 m",2);g.audio.play("deny");return;}
        if(g.player.emp>0||g.player.detained>0||g.player.dead||!g.training&&g.player.qCharges<=0){cancelWire();return;}
        Device d=add(Ability.TRIPWIRE,g.player,preview.a(),180,45);d.endpoint=preview.b();if(!g.training)g.player.qCharges--;
        cancelWire();g.audio.play("select");g.abilityAnim=.5;g.tell("FIO INSTALADO • Armando por 0,45 s",2);
    }
    Wire wire(Actor a){
        SurfaceHit first=SurfaceHit.cast(g.world,a.eye(),a.dir(),18,0);if(first==null||Math.abs(first.normal().y())>.1)return null;
        double height=first.point().y()-g.world.groundAt(first.point().x()+first.normal().x()*.1,first.point().z()+first.normal().z()*.1,first.point().y());if(height<.25||height>2.3)return null;
        V start=first.point().add(first.normal().mul(.035));SurfaceHit second=SurfaceHit.cast(g.world,start,first.normal(),12,0);
        if(second==null||second.distance()<.6||second.normal().dot(first.normal())>-.7)return null;
        return new Wire(start,second.point().add(second.normal().mul(.035)));
    }
    static boolean crossed(Actor a,V p,V q){
        V begin=a.motionStart==null?new V(a.x,a.y,a.z):a.motionStart;
        double dx=a.x-begin.x(),dz=a.z-begin.z(),ex=q.x()-p.x(),ez=q.z()-p.z(),rx=begin.x()-p.x(),rz=begin.z()-p.z();
        double aa=dx*dx+dz*dz,ee=ex*ex+ez*ez,ff=ex*rx+ez*rz;if(ee<1e-9)return false;
        double ss=0,tt;if(aa<1e-9)tt=Settings.clamp(ff/ee,0,1);else{double bb=dx*ex+dz*ez,cc=dx*rx+dz*rz,den=aa*ee-bb*bb;ss=den>1e-9?Settings.clamp((bb*ff-cc*ee)/den,0,1):0;tt=(bb*ss+ff)/ee;if(tt<0){tt=0;ss=Settings.clamp(-cc/aa,0,1);}else if(tt>1){tt=1;ss=Settings.clamp((bb-cc)/aa,0,1);}}
        double feet=begin.y()+(a.y-begin.y())*ss,wireY=p.y()+(q.y()-p.y())*tt;
        return wireY>feet+.04&&wireY<feet+(a.crouch?1.16:1.77)&&Math.hypot(rx+dx*ss-ex*tt,rz+dz*ss-ez*tt)<.34;
    }
    static double segment(double x,double z,V a,V b){double dx=b.x()-a.x(),dz=b.z()-a.z(),t=Settings.clamp(((x-a.x())*dx+(z-a.z())*dz)/(dx*dx+dz*dz),0,1);return Math.hypot(x-a.x()-t*dx,z-a.z()-t*dz);}
    boolean cageObscures(V from,V to){
        V delta=to.sub(from);double aa=delta.x()*delta.x()+delta.z()*delta.z();if(aa<1e-9)return false;
        for(Device d:g.abilities.devices)if(d.type==Ability.CAGE&&d.active&&d.life>0&&d.hp>0){
            double x=from.x()-d.position.x(),z=from.z()-d.position.z(),bb=2*(x*delta.x()+z*delta.z()),cc=x*x+z*z-9,disc=bb*bb-4*aa*cc;
            if(disc<0)continue;double root=Math.sqrt(disc);
            for(double t:new double[]{(-bb-root)/(2*aa),(-bb+root)/(2*aa)})if(t>=0&&t<=1){double height=from.y()+delta.y()*t-d.position.y();if(height>=0&&height<=3.2)return true;}
        }return false;
    }
    void control(double dt,Input.Frame in){
        if(in.pressed(VK_E)||g.settings.abilityHold&&!in.held(VK_E)){leave();return;}
        eye.yaw+=in.dx()*g.settings.sensitivity;eye.pitch=Settings.clamp(eye.pitch+in.dy()*g.settings.sensitivity*(g.settings.invertY?1:-1),-1.25,1.25);
        if(in.click()&&camera.cooldown<=0){Actor best=null;double distance=60;for(Actor a:g.actors)if(!a.dead&&a.team!=g.player.team){V delta=a.center().sub(eye.eye());if(delta.length()<distance&&delta.unit().dot(eye.dir())>.985&&g.world.visible(eye.eye(),a.center())&&!g.obscured(eye.eye(),a.center())){best=a;distance=delta.length();}}if(best!=null){best.revealed=5;g.traces.add(new Trace(eye.eye(),best.center(),0x87EAF5));g.audio.play("mark");}camera.cooldown=3;}
    }
}
