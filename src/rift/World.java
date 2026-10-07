package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

final class World {
    static final double WIDTH = 144, LENGTH = 128;
    static final int NW=144,NH=128;
    static final String[] NAMES={"CAIS-7","MONTE AURORA","FERROVIA","FENDA","PÁTIO ZERO","GALERIA"};
    static final String[] DETAILS={"Porto • passarela leste e docas elevadas","Observatório • duas torres e pátio central","Terminal • viaduto e plataformas de carga","1v1 • três passagens e coberturas","1v1 • passarelas e rampas","1v1 • corredores de precisão"};
    final int mapIndex;
    record V(double x, double y, double z) {
        V add(V b) { return new V(x+b.x, y+b.y, z+b.z); }
        V sub(V b) { return new V(x-b.x, y-b.y, z-b.z); }
        V mul(double k) { return new V(x*k,y*k,z*k); }
        double length() { return Math.sqrt(x*x+y*y+z*z); }
        V unit() { double l=length(); return l < 1e-8 ? new V(0,0,1) : mul(1/l); }
        double dot(V b) { return x*b.x+y*b.y+z*b.z; }
    }
    record Box(double x1, double y1, double z1, double x2, double y2, double z2, int color) { }
    record Tri(V a,V b,V c,int color,int material,double ua,double va,double ub,double vb,double uc,double vc) {
        Tri(V a,V b,V c,int color){this(a,b,c,color,-1,0,0,0,0,0,0);}
        Tri at(V aa,V bb,V cc){return new Tri(aa,bb,cc,color,material,ua,va,ub,vb,uc,vc);}
    }
    int posterCount;int textureMaterial=Assets.CONCRETE;
    static Tri texture(Tri t,int material,double scale){
        V u=t.b.sub(t.a),v=t.c.sub(t.a);double nx=Math.abs(u.y*v.z-u.z*v.y),ny=Math.abs(u.z*v.x-u.x*v.z),nz=Math.abs(u.x*v.y-u.y*v.x);
        boolean floor=ny>=nx&&ny>=nz,side=nx>nz;V a=t.a,b=t.b,c=t.c;
        return new Tri(a,b,c,t.color,material,(floor?a.x:side?a.z:a.x)*scale,(floor?a.z:-a.y)*scale,(floor?b.x:side?b.z:b.x)*scale,(floor?b.z:-b.y)*scale,(floor?c.x:side?c.z:c.x)*scale,(floor?c.z:-c.y)*scale);
    }
    void paint(int start,int material,double scale){for(int i=start;i<triangles.size();i++)triangles.set(i,texture(triangles.get(i),material,scale));}
    record Decal(V a,V b,V c,V d,BufferedImage image) { }
    record Site(String name, double x, double z) { boolean contains(double px, double pz) { return Math.hypot(px-x,pz-z)<6.0; } }
    final List<Box> penetrable=new ArrayList<>();
    final Map<Box,Ballistics.Material> coverMaterials=new HashMap<>();
    final Map<Box,Integer> surfaceMaterials=new HashMap<>();
    Ballistics.Material material(Box box){return coverMaterials.getOrDefault(box,penetrable.contains(box)?Ballistics.Material.WOOD:Ballistics.Material.CONCRETE);}
    int visualMaterial(Box box){return surfaceMaterials.getOrDefault(box,material(box).texture);}
    final List<Box> solids = new ArrayList<>();
    final List<Tri> triangles = new ArrayList<>();
    final List<Decal> decals = new ArrayList<>();
    final Site a,b;
    record Ramp(double x1,double z1,double x2,double z2,double height,boolean reverse) {
        boolean contains(double x,double z){return x>=x1&&x<=x2&&z>=z1&&z<=z2;}
        double at(double z){return height*(reverse?1-(z-z1)/(z2-z1):(z-z1)/(z2-z1));}
    }
    final List<Ramp> ramps=new ArrayList<>();
    final List<Box> platforms=new ArrayList<>(),temporary=new ArrayList<>();
    boolean shadows=true;BufferedImage ground;
    int[] texture;
    final int texScale=16,texW=NW*texScale,texH=NH*texScale;
    final boolean[] nav = new boolean[NW*NH];
    final double[] navHeight=new double[NW*NH];
    final int[] previous=new int[NW*NH],queue=new int[NW*NH];

