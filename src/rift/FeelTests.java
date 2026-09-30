package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import static java.awt.event.KeyEvent.*;
import static rift.Game.*;
import static rift.World.*;
import static rift.Tests.*;
import static rift.UpdateTests.*;

/** Regression scenarios for input timing, contact, larger routes and saved equipment. */
final class FeelTests {
    static void advance(Game g,double seconds,Input.Frame input){int n=(int)Math.ceil(seconds*120);for(int i=0;i<n;i++)g.tick(seconds/n,input);}
    static void run()throws Exception{movement();shooting();melee();collection();maps();}
    static void movement(){
        Game g=arena();g.weaponEquip=0;double start=g.player.z;
        g.tick(1/120.,Input.Frame.keys(VK_W));check(g.player.moveSpeed>0&&g.player.moveSpeed<1,"Movimento acelera a partir do repouso");
        advance(g,.3,Input.Frame.keys(VK_W));check(g.player.moveSpeed>5,"Velocidade de corrida responde em menos de 0,3 s");
        double running=g.playerSpread();g.tick(1/120.,Input.Frame.empty());check(g.player.moveSpeed>0,"Soltar tecla inicia frenagem contínua");
        advance(g,.09,Input.Frame.empty());check(g.player.moveSpeed<.001&&g.playerSpread()<running*.15,"Parar recupera a precisão de movimento em até 100 ms");
        advance(g,.25,Input.Frame.keys(VK_D));advance(g,.08,Input.Frame.keys(VK_A));check(g.player.vx>0,"Contra-direção atravessa o repouso e inverte o strafe");
        check(g.player.z<start-1,"Aceleração produz deslocamento real");
        g.player.vx=g.player.vz=0;g.player.x=42;g.player.z=72;g.combat.equip(3);advance(g,.4,Input.Frame.keys(VK_W));check(g.player.moveSpeed>6.1,"Lâmina equipada aumenta mobilidade");
        advance(g,.2,Input.Frame.keys(VK_W,VK_SHIFT));check(g.player.moveSpeed<2.5,"Caminhada lenta funciona com lâmina");
        g.combat.equip(2);g.player.x=42;g.player.z=72;advance(g,.25,Input.Frame.keys(VK_W));g.damage(g.player,10,g.actors.get(5),false);advance(g,.10,Input.Frame.keys(VK_W));check(g.player.moveSpeed<3.3&&g.player.tagTime>0,"Impacto reduz temporariamente a velocidade");
        // Identical duration and input sequence at different presentation rates.
        double[] travel=new double[3];int[] rates={60,120,144};
        for(int i=0;i<3;i++){g.player.x=42;g.player.z=72;g.player.vx=g.player.vz=g.player.tagTime=0;g.player.y=0;g.player.grounded=true;for(int j=0;j<rates[i];j++)g.tick(1./rates[i],Input.Frame.keys(VK_W));travel[i]=72-g.player.z;}
        check(Arrays.stream(travel).max().orElse(0)-Arrays.stream(travel).min().orElse(0)<.025,"Deslocamento consistente a 60, 120 e 144 FPS");
        g.player.x=6;g.player.z=37;g.player.yaw=Math.PI/2;g.player.vx=g.player.vz=0;advance(g,1,Input.Frame.keys(VK_W));check(g.player.x<9.7&&g.player.vx==0,"Velocidade é anulada na parede, sem atravessar colisão");
        g.close();
    }
    static void shooting(){
        Game g=arena();g.weaponEquip=0;double base=g.playerSpread();
        for(int i=0;i<9;i++){g.firePlayer();g.player.gun().tick(.115);}
        check(g.playerSpread()>base*3&&g.player.gun().pitchRecoil>.05&&Math.abs(g.player.gun().yawRecoil)>.002,"Rajada longa acumula recuo vertical, lateral e dispersão");
        advance(g,.65,Input.Frame.empty());check(g.playerSpread()<base*1.01&&g.player.gun().sprayStep==0,"Pausa entre rajadas restaura o primeiro disparo");
        double idle=g.playerSpread();g.player.grounded=false;check(g.playerSpread()>idle*20,"Tiro no ar recebe penalidade forte de precisão");g.player.grounded=true;
        g.player.gun().ammo=5;g.reload(g.player);g.combat.equip(3);advance(g,2.5,Input.Frame.empty());check(g.player.primary.ammo==5&&g.player.primary.reload==0,"Trocar de arma cancela recarga sem conceder munição");
        g.combat.equip(2);g.weaponEquip=0;g.combat.inspect=2;g.firePlayer();check(g.combat.inspect==0,"Disparo interrompe inspeção imediatamente");
        int[] rounds=new int[3];int[] rates={60,120,144};for(int i=0;i<3;i++){g.player.primary=new Gun(Weapon.WISP);g.player.slot=2;g.weaponEquip=0;int before=g.trainingShots;for(int j=0;j<rates[i];j++)g.tick(1./rates[i],trigger(true,j==0));rounds[i]=g.trainingShots-before;}
        check(Arrays.stream(rounds).max().orElse(0)-Arrays.stream(rounds).min().orElse(0)<=1,"Cadência automática não cai ao passar de 144 para 60 FPS");
        g.player.primary=new Gun(Weapon.HORIZON);g.aiming=false;double hip=g.playerSpread();g.aiming=true;check(g.playerSpread()<hip*.1,"Luneta estabiliza a precisão da sniper");
        g.close();
    }
    static void melee(){
        Game g=arena();g.combat.equip(3);g.weaponEquip=0;Actor e=enemy(g,42,70.5);e.yaw=0;
        int ammo=g.player.pistol.ammo;g.combat.attack(false);advance(g,.10,Input.Frame.empty());check(e.hp==100,"Corte respeita antecipação antes do contato");advance(g,.10,Input.Frame.empty());check(e.hp==50&&g.player.pistol.ammo==ammo,"Corte causa 50 de dano frontal sem gastar munição");
        g.combat.attack(false);advance(g,.10,Input.Frame.empty());check(e.hp==50,"Golpe não repete dano dentro da mesma animação");advance(g,.25,Input.Frame.empty());g.combat.attack(false);advance(g,.18,Input.Frame.empty());check(e.dead&&g.player.kills==1,"Segundo corte elimina e entra no placar");
        advance(g,.5,Input.Frame.empty());e=enemy(g,42,70.5);e.yaw=Math.PI;g.combat.attack(true);advance(g,.30,Input.Frame.empty());check(e.dead,"Golpe forte pelas costas causa 150 de dano");
        advance(g,1,Input.Frame.empty());e=enemy(g,42,70.5);e.yaw=0;g.combat.attack(true);advance(g,.30,Input.Frame.empty());check(e.hp==25,"Golpe forte frontal causa 75 de dano");
        advance(g,1,Input.Frame.empty());e=enemy(g,42,68);g.combat.attack(false);advance(g,.2,Input.Frame.empty());check(e.hp==100,"Lâmina não alcança inimigo a quatro metros");
        advance(g,.5,Input.Frame.empty());e=enemy(g,43.5,72);g.combat.attack(false);advance(g,.2,Input.Frame.empty());check(e.hp==100,"Golpe exige alvo dentro do arco à frente");
        advance(g,.5,Input.Frame.empty());e=enemy(g,42,70.5);Box wall=new Box(41,0,71,43,3,71.2,0);g.world.temporary.add(wall);g.combat.attack(true);advance(g,.35,Input.Frame.empty());check(e.hp==100,"Golpe corpo a corpo não atravessa parede");g.world.temporary.clear();
        advance(g,1,Input.Frame.empty());g.combat.attack(false);g.combat.equip(2);advance(g,.3,Input.Frame.empty());check(e.hp==100,"Troca de arma cancela golpe pendente");
        g.combat.equip(3);g.weaponEquip=0;int shots=g.trainingShots;g.tick(.016,trigger(true,true));check(g.trainingShots==shots&&g.combat.swing>0&&!g.aiming,"Tecla 3 usa combate corpo a corpo em vez do gatilho da pistola");
        g.close();
    }
    static void collection()throws Exception{
        Path dir=Files.createTempDirectory("rift-loadout-");try{Path path=dir.resolve("profile.properties");Profile p=new Profile(path);
            p.skins[Weapon.ECHO.ordinal()]=3;p.charms[Weapon.ECHO.ordinal()]=4;p.skins[Weapon.TALON.ordinal()]=5;p.melee=2;p.meleeSkin=1;p.save();Profile saved=new Profile(path);
            check(saved.skin(Weapon.ECHO)==Cosmetics.Skin.PULSE&&saved.charm(Weapon.ECHO)==Cosmetics.Charm.STAR,"Skin e pingente persistem por arma");
            check(saved.skin(Weapon.TALON)==Cosmetics.Skin.ROYAL&&saved.skin(Weapon.SPARK)==Cosmetics.Skin.STANDARD,"Visuais de armas distintas são independentes");
            check(saved.blade()==Cosmetics.Melee.AXE&&saved.meleeSkin==1,"Modelo e acabamento corpo a corpo persistem");
            Files.writeString(path,"skin.ECHO=999\ncharm.ECHO=-2\nmelee=oops\nmeleeSkin=800\n");saved=new Profile(path);check(saved.skins[7]<6&&saved.charms[7]==0&&saved.melee==0&&saved.meleeSkin<6,"Perfil antigo ou inválido não quebra a coleção");
        }finally{try(var files=Files.list(dir)){for(Path p:files.toList())Files.delete(p);}Files.delete(dir);}
        Game g=arena();View v=new View(g);v.collectionUI.open();v.collectionUI.skin=3;v.collectionUI.charm=1;v.collectionUI.equip(false);
        check(g.profile.skin(Weapon.ECHO)==Cosmetics.Skin.PULSE&&g.profile.charm(Weapon.ECHO)==Cosmetics.Charm.CRYSTAL,"Equipar na coleção altera o equipamento usado em partida");
        double accuracy=g.playerSpread();v.collectionUI.skin=5;v.collectionUI.equip(true);check(Arrays.stream(g.profile.skins).allMatch(i->i==5)&&g.playerSpread()==accuracy,"Aplicar a todas mantém estatísticas de combate");
        v.collectionUI.blades=true;v.collectionUI.blade=1;v.collectionUI.equip(false);check(g.profile.blade()==Cosmetics.Melee.HOOK,"Seleção da lâmina equipa modelo correspondente");
        BufferedImage image=new BufferedImage(960,540,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();v.render(graphics,960,540);v.click((int)(1140*.75),(int)(49*.75));check(g.ui.equals("play"),"Voltar da coleção funciona em janela reduzida");graphics.dispose();
        Renderer renderer=new Renderer(480,270);Set<Integer> hashes=new HashSet<>();for(Cosmetics.Skin s:Cosmetics.Skin.values()){BufferedImage shot=renderer.collectionPreview(Weapon.ECHO,null,s,Cosmetics.Charm.CRYSTAL,2);hashes.add(Arrays.hashCode(shot.getRGB(0,0,480,270,null,0,480)));}check(hashes.size()==6,"Os seis acabamentos geram materiais visivelmente distintos");
        hashes.clear();for(Cosmetics.Melee m:Cosmetics.Melee.values()){BufferedImage shot=renderer.collectionPreview(null,m,Cosmetics.Skin.AURORA,Cosmetics.Charm.NONE,2);hashes.add(Arrays.hashCode(shot.getRGB(0,0,480,270,null,0,480)));}check(hashes.size()==3,"Três armas corpo a corpo possuem malhas distintas");
        g.player.moveSpeed=6;g.combat.tick(.016);for(int i=0;i<200;i++)g.combat.tick(.016);check(Double.isFinite(g.combat.charmAngle)&&Math.abs(g.combat.charmAngle)<.04,"Pingente reage e retorna ao repouso sem instabilidade");
        g.close();
    }
    static void maps(){
        check(World.WIDTH*World.LENGTH/(88*80)>2.6,"Mapas possuem mais de 2,6 vezes a área da versão 1.3");
        for(int i=0;i<3;i++){World w=new World(i);for(Site s:java.util.List.of(w.a,w.b)){
            java.util.List<V> path=w.path(68,120,s.x(),s.z());check(path.size()>80,"Rota longa e conectada até "+s.name()+" no mapa "+i);
            check(!w.blocked(s.x(),s.z(),w.surfaceAt(s.x(),s.z()),.31,1.78),"Ponto "+s.name()+" permanece plantável no mapa "+i);
        }check(!w.path(w.a.x(),w.a.z(),w.b.x(),w.b.z()).isEmpty(),"Rotação entre A e B acessível no mapa "+i);check(w.platforms.stream().anyMatch(b->b.x1()>88&&b.y2()>4),"Novo setor leste possui posição elevada no mapa "+i);}
    }
    static void capture(Path dir)throws Exception{
        Game g=arena();g.settings.quality=2;g.weaponEquip=0;g.noticeTime=0;View v=new View(g);v.collectionUI.open();v.collectionUI.skin=3;v.collectionUI.charm=1;g.visualTime=3;shot(v,dir.resolve("30-colecao.png"));v.collectionUI.equip(false);
        v.collectionUI.blades=true;v.collectionUI.blade=1;v.collectionUI.skin=5;shot(v,dir.resolve("31-laminas.png"));v.collectionUI.equip(false);
        g.ui="play";g.player.x=126;g.player.z=49;g.player.yaw=Math.PI;g.player.pitch=-.035;g.combat.inspect=1.2;shot(v,dir.resolve("32-skin-pingente.png"));
        g.combat.equip(3);g.weaponEquip=0;g.combat.inspect=.6;shot(v,dir.resolve("33-karambit.png"));g.combat.inspect=0;
        for(int i=0;i<3;i++){g.flow.mapIndex=i;g.start(true);g.weaponEquip=0;g.noticeTime=0;g.player.x=138;g.player.z=20;g.player.y=4.2;g.player.yaw=-1.95;g.player.pitch=-.12;shot(v,dir.resolve("34-mapa-amplo-"+i+".png"));}
        BufferedImage sheet=new BufferedImage(1440,780,BufferedImage.TYPE_INT_RGB);Graphics2D gr=sheet.createGraphics();Renderer rr=new Renderer(480,350);for(Cosmetics.Skin s:Cosmetics.Skin.values()){int x=s.ordinal()%3*480,y=s.ordinal()/3*390;gr.drawImage(rr.collectionPreview(Weapon.ECHO,null,s,Cosmetics.Charm.values()[Math.max(1,s.ordinal())],3),x,y,null);View.text(gr,s.label,x+20,y+374,19,new Color(s.accent),true);}gr.dispose();ImageIO.write(sheet,"png",dir.resolve("35-acabamentos.png").toFile());
        mapSheet(dir.resolve("36-mapas-completos.png"));g.close();
    }
    static void mapSheet(Path path)throws Exception{
        BufferedImage sheet=new BufferedImage(1500,570,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(0x102632));g.fillRect(0,0,1500,570);
        for(int i=0;i<3;i++){Game game=arena();game.world=new World(i);View view=new View(game);int x=i*500;View.text(g,World.NAMES[i],x+24,39,22,View.WHITE,true);MapProjection m=new MapProjection(x+24,67,452,418,Math.PI);view.mapTerrain(g,m,true);for(Site site:java.util.List.of(game.world.a,game.world.b)){View.center(g,site.name(),m.x(site.x(),site.z()),m.y(site.x(),site.z())+5,18,View.GOLD,true);}View.text(g,"144 × 128 m / rotas e plataformas",x+24,526,13,View.MUTED,false);game.close();}
        g.dispose();ImageIO.write(sheet,"png",path.toFile());
    }
}
