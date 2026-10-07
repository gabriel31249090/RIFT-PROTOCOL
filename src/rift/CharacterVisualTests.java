package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import static rift.Game.*;
import static rift.World.*;
import static rift.Tests.check;

/** Rigid visual pilot: animation, bounded geometry and real raster output. */
final class CharacterVisualTests {
    static void run(){
        int meshTriangles=0;
        for(String asset:new String[]{"helmet.obj","chest.obj","impulse-pack.obj"}){
            MeshAssets.Mesh mesh=MeshAssets.load("models/vertice/"+asset);
            meshTriangles+=mesh.triangleCount();
            check(mesh.vertexCount()<=40&&mesh.triangleCount()>0,"VERTICE original OBJ is present and bounded: "+asset);
        }
        check(meshTriangles==88,"VERTICE three rigid meshes stay within the 120-triangle budget");
        Actor a=actor();List<Tri> idle=new ArrayList<>(),low=new ArrayList<>();
        CharacterModel.add(idle,a,0,true);CharacterModel.add(low,a,0,false);
        long textures=idle.stream().filter(t->t.material()>=0).count();
        check(textures>=200,"VERTICE detailed mesh includes fixed UV cloth and rigid materials");
        check(idle.size()<=1200&&low.size()<idle.size()&&low.size()<=600,"VERTICE preserves bounded detailed and distant LOD geometry");
        int[] materials=idle.stream().mapToInt(Tri::material).toArray();
        a.yaw+=.7;a.pitch=.45;
        List<Tri> aimed=new ArrayList<>();CharacterModel.add(aimed,a,0,true);
        check(Arrays.equals(materials,aimed.stream().mapToInt(Tri::material).toArray()),"Head aim preserves imported texture material assignments");
        boolean sameUv=idle.size()==aimed.size();
        for(int i=0;i<idle.size()&&sameUv;i++){
            Tri p=idle.get(i),q=aimed.get(i);
            sameUv=p.ua()==q.ua()&&p.va()==q.va()&&p.ub()==q.ub()&&p.vb()==q.vb()&&p.uc()==q.uc()&&p.vc()==q.vc();
        }
        check(sameUv&&!idle.equals(aimed),"Head rotates real vertices without losing OBJ UVs");
        for(int pose=0;pose<6;pose++){
            Actor state=actor();pose(state,pose);List<Tri> geometry=new ArrayList<>();
            CharacterModel.add(geometry,state,.35,true);
            boolean finite=!geometry.isEmpty();
            for(Tri t:geometry)for(V v:new V[]{t.a(),t.b(),t.c()})
                finite&=Double.isFinite(v.x())&&Double.isFinite(v.y())&&Double.isFinite(v.z())&&Math.abs(v.x())<(state.dead?2.2:1.4)&&v.y()>-.2&&v.y()<2.7&&Math.abs(v.z())<1.5;
            check(finite,"VERTICE rigid pilot follows existing animation bounds, pose "+pose);
        }
        BufferedImage front=render(actor(),320,440);Actor backActor=actor();backActor.yaw=backActor.bodyYaw=.38;
        BufferedImage back=render(backActor,320,440);int changed=0;
        int[] f=front.getRGB(0,0,320,440,null,0,320),b=back.getRGB(0,0,320,440,null,0,320);
        for(int i=0;i<f.length;i++)if(f[i]!=b[i])changed++;
        check(changed>3000,"VERTICE front and back produce distinct nonblank native raster frames");
    }
    static Actor actor(){Actor a=new Actor(0,0,"VERTICE");a.agentIndex=0;a.yaw=a.bodyYaw=Math.PI+.38;return a;}
    static void pose(Actor a,int pose){switch(pose){
        case 1->{a.animSpeed=4.8;a.walk=.45;}
        case 2->{a.crouch=true;a.animCrouch=1;}
        case 3->{a.grounded=false;a.y=.4;}
        case 4->{a.gun().reloadTotal=2.15;a.gun().reload=1.2;}
        case 5->{a.dead=true;a.deathAge=.6;}
    }}
    static BufferedImage render(Actor a,int width,int height){
        Renderer r=new Renderer(width,height);Arrays.fill(r.pixels,0x22292E);
        r.camera(0,1.05,-3.6,0,-.035,Math.toRadians(37));
        CharacterModel.add(r.dynamic,a,.35,true);for(Tri t:r.dynamic)r.worldTriangle(t);
        return r.image;
    }
    static void capture(Path directory)throws Exception{
        Files.createDirectories(directory);
        BufferedImage sheet=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();
        g.setColor(new Color(0x171D21));g.fillRect(0,0,1280,720);
        String[] names={"VERTICE / FRONT","IMPULSE MODULES","CROUCH","RELOAD"};
        for(int i=0;i<4;i++){
            Actor a=actor();if(i==1)a.yaw=a.bodyYaw=.38;else if(i==2)pose(a,2);else if(i==3)pose(a,4);
            BufferedImage frame=render(a,320,620);g.drawImage(frame,i*320,60,null);
            g.setColor(new Color(0xC8E7E3));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));g.drawString(names[i],i*320+18,34);
            ImageIO.write(render(a,320,440),"png",directory.resolve("vertice-"+i+".png").toFile());
        }
        g.dispose();ImageIO.write(sheet,"png",directory.resolve("vertice-pilot-1280x720.png").toFile());
    }
    public static void main(String[] args)throws Exception{run();if(args.length>0)capture(Path.of(args[0]));System.out.println("Character visual checks: "+Tests.passed);}
}
