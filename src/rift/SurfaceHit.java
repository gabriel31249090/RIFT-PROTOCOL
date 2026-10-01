package rift;

import static rift.World.*;

/** Swept sphere against the collision world. Normals always point out of the surface. */
record SurfaceHit(double distance, V point, V normal, int material) {
    static SurfaceHit cast(World world,V from,V direction,double limit,double radius){
        SurfaceHit best=null;
        for(var group:java.util.List.of(world.solids,world.temporary))for(Box b:group){
            Box expanded=new Box(b.x1()-radius,b.y1()-radius,b.z1()-radius,b.x2()+radius,b.y2()+radius,b.z2()+radius,b.color());
            double t=World.rayBox(from,direction,expanded);
            if(t<0||t>limit||best!=null&&t>=best.distance)continue;
            V p=from.add(direction.mul(t));V n=normal(p,expanded,direction);
            if(direction.dot(n)>=-1e-8)continue;
            if(t<1e-8)p=new V(n.x()<0?expanded.x1():n.x()>0?expanded.x2():p.x(),n.y()<0?expanded.y1():n.y()>0?expanded.y2():p.y(),n.z()<0?expanded.z1():n.z()>0?expanded.z2():p.z());
<<<<<<< HEAD
            best=new SurfaceHit(t,p,n,world.material(b).texture);
=======
            best=new SurfaceHit(t,p,n,world.penetrable.contains(b)?Assets.WOOD:Assets.CONCRETE);
>>>>>>> a28a0d3591e35d0c3bb500da202ff4a47e878941
        }
        if(direction.y()<-.000001){double t=(radius-from.y())/direction.y();if(t>=-1e-7&&t<=limit&&(best==null||t<best.distance))best=new SurfaceHit(Math.max(0,t),from.add(direction.mul(Math.max(0,t))),new V(0,1,0),Assets.FLOOR);}
        for(Ramp ramp:world.ramps){
            double slope=(ramp.reverse()?-1:1)*ramp.height()/(ramp.z2()-ramp.z1());V n=new V(0,1,-slope).unit();double den=direction.dot(n);
            if(den>=-1e-8)continue;
            double signed=(from.y()-ramp.at(from.z()))*n.y();double t=(radius-signed)/den;
            if(t<0||t>limit||best!=null&&t>=best.distance)continue;V p=from.add(direction.mul(t));
            if(ramp.contains(p.x(),p.z()))best=new SurfaceHit(t,p,n,Assets.PLATE);
        }
        return best;
    }
    static V normal(V p,Box b,V dir){
        double[] d={Math.abs(p.x()-b.x1()),Math.abs(p.x()-b.x2()),Math.abs(p.y()-b.y1()),Math.abs(p.y()-b.y2()),Math.abs(p.z()-b.z1()),Math.abs(p.z()-b.z2())};
        V[] normals={new V(-1,0,0),new V(1,0,0),new V(0,-1,0),new V(0,1,0),new V(0,0,-1),new V(0,0,1)};
        int index=0;double near=Double.MAX_VALUE;for(int i=0;i<6;i++)if(d[i]<near-1e-7||Math.abs(d[i]-near)<1e-7&&dir.dot(normals[i])<dir.dot(normals[index])){near=d[i];index=i;}return normals[index];
    }
}
