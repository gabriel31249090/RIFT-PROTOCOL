package rift;

import java.util.*;
import static rift.World.*;
import static rift.Game.*;

/** Material and physical thickness consume a shared penetration budget along the ray. */
final class Ballistics {
    enum Material {
        WOOD(1,.82,Assets.WOOD), METAL(3.5,.62,Assets.PLATE), CONCRETE(Double.POSITIVE_INFINITY,0,Assets.CONCRETE);
        final double resistance,retention;final int texture;
        Material(double resistance,double retention,int texture){this.resistance=resistance;this.retention=retention;this.texture=texture;}
    }
    static double power(Weapon w){return switch(w){
        case SPARK,RUSH -> .12;case VEIL -> .14;case TALON -> .45;
        case WISP -> .30;case CIRCUIT -> .24;
        case ECHO -> .60;case SHADE -> .56;case HELIX -> .52;
        case RIDGE -> .80;case HORIZON -> 1;case BASTION -> 1.20;
        case DART,MARROW,BREACH -> 0;
    };}
    static int surfaces(Weapon w){return w.pellets>1?0:w.sidearm()||w.category==Category.SMG?1:2;}
    record Passage(double exit,double factor){}
    record Cover(double entry,double exit,Material material){}
    record Shot(double distance,List<Passage> passages,List<Cover> covers){
        Shot {passages=List.copyOf(passages);covers=List.copyOf(covers);}
        double factorAt(double target){return damageFactor(covers,Math.min(distance,target));}
    }
    static double damageFactor(List<Cover> covers,double distance){
        double factor=1;
        for(Cover cover:covers)if(distance>cover.entry()){
            double cost=(Math.min(distance,cover.exit())-cover.entry())*cover.material().resistance;
            factor*=Math.max(.20,cover.material().retention-cost*.55);
        }
        return factor;
    }
    static Shot trace(World world,V origin,V direction,Weapon weapon){
        Set<Box> penetrable=new HashSet<>(world.penetrable);
        double blocked=world.ray(origin,direction,210,penetrable);
        List<Cover> covers=new ArrayList<>();
        for(Box box:world.solids)if(penetrable.contains(box)){
            double entry=World.rayBox(origin,direction,box);
            if(entry>=0&&entry<blocked)covers.add(new Cover(entry,exit(origin,direction,box),world.material(box)));
        }
        covers.sort(Comparator.comparingDouble(Cover::entry));
        PriorityQueue<Cover> traversing=new PriorityQueue<>(Comparator.comparingDouble(Cover::exit));
        double power=power(weapon),cursor=0;int next=0,count=0;
        List<Passage> passages=new ArrayList<>();
        List<Cover> entered=new ArrayList<>();
        // Entry and exit events also account for overlapping panels; no geometry is skipped.
        while(next<covers.size()||!traversing.isEmpty()){
            double entry=next<covers.size()?covers.get(next).entry():Double.POSITIVE_INFINITY;
            double exit=traversing.isEmpty()?Double.POSITIVE_INFINITY:traversing.peek().exit();
            double end=Math.min(blocked,Math.min(entry,exit));
            double resistance=0;for(Cover cover:traversing)resistance+=cover.material().resistance;
            double cost=(end-cursor)*resistance;
            if(cost>power+1e-8)return new Shot(cursor+power/resistance,passages,entered);
            power=Math.max(0,power-cost);cursor=end;
            if(blocked<=cursor)return new Shot(blocked,passages,entered);
            if(exit<=entry){
                Cover cover=traversing.remove();
                passages.add(new Passage(cover.exit(),damageFactor(entered,cover.exit())));
            }else{
                Cover cover=covers.get(next++);
                if(count>=surfaces(weapon)||power<=0||!Double.isFinite(cover.material().resistance)||cover.exit()<cover.entry())
                    return new Shot(cover.entry(),passages,entered);
                count++;traversing.add(cover);entered.add(cover);
            }
        }
        return new Shot(blocked,passages,entered);
    }
    static double exit(V p,V d,Box b){double limit=Double.POSITIVE_INFINITY;if(Math.abs(d.x())>1e-9)limit=Math.min(limit,((d.x()>0?b.x2():b.x1())-p.x())/d.x());if(Math.abs(d.y())>1e-9)limit=Math.min(limit,((d.y()>0?b.y2():b.y1())-p.y())/d.y());if(Math.abs(d.z())>1e-9)limit=Math.min(limit,((d.z()>0?b.z2():b.z1())-p.z())/d.z());return limit;}
}
