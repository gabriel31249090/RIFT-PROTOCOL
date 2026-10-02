package rift;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import static rift.World.*;

/** Bitmap surfaces and original harbor scenery; every asset is bundled for offline play. */
final class EnvironmentArt {
    static BufferedImage sky(){
        int width=2048,height=512;BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setPaint(new LinearGradientPaint(0,0,0,360,new float[]{0,.43f,.71f,1},new Color[]{new Color(0x285F93),new Color(0x62A7C8),new Color(0xE5D3AF),new Color(0x8DAFB3)}));g.fillRect(0,0,width,height);
        Random r=new Random(731);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        for(int i=0;i<27;i++){int x=r.nextInt(width),y=104+r.nextInt(128),w=70+r.nextInt(130);for(int j=4;j>=0;j--){g.setColor(new Color(255,241,211,9+j*2));g.fill(new Ellipse2D.Double(x-w*.2*j,y-j*3,w*(1+j*.3),10+j*9));}}
        for(int i=65;i>0;i--){g.setColor(new Color(255,230,166,Math.max(1,13-i/6)));g.fillOval(1237-i,170-i,i*2,i*2);}g.setColor(new Color(0xFFF1CC));g.fillOval(1226,159,22,22);
        for(int layer=0;layer<3;layer++){Path2D mountains=new Path2D.Double();mountains.moveTo(0,360);for(int x=0;x<=width;x+=16){double y=278+layer*14+Math.sin(x*.008+layer)*19+Math.sin(x*.019+layer*3)*9;mountains.lineTo(x,y);}mountains.lineTo(width,512);mountains.lineTo(0,512);mountains.closePath();g.setColor(new Color(new int[]{0x9BAFB0,0x7F9FA7,0x698B99}[layer]));g.fill(mountains);}
        g.dispose();return image;
    }
    static BufferedImage ground(World world){
        if(world.mapIndex>=3)return DuelMaps.ground(world);
        int s=world.texScale;BufferedImage image=new BufferedImage(world.texW,world.texH,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();Random rng=new Random(27);
        BufferedImage tile=Assets.cell(Assets.MATERIALS,4,4,world.mapIndex==2?Assets.PLATE:world.mapIndex==1?Assets.CONCRETE:Assets.FLOOR);
        g.setPaint(new TexturePaint(tile,new Rectangle(0,0,s*3,s*3)));g.fillRect(0,0,image.getWidth(),image.getHeight());
        g.setColor(new Color(0x828D88));g.fillRect(30*s,3*s,4*s,54*s);g.fillRect(2*s,27*s,60*s,4*s);
        g.setColor(new Color(0xD7CEAC));g.setStroke(new BasicStroke(2));g.drawLine(30*s,3*s,30*s,57*s);g.drawLine(34*s,3*s,34*s,57*s);g.drawLine(2*s,27*s,62*s,27*s);g.drawLine(2*s,31*s,62*s,31*s);
        for(int x=3;x<62;x+=5){g.setColor(new Color(0xB3B7A2));g.fillRect(x*s,29*s-1,2*s,2);}
        g.setColor(new Color(0x85948F));g.fillRect(4*s,77*s,136*s,5*s);g.fillRect(62*s,4*s,4*s,119*s);g.fillRect(111*s,3*s,4*s,113*s);g.fillRect(4*s,109*s,136*s,4*s);
        g.setColor(new Color(0xD0C9AA));g.setStroke(new BasicStroke(2));g.drawLine(4*s,77*s,140*s,77*s);g.drawLine(111*s,4*s,111*s,110*s);
        for(int z=8;z<124;z+=8){g.fillRect(113*s,z*s,2,3*s);g.fillRect(64*s,z*s,2,3*s);}
        for(Site site:java.util.List.of(world.a,world.b)){
            int x=(int)((site.x()-5)*s),z=(int)((site.z()-5)*s);g.setColor(new Color(site==world.a?0xBEAA88:0x88A7A0));g.fillRect(x,z,10*s,10*s);
            g.setStroke(new BasicStroke(3));g.setColor(new Color(0xEFDFC0));g.drawRect(x,z,10*s,10*s);
            for(int j=0;j<4;j++)for(int i=0;i<8;i++){g.setColor(new Color(0x556A6C));g.fillRect(x+12+i*17,z+11+j*42,13,2);}
            g.setColor(new Color(0xF8E6BC));g.setFont(new Font("SansSerif",Font.BOLD,70));g.drawString(site.name(),x+49,z+102);
            g.setStroke(new BasicStroke(5));for(int i=0;i<11;i++){g.setColor(new Color(i%2==0?0xDEBB76:0x6E766D));g.drawLine(x+i*15,z+10*s+12,x+i*15+9,z+10*s+3);}
        }
        // Contact darkening and directional shadows baked into the walkable ground.
        for(Box b:world.solids)if(world.shadows&&b.x1()>0&&b.z1()>0){int x=(int)(b.x1()*s),z=(int)(b.z1()*s),w=(int)((b.x2()-b.x1())*s),h=(int)((b.z2()-b.z1())*s);double offset=b.y2()*.48*s;
            Path2D shadow=new Path2D.Double();shadow.moveTo(x,z);shadow.lineTo(x+w,z);shadow.lineTo(x+w+offset,z+h+offset*.57);shadow.lineTo(x+offset,z+h+offset*.57);shadow.closePath();g.setColor(new Color(30,58,74,75));g.fill(shadow);
            for(int i=5;i>0;i--){g.setColor(new Color(31,53,61,10));g.fillRoundRect(x-i,z-i,w+2*i,h+2*i,10,10);}
        }
        // Drain grates, route arrows, repaired paving and restrained surface variation.
        for(int x=4;x<62;x+=9){g.setColor(new Color(0x50686E));g.fillRect(x*s,30*s+7,2*s,8);g.setColor(new Color(0xA7B2A7));for(int i=0;i<9;i++)g.drawLine(x*s+i*4,30*s+7,x*s+i*4,30*s+15);}
        for(int z=8;z<57;z+=9){g.setColor(new Color(0xD9CB9F));int x=32*s;Path2D arrow=new Path2D.Double();arrow.moveTo(x-9,z*s+9);arrow.lineTo(x,z*s-6);arrow.lineTo(x+9,z*s+9);arrow.lineTo(x+4,z*s+9);arrow.lineTo(x+4,z*s+19);arrow.lineTo(x-4,z*s+19);arrow.lineTo(x-4,z*s+9);arrow.closePath();g.fill(arrow);}
        g.dispose();int[] data=image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
        for(int i=0;i<data.length;i++){int c=data[i],noise=rng.nextInt(7)-3,r=Math.max(0,Math.min(255,(c>>16&255)+noise)),gr=Math.max(0,Math.min(255,(c>>8&255)+noise)),b=Math.max(0,Math.min(255,(c&255)+noise));data[i]=r<<16|gr<<8|b;}
        image.setRGB(0,0,image.getWidth(),image.getHeight(),data,0,image.getWidth());return image;
    }
    static void decorate(World w){
        // The walls keep the original collision and navigation layout.
        for(int z=2;z<58;z+=7){
            for(double x:new double[]{.025,World.WIDTH-.13}){
                w.box(x,.3,z,x+.1,2.0,z+5.6,0x4E747C);w.box(x,2.0,z,x+.13,2.09,z+5.6,0xDDD0AD);
                w.box(x,3.2,z+.6,x+.12,4.5,z+3.8,0x778F92);w.box(x,3.26,z+.67,x+.15,4.42,z+3.73,0x38596A);
                for(double zz=z+.9;zz<z+3.8;zz+=.55)w.box(x,3.25,zz,x+.18,4.43,zz+.065,0xABC0B5);
                w.box(x,.1,z+5.9,x+.17,5.75,z+6.04,0x9C7C61);
            }
        }
        for(int x=3;x<World.NW;x+=8){w.box(x,.15,.035,x+5.6,2.5,.14,0x52737B);w.box(x,2.55,.035,x+5.6,2.67,.18,0xE1C18E);w.box(x,4.5,.035,x+5.6,4.65,.16,0xA9C1B8);}
        // Roof vents, water tanks and hanging service cables.
        for(double[] pos:new double[][]{{26,8.2,12},{37,8.2,12},{12,7.5,37},{49,7.5,37}}){
            w.box(pos[0],pos[1],pos[2],pos[0]+1.4,pos[1]+1.1,pos[2]+1.3,0xA0B4B1);for(int i=0;i<7;i++)w.box(pos[0]-.025,pos[1]+.16+i*.115,pos[2]+.12,pos[0],pos[1]+.20+i*.115,pos[2]+1.15,0x345663);
        }
        for(int x=25;x<40;x+=2)w.box(x,5.31,25,x+.65,5.40,27,0x8DCBC2);
        for(int i=0;i<18;i++){double x=6+i*2.8,y=6.6-Math.sin(i/17.*Math.PI)*1.1;w.box(x,y,28.08,x+2.82,y+.025,28.105,0x3A5260);}
        // Cargo cranes and stacks above the boundary, never obstructing the playing lanes.
        for(int i=0;i<3;i++){double x=-8+i*36,z=-16-i%2*4;int color=i==1?0xC59562:0x668A96;
            w.box(x,0,z,x+1.2,24,z+1.2,color);w.box(x+.25,23,z-9,x+.95,24,z+12,color);w.box(x+.28,17,z-7,x+.85,23.1,z-6.5,0x6B8591);w.box(x+.48,9,z+10,x+.54,23,z+10.1,0x667B80);
        }
        for(int i=0;i<7;i++)w.box(5+i*9,0,-8,12+i*9,5+(i%3)*2,-3,i%2==0?0x9F8A74:0x6A9196);
        signZ(w,"A  /  CARGA",13,3.0,.19,8,1.6,0xE0B579);signZ(w,"MEIO / TERMINAL",51,3.0,.19,8,1.6,0x9DCBBE);
        signZ(w,"CAIS 07",26.5,3.9,25.06,4.4,1.1,0xE2CCAA);signZ(w,"NEXO",37.5,3.9,25.06,4.4,1.1,0xA3D7CF);
        signZ(w,"07 - A",11,1.55,25.085,2.7,.62,0xF3D2A5);signZ(w,"M - 09",56,1.55,26.085,2.7,.62,0xC0DDD0);
        signX(w,"A  >",23.99,2.0,18,3.0,1.2,0xEBD1A0,false);signX(w,"<  B",40.06,2.0,18,3.0,1.2,0xA6D1C6,true);
    }
    static BufferedImage plaque(String text,int color){
        BufferedImage image=new BufferedImage(512,128,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(0x253F50));g.fillRect(0,0,512,128);g.setColor(new Color(color));g.fillRect(0,0,512,6);g.fillRect(15,20,8,88);g.setFont(new Font("SansSerif",Font.BOLD,52));int w=g.getFontMetrics().stringWidth(text);if(w>440)g.setFont(g.getFont().deriveFont(52f*440/w));g.drawString(text,42,83);g.setColor(new Color(0x789995));g.setFont(new Font("Monospaced",Font.PLAIN,12));g.drawString("RIFT  /  PORT AUTHORITY",43,111);g.fillOval(493,15,5,5);g.fillOval(493,108,5,5);g.dispose();return image;
    }
    static void signZ(World w,String text,double x,double y,double z,double width,double height,int color){w.decals.add(new Decal(new V(x+width/2,y+height,z),new V(x-width/2,y+height,z),new V(x-width/2,y,z),new V(x+width/2,y,z),plaque(text,color)));}
    static void signX(World w,String text,double x,double y,double z,double width,double height,int color,boolean positive){double sign=positive?1:-1;w.decals.add(new Decal(new V(x,y+height,z-width/2*sign),new V(x,y+height,z+width/2*sign),new V(x,y,z+width/2*sign),new V(x,y,z-width/2*sign),plaque(text,color)));}
}
