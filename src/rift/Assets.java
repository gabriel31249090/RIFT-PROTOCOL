package rift;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

/** Cached bitmap art. The same packaged images are used in the UI and the renderer. */
final class Assets {
    static final int PLASTER=0,CONCRETE=1,BRICK=2,STONE=3,TEAL=4,RUST=5,STEEL=6,BRASS=7,WOOD=8,FLOOR=9,ASPHALT=10,PLATE=11,RUBBER=12,CLOTH=13,GLASS=14,ROOF=15;
    static final BufferedImage HERO=load("menu-keyart.png"),MATERIALS=load("materials.png"),ICONS=load("abilities.png"),AGENTS=load("agents.png");
    static final BufferedImage[] portraits=new BufferedImage[12],icons=new BufferedImage[48];
<<<<<<< HEAD
    // Each render thread owns its palettes: two local windows cannot recolor each other's triangles.
    static final class TintCache { final long[] keys=new long[8192]; final int[][] colors=new int[8192][]; }
    static final ThreadLocal<TintCache> TINT=ThreadLocal.withInitial(TintCache::new);
=======
    static final java.util.Map<Long,int[]> tintCache=new java.util.LinkedHashMap<>(){protected boolean removeEldestEntry(java.util.Map.Entry<Long,int[]> e){return size()>384;}};
>>>>>>> a28a0d3591e35d0c3bb500da202ff4a47e878941
    static final int[][] average=new int[16][3];
    static final int[][][] tiles=new int[16][5][];
    static final byte[][][] indices=new byte[16][5][];
    static final int[][][] palettes=new int[16][5][];
    static {for(int i=0;i<12;i++)portraits[i]=cell(AGENTS,4,3,i);for(int i=0;i<48;i++)icons[i]=cell(ICONS,8,6,i);
        for(int i=0;i<16;i++){BufferedImage tile=cell(MATERIALS,4,4,i);for(int level=0;level<5;level++){int size=256>>level;BufferedImage m=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);Graphics2D g=m.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(tile,0,0,size,size,null);g.dispose();tiles[i][level]=((DataBufferInt)m.getRaster().getDataBuffer()).getData();}for(int c:tiles[i][4]){average[i][0]+=c>>16&255;average[i][1]+=c>>8&255;average[i][2]+=c&255;}for(int ch=0;ch<3;ch++)average[i][ch]=Math.max(1,average[i][ch]/tiles[i][4].length);}
        // Recolor a small luminance palette instead of allocating a full texture
        // for every fog/lighting value as the camera turns. Pixel positions and
        // mip levels still come directly from the packaged material bitmap.
        for(int material=0;material<16;material++)for(int level=0;level<5;level++){
            int[] src=tiles[material][level],palette=new int[256];byte[] lookup=new byte[src.length];int[][] sums=new int[256][4];
            for(int i=0;i<src.length;i++){int c=src[i],r=c>>16&255,g=c>>8&255,b=c&255,key=(r*77+g*150+b*29)>>8;lookup[i]=(byte)key;sums[key][0]+=r;sums[key][1]+=g;sums[key][2]+=b;sums[key][3]++;}
            for(int i=0;i<256;i++){int count=sums[i][3];if(count>0)palette[i]=(sums[i][0]/count)<<16|(sums[i][1]/count)<<8|sums[i][2]/count;}
            indices[material][level]=lookup;palettes[material][level]=palette;
        }
    }
    static BufferedImage load(String name){
        try(InputStream in=Assets.class.getResourceAsStream("/assets/"+name)){
            BufferedImage raw=in!=null?ImageIO.read(in):ImageIO.read(new File("assets",name));if(raw==null)throw new IOException("Imagem inválida");
            BufferedImage rgb=new BufferedImage(raw.getWidth(),raw.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=rgb.createGraphics();g.drawImage(raw,0,0,null);g.dispose();return rgb;
        }catch(IOException e){throw new IllegalStateException("Asset ausente: assets/"+name+". Extraia o pacote completo ou recompile incluindo assets.",e);}
    }
    static BufferedImage cell(BufferedImage image,int cols,int rows,int i){int x=i%cols*image.getWidth()/cols,y=i/cols*image.getHeight()/rows;int xx=(i%cols+1)*image.getWidth()/cols,yy=(i/cols+1)*image.getHeight()/rows;BufferedImage out=new BufferedImage(xx-x,yy-y,BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();g.drawImage(image,0,0,out.getWidth(),out.getHeight(),x,y,xx,yy,null);g.dispose();return out;}
    static int sample(int material,double u,double v,int level){level=Math.min(4,Math.max(0,level));int size=256>>level,mask=size-1;int x=(int)Math.floor(u*size)&mask,y=(int)Math.floor(v*size)&mask;return tiles[material][level][y*size+x];}
    static int[] tinted(int material,int level,int color){
<<<<<<< HEAD
        TintCache cache=TINT.get();long[] tintKeys=cache.keys;int[][] tintCache=cache.colors;
        color&=0xFCFCFC;long key=((long)color<<9)|(material<<4)|level;
        int slot=((int)(key^(key>>>32))*0x9E3779B9)>>>19;int[] out=tintCache[slot];if(out!=null&&tintKeys[slot]==key)return out;
        if(out==null)out=tintCache[slot]=new int[256];
        int[] src=palettes[material][level];int cr=color>>16&255,cg=color>>8&255,cb=color&255;
        for(int i=0;i<out.length;i++){int c=src[i];int r=Math.min(255,cr*(average[material][0]+(c>>16&255))/(2*average[material][0])),g=Math.min(255,cg*(average[material][1]+(c>>8&255))/(2*average[material][1])),b=Math.min(255,cb*(average[material][2]+(c&255))/(2*average[material][2]));out[i]=r<<16|g<<8|b;}
        tintKeys[slot]=key;return out;
=======
        long key=((long)(color&0xffffff)<<9)|(material<<4)|level;int[] cached=tintCache.get(key);if(cached!=null)return cached;
        int[] src=palettes[material][level],out=new int[src.length];int cr=color>>16&255,cg=color>>8&255,cb=color&255;
        for(int i=0;i<out.length;i++){int c=src[i];int r=Math.min(255,cr*(average[material][0]+(c>>16&255))/(2*average[material][0])),g=Math.min(255,cg*(average[material][1]+(c>>8&255))/(2*average[material][1])),b=Math.min(255,cb*(average[material][2]+(c&255))/(2*average[material][2]));out[i]=r<<16|g<<8|b;}
        tintCache.put(key,out);return out;
>>>>>>> a28a0d3591e35d0c3bb500da202ff4a47e878941
    }
    static void portrait(Graphics2D g,int index,int x,int y,int w,int h){g.drawImage(portraits[Math.floorMod(index,12)],x,y,w,h,null);}
    static void icon(Graphics2D g,Game.Ability a,double x,double y,double r){int d=(int)Math.ceil(r*3.1);g.drawImage(icons[a.ordinal()],(int)(x-d/2.),(int)(y-d/2.),d,d,null);}
}
