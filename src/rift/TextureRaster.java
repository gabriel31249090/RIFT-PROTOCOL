package rift;

import static rift.World.*;

/** Perspective-correct bitmap UVs, near-plane clipping and distance mipmaps. */
final class TextureRaster {
    record Vertex(V p,double u,double v){}
    static void draw(Renderer r,Tri t){
<<<<<<< HEAD
        double ax=t.a().x()-r.cx,ay=t.a().y()-r.cy,az=t.a().z()-r.cz,bx=t.b().x()-r.cx,by=t.b().y()-r.cy,bz=t.b().z()-r.cz,cx=t.c().x()-r.cx,cy=t.c().y()-r.cy,cz=t.c().z()-r.cz;
        double af=ax*r.sinY+az*r.cosY,bf=bx*r.sinY+bz*r.cosY,cf=cx*r.sinY+cz*r.cosY;
        double xx=ax*r.cosY-az*r.sinY,yy=ay*r.cosP-af*r.sinP,zz=ay*r.sinP+af*r.cosP;
        double xxx=bx*r.cosY-bz*r.sinY,yyy=by*r.cosP-bf*r.sinP,zzz=by*r.sinP+bf*r.cosP;
        double xxxx=cx*r.cosY-cz*r.sinY,yyyy=cy*r.cosP-cf*r.sinP,zzzz=cy*r.sinP+cf*r.cosP;
        if(zz<.09&&zzz<.09&&zzzz<.09||zz>r.far&&zzz>r.far&&zzzz>r.far)return;
        if(zz>=.09&&zzz>=.09&&zzzz>=.09){raster(r,xx,yy,zz,t.ua(),t.va(),xxx,yyy,zzz,t.ub(),t.vb(),xxxx,yyyy,zzzz,t.uc(),t.vc(),t);return;}
        Vertex a=new Vertex(new V(xx,yy,zz),t.ua(),t.va()),b=new Vertex(new V(xxx,yyy,zzz),t.ub(),t.vb()),c=new Vertex(new V(xxxx,yyyy,zzzz),t.uc(),t.vc());
=======
        Vertex a=new Vertex(r.transform(t.a()),t.ua(),t.va()),b=new Vertex(r.transform(t.b()),t.ub(),t.vb()),c=new Vertex(r.transform(t.c()),t.uc(),t.vc());
        if(a.p.z()>r.far&&b.p.z()>r.far&&c.p.z()>r.far)return;
        if(a.p.z()>=.09&&b.p.z()>=.09&&c.p.z()>=.09){raster(r,a,b,c,t);return;}
>>>>>>> a28a0d3591e35d0c3bb500da202ff4a47e878941
        Vertex[] input={a,b,c},out=new Vertex[5];int count=0;
        for(int i=0;i<3;i++){Vertex p=input[i],q=input[(i+1)%3];boolean pin=p.p.z()>=.09,qin=q.p.z()>=.09;if(pin)out[count++]=p;
            if(pin!=qin){double f=(.09-p.p.z())/(q.p.z()-p.p.z());out[count++]=new Vertex(p.p.add(q.p.sub(p.p).mul(f)),p.u+(q.u-p.u)*f,p.v+(q.v-p.v)*f);}}
        for(int i=1;i<count-1;i++)raster(r,out[0],out[i],out[i+1],t);
    }
<<<<<<< HEAD
    static void raster(Renderer r,Vertex a,Vertex b,Vertex c,Tri tri){raster(r,a.p.x(),a.p.y(),a.p.z(),a.u,a.v,b.p.x(),b.p.y(),b.p.z(),b.u,b.v,c.p.x(),c.p.y(),c.p.z(),c.u,c.v,tri);}
    static void raster(Renderer r,double ax3,double ay3,double az3,double au,double av,double bx3,double by3,double bz3,double bu,double bv,double cx3,double cy3,double cz3,double cu,double cv,Tri tri){
        double za=1/az3,zb=1/bz3,zc=1/cz3;
        double ax=r.width*.5+ax3*r.focal*za,ay=r.height*.5-ay3*r.focal*za,bx=r.width*.5+bx3*r.focal*zb,by=r.height*.5-by3*r.focal*zb,cx=r.width*.5+cx3*r.focal*zc,cy=r.height*.5-cy3*r.focal*zc;
=======
    static void raster(Renderer r,Vertex a,Vertex b,Vertex c,Tri tri){
        double za=1/a.p.z(),zb=1/b.p.z(),zc=1/c.p.z();
        double ax=r.width*.5+a.p.x()*r.focal*za,ay=r.height*.5-a.p.y()*r.focal*za,bx=r.width*.5+b.p.x()*r.focal*zb,by=r.height*.5-b.p.y()*r.focal*zb,cx=r.width*.5+c.p.x()*r.focal*zc,cy=r.height*.5-c.p.y()*r.focal*zc;
>>>>>>> a28a0d3591e35d0c3bb500da202ff4a47e878941
        double area=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);if(Math.abs(area)<.01)return;
        int x0=Math.max(0,(int)Math.floor(Math.min(ax,Math.min(bx,cx)))),x1=Math.min(r.width-1,(int)Math.ceil(Math.max(ax,Math.max(bx,cx))));
        int y0=Math.max(0,(int)Math.floor(Math.min(ay,Math.min(by,cy)))),y1=Math.min(r.height-1,(int)Math.ceil(Math.max(ay,Math.max(by,cy))));if(x0>x1||y0>y1)return;r.triangleCount++;
        double iz=1/area,A=(by-cy)*iz,B=(cx-bx)*iz,C=(bx*cy-by*cx)*iz,D=(cy-ay)*iz,E=(ax-cx)*iz,F=(cx*ay-cy*ax)*iz;
<<<<<<< HEAD
        double u0=au*za,u1=bu*zb,u2=cu*zc,v0=av*za,v1=bv*zb,v2=cv*zc;
        double zx=A*(za-zc)+D*(zb-zc),zy=B*(za-zc)+E*(zb-zc),ux=A*(u0-u2)+D*(u1-u2),uy=B*(u0-u2)+E*(u1-u2),vx=A*(v0-v2)+D*(v1-v2),vy=B*(v0-v2)+E*(v1-v2);
        double wr=A*(x0+.5)+B*(y0+.5)+C,qr=D*(x0+.5)+E*(y0+.5)+F,zr=zc+wr*(za-zc)+qr*(zb-zc),ur=u2+wr*(u0-u2)+qr*(u1-u2),vr=v2+wr*(v0-v2)+qr*(v1-v2);
        double dist=(az3+bz3+cz3)/3;int level=dist>90?4:dist>55?3:dist>25?2:dist>10?1:0,size=256>>level,mask=size-1;
        int fog=(int)Settings.clamp((dist-12)*1.7,0,215),color=Renderer.blend(tri.color(),0xC3D1C5,fog);if(r.nearSight)color=Renderer.blend(color,0x272637,(int)Settings.clamp((dist-2)*55,0,255));int[] tex=Assets.tinted(tri.material(),level,color);byte[] lookup=Assets.indices[tri.material()][level];
        TriangleSpan span=r.span;span.set(ax,ay,bx,by,cx,cy);
        for(int y=y0;y<=y1;y++,zr+=zy,ur+=uy,vr+=vy){if(!span.row(y,r.width))continue;
            int offset=span.left-x0,index=y*r.width+span.left;double z=zr+zx*offset,u=ur+ux*offset,v=vr+vx*offset;
            for(int x=span.left;x<=span.right;x++,index++,z+=zx,u+=ux,v+=vx)if(z>r.depth[index]){
                double inv=size/z;int tx=(int)Math.floor(u*inv)&mask,ty=(int)Math.floor(v*inv)&mask;r.pixels[index]=tex[lookup[ty*size+tx]&255];r.depth[index]=(float)z;
            }
        }
=======
        double u0=a.u*za,u1=b.u*zb,u2=c.u*zc,v0=a.v*za,v1=b.v*zb,v2=c.v*zc;
        double zx=A*(za-zc)+D*(zb-zc),zy=B*(za-zc)+E*(zb-zc),ux=A*(u0-u2)+D*(u1-u2),uy=B*(u0-u2)+E*(u1-u2),vx=A*(v0-v2)+D*(v1-v2),vy=B*(v0-v2)+E*(v1-v2);
        double wr=A*(x0+.5)+B*(y0+.5)+C,qr=D*(x0+.5)+E*(y0+.5)+F,zr=zc+wr*(za-zc)+qr*(zb-zc),ur=u2+wr*(u0-u2)+qr*(u1-u2),vr=v2+wr*(v0-v2)+qr*(v1-v2);
        double dist=(a.p.z()+b.p.z()+c.p.z())/3;int level=dist>90?4:dist>55?3:dist>25?2:dist>10?1:0,size=256>>level,mask=size-1;
        int fog=(int)Settings.clamp((dist-12)*1.7,0,215),color=Renderer.blend(tri.color(),0xC3D1C5,fog);if(r.nearSight)color=Renderer.blend(color,0x272637,(int)Settings.clamp((dist-2)*55,0,255));int[] tex=Assets.tinted(tri.material(),level,color);byte[] lookup=Assets.indices[tri.material()][level];
        for(int y=y0;y<=y1;y++,wr+=B,qr+=E,zr+=zy,ur+=uy,vr+=vy){double w=wr,q=qr,z=zr,u=ur,v=vr;int index=y*r.width+x0;
            for(int x=x0;x<=x1;x++,index++,w+=A,q+=D,z+=zx,u+=ux,v+=vx)if(w>=-.00001&&q>=-.00001&&w+q<=1.00001&&z>r.depth[index]){double inv=size/z;int tx=(int)Math.floor(u*inv)&mask,ty=(int)Math.floor(v*inv)&mask;r.pixels[index]=tex[lookup[ty*size+tx]&255];r.depth[index]=(float)z;}}
>>>>>>> a28a0d3591e35d0c3bb500da202ff4a47e878941
    }
}
