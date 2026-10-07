package rift;

import java.util.List;
import java.util.ArrayList;
import java.util.function.UnaryOperator;
import static rift.World.*;
import static rift.Game.*;

/** Shared weapon meshes; ECHO combines a licensed receiver with original RIFT equipment. */
final class WeaponModel {
    final List<Tri> out;final double ox,oy,oz,yaw,size,sinYaw,cosYaw;final int metal,dark,trim;
    int textureMetal=Assets.STEEL;
    WeaponModel(List<Tri> out,double x,double y,double z,double yaw,double size,int trim){this.out=out;ox=x;oy=y;oz=z;this.yaw=yaw;this.size=size;sinYaw=Math.sin(yaw);cosYaw=Math.cos(yaw);this.trim=trim;metal=0x293943;dark=0x15232D;}
    WeaponModel(List<Tri> out,double x,double y,double z,double yaw,double size,Cosmetics.Skin skin){this.out=out;ox=x;oy=y;oz=z;this.yaw=yaw;this.size=size;sinYaw=Math.sin(yaw);cosYaw=Math.cos(yaw);trim=skin.trim;metal=skin.metal;dark=skin.dark;}
    static void add(List<Tri> out,Weapon w,double x,double y,double z,double yaw,double size,double magazine,double cycle){
        WeaponModel m=new WeaponModel(out,x,y,z,yaw,size,w.color);m.gun(w,magazine,cycle);
    }
    static void add(List<Tri> out,Weapon w,double x,double y,double z,double yaw,double size,double magazine,double cycle,Cosmetics.Skin skin,Cosmetics.Charm charm,double swing,double twist){
        WeaponModel m=skin==Cosmetics.Skin.STANDARD?new WeaponModel(out,x,y,z,yaw,size,w.color):new WeaponModel(out,x,y,z,yaw,size,skin);m.gun(w,magazine,cycle);Cosmetics.decorate(m,w,skin);Cosmetics.charm(m,charm,swing,twist);
    }
    V p(double x,double y,double z){return new V(ox+size*(x*cosYaw+z*sinYaw),oy+y*size,oz+size*(-x*sinYaw+z*cosYaw));}
    int material(int color){return color==dark?Assets.RUBBER:color==0x8A6851||color==0xB38A65?Assets.WOOD:color==0xC6A064||color==0xC79B65?Assets.BRASS:textureMetal;}
    void textured(int start,int color){for(int i=start;i<out.size();i++){Tri t=out.get(i);Tri local=new Tri(local(t.a()),local(t.b()),local(t.c()),t.color());Tri tex=World.texture(local,material(color),3.5);out.set(i,new Tri(t.a(),t.b(),t.c(),t.color(),tex.material(),tex.ua(),tex.va(),tex.ub(),tex.vb(),tex.uc(),tex.vc()));}}
    V local(V v){double x=(v.x()-ox)/size,z=(v.z()-oz)/size;return new V(x*cosYaw-z*sinYaw,(v.y()-oy)/size,x*sinYaw+z*cosYaw);}
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
    private static final class EchoAssets {
        static final MeshAssets.Mesh BODY=MeshAssets.tryLoad("models/echo/echo-body.obj");
        static final MeshAssets.Mesh WORLD_BODY=MeshAssets.tryLoad("models/echo/echo-world-body.obj");
        static final List<Tri> WORLD_FIXED=worldEquipment(false),WORLD_MAGAZINE=worldEquipment(true);
    }
    int echoColor(String material){return switch(material){case "Black"->dark;case "Metal"->shade(metal,1.9);case "DarkWood"->shade(trim,.58);default->metal;};}
    static int echoTexture(String material){return material.equals("Black")||material.equals("DarkWood")?Assets.RUBBER:ModelMaterials.METAL;}
    void echo(double magazine,double cycle){
        textureMetal=ModelMaterials.METAL;
        if(EchoAssets.BODY!=null)EchoAssets.BODY.add(out,v->p(v.x(),v.y(),v.z()),this::echoColor,WeaponModel::echoTexture);
        else {body(-.065,-.075,.018,.065,.080,.57,metal);body(-.062,-.052,.57,.062,.075,.815,trim);}
        // Original modular stock and segmented magazine keep the ECHO silhouette and reload motion.
        body(-.052,-.060,-.17,.052,.018,.08,metal);body(-.072,-.132,-.28,.072,-.026,-.12,dark);
        box(-.054,-.103,-.265,.054,-.047,-.244,shade(trim,.60));box(-.047,.019,-.15,.047,.042,.048,dark);
        grip(.19,dark);box(-.030,-.142,.275,.030,-.119,.38,metal);box(-.030,-.179,.29,.030,-.163,.40,metal);box(-.030,-.17,.385,.030,-.11,.40,metal);
        echoMagazine(magazine,true);
        for(int side:new int[]{-1,1}){
            double x=side*.060;for(int i=0;i<4;i++)box(x-.002,-.023,.60+i*.042,x+.002,-.008,.619+i*.042,dark);
            box(side*.070-.002,-.010,.35,side*.070+.002,.009,.46,0x86D4C0);
            box(side*.071-.002,-.048,.15,side*.071+.002,-.034,.24,shade(trim,1.10));
        }
        box(-.039,.079,.11,.039,.085,.58,dark);for(int i=0;i<7;i++)box(-.038,.086,.14+i*.055,.038,.094,.159+i*.055,metal);
        double slide=Math.sin(Math.PI*Math.min(1,cycle*5))*.067;
        box(.052,-.024,.28-slide,.077,.005,.41-slide,dark);tube(.080,-.009,.31-slide,.052,.012,trim,6);
        tube(0,-.023,.808,.195,.028,metal,8);tube(0,-.023,1.0,.075,.038,dark,8);
        for(int i=0;i<2;i++){double z=1.025+i*.025;box(-.039,-.017,z,.039,-.008,z+.008,shade(metal,1.2));}
        sights(.78);box(-.017,.082,.16,.017,.101,.25,dark);
        for(double z:new double[]{.19,.49}){box(-.071,-.037,z,-.068,-.025,z+.012,0xA6ADAA);box(.068,-.037,z,.071,-.025,z+.012,0xA6ADAA);}
    }
    void echoMagazine(double drop,boolean detailed){
        body(-.047,-.232-drop,.31,.047,-.089-drop,.425,dark);
        body(-.043,-.300-drop,.335,.043,-.222-drop,.447,dark);
        body(-.041,-.367-drop,.370,.041,-.291-drop,.483,dark);
        box(-.045,-.372-drop,.367,.045,-.352-drop,.491,shade(trim,.68));
        if(detailed)for(int side:new int[]{-1,1})for(int i=0;i<3;i++){
            double x=side*.048;box(x-.002,-.203+i*.032-drop,.324,x+.002,-.192+i*.032-drop,.414,shade(trim,.45));
        }
    }
    static List<Tri> worldEquipment(boolean magazine){
        List<Tri> tris=new ArrayList<>();WeaponModel m=new WeaponModel(tris,0,0,0,0,1,Weapon.ECHO.color);
        m.textureMetal=ModelMaterials.METAL;
        if(magazine){m.box(-.046,-.27,.32,.046,-.10,.43,m.dark);m.box(-.041,-.36,.37,.041,-.25,.48,m.dark);}
        else {
            if(EchoAssets.WORLD_BODY==null)m.box(-.064,-.075,.02,.064,.080,.81,m.metal);
            m.box(-.052,-.06,-.17,.052,.018,.08,m.metal);m.box(-.071,-.13,-.28,.071,-.025,-.12,m.dark);
            m.box(-.046,-.25,.15,.046,-.078,.25,m.dark);m.box(-.059,-.052,.59,.059,.075,.815,shade(m.trim,.58));
            m.tube(0,-.023,.808,.195,.028,m.metal,6);m.tube(0,-.023,1.0,.075,.037,m.dark,6);
            m.box(-.032,.081,.13,.032,.095,.53,m.dark);m.box(-.018,.095,.20,.018,.127,.25,m.dark);
            m.box(-.015,.080,.74,.015,.121,.77,m.dark);m.box(-.070,-.010,.35,.070,.009,.46,0x86D4C0);
        }
        return List.copyOf(tris);
    }
    /** Local +Z forward; no first-person hands, collection ornaments or extra actor hitboxes. */
    static void addEchoWorld(List<Tri> out,UnaryOperator<V> pose,double magazineDrop){
        if(EchoAssets.WORLD_BODY!=null)EchoAssets.WORLD_BODY.add(out,pose,name->switch(name){case "Black"->0x15232D;case "Metal"->0x4E6C7F;default->0x293943;},WeaponModel::echoTexture);
        for(Tri t:EchoAssets.WORLD_FIXED)out.add(t.at(pose.apply(t.a()),pose.apply(t.b()),pose.apply(t.c())));
        for(Tri t:EchoAssets.WORLD_MAGAZINE)out.add(t.at(pose.apply(t.a().add(new V(0,-magazineDrop,0))),pose.apply(t.b().add(new V(0,-magazineDrop,0))),pose.apply(t.c().add(new V(0,-magazineDrop,0)))));
    }
    void gun(Weapon w,double magazine,double cycle){
        double slide=Math.sin(Math.PI*Math.min(1,cycle*5))*.067;
        if(w==Weapon.ECHO){echo(magazine,cycle);return;}
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
