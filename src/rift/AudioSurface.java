package rift;

import static rift.Game.*;
import static rift.World.*;

/** Surface cues share positional mixing, not the bots' hearing simulation. */
final class AudioSurface {
    enum Material {
        CONCRETE("concrete"),WOOD("wood"),METAL("metal");
        final String key;
        Material(String key){this.key=key;}
    }
    static Material material(int texture){return switch(texture){
        case Assets.WOOD -> Material.WOOD;
        case Assets.TEAL,Assets.RUST,Assets.STEEL,Assets.BRASS,Assets.PLATE,Assets.ROOF -> Material.METAL;
        default -> Material.CONCRETE;
    };}
    static Material floor(World world){return world.mapIndex==2||world.mapIndex==5?Material.METAL:Material.CONCRETE;}
    static Material ground(World world,Actor actor){
        for(Ramp ramp:world.ramps)if(ramp.contains(actor.x,actor.z)&&Math.abs(ramp.at(actor.z)-actor.y)<.08)return Material.METAL;
        for(Box box:world.solids)if(actor.x>=box.x1()&&actor.x<=box.x2()&&actor.z>=box.z1()&&actor.z<=box.z2()&&Math.abs(box.y2()-actor.y)<.08)return material(world.visualMaterial(box));
        for(Box box:world.temporary)if(actor.x>=box.x1()&&actor.x<=box.x2()&&actor.z>=box.z1()&&actor.z<=box.z2()&&Math.abs(box.y2()-actor.y)<.08)return material(world.visualMaterial(box));
        return floor(world);
    }
    static Material material(World world,SurfaceHit hit){
        V p=hit.point(),n=hit.normal();
        for(Box box:world.solids)if(face(box,p,n))return material(world.visualMaterial(box));
        for(Box box:world.temporary)if(face(box,p,n))return material(world.visualMaterial(box));
        return hit.material()==Assets.FLOOR?floor(world):material(hit.material());
    }
    static boolean face(Box box,V p,V n){
        double epsilon=.025;
        if(p.x()<box.x1()-epsilon||p.x()>box.x2()+epsilon||p.y()<box.y1()-epsilon||p.y()>box.y2()+epsilon||p.z()<box.z1()-epsilon||p.z()>box.z2()+epsilon)return false;
        return n.x()<-.5&&Math.abs(p.x()-box.x1())<epsilon||n.x()>.5&&Math.abs(p.x()-box.x2())<epsilon
            ||n.y()<-.5&&Math.abs(p.y()-box.y1())<epsilon||n.y()>.5&&Math.abs(p.y()-box.y2())<epsilon
            ||n.z()<-.5&&Math.abs(p.z()-box.z1())<epsilon||n.z()>.5&&Math.abs(p.z()-box.z2())<epsilon;
    }
    static String group(Weapon weapon){
        if(weapon.pellets>1)return "shotgun";
        return switch(weapon.category){case PISTOL->"pistol";case SMG->"smg";case RIFLE->"rifle";case SHOTGUN->"shotgun";case PRECISION->"precision";case HEAVY->"heavy";};
    }
    static void play(Game game,String name,V point,double strength,double range){
        Actor listener=game.cameraActor();
        if(listener==null||game.world==null||!Double.isFinite(strength)||strength<=0||!Double.isFinite(range)||range<=0)return;
        V delta=point.sub(listener.eye());double distance=delta.length();
        if(!Double.isFinite(distance)||distance>=range)return;
        double pan=Math.sin(Math.atan2(delta.x(),delta.z())-listener.yaw),fall=1-distance/range;
        double gain=strength*fall*fall;
        if(distance>.05&&!game.world.visible(listener.eye(),point))gain*=.35;
        if(gain>.005)game.audio.playAt(name,pan,gain);
    }
    static void step(Game game,Actor actor){
        if(actor.dead||actor.crouch||!actor.grounded||actor.moveSpeed<=3.3)return;
        play(game,"step_"+ground(game.world,actor).key,new V(actor.x,actor.y+.12,actor.z),.68,19);
    }
    static void land(Game game,Actor actor,double speed){
        if(actor.dead||!Double.isFinite(speed)||speed<=3)return;
        play(game,"land_"+ground(game.world,actor).key,new V(actor.x,actor.y+.12,actor.z),Settings.clamp(speed/9,.25,1.4),28);
    }
    static void impact(Game game,SurfaceHit hit){
        play(game,"impact_"+material(game.world,hit).key,hit.point().add(hit.normal().mul(.025)),.72,32);
    }
    static void reload(Game game,Actor actor){
        if(!actor.dead)play(game,"reload_"+group(actor.gun().kind),actor.eye(),.70,13);
    }
    static void reloadStage(Game game,Actor actor,int stage){
        if(actor.dead)return;
        String name=stage==1?"magout":stage==2?"magin":"bolt";
        play(game,name+"_"+group(actor.gun().kind),actor.eye(),.64,13);
    }
    static void reloadTick(Game game,Actor actor,double dt){
        if(actor.dead||actor.melee()||!Double.isFinite(dt)||dt<=0)return;
        Gun gun=actor.gun();
        if(gun.reload<=0||gun.reloadTotal<=0)return;
        double progress=1-Math.max(0,gun.reload-dt)/gun.reloadTotal;
        int stage=progress>=.82?3:progress>=.55?2:progress>=.14?1:0;
        while(gun.reloadStage<stage)reloadStage(game,actor,++gun.reloadStage);
    }
}
