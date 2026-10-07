package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import static rift.World.*;
import static rift.Game.*;

/** Reproducible native screenshots, including actual CPU-rasterized imported models. */
final class VisualCapture {
    static void run(Path directory)throws Exception{
        Files.createDirectories(directory);UiTests.capture(directory);CharacterVisualTests.capture(directory);
        Game game=new Game(new Settings(false),false,192);try{
            game.start(true);game.ui="play";game.settings.quality=1;game.weaponEquip=0;
            for(Actor actor:game.actors)if(actor!=game.player)actor.dead=true;
            View view=new View(game);double[][] positions={{13,31,Math.PI},{71,66,0},{121,38,Math.PI}};
            for(int i=0;i<positions.length;i++){
                game.player.x=positions[i][0];game.player.z=positions[i][1];game.player.yaw=positions[i][2];game.player.pitch=-.025;
                for(int[] size:new int[][]{{1280,720},{800,450}}){
                    BufferedImage image=UiTests.draw(view,size[0],size[1]);
                    ImageIO.write(image,"png",directory.resolve("cais7-"+i+"-"+size[0]+".png").toFile());
                }
            }
            game.aimLerp=1;ImageIO.write(UiTests.draw(view,1280,720),"png",directory.resolve("echo-ads-1280.png").toFile());
            game.aimLerp=0;game.player.gun().reloadTotal=2.2;game.player.gun().reload=1.1;
            ImageIO.write(UiTests.draw(view,1280,720),"png",directory.resolve("echo-reload-1280.png").toFile());
        }finally{game.close();}
        Renderer preview=new Renderer(800,450);
        ImageIO.write(preview.preview(Weapon.ECHO,0),"png",directory.resolve("echo-model-800.png").toFile());
        Renderer unit=new Renderer(800,450);Arrays.fill(unit.pixels,UiTheme.BACKGROUND.getRGB());
        unit.camera(2,.9,-3.8,-.484,-.116,Math.toRadians(48));
        CaisArt.UNIT.add(unit.dynamic,v->v,name->name.equals("grille")?0x303A3A:0x9AA9A7,name->name.equals("grille")?Assets.RUBBER:CaisArt.METAL);
        for(Tri t:unit.dynamic)unit.worldTriangle(t);
        ImageIO.write(unit.image,"png",directory.resolve("cais7-service-unit-800.png").toFile());
        System.out.println("Visual captures: "+directory.toAbsolutePath());
    }
}
