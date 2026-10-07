package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** Rasterized upstream Lucide icons, tinted once per palette color. */
final class UiIcons {
    static final java.util.List<String> NAMES=java.util.List.of("play","crosshair","settings-2","users","trophy","box","eye","log-out","arrow-left","chevron-left","chevron-right","minus","plus","check","save");
    static final Map<String,BufferedImage> images=new HashMap<>(),tinted=new ConcurrentHashMap<>();
    static {
        for(String name:NAMES)try(InputStream in=UiIcons.class.getResourceAsStream("/assets/ui/lucide/"+name+".png")) {
            if(in!=null){BufferedImage icon=ImageIO.read(in);if(icon!=null)images.put(name,icon);}
        }catch(IOException ignored) {}
    }
    private UiIcons() {}
    static void draw(Graphics2D g,String name,double x,double y,int size,Color color) {
        BufferedImage source=images.get(name);if(source==null)return;
        String key=name+":"+color.getRGB();BufferedImage icon=tinted.get(key);
        if(icon==null) {
            icon=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
            int rgb=color.getRGB()&0xffffff;
            for(int yy=0;yy<source.getHeight();yy++)for(int xx=0;xx<source.getWidth();xx++)
                icon.setRGB(xx,yy,((source.getRGB(xx,yy)>>>24)*color.getAlpha()/255<<24)|rgb);
            tinted.put(key,icon);
        }
        g.drawImage(icon,(int)Math.round(x),(int)Math.round(y),size,size,null);
    }
}
