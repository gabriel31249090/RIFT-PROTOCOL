package rift;

import java.util.*;
import static rift.World.*;

/** Immutable spatial groups for the static scene; no gameplay collision changes. */
final class SceneMesh {
    record Face(Tri tri,double nx,double ny,double nz,double plane){}
    static final class Group {
        final List<Face> faces=new ArrayList<>();
        double x1=Double.POSITIVE_INFINITY,y1=x1,z1=x1,x2=-x1,y2=x2,z2=x2,x,y,z,radius,order;
        void add(Tri t){
            double ux=t.b().x()-t.a().x(),uy=t.b().y()-t.a().y(),uz=t.b().z()-t.a().z(),vx=t.c().x()-t.a().x(),vy=t.c().y()-t.a().y(),vz=t.c().z()-t.a().z();
            double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;faces.add(new Face(t,nx,ny,nz,nx*t.a().x()+ny*t.a().y()+nz*t.a().z()));
            for(V p:new V[]{t.a(),t.b(),t.c()}){x1=Math.min(x1,p.x());y1=Math.min(y1,p.y());z1=Math.min(z1,p.z());x2=Math.max(x2,p.x());y2=Math.max(y2,p.y());z2=Math.max(z2,p.z());}
        }
        void finish(){x=(x1+x2)*.5;y=(y1+y2)*.5;z=(z1+z2)*.5;radius=Math.sqrt((x2-x)*(x2-x)+(y2-y)*(y2-y)+(z2-z)*(z2-z));}
        boolean visible(Renderer r){
            double dx=x-r.cx,dy=y-r.cy,dz=z-r.cz,forward=dx*r.sinY+dz*r.cosY,xx=dx*r.cosY-dz*r.sinY,yy=dy*r.cosP-forward*r.sinP,zz=dy*r.sinP+forward*r.cosP;
            if(zz+radius<.09||zz-radius>r.far)return false;
            double hx=r.width*.5/r.focal,hy=r.height*.5/r.focal;
            if(Math.abs(xx)-zz*hx>radius*Math.sqrt(1+hx*hx)||Math.abs(yy)-zz*hy>radius*Math.sqrt(1+hy*hy))return false;
            order=dx*dx+dy*dy+dz*dz;return true;
        }
    }
    final World world;final Group[] groups,visible;final int sourceCount;
    SceneMesh(World world){this.world=world;sourceCount=world.triangles.size();Map<Long,Group> grid=new LinkedHashMap<>();
        for(Tri t:world.triangles){int x=(int)Math.floor((t.a().x()+t.b().x()+t.c().x())/36),z=(int)Math.floor((t.a().z()+t.b().z()+t.c().z())/36);long key=((long)x<<32)|(z&0xffffffffL);grid.computeIfAbsent(key,k->new Group()).add(t);}
        groups=grid.values().toArray(Group[]::new);visible=new Group[groups.length];for(Group group:groups)group.finish();
    }
    void render(Renderer r){int count=0;for(Group group:groups)if(group.visible(r))visible[count++]=group;
        Arrays.sort(visible,0,count,Comparator.comparingDouble(g->g.order));
        for(int i=0;i<count;i++)for(Face f:visible[i].faces)if(f.nx*r.cx+f.ny*r.cy+f.nz*r.cz<f.plane)r.worldTriangle(f.tri);
    }
}
