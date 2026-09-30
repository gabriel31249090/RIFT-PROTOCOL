package rift;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;
import static rift.World.*;

/** Alpha sprites generated for this release; depth-tested in the world before smoke. */
final class EffectSprites {
    static final int SIZE=128;
    static final int[][] pixels=new int[4][];
    static {
        try(InputStream stream=EffectSprites.class.getResourceAsStream("/assets/effects.png")){
            BufferedImage sheet=stream==null?ImageIO.read(new File("assets/effects.png")):ImageIO.read(stream);
            for(int i=0;i<4;i++){BufferedImage tile=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);Graphics2D g=tile.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);int x=i%2*sheet.getWidth()/2,y=i/2*sheet.getHeight()/2;g.drawImage(sheet,0,0,SIZE,SIZE,x,y,x+sheet.getWidth()/2,y+sheet.getHeight()/2,null);g.dispose();pixels[i]=tile.getRGB(0,0,SIZE,SIZE,null,0,SIZE);}
        }catch(IOException e){throw new IllegalStateException("Asset de efeitos ausente",e);}
    }
    static void particle(Renderer r,Game.Particle p){int kind=(p.color&255)>(p.color>>16&255)?3:p.duration>.45?1:2;draw(r,new V(p.x,p.y,p.z),p.size*3.2,kind,Math.min(1,p.life/p.duration*1.5));}
    static void draw(Renderer r,V point,double size,int kind,double alpha){
        V camera=r.transform(point);if(camera.z()<.09||camera.z()>r.far)return;double radius=Math.min(110,r.focal*size/camera.z());if(radius<.6)return;
        double centerX=r.width*.5+camera.x()*r.focal/camera.z(),centerY=r.height*.5-camera.y()*r.focal/camera.z();int x0=Math.max(0,(int)(centerX-radius)),x1=Math.min(r.width-1,(int)(centerX+radius)),y0=Math.max(0,(int)(centerY-radius)),y1=Math.min(r.height-1,(int)(centerY+radius));
        double inv=1/camera.z(),step=SIZE/(radius*2);int[] texture=pixels[kind];
        for(int y=y0;y<=y1;y++){int ty=(int)((y-centerY+radius)*step);if(ty<0||ty>=SIZE)continue;for(int x=x0;x<=x1;x++){int tx=(int)((x-centerX+radius)*step),index=y*r.width+x;if(tx<0||tx>=SIZE||inv<r.depth[index]-.002)continue;int color=texture[ty*SIZE+tx],a=(int)((color>>>24)*alpha);if(a>2)r.pixels[index]=Renderer.blend(r.pixels[index],color,a);}}
    }
}
