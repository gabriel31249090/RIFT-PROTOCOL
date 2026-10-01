package rift;

import java.awt.image.*;
import java.util.*;
import static rift.World.*;
import rift.Game.Smoke;

/** Perspective triangle renderer with near-plane clipping and reciprocal-depth buffer. */
final class Renderer {
    BufferedImage image;int width,height;int[] pixels;float[] depth;
    double cx,cy,cz,yaw,pitch,sinY,cosY,sinP,cosP,focal;
    final List<Tri> dynamic=new ArrayList<>(2500);
    final TriangleSpan span=new TriangleSpan();SceneMesh scene;
    final int[] smokeOpacity=new int[4096];final float[] smokeNoise=new float[128*128];
    static final BufferedImage SKY=EnvironmentArt.sky();
    static final int[] SKY_PIXELS=SKY.getRGB(0,0,SKY.getWidth(),SKY.getHeight(),null,0,SKY.getWidth());
    int triangleCount;double far=210;boolean shadows=true,nearSight,textures=true;
    Renderer(int w,int h){resize(w,h);for(int i=0;i<smokeOpacity.length;i++)smokeOpacity[i]=(int)(255*(1-Math.exp(-i*.018)));double period=Math.PI*2/128;for(int y=0;y<128;y++)for(int x=0;x<128;x++)smokeNoise[y*128+x]=(float)(Math.sin(x*period*4+Math.sin(y*period*2)*2)*.35+Math.cos(y*period*5+Math.sin(x*period*3))*.25+Math.sin((x+y)*period*7)*.10);}
    void resize(int w,int h){if(w==width&&h==height)return;width=w;height=h;image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);pixels=((DataBufferInt)image.getRaster().getDataBuffer()).getData();depth=new float[w*h];}
    void camera(double x,double y,double z,double yaw,double pitch,double fov) {
        cx=x;cy=y;cz=z;this.yaw=yaw;this.pitch=pitch;sinY=Math.sin(yaw);cosY=Math.cos(yaw);sinP=Math.sin(pitch);cosP=Math.cos(pitch);focal=width/(2*Math.tan(fov/2));
    }
    boolean menu(Game game){return game.player==null||game.ui.equals("menu")||game.ui.equals("agents")||(game.ui.equals("settings")&&game.backUi.equals("menu"));}
    BufferedImage render(Game game) {
        boolean menu=menu(game);Game.Actor cam=game.cameraActor();far=game.settings.renderDistance;textures=game.settings.textures;shadows=game.settings.shadows;nearSight=!menu&&cam!=null&&cam.nearSight>0;
        if(menu)camera(23,3.15,31,3.30+Math.sin(game.visualTime*.07)*.10,-.07,Math.toRadians(84));
        else {
            V e=cam.eye();double bob=!game.settings.lowMotion&&cam.moveSpeed>.1&&cam.grounded?Math.sin(cam.walk*2.4)*.027:0;
            double dash=cam==game.player&&game.dashTime>0?Math.sin(Math.PI*(1-game.dashTime/.4)):0;
            double fov=Math.toRadians(game.settings.fov+(game.settings.lowMotion?0:dash*11)-(cam==game.player?game.aimLerp*cam.gun().kind.zoom:0));
            double death=cam.dead?Smoke.smooth(cam.deathAge/.8):0;
            camera(e.x(),e.y()+bob-death*.95-(cam==game.player?game.landing*.06:0),e.z(),cam.yaw+(cam==game.player&&!cam.melee()?cam.gun().yawRecoil:0),cam.pitch+(cam==game.player&&!cam.melee()?cam.gun().pitchRecoil+(game.settings.lowMotion?0:game.viewKick*.025):0)-death*.15,fov);
        }
        floor(game.world);triangleCount=0;
        if(scene==null||scene.world!=game.world||scene.sourceCount!=game.world.triangles.size())scene=new SceneMesh(game.world);
        scene.render(this);
        for(Decal decal:game.world.decals)decal(decal);
        dynamic.clear();
        if(!menu){
            for(Game.Actor actor:game.actors)if(actor!=cam)actor(actor,game.visualTime);
            abilityGeometry(game);game.shotFX.draw(this);if(!game.flow.mode.respawn||game.training)game.orbs.draw(this,game.visualTime);
            for(Game.Pulse p:game.pulses)ring(p.at.x(),.11,p.at.z(),p.radius(),.07,p.color);
            for(Game.Orbital o:game.orbitals){
                ring(o.x,.13,o.z,5.5,.14,o.active()?0xFFE6AB:0xE69764);ring(o.x,.14,o.z,5.5*Math.min(1,o.age/1.2),.08,0xFFC888);
                if(o.active()){
                    addBox(dynamic,-.34,0,-.34,.34,18,.34,0xFFEEB6,o.x,0,o.z,game.visualTime*2);
                    for(int i=0;i<8;i++){double angle=i*Math.PI/4+game.visualTime*1.8;double r=3.8+.5*Math.sin(i+game.visualTime*4);addBox(dynamic,-.07,0,-.07,.07,9+3*Math.sin(i),.07,0xFFC274,o.x+Math.sin(angle)*r,0,o.z+Math.cos(angle)*r,angle);}
                }
            }
            
            if(game.planted) {
                addBox(dynamic,-.25,0,-.25,.25,.55,.25,0x273F4B,game.spikeX,0,game.spikeZ,game.visualTime*.5);
                addBox(dynamic,-.12,.5,-.12,.12,.85,.12,((int)(game.visualTime*5)%2==0)?0xFF755C:0xF4D7A0,game.spikeX,0,game.spikeZ,game.visualTime*.5);
            }else if(!game.duel&&game.phase==Game.Phase.LIVE&&game.actors.stream().noneMatch(a->a.carrier&&!a.dead)){
                addBox(dynamic,-.28,.05,-.2,.28,.25,.2,0xEED0A3,game.spikeX,0,game.spikeZ,0);
            }
        }
        for(Tri tri:dynamic)worldTriangle(tri);
        if(!menu){
            for(Game.Particle p:game.particles)EffectSprites.particle(this,p);
            for(Game.Trace trace:game.traces)line(trace.from,trace.to,trace.color);
            ArrayList<Game.Smoke> sorted=new ArrayList<>(game.smokes);sorted.sort(Comparator.comparingDouble((Game.Smoke s)->Math.hypot(s.x-cx,s.z-cz)).reversed());
            for(Game.Smoke smoke:sorted)volumeSmoke(smoke,game.visualTime);
            if(!game.observing&&cam==game.player&&!game.player.dead&&game.phase!=Game.Phase.MATCH&&!game.ui.equals("tactical"))weapon(game);
        }
        return image;
    }
    void floor(World world) {
        double halfW=width*.5,halfH=height*.5;
        int[] skyU=new int[width];for(int x=0;x<width;x++)skyU[x]=Math.floorMod((int)((yaw+Math.atan((x+.5-halfW)/focal))/(Math.PI*2)*SKY.getWidth()),SKY.getWidth());
        for(int y=0;y<height;y++){
            double sy=(halfH-y-.5)/focal,vy=sy*cosP+sinP,vf=cosP-sy*sinP;
            double t=vy<-.00001?-cy/vy:-1;int row=y*width;
            if(t>0&&t<far){
                double dx=cosY*t/focal,dz=-sinY*t/focal;
                double wx=cx+t*((.5-halfW)/focal*cosY+vf*sinY),wz=cz+t*(-(.5-halfW)/focal*sinY+vf*cosY);
                int fog=(int)Math.min(210,Math.max(0,(t-12)*1.7));float inv=(float)(1/t);
                for(int x=0;x<width;x++,wx+=dx,wz+=dz){
                    int tx=(int)(wx*world.texScale),tz=(int)(wz*world.texScale),col=0x879FA3;
                    if(tx>=0&&tz>=0&&tx<world.texW-1&&tz<world.texH-1){int index=tz*world.texW+tx;col=world.texture[index];if(t<40){int fx=(int)((wx*world.texScale-tx)*256),fz=(int)((wz*world.texScale-tz)*256);col=lerp(lerp(col,world.texture[index+1],fx),lerp(world.texture[index+world.texW],world.texture[index+world.texW+1],fx),fz);}}
                    pixels[row+x]=nearSight?blend(col,0x272637,(int)Settings.clamp((t-2)*55,0,255)):blend(col,0xC3D1C5,fog);depth[row+x]=inv;
                }
            }else{
                int skyV=(int)Settings.clamp((.5-Math.atan2(vy,vf)/Math.PI)*SKY.getHeight(),0,SKY.getHeight()-1),skyRow=skyV*SKY.getWidth();
                for(int x=0;x<width;x++)pixels[row+x]=SKY_PIXELS[skyRow+skyU[x]];Arrays.fill(depth,row,row+width,0);
            }
        }
    }
    V transform(V p){double x=p.x()-cx,y=p.y()-cy,z=p.z()-cz,f=x*sinY+z*cosY;return new V(x*cosY-z*sinY,y*cosP-f*sinP,y*sinP+f*cosP);}
    BufferedImage preview(Game.Weapon weapon,double time){return collectionPreview(weapon,null,Cosmetics.Skin.STANDARD,Cosmetics.Charm.NONE,time);}
    BufferedImage collectionPreview(Game.Weapon weapon,Cosmetics.Melee melee,Cosmetics.Skin skin,Cosmetics.Charm charm,double time){
        for(int y=0;y<height;y++){int col=blend(0x203E4B,0x122B3B,(int)(y*180./height));Arrays.fill(pixels,y*width,(y+1)*width,col);}Arrays.fill(depth,0);
        java.awt.Graphics2D g=image.createGraphics();g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new java.awt.Color(64,100,111,80));g.drawOval(31,26,width-62,height-44);g.drawLine(15,height-31,width-15,height-31);g.dispose();
        dynamic.clear();double angle=-Math.PI/2-.36+Math.sin(time*.4)*.16;if(melee!=null){Cosmetics.melee(dynamic,melee,skin,0,0,0,angle,1);for(int i=0;i<dynamic.size();i++){Tri t=dynamic.get(i);dynamic.set(i,t.at(tiltBlade(t.a()),tiltBlade(t.b()),tiltBlade(t.c())));}}else WeaponModel.add(dynamic,weapon,0,0,0,angle,1,0,1,skin,charm,Math.sin(time*2)*.13,Math.cos(time*1.8)*.16);
        double minX=1e9,minY=1e9,minZ=1e9,maxX=-1e9,maxY=-1e9,maxZ=-1e9;
        for(Tri t:dynamic)for(V p:new V[]{t.a(),t.b(),t.c()}){minX=Math.min(minX,p.x());maxX=Math.max(maxX,p.x());minY=Math.min(minY,p.y());maxY=Math.max(maxY,p.y());minZ=Math.min(minZ,p.z());maxZ=Math.max(maxZ,p.z());}
        V center=new V((minX+maxX)/2,(minY+maxY)/2,(minZ+maxZ)/2);double tangent=Math.tan(Math.toRadians(22)),distance=Math.max((maxX-minX)*.60/tangent,(maxY-minY)*.63/(tangent*height/width))+(maxZ-minZ)*.5+.16;
        camera(0,.16,-distance,0,-Math.atan2(.16,distance),Math.toRadians(44));for(Tri t:dynamic)worldTriangle(t.at(t.a().sub(center),t.b().sub(center),t.c().sub(center)));return image;
    }
    V tiltBlade(V p){return new V(p.x(),p.y()*Math.cos(.85)-p.z()*Math.sin(.85),p.y()*Math.sin(.85)+p.z()*Math.cos(.85));}
    void decal(Decal d){
        V u=d.b().sub(d.a()),v=d.d().sub(d.a()),eye=new V(cx,cy,cz).sub(d.a());V normal=new V(u.y()*v.z()-u.z()*v.y(),u.z()*v.x()-u.x()*v.z(),u.x()*v.y()-u.y()*v.x());if(normal.dot(eye)<=0)return;
        V a=transform(d.a()),b=transform(d.b()),c=transform(d.c()),e=transform(d.d());if(Math.min(Math.min(a.z(),b.z()),Math.min(c.z(),e.z()))<.1)return;
        textured(a,b,c,0,0,1,0,1,1,d.image());textured(a,c,e,0,0,1,1,0,1,d.image());
    }
    void textured(V a,V b,V c,double ua,double va,double ub,double vb,double uc,double vc,BufferedImage texture){
        double ax=width*.5+a.x()*focal/a.z(),ay=height*.5-a.y()*focal/a.z(),bx=width*.5+b.x()*focal/b.z(),by=height*.5-b.y()*focal/b.z(),xx=width*.5+c.x()*focal/c.z(),yy=height*.5-c.y()*focal/c.z();
        double area=(bx-ax)*(yy-ay)-(by-ay)*(xx-ax);if(Math.abs(area)<.1)return;
        int minX=Math.max(0,(int)Math.floor(Math.min(ax,Math.min(bx,xx)))),maxX=Math.min(width-1,(int)Math.ceil(Math.max(ax,Math.max(bx,xx)))),minY=Math.max(0,(int)Math.floor(Math.min(ay,Math.min(by,yy)))),maxY=Math.min(height-1,(int)Math.ceil(Math.max(ay,Math.max(by,yy))));
        double ia=1/area,a0=(by-yy)*ia,b0=(xx-bx)*ia,c0=(bx*yy-by*xx)*ia,a1=(yy-ay)*ia,b1=(ax-xx)*ia,c1=(xx*ay-yy*ax)*ia,za=1/a.z(),zb=1/b.z(),zc=1/c.z();
        int[] tex=((DataBufferInt)texture.getRaster().getDataBuffer()).getData();int tw=texture.getWidth(),th=texture.getHeight(),fog=(int)Settings.clamp(((a.z()+b.z()+c.z())/3-12)*1.7,0,215);
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
            double w0=a0*(x+.5)+b0*(y+.5)+c0,w1=a1*(x+.5)+b1*(y+.5)+c1,w2=1-w0-w1;if(w0<0||w1<0||w2<0)continue;
            double iz=w0*za+w1*zb+w2*zc;int index=y*width+x;if(iz<depth[index]-.000001)continue;
            double u=(w0*ua*za+w1*ub*zb+w2*uc*zc)/iz,vv=(w0*va*za+w1*vb*zb+w2*vc*zc)/iz;int tx=(int)Settings.clamp(u*(tw-1),0,tw-1),ty=(int)Settings.clamp(vv*(th-1),0,th-1);
            pixels[index]=nearSight?blend(tex[ty*tw+tx],0x272637,(int)Settings.clamp((1/iz-2)*55,0,255)):blend(tex[ty*tw+tx],0xC3D1C5,fog);depth[index]=(float)iz;
        }
    }
    void worldTriangle(Tri tri) {
        if(textures&&tri.material()>=0){TextureRaster.draw(this,tri);return;}
        double ax=tri.a().x()-cx,ay=tri.a().y()-cy,az=tri.a().z()-cz;
        double bx=tri.b().x()-cx,by=tri.b().y()-cy,bz=tri.b().z()-cz;
        double dx=tri.c().x()-cx,dy=tri.c().y()-cy,dz=tri.c().z()-cz;
        double af=ax*sinY+az*cosY,bf=bx*sinY+bz*cosY,cf=dx*sinY+dz*cosY;
        double x0=ax*cosY-az*sinY,y0=ay*cosP-af*sinP,z0=ay*sinP+af*cosP;
        double x1=bx*cosY-bz*sinY,y1=by*cosP-bf*sinP,z1=by*sinP+bf*cosP;
        double x2=dx*cosY-dz*sinY,y2=dy*cosP-cf*sinP,z2=dy*sinP+cf*cosP;
        if(z0<.09&&z1<.09&&z2<.09||z0>far&&z1>far&&z2>far)return;
        double distance=(z0+z1+z2)/3;
        int fog=(int)Settings.clamp((distance-12)*1.7,0,215);
        int color=blend(tri.color(),0xC3D1C5,fog);
        if(nearSight)color=blend(color,0x272637,(int)Settings.clamp((distance-2)*55,0,255));
        if(z0>=.09&&z1>=.09&&z2>=.09)triangle(x0,y0,z0,x1,y1,z1,x2,y2,z2,color);
        else clip(new V(x0,y0,z0),new V(x1,y1,z1),new V(x2,y2,z2),color);
    }

    void clip(V a,V b,V c,int color){
        if(a.z()>=.09&&b.z()>=.09&&c.z()>=.09){triangle(a,b,c,color);return;}
        V[] in={a,b,c},out=new V[5];int n=0;
        for(int i=0;i<3;i++){
            V p=in[i],q=in[(i+1)%3];boolean pin=p.z()>=.09,qin=q.z()>=.09;
            if(pin)out[n++]=p;
            if(pin!=qin){double t=(.09-p.z())/(q.z()-p.z());out[n++]=new V(p.x()+(q.x()-p.x())*t,p.y()+(q.y()-p.y())*t,.09);}
        }
        for(int i=1;i<n-1;i++)triangle(out[0],out[i],out[i+1],color);
    }
    void triangle(V a,V b,V c,int color){triangle(a.x(),a.y(),a.z(),b.x(),b.y(),b.z(),c.x(),c.y(),c.z(),color);}
    void triangle(double x0,double y0,double z0,double x1,double y1,double z1,double x2,double y2,double z2,int color){
        double ax=width*.5+x0*focal/z0,ay=height*.5-y0*focal/z0;
        double bx=width*.5+x1*focal/z1,by=height*.5-y1*focal/z1;
        double cx=width*.5+x2*focal/z2,cy=height*.5-y2*focal/z2;
        double area=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);if(Math.abs(area)<.01)return;
        int minX=Math.max(0,(int)Math.floor(Math.min(ax,Math.min(bx,cx)))),maxX=Math.min(width-1,(int)Math.ceil(Math.max(ax,Math.max(bx,cx))));
        int minY=Math.max(0,(int)Math.floor(Math.min(ay,Math.min(by,cy)))),maxY=Math.min(height-1,(int)Math.ceil(Math.max(ay,Math.max(by,cy))));
        if(minX>maxX||minY>maxY)return;triangleCount++;
        double ia=1/area,a0=(by-cy)*ia,b0=(cx-bx)*ia,c0=(bx*cy-by*cx)*ia;
        double a1=(cy-ay)*ia,b1=(ax-cx)*ia,c1=(cx*ay-cy*ax)*ia;
        double za=1/z0,zb=1/z1,zc=1/z2;
        double dzx=a0*(za-zc)+a1*(zb-zc),dzy=b0*(za-zc)+b1*(zb-zc);
        double w0row=a0*(minX+.5)+b0*(minY+.5)+c0,w1row=a1*(minX+.5)+b1*(minY+.5)+c1;
        double zrow=zc+w0row*(za-zc)+w1row*(zb-zc);
        span.set(ax,ay,bx,by,cx,cy);
        for(int y=minY;y<=maxY;y++,zrow+=dzy){if(!span.row(y,width))continue;
            double z=zrow+(span.left-minX)*dzx;int offset=y*width+span.left;
            for(int x=span.left;x<=span.right;x++,offset++,z+=dzx)if(z>depth[offset]){depth[offset]=(float)z;pixels[offset]=color;}
        }
    }

    void line(V start,V end,int color){
        V a=transform(start),b=transform(end);if(a.z()<.1&&b.z()<.1)return;
        if(a.z()<.1){double t=(.1-a.z())/(b.z()-a.z());a=a.add(b.sub(a).mul(t));}
        if(b.z()<.1){double t=(.1-b.z())/(a.z()-b.z());b=b.add(a.sub(b).mul(t));}
        double ax=width*.5+a.x()*focal/a.z(),ay=height*.5-a.y()*focal/a.z(),bx=width*.5+b.x()*focal/b.z(),by=height*.5-b.y()*focal/b.z();
        int steps=(int)Math.min(width*2,Math.max(Math.abs(bx-ax),Math.abs(by-ay)));if(steps<1)return;
        for(int i=0;i<=steps;i++){double t=i/(double)steps;int x=(int)(ax+(bx-ax)*t),y=(int)(ay+(by-ay)*t);if(x<0||y<0||x>=width||y>=height)continue;double inv=(1-t)/a.z()+t/b.z();int p=y*width+x;if(inv>=depth[p]-.002)pixels[p]=color;}
    }
    double[] project(V p){V v=transform(p);if(v.z()<.1)return null;return new double[]{(width*.5+v.x()*focal/v.z())/width,(height*.5-v.y()*focal/v.z())/height,v.z()};}
    void actor(Game.Actor a,double time){
        V c=transform(a.center());if(c.z()<-2||c.z()>far||Math.abs(c.x())>Math.max(3,c.z()*1.4))return;
        CharacterModel.add(dynamic,a,time,c.z()<23);
        if(!a.dead&&a.flash>0){for(int i=0;i<3;i++){double t=time*5+i*2.094;addBox(dynamic,-.025,-.025,-.025,.025,.025,.025,0xFFEDB8,a.x+Math.sin(t)*.32,a.y+1.94,a.z+Math.cos(t)*.32,0);}}
        if(!a.dead&&a.energy>0)ring(a.x,a.y+.055,a.z,.48,.035,0xC0F5E3);
        if(!a.dead&&a.invulnerable>0)ring(a.x,a.y+.065,a.z,.55,.04,0xC9EEF1);
    }
    BufferedImage agentPreview(int index,double time){
        for(int y=0;y<height;y++)Arrays.fill(pixels,y*width,(y+1)*width,blend(0x244B62,0x0F2539,(int)(y*160./height)));Arrays.fill(depth,0);
        java.awt.Graphics2D bg=image.createGraphics();bg.setColor(new java.awt.Color(119,165,187,40));for(int i=0;i<8;i++)bg.drawLine(0,height/2+i*28,width,height/2+i*28);bg.fillOval(width/4,height-65,width/2,30);bg.dispose();
        camera(0,1.05,-Math.max(3.65,3.4*width/height),0,-.035,Math.toRadians(37));dynamic.clear();Game.Actor a=new Game.Actor(0,0,"PREVIEW");a.agentIndex=index;a.yaw=a.bodyYaw=Math.PI+.28+Math.sin(time*.35)*.12;a.primary=new Game.Gun(Game.Weapon.ECHO);a.slot=2;a.walk=time*.3;
        CharacterModel.add(dynamic,a,time,true);for(Tri t:dynamic)worldTriangle(t);return image;
    }
    void abilityGeometry(Game g){AbilityArt.draw(this,g);}
    void staticTriangle(Tri t){
        double ux=t.b().x()-t.a().x(),uy=t.b().y()-t.a().y(),uz=t.b().z()-t.a().z(),vx=t.c().x()-t.a().x(),vy=t.c().y()-t.a().y(),vz=t.c().z()-t.a().z();
        // World boxes are wound inward. Remove the hidden face before transforming or rasterizing it.
        if((uy*vz-uz*vy)*(cx-t.a().x())+(uz*vx-ux*vz)*(cy-t.a().y())+(ux*vy-uy*vx)*(cz-t.a().z())>=0)return;
        worldTriangle(t);
    }
    V fall(V p,Game.Actor a,double angle,double t){double x=p.x()-a.x,y=p.y()-a.y,z=p.z()-a.z;return new V(a.x+x*Math.cos(angle)+y*Math.sin(angle),a.y+.23*t+Math.max(-.17,x*-Math.sin(angle)+y*Math.cos(angle)),a.z+z);}
    void limb(Game.Actor a,double x,double y,double z,double width,double length,double thick,double angle,int color){
        V[] p=new V[8];double sy=Math.sin(a.yaw),cy=Math.cos(a.yaw),sa=Math.sin(angle),ca=Math.cos(angle);
        for(int i=0;i<8;i++){double px=x+((i&1)==0?-width/2:width/2),ly=(i&2)==0?-length:0,lz=(i&4)==0?-thick/2:thick/2,py=y+ly*ca-lz*sa,pz=z+ly*sa+lz*ca;p[i]=new V(a.x+px*cy+pz*sy,py,a.z-px*sy+pz*cy);}
        quad(dynamic,p[0],p[1],p[3],p[2],shade(color,.86));quad(dynamic,p[4],p[6],p[7],p[5],shade(color,.71));quad(dynamic,p[0],p[2],p[6],p[4],shade(color,.78));quad(dynamic,p[1],p[5],p[7],p[3],shade(color,.97));quad(dynamic,p[2],p[3],p[7],p[6],shade(color,1.13));quad(dynamic,p[0],p[4],p[5],p[1],shade(color,.55));
    }
    void part(Game.Actor a,double x,double y,double z,double xx,double yy,double zz,int col,double oy){
        if(xx-x>.20&&yy-y>.16)new WeaponModel(dynamic,a.x,oy,a.z,a.yaw,1,col).body(x,y,z,xx,yy,zz,col);
        else addBox(dynamic,x,y,z,xx,yy,zz,col,a.x,oy,a.z,a.yaw);
    }
    void sphere(double x,double y,double z,double radius,int color,double time){
        int rings=7,segments=14;
        for(int i=0;i<rings;i++)for(int j=0;j<segments;j++){
            double p1=-Math.PI/2+i*Math.PI/rings,p2=-Math.PI/2+(i+1)*Math.PI/rings,t1=j*Math.PI*2/segments+time*.04,t2=(j+1)*Math.PI*2/segments+time*.04;
            V a=spherePoint(x,y,z,radius,p1,t1),b=spherePoint(x,y,z,radius,p2,t1),c=spherePoint(x,y,z,radius,p2,t2),d=spherePoint(x,y,z,radius,p1,t2);
            quad(dynamic,a,b,c,d,shade(color,.82+.16*(i/(double)rings)+.04*Math.sin(j*1.7+i)));
        }
    }
    V spherePoint(double x,double y,double z,double r,double p,double t){return new V(x+r*Math.cos(p)*Math.cos(t),y+r*Math.sin(p),z+r*Math.cos(p)*Math.sin(t));}
    void ring(double x,double y,double z,double radius,double thickness,int color){
        if(radius<.03)return;
        for(int i=0;i<48;i++){double a=i*Math.PI/24,b=(i+1)*Math.PI/24;quad(dynamic,new V(x+Math.cos(a)*radius,y,z+Math.sin(a)*radius),new V(x+Math.cos(b)*radius,y,z+Math.sin(b)*radius),new V(x+Math.cos(b)*(radius+thickness),y,z+Math.sin(b)*(radius+thickness)),new V(x+Math.cos(a)*(radius+thickness),y,z+Math.sin(a)*(radius+thickness)),color);}
    }
    void billboard(Game.Particle p){double s=p.size;V right=new V(cosY*s,0,-sinY*s),up=new V(-sinY*sinP*s,cosP*s,-cosY*sinP*s),center=new V(p.x,p.y,p.z);quad(dynamic,center.sub(right).sub(up),center.add(right).sub(up),center.add(right).add(up),center.sub(right).add(up),blend(0x667F83,p.color,(int)(255*Math.min(1,p.life/p.duration*2))));}
    /** Opaque outer shell, clear interior: scene depth keeps objects inside visible. */
    void volumeSmoke(Game.Smoke s,double time){
        double radius=s.radius();if(radius<.04)return;V c=transform(new V(s.x,s.y,s.z));if(c.z()+radius<=.09)return;
        int x0=0,x1=width-1,y0=0,y1=height-1;
        if(c.z()>radius+.1){double span=focal*radius/(c.z()-radius);double centerX=width*.5+c.x()*focal/c.z(),centerY=height*.5-c.y()*focal/c.z();x0=Math.max(0,(int)(centerX-span));x1=Math.min(width-1,(int)(centerX+span));y0=Math.max(0,(int)(centerY-span));y1=Math.min(height-1,(int)(centerY+span));}
        double cc=c.dot(c)-radius*radius;int step=width>=800?2:1;
        for(int y=y0;y<=y1;y+=step){
            double dy=(height*.5-y-step*.5)/focal;
            for(int x=x0;x<=x1;x+=step){
                double dx=(x+step*.5-width*.5)/focal,aa=dx*dx+dy*dy+1,bb=c.x()*dx+c.y()*dy+c.z(),disc=bb*bb-aa*cc;
                if(disc<=0)continue;double root=Math.sqrt(disc),entry=Math.max(.01,(bb-root)/aa),exit=(bb+root)/aa;if(exit<=entry)continue;
                boolean inside=cc<0;double surface=inside?exit:entry;
                double hitX=dx*surface-c.x(),hitY=dy*surface-c.y();int nx=((int)(hitX*13+time*7+64))&127,ny=((int)(hitY*13-time*4+64))&127;double noise=smokeNoise[ny*128+nx];if(s.style==Game.Ability.QUICK_SMOKE)noise+=.35*Math.sin(hitY*7+hitX*2+time*7);else if(s.style==Game.Ability.POISON_CLOUD)noise+=.30*Math.sin(hitX*4)*Math.sin(hitY*5-time*2);else if(s.style==Game.Ability.TOXIC_DOME)noise+=.38*Math.cos(hitX*1.5+hitY*2-time);
                int lighting=(int)Settings.clamp(164+hitY*14+noise*36,80,234),color=blend(shade(s.color,.45),s.color,lighting);
                int edge=(int)Math.min(255,root*220),shellAlpha=Math.min(255,edge);
                for(int py=y;py<Math.min(height,y+step);py++)for(int px=x;px<Math.min(width,x+step);px++){
                    int index=py*width+px;double scene=depth[index]>0?1/depth[index]:1000;
                    if(scene>surface){pixels[index]=blend(pixels[index],color,shellAlpha);}
                    else if(inside){pixels[index]=blend(pixels[index],s.color,13);}
                }
            }
        }
    }
    void weapon(Game game){
        Game.Gun gun=game.player.gun();Game.Weapon kind=gun.kind;
        if(!game.player.melee()&&kind.scoped()&&game.aimLerp>.94)return;
        double oldX=cx,oldY=cy,oldZ=cz,oldYaw=yaw,oldPitch=pitch,oldFocal=focal;
        camera(0,0,0,0,0,Math.toRadians(76));Arrays.fill(depth,0);dynamic.clear();
        if(game.player.melee()){meleeView(game);for(Tri tri:dynamic)worldTriangle(tri);camera(oldX,oldY,oldZ,oldYaw,oldPitch,Math.toRadians(90));focal=oldFocal;return;}
        double inspect=game.combat.inspect>0?Math.sin(Math.PI*(1-game.combat.inspect/2.4)):0;
        double impulse=ShotEffects.kick(gun)*(game.settings.lowMotion?.35:1);
        double ads=game.aimLerp,reloadPhase=gun.reload>0?1-gun.reload/gun.reloadTotal:0,reload=gun.reload>0?Math.sin(reloadPhase*Math.PI):0;
        double bob=Math.sin(game.player.walk*2.4)*.011*(1-ads);
        double idle=Math.sin(game.visualTime*1.8)*.003;
        double ox=.31*(1-ads)+bob+game.swayX,oy=-.27+ads*.13-reload*.16+game.viewKick*.12+impulse*.028-game.weaponEquip*.7-(game.dashTime>0?.16:0)+idle+game.swayY-game.landing*.04,oz=.58-game.viewKick*.22-impulse*.09;
        ox-=inspect*.04;oy+=inspect*.29;oz+=inspect*.50;
        double angle=-.17*(1-ads)+reload*.55+game.swayX-inspect*1.0+impulse*.06;
        double magazineDrop=reloadPhase>.18&&reloadPhase<.72?Math.sin((reloadPhase-.18)/.54*Math.PI)*.25:0;
        double cycle=gun.cooldown>0?1-Math.min(1,gun.cooldown/gun.kind.interval):1;
        if(game.player.slot>=4)AbilityArt.specialGun(dynamic,game.player.slot==5,ox,oy,oz,angle,cycle);else WeaponModel.add(dynamic,kind,ox,oy,oz,angle,1,magazineDrop,cycle,game.profile.skin(kind),game.profile.charm(kind),game.combat.charmAngle,game.combat.charmTwist);
        // Gloved hands and layered sleeves, with the support hand following the magazine.
        WeaponModel hands=new WeaponModel(dynamic,ox,oy,oz,angle,1,game.agent.color);
        hands.body(-.063,-.28,.20,.078,-.13,.35,0x324653);hands.body(-.073,-.51,.07,.089,-.26,.31,game.agent.color);
        hands.box(-.075,-.31,.09,.090,-.272,.29,0x203843);
        for(int i=0;i<3;i++)hands.body(.064,-.22+i*.026,.25,.082,-.205+i*.026,.35,0xA5AFA5);
        if(!kind.sidearm()){double hy=-magazineDrop*.8,hz=-reload*.25;hands.body(-.16,-.24+hy,.55+hz,.009,-.086+hy,.69+hz,0x354E57);hands.body(-.25,-.46+hy,.28+hz,-.12,-.22+hy,.63+hz,game.agent.color);hands.box(-.20,-.27+hy,.46+hz,-.12,-.22+hy,.56+hz,0x203843);}
        if(game.agent==Game.Agent.BRUMA)addBox(dynamic,-.12,-.32,.13,.12,-.27,.26,0xA8E2D0,ox,oy,oz,angle);
        if(game.agent==Game.Agent.ION){addBox(dynamic,-.09,-.38,.10,.10,-.34,.24,0xCBC5FF,ox,oy,oz,angle);addBox(dynamic,-.09,-.47,.13,.10,-.43,.28,0x9694D4,ox,oy,oz,angle);}
        if(game.focus>0){for(int i=0;i<4;i++)addBox(dynamic,-.078,-.045,.23+i*.085,-.073,-.016,.27+i*.085,0xFFE5A8,ox,oy,oz,angle);addBox(dynamic,-.06,.033,.22,.06,.042,.50,0xFFE5A8,ox,oy,oz,angle);}
        if(game.abilityAnim>0){double t=Math.sin(game.abilityAnim/.7*Math.PI);addBox(dynamic,-.25,-.38+t*.08,.40,-.10,-.17+t*.08,.55,game.agent.color,-.36,0,.2,0);addBox(dynamic,-.23,-.17+t*.08,.40,-.11,-.07+t*.08,.53,0xD4BFA2,-.36,0,.2,0);}
        for(Tri tri:dynamic)worldTriangle(tri);
        if(game.player.shotGlow>0){WeaponModel at=new WeaponModel(dynamic,ox,oy,oz,angle,1,0);double z=kind.sidearm()?(kind==Game.Weapon.VEIL?.94:kind==Game.Weapon.TALON?.83:.69):kind==Game.Weapon.HORIZON?1.34:kind.silenced()?1.29:kind.category==Game.Category.SMG?.83:1.12;EffectSprites.draw(this,at.p(0,-.022,z),kind.silenced()?.055:kind.pellets>1?.16:.115,0,.9);}
        camera(oldX,oldY,oldZ,oldYaw,oldPitch,Math.toRadians(90));focal=oldFocal;
    }
    void meleeView(Game g){
        Combat c=g.combat;double t=c.swing>0?c.elapsed/(c.heavy?.88:.46):0;
        double action=c.swing>0?Math.sin(Math.min(1,t*1.6)*Math.PI):0;
        double inspect=c.inspect>0?Math.sin(Math.PI*(1-c.inspect/2.4)):0;
        double bob=Math.sin(g.player.walk*2.5)*.012;
        double ox=.29+bob+g.swayX-(c.heavy?.13:.46)*action,oy=-.26+action*.11-g.weaponEquip*.65+g.swayY-g.landing*.04+inspect*.10;
        double oz=.31+(c.heavy?.38:.10)*action,angle=-.22+action*(c.heavy?.15:1.1)*(c.combo%2==0?-1:1)-inspect*.85;
        ox-=inspect*.12;oz+=inspect*.32;oy+=inspect*.04;
        Cosmetics.Skin skin=Cosmetics.Skin.values()[g.profile.meleeSkin];
        Cosmetics.melee(dynamic,g.profile.blade(),skin,ox,oy,oz,angle,1);
        WeaponModel hands=new WeaponModel(dynamic,ox,oy,oz,angle,1,g.agent.color);
        hands.body(-.055,-.071,.18,.060,.025,.32,0x344E59);hands.body(-.065,-.25,.025,.071,-.055,.24,g.agent.color);hands.box(-.072,-.105,.13,.077,-.07,.25,0x203743);
        for(int i=0;i<3;i++)hands.body(.045,-.037,.20+i*.035,.073,.011,.225+i*.035,0x9CAFA9);
        WeaponModel left=new WeaponModel(dynamic,-.37-bob,-.40-action*.08,.35,-.35,1,g.agent.color);
        left.body(-.06,-.08,.01,.065,.06,.19,g.agent.color);left.body(-.06,.03,.12,.055,.115,.27,0x344E59);
    }
    void wash(int color,double alpha){int amount=(int)(alpha*255);for(int i=0;i<pixels.length;i++)pixels[i]=blend(pixels[i],color,amount);}
    static int lerp(int c,int d,int a){int b=256-a;return (((c&0xff00ff)*b+(d&0xff00ff)*a)&0xff00ff00)>>>8|(((c&0xff00)*b+(d&0xff00)*a)&0xff0000)>>>8;}
    static int blend(int c,int d,int a){int b=255-a;int r=(((c>>16)&255)*b+((d>>16)&255)*a)/255,g=(((c>>8)&255)*b+((d>>8)&255)*a)/255,bl=((c&255)*b+(d&255)*a)/255;return(r<<16)|(g<<8)|bl;}
}
