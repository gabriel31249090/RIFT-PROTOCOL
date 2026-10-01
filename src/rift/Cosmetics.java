package rift;

import java.util.List;
import static rift.World.*;

/** Original, free cosmetics. Materials never change combat statistics. */
final class Cosmetics {
    enum Skin {
        STANDARD("PADRÃO",0x293943,0x15232D,0xB5C6C8,0xE2C99F),
        CARBON("CARBONO",0x29303B,0x10151F,0x576173,0xF0B859),
        AURORA("AURORA",0xD7E6DC,0x355C64,0x82CDBE,0xEFF6DD),
        PULSE("PULSO",0x353049,0x161D34,0x9573D1,0x54F1D8),
        EMBER("BRASA",0x713D36,0x272C35,0xC96B48,0xFFD392),
        ROYAL("REGENTE",0x2D4660,0x14273D,0xD6B96C,0xF4E9BE);
        final String label;final int metal,dark,trim,accent;
        Skin(String label,int metal,int dark,int trim,int accent){this.label=label;this.metal=metal;this.dark=dark;this.trim=trim;this.accent=accent;}
    }
    enum Charm {
        NONE("SEM PINGENTE",0),CRYSTAL("CRISTAL",0x84EBD6),DICE("DADO",0xE9C384),
        BOT("MINIBOT",0xA1B8EC),STAR("ESTRELA",0xF3D982),CORE("NÚCLEO",0xF19783);
        final String label;final int color;Charm(String label,int color){this.label=label;this.color=color;}
    }
    enum Melee {
        EDGE("FIO","Faca de combate / lâmina reta"),HOOK("ARCO","Karambit / lâmina curva"),AXE("RUPTOR","Machadinha / cabeça de aço");
        final String label,detail;Melee(String label,String detail){this.label=label;this.detail=detail;}
    }
    static void decorate(WeaponModel m,Game.Weapon w,Skin s){
        if(s==Skin.STANDARD)return;
        double end=w.sidearm()?.48:.79;
        // Raised, beveled inlays on both faces: finishes remain visible in either view.
        for(int side:new int[]{-1,1})for(int i=0;i<4;i++){
            double x=side*(w.sidearm()?.058:.073),z=.18+i*(end-.18)/4;
            m.box(x-.002,-.037,z,x+.002,-.026,z+.043,s.accent);
            if(s==Skin.CARBON)m.box(x-.002,-.07,z+.016,x+.002,-.051,z+.034,0x87909A);
            if(s==Skin.ROYAL)m.box(x-.003,-.08,z,x+.003,-.016,z+.007,s.trim);
        }
        m.box(-.025,.028,.35,.025,.034,end,s.accent);
    }
    static void charm(WeaponModel m,Charm charm,double angle,double twist){
        if(charm==Charm.NONE)return;
        double ax=-.095,ay=-.03,az=.33,len=.19,dx=Math.sin(angle)*len,dy=-Math.cos(angle)*len,dz=Math.sin(twist)*.055;
        for(int i=0;i<5;i++){double t=i/5.;double x=ax+dx*t,y=ay+dy*t,z=az+dz*t;m.box(x-.006,y-.022,z-.006,x+.006,y+.006,z+.006,0xCFCCC0);}
        double x=ax+dx,y=ay+dy-.018,z=az+dz;int c=charm.color;
        switch(charm){
            case CRYSTAL,CORE -> {double r=charm==Charm.CORE?.051:.038;V top=m.p(x,y+.027,z),bottom=m.p(x,y-.100,z);
                for(int i=0;i<6;i++){double a=i*Math.PI/3,b=(i+1)*Math.PI/3;V p=m.p(x+Math.cos(a)*r,y-.026,z+Math.sin(a)*r),q=m.p(x+Math.cos(b)*r,y-.026,z+Math.sin(b)*r);m.out.add(new Tri(top,p,q,shade(c,.78+i*.055)));m.out.add(new Tri(bottom,q,p,shade(c,.70+i*.05)));}
            }
            case DICE -> {m.body(x-.039,y-.076,z-.03,x+.039,y+.002,z+.038,c);for(int i=0;i<3;i++)m.box(x-.023+i*.018,y-.057+i*.02,z-.033,x-.014+i*.018,y-.048+i*.02,z-.030,0x2D414C);}
            case BOT -> {m.body(x-.045,y-.07,z-.025,x+.045,y+.003,z+.03,c);m.box(x-.036,y-.027,z-.028,x+.036,y-.009,z-.025,0x1E3E50);for(int side:new int[]{-1,1})m.box(x+side*.021-.007,y-.024,z-.031,x+side*.021+.007,y-.015,z-.028,0x9CFFE6);m.box(x-.006,y+.005,z-.006,x+.006,y+.043,z+.006,c);}
            case STAR -> {for(int i=0;i<10;i++){double a=i*Math.PI/5-Math.PI/2,b=(i+1)*Math.PI/5-Math.PI/2;double r=i%2==0?.065:.028,s=(i+1)%2==0?.065:.028;V p=m.p(x+Math.cos(a)*r,y-.03+Math.sin(a)*r,z),q=m.p(x+Math.cos(b)*s,y-.03+Math.sin(b)*s,z);m.out.add(new Tri(m.p(x,y-.03,z-.018),p,q,shade(c,.8+(i%3)*.12)));m.out.add(new Tri(m.p(x,y-.03,z+.018),q,p,shade(c,.7+(i%3)*.12)));}}
            default -> { }
        }
    }
    static void melee(List<Tri> out,Melee kind,Skin skin,double x,double y,double z,double yaw,double size){
        WeaponModel m=new WeaponModel(out,x,y,z,yaw,size,skin);
        m.body(-.036,-.03,.09,.036,.035,.38,skin.dark);
        for(int i=0;i<6;i++)m.box(-.038,-.031,.12+i*.035,.038,.036,.129+i*.035,skin.metal);
        m.body(-.072,-.04,.36,.072,.044,.40,skin.trim);
        if(kind==Melee.EDGE){blade(m,new double[][]{{-.045,.40},{.055,.40},{.046,.76},{0,.91},{-.043,.70}},skin);m.box(-.009,.021,.44,.009,.027,.70,skin.accent);}
        else if(kind==Melee.HOOK){blade(m,new double[][]{{-.045,.39},{.047,.40},{.067,.56},{.03,.71},{-.065,.79},{-.15,.79},{-.06,.69},{-.019,.57}},skin);m.tube(0,0,.05,.05,.057,skin.trim,12);m.tube(0,0,.042,.065,.038,skin.dark,12);}
        else {m.body(-.030,-.03,.38,.030,.03,.69,skin.metal);blade(m,new double[][]{{-.025,.53},{.09,.55},{.18,.47},{.27,.44},{.29,.70},{.23,.78},{.07,.68},{-.025,.69}},skin);m.box(-.023,-.02,.46,.023,.035,.56,skin.accent);}
    }
    static void blade(WeaponModel m,double[][] outline,Skin skin){
        double cx=0,cz=0;for(double[] p:outline){cx+=p[0];cz+=p[1];}cx/=outline.length;cz/=outline.length;
        for(int i=0;i<outline.length;i++){double[] a=outline[i],b=outline[(i+1)%outline.length];V p=m.p(a[0],0,a[1]),q=m.p(b[0],0,b[1]);m.out.add(new Tri(p,q,m.p(cx,.029,cz),i%2==0?skin.trim:shade(skin.trim,1.22)));m.out.add(new Tri(q,p,m.p(cx,-.023,cz),skin.metal));}
    }
}
