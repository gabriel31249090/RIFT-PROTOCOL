package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import static rift.Tests.check;

/** UI rendering and input checks without opening a window or audio device. */
final class UiTests {
    static void run()throws Exception {
        for(String name:UiIcons.NAMES) {
            BufferedImage icon=UiIcons.images.get(name);
            check(icon!=null&&icon.getWidth()==48&&icon.getHeight()==48,"UI: Lucide empacotado "+name);
            boolean transparent=false,visible=false;
            for(int y=0;y<48;y++)for(int x=0;x<48;x++){int alpha=icon.getRGB(x,y)>>>24;transparent|=alpha==0;visible|=alpha>200;}
            check(transparent&&visible,"UI: mascara de icone transparente e nao vazia "+name);
        }
        Game game=UpdateTests.arena();try {
            View view=new View(game);
            for(int[] size:new int[][]{{1280,720},{800,450}}) {
                for(String screen:new String[]{"menu","modes","queue","found","loading","tournament","agents","profile","collection","shop","settings","play","pause"}) {
                    game.ui=screen;BufferedImage image=draw(view,size[0],size[1]);
                    check(nonblank(image),"UI: "+screen+" nao vazio em "+size[0]+"x"+size[1]);
                    check(view.buttons.stream().allMatch(b->b.x()>=0&&b.y()>=0&&b.w()>0&&b.h()>0&&b.x()+b.w()<=1280&&b.y()+b.h()<=720),"UI: botoes de "+screen+" dentro da tela");
                }
                game.ui="menu";draw(view,size[0],size[1]);view.click((int)(100*view.scale+view.offsetX),(int)(400*view.scale+view.offsetY));check(game.ui.equals("modes"),"UI: jogar usa hitbox escalada em "+size[0]);
            }
            game.ui="menu";draw(view,1000,700);view.click(100,40);check(game.ui.equals("menu"),"UI: margem de letterbox nao aciona botao");
            view.click((int)(290*view.scale+view.offsetX),(int)(523*view.scale+view.offsetY));check(game.ui.equals("settings"),"UI: configuracoes mantem acao do menu");
            view.settingsUI.tab=0;draw(view,1280,720);double old=game.settings.sensitivity;view.click(850,155);check(game.settings.sensitivity<old,"UI: icone menos diminui valor");view.click(1185,155);check(Math.abs(game.settings.sensitivity-old)<1e-9,"UI: icone mais aumenta valor");
            view.settingsUI.tab=1;draw(view,1280,720);view.click(1090,155);check(game.settings.quality==2,"UI: qualidade seleciona opcao diretamente");draw(view,1280,720);boolean shadows=game.settings.shadows;view.click(1190,214);check(game.settings.shadows!=shadows,"UI: switch binario alterna sombras");
            view.settingsUI.tab=3;draw(view,1280,720);view.click(1140,392);check(game.settings.crossColor==3,"UI: swatch seleciona cor da mira");
            boolean dynamic=game.settings.dynamicCrosshair,dot=game.settings.crossDot;view.click(972,457);check(game.settings.dynamicCrosshair!=dynamic&&game.settings.crossDot==dot,"UI: switches de mira sao independentes");
            for(int tab=0;tab<5;tab++){view.settingsUI.tab=tab;check(nonblank(draw(view,800,450)),"UI: aba de configuracoes "+tab+" em janela compacta");}
            game.ui="collection";draw(view,1280,720);view.click(110,241);check(view.collectionUI.weapon==Game.Weapon.values()[1],"UI: lista de armas seleciona sem mudar equipagem");
        }finally{game.close();}
        try(DuelSimulation simulation=new DuelSimulation(0)) {
            DuelProtocol.State state=new DuelProtocol.State();Game copy=DuelTests.replica(simulation.world,0);try {
                DuelProtocol.snapshot(DuelProtocol.snapshot(simulation,0,0),copy,state,0);DuelView view=new DuelView(copy);
                for(int[] size:new int[][]{{1280,720},{800,450}})for(boolean equipment:new boolean[]{false,true}) {
                    BufferedImage image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();
                    view.render(p,size[0],size[1],state,0,DuelProtocol.Choice.defaults(),equipment,false,"Sala de verificacao UI","");p.dispose();
                    check(nonblank(image),"UI: duelo "+(equipment?"equipamento":"HUD")+" em "+size[0]);
                }
            }finally{copy.close();}
        }
        System.out.println("UI TESTS OK");
    }
    static BufferedImage draw(View view,int w,int h) {
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();view.render(p,w,h);p.dispose();return image;
    }
    static boolean nonblank(BufferedImage image) {
        int first=image.getRGB(0,0),different=0;
        for(int y=0;y<image.getHeight();y+=8)for(int x=0;x<image.getWidth();x+=8)if(image.getRGB(x,y)!=first)different++;
        return different>image.getWidth()*image.getHeight()/4000;
    }
    static void capture(Path directory)throws Exception {
        Files.createDirectories(directory);Game game=UpdateTests.arena();try {
            View view=new View(game);
            for(int[] size:new int[][]{{1280,720},{800,450}})for(String screen:new String[]{"menu","modes","agents","profile","tournament","collection","shop","settings","play"}) {
                game.ui=screen;view.settingsUI.tab=1;ImageIO.write(draw(view,size[0],size[1]),"png",directory.resolve(screen+"-"+size[0]+".png").toFile());
            }
            game.ui="settings";view.settingsUI.tab=2;ImageIO.write(draw(view,1280,720),"png",directory.resolve("audio-1280.png").toFile());
        }finally{game.close();}
        try(DuelSimulation simulation=new DuelSimulation(0)) {
            DuelProtocol.State state=new DuelProtocol.State();Game copy=DuelTests.replica(simulation.world,0);try {
                DuelProtocol.snapshot(DuelProtocol.snapshot(simulation,0,0),copy,state,0);DuelView view=new DuelView(copy);
                for(int[] size:new int[][]{{1280,720},{800,450}})for(boolean equipment:new boolean[]{false,true}) {
                    BufferedImage image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();view.render(p,size[0],size[1],state,0,DuelProtocol.Choice.defaults(),equipment,false,"Sala 1v1","");p.dispose();
                    ImageIO.write(image,"png",directory.resolve("duelo"+(equipment?"-equipamento":"")+"-"+size[0]+".png").toFile());
                }
            }finally{copy.close();}
        }
    }
}
