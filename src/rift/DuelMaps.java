package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import static rift.World.*;

/** Three bounded, mirrored arenas. A central blocker protects both starting positions. */
final class DuelMaps {
    static final String[] NAMES={"FENDA", "PÁTIO ZERO", "GALERIA"};
    static V spawn(int side){return new V(72,0,side==0?84:44);}
    static void build(World w){
        int map=w.mapIndex-3;
        int wall=map==0?0xA2BAB4:map==1?0xB6AB96:0x8BADC2;
        int accent=map==0?0xC98972:map==1?0x6A99A3:0xBD988D;
        w.solid(48,0,39,96,9,40,wall);w.solid(48,0,88,96,9,89,wall);
        w.solid(47,0,39,48,9,89,wall);w.solid(96,0,39,97,9,89,wall);
        if(map==0){
            w.solid(65,0,61,79,4.3,67,wall);
            w.container(51,52,59,56,2.5,accent);w.container(85,72,93,76,2.5,accent);
            w.container(85,52,93,56,2.5,accent);w.container(51,72,59,76,2.5,accent);
            w.crate(63,76,2,2,1.2);w.crate(79,50,2,2,1.2);
            w.crate(79,76,2,2,1.2);w.crate(63,50,2,2,1.2);
        }else if(map==1){
            w.container(68,61,76,67,3.1,accent);
            w.platform(50,57,56,71,2.2,wall);w.platform(88,57,94,71,2.2,wall);
            w.ramp(50,47,56,57,2.2,false,wall);w.ramp(50,71,56,81,2.2,true,wall);
            w.ramp(88,47,94,57,2.2,false,wall);w.ramp(88,71,94,81,2.2,true,wall);
            w.crate(62,52,3,3,1.2);w.crate(79,73,3,3,1.2);
            w.crate(79,52,3,3,1.2);w.crate(62,73,3,3,1.2);
        }else{
            w.solid(69,0,57,75,5,71,wall);
            w.solid(56,0,50,64,4,59,wall);w.solid(80,0,69,88,4,78,wall);
            w.solid(80,0,50,88,4,59,wall);w.solid(56,0,69,64,4,78,wall);
            w.crate(50,62,3,4,1.15);w.crate(91,62,3,4,1.15);
        }
        for(double x:new double[]{49,60,84,95}){
            w.box(x,5,40.02,x+.3,5.3,40.15,0xE6DDC2);
            w.box(x,5,87.85,x+.3,5.3,87.98,0xA2DED8);
        }
        EnvironmentArt.signZ(w,"RIFT / "+NAMES[map],72,5,40.03,20,2,0xD9E5DA);
        EnvironmentArt.signZ(w,"DUEL / 01",72,5,87.97,16,2,0xA6DBD0);
    }
    static BufferedImage ground(World w){
        BufferedImage image=new BufferedImage(w.texW,w.texH,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();int s=w.texScale;
        g.setPaint(new TexturePaint(Assets.cell(Assets.MATERIALS,4,4,w.mapIndex==5?Assets.PLATE:Assets.FLOOR),new Rectangle(0,0,s*3,s*3)));
        g.fillRect(0,0,image.getWidth(),image.getHeight());
        g.setColor(new Color(190,214,200,110));g.setStroke(new BasicStroke(3));
        g.drawRect(50*s,42*s,44*s,44*s);g.drawLine(50*s,64*s,94*s,64*s);
        for(int side=0;side<2;side++){
            V p=spawn(side);int x=(int)p.x()*s,z=(int)p.z()*s;
            g.setColor(new Color(side==0?0x8EBEB4:0xCD9686));g.drawOval(x-2*s,z-2*s,4*s,4*s);
            g.setFont(new Font("SansSerif",Font.BOLD,28));g.drawString(side==0?"01":"02",x-18,z+10);
        }
        if(w.shadows)for(Box b:w.solids)if(b.x1()>=48&&b.x2()<=96&&b.z1()>40&&b.z2()<88){
            g.setColor(new Color(18,39,51,70));g.fillRect((int)(b.x1()*s),(int)(b.z1()*s),(int)((b.x2()-b.x1()+b.y2()*.4)*s),(int)((b.z2()-b.z1()+b.y2()*.3)*s));
        }
        g.dispose();return image;
    }
}
