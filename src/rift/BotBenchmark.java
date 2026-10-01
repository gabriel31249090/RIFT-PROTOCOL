package rift;
import java.awt.*;
import java.awt.image.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import static rift.Game.*;

/** Same seed, camera, actors, graphics settings and input for both release JARs. */
final class BotBenchmark {
    public static void main(String[] args) {
        int count=240;
        System.out.println("RIFT 1.7 / combate com 10 atores vivos / 9 bots / 1280x720 / sem janela");
        System.out.println("120 quadros de aquecimento + 240 medidos por qualidade; vida alta para manter carga contínua.");
        for(int quality:new int[]{0,1,2}){
            Game g=new Game(new Settings(false),false,1717);g.settings.quality=quality;
            g.start(false);g.beginRound();g.timer=1000;g.player.x=15;g.player.z=29;g.player.yaw=Math.PI/2;g.weaponEquip=0;
            for(Actor a:g.actors){
                a.hp=1_000_000;a.armor=0;a.invulnerable=0;a.skillCooldown=10000;a.primary=new Gun(Weapon.ECHO);a.slot=2;
                if(a.id>0){a.x=a.team==0?12:23;a.z=27+(a.id%5)*.8;a.y=0;a.yaw=a.team==0?Math.PI/2:-Math.PI/2;a.bodyYaw=a.yaw;}
            }
            View view=new View(g);BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D output=image.createGraphics();
            double[] cpu=new double[count],frames=new double[count];
            com.sun.management.ThreadMXBean mx=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
            long thread=Thread.currentThread().getId(),simBytes=0;
            for(int i=-120;i<count;i++){
                long allocated=mx.getThreadAllocatedBytes(thread),start=System.nanoTime();g.tick(1/60.,Input.Frame.empty());
                long end=System.nanoTime(),bytes=mx.getThreadAllocatedBytes(thread)-allocated;g.simulationMillis=(end-start)/1e6;
                view.render(output,1280,720);
                if(i>=0){cpu[i]=g.simulationMillis;frames[i]=(System.nanoTime()-start)/1e6;simBytes+=bytes;}
            }
            double avg=Arrays.stream(frames).average().orElse(0);Arrays.sort(frames);
            System.out.printf(Locale.ROOT,"quality=%d interno=%dx%d | %.2fms %.1fFPS p95=%.2fms | IA+sim=%.3fms | simAllocation=%d bytes/tick | live=%d%n",
                quality,view.renderer.width,view.renderer.height,avg,1000/avg,frames[(int)(count*.95)],Arrays.stream(cpu).average().orElse(0),simBytes/count,g.actors.stream().filter(a->!a.dead).count());
            output.dispose();g.close();
        }
    }
}
