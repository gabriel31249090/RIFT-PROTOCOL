package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import static rift.World.*;

/** CAIS-7 visual pilot. The original navigation, cover and collision remain intact. */
final class CaisArt {
    static final int FLOOR=Assets.registerMaterial("models/cais7/concrete_floor_worn_001_diff_1k.jpg");
    static final int WALL=Assets.registerMaterial("models/cais7/concrete_wall_003_diff_1k.jpg");
    static final int METAL=Assets.registerMaterial("models/cais7/blue_metal_plate_diff_1k.jpg");
    static final MeshAssets.Mesh UNIT=MeshAssets.load("models/cais7/service-unit.obj");
    static void decorate(World w){
        for(int i=0;i<w.triangles.size();i++){
            Tri t=w.triangles.get(i);int material=t.material();
            if(material==Assets.PLASTER||material==Assets.CONCRETE||material==Assets.STONE){
                int color=t.color();double brightness=((color>>16&255)*.299+(color>>8&255)*.587+(color&255)*.114)/175.;
                // Preserve shadow intensity and facade accents, neutralize only broad concrete.
                if((color>>16&255)>110&&(color>>8&255)>110&&(color&255)>90)color=shade(0xB6BDB9,brightness);
                t=World.texture(new Tri(t.a(),t.b(),t.c(),color),WALL,.25);
            }else if(material==Assets.ROOF||material==Assets.TEAL||material==Assets.RUST){
                t=World.texture(t,METAL,.38);
            }
            w.triangles.set(i,t);
        }
        // Even airborne actors cannot cross the map boundary. Keep new props outside it.
        for(int module=0;module<12;module++){
            double x=12+module*11,z=-2.1,y=6.11;
            UNIT.add(w.triangles,v->new V(x+v.x(),y+v.y(),z+v.z()),name->name.equals("grille")?0x303A3A:0x9AA9A7,name->name.equals("grille")?Assets.RUBBER:METAL);
            int start=w.triangles.size();
            for(int i=0;i<5;i++)w.box(x-.57,y+.24+i*.066,z-.43,x+.57,y+.263+i*.066,z-.418,0x556362);
            w.paint(start,Assets.STEEL,1);
        }
        // Exterior dock cranes are beyond the playable boundary, never visual-only cover.
        for(double x:new double[]{24,92}){
            int start=w.triangles.size();
            w.box(x,0,-17,x+1.3,21,-15.7,0x72817C);w.box(x-9,19.8,-18,x+18,20.6,-15,0xD6B965);
            w.box(x+15,8,-16.7,x+15.12,20,-16.58,0x44524E);
            w.paint(start,Assets.STEEL,.25);
        }
    }
    static void groundDetails(Graphics2D g,World w){
        int s=w.texScale;
        g.setColor(new Color(0x65706C));g.setStroke(new BasicStroke(1));
        for(int z=6;z<128;z+=6)g.drawLine(0,z*s,w.texW,z*s);
        for(int x=6;x<144;x+=6)g.drawLine(x*s,0,x*s,w.texH);
        g.setColor(new Color(0xC5B66E));g.setStroke(new BasicStroke(2));
        g.drawLine(3*s,81*s,10*s,81*s);g.drawLine(135*s,40*s,135*s,73*s);
    }
}
