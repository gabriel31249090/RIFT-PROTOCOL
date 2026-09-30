package rift;

import java.util.*;
import static rift.World.*;
import static rift.Game.*;

/** Up to two thin, explicitly tagged cover panels. Structural walls always stop bullets. */
final class Ballistics {
    record Passage(double exit,double factor){}
    record Shot(double distance,List<Passage> passages){double factorAt(double distance){double factor=1;for(Passage p:passages)if(distance>p.exit())factor=p.factor();return factor;}}
    static Shot trace(World world,V origin,V direction,Weapon weapon){
        double power=weapon.pellets>1?0:weapon.category==Category.HEAVY?1.2:weapon.scoped()?.9:weapon.category==Category.RIFLE?.6:weapon==Weapon.TALON?.45:weapon.category==Category.SMG?.28:.15;
        double cursor=0,factor=1;List<Passage> passages=new ArrayList<>();
        for(int i=0;i<3;i++){
            V start=origin.add(direction.mul(cursor));double hit=world.ray(start,direction,210-cursor);Box cover=null;
            for(Box b:world.penetrable){double t=World.rayBox(start,direction,b);if(t>=0&&Math.abs(t-hit)<.001){cover=b;break;}}
            if(cover==null||i==2)return new Shot(cursor+hit,passages);
            double exit=exit(start,direction,cover),thickness=exit-hit;
            if(thickness>power||thickness<0)return new Shot(cursor+hit,passages);
            factor*=Math.max(.25,.78-thickness*.55);cursor+=exit+.002;passages.add(new Passage(cursor,factor));
        }
        return new Shot(cursor,passages);
    }
    static double exit(V p,V d,Box b){double limit=Double.POSITIVE_INFINITY;if(Math.abs(d.x())>1e-9)limit=Math.min(limit,((d.x()>0?b.x2():b.x1())-p.x())/d.x());if(Math.abs(d.y())>1e-9)limit=Math.min(limit,((d.y()>0?b.y2():b.y1())-p.y())/d.y());if(Math.abs(d.z())>1e-9)limit=Math.min(limit,((d.z()>0?b.z2():b.z1())-p.z())/d.z());return limit;}
}
