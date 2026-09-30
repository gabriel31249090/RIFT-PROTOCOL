package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import static rift.Game.*;

/** Short repeatable capture using the actual simulation and viewmodel animations. */
final class LoadoutPreview {
    static void frames(Path directory)throws Exception{
        Files.createDirectories(directory);Game g=UpdateTests.arena();View view=new View(g);g.settings.quality=1;
        g.player.x=126;g.player.z=49;g.player.yaw=Math.PI;g.player.pitch=-.035;g.weaponEquip=0;g.noticeTime=0;
        g.profile.skins[Weapon.ECHO.ordinal()]=3;g.profile.charms[Weapon.ECHO.ordinal()]=1;g.profile.melee=1;g.profile.meleeSkin=5;
        g.combat.inspect=2.4;
        for(int frame=0;frame<144;frame++){
            if(frame==58){g.combat.equip(3);}
            if(frame==72||frame==86)g.combat.attack(false);
            if(frame==104)g.combat.attack(true);
            Input.Frame input=frame>=36&&frame<46?Tests.trigger(true,frame==36):Input.Frame.empty();
            // Four 96 Hz simulation steps per 24 Hz output frame.
            for(int i=0;i<4;i++)g.tick(1/96.,input);
            g.noticeTime=0;BufferedImage image=new BufferedImage(960,540,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();view.render(graphics,960,540);
            View.rect(graphics,259,68,442,26,new Color(8,24,34,235));View.center(graphics,frame<36?"V / INSPEÇÃO  •  PULSO + CRISTAL":frame<58?"DISPARO / RECUO E RECUPERAÇÃO":frame<104?"ARCO / CORTE":"ARCO / GOLPE FORTE",480,86,11,View.MINT,true);graphics.dispose();
            ImageIO.write(image,"png",directory.resolve(String.format("frame-%03d.png",frame)).toFile());
        }
        g.close();System.out.println("144 frames / 24 FPS / 6 s: "+directory.toAbsolutePath());
    }
}
