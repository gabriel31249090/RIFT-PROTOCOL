package rift;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import static rift.World.*;
import static rift.Game.*;

/** Original silhouettes and animated materials, shared by in-world effects and the kit atlas. */
final class AbilityArt {
    static final int DARK=0x223B49;
    static int color(Ability a){for(Agent agent:Agent.values())for(int slot=0;slot<4;slot++)if(agent.slot(slot)==a)return agent.color;return 0xBBE2CE;}
    static WeaponModel model(Renderer r,V p,double yaw,int c){return new WeaponModel(r.dynamic,p.x(),p.y(),p.z(),yaw,1,c);}
    static void gem(Renderer r,V p,double width,double height,int color,double angle){
        V top=p.add(new V(0,height,0)),bottom=p.add(new V(0,-height*.55,0));
        for(int i=0;i<6;i++){double a=angle+i*Math.PI/3,b=a+Math.PI/3;V one=p.add(new V(Math.cos(a)*width,0,Math.sin(a)*width)),two=p.add(new V(Math.cos(b)*width,0,Math.sin(b)*width));r.dynamic.add(new Tri(top,one,two,shade(color,.75+i*.08)));r.dynamic.add(new Tri(bottom,two,one,shade(color,.5+i*.06)));}
    }
    static void tether(Renderer r,V a,V b,double width,int c){V direction=b.sub(a),side=new V(direction.z(),0,-direction.x()).unit().mul(width);quad(r.dynamic,a.add(side),a.sub(side),b.sub(side),b.add(side),c);V up=new V(0,width,0);quad(r.dynamic,a.add(up),a.sub(up),b.sub(up),b.add(up),c);}
    static void projectile(Renderer r,Abilities.Projectile p,double time){
        V pos=p.position;int c=color(p.type);double yaw=Math.atan2(p.velocity.x(),p.velocity.z());WeaponModel m=model(r,pos,yaw,c);
        switch(p.type){
            case QUICK_SMOKE -> {for(int i=0;i<3;i++){double a=time*18+i*2.094;gem(r,pos.add(new V(Math.cos(a)*.15,Math.sin(a)*.12,0)),.04,.09,0xE8FFFF,a);}r.sphere(pos.x(),pos.y(),pos.z(),.08,c,time);}
            case FLASH -> {m.body(-.09,-.13,-.08,.09,.13,.08,DARK);for(int i=0;i<3;i++){double a=i*2.094;m.box(Math.cos(a)*.12-.03,Math.sin(a)*.12-.03,-.1,Math.cos(a)*.12+.03,Math.sin(a)*.12+.03,.1,0xC4BCFF);}m.tube(0,0,-.11,.23,.045,0xF5F0DC,6);}
            case CURVEFLASH -> {gem(r,pos,.16,.19,0xFFD398,time*12);for(int i=1;i<6;i++)gem(r,pos.sub(p.velocity.unit().mul(i*.1)),.10/i,.15/i,shade(0xF6B478,1-i*.1),time*8+i);}
            case ACID -> {m.tube(0,0,-.16,.28,.073,0xAEE973,6);m.body(-.11,-.10,-.19,.11,.10,-.13,DARK);m.box(-.04,-.04,.12,.04,.04,.20,0xD8F9A5);}
            case INCENDIARY -> {m.tube(0,0,-.15,.3,.085,0x8B6553,8);for(int i=0;i<3;i++)m.body(-.092,-.092,-.12+i*.095,.092,.092,-.10+i*.095,0xEFBE83);}
            case POISON_CLOUD -> {m.body(-.12,-.10,-.1,.12,.10,.1,0x6C8549);for(int i=0;i<4;i++)m.box(-.125,-.065+i*.04,-.11,.125,-.05+i*.04,-.10,0xDCEDA3);gem(r,pos,.08,.16,0xC2DB76,time);}
            case SUPPRESS -> {m.body(-.08,-.05,-.20,.08,.05,.05,DARK);m.face(m.p(-.15,0,.02),m.p(0,0,.36),m.p(.15,0,.02),m.p(0,0,-.1),0xD6CDFF);m.box(-.025,.005,-.2,.025,.04,.22,0x958AE1);}
            case FRAG -> {m.body(-.14,-.13,-.12,.14,.13,.12,0xB37658);for(int i=0;i<6;i++){double a=i*Math.PI/3;gem(r,pos.add(new V(Math.cos(a)*.13,Math.sin(a)*.13,0)),.04,.07,0xF2CB8F,time);}}
            case SLOW -> {gem(r,pos,.14,.22,0xC9FFEF,time*3);for(int i=0;i<3;i++){double a=time*4+i*2.094;gem(r,pos.add(new V(Math.sin(a)*.23,0,Math.cos(a)*.23)),.04,.06,0x8EE9DD,-a);}}
            case ECLIPSE -> {for(int i=0;i<4;i++){double a=time*6+i*Math.PI/2;gem(r,pos.add(new V(Math.cos(a)*.13,Math.sin(a)*.13,0)),.07,.16,0x9282C9,a);}r.sphere(pos.x(),pos.y(),pos.z(),.09,0x352E56,time);}
            case ROCKET -> {m.tube(0,0,-.35,.65,.10,DARK,8);gem(r,pos.add(p.velocity.unit().mul(.37)),.12,.18,0xEABC80,time);for(int i:new int[]{-1,1}){m.box(i*.17-.02,-.02,-.3,i*.17+.02,.02,-.08,c);m.box(-.02,i*.17-.02,-.3,.02,i*.17+.02,-.08,c);}gem(r,pos.sub(p.velocity.unit().mul(.43)),.10,.18,0xFFEFC1,time);}
            default -> gem(r,pos,.13,.20,c,time*4);
        }
        tether(r,pos,pos.sub(p.velocity.unit().mul(p.returning?.8:.25)),.014,p.returning?0xFFDEB4:c);
    }
    static void zone(Renderer r,Abilities.Zone z,double time,World world){
        V p=z.position;int c=color(z.type);
        if(z.wall()){
            for(int i=0;i<(int)z.radius;i++){double x=p.x()+Math.sin(z.yaw)*(i+.5),zz=p.z()+Math.cos(z.yaw)*(i+.5),y=world.surfaceAt(x,zz),h=3.1+.25*Math.sin(time*5+i);WeaponModel m=model(r,new V(x,y,zz),z.yaw,c);
                m.box(-.08,0,-.51,.08,h*Math.min(1,z.age/.3),.51,shade(c,.55+.12*Math.sin(i+time)));
                if(z.type==Ability.FIRE_WALL){gem(r,new V(x,y+h,zz),.30,.6,0xFAD6A3,time+i);m.box(-.10,.04,-.51,.10,.18,.51,0xFFC083);}else{for(int j=0;j<3;j++)gem(r,new V(x,y+(time*.7+j)%3,zz),.12,.20,0xBDDC80,time+i);}
            }return;
        }
        if(z.type==Ability.BLIND_WAVE){V center=p.add(new V(Math.sin(z.yaw)*z.age*22,0,Math.cos(z.yaw)*z.age*22));for(int i=0;i<7;i++){double a=i*Math.PI/3.5;gem(r,center.add(new V(Math.cos(a)*1.1,Math.sin(a)*1.1,0)),.24,.45,shade(c,.8),-time*4);}return;}
        if(z.type==Ability.TOXIC_DOME||z.type==Ability.POISON_CLOUD)return;
        double y=p.y()+.05;
        if(z.type==Ability.ECLIPSE){for(int i=0;i<24;i++){double a=time*1.3+i*Math.PI/12,rad=z.radius*(.4+i%3*.2);gem(r,new V(p.x()+Math.cos(a)*rad,y+(i%5)*.45,p.z()+Math.sin(a)*rad),.11,.23,shade(c,.6+i%3*.15),a);}return;}
        r.ring(p.x(),y,p.z(),z.radius,.05,c);
        for(int i=0;i<20;i++){double a=i*2.399,radius=z.radius*Math.sqrt((i+.5)/20),x=p.x()+Math.cos(a)*radius,zz=p.z()+Math.sin(a)*radius;
            switch(z.type){
                case SLOW -> {gem(r,new V(x,y,zz),.13,.20+(i%4)*.11,0xBDF7EB,a);tether(r,new V(x,y,zz),p.add(new V(0,.04,0)),.014,0x94C9C9);}
                case ACID -> {r.ring(x,y+.015,zz,.24+.04*Math.sin(time*4+i),.10,0xABC668);if(i%3==0)gem(r,new V(x,y+.12+.07*Math.sin(time*4+i),zz),.055,.07,0xE0EBA3,a);}
                case HEAL_FIRE -> {WeaponModel m=model(r,new V(x,y+.2+.1*Math.sin(time*3+i),zz),a,c);m.box(-.035,0,-.03,.035,.25,.03,0xFFE5B2);m.box(-.10,.09,-.03,.10,.16,.03,0xFFE5B2);}
                case INCENDIARY,FRAG -> gem(r,new V(x,y+.10,zz),.16,.18+.32*Math.abs(Math.sin(time*5+i)),z.type==Ability.HEAL_FIRE?0xFFD69B:0xEFAD6E,a);
                case STIM -> {if(i%2==0){WeaponModel m=model(r,new V(x,y,zz),a,c);m.box(-.13,0,-.3,.13,.04,.1,c);m.face(m.p(-.27,.045,.1),m.p(0,.045,.43),m.p(.27,.045,.1),m.p(0,.045,.16),0xE5E7A9);}}
                default -> { }
            }
        }
    }
    static void device(Renderer r,Abilities.Device d,double time){
        V p=d.position;int c=color(d.type);WeaponModel m=model(r,p,d.yaw,c);
        switch(d.type){
            case BARRIER -> {Box b=d.collider;if(b==null)return;boolean along=b.x2()-b.x1()>b.z2()-b.z1();for(int i=0;i<7;i++){double x=along?b.x1()+.5+i:p.x(),z=along?p.z():b.z1()+.5+i;WeaponModel stone=model(r,new V(x,p.y(),z),along?0:Math.PI/2,c);stone.body(-.47,0,-.50,.47,2.5*Math.min(1,d.age/.25),.5,shade(c,.75+d.hp/2000));gem(r,new V(x,p.y()+2.2,z),.31,.40,0xBDF4DF,i);stone.box(-.04,.3,-.51,.04,2.35,-.50,0xE4FDEB);} }
            case TURRET -> {m.body(-.32,0,-.30,.32,.18,.30,DARK);for(int side:new int[]{-1,1}){m.box(side*.36-.1,0,-.4,side*.36+.1,.08,.4,DARK);m.tube(side*.13,.66,.04,.62,.046,DARK,8);}m.body(-.23,.35,-.23,.23,.78,.19,c);m.box(-.14,.57,.20,.14,.68,.22,0xFFF0AB);m.box(-.045,.15,-.07,.045,.4,.07,0xABAAA1);}
            case HUNTER_BOT -> {m.body(-.25,.1,-.4,.25,.48,.30,c);for(int side:new int[]{-1,1}){m.body(side*.32-.1,.03,-.38,side*.32+.1,.26,.32,DARK);for(int i=0;i<4;i++)m.box(side*.32-.11,.05,-.30+i*.16,side*.32+.11,.12,-.24+i*.16,0x718387);}m.tube(0,.33,.31,.13,.12,0xFBD89B,8);}
            case TRAP -> {for(int i=0;i<4;i++){double a=i*Math.PI/2;WeaponModel jaw=model(r,p,a,c);jaw.body(-.12,.02,.08,.12,.15,.43,DARK);gem(r,p.add(new V(Math.sin(a)*.38,.15,Math.cos(a)*.38)),.08,.19,c,a);}r.ring(p.x(),p.y()+.05,p.z(),.25,.065,c);}
            case SENSOR -> {m.body(-.22,0,-.22,.22,.12,.22,DARK);m.box(-.035,.1,-.035,.035,.8,.035,c);m.tube(0,.64,-.11,.22,.20,0x889AA2,10);m.tube(0,.64,.115,.015,.12,c,10);m.box(-.04,.76,-.04,.04,1.0,.04,c);}
            case LOCKDOWN -> {m.body(-.34,0,-.34,.34,.18,.34,DARK);for(int i=0;i<3;i++){double a=i*2.094;V at=p.add(new V(Math.cos(a)*.25,.3,Math.sin(a)*.25));gem(r,at,.18,.75,c,a);}gem(r,p.add(new V(0,.65,0)),.17,.30,0xFFF3BD,time*3);r.ring(p.x(),p.y()+.04,p.z(),25*d.age/7.2,.06,c);}
            case ANCHOR -> {m.body(-.28,0,-.28,.28,.13,.28,DARK);r.ring(p.x(),p.y()+.15,p.z(),.28,.07,c);gem(r,p.add(new V(0,.45,0)),.15,.35,c,time*2);for(int i=0;i<4;i++){double a=i*Math.PI/2;gem(r,p.add(new V(Math.sin(a)*.45,.1,Math.cos(a)*.45)),.055,.17,0xF8E6B1,a);}}
            case BULWARK -> {m.body(-.28,0,-.18,.28,.2,.18,DARK);for(int i=-2;i<=2;i++){m.body(i*.59-.27,.3,-.06,i*.59+.27,2.18,.06,shade(c,.7));m.box(i*.59-.015,.3,-.068,i*.59+.015,2.18,-.06,0xF9E8B2);}m.box(-1.5,2.2,-.08,1.5,2.26,.08,c);}
            case TRIPWIRE -> {m.body(-.12,-.13,-.08,.12,.13,.08,DARK);gem(r,p,.09,.13,d.age<.45?0xE5CB8A:c,time);if(d.endpoint!=null){WeaponModel end=model(r,d.endpoint,d.yaw,c);end.body(-.12,-.13,-.08,.12,.13,.08,DARK);gem(r,d.endpoint,.09,.13,c,time);tether(r,p,d.endpoint,d.tethered==null?.020:.035,d.tethered==null?0x9EE7EC:0xFFD6A4);if(d.tethered!=null){V mid=p.add(d.endpoint).mul(.5);tether(r,mid,d.tethered.center(),.029,0xF7C17C);r.ring(d.tethered.x,d.tethered.y+.08,d.tethered.z,.47,.05,0xF7C17C);}}}
            case SPYCAM -> {m.body(-.17,-.13,-.08,.17,.13,.12,DARK);m.tube(0,0,.12,.16,.105,0x8295A0,10);m.tube(0,0,.285,.015,.076,0x9AEFFC,10);m.box(-.035,-.25,-.08,.035,-.12,.04,c);for(int i:new int[]{-1,1})m.box(i*.19-.015,-.02,-.04,i*.19+.015,.23,-.01,c);}
            case CAGE -> {m.body(-.22,0,-.22,.22,.08,.22,DARK);r.ring(p.x(),p.y()+.10,p.z(),.18,.07,c);if(d.active){int n=24;for(int i=0;i<n;i++){double a=i*Math.PI*2/n,b=(i+1)*Math.PI*2/n;V p1=p.add(new V(Math.sin(a)*3,.03,Math.cos(a)*3)),p2=p.add(new V(Math.sin(b)*3,.03,Math.cos(b)*3));quad(r.dynamic,p1,p2,p2.add(new V(0,3.2,0)),p1.add(new V(0,3.2,0)),shade(0x8FAFBA,.72+.07*Math.sin(i+time*3)));tether(r,p1,p1.add(new V(0,3.2,0)),.018,0xBDFAF7);}for(int i=0;i<4;i++)r.ring(p.x(),p.y()+.1+i*.85,p.z(),3.015,.018,c);}}
            default -> { }
        }
    }
    static void effect(Renderer r,Abilities.Effect e){
        double t=e.age/e.duration;V p=e.position;int c=color(e.type);WeaponModel m=model(r,p,e.yaw,c);
        switch(e.type){
            case HEAL -> {m.box(-.10,-.4,.12,.10,.4,.2,c);m.box(-.38,-.1,.12,.38,.1,.2,c);}
            case SURGE -> {for(int i=0;i<6;i++){double a=i*Math.PI/3;gem(r,p.add(new V(Math.cos(a)*(1+t),0,Math.sin(a)*(1+t))),.2,.7,c,a);}}
            case SCAN -> {for(int i=0;i<24;i++){double a=i*Math.PI/12;V at=p.add(new V(Math.cos(a)*t*8,Math.sin(t*4)*.3,Math.sin(a)*t*8));tether(r,at,at.add(new V(0,.5,0)),.02,c);}}
            case FOCUS -> {for(int i=0;i<5;i++){double a=i*Math.PI*.4+e.age;gem(r,p.add(new V(Math.cos(a)*1.1,.15,Math.sin(a)*1.1)),.055,.4,c,a);}}
            case DASH,SATCHEL,UPDRAFT -> {for(int i=0;i<8;i++){double a=i*Math.PI/4;V at=p.add(new V(Math.cos(a)*(.5+t),e.type==Ability.UPDRAFT?t*2:0,Math.sin(a)*(.5+t)));tether(r,at,at.add(new V(-Math.sin(e.yaw)*.8,e.type==Ability.UPDRAFT?-1:0,-Math.cos(e.yaw)*.8)),.027,c);}}
            case BLINK,GLOBAL_TELEPORT,ANCHOR -> {for(int i=0;i<12;i++){double a=i*Math.PI/6;gem(r,p.add(new V(Math.cos(a)*(.8+t*.2),Math.sin(a),0)),.08,.16,c,a);}if(e.type==Ability.GLOBAL_TELEPORT)r.ring(p.x(),p.y()-1,p.z(),2+t,.04,c);}
            case RETURN -> {for(int i=0;i<3;i++)gem(r,p.add(new V(0,-.8+i*.35,0)),.35,.28,0xFDD6A5,e.age);}
            case REVIVE -> {for(int side:new int[]{-1,1})for(int i=0;i<5;i++)gem(r,p.add(new V(side*(.3+i*.15),.3-i*.16,0)),.12,.40,c,side*.5);}
            case NETWORK -> {for(int i=0;i<8;i++){double a=i*Math.PI/4;V end=p.add(new V(Math.sin(a)*(1+t*5),.5,Math.cos(a)*(1+t*5)));tether(r,p,end,.017,c);gem(r,end,.12,.17,c,a);}}
            default -> { } // Persistent and thrown skills carry their art in their own simulation object.
        }
    }
    static void draw(Renderer r,Game g){if(g.sentinels.placingWire&&g.sentinels.preview!=null){var w=g.sentinels.preview;tether(r,w.a(),w.b(),.025,0xB8FFDA);gem(r,w.a(),.14,.2,0xB8FFDA,g.visualTime);gem(r,w.b(),.14,.2,0xB8FFDA,g.visualTime);}
        for(var p:g.abilities.projectiles)projectile(r,p,g.visualTime);for(var z:g.abilities.zones)zone(r,z,g.visualTime,g.world);for(var d:g.abilities.devices)device(r,d,g.visualTime);for(var e:g.abilities.effects)effect(r,e);
        for(Actor a:g.actors)if(!a.dead&&a.returnTime>0&&a.returnPoint!=null){gem(r,a.returnPoint.add(new V(0,.12,0)),.5,.18,0xF6C587,g.visualTime);}
        if(g.abilities.pingLife>0){V p=g.abilities.pingPoint;gem(r,p.add(new V(0,1.7,0)),.15,.3,0xECD69F,g.visualTime);r.ring(p.x(),p.y()+.03,p.z(),.7,.07,0xECD69F);}}
    static void specialGun(List<Tri> out,boolean rail,double x,double y,double z,double yaw,double cycle){
        WeaponModel m=new WeaponModel(out,x,y,z,yaw,1,0xEACF8A);m.grip(.23,0x243C4B);m.body(-.06,-.08,.12,.06,.045,rail?1.22:.68,0xD7BE84);
        int count=rail?7:3;for(int i=0;i<count;i++){double at=.35+i*.10;m.body(-.083,-.06,at,.083,.075,at+.033,0x2C4554);m.box(-.06,.076,at,.06,.088,at+.035,0xFFF1BB);}m.tube(0,-.015,rail?1.16:.61,.10,.028,0x1A303F,8);
        if(rail){m.body(-.10,-.16,-.21,.10,-.02,.18,0x2B414E);m.tube(0,.125,.30,.29,.057,0x2B414E,8);m.tube(0,.125,.29,.012,.048,0xBFF5ED,8);}else m.sights(.64);
    }
    static void icon(Graphics2D g,Ability a,double x,double y,double radius,Color color){Assets.icon(g,a,x,y,radius);}
    static void legacyIcon(Graphics2D g,Ability a,double x,double y,double radius,Color color){
        Graphics2D p=(Graphics2D)g.create();p.translate(x,y);p.scale(radius/16,radius/16);p.setColor(color);p.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch(a){
            case DASH -> {path(p,-13,7,-3,0,-13,-7);path(p,0,7,10,0,0,-7);}
            case QUICK_SMOKE -> {p.drawArc(-12,-10,23,20,10,285);path(p,2,-13,11,-9,5,-3);}
            case UPDRAFT -> {path(p,-12,4,0,-9,12,4);path(p,-8,11,0,3,8,11);}
            case FOCUS -> {for(int i=-1;i<=1;i++)path(p,i*9-3,10,i*9,-12,i*9+3,10);}
            case SMOKE -> {p.drawOval(-12,-9,24,18);p.drawArc(-7,-4,14,12,0,180);path(p,-15,11,15,11);}
            case INCENDIARY -> {path(p,0,-14,9,0,7,10,0,14,-8,9,-10,1,-4,5,0,-14);}
            case STIM -> {p.drawOval(-12,-12,24,24);path(p,-7,4,0,-5,7,4);path(p,-6,10,0,2,6,10);}
            case ORBITAL -> {p.drawOval(-13,4,26,9);path(p,0,-14,0,9,-4,4);path(p,-8,-10,-8,0);path(p,8,-10,8,0);}
            case SCAN -> {p.drawArc(-13,-13,26,26,20,290);path(p,0,0,9,-9);p.fillOval(-2,-2,4,4);}
            case FLASH -> {p.drawRect(-5,-7,10,14);for(int i=0;i<8;i++){double angle=i*Math.PI/4;path(p,Math.cos(angle)*10,Math.sin(angle)*10,Math.cos(angle)*15,Math.sin(angle)*15);}}
            case SUPPRESS -> {path(p,-5,13,5,13,5,-3,0,-14,-5,-3,-5,13);path(p,-13,-2,-7,-2);path(p,7,-2,13,-2);}
            case SURGE -> {path(p,-12,-6,0,-13,12,-6,9,7,0,14,-9,7,-12,-6);path(p,-4,-3,4,-3,0,5);}
            case TOXIC_WALL -> {for(int i=-1;i<=1;i++)path(p,i*10-3,12,i*10-3,-10,i*10+3,-5,i*10+3,12);}
            case POISON_CLOUD -> {p.drawOval(-11,-11,22,22);p.fillOval(-6,-4,3,3);p.fillOval(3,-4,3,3);path(p,-4,5,4,5);}
            case ACID -> {path(p,-4,-13,4,-13,4,-4,12,9,8,12,-8,12,-12,9,-4,-4,-4,-13);path(p,-7,6,7,6);}
            case TOXIC_DOME -> {p.drawArc(-14,-8,28,25,0,180);path(p,-14,4,-14,12,14,12,14,4);p.drawOval(-4,-3,8,8);}
            case BLINK -> {path(p,-12,-12,-3,-12,-3,12,-12,12);path(p,2,0,14,0,8,-6);}
            case BLIND_WAVE -> {path(p,-14,0,-7,-7,0,0,7,-7,14,0,7,7,0,0,-7,7,-14,0);}
            case ECLIPSE -> {p.drawArc(-11,-12,23,24,60,250);path(p,4,-10,1,-3,-7,0,1,3,4,10,7,3,14,0,7,-3,4,-10);}
            case GLOBAL_TELEPORT -> {p.drawOval(-13,-13,26,26);p.drawOval(-6,-13,12,26);path(p,-13,0,13,0);path(p,0,-13,0,13);}
            case CURVEFLASH -> {p.drawArc(-13,-12,24,24,-20,220);path(p,7,9,13,9,13,2);p.fillOval(-5,-2,6,6);}
            case HEAL_FIRE -> {path(p,0,-14,10,1,7,12,-7,12,-10,1,0,-14);path(p,-5,3,5,3);path(p,0,-2,0,8);}
            case FIRE_WALL -> {for(int i=-1;i<=1;i++)path(p,i*9-3,12,i*9-4,-1,i*9,-12,i*9+4,1,i*9+3,12);}
            case RETURN -> {p.drawArc(-12,-12,24,24,35,300);path(p,2,-14,12,-9,4,-4);p.drawOval(-3,-3,6,6);}
            case SATCHEL -> {p.drawRect(-10,-8,20,16);path(p,-5,-13,-5,-8,5,-8,5,-13);path(p,-5,13,0,8,5,13);}
            case FRAG -> {p.drawOval(-8,-9,16,20);path(p,-3,-14,6,-14,8,-9);for(int i=-1;i<=1;i++)path(p,-7,i*5,7,i*5);}
            case HUNTER_BOT -> {p.drawRect(-11,-7,22,14);p.drawOval(-5,-4,10,8);path(p,-14,-9,-14,9);path(p,14,-9,14,9);}
            case ROCKET -> {path(p,-4,11,-4,-3,0,-14,4,-3,4,11,-4,11);path(p,-4,1,-11,10,-4,8);path(p,4,1,11,10,4,8);}
            case BARRIER -> {for(int i=-1;i<=1;i++)path(p,i*10-4,12,i*10-4,-8,i*10,-13,i*10+4,-8,i*10+4,12);}
            case SLOW -> {for(int i=0;i<3;i++){p.rotate(Math.PI/3);path(p,0,-14,0,14);path(p,-4,-9,0,-5,4,-9);}}
            case HEAL -> {path(p,-4,-13,4,-13,4,-4,13,-4,13,4,4,4,4,13,-4,13,-4,4,-13,4,-13,-4,-4,-4,-4,-13);}
            case REVIVE -> {path(p,0,12,0,-8,-4,-3);path(p,-4,5,-13,-8,-12,5,-4,10);path(p,4,5,13,-8,12,5,4,10);}
            case TURRET -> {p.drawRect(-9,-8,18,11);path(p,-4,3,-10,12);path(p,4,3,10,12);path(p,-3,-8,-3,-14);path(p,3,-8,3,-14);}
            case TRAP -> {path(p,-14,-8,-7,8,7,8,14,-8);path(p,-7,8,-5,-3,0,6,5,-3,7,8);}
            case SENSOR -> {p.drawOval(-4,-6,8,12);path(p,0,6,0,13,-5,13,5,13);p.drawArc(-10,-11,20,22,-65,130);}
            case LOCKDOWN -> {path(p,-12,10,-12,-6,0,-13,12,-6,12,10,-12,10);p.drawRect(-5,-2,10,10);p.drawArc(-4,-6,8,9,0,180);}
            case VERDICT -> {path(p,-12,-7,13,-7,13,-1,1,-1,-3,12,-9,10,-6,-1,-12,-1,-12,-7);path(p,-2,-11,7,-11);}
            case BULWARK -> {path(p,-13,-10,13,-10,11,8,0,14,-11,8,-13,-10);path(p,-7,-4,7,-4,6,5,0,8,-6,5,-7,-4);}
            case ANCHOR -> {p.drawOval(-4,-14,8,8);path(p,0,-6,0,12);path(p,-12,0,-10,7,0,12,10,7,12,0);}
            case RAIL -> {path(p,-14,-5,14,-5,14,1,-5,1,-9,8,-13,8,-12,-5);path(p,0,-5,0,-10,7,-10,7,-5);path(p,5,1,5,8);}
            case TRIPWIRE -> {p.drawRect(-14,-6,5,12);p.drawRect(9,-6,5,12);path(p,-9,0,9,0);path(p,-2,-5,2,5);}
            case CAGE -> {p.drawOval(-12,-12,24,8);p.drawArc(-12,7,24,8,180,180);for(int i=-1;i<=1;i++)path(p,i*11,-8,i*11,11);}
            case SPYCAM -> {p.drawRect(-12,-8,18,13);path(p,6,-4,14,-8,14,6,6,2);path(p,-4,5,-4,13,-11,13);}
            case NETWORK -> {p.drawOval(-4,-4,8,8);for(int i=0;i<4;i++){double angle=i*Math.PI/2;double xx=Math.cos(angle)*12,yy=Math.sin(angle)*12;path(p,Math.cos(angle)*4,Math.sin(angle)*4,xx,yy);p.draw(new Rectangle2D.Double(xx-2,yy-2,4,4));}}
        }p.dispose();
    }
    static void path(Graphics2D g,double... xy){Path2D p=new Path2D.Double();p.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);g.draw(p);}
}
