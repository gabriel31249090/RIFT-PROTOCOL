package rift;

import java.util.*;
import static rift.World.*;
import static rift.Game.*;

/** Articulated low-poly rig: feet, knees, hips, spine, elbows and independently aimed head. */
final class CharacterModel {
    static final int[] SKIN={0xCEB8A1,0xAF9073,0x7B8C9A,0xAE9579,0x84909C,0xAE8064,0xCFAC8C,0xB28D76,0xC1A082,0xAC8063,0xD1B299};
    final List<Tri> out;final Actor actor;final double yaw,base,lean,sinYaw,cosYaw,sinLean,cosLean;final int coat,dark=0x263844,skin;
    CharacterModel(List<Tri> out,Actor a,double base,double lean){this.out=out;actor=a;this.base=base;this.lean=lean;yaw=a.bodyYaw;sinYaw=Math.sin(yaw);cosYaw=Math.cos(yaw);sinLean=Math.sin(lean);cosLean=Math.cos(lean);coat=Agent.values()[a.agentIndex].color;skin=SKIN[a.agentIndex];}
    static void animate(Actor a,double dt){
        double k=1-Math.exp(-dt*12);a.animSpeed+=(Math.min(6,a.moveSpeed)-a.animSpeed)*k;a.animCrouch+=((a.crouch?1:0)-a.animCrouch)*k;
        double d=Math.atan2(Math.sin(a.yaw-a.bodyYaw),Math.cos(a.yaw-a.bodyYaw));a.animTurn+=(Settings.clamp(d,-1,1)-a.animTurn)*k;a.bodyYaw+=d*(1-Math.exp(-dt*(a.moveSpeed>.4?10:6)));a.lean+=(Settings.clamp(a.moveSpeed/7,0,.7)-a.lean)*k;
    }
    V p(V p){double y=p.y(),z=p.z(),c=cosLean,s=sinLean;double yy=(y-.85)*c-z*s+.85,zz=(y-.85)*s+z*c;return new V(actor.x+p.x()*cosYaw+zz*sinYaw,base+yy,actor.z-p.x()*sinYaw+zz*cosYaw);}
    void tri(V a,V b,V c,int color){out.add(new Tri(p(a),p(b),p(c),color));}
    void bone(V a,V b,double ra,double rb,int color,int sides){
        V dir=b.sub(a).unit(),side=new V(dir.z(),0,-dir.x());if(side.length()<.05)side=new V(1,0,0);side=side.unit();V up=new V(dir.y()*side.z()-dir.z()*side.y(),dir.z()*side.x()-dir.x()*side.z(),dir.x()*side.y()-dir.y()*side.x());
        for(int i=0;i<sides;i++){double t=i*Math.PI*2/sides,tt=(i+1)*Math.PI*2/sides;V n=side.mul(Math.cos(t)).add(up.mul(Math.sin(t))),nn=side.mul(Math.cos(tt)).add(up.mul(Math.sin(tt)));V aa=a.add(n.mul(ra)),ab=a.add(nn.mul(ra)),bb=b.add(nn.mul(rb)),ba=b.add(n.mul(rb));int shade=World.shade(color,.70+.28*Math.max(0,Math.sin(t+.5)));tri(aa,ab,bb,shade);tri(aa,bb,ba,shade);tri(a,ab,aa,World.shade(color,.85));tri(b,ba,bb,World.shade(color,1.1));}
    }
    void box(double x,double y,double z,double xx,double yy,double zz,int color){
        // Share eight transformed corners, instead of transforming 36 triangle vertices.
        V a=p(new V(x,y,z)),b=p(new V(xx,y,z)),c=p(new V(x,yy,z)),d=p(new V(xx,yy,z));
        V e=p(new V(x,y,zz)),f=p(new V(xx,y,zz)),h=p(new V(x,yy,zz)),i=p(new V(xx,yy,zz));
        quad(out,a,b,d,c,shade(color,.86));quad(out,e,h,i,f,shade(color,.71));
        quad(out,a,c,h,e,shade(color,.78));quad(out,b,f,i,d,shade(color,.97));
        quad(out,c,d,i,h,shade(color,1.13));quad(out,a,e,f,b,shade(color,.55));
    }
    void oval(V center,double rx,double ry,double rz,int color,int sides){
        for(int j=0;j<4;j++){double lat=-Math.PI/2+j*Math.PI/4,lat2=lat+Math.PI/4;for(int i=0;i<sides;i++){double t=i*Math.PI*2/sides,t2=(i+1)*Math.PI*2/sides;V a=ellipsoid(center,rx,ry,rz,lat,t),b=ellipsoid(center,rx,ry,rz,lat2,t),c=ellipsoid(center,rx,ry,rz,lat2,t2),d=ellipsoid(center,rx,ry,rz,lat,t2);int col=World.shade(color,.80+.18*Math.sin(t+.8)+.12*Math.sin(lat2));tri(a,b,c,col);tri(a,c,d,col);}}
    }
    V ellipsoid(V c,double rx,double ry,double rz,double p,double t){return c.add(new V(rx*Math.cos(p)*Math.cos(t),ry*Math.sin(p),rz*Math.cos(p)*Math.sin(t)));}
    static void add(List<Tri> out,Actor a,double time,boolean detailed){
        int start=out.size();double speed=Math.min(1,a.animSpeed/4.5),cycle=a.walk*3.9,duck=a.animCrouch;
        double breathe=Math.sin(time*2.2+a.id)*.008,bob=Math.cos(cycle*2)*.020*speed*(1-duck),base=a.y+breathe+bob;
        CharacterModel m=new CharacterModel(out,a,base, a.dead?0:a.lean*.1+duck*.15);int sides=detailed?6:4;
        double hip=.91-duck*.27,torso=1.32-duck*.38,head=1.65-duck*.46;
        // Each foot plants during its stance phase; the swing phase lifts the knee.
        for(int side=-1;side<=1;side+=2){
            double phase=cycle+(side==1?0:Math.PI),stride=Math.sin(phase)*.34*speed,lift=Math.max(0,Math.cos(phase))*.19*speed;
            V h=new V(side*.135,hip,-duck*.08),foot=new V(side*.15,.10+lift,stride+duck*.12);
            if(!a.grounded)foot=new V(side*.18,.32+(side==1?.15:0),side*.12-.12);
            double kneeY=(h.y()+foot.y())*.52;V knee=new V(side*.145,kneeY,(h.z()+foot.z())*.5+.18+duck*.2);
            m.bone(h,knee,.112,.098,m.dark,sides);m.bone(knee,foot,.096,.071,m.coat,sides);if(detailed)m.oval(knee,.105,.108,.105,World.shade(m.coat,.7),6);
            m.box(foot.x()-.085,foot.y()-.1,foot.z()-.1,foot.x()+.085,foot.y()+.035,foot.z()+.22,m.dark);
        }
        m.bone(new V(0,hip-.06,0),new V(0,hip+.18,0),.235,.21,m.dark,8);
        // Tapered chest and shoulder armor, instead of a rigid rectangular torso.
        m.bone(new V(0,hip+.08,0),new V(0,torso,.015),.20,.28,m.coat,8);
        m.box(-.175,hip+.14,.18,.175,torso-.02,.23,0x304955);m.box(-.21,hip-.02,-.15,.21,hip+.055,.19,0xAFB6A0);
        m.bone(new V(0,torso-.015,0),new V(0,head-.13,0),.073,.065,m.skin,sides);
        if(detailed){
            m.box(-.19,hip+.12,.205,-.11,torso-.08,.25,World.shade(m.coat,.7));
            m.box(.10,hip+.15,.205,.17,torso-.15,.26,0xA2B0A2);
            for(int i=0;i<3;i++)m.box(-.11+i*.075,hip+.16,.228,-.06+i*.075,hip+.30,.27,0x7B8B87);
        }
        if(a.agentIndex==9){m.box(-.23,hip+.12,-.17,.23,torso-.08,.17,0x283B48);m.box(-.035,hip+.18,.176,.035,torso-.10,.20,0xE9C879);}
        int headStart=out.size();m.oval(new V(0,head,0),.155,.205,.153,m.skin,detailed?8:6);
        m.box(-.12,head-.025,.132,.12,head+.005,.164,0x253E49);
        if(detailed){m.oval(new V(0,head-.065,.145),.031,.046,.039,m.skin,6);m.box(-.058,head-.117,.121,.058,head-.108,.145,World.shade(m.skin,.60));}
        int ai=a.agentIndex;
        if(ai==9){m.box(-.17,head+.06,.08,.17,head+.10,.17,0xDEC275);}
        if(ai==10){m.oval(new V(0,head+.18,0),.28,.045,.24,0x6C8998,8);m.box(-.15,head-.04,.13,.15,head+.045,.18,0x273E50);m.box(-.06,head-.005,.185,.06,head+.018,.2,0x83E7F2);}
        if(ai==0){m.bone(new V(0,head+.12,-.01),new V(.025,head+.28,-.11),.15,.035,0xDFEDE3,6);m.box(-.145,head-.02,.144,.145,head+.038,.17,0xA3E9DE);}
        else if(ai==1||ai==4){m.bone(new V(0,head-.20,-.12),new V(0,head+.2,-.08),.22,.19,World.shade(m.coat,.65),8);m.box(-.105,head-.10,.15,.105,head-.035,.19,0x334B58);}
        else if(ai==2){m.box(-.17,head+.05,-.15,.17,head+.19,.15,m.dark);m.box(-.15,head-.01,.15,.15,head+.085,.17,0xC5C2FF);}
        else if(ai==3){m.bone(new V(-.18,torso-.27,-.20),new V(-.18,torso+.04,-.20),.09,.09,0x789643,6);m.bone(new V(.18,torso-.27,-.20),new V(.18,torso+.04,-.20),.09,.09,0x789643,6);m.box(-.11,head-.16,.12,.11,head-.055,.22,m.dark);}
        else if(ai==5){m.bone(new V(0,head+.11,-.01),new V(.01,head+.29,-.035),.15,.09,0x483D33,6);m.box(-.21,torso-.08,.16,-.13,torso+.08,.22,0xF3C584);}
        else if(ai==6){m.box(-.17,head+.075,-.17,.17,head+.2,.19,0xB67853);m.box(-.15,head+.015,.15,.15,head+.07,.2,0xCFEDE2);}
        else if(ai==7){m.bone(new V(0,head+.09,-.1),new V(0,head+.21,-.1),.18,.14,0xE0E7D7,8);m.bone(new V(0,head+.02,-.18),new V(.05,torso-.1,-.28),.055,.018,0x52695F,6);}
        else if(ai==9){m.bone(new V(0,head+.11,-.04),new V(.01,head+.24,-.05),.15,.09,0x463F36,6);m.box(-.16,head+.02,.14,.16,head+.04,.18,0xE4CF8C);}
        else if(ai==10){m.box(-.11,head-.16,.13,.11,head-.02,.19,0x527184);}
        else {m.box(-.18,head+.08,-.17,.18,head+.2,.18,0xCFB268);m.box(-.17,head+.07,.13,.17,head+.11,.23,0xE8D69B);m.box(-.13,head-.025,.143,.13,head+.026,.176,0x374D56);}
        // Head follows aim while the body eases into turns.
        double turn=Math.atan2(Math.sin(a.yaw-a.bodyYaw),Math.cos(a.yaw-a.bodyYaw));V pivot=m.p(new V(0,head,0));
        for(int i=headStart;i<out.size();i++){Tri t=out.get(i);out.set(i,new Tri(rotateHead(t.a(),pivot,turn,a.pitch),rotateHead(t.b(),pivot,turn,a.pitch),rotateHead(t.c(),pivot,turn,a.pitch),t.color()));}
        if(ai==1||ai==4){
            m.bone(new V(0,torso-.02,-.09),new V(0,hip-.27,-.14),.245,.30,World.shade(m.coat,ai==4?.52:.70),8);
            m.box(-.16,hip+.1,.195,.16,torso-.05,.24,0x334B57);
        }
        if(ai==2){m.oval(new V(0,torso-.14,.242),.10,.10,.03,0xC4C5FE,6);m.box(-.29,torso-.08,-.18,-.23,torso+.03,.13,0xCBD1EC);}
        if(ai==3){for(int side=-1;side<=1;side+=2)m.bone(new V(side*.22,hip+.16,-.22),new V(side*.22,torso-.04,-.22),.10,.10,0x6C873F,6);}
        if(ai==5){m.box(-.19,hip+.10,.25,-.10,torso-.07,.28,0xF0C689);m.box(.15,torso-.11,-.13,.30,torso+.04,.13,0xC58858);}
        if(ai==6){for(int i=0;i<3;i++)m.bone(new V(-.16+i*.12,torso-.16-i*.065,.25),new V(-.16+i*.12,torso-.28-i*.065,.25),.042,.042,0xDBBA82,6);}
        if(ai==7){m.box(-.235,hip-.20,-.19,-.15,torso-.07,.10,World.shade(m.coat,.85));m.box(.15,hip-.20,-.19,.235,torso-.07,.10,World.shade(m.coat,.85));m.oval(new V(.27,torso-.01,.01),.09,.13,.12,0xC7F5E9,6);}
        if(ai==8){m.box(.19,hip-.05,-.02,.31,hip+.14,.18,0xBEA779);m.bone(new V(-.21,torso-.16,-.16),new V(-.25,torso+.27,-.16),.021,.012,0x6D817C,4);}
        double reload=a.gun().reload>0?1-a.gun().reload/a.gun().reloadTotal:0,reach=a.gun().reload>0?Math.sin(reload*Math.PI):0;
        double shot=a.shotGlow>0?.045:0,armSwing=Math.sin(cycle)*speed*.065;
        V grip=new V(.07,torso-.21+shot,.37-shot),support=new V(-.04-reach*.19,torso-.24-reach*.32,.55-reach*.30);
        if(a.flash>0){support=new V(-.14,head-.08,.26);grip=new V(.17,torso-.35,.22);}
        for(int side=-1;side<=1;side+=2){
            V shoulder=new V(side*.255,torso-.02,armSwing*side),hand=side==1?grip:support,elbow=new V(side*.30,torso-.29-reach*(side==-1?.12:0),.10+armSwing*side);
            m.bone(shoulder,elbow,.115,.08,m.coat,sides);m.bone(elbow,hand,.08,.064,m.dark,sides);if(detailed)m.oval(hand,.075,.075,.09,0x455763,6);
            if(side==1)m.bone(shoulder.add(elbow.sub(shoulder).mul(.35)),shoulder.add(elbow.sub(shoulder).mul(.55)),.113,.105,a.team==0?0xADE8CE:0xE78275,sides);
        }
        double gy=grip.y(),gz=grip.z(),end=a.gun().kind.sidearm()?.37:.76;
        if(a.melee()){m.bone(new V(.045,gy+.04,gz),new V(.045,gy+.04,gz+.43),.037,.002,0xAFCBC6,4);}else {
        m.box(-.02,gy-.015,gz,.11,gy+.085,gz+end-.12,m.dark);m.box(.005,gy+.075,gz+.08,.09,gy+.12,gz+end-.17,a.gun().kind.color);m.bone(new V(.045,gy+.04,gz+end-.14),new V(.045,gy+.04,gz+end),.026,.026,0x839798,6);
        if(!a.gun().kind.sidearm())m.box(.012,gy-.17-reach*.15,gz+.16,.09,gy,gz+.28,0x354652);
        if(a.gun().kind.scoped())m.bone(new V(.045,gy+.17,gz+.12),new V(.045,gy+.17,gz+.40),.047,.047,0x1C303D,6);
        if(a.shotGlow>0)m.bone(new V(.045,gy+.04,gz+end),new V(.045,gy+.04,gz+end+.15),.11,.008,0xFFE2A2,4);
        }
        if(a.carrier)m.box(-.15,hip+.1,-.31,.15,torso-.1,-.20,0xBCAA85);
        // Counter-rotation of hips and shoulders gives stride weight while preserving foot contact.
        if(!a.dead&&speed>.02){double twist=Math.sin(cycle)*speed*.07;for(int i=start;i<out.size();i++){Tri t=out.get(i);out.set(i,t.at(stride(t.a(),a,twist),stride(t.b(),a,twist),stride(t.c(),a,twist)));}}
        if(a.dead){double t=Smoke.smooth(a.deathAge/.85),angle=t*Math.PI*.49;for(int i=start;i<out.size();i++){Tri tr=out.get(i);out.set(i,tr.at(fall(tr.a(),a,angle,t),fall(tr.b(),a,angle,t),fall(tr.c(),a,angle,t)));}}
    }
    static V rotateHead(V v,V pivot,double yaw,double pitch){V p=v.sub(pivot);double x=p.x()*Math.cos(yaw)+p.z()*Math.sin(yaw),z=-p.x()*Math.sin(yaw)+p.z()*Math.cos(yaw);return pivot.add(new V(x,p.y()*Math.cos(pitch)-z*Math.sin(pitch)*.3,z*Math.cos(pitch)+p.y()*Math.sin(pitch)*.3));}
    static V stride(V v,Actor a,double angle){double weight=Settings.clamp((v.y()-a.y-.65)/.8,0,1),x=v.x()-a.x,z=v.z()-a.z,t=angle*weight;return new V(a.x+x*Math.cos(t)+z*Math.sin(t),v.y(),a.z-x*Math.sin(t)+z*Math.cos(t));}
    static V fall(V p,Actor a,double angle,double t){V q=p.sub(new V(a.x,a.y,a.z));double sign=a.id%2==0?1:-1;return new V(a.x+q.x()*Math.cos(angle)+q.y()*Math.sin(angle)*sign,a.y+.18*t+Math.max(-.14,-q.x()*Math.sin(angle)*sign+q.y()*Math.cos(angle)),a.z+q.z()-t*.12);}
}
