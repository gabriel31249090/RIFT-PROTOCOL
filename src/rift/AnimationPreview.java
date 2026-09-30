package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import static rift.Game.*;
import static rift.World.*;

/** Exports the real character rig for visual review; no prerendered animations are used in-game. */
final class AnimationPreview {
    static void frames(Path dir)throws Exception{
        Files.createDirectories(dir);Renderer r=new Renderer(240,370);Actor[] actors=new Actor[4];
        int[] agents={0,6,7,2};for(int i=0;i<4;i++){Actor a=new Actor(i,0,"DEMO");a.agentIndex=agents[i];a.yaw=a.bodyYaw=Math.PI+.45;a.primary=new Gun(Weapon.ECHO);a.slot=2;actors[i]=a;}
        for(int frame=0;frame<96;frame++){
            double time=frame/24.;BufferedImage image=new BufferedImage(960,440,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();graphics.setColor(new Color(0x102738));graphics.fillRect(0,0,960,440);
            for(int i=0;i<4;i++){
                Actor a=actors[i];a.moveSpeed=i==0?2.3:i==1?4.8:0;a.walk+=a.moveSpeed/24.;a.crouch=i==2&&time>=2;a.y=i==2&&time<2?Math.max(0,Math.sin(time/2*Math.PI))*.55:0;a.grounded=a.y<.01;
                if(i==3){a.gun().reloadTotal=2.15;a.gun().reload=2.15-time%2.15;}CharacterModel.animate(a,1/24.);
                for(int y=0;y<r.height;y++)Arrays.fill(r.pixels,y*r.width,(y+1)*r.width,Renderer.blend(0x23495F,0x132B3C,y*140/r.height));Arrays.fill(r.depth,0);r.camera(0,1.12,-3.6,0,-.035,Math.toRadians(37));r.dynamic.clear();CharacterModel.add(r.dynamic,a,time,true);for(Tri t:r.dynamic)r.worldTriangle(t);
                graphics.drawImage(r.image,i*240,42,null);View.text(graphics,new String[]{"ANDAR","CORRER","SALTAR / AGACHAR","RECARREGAR"}[i],i*240+17,30,14,View.MINT,true);View.text(graphics,Agent.values()[a.agentIndex].name,i*240+17,429,11,View.WHITE,true);
            }
            graphics.dispose();ImageIO.write(image,"png",dir.resolve(String.format("frame-%03d.png",frame)).toFile());
        }
    }
}