    void setShadows(boolean enabled){if(shadows==enabled)return;shadows=enabled;ground=EnvironmentArt.ground(this);texture=ground.getRGB(0,0,texW,texH,null,0,texW);}
    String callout(double x,double z){if(z>110)return "BASE ATACANTE";if(z<10)return "BASE DEFENSORA";if(x<30&&z<33)return yName("A");if(x>108&&z<41)return yName("B");if(x<40)return z>80?"ENTRADA A":"CORREDOR A";if(x>103)return z>80?"ENTRADA B":"CORREDOR B";if(z>83)return "CONEXÃO SUL";if(z<43)return "ROTA DEFENSORA";return x<60?"OFICINA":"MEIO";}
    String yName(String site){return "PONTO "+site;}
    World(){this(0);}
    World(int mapIndex) {
        this.mapIndex=Math.floorMod(mapIndex,6);
        a=new Site("A",this.mapIndex==0?13:18,this.mapIndex==2?27:17);
        b=new Site("B",this.mapIndex==1?122:119,this.mapIndex==2?30:24);
        solid(-1,0,-1,WIDTH+1,6,0,0xC5B79F); solid(-1,0,LENGTH,WIDTH+1,6,LENGTH+1,0xC5B79F);
        solid(-1,0,0,0,6,LENGTH,0xC2B69D); solid(WIDTH,0,0,WIDTH+1,6,LENGTH,0xC2B69D);
        if(this.mapIndex>=3)DuelMaps.build(this);else new TacticalLayout(this).build();
        for (int z=0;z<NH;z++) for(int x=0;x<NW;x++) {double h=surfaceAt(x+.5,z+.5);navHeight[z*NW+x]=h;nav[z*NW+x]=!blocked(x+.5,z+.5,h,.34,1.7);}
        ground=EnvironmentArt.ground(this);texture=ground.getRGB(0,0,texW,texH,null,0,texW);
    }
    /** New playable districts retain human-scale cover instead of stretching old meshes. */
    void expandedSectors(){
        int stone=mapIndex==1?0x9BAFC2:mapIndex==2?0xBAA183:0xB5BBA5;
        int cargo=mapIndex==1?0x818EA9:mapIndex==2?0xA77561:0x628B95;
        // South staging area, three exits and two cross-links at z=78 and z=110.
        building(15,85,35,106,7.8,stone);building(48,86,61,102,7.1,cargo);
        building(82,86,100,105,7.5,stone);building(114,82,131,103,7.8,cargo);
        crate(39,96,3,3,1.2);crate(71,109,4,3,1.25);crate(104,91,3,4,1.2);
        container(4,114,12,119,2.6,cargo);container(132,112,141,116,2.7,cargo);
        // Mid connector with an accessible overlook and cover below its approach.
        platform(64,82,77,90,3.6,stone);ramp(67,90,73,105,3.6,true,0xB7BCAB);
        crate(65,78,2,2,1.2);crate(77,106,2,3,1.2);
        // East district: long approach and an interior lane, with gaps for rotation.
        building(98,9,107,42,8.3,stone);building(98,52,110,70,7.2,cargo);
        building(119,54,131,69,6.8,stone);
        container(91,73,97,77,2.6,cargo);crate(113,46,3,3,1.2);
        crate(132,40,3,3,1.2);crate(121,17,3,3,2.6);
        platform(135,12,142,27,4.2,cargo);ramp(135,27,142,44,4.2,true,0xB4B7A6);
        if(mapIndex==0){
            container(114,34,123,37,2.7,0xCB8062);container(88,16,94,22,2.7,cargo);
            building(3,71,17,78,6.5,0x93A9A4);
            EnvironmentArt.signZ(this,"B / ESTALEIRO",120,3,1,17,1.8,0xA4D8C5);
        }else if(mapIndex==1){
            platform(113,34,129,42,2.4,stone);ramp(116,42,123,52,2.4,true,0xB9C9D0);
            building(33,69,50,76,7,0xABBBC8);crate(126,27,2,2,1.2);
            EnvironmentArt.signZ(this,"B / ANTENA LESTE",122,3,1,19,1.8,0xC2DBF0);
        }else{
            container(112,39,124,44,2.8,0xB28061);container(84,44,92,49,2.8,cargo);
            platform(40,70,52,78,2.4,stone);ramp(40,78,47,88,2.4,true,stone);
            for(double x:new double[]{115,117})box(x,.014,1,x+.1,.03,126,0x435965);
            EnvironmentArt.signZ(this,"B / LINHA 04",120,3,1,18,1.8,0xE7BF88);
        }
        EnvironmentArt.signZ(this,"BASE / ATAQUE",70,2.4,LENGTH-.2,21,2,0xA2D8C1);
        EnvironmentArt.signZ(this,"CONEXAO / MEIO",70,4.1,82.03,12,1.2,0xE2C29A);
        EnvironmentArt.signX(this,"B / LESTE",97.98,3,60,10,1.6,0xA5D8D1,false);
    }
    void harbor(){
        building(24,9,29,25,7.5,0xDBCBB0); building(35,9,40,25,7.5,0x82A5AC);
        building(7,33,22,45,6.8,0xD4C2A2); building(42,33,57,45,6.8,0x9FB8B1);
        building(1,1,7,8,9,0x799194); building(57,1,63,8,10,0x7D959C);
        container(7,22,15,25,2.7,0xCE755C); container(53,23,59,26,2.7,0x698D94);
        container(27,39,30,45,2.6,0x547B88); container(34,39,37,45,2.6,0xBD7C62);
        crate(10,13,3.4,3.2,2.5); crate(17,24,2.5,2,1.2);
        crate(47,14,3.6,3.6,2.8); crate(44,24,2.5,2,1.2);
        crate(29,29,2,2,1.15); crate(35,30,2,2,1.15);
        crate(3,39,2,3,1.25); crate(59,36,2,3,1.25);
        crate(13,49,3,2,1.2); crate(48,49,3,2,1.2);
        // Overhead bridges, canopy and lamps are visible geometry, not navigation walls.
        box(24,5.4,25,40,5.8,27,0x445F70); box(29,5.8,25.5,35,6.1,26.5,0xF6B47B);
        for (double x : new double[]{3,22,42,61}) {
            box(x,0,28,x+.18,5.8,28.18,0x3B515E);
            box(x-.3,5.6,27.7,x+.6,5.9,28.5,0xDDEDDC);
        }
        // Distant industrial silhouettes.
        for (int i=0;i<10;i++) box(-14+i*10,0,-18-(i%3)*4,-7+i*10,8+(i%4)*4,-8-(i%3)*4,0x667C90);
        EnvironmentArt.decorate(this);
        building(10,59,24,68,7,0x93ABA5);building(48,59,61,68,7,0xD3AD89);
        container(80,35,86,40,2.6,0xA77861);container(72,58,82,62,2.6,0x648B9C);
        platform(68,12,82,22,3.2,0x849D9B);ramp(72,22,78,34,3.2,true,0xC4B898);
        platform(2,6,7,13,2.4,0x9FA894);ramp(2,13,7,23,2.4,true,0xB8AC91);
        platform(30,49,40,56,2.4,0x7F959A);ramp(32,56,38,66,2.4,true,0xADB69D);
        EnvironmentArt.signZ(this,"DOCA ALTA  /  +3.2",75,3.4,12.02,9,1.4,0xDDC19A);
        EnvironmentArt.signZ(this,"CAIS-7  /  SUL",35,3,127.8,12,1.7,0xD4C3A2);
    }
    void observatory(){
        building(28,10,37,30,8,0xBBC5D1);building(51,10,60,30,8,0x9EACBE);
        building(15,48,31,60,6,0xB8B0BA);building(57,48,73,60,6,0x849DB7);
        building(40,40,48,46,9,0xCFBEA4);
        platform(5,16,13,30,4,0x7895AB);ramp(5,30,13,46,4,true,0xAABAC4);
        platform(75,16,83,30,4,0x7895AB);ramp(75,30,83,46,4,true,0xAABAC4);
        platform(37,22,51,28,2.4,0x9AADB9);ramp(40,28,48,38,2.4,true,0xB4C4CA);
        for(double[] p:new double[][]{{18,22},{63,23},{19,38},{65,39},{39,58},{47,58}})crate(p[0],p[1],3,2,1.2);
        for(int x=2;x<88;x+=12){box(x,0,-9,x+7,12+(x%5),-3,0x7896AE);box(x,5.7,.05,x+6,5.9,.12,0xD6DADE);}
        EnvironmentArt.signZ(this,"AURORA / OBSERVATORIO",44,5,1,26,2,0xB8D8F4);
        EnvironmentArt.signZ(this,"A / TELESCOPIO",18,3,1,14,1.5,0xDFBB99);
        EnvironmentArt.signZ(this,"MEIO / LABORATORIO",70,3,1,14,1.5,0xA2DBCF);
    }
    void railway(){
        building(25,5,35,20,7,0xB48D72);building(53,5,63,20,7,0x849586);
        building(3,49,18,60,7,0xA39E8A);building(70,49,85,60,7,0xC4A783);
        for(int i=0;i<4;i++){container(28,27+i*9,35,33+i*9,2.9,0x7B919B);container(53,27+i*9,60,33+i*9,2.9,0xB7795F);}
        platform(36,36,52,44,3.2,0xA18D76);ramp(39,44,49,58,3.2,true,0xBCA788);ramp(39,22,49,36,3.2,false,0xBCA788);
        platform(5,18,13,28,2.4,0xB9AE97);ramp(5,28,13,38,2.4,true,0xBCB798);
        platform(75,18,83,28,2.4,0xB9AE97);ramp(75,28,83,38,2.4,true,0xBCB798);
        crate(19,34,3,3,1.2);crate(65,35,3,3,1.2);crate(39,65,3,3,1.2);crate(49,65,3,3,1.2);
        for(double x:new double[]{23,25,63,65})box(x,.012,1,x+.11,.03,79,0x435965);
        EnvironmentArt.signZ(this,"FERROVIA / TERMINAL 03",44,4,1,26,2,0xE5C28C);
        EnvironmentArt.signZ(this,"A / CARGA",17,3,1,12,1.4,0xE4B685);EnvironmentArt.signZ(this,"MEIO / CARGA",71,3,1,12,1.4,0xB6CEC1);
    }
    void platform(double x,double z,double xx,double zz,double h,int color){solid(x,0,z,xx,h,zz,color);platforms.add(solids.get(solids.size()-1));}
    void ramp(double x,double z,double xx,double zz,double h,boolean reverse,int color){
        int start=triangles.size();Ramp r=new Ramp(x,z,xx,zz,h,reverse);ramps.add(r);double lo=reverse?h:0,hi=reverse?0:h;
        V a=new V(x,lo,z),b=new V(xx,lo,z),c=new V(xx,hi,zz),d=new V(x,hi,zz);
        quad(triangles,a,b,c,d,color);quad(triangles,d,c,b,a,color);
        quad(triangles,new V(x,0,z),a,d,new V(x,0,zz),shade(color,.8));quad(triangles,new V(xx,0,z),new V(xx,0,zz),c,b,shade(color,.85));
        paint(start,Assets.PLATE,.45);
        for(double t=.8;t<zz-z;t+=2){double y=r.at(z+t)+.015;box(x,y,z+t,xx,y+.016,z+t+.07,shade(color,1.14));}
    }
    double surfaceAt(double x,double z){double h=0;for(Ramp r:ramps)if(r.contains(x,z))h=Math.max(h,r.at(z));for(Box b:platforms)if(x>=b.x1&&x<=b.x2&&z>=b.z1&&z<=b.z2)h=Math.max(h,b.y2);return h;}
    boolean obstacle(Box b,double x,double z,double y,double radius,double height){
        if(y>=b.y2-.32||y+height<=b.y1+.02)return false;
        double dx=x-Math.max(b.x1,Math.min(b.x2,x)),dz=z-Math.max(b.z1,Math.min(b.z2,z));return dx*dx+dz*dz<radius*radius;
    }
    void solid(double x1,double y1,double z1,double x2,double y2,double z2,int col) {
        Box solid=new Box(x1,y1,z1,x2,y2,z2,col);solids.add(solid);int start=triangles.size();box(x1,y1,z1,x2,y2,z2,col);
        surfaceMaterials.put(solid,triangles.get(start).material());
    }
    void building(double x1,double z1,double x2,double z2,double h,int col) {
        int previousMaterial=textureMaterial;textureMaterial=mapIndex==2?Assets.BRICK:mapIndex==1?Assets.STONE:Assets.PLASTER;
        solid(x1,0,z1,x2,h,z2,col);
        box(x1-.1,h-.22,z1-.1,x2+.1,h+.1,z2+.1,0xD0D5C6);
        box(x1-.025,.05,z1-.025,x2+.025,.5,z2+.025,0x455F6A);
        for(double z=z1+1;z<z2-1;z+=3) {
            box(x1-.04,3,z,x1,4.3,z+1.7,0x395865); box(x2,3,z,x2+.04,4.3,z+1.7,0x395865);
            box(x1-.06,3,z,x1-.04,3.12,z+1.7,0xA5DAD2); box(x2+.04,3,z,x2+.06,3.12,z+1.7,0xA5DAD2);
        }
        for(double x=x1+1;x<x2-1;x+=3) {
            box(x,2.7,z1-.04,x+1.8,4,z1,0x36586A);box(x,2.7,z2,x+1.8,4,z2+.04,0x36586A);
        }
        box(x1+.7,h,z1+.7,Math.min(x2-.3,x1+2.7),h+.8,z1+2.3,0x425E6D);
        for(double z=z1+.4;z<z2;z+=4){box(x1-.045,.55,z,x1-.015,h-.25,z+.10,shade(col,.83));box(x2+.015,.55,z,x2+.045,h-.25,z+.10,shade(col,.83));}
        box(x1-.06,1.1,z1-.055,x2+.06,1.28,z2+.055,0x68858C);
        // A bright service door anchors each facade.
        double mid=(x1+x2)*.5;box(mid-.85,.5,z2,mid+.85,2.55,z2+.07,0x375466);
        box(mid-.9,2.5,z2+.06,mid+.9,2.62,z2+.11,0xE4B688);
        box(mid+.55,1.10,z2+.07,mid+.62,1.32,z2+.12,0xF0D1A0);
        for(int i=0;i<6;i++)box(mid-.70,.72+i*.24,z2+.072,mid+.40,.74+i*.24,z2+.079,0x5E7983);
        if((posterCount++%5)==1&&x2-x1>6){double center=x1+2.3;decals.add(new Decal(new V(center+1,3.1,z2+.085),new V(center-1,3.1,z2+.085),new V(center-1,1.1,z2+.085),new V(center+1,1.1,z2+.085),Assets.portraits[(posterCount+mapIndex*3)%11]));}
        textureMaterial=previousMaterial;
    }
    void container(double x,double z,double x2,double z2,double h,int col) {
        int previousMaterial=textureMaterial;textureMaterial=(col>>16&255)>(col&255)?Assets.RUST:Assets.TEAL;
        solid(x,0,z,x2,h,z2,col);box(x-.04,h-.12,z-.04,x2+.04,h+.04,z2+.04,shade(col,1.2));
        for(double xx=x+.4;xx<x2;xx+=.65)box(xx,.12,z2,xx+.09,h-.16,z2+.07,shade(col,.78));
        for(double xx:new double[]{x+.15,x2-.20}){box(xx,.05,z2+.05,xx+.10,h-.05,z2+.12,0xCED1BC);box(xx-.06,h*.45,z2+.12,xx+.17,h*.45+.1,z2+.16,0x536A72);}
        for(double zz=z+.3;zz<z2;zz+=.65)box(x2,.12,zz,x2+.07,h-.16,zz+.09,shade(col,.8));
        textureMaterial=previousMaterial;
    }
    void panel(double x,double z,double width,double depth){panel(x,z,width,depth,Ballistics.Material.WOOD);}
    void panel(double x,double z,double width,double depth,Ballistics.Material material){
        int start=triangles.size();
        solid(x,0,z,x+width,2.2,z+depth,material==Ballistics.Material.METAL?0x728694:0x95765B);
        Box cover=solids.get(solids.size()-1);penetrable.add(cover);coverMaterials.put(cover,material);
        for(double xx=x+.12;xx<x+width;xx+=.35)box(xx,.08,z-.01,xx+.03,2.1,z+depth+.01,material==Ballistics.Material.METAL?0xAAC5CC:0xC8AD83);
        paint(start,material.texture,.65);
        surfaceMaterials.put(cover,material.texture);
    }
    void crate(double x,double z,double w,double d,double h) {
        int previousMaterial=textureMaterial;textureMaterial=Assets.WOOD;
        solid(x,0,z,x+w,h,z+d,0x9BA497);box(x-.03,h-.13,z-.03,x+w+.03,h+.03,z+d+.03,0xD4CDA8);
        box(x+.12,.18,z+d,x+.27,h-.12,z+d+.03,0x4E6465);box(x+w-.27,.18,z+d,x+w-.12,h-.12,z+d+.03,0x4E6465);
        box(x+w,.15,z+.16,x+w+.035,h-.15,z+.3,0x445C62);
        textureMaterial=previousMaterial;
    }
    void box(double x1,double y1,double z1,double x2,double y2,double z2,int color) {
        int start=triangles.size();addBox(triangles,x1,y1,z1,x2,y2,z2,color,0,0,0,0);
        int mat=(color==0x395865||color==0x36586A)?Assets.GLASS:color==0xD0D5C6?Assets.ROOF:textureMaterial;
        paint(start,mat,mat==Assets.WOOD?.65:mat==Assets.BRICK?.38:.33);
    }
    static int shade(int c,double f) { return ((int)Math.min(255,((c>>16)&255)*f)<<16)|((int)Math.min(255,((c>>8)&255)*f)<<8)|(int)Math.min(255,(c&255)*f); }
    static void addBox(List<Tri> out,double x1,double y1,double z1,double x2,double y2,double z2,int color,double ox,double oy,double oz,double yaw) {
        V[] p=new V[8];double s=Math.sin(yaw),c=Math.cos(yaw);
        for(int i=0;i<8;i++) {double x=(i&1)==0?x1:x2,y=(i&2)==0?y1:y2,z=(i&4)==0?z1:z2;p[i]=new V(ox+x*c+z*s,oy+y,oz-x*s+z*c);}
        quad(out,p[0],p[1],p[3],p[2],shade(color,.86));quad(out,p[4],p[6],p[7],p[5],shade(color,.71));
        quad(out,p[0],p[2],p[6],p[4],shade(color,.78));quad(out,p[1],p[5],p[7],p[3],shade(color,.97));
        quad(out,p[2],p[3],p[7],p[6],shade(color,1.13));quad(out,p[0],p[4],p[5],p[1],shade(color,.55));
    }
    static void quad(List<Tri> out,V a,V b,V c,V d,int color) {out.add(new Tri(a,b,c,color));out.add(new Tri(a,c,d,color));}
    boolean blocked(double x,double z,double y,double radius,double height) {
        if(x<radius||z<radius||x>WIDTH-radius||z>LENGTH-radius)return true;
        for(Box b:solids)if(obstacle(b,x,z,y,radius,height))return true;
        for(Box b:temporary)if(obstacle(b,x,z,y,radius,height))return true;
        for(Ramp r:ramps)if(r.contains(x,z)&&r.at(z)>y+.32)return true;
        return false;
    }
    double groundAt(double x,double z,double oldY) {
        double surface=surfaceAt(x,z);double floor=surface<=oldY+.35?surface:0;
        for(Box b:solids)if(x>=b.x1&&x<=b.x2&&z>=b.z1&&z<=b.z2&&oldY>=b.y2-.32)floor=Math.max(floor,b.y2);
        return floor;
    }
    double ray(V start,V dir,double max) {
        return ray(start,dir,max,List.of());
    }
    double ray(V start,V dir,double max,Collection<Box> ignoredSolids) {
        double best=max;
        for(Box b:solids) {if(ignoredSolids.contains(b))continue;double t=rayBox(start,dir,b);if(t>=0&&t<best)best=t;}
        for(Box b:temporary){double t=rayBox(start,dir,b);if(t>=0&&t<best)best=t;}
        for(Ramp r:ramps){double slope=(r.reverse?-1:1)*r.height/(r.z2-r.z1),base=r.reverse?r.height:0,den=dir.y-slope*dir.z;
            double entry=rayBox(start,dir,new Box(r.x1,0,r.z1,r.x2,r.height,r.z2,0));
            if(entry>=0&&entry<best&&start.y+dir.y*entry<=r.at(start.z+dir.z*entry)+.001)best=entry;
            if(Math.abs(den)>1e-9){double t=(base+slope*(start.z-r.z1)-start.y)/den;double x=start.x+dir.x*t,z=start.z+dir.z*t;if(t>=0&&t<best&&r.contains(x,z))best=t;}}
        if(dir.y<-.0001) {double t=-start.y/dir.y;if(t>0&&t<best)best=t;}
        return best;
    }
    static double rayBox(V p,V d,Box b) {
        double lo=0,hi=1e6,t1,t2;
        if(Math.abs(d.x)<1e-9){if(p.x<b.x1||p.x>b.x2)return -1;}else{t1=(b.x1-p.x)/d.x;t2=(b.x2-p.x)/d.x;lo=Math.max(lo,Math.min(t1,t2));hi=Math.min(hi,Math.max(t1,t2));if(lo>hi)return -1;}
        if(Math.abs(d.y)<1e-9){if(p.y<b.y1||p.y>b.y2)return -1;}else{t1=(b.y1-p.y)/d.y;t2=(b.y2-p.y)/d.y;lo=Math.max(lo,Math.min(t1,t2));hi=Math.min(hi,Math.max(t1,t2));if(lo>hi)return -1;}
        if(Math.abs(d.z)<1e-9){if(p.z<b.z1||p.z>b.z2)return -1;}else{t1=(b.z1-p.z)/d.z;t2=(b.z2-p.z)/d.z;lo=Math.max(lo,Math.min(t1,t2));hi=Math.min(hi,Math.max(t1,t2));if(lo>hi)return -1;}
        return hi<0?-1:lo;
    }
    boolean visible(V from,V to) {V d=to.sub(from);double len=d.length();return ray(from,d.mul(1/Math.max(.001,len)),len)>=len-.08;}
    Site site(double x,double z) {if(a.contains(x,z))return a;if(b.contains(x,z))return b;return null;}
    // Breadth-first search over a fixed grid. Call only when destinations change or a path expires.
    List<V> path(double sx,double sz,double tx,double tz) {
        int start=nearestCell(sx,sz),goal=nearestCell(tx,tz);
        if(start<0||goal<0)return List.of();
        int[] prev=previous;Arrays.fill(prev,-1);
        int head=0,tail=0;queue[tail++]=start;prev[start]=start;
        while(head<tail && prev[goal]<0) {
            int q=queue[head++],x=q%NW,z=q/NW;
            for(int direction=0;direction<4;direction++){
                int n=switch(direction){case 0->x>0?q-1:-1;case 1->x<NW-1?q+1:-1;case 2->z>0?q-NW:-1;default->z<NH-1?q+NW:-1;};
                if(n>=0&&nav[n]&&prev[n]<0&&Math.abs(navHeight[n]-navHeight[q])<.45&&!temporaryBlocked(n)){prev[n]=q;queue[tail++]=n;}
            }
        }
        if(prev[goal]<0)return List.of();
        ArrayList<V> list=new ArrayList<>();int n=goal;
        while(n!=start){list.add(new V(n%NW+.5,navHeight[n],n/NW+.5));n=prev[n];}
        Collections.reverse(list);return list;
    }
    boolean temporaryBlocked(int cell){if(temporary.isEmpty())return false;double x=cell%NW+.5,z=cell/NW+.5;for(Box b:temporary)if(obstacle(b,x,z,navHeight[cell],.34,1.7))return true;return false;}
    int nearestCell(double x,double z) {
        int ix=(int)Settings.clamp(x,0,NW-1),iz=(int)Settings.clamp(z,0,NH-1);
        if(nav[iz*NW+ix]&&!temporaryBlocked(iz*NW+ix))return iz*NW+ix;
        for(int r=1;r<8;r++)for(int dz=-r;dz<=r;dz++)for(int dx=-r;dx<=r;dx++) {
            int xx=ix+dx,zz=iz+dz;if(xx>=0&&xx<NW&&zz>=0&&zz<NH&&nav[zz*NW+xx]&&!temporaryBlocked(zz*NW+xx))return zz*NW+xx;
        }
        return -1;
    }
}
