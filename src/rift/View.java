package rift;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import static rift.Game.*;
import static rift.World.*;

final class View {
    static final Color INK=new Color(0x10232F),WHITE=new Color(0xE9EFE7),MUTED=new Color(0x91AAA9),CORAL=new Color(0xFF6D67),MINT=new Color(0xA4D6BF),GOLD=new Color(0xE4C59A);
    final SettingsUI settingsUI;final Game game;final FlowUI flowUI;final CollectionUI collectionUI;double renderMillis=10,frameMillis=10;final Renderer renderer=new Renderer(854,480);final AdaptiveQuality adaptive=new AdaptiveQuality();
    static final int[] FIXED_WIDTHS={640,854,1066};
    BufferedImage miniTerrain;World miniWorld;
    final List<Button> buttons=new ArrayList<>();
    Category shopCategory;Weapon shopWeapon=Weapon.ECHO;
    final Renderer previewRenderer=new Renderer(350,195);
    double mouseX=-100,mouseY=-100,scale=1,offsetX,offsetY;
    record Button(double x,double y,double w,double h,Runnable action) {boolean contains(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}}
    View(Game game){this.game=game;flowUI=new FlowUI(this);collectionUI=new CollectionUI(this);settingsUI=new SettingsUI(this);}
    void pointer(int x,int y){mouseX=(x-offsetX)/scale;mouseY=(y-offsetY)/scale;}
    boolean click(int x,int y){pointer(x,y);for(int i=buttons.size()-1;i>=0;i--){Button b=buttons.get(i);if(b.contains(mouseX,mouseY)){b.action.run();return true;}}return false;}
    void render(Graphics2D output,int width,int height) {
        long frameStart=System.nanoTime();boolean worldFrame=false;
        buttons.clear();output.setColor(Color.BLACK);output.fillRect(0,0,width,height);
        scale=Math.min(width/1280.,height/720.);offsetX=(width-1280*scale)/2;offsetY=(height-720*scale)/2;
        Graphics2D g=(Graphics2D)output.create();g.translate(offsetX,offsetY);g.scale(scale,scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int internal=game.settings.quality==3?adaptive.width():FIXED_WIDTHS[game.settings.quality];renderer.resize(internal,internal*9/16);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if(!java.util.Set.of("menu","collection","shop","agents","modes","queue","found","loading","tournament","profile","settings").contains(game.ui)){worldFrame=true;long start=System.nanoTime();BufferedImage world=renderer.render(game);renderMillis=renderMillis*.95+(System.nanoTime()-start)/1e6*.05;
            if(!game.settings.smoothUpscale)g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(world,0,0,1280,720,null);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        switch(game.ui){
            case "menu"->menu(g);
            case "collection"->collectionUI.render(g);
            case "agents"->agents(g);
            case "modes"->flowUI.modes(g);case "queue","found"->flowUI.queue(g);case "loading"->flowUI.loading(g);case "tournament"->flowUI.tournament(g);case "profile"->flowUI.profile(g);
            case "help"->help(g);
            case "settings"->settings(g);
            default->{if(game.ui.equals("shop"))shop(g);else{hud(g);if(game.ui.equals("tactical"))tactical(g);else if(game.ui.equals("pause"))pause(g);else if(game.phase==Phase.MATCH)match(g);else if(game.scoreboard)scoreboard(g,120);}}
        }
        if(game.noticeTime>0&&(game.ui.equals("play")||game.ui.equals("shop")||game.ui.equals("tactical"))){int tw=g.getFontMetrics(font(13,true)).stringWidth(game.notice);int y=game.ui.equals("tactical")?678:580;rect(g,640-tw/2-18,y,tw+36,32,new Color(10,28,39,222));center(g,game.notice,640,y+21,13,WHITE,true);}
        g.dispose();
        double elapsed=(System.nanoTime()-frameStart)/1e6;frameMillis=frameMillis*.9+elapsed*.1;if(worldFrame&&game.settings.quality==3)adaptive.sample(elapsed+game.simulationMillis,game.settings.frameLimit);
    }
    void menu(Graphics2D g){
        g.drawImage(Assets.HERO,0,0,1280,720,null);
        g.setPaint(new GradientPaint(0,0,new Color(8,22,32,205),720,0,new Color(8,24,35,0)));g.fillRect(0,0,1280,720);
        logo(g,46,38,25);tracked(g,"RIFT / PROTOCOL",84,58,15,2.6f,WHITE);
        badge(g,"EDIÇÃO JAVA",1110,40,120,26,MINT);
        tracked(g,"01  /  TACTICAL COMBAT",55,146,12,2.7f,MINT);
        text(g,"RIFT",48,252,106,WHITE,true);tracked(g,"PROTOCOL",57,301,34,9.2f,WHITE);
        text(g,"CADA SEGUNDO MUDA A RODADA.",57,344,13,MUTED,true);
        button(g,"JOGAR SOLO",56,386,170,56,true,game.flow::play);
        button(g,"1v1 / MULTIPLAYER",238,386,225,56,false,()->game.multiplayerRequested=true);
        button(g,"CAMPO DE TREINO",56,452,354,46,false,()->game.openAgentSelect(true,true));
        button(g,"AGENTES",56,508,170,44,false,()->game.openAgentSelect(false,false));
        button(g,"CONFIGURAÇÕES",238,508,172,44,false,()->{game.backUi="menu";game.ui="settings";});
        button(g,"CARREIRA LOCAL",56,562,170,40,false,()->game.ui="profile");
        button(g,"COLEÇÃO",238,562,172,40,false,collectionUI::open);
        button(g,"ASSISTIR BOTS",56,612,170,30,false,game::startBots);
        button(g,"SAIR",238,612,172,30,false,()->game.quit=true);
        rect(g,871,540,362,103,new Color(9,25,37,226));Assets.portrait(g,game.settings.agent,883,551,80,80);
        tracked(g,"SEU AGENTE",979,568,9,1.5f,MUTED);text(g,game.agent.name,978,599,25,WHITE,true);text(g,game.agent.role,979,622,10,new Color(game.agent.color),true);
        buttons.add(new Button(871,540,362,103,()->game.openAgentSelect(false,false)));
        line(g,48,661,1232,661,new Color(124,164,168,72),1);
        text(g,"3 MAPAS",54,690,12,WHITE,true);text(g,"DOIS PONTOS. UMA CHANCE POR RODADA.",120,690,10,MUTED,false);
        right(g,"15 ARMAS + 3 LÂMINAS  /  v1.9",1228,690,11,MUTED,false);
    }
    void portrait(Graphics2D g,double x,double y,double s,Agent agent){
        Graphics2D p=(Graphics2D)g.create();p.translate(x,y+Math.sin(game.visualTime*1.6+agent.ordinal())*2);p.scale(s,s);
        Color coat=new Color(agent.color),shadow=new Color(World.shade(agent.color,.62));
        p.setColor(new Color(174,215,193,28));p.drawOval(-122,-139,244,244);p.drawOval(-152,-169,304,304);
        if(agent==Agent.BRUMA)polygon(p,new double[]{-37,-138,-82,-44,-85,95,-43,123,45,113,82,94,77,-37,40,-139},new Color(0x36565A));
        if(agent==Agent.ION){p.setColor(new Color(166,172,219,80));p.setStroke(new BasicStroke(3));p.drawArc(-63,-178,126,119,10,157);for(int i=0;i<4;i++)rect(p,-63+i*37,-165+i%2*6,12,8,coat);}
        polygon(p,new double[]{-77,82,-87,-22,-53,-72,52,-72,84,-19,70,87},shadow);
        polygon(p,new double[]{-66,-22,-49,-68,46,-68,67,-18,37,68,-41,68},coat);
        polygon(p,new double[]{-45,-49,43,-49,35,32,-32,37},new Color(0x27434F));
        polygon(p,new double[]{-49,-70,-76,-35,-117,83,-85,94,-48,8},shadow);
        polygon(p,new double[]{46,-70,83,-31,114,67,78,83,48,8},coat);
        polygon(p,new double[]{-43,34,38,32,52,108,12,140,-3,86,-17,145,-58,111},new Color(0x1C3342));
        polygon(p,new double[]{-29,-99,-22,-140,27,-137,37,-99,19,-65,-16,-65},new Color(0xD4C7AA));
        if(agent==Agent.VERTICE){
            polygon(p,new double[]{-36,-120,-25,-145,12,-151,41,-169,37,-147,48,-153,31,-123,13,-135,-21,-127,-28,-100},new Color(0xDBE5D3));
            polygon(p,new double[]{-25,-116,26,-113,25,-105,-23,-107},new Color(0x28464B));
            line(p,-22,-112,-4,-111,new Color(0xA4E9EB),3);line(p,8,-110,23,-109,new Color(0xA4E9EB),3);
            rect(p,-36,-114,7,21,new Color(0x4E8C90));polygon(p,new double[]{-26,-75,31,-73,39,-57,-4,-52,-43,-64},new Color(0xE7CEAA));
            polygon(p,new double[]{-55,-55,-31,-46,-40,4,-70,11},new Color(0xEAD4AD));
        }else if(agent==Agent.BRUMA){
            polygon(p,new double[]{-51,-90,-45,-137,-18,-160,22,-157,48,-139,53,-87,32,-53,-23,-58},shadow);
            polygon(p,new double[]{-24,-129,24,-127,34,-98,19,-66,-15,-67,-31,-100},new Color(0xB3C6AA));
            polygon(p,new double[]{-33,-107,34,-103,28,-86,-27,-89},new Color(0x1D3944));
            line(p,-22,-100,21,-97,new Color(0xA5EBC5),3);
            polygon(p,new double[]{-21,-86,22,-83,24,-63,-17,-60,-27,-74},new Color(0x34505A));
            for(int i=0;i<3;i++)rect(p,-13+i*9,-75,5,9,new Color(0xA3BCAF));
            polygon(p,new double[]{-46,-63,-62,62,-10,46,22,-58},new Color(0x3C6965));
        }else if(agent==Agent.AUREO){
            polygon(p,new double[]{-31,-123,-23,-149,24,-147,37,-126,26,-118,-25,-118},new Color(0x473F36));
            line(p,-27,-112,30,-112,new Color(0xD8C181),6);polygon(p,new double[]{-45,-65,45,-65,30,42,-30,42},new Color(0x253D4B));polygon(p,new double[]{-7,-61,7,-61,11,-5,0,9,-11,-5},coat);
        }else if(agent==Agent.TRAMA){
            polygon(p,new double[]{-55,-130,-34,-143,-26,-166,27,-162,37,-140,58,-132,47,-121,-47,-121},new Color(0x5E8093));polygon(p,new double[]{-28,-118,29,-117,26,-79,-21,-81},new Color(0x273E50));line(p,-18,-110,19,-110,new Color(0x96EBF4),5);
        }else{
            polygon(p,new double[]{-34,-118,-21,-146,26,-143,42,-115,29,-76,11,-65,-24,-79},new Color(0x4D536F));
            polygon(p,new double[]{-27,-120,32,-117,25,-99,-23,-101},new Color(0xCFD1FF));
            polygon(p,new double[]{-27,-101,25,-99,19,-80,-15,-83},new Color(0x263B51));
            rect(p,-40,-119,13,29,coat);rect(p,32,-114,12,30,coat);
            polygon(p,new double[]{-29,-48,24,-46,23,-18,-5,5,-31,-18},new Color(0xBCBBFA));
            polygon(p,new double[]{-71,-63,-95,-33,-69,-16,-38,-50},new Color(0x858AAD));
            polygon(p,new double[]{52,-60,84,-43,101,-13,68,-9,31,-49},new Color(0x858AAD));
        }
        polygon(p,new double[]{-33,-13,26,-13,27,1,-32,1},new Color(0xBDCAA9));
        for(int i=0;i<3;i++)rect(p,-26+i*17,-39,12,19,new Color(0xA2B6A7));
        if(agent==Agent.BRUMA){
            p.rotate(.12);polygon(p,new double[]{-74,2,64,2,81,65,-71,65},new Color(0x152C3B));polygon(p,new double[]{-62,10,54,10,67,53,-59,53},new Color(0x35665F));
            p.setColor(new Color(0xA2DFBB));p.setStroke(new BasicStroke(2));p.drawOval(-25,15,41,31);line(p,-52,30,54,30,new Color(0x88B3A5),1);rect(p,-85,24,19,26,new Color(0xBAAC94));rect(p,61,27,21,22,new Color(0xBAAC94));
        }else{
            p.rotate(agent==Agent.ION?.25:-.22);rect(p,-81,14,161,19,new Color(0x192E3C));rect(p,53,19,75,8,new Color(0x324A55));rect(p,-49,9,103,7,new Color(0x9BADA9));
            polygon(p,new double[]{-73,15,-105,12,-100,48,-72,35},new Color(0x395764));rect(p,15,28,21,30,new Color(0x243C48));
            rect(p,-70,27,23,20,new Color(0xBEA991));rect(p,41,28,27,16,new Color(0xBEA991));
            if(agent==Agent.ION){p.setColor(new Color(178,176,255,60));p.fillOval(-126,-7,49,49);p.setColor(new Color(0xD9D2FF));p.setStroke(new BasicStroke(2));p.drawOval(-120,-1,37,37);polygon(p,new double[]{-103,4,-96,13,-105,32,-111,21},new Color(0xCCC4FF));}
        }
        p.dispose();
    }
    void hud(Graphics2D g){
        if(game.settings.captions&&!game.audio.caption.isEmpty()&&System.nanoTime()-game.audio.captionAt<1_300_000_000L){rect(g,492,554,296,25,new Color(5,18,28,210));center(g,"[ "+game.audio.caption+" ]",640,572,11,WHITE,true);}
        if(game.settings.highContrast){rect(g,0,582,1280,138,new Color(4,13,21,215));rect(g,459,0,362,95,new Color(4,13,21,215));}
        if(game.sentinels.placingWire){boolean valid=game.sentinels.preview!=null;rect(g,403,468,474,71,new Color(6,20,28,232));center(g,valid?"FIO VÁLIDO • CLIQUE PARA INSTALAR":"MIRE EM PAREDES OPOSTAS ATÉ 12 m",640,496,14,valid?MINT:CORAL,true);center(g,"ALTURA DEFINIDA PELA MIRA • DIREITO / ESC CANCELA",640,522,10,MUTED,false);}
        if(game.player!=null){text(g,game.world.callout(game.cameraActor().x,game.cameraActor().z),28,348,10,MINT,true);
            if(!game.observing&&!game.flow.mode.respawn&&game.orbs.nearest()>=0){center(g,"SEGURE "+game.settings.key(Settings.Action.USE)+"  /  COLETAR ORBE",640,423,12,MINT,true);rect(g,580,434,120,4,new Color(0x28444D));rect(g,580,434,120*game.orbs.progress/.9,4,MINT);}
            if(game.sentinels.watching()){rect(g,916,22,334,70,new Color(7,34,45,235));center(g,"● OLHO REMOTO",1083,47,13,MINT,true);center(g,"CLIQUE: MARCAR / E OU ESC: SAIR",1083,77,10,WHITE,false);}
            if(game.training&&game.bhopTrainer){rect(g,408,486,464,68,new Color(7,25,37,230));center(g,String.format(Locale.ROOT,"%.2f m/s  •  %d SALTOS  •  MÁX %.2f",Math.hypot(game.player.vx,game.player.vz),game.combat.hops,game.combat.bestSpeed),640,513,18,MINT,true);center(g,"ESPAÇO NO POUSO + A / D + GIRO DO MOUSE",640,539,11,WHITE,false);}
        }

        if(game.player==null)return;
        Actor cam=game.cameraActor(),p=game.observing?cam:game.player;
        // Soft edge vignettes preserve contrast without hiding the scene.
        g.setPaint(new GradientPaint(0,0,new Color(4,17,25,95),0,150,new Color(4,17,25,0)));g.fillRect(0,0,1280,150);
        g.setPaint(new GradientPaint(0,600,new Color(4,17,25,0),0,720,new Color(4,17,25,220)));g.fillRect(0,600,1280,120);
        minimap(g);
        rect(g,477,19,326,54,new Color(10,26,36,218));
        text(g,""+game.scoreBlue,504,58,32,MINT,true);text(g,""+game.scoreRed,752,58,32,CORAL,true);
        if(game.training)center(g,"TREINO",640,52,20,WHITE,true);
        else center(g,clock(game.planted?game.spikeTime:game.timer),640,54,27,game.planted||game.timer<15?CORAL:WHITE,true);
        if(!game.training){center(g,game.flow.mode.respawn?game.flow.mode.label+"  /  "+game.flow.mode.target+" ABATES":"RODADA "+game.round+"  /  PRIMEIRO A "+game.flow.mode.target,640,93,10,WHITE,true);for(int i=0;i<5;i++){rect(g,554+i*10,33,6,23,i<game.living(0)?MINT:new Color(80,98,105,130));rect(g,680+i*10,33,6,23,i<game.living(1)?CORAL:new Color(80,98,105,130));}}
        int fy=35;
        for(Feed f:game.feed){rect(g,967,fy-19,289,29,new Color(10,26,35,222));text(g,f.killer(),978,fy,12,f.team()==0?MINT:CORAL,true);center(g,f.head()?"◇":"›",1112,fy,16,WHITE,true);right(g,f.victim(),1244,fy,12,WHITE,false);fy+=35;}
        if(game.fps>0)right(g,game.fps+" FPS  /  "+Math.round(frameMillis+game.simulationMillis)+" ms  /  "+renderer.width+" × "+renderer.height,1250,fy+13,10,MUTED,false);
        String role=game.attackTeam==0?"ATAQUE":"DEFESA";
        if(game.training){text(g,"CAMPO DE TREINO",28,218,12,MINT,true);text(g,"G  agente   ·   H  dano   ·   T  bhop",28,240,11,WHITE,false);text(g,"Acertos: "+game.trainingHits+"  •  Tiros: "+game.trainingShots,28,261,11,MUTED,false);if(!p.melee()){boolean steady=game.playerSpread()<.007;rect(g,28,300,156,4,new Color(0x334C59));rect(g,28,300,156*(1-Math.min(1,game.playerSpread()*10)),4,steady?MINT:GOLD);text(g,steady?"MIRA ESTÁVEL":!p.grounded?"NO AR":p.moveSpeed>1.4?"EM MOVIMENTO":"RECUPERANDO RECUO",28,322,10,steady?MINT:GOLD,true);}}
        else {text(g,(game.flow.mode.respawn?"RESPAWN":role)+" / "+World.NAMES[game.world.mapIndex],28,218,12,MINT,true);text(g,game.planted?"Núcleo armado":game.observing?"Simulação: duas equipes autônomas":p.carrier?"Você carrega o núcleo":"Elimine a equipe adversária",28,240,11,WHITE,false);}
        if(game.phase==Phase.BUY){
            center(g,"FASE DE COMPRA",640,164,30,WHITE,true);center(g,"B  ARSENAL    ·    ENTER  INICIAR RODADA",640,190,12,MINT,true);
        }
        if(game.phase==Phase.END){
            rect(g,383,140,514,110,new Color(12,31,41,231));line(g,383,140,897,140,game.roundWinner==0?MINT:CORAL,3);
            center(g,game.endTitle,640,189,30,game.roundWinner==0?MINT:CORAL,true);center(g,game.endReason,640,224,14,WHITE,false);
        }
        if(game.planted&&game.phase==Phase.LIVE){rect(g,547,111,186,29,new Color(40,26,34,210));center(g,"◆  NÚCLEO ARMADO",640,131,11,CORAL,true);}
        if(game.observing){
            rect(g,364,598,552,106,new Color(8,25,36,232));
            center(g,"OBSERVADOR / BOTS × BOTS",640,623,12,MINT,true);
            center(g,cam.name+" / "+Agent.values()[cam.agentIndex].name+" / "+cam.mind.role.label,640,650,17,WHITE,true);
            center(g,(int)cam.hp+" PV · "+cam.gun().kind.label+" · "+cam.gun().ammo+" TIROS · ESPAÇO: TROCAR POV",640,676,11,MUTED,false);
            text(g,"IA: "+cam.mind.memory.label+" / "+cam.mind.personality.label,28,374,10,MINT,false);
            return;
        }
        if(!p.dead){
            if(game.ui.equals("tactical")){}else if(!(game.aimLerp>.90&&p.gun().kind.scoped()))crosshair(g,p);
            else scope(g);
            text(g,game.agent.name,49,624,11,new Color(game.agent.color),true);
            text(g,""+(int)Math.ceil(p.hp),48,671,42,p.healing>0?MINT:WHITE,true);text(g,"VIDA",51,694,10,MUTED,true);
            if(p.healing>0)text(g,"+ REPARANDO",174,625,10,MINT,true);
            line(g,144,643,144,686,new Color(133,169,166,115),1);
            shield(g,171,652,14,MINT);text(g,""+(int)Math.ceil(p.armor),190,669,25,MINT,true);text(g,"PROTEÇÃO",174,694,9,MUTED,true);
            text(g,"¤  "+p.credits,279,669,18,GOLD,true);text(g,"CRÉDITOS",280,692,9,MUTED,true);
            abilityHUD(g,469,game.settings.key(Settings.Action.Q),agentFor().q,p.qCharges,false);
            abilityHUD(g,574,game.settings.key(Settings.Action.E),agentFor().e,p.eCharges,false);
            abilityHUD(g,679,game.settings.key(Settings.Action.C),agentFor().c,p.cCharges,false);
            abilityHUD(g,784,game.settings.key(Settings.Action.ULT),agentFor().x,p.ult,true);
            Gun gun=p.gun();if(p.melee()){right(g,game.profile.blade().label,1232,625,23,WHITE,true);right(g,"CORTE / GOLPE FORTE",1232,654,11,MINT,true);right(g,"3  CORPO A CORPO    V  INSPECIONAR",1232,695,10,MUTED,false);}else {right(g,p.slot==4?"VEREDITO":p.slot==5?"EXECUÇÃO":gun.kind.label,1232,624,13,WHITE,true);right(g,gun.kind.mode.label,1232,602,9,MUTED,true);right(g,""+gun.ammo,1175,670,45,WHITE,true);text(g,"/ "+gun.reserve,1185,667,22,MUTED,false);right(g,gun.reload>0?"RECARREGANDO":"1 "+p.pistol.kind.label+"   2 "+(p.primary==null?"VAZIO":p.primary.kind.label)+"   3 LÂMINA",1232,696,10,MUTED,false);
            if(gun.reload>0){rect(g,1101,681,131,3,new Color(70,97,107));rect(g,1101,681,131*(1-gun.reload/gun.reloadTotal),3,MINT);}}
            boolean defuse=game.planted&&p.team!=game.attackTeam&&Math.hypot(p.x-game.spikeX,p.z-game.spikeZ)<2.5;
            boolean plant=p.carrier&&game.world.site(p.x,p.z)!=null;
            if((plant||defuse)&&game.phase==Phase.LIVE){
                center(g,"SEGURE "+game.settings.key(Settings.Action.USE)+" PARA "+(defuse?"DESARMAR":"PLANTAR"),640,490,14,WHITE,true);
                double progress=defuse?game.defuseProgress/5:game.plantProgress/3.2;
                rect(g,533,505,214,5,new Color(11,28,38,210));rect(g,533,505,214*progress,5,MINT);
            }
            if(game.hitMarker>0){Color c=game.hitHead>0?CORAL:WHITE;for(int s:new int[]{-1,1}){line(g,640+s*7,360+s*7,640+s*13,360+s*13,c,2);line(g,640+s*7,360-s*7,640+s*13,360-s*13,c,2);}}
            if(game.damageFlash>0){g.setColor(new Color(220,75,64,(int)(game.damageFlash*115)));g.setStroke(new BasicStroke(24));g.drawRect(0,0,1280,720);double angle=game.damageYaw-p.yaw,sx=Math.sin(angle),sy=-Math.cos(angle);polygon(g,new double[]{640+sx*77,360+sy*77,640+sx*94+sy*9,360+sy*94-sx*9,640+sx*94-sy*9,360+sy*94+sx*9},new Color(255,125,101,(int)(game.damageFlash*350)));}
            if(game.focus>0){rect(g,488,611,300,20,new Color(42,38,25,225));center(g,String.format(Locale.ROOT,"FOCO  %.1fs   /   +30%% CADÊNCIA",game.focus),640,625,10,GOLD,true);rect(g,488,633,300*game.focus/12,2,GOLD);}
        }else{
            center(g,"ELIMINADO",640,636,24,CORAL,true);center(g,cam==p?(game.flow.mode.respawn?"RESPAWN EM "+Math.max(0,(int)Math.ceil(p.respawn))+" s":"Aguardando o fim da rodada"):"ASSISTINDO "+cam.name+"  ·  ESPAÇO PARA TROCAR",640,664,12,WHITE,true);
        }
        markers(g,cam);
        text(g,game.settings.key(Settings.Action.SCORE)+" placar  ·  "+game.settings.key(Settings.Action.PING)+" ping  ·  F10 ajustes",28,282,10,new Color(219,234,222,150),false);
        if(p.flash>0){rect(g,0,0,1280,720,new Color(game.settings.softFlash?35:255,game.settings.softFlash?43:247,game.settings.softFlash?51:224,(int)(255*Math.min(1,p.flash/.7))));center(g,"CLARÃO",640,438,13,new Color(154,137,107),true);}
        if(p.emp>0||p.detained>0)center(g,p.detained>0?"CONFINADO  /  ARMAS BLOQUEADAS":"SUPRIMIDO  /  HABILIDADES BLOQUEADAS",640,559,12,CORAL,true);
        if(p.returnTime>0)center(g,"RENASCER  "+(int)Math.ceil(p.returnTime)+" s",640,542,12,GOLD,true);
        combatFeedback(g);
    }
    Agent agentFor(){return game.agent;}
    void abilityHUD(Graphics2D g,int x,String key,Ability ability,int charges,boolean ult){
        boolean active=game.player!=null&&(game.sentinels.own(game.player,ability)!=null||ability==Ability.VERDICT&&game.player.specialPistol!=null&&game.player.specialPistol.ammo>0||ability==Ability.RAIL&&game.player.specialRifle!=null&&game.player.specialRifle.ammo>0);
        Color color=active?GOLD:(ult?charges>=6:charges>0)||game.training?WHITE:MUTED;
        rect(g,x,639,76,45,new Color(13,33,43,180));line(g,x,684,x+76,684,color,1);
        center(g,key,x+14,657,11,MINT,true);icon(g,ability,x+43,661,13,color);
        if(ult){for(int i=0;i<6;i++)rect(g,x+i*13,689,10,3,i<charges||game.training?GOLD:new Color(70,92,98));}
        else if(ability==game.agent.e&&charges==0&&game.player.eRegen>0)center(g,(int)Math.ceil(30-game.player.eRegen)+"s",x+38,697,9,MUTED,true);
        else for(int i=0;i<charges;i++)rect(g,x+28+i*12,689,8,3,MINT);
        center(g,abilityName(ability),x+38,710,9,color,true);
    }
    void crosshair(Graphics2D g,Actor p){SettingsUI.cross(g,game.settings,640,360,p.melee()?0:(int)Math.min(30,game.playerSpread()*420));}
    void scope(Graphics2D g){Area mask=new Area(new Rectangle2D.Double(0,0,1280,720));mask.subtract(new Area(new Ellipse2D.Double(344,64,592,592)));g.setColor(new Color(2,10,15,245));g.fill(mask);line(g,344,360,936,360,new Color(15,35,39),1);line(g,640,64,640,656,new Color(15,35,39),1);rect(g,638,358,4,4,CORAL);}
    void minimap(Graphics2D g){
        int x=24,y=22,w=176,h=177;rect(g,x,y,w,h,new Color(9,26,35,228));Actor cam=game.cameraActor();
        MapProjection m=new MapProjection(x+12,y+13,152,146,cam.yaw);mapTerrain(g,m,false);
        if(game.planted){g.setColor(CORAL);g.fill(new Ellipse2D.Double(m.x(game.spikeX,game.spikeZ)-4,m.y(game.spikeX,game.spikeZ)-4,8,8));}
        for(Smoke smoke:game.smokes){double r=smoke.radius()*m.scale;g.setColor(new Color(184,215,211,75));g.fill(new Ellipse2D.Double(m.x(smoke.x,smoke.z)-r,m.y(smoke.x,smoke.z)-r,r*2,r*2));}
        for(Actor a:game.actors)if(!a.dead&&(game.observing||a.team==0||a.revealed>0)) {
            mapArrow(g,m,a.x,a.z,a.yaw,a==cam?6:4,a==cam?WHITE:a.team==0?MINT:CORAL);
        }
        if(game.abilities.pingLife>0){V ping=game.abilities.pingPoint;double px=m.x(ping.x(),ping.z()),py=m.y(ping.x(),ping.z());polygon(g,new double[]{px,py-5,px+5,py,px,py+5,px-5,py},GOLD);}
        center(g,"▲ DIREÇÃO DA VISÃO",x+w/2,y+h-6,8,MUTED,true);line(g,x,y,x+30,y,MINT,2);line(g,x,y,x,y+20,MINT,2);
    }
    MapProjection tacticalMap(){boolean global=game.tacticalAbility==Ability.GLOBAL_TELEPORT;return new MapProjection(640,342,240,global?Math.hypot(World.WIDTH,World.LENGTH)/2+2:game.tacticalRange()+2,(TacticalUI.chemical(game)||TacticalUI.astral(game)?0:game.tacticalYaw),global?World.WIDTH/2:game.player.x,global?World.LENGTH/2:game.player.z);}
    Shape mapShape(MapProjection m,double x,double z,double xx,double zz){Path2D p=new Path2D.Double();p.moveTo(m.x(x,z),m.y(x,z));p.lineTo(m.x(xx,z),m.y(xx,z));p.lineTo(m.x(xx,zz),m.y(xx,zz));p.lineTo(m.x(x,zz),m.y(x,zz));p.closePath();return p;}
    void mapTerrain(Graphics2D g,MapProjection m,boolean large){
        if(!large){
            if(miniWorld!=game.world||miniTerrain==null){miniWorld=game.world;miniTerrain=new BufferedImage(576,512,BufferedImage.TYPE_INT_RGB);Graphics2D cached=miniTerrain.createGraphics();cached.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);drawTerrain(cached,new MapProjection(0,0,576,512,0),false);cached.dispose();}
            double k=m.scale/4;g.drawImage(miniTerrain,new AffineTransform(m.cos*k,-m.sin*k,m.sin*k,m.cos*k,m.x(0,World.LENGTH),m.y(0,World.LENGTH)),null);
            for(Site site:List.of(game.world.a,game.world.b))center(g,site.name(),m.x(site.x(),site.z()),m.y(site.x(),site.z())+4,11,GOLD,true);return;
        }
        drawTerrain(g,m,true);
    }
    void drawTerrain(Graphics2D g,MapProjection m,boolean large){
        g.setColor(new Color(0x4F6974));g.fill(mapShape(m,0,0,World.WIDTH,World.LENGTH));
        if(large){double k=m.scale/game.world.texScale;g.drawImage(game.world.ground,new AffineTransform(m.cos*k,-m.sin*k,-m.sin*k,-m.cos*k,m.x(0,0),m.y(0,0)),null);}
        g.setColor(new Color(0x708591));g.fill(mapShape(m,30,3,34,57));g.fill(mapShape(m,2,27,62,31));g.fill(mapShape(m,4,77,140,82));g.fill(mapShape(m,62,4,66,123));g.fill(mapShape(m,111,3,115,116));g.fill(mapShape(m,4,109,140,113));
        for(Site site:List.of(game.world.a,game.world.b)){g.setColor(new Color(site==game.world.a?0xA18468:0x5F8D91));g.fill(mapShape(m,site.x()-5,site.z()-5,site.x()+5,site.z()+5));}
        for(Box b:game.world.solids){if(b.x1()<0||b.z1()<0||b.x1()>=World.WIDTH||b.z1()>=World.LENGTH)continue;
            g.setColor(new Color(0x304652));g.fill(mapShape(m,b.x1()+.8,b.z1()+.8,b.x2()+.8,b.z2()+.8));
            Shape block=mapShape(m,b.x1(),b.z1(),Math.min(World.WIDTH,b.x2()),Math.min(World.LENGTH,b.z2()));g.setColor(new Color(b.y2()>4?0x162F40:0x334F5E));g.fill(block);
            if(large){g.setColor(new Color(0x8BA2A8));g.setStroke(new BasicStroke(1));g.draw(block);}
        }
        for(World.Ramp ramp:game.world.ramps){Shape shape=mapShape(m,ramp.x1(),ramp.z1(),ramp.x2(),ramp.z2());g.setColor(new Color(large?0xB29167:0x8BA5A3));g.fill(shape);if(large){center(g,"↗",m.x((ramp.x1()+ramp.x2())/2,(ramp.z1()+ramp.z2())/2),m.y((ramp.x1()+ramp.x2())/2,(ramp.z1()+ramp.z2())/2)+4,12,GOLD,true);}}
        if(large)for(Site site:List.of(game.world.a,game.world.b))center(g,site.name(),m.x(site.x(),site.z()),m.y(site.x(),site.z())+8,24,GOLD,true);
    }
    void mapArrow(Graphics2D g,MapProjection m,double wx,double wz,double heading,double size,Color color){
        double px=m.x(wx,wz),py=m.y(wx,wz),rx=Math.sin(heading)*m.cos-Math.cos(heading)*m.sin,fy=Math.sin(heading)*m.sin+Math.cos(heading)*m.cos;
        polygon(g,new double[]{px+rx*size,py-fy*size,px-rx*size*.7-fy*size*.6,py+fy*size*.7-rx*size*.6,px-rx*size*.4,py+fy*size*.4,px-rx*size*.7+fy*size*.6,py+fy*size*.7+rx*size*.6},color);
    }
    void markers(Graphics2D g,Actor cam){
        for(Actor a:game.actors)if(!a.dead&&a!=cam&&(a.team==0||a.revealed>0)){
            if(a.team==0&&!game.world.visible(cam.eye(),a.center()))continue;
            double[] p=renderer.project(new V(a.x,a.y+2.1,a.z));if(p==null||p[0]<.1||p[0]>.96||p[1]<.2||p[1]>.8)continue;
            double x=p[0]*1280,y=p[1]*720;Color c=a.team==0?MINT:CORAL;
            if(a.team!=cam.team&&a.revealed>.5){
                double[] bottom=renderer.project(new V(a.x,a.y+.08,a.z)),top=renderer.project(new V(a.x,a.y+1.83,a.z));
                if(bottom!=null&&top!=null){double h=Math.max(12,bottom[1]*720-top[1]*720),w=h*.42,xx=x-w/2,yy=top[1]*720;
                    rect(g,xx,yy,w,h,new Color(255,108,91,38));g.setColor(new Color(255,141,117,210));g.setStroke(new BasicStroke(1.6f));g.draw(new RoundRectangle2D.Double(xx,yy,w,h,8,8));
                    line(g,x,yy+h*.25,x,yy+h*.66,new Color(255,173,135),2);line(g,x-w*.3,yy+h*.4,x+w*.3,yy+h*.4,CORAL,2);
                    line(g,x,yy+h*.66,x-w*.22,yy+h*.91,CORAL,2);line(g,x,yy+h*.66,x+w*.22,yy+h*.91,CORAL,2);
                }
            }
            polygon(g,new double[]{x-4,y-9,x+4,y-9,x,y-3},c);
            center(g,a.team==0?a.name:"DETECTADO",x,y-16,9,c,true);
        }
    }
    void combatFeedback(Graphics2D g){
        if(game.player==null)return;
        if(game.focus>0&&!game.player.dead){
            int alpha=22+(int)(8*Math.sin(game.visualTime*5));g.setPaint(new GradientPaint(0,0,new Color(255,209,122,alpha),135,0,new Color(255,209,122,0)));g.fillRect(0,0,135,720);g.setPaint(new GradientPaint(1145,0,new Color(255,209,122,0),1280,0,new Color(255,209,122,alpha)));g.fillRect(1145,0,135,720);
        }
        if(game.dashTime>0){
            double t=1-game.dashTime/.4;g.setStroke(new BasicStroke(1.5f));
            for(int i=0;i<22;i++){double a=i*Math.PI/11+.07*Math.sin(i*19),r=290+((i*53+t*950)%180);Color col=new Color(168,238,222,(int)(80*Math.sin(Math.PI*t)));line(g,640+Math.cos(a)*r,360+Math.sin(a)*r*.6,640+Math.cos(a)*(r+90),360+Math.sin(a)*(r+90)*.6,col,1.5f);}
        }
        if(game.ultimateBurst>0){double t=1-game.ultimateBurst/1.2;g.setColor(new Color(game.agent.color>>16&255,game.agent.color>>8&255,game.agent.color&255,(int)(Math.max(0,1-t)*90)));g.setStroke(new BasicStroke((float)(2+7*(1-t))));g.draw(new Ellipse2D.Double(640-t*750,360-t*750,t*1500,t*1500));}
        if(game.scan>0){rect(g,1030,108,220,46,new Color(19,32,52,220));text(g,"PULSO / "+game.scanCount+" DETECTADOS",1043,126,10,new Color(190,195,255),true);rect(g,1043,139,194*game.scan/5,3,new Color(168,175,235));}
        if(game.killToast>0&&game.aceTime<=0){
            double pop=Smoke.smooth(Math.min(1,(2.4-game.killToast)/.15));double y=491+(1-pop)*16;Color col=game.hitHead>0?CORAL:GOLD;
            g.setColor(new Color(8,25,35,195));g.fill(new Ellipse2D.Double(610,y-24,60,60));g.setColor(col);g.setStroke(new BasicStroke(2));g.draw(new Ellipse2D.Double(613,y-21,54,54));
            center(g,""+game.lastKillCount,640,y+17,26,col,true);center(g,game.lastKillCount==1?"ELIMINAÇÃO":game.lastKillCount+" ELIMINAÇÕES",640,y+53,10,WHITE,true);
            for(int i=0;i<5;i++)rect(g,603+i*16,y+62,11,3,i<game.lastKillCount?GOLD:new Color(66,87,95));
        }
        if(game.aceTime>0){
            double age=6-game.aceTime,enter=Smoke.smooth(age/.35),fade=Math.min(enter,game.aceTime/.5);Graphics2D a=(Graphics2D)g.create();a.setComposite(AlphaComposite.SrcOver.derive((float)Settings.clamp(fade,0,1)));a.translate(0,(1-enter)*25);
            rect(a,420,277,440,175,new Color(9,29,39,225));Color col=game.aceTeam==0?GOLD:CORAL;line(a,420,277,860,277,col,2);line(a,460,452,820,452,col,1);
            tracked(a,"CINCO ADVERSÁRIOS. UMA RODADA.",464,305,9,1.5f,MUTED);center(a,"ACE",640,398,94,col,true);center(a,game.aceName.equals("VOCÊ")?"EQUIPE ELIMINADA POR VOCÊ":game.aceName+" ELIMINOU A EQUIPE",640,424,11,WHITE,true);
            for(int i=0;i<5;i++)polygon(a,new double[]{602+i*19,439,606+i*19,435,610+i*19,439,606+i*19,443},col);a.dispose();
        }
    }
    void tactical(Graphics2D g){TacticalUI.draw(this,g);}
    void shop(Graphics2D g){
        rect(g,0,0,1280,720,new Color(9,22,34));logo(g,32,28,25);tracked(g,"EQUIPAMENTO / RIFT",71,48,12,2,MUTED);
        text(g,"ARSENAL",30,115,47,WHITE,true);text(g,"15 armas. Escolha seu estilo de combate.",33,143,13,MUTED,false);button(g,"SKINS E PINGENTES",657,102,276,36,false,collectionUI::open);
        right(g,game.training||game.flow.mode.respawn?"CRÉDITOS LIVRES":"¤ "+game.player.credits,1244,106,26,GOLD,true);right(g,game.training?"CAMPO DE TREINO":game.flow.mode.respawn?"RESPAWN / EQUIPAMENTO LIVRE":"FASE DE COMPRA  /  "+clock(game.timer),1243,134,10,MUTED,true);
        button(g,"TODAS",32,167,103,34,shopCategory==null,()->shopCategory=null);
        Category[] categories=Category.values();int[] widths={106,165,92,120,111,102};int tabX=145;
        for(int i=0;i<categories.length;i++){Category cat=categories[i];button(g,cat.label,tabX,167,widths[i],34,shopCategory==cat,()->shopCategory=cat);tabX+=widths[i]+9;}
        List<Weapon> guns=Arrays.stream(Weapon.values()).filter(w->shopCategory==null||w.category==shopCategory).toList();
        for(int i=0;i<guns.size();i++){
            Weapon w=guns.get(i);int x=32+(i%5)*183,y=204+(i/5)*119;boolean owned=owns(w),selected=w==shopWeapon,hover=mouseX>=x&&mouseX<x+171&&mouseY>=y&&mouseY<y+109;
            rect(g,x,y,171,109,new Color(selected?0x294951:hover?0x233D4A:0x172F3E));line(g,x,y,x+171,y,selected?MINT:new Color(49,76,89),selected?2:1);
            text(g,w.label,x+12,y+23,15,WHITE,true);right(g,owned?"✓":w.sidearm()?"01":"02",x+158,y+22,10,owned?MINT:MUTED,true);
            gunDrawing(g,x+12,y+33,149,46,w);text(g,w.price==0?"GRÁTIS":"¤ "+w.price,x+12,y+99,11,game.training||game.player.credits>=w.price?GOLD:MUTED,true);right(g,owned?"EQUIPADA":w.mag+" TIROS",x+158,y+99,9,owned?MINT:MUTED,false);
            buttons.add(new Button(x,y,171,109,()->{shopWeapon=w;game.audio.play("select");}));
        }
        int x=966;Weapon w=shopWeapon;rect(g,x,167,282,461,new Color(0x18323F));text(g,w.type,x+17,192,9,MINT,true);text(g,w.label,x+15,230,30,WHITE,true);
        BufferedImage preview=previewRenderer.collectionPreview(w,null,game.profile.skin(w),game.profile.charm(w),game.visualTime);g.drawImage(preview,x+7,245,268,150,null);
        text(g,w.mode.label+(w.silenced()?" / SILENCIADA":""),x+17,403,9,GOLD,true);wrap(g,w.detail(),x+17,427,247,12,WHITE);
        stat(g,"DANO / CABEÇA",w.body+" / "+w.head+(w.pellets>1?" × "+w.pellets:""),x+17,475,w.body/100.);
        stat(g,"CADÊNCIA",w.mode==FireMode.BURST?"3 TIROS / RAJADA":String.format(Locale.ROOT,"%.1f TIROS/S",1/w.interval),x+17,507,(1/w.interval)/16);
        stat(g,"RECARGA",String.format(Locale.ROOT,"%.2f s   ·   %d / %d",w.reload,w.mag,w.reserve),x+17,539,1-w.reload/5);
        boolean owned=owns(w),can=game.training||game.player.credits>=w.price;
        button(g,owned?"EQUIPADA":can?(w.price==0?"EQUIPAR GRÁTIS":"COMPRAR  /  ¤ "+w.price):"CRÉDITOS INSUFICIENTES",x+15,576,252,38,owned||can,()->game.buyWeapon(w));
        for(int i=0;i<3;i++){final int slot=new int[]{0,3,1}[i];button(g,new String[]{"Q","E","C"}[i]+" "+abilityName(game.agent.slot(slot))+"  "+game.charges(slot)+"/"+game.agent.charges(slot)+"  ¤200",32+i*304,569,291,42,false,()->game.buyAbility(slot));}
        button(g,"SALVAR LOADOUT",32,617,220,26,false,()->{if(game.player.primary!=null)game.profile.savedPrimary=game.player.primary.kind;game.profile.savedPistol=game.player.pistol.kind;game.profile.save();game.tell("Loadout salvo",2);});button(g,"RECOMPRAR ["+game.settings.key(Settings.Action.QUICKBUY)+"]",266,617,264,26,false,game::quickBuy);button(g,"PEDIR CRÉDITOS ["+game.settings.key(Settings.Action.FUNDS)+"]",544,617,385,26,false,game::requestFunds);
        line(g,32,646,1248,646,new Color(54,79,90),1);
        button(g,"PROTEÇÃO LEVE  /  ¤ 400",32,651,238,41,false,()->game.buyArmor(25));button(g,"PROTEÇÃO COMPLETA  /  ¤ 1.000",282,651,279,41,false,()->game.buyArmor(50));
        text(g,"PROTEÇÃO  "+(int)game.player.armor+" / 50",590,666,11,MINT,true);text(g,"Compra substitui apenas a arma do mesmo slot.",590,687,10,MUTED,false);button(g,"VOLTAR  [ B / ESC ]",966,651,282,41,true,()->game.ui="play");
    }
    boolean owns(Weapon w){return w.sidearm()?game.player.pistol.kind==w:game.player.primary!=null&&game.player.primary.kind==w;}
    void stat(Graphics2D g,String name,String value,int x,int y,double amount){text(g,name,x,y,9,MUTED,true);right(g,value,x+248,y,10,WHITE,true);rect(g,x,y+8,248,3,new Color(45,71,81));rect(g,x,y+8,248*Settings.clamp(amount,0,1),3,new Color(shopWeapon.color));}
    void gunDrawing(Graphics2D g,int x,int y,int w,int h,Weapon gun){
        Graphics2D p=(Graphics2D)g.create();p.translate(x,y);p.scale(w/230.,h/68.);Color tint=new Color(gun.color),metal=new Color(0x587582);
        if(gun.sidearm()){
            if(gun==Weapon.DART){polygon(p,new double[]{55,19,162,19,177,23,177,34,110,34,98,52,73,56,67,32,55,32},tint);rect(p,119,25,58,3,INK);rect(p,89,34,32,9,metal);}
            else {int end=gun==Weapon.VEIL?187:gun==Weapon.TALON?169:153;polygon(p,new double[]{60,15,end,15,end,29,114,29,104,37,101,57,72,57,78,29,60,29},tint);rect(p,73,36,23,20,metal);if(gun==Weapon.VEIL)rect(p,153,18,38,13,metal);if(gun==Weapon.TALON){p.setColor(metal);p.fillOval(94,18,24,20);rect(p,132,29,33,5,metal);}if(gun==Weapon.RUSH)rect(p,75,52,24,13,metal);}
        }else {
            double end=gun.category==Category.SMG?184:gun==Weapon.HORIZON?224:213;
            polygon(p,new double[]{8,26,43,28,58,22,159,22,168,27,end,27,end,34,165,34,158,42,82,42,77,58,62,56,63,39,40,37,8,47},tint);
            rect(p,61,17,84,7,metal);rect(p,107,40,gun==Weapon.BASTION?40:18,gun==Weapon.RIDGE?11:22,metal);
            if(gun==Weapon.BREACH){p.setColor(metal);p.fillOval(104,36,35,30);}if(gun==Weapon.MARROW)rect(p,143,31,32,12,new Color(0xA9805B));if(gun.silenced())rect(p,194,23,32,15,metal);
            if(gun.scoped()){rect(p,79,5,58,12,metal);rect(p,89,15,7,7,tint);rect(p,120,15,7,7,tint);}else if(gun==Weapon.HELIX)rect(p,87,7,29,10,metal);
            for(int i=0;i<4;i++)rect(p,122+i*8,29,4,4,INK);
        }
        p.dispose();
    }
    void pause(Graphics2D g){
        rect(g,0,0,1280,720,new Color(6,20,30,224));tracked(g,"RIFT / PROTOCOL",490,141,14,3,MUTED);
        center(g,"PAUSADO",640,226,53,WHITE,true);center(g,"A partida fica parada enquanto você estiver aqui.",640,265,13,MUTED,false);
        button(g,"VOLTAR À PARTIDA",452,315,376,55,true,()->game.ui="play");
        button(g,"CONFIGURAÇÕES",452,383,376,49,false,()->{game.backUi="pause";game.ui="settings";});
        button(g,"MENU PRINCIPAL",452,445,376,49,false,()->{game.ui="menu";game.phase=Phase.MENU;});
        button(g,"SAIR DO JOGO",452,507,376,49,false,()->game.quit=true);
        button(g,"COLEÇÃO",452,567,376,34,false,collectionUI::open);
        center(g,"ESC para continuar",640,617,11,MUTED,false);
    }
    void settings(Graphics2D g){settingsUI.draw(g);}
    void agents(Graphics2D g){flowUI.agents(g);}
    void help(Graphics2D g){
        rect(g,0,0,1280,720,new Color(8,23,33,249));text(g,"DOMINE O BÁSICO",47,75,38,WHITE,true);text(g,"Movimento, precisão e objetivo.",50,109,14,MUTED,false);
        String[][] keys={{"W A S D","Mover"},{"MOUSE","Olhar e mirar"},{"CLIQUE ESQUERDO","Atirar; no mapa, marcar ponto"},{"CLIQUE DIREITO","Mira / golpe forte; no mapa, desfazer"},{"R / 1 / 2 / 3","Recarga / pistola / principal / lâmina"},{"SHIFT / CTRL","Andar devagar / agachar"},{"ESPAÇO","Pular / trocar aliado ao morrer"},{"F (SEGURAR)","Plantar 3,2 s / desarmar 5 s"},{"B / ENTER","Arsenal / confirmar ação"},{"G / H / V","Agente / dano no treino / inspecionar"},{"TAB / ESC / F11","Placar / pausa / tela cheia"}};
        for(int i=0;i<keys.length;i++){int y=163+i*42;text(g,keys[i][0],53,y,11,MINT,true);text(g,keys[i][1],250,y,12,WHITE,false);line(g,52,y+14,590,y+14,new Color(45,71,83),1);}
        text(g,"HABILIDADES / "+Agent.values()[game.settings.agent].name,674,161,16,WHITE,true);
        Agent a=Agent.values()[game.settings.agent];Ability[] abs={a.q,a.e,a.c,a.x};String[] ks={"Q","E","C","X"};
        for(int i=0;i<4;i++){int y=195+i*72;icon(g,abs[i],691,y-4,14,MINT);text(g,ks[i]+"  "+abilityName(abs[i]),722,y,14,WHITE,true);wrap(g,abilityDescription(abs[i]),674,y+27,520,12,MUTED);}
        text(g,"O OBJETIVO",674,490,16,WHITE,true);
        wrap(g,"Z marca uma posição para sua equipe. Atacantes levam o núcleo a A ou B. Defensores impedem o plantio ou desarmam. O núcleo detona após 40 s. Uma vida por rodada; o placar necessário depende do modo escolhido.",674,522,510,13,MUTED);
        wrap(g,"Pare de se mover para atirar com precisão. Cada rodada concluída e cada eliminação carregam a suprema. Confira a troca de lados e a condição de vitória ao escolher o modo.",674,599,510,12,MUTED);
        button(g,"ENTENDI",986,652,246,40,true,()->game.ui=game.helpReturn);
    }
    void scoreboard(Graphics2D g,int y){
        rect(g,247,y,786,481,new Color(9,27,38,239));text(g,"PLACAR DA PARTIDA",272,y+36,20,WHITE,true);right(g,game.scoreBlue+" : "+game.scoreRed,1004,y+39,26,MINT,true);
        text(g,"JOGADOR",277,y+74,10,MUTED,true);text(g,"K",721,y+74,10,MUTED,true);text(g,"D",787,y+74,10,MUTED,true);text(g,"ACE",840,y+74,10,GOLD,true);text(g,"CRÉDITOS",908,y+74,10,MUTED,true);
        for(int i=0;i<game.actors.size();i++){
            Actor a=game.actors.get(i);int yy=y+105+i*33+(i>=5?22:0);Color col=a.team==0?MINT:CORAL;
            rect(g,266,yy-19,749,29,a.id==0?new Color(66,119,115,100):new Color(36,58,68,100));
            rect(g,271,yy-13,3,14,col);text(g,a.name+(a.carrier?"  ◆":"")+(a.dead?"  / CAIU":""),285,yy,12,a.dead?MUTED:WHITE,a.id==0);
            text(g,""+a.kills,721,yy,12,WHITE,true);text(g,""+a.deaths,787,yy,12,MUTED,false);text(g,""+a.aces,851,yy,12,GOLD,true);text(g,"¤ "+a.credits,908,yy,12,GOLD,false);
        }
    }
    void match(Graphics2D g){
        rect(g,0,0,1280,720,new Color(5,20,30,214));center(g,game.endTitle,640,75,45,game.flow.matchWon?MINT:CORAL,true);scoreboard(g,104);center(g,game.observing?"SIMULAÇÃO LOCAL / SEM XP OU RANK":"+"+game.profile.lastXp+" XP  /  MAESTRIA "+game.profile.level(game.player.agentIndex)+"  /  "+game.profile.rank()+(game.profile.lastRating==0?"":" ("+(game.profile.lastRating>0?"+":"")+game.profile.lastRating+")"),640,605,13,GOLD,true);
        button(g,"JOGAR NOVAMENTE",405,622,260,49,true,game.flow::play);button(g,"MENU PRINCIPAL",681,622,222,49,false,()->{game.ui="menu";game.phase=Phase.MENU;});
    }
    void button(Graphics2D g,String label,double x,double y,double w,double h,boolean primary,Runnable action){
        boolean hover=mouseX>=x&&mouseX<x+w&&mouseY>=y&&mouseY<y+h;
        Color fill=primary?(hover?new Color(0xFF8D79):CORAL):(hover?new Color(0x30555D):new Color(28,51,62,220));
        Path2D p=new Path2D.Double();p.moveTo(x,y);p.lineTo(x+w-10,y);p.lineTo(x+w,y+10);p.lineTo(x+w,y+h);p.lineTo(x,y+h);p.closePath();g.setColor(fill);g.fill(p);
        if(!primary){g.setStroke(new BasicStroke(1));g.setColor(new Color(87,124,130,160));g.draw(p);}
        center(g,label,x+w/2,y+h/2+4,Math.max(10,Math.min(13,(int)(w/Math.max(1,label.length())*1.5))),primary?INK:WHITE,true);
        buttons.add(new Button(x,y,w,h,action));
    }
    void badge(Graphics2D g,String text,int x,int y,int w,int h,Color color){g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),22));g.fillRect(x,y,w,h);g.setColor(color);g.setStroke(new BasicStroke(1));g.drawRect(x,y,w,h);center(g,text,x+w*.5,y+h*.5+4,10,color,true);}
    void icon(Graphics2D g,Ability a,double x,double y,double r,Color c){AbilityArt.icon(g,a,x,y,r,c);}

    static void logo(Graphics2D g,double x,double y,double size){polygon(g,new double[]{x,y,x+size*.43,y+size*.6,x+size*.43,y+size,x,y+size*.4},CORAL);polygon(g,new double[]{x+size,y,x+size*.57,y+size*.6,x+size*.57,y+size,x+size,y+size*.4},MINT);}
    static void shield(Graphics2D g,double x,double y,double s,Color c){Path2D p=new Path2D.Double();p.moveTo(x,y);p.lineTo(x+s,y);p.lineTo(x+s,y+s*.8);p.lineTo(x+s*.5,y+s*1.2);p.lineTo(x,y+s*.8);p.closePath();g.setColor(c);g.setStroke(new BasicStroke(2));g.draw(p);}
    static Font font(int size,boolean bold){return new Font(Font.SANS_SERIF,bold?Font.BOLD:Font.PLAIN,size);}
    static void text(Graphics2D g,String t,double x,double y,int size,Color color,boolean bold){g.setFont(font(size,bold));g.setColor(color);g.drawString(t,(float)x,(float)y);}
    static void center(Graphics2D g,String t,double x,double y,int size,Color col,boolean bold){g.setFont(font(size,bold));text(g,t,x-g.getFontMetrics().stringWidth(t)*.5,y,size,col,bold);}
    static void right(Graphics2D g,String t,double x,double y,int size,Color col,boolean bold){g.setFont(font(size,bold));text(g,t,x-g.getFontMetrics().stringWidth(t),y,size,col,bold);}
    static void tracked(Graphics2D g,String t,double x,double y,int size,float spacing,Color color){g.setFont(font(size,true));g.setColor(color);for(char c:t.toCharArray()){g.drawString(""+c,(float)x,(float)y);x+=g.getFontMetrics().charWidth(c)+spacing;}}
    static void rect(Graphics2D g,double x,double y,double w,double h,Color c){g.setColor(c);g.fill(new Rectangle2D.Double(x,y,w,h));}
    static void line(Graphics2D g,double x,double y,double xx,double yy,Color c,float stroke){g.setColor(c);g.setStroke(new BasicStroke(stroke));g.draw(new Line2D.Double(x,y,xx,yy));}
    static void polygon(Graphics2D g,double[] pts,Color c){Path2D p=new Path2D.Double();p.moveTo(pts[0],pts[1]);for(int i=2;i<pts.length;i+=2)p.lineTo(pts[i],pts[i+1]);p.closePath();g.setColor(c);g.fill(p);}
    static void wrap(Graphics2D g,String value,int x,int y,int max,int size,Color c){g.setFont(font(size,false));StringBuilder line=new StringBuilder();for(String word:value.split(" ")){String test=line.length()==0?word:line+" "+word;if(g.getFontMetrics().stringWidth(test)>max){text(g,line.toString(),x,y,size,c,false);line=new StringBuilder(word);y+=size+8;}else{line=new StringBuilder(test);}}if(line.length()>0)text(g,line.toString(),x,y,size,c,false);}
    static String clock(double time){int seconds=Math.max(0,(int)Math.ceil(time));return seconds/60+":"+String.format(Locale.ROOT,"%02d",seconds%60);}
}
