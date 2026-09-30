package rift;

import java.util.List;
import static rift.World.*;
import static rift.Game.*;

/** Original procedural gun meshes, shared by first-person view and the arsenal viewer. */
final class WeaponModel {
    final List<Tri> out;final double ox,oy,oz,yaw,size;final int metal,dark,trim;
    WeaponModel(List<Tri> out,double x,double y,double z,double yaw,double size,int trim){this.out=out;ox=x;oy=y;oz=z;this.yaw=yaw;this.size=size;this.trim=trim;metal=0x293943;dark=0x15232D;}
    WeaponModel(List<Tri> out,double x,double y,double z,double yaw,double size,Cosmetics.Skin skin){this.out=out;ox=x;oy=y;oz=z;this.yaw=yaw;this.size=size;trim=skin.trim;metal=skin.metal;dark=skin.dark;}
    static void add(List<Tri> out,Weapon w,double x,double y,double z,double yaw,double size,double magazine,double cycle){
        WeaponModel m=new WeaponModel(out,x,y,z,yaw,size,w.color);m.gun(w,magazine,cycle);
    }
    static void add(List<Tri> out,Weapon w,double x,double y,double z,double yaw,double size,double magazine,double cycle,Cosmetics.Skin skin,Cosmetics.Charm charm,double swing,double twist){
        WeaponModel m=skin==Cosmetics.Skin.STANDARD?new WeaponModel(out,x,y,z,yaw,size,w.color):new WeaponModel(out,x,y,z,yaw,size,skin);m.gun(w,magazine,cycle);Cosmetics.decorate(m,w,skin);Cosmetics.charm(m,charm,swing,twist);
    }
    V p(double x,double y,double z){return new V(ox+size*(x*Math.cos(yaw)+z*Math.sin(yaw)),oy+y*size,oz+size*(-x*Math.sin(yaw)+z*Math.cos(yaw)));}
    int material(int color){return color==dark?Assets.RUBBER:color==0x8A6851||color==0xB38A65?Assets.WOOD:color==0xC6A064||color==0xC79B65?Assets.BRASS:Assets.STEEL;}
    void textured(int start,int color){for(int i=start;i<out.size();i++){Tri t=out.get(i);Tri local=new Tri(local(t.a()),local(t.b()),local(t.c()),t.color());Tri tex=World.texture(local,material(color),3.5);out.set(i,new Tri(t.a(),t.b(),t.c(),t.color(),tex.material(),tex.ua(),tex.va(),tex.ub(),tex.vb(),tex.uc(),tex.vc()));}}
    V local(V v){double x=(v.x()-ox)/size,z=(v.z()-oz)/size;return new V(x*Math.cos(yaw)-z*Math.sin(yaw),(v.y()-oy)/size,x*Math.sin(yaw)+z*Math.cos(yaw));}
    void face(V a,V b,V c,V d,int color){int start=out.size();quad(out,a,b,c,d,color);textured(start,color);}
    void box(double x,double y,double z,double xx,double yy,double zz,int c){int start=out.size();addBox(out,x*size,y*size,z*size,xx*size,yy*size,zz*size,c,ox,oy,oz,yaw);textured(start,c);}
    void body(double x,double y,double z,double xx,double yy,double zz,int color){
        double b=Math.min(xx-x,yy-y)*.19;double[] px={x+b,xx-b,xx,xx,xx-b,x+b,x,x},py={y,y,y+b,yy-b,yy,yy,yy-b,y+b};
        V[] a=new V[8],c=new V[8];for(int i=0;i<8;i++){a[i]=p(px[i],py[i],z);c[i]=p(px[i],py[i],zz);}
        for(int i=0;i<8;i++){int j=(i+1)%8;face(a[i],a[j],c[j],c[i],shade(color,new double[]{.53,.7,.92,1.12,1.18,.99,.74,.61}[i]));}
        for(int i=1;i<7;i++){out.add(new Tri(a[0],a[i+1],a[i],shade(color,.82)));out.add(new Tri(c[0],c[i],c[i+1],shade(color,.7)));}
    }
    void tube(double x,double y,double z,double length,double radius,int color,int sides){
        for(int i=0;i<sides;i++){double a=i*Math.PI*2/sides,b=(i+1)*Math.PI*2/sides;V p1=p(x+Math.cos(a)*radius,y+Math.sin(a)*radius,z),p2=p(x+Math.cos(b)*radius,y+Math.sin(b)*radius,z),q1=p(x+Math.cos(a)*radius,y+Math.sin(a)*radius,z+length),q2=p(x+Math.cos(b)*radius,y+Math.sin(b)*radius,z+length);
            face(p1,p2,q2,q1,shade(color,.8+.23*Math.sin(a)));out.add(new Tri(p(x,y,z),p2,p1,shade(color,.75)));out.add(new Tri(p(x,y,z+length),q1,q2,shade(color,.56)));
        }
    }
    void grip(double z,int color){body(-.047,-.27,z-.035,.047,-.095,z+.10,color);for(int i=0;i<4;i++)box(-.049,-.245+i*.028,z-.007,.049,-.235+i*.028,z+.075,shade(color,.75));}
    void sights(double end){body(-.034,.018,.21,.034,.065,.26,dark);box(-.017,.037,.205,.017,.051,.22,0x8BDCCB);box(-.015,.018,end-.02,.015,.068,end+.005,dark);box(-.008,.048,end-.027,.008,.059,end-.02,0xF2D5A4);}
    void gun(Weapon w,double magazine,double cycle){
        double slide=Math.sin(Math.PI*Math.min(1,cycle*5))*.067;
        if(w.sidearm()){
            grip(.24,metal);body(-.057,-.12,.09,.057,-.063,.53,metal);box(-.034,-.204-magazine,.235,.034,-.105-magazine,.32,dark);
            if(w==Weapon.TALON){
                double swing=magazine*.45;tube(-swing,-.045,.22,.18,.077,trim,10);for(int i=0;i<6;i++){double a=i*Math.PI/3;tube(-swing+Math.cos(a)*.048,-.045+Math.sin(a)*.048,.215,.018,.011,0xC79B65,6);}
                body(-.044,-.047,.39,.044,.026,.74,trim);tube(0,-.005,.73,.08,.029,dark,10);box(-.03,-.09,.45,.03,-.047,.73,metal);sights(.72);
            }else if(w==Weapon.DART){
                for(double x:new double[]{-.045,.045}){tube(x,-.01,.21,.40,.039,trim,10);tube(x,-.01,.607,.01,.027,dark,10);}body(-.086,-.085,.15,.086,.038,.32,metal);body(-.076,-.107,.36,.076,-.055,.55,0x8A6851);sights(.57);
            }else{
                body(-.055,-.053,.14-slide,.055,.026,.59-slide,trim);tube(0,-.017,.575,.09,.024,metal,8);box(.054,-.03,.29-slide,.057,.016,.38-slide,dark);
                for(int i=0;i<5;i++)box(-.056,-.038,.17+i*.014-slide,-.053,.013,.175+i*.014-slide,dark);
                if(w.silenced()){tube(0,-.017,.61,.29,.038,dark,10);tube(0,-.017,.89,.016,.025,0x536D73,8);}
                if(w==Weapon.RUSH){body(-.035,-.37-magazine,.24,.035,-.17-magazine,.32,dark);box(-.058,-.047,.41,.058,-.015,.56,0xD86850);}
                sights(.55);
            }
            box(-.031,-.14,.33,.031,-.113,.43,metal);box(-.031,-.185,.36,.031,-.17,.43,metal);box(-.031,-.18,.42,.031,-.12,.435,metal);return;
        }
        boolean shortGun=w.category==Category.SMG,shotgun=w.category==Category.SHOTGUN,heavy=w==Weapon.BASTION;
        double end=w==Weapon.HORIZON?1.24:w==Weapon.RIDGE?1.12:shortGun?.75:heavy?1.19:1.0;
        body(-.065,-.108,.08,.065,.018,.54,metal);body(-.069,-.031,.10,.069,.038,.53,trim);
        body(-.058,-.09,-.25,.058,-.015,.14,metal);body(-.088,-.15,-.27,.088,-.012,-.16,dark);box(-.067,-.05,-.15,.067,-.025,.09,shade(trim,.75));
        grip(.19,dark);box(-.032,-.14,.27,.032,-.117,.37,metal);box(-.032,-.178,.29,.032,-.161,.39,metal);box(-.032,-.17,.38,.032,-.11,.395,metal);
        if(w==Weapon.CIRCUIT){body(-.059,-.20-magazine,.06,.059,-.075-magazine,.20,trim);body(-.076,-.077,.42,.076,.037,.72,trim);}
        else if(w==Weapon.BREACH){tube(0,-.197-magazine,.27,.19,.105,dark,12);tube(0,-.197-magazine,.25,.025,.085,trim,12);}
        else if(heavy){body(-.132,-.27-magazine,.20,.132,-.10-magazine,.45,trim);for(int i=0;i<5;i++)box(-.14,-.08+i*.018,.24,-.068,-.068+i*.018,.29,0xC6A064);}
        else if(!shotgun){double length=w==Weapon.RIDGE?.12:w==Weapon.HORIZON?.095:.23;body(-.047,-.105-length-magazine,.31,.047,-.10-magazine,.43,dark);for(int i=0;i<3;i++)box(-.049,-.29+i*.04-magazine,.32,-.046,-.274+i*.04-magazine,.415,shade(trim,.55));}
        body(-.059,-.080,.53,.059,.023,end-.16,shotgun?0xA37A57:trim);tube(0,-.023,end-.17,.21,.028,metal,10);
        if(w==Weapon.MARROW){double pump=cycle<.6?Math.sin(cycle/.6*Math.PI)*.09:0;body(-.073,-.112,.56-pump,.073,-.034,.78-pump,0xB38A65);for(int i=0;i<7;i++)box(-.074,-.096,.56+i*.029-pump,.074,-.087,.568+i*.029-pump,dark);tube(0,-.074,.78,.15,.023,dark,8);}
        else for(int i=0;i<5;i++){double z=.55+(end-.76)*i/5;box(-.061,-.05,z,-.058,-.025,z+.023,dark);box(.058,-.05,z,.061,-.025,z+.023,dark);}
        for(int i=0;i<8;i++)box(-.031,.039,.17+i*.038,.031,.047,.185+i*.038,metal);
        box(.064,-.035,.24,.070,.007,.34,dark);tube(.076,-.014,.27-slide,.13,.014,0x99AEB2,6);
        if(w.silenced()){tube(0,-.023,end,.25,.044,dark,10);tube(0,-.023,end+.247,.009,.029,0x708B91,10);}
        else {tube(0,-.023,end,.075,.037,dark,8);box(-.038,-.018,end+.03,.038,-.009,end+.055,metal);}
        if(w.scoped()){
            box(-.026,.034,.22,.026,.095,.28,dark);box(-.026,.034,.49,.026,.095,.55,dark);tube(0,.102,.16,.44,w==Weapon.HORIZON?.060:.046,dark,12);tube(0,.102,.15,.009,w==Weapon.HORIZON?.050:.036,0x78C5C2,12);tube(0,.102,.19,.06,.064,trim,12);box(-.024,.145,.35,.024,.183,.39,metal);
        }else {sights(end-.2);if(w==Weapon.HELIX||w==Weapon.SHADE){box(-.042,.046,.26,-.029,.109,.34,dark);box(.029,.046,.26,.042,.109,.34,dark);box(-.042,.101,.26,.042,.114,.34,trim);box(-.028,.048,.269,.028,.100,.274,0x81B7B5);}}
        if(heavy){body(-.088,.074,.30,.088,.10,.52,dark);box(-.015,-.10,.83,.015,-.28,.86,metal);body(-.038,-.09,.76,.038,-.059,1.08,dark);}
        // Receiver-specific mechanisms and silhouettes.
        if(w==Weapon.HORIZON){double bolt=cycle<.68?Math.sin(Math.max(0,(cycle-.10)/.58)*Math.PI)*.13:0;box(.073,-.021,.26-bolt,.15,.003,.31-bolt,metal);tube(.145,-.01,.265-bolt,.035,.032,dark,8);body(-.052,-.14,-.11,.052,-.075,.08,0x879098);}
        if(w==Weapon.ECHO){body(-.043,-.29-magazine,.34,.043,-.20-magazine,.45,dark);body(-.042,-.36-magazine,.38,.042,-.28-magazine,.48,dark);body(-.076,-.052,.53,.076,.014,.76,0x8A6851);}
        if(w==Weapon.HELIX){body(-.067,-.24-magazine,-.12,.067,-.09-magazine,.035,dark);box(-.076,-.05,-.15,.076,.045,.03,trim);}
        if(w==Weapon.WISP){box(-.079,-.072,-.23,-.061,-.055,.12,trim);box(.061,-.072,-.23,.079,-.055,.12,trim);}
        if(w==Weapon.BASTION){for(int i=0;i<8;i++)tube(-.12-i*.012,-.035-i*.009,.24,.075,.009,0xC6A064,5);}
        // Small steel fasteners and manufacturer stripe break up broad surfaces.
        for(double z:new double[]{.16,.44}){box(-.071,-.060,z,-.067,-.046,z+.014,0xAFBAB5);box(.067,-.060,z,.071,-.046,z+.014,0xAFBAB5);}
        box(-.071,-.026,.37,-.068,-.015,.47,0xF0D6A3);
    }
}
