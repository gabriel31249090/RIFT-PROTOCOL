package rift;

import java.awt.*;
import java.awt.image.*;
import java.nio.file.*;
import java.util.*;
import static rift.World.*;
import static rift.Tests.*;

/** Rendering correctness and frame-budget regressions for 1.6.1. */
final class PerformanceTests {
    static void run()throws Exception{coverage();scene();quality();dynamicGeometry();}
    static void coverage(){
        Renderer r=new Renderer(73,47);r.camera(0,0,0,0,0,Math.toRadians(90));Random random=new Random(931);
        for(int i=0;i<120;i++){
            double ax=random.nextDouble()*150-40,ay=random.nextDouble()*100-28,bx=random.nextDouble()*150-40,by=random.nextDouble()*100-28,cx=random.nextDouble()*150-40,cy=random.nextDouble()*100-28;
            double area=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);if(Math.abs(area)<.1){i--;continue;}
            Arrays.fill(r.depth,0);Arrays.fill(r.pixels,0);r.triangle(screen(r,ax,ay,2),screen(r,bx,by,2),screen(r,cx,cy,2),0xFFFFFF);
            for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++){
                double w=((by-cy)*(x+.5)+(cx-bx)*(y+.5)+bx*cy-by*cx)/area,q=((cy-ay)*(x+.5)+(ax-cx)*(y+.5)+cx*ay-cy*ax)/area;
                if(Math.min(Math.min(Math.abs(w),Math.abs(q)),Math.abs(1-w-q))<1e-7)continue;
                boolean inside=w>=0&&q>=0&&w+q<=1;
                if((r.depth[y*r.width+x]>0)!=inside)throw new AssertionError("Raster perdeu cobertura em "+x+","+y);
            }
        }
        check(true,"Varredura cobre 120 triângulos recortados como referência baricêntrica");
        Arrays.fill(r.depth,0);Arrays.fill(r.pixels,0);r.triangle(screen(r,0,0,3),screen(r,72,0,3),screen(r,0,46,3),0xFF0000);r.triangle(screen(r,0,0,1),screen(r,72,0,1),screen(r,0,46,1),0x00FF00);r.triangle(screen(r,0,0,4),screen(r,72,0,4),screen(r,0,46,4),0x0000FF);
        check(r.pixels[10*r.width+10]==0x00FF00,"Ordem de desenho preserva oclusão pelo objeto mais próximo");
    }
    static V screen(Renderer r,double x,double y,double z){return new V((x-r.width*.5)*z/r.focal,(r.height*.5-y)*z/r.focal,z);}
    static void scene(){
        double[][] poses={{42,1.62,72,1.57,-.05},{68,1.62,120,3.1,.1},{136.5,5.82,17,-1.95,-.2},{40,2,52,-.4,.25}};
        for(int map=0;map<3;map++){
            World w=new World(map);SceneMesh mesh=new SceneMesh(w);Renderer fast=new Renderer(240,135),reference=new Renderer(240,135);
            for(double[] p:poses){for(Renderer r:new Renderer[]{fast,reference}){r.camera(p[0],p[1],p[2],p[3],p[4],Math.toRadians(90));r.floor(w);}
                mesh.render(fast);for(Tri t:w.triangles)reference.staticTriangle(t);
                int changed=0;for(int i=0;i<fast.pixels.length;i++){int a=fast.pixels[i],b=reference.pixels[i];if(Math.abs((a>>16&255)-(b>>16&255))+Math.abs((a>>8&255)-(b>>8&255))+Math.abs((a&255)-(b&255))>24)changed++;}
                check(changed<fast.pixels.length*.012,"Recorte espacial mantém superfícies do mapa "+map+" (diferença "+changed+" pixels)");
            }
        }
        Game game=new Game(new Settings(false),false,817);game.start(true);View v=new View(game);BufferedImage a=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=a.createGraphics();v.minimap(g);g.dispose();game.world=new World(1);g=a.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,0,1280,720);v.minimap(g);int[] changed=a.getRGB(24,22,176,177,null,0,176);g.dispose();
        BufferedImage b=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);g=b.createGraphics();new View(game).minimap(g);g.dispose();
        check(Arrays.equals(changed,b.getRGB(24,22,176,177,null,0,176)),"Troca de mapa invalida terreno do minimapa sem alterar marcadores");game.close();
    }
    static void quality()throws Exception{
        AdaptiveQuality q=new AdaptiveQuality();q.reset(4);for(int i=0;i<12;i++)q.sample(77,120);check(q.level<4,"AUTO reduz resolução em menos de um segundo a 13 FPS");
        for(int i=0;i<120;i++)q.sample(77,120);check(q.width()==384,"AUTO tem resolução de emergência para CPU limitada");
        int level=q.level;for(int i=0;i<30;i++)q.sample(2,120);check(q.level==level,"Breve alívio de carga não oscila resolução");for(int i=0;i<700;i++)q.sample(2,120);check(q.level>level,"AUTO recupera qualidade após folga sustentada");
        q.sample(Double.NaN,120);q.sample(-1,120);check(q.width()>=384&&q.width()<=1066,"Medição inválida não corrompe qualidade");
        Path dir=Files.createTempDirectory("rift161-prefs-");try{Settings s=new Settings(dir.resolve("prefs"));s.sensitivity=.004;s.sniperToggle=false;s.performance();s.smoothUpscale=true;s.save();Settings read=new Settings(dir.resolve("prefs"));check(read.smoothUpscale&&read.quality==3&&read.textures,"Filtro e modo de desempenho persistem mantendo texturas");check(read.sensitivity==.004&&!read.sniperToggle,"Perfil de desempenho preserva controles e escolha de mira");}finally{try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        Game game=new Game(new Settings(false),false,621);game.start(true);game.settings.quality=1;View v=new View(game);BufferedImage frame=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=frame.createGraphics();v.render(g,1280,720);check(v.renderer.width==854,"Qualidade fixa continua respeitando escolha manual");g.dispose();game.close();
    }
    static void dynamicGeometry(){
        Game.Actor actor=new Game.Actor(1,0,"TEST");actor.x=7;actor.y=2;actor.z=11;
        for(double yaw:new double[]{0,.7,2.9}){
            actor.bodyYaw=yaw;java.util.List<Tri> actual=new ArrayList<>(),local=new ArrayList<>();
            CharacterModel model=new CharacterModel(actual,actor,2.03,.13);
            model.box(-.3,.8,-.2,.3,1.4,.2,0x82AEBD);
            World.addBox(local,-.3,.8,-.2,.3,1.4,.2,0x82AEBD,0,0,0,0);
            boolean same=actual.size()==local.size();
            for(int i=0;i<actual.size();i++){
                Tri a=actual.get(i),b=local.get(i);
                same&=a.color()==b.color()&&a.a().sub(model.p(b.a())).length()<1e-12
                    &&a.b().sub(model.p(b.b())).length()<1e-12&&a.c().sub(model.p(b.c())).length()<1e-12;
            }
            check(same,"Otimização da malha preserva cantos, faces e cores em yaw "+yaw);
        }
        Renderer fast=new Renderer(91,67),reference=new Renderer(91,67);Random random=new Random(17171);
        for(int i=0;i<90;i++){
            for(Renderer r:new Renderer[]{fast,reference}){
                r.camera(2,1,3,.7,-.1,Math.toRadians(90));r.nearSight=i%2==0;
                Arrays.fill(r.pixels,0);Arrays.fill(r.depth,0);
            }
            Tri t=new Tri(new V(random.nextDouble()*14-5,random.nextDouble()*10-4,random.nextDouble()*15-5),
                new V(random.nextDouble()*14-5,random.nextDouble()*10-4,random.nextDouble()*15-5),
                new V(random.nextDouble()*14-5,random.nextDouble()*10-4,random.nextDouble()*15-5),0xAEC27D);
            fast.worldTriangle(t);
            V a=reference.transform(t.a()),b=reference.transform(t.b()),c=reference.transform(t.c());
            if(!(a.z()<.09&&b.z()<.09&&c.z()<.09||a.z()>reference.far&&b.z()>reference.far&&c.z()>reference.far)){
                double distance=(a.z()+b.z()+c.z())/3;int color=Renderer.blend(t.color(),0xC3D1C5,(int)Settings.clamp((distance-12)*1.7,0,215));
                if(reference.nearSight)color=Renderer.blend(color,0x272637,(int)Settings.clamp((distance-2)*55,0,255));
                reference.clip(a,b,c,color);
            }
            if(!Arrays.equals(fast.pixels,reference.pixels)||!Arrays.equals(fast.depth,reference.depth))throw new AssertionError("Raster dinâmico alterou pixels na amostra "+i);
        }
        check(true,"Raster sem V temporários mantém pixels e profundidade em 90 cenas com recorte e nearsight");
    }
    static void benchmark(){
        System.out.println("RIFT 1.7 / quadro completo: simulação + cenário + HUD + escala 1280x720; sem janela");
        System.out.println("Java "+System.getProperty("java.version")+" / "+System.getProperty("os.name")+" / CPUs disponíveis: "+Runtime.getRuntime().availableProcessors());
        for(int quality:new int[]{0,1,2,3})measure(quality,false);measure(1,true);
    }
    static void measure(int quality,boolean smooth){
        Game game=new Game(new Settings(false),false,6611);game.settings.quality=quality;game.settings.smoothUpscale=smooth;game.start(true);game.player.primary=new Game.Gun(Game.Weapon.ECHO);game.player.slot=2;game.weaponEquip=0;game.player.x=42;game.player.z=72;game.player.yaw=1.57;
        View view=new View(game);BufferedImage frame=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=frame.createGraphics();double[] times=new double[240];
        for(int i=-90;i<times.length;i++){game.player.yaw+=.018;long t=System.nanoTime();game.tick(1./120,Input.Frame.empty());game.simulationMillis=(System.nanoTime()-t)/1e6;view.render(g,1280,720);if(i>=0)times[i]=(System.nanoTime()-t)/1e6;}
        double average=Arrays.stream(times).average().orElse(0);Arrays.sort(times);System.out.printf(Locale.ROOT,"%s / %s | %.2f ms | %.1f FPS | p95 %.2f ms | interno %dx%d%n",new String[]{"BAIXA","MÉDIA","ALTA","AUTO"}[quality],smooth?"suave":"rápida",average,1000/average,times[228],view.renderer.width,view.renderer.height);g.dispose();game.close();
    }
}
