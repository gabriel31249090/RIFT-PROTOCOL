package rift;

import java.util.*;
import static rift.World.*;
import static rift.Game.*;

<<<<<<< HEAD
/** Material and physical thickness consume a shared penetration budget along the ray. */
final class Ballistics {
    enum Material {
        WOOD(1,.82,Assets.WOOD), METAL(3.5,.62,Assets.PLATE), CONCRETE(Double.POSITIVE_INFINITY,0,Assets.CONCRETE);
        final double resistance,retention;final int texture;
        Material(double resistance,double retention,int texture){this.resistance=resistance;this.retention=retention;this.texture=texture;}
    }
    static double power(Weapon w){return w.pellets>1?0:w.category==Category.HEAVY?1.2:w.scoped()?.9:w.category==Category.RIFLE?.6:w==Weapon.TALON?.45:w.category==Category.SMG?.28:.15;}
    record Passage(double exit,double factor){}
    record Shot(double distance,List<Passage> passages){double factorAt(double distance){double factor=1;for(Passage p:passages)if(distance>p.exit())factor=p.factor();return factor;}}
    static Shot trace(World world,V origin,V direction,Weapon weapon){
        double power=power(weapon);
=======
/** Up to two thin, explicitly tagged cover panels. Structural walls always stop bullets. */
final class Ballistics {
    record Passage(double exit,double factor){}
    record Shot(double distance,List<Passage> passages){double factorAt(double distance){double factor=1;for(Passage p:passages)if(distance>p.exit())factor=p.factor();return factor;}}
    static Shot trace(World world,V origin,V direction,Weapon weapon){
        double power=weapon.pellets>1?0:weapon.category==Category.HEAVY?1.2:weapon.scoped()?.9:weapon.category==Category.RIFLE?.6:weapon==Weapon.TALON?.45:weapon.category==Category.SMG?.28:.15;
>>>>>>> a28a0d3591e35d0c3bb500da202ff4a47e878941
        double cursor=0,factor=1;List<Passage> passages=new ArrayList<>();
        for(int i=0;i<3;i++){
            V start=origin.add(direction.mul(cursor));double hit=world.ray(start,direction,210-cursor);Box cover=null;
            for(Box b:world.penetrable){double t=World.rayBox(start,direction,b);if(t>=0&&Math.abs(t-hit)<.001){cover=b;break;}}
            if(cover==null||i==2)return new Shot(cursor+hit,passages);
            double exit=exit(start,direction,cover),thickness=exit-hit;
<<<<<<< HEAD
            Material material=world.material(cover);double cost=thickness*material.resistance;
            if(power<=0||cost>power+1e-8||thickness<0)return new Shot(cursor+hit,passages);
            power-=cost;
            factor*=Math.max(.20,material.retention-cost*.55);cursor+=exit+.002;passages.add(new Passage(cursor,factor));
=======
            if(thickness>power||thickness<0)return new Shot(cursor+hit,passages);
            factor*=Math.max(.25,.78-thickness*.55);cursor+=exit+.002;passages.add(new Passage(cursor,factor));
>>>>>>> a28a0d3591e35d0c3bb500da202ff4a47e878941
        }
        return new Shot(cursor,passages);
    }
    static double exit(V p,V d,Box b){double limit=Double.POSITIVE_INFINITY;if(Math.abs(d.x())>1e-9)limit=Math.min(limit,((d.x()>0?b.x2():b.x1())-p.x())/d.x());if(Math.abs(d.y())>1e-9)limit=Math.min(limit,((d.y()>0?b.y2():b.y1())-p.y())/d.y());if(Math.abs(d.z())>1e-9)limit=Math.min(limit,((d.z()>0?b.z2():b.z1())-p.z())/d.z());return limit;}
}
