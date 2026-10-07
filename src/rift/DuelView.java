package rift;

import java.awt.*;
import java.awt.geom.*;
import static rift.View.*;
import static rift.Game.*;

/** A duel HUD without spike, bot roster, economy or ability indicators. */
final class DuelView {
    final Game g;final Renderer renderer=new Renderer(640,360);final AdaptiveQuality adaptive=new AdaptiveQuality();
    double frameMillis;int fps;long latency;
    DuelView(Game g){this.g=g;adaptive.reset(2);}
    void render(Graphics2D output,int w,int h,DuelProtocol.State s,int seat,DuelProtocol.Choice choice,boolean equipment,boolean paused,String room,String status){
        long start=System.nanoTime();
        int width=g.settings.quality==3?adaptive.width():View.FIXED_WIDTHS[g.settings.quality];renderer.resize(width,width*9/16);
        output.setColor(Color.BLACK);output.fillRect(0,0,w,h);double scale=Math.min(w/1280.,h/720.);
        Graphics2D p=(Graphics2D)output.create();p.translate((w-1280*scale)/2,(h-720*scale)/2);p.scale(scale,scale);
        p.setRenderingHint(RenderingHints.KEY_INTERPOLATION,g.settings.smoothUpscale?RenderingHints.VALUE_INTERPOLATION_BILINEAR:RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        p.drawImage(renderer.render(g),0,0,1280,720,null);p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);p.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Actor a=g.player;Gun gun=a.gun();
        if(g.aimLerp>.90&&gun.kind.scoped()&&!a.melee()){
            Area mask=new Area(new Rectangle2D.Double(0,0,1280,720));mask.subtract(new Area(new Ellipse2D.Double(344,64,592,592)));p.setColor(new Color(2,10,15,245));p.fill(mask);
            line(p,344,360,936,360,INK,1);line(p,640,64,640,656,INK,1);rect(p,638,358,4,4,CORAL);
        }else if(!a.dead&&!equipment)SettingsUI.cross(p,g.settings,640,360,a.melee()?0:(int)Math.min(30,g.playerSpread()*420));
        if(a.damageGlow>0){p.setColor(new Color(255,66,66,(int)Math.min(55,a.damageGlow*300)));p.fillRect(0,0,1280,720);}
        if(g.hitMarker>0){Color c=g.hitHead>0?CORAL:WHITE;for(int x:new int[]{-1,1})for(int y:new int[]{-1,1})line(p,640+x*9,360+y*9,640+x*17,360+y*17,c,2);}
        UiTheme.panel(p,26,22,290,76,MINT);text(p,"RIFT / DUELO",42,46,14,MINT,true);text(p,DuelMaps.NAMES[g.world.mapIndex-3]+" • PRIMEIRO A 7",42,70,12,WHITE,true);String name="J"+(seat+1)+" / "+s.names[seat];text(p,name,42,89,UiTheme.fit(p,name,11,258,false),MUTED,false);
        UiTheme.panel(p,447,20,386,80,null);center(p,""+s.scores[seat],496,65,34,MINT,true);center(p,""+s.scores[1-seat],784,65,34,CORAL,true);center(p,clock(s.timer),640,65,26,WHITE,true);center(p,"RODADA "+s.round,640,89,10,MUTED,true);line(p,539,36,539,73,UiTheme.BORDER,1);line(p,740,36,740,73,UiTheme.BORDER,1);
        UiTheme.panel(p,891,22,362,56,null);
        right(p,(fps>0?""+fps:"—")+" FPS  •  "+Math.round(frameMillis)+" ms  •  REDE "+latency+" ms",1248,42,11,WHITE,true);
        right(p,room,1248,64,UiTheme.fit(p,room,10,342,false),MUTED,false);
        if(s.phase==DuelSimulation.LIVE){String opponent="Contra "+s.names[1-seat];center(p,opponent,640,124,UiTheme.fit(p,opponent,12,386,true),WHITE,true);}
        if(!equipment&&(s.phase==DuelSimulation.RESULT||s.phase==DuelSimulation.FINISHED)){
            UiTheme.panel(p,220,140,840,90,s.winner==seat?MINT:CORAL);center(p,s.message,640,181,UiTheme.fit(p,s.message,20,800,true),WHITE,true);
            center(p,s.phase==DuelSimulation.FINISHED?"ENTER  REVANCHE  •  ESC  LIBERAR CURSOR":"TROCA DE LADOS NA PRÓXIMA RODADA",640,211,12,MINT,true);
        }
        UiTheme.panel(p,26,617,295,87,null);text(p,""+(int)Math.ceil(a.hp),43,665,37,a.hp<=25?CORAL:WHITE,true);text(p,"VIDA",45,685,9,MUTED,true);UiTheme.bar(p,45,695,71,3,a.hp/100,a.hp<=25?CORAL:MINT);line(p,139,641,139,685,UiTheme.BORDER,1);
        text(p,""+(int)Math.ceil(a.armor),167,665,29,MINT,true);text(p,"ESCUDO",167,685,9,MUTED,true);UiTheme.bar(p,167,695,70,3,a.armor/50,MINT);text(p,Agent.values()[a.agentIndex].name,42,638,11,MINT,true);
        UiTheme.panel(p,910,617,342,87,null);right(p,a.melee()?g.profile.blade().label:gun.kind.label,1235,639,14,MINT,true);
        right(p,a.melee()?"CORPO A CORPO":gun.ammo+" / "+gun.reserve,1235,674,32,WHITE,true);
        if(gun.reload>0){UiTheme.bar(p,1034,689,200,3,1-gun.reload/gun.reloadTotal,MINT);center(p,"RECARREGANDO",640,685,10,MINT,true);}
        if(g.settings.captions&&!g.audio.caption.isEmpty()&&System.nanoTime()-g.audio.captionAt<1_300_000_000L){rect(p,492,554,296,25,UiTheme.OVERLAY);center(p,g.audio.caption,640,572,11,WHITE,true);}
        if(equipment){
            UiTheme.panel(p,338,155,604,423,MINT);
            center(p,s.phase==DuelSimulation.FINISHED?(s.winner==seat?"VITÓRIA / REVANCHE":"DERROTA / REVANCHE"):"SEU EQUIPAMENTO",640,194,24,WHITE,true);
            center(p,s.phase==DuelSimulation.FINISHED?s.message:"Gratuito a cada rodada • habilidades desativadas no duelo",640,222,12,MUTED,false);
            Weapon primary=Weapon.values()[choice.primary()],pistol=Weapon.values()[choice.pistol()];
            for(int row=0;row<6;row++)line(p,370,277+row*38,910,277+row*38,UiTheme.BORDER,1);
            text(p,"F5    PRINCIPAL",370,266,12,MUTED,true);right(p,primary.label,910,266,20,WHITE,true);
            text(p,"F6    PISTOLA",370,304,12,MUTED,true);right(p,pistol.label,910,304,20,WHITE,true);
            text(p,"F7    AGENTE",370,342,12,MUTED,true);right(p,Agent.values()[choice.agent()].name,910,342,17,WHITE,true);
            text(p,"F8    ACABAMENTO",370,380,12,MUTED,true);right(p,Cosmetics.Skin.values()[choice.skin()].label,910,380,17,MINT,true);
            text(p,"HOME  PINGENTE",370,418,12,MUTED,true);right(p,Cosmetics.Charm.values()[choice.charm()].label,910,418,17,MINT,true);
            text(p,"END   LÂMINA",370,456,12,MUTED,true);right(p,Cosmetics.Melee.values()[choice.blade()].label,910,456,17,MINT,true);
            center(p,s.phase==DuelSimulation.PREP?"COMEÇA EM "+(int)Math.ceil(s.timer)+" s   •   B FECHA O PAINEL":s.ready[seat]?"PRONTO • AGUARDANDO O ADVERSÁRIO":"ENTER  CONFIRMAR E FICAR PRONTO",640,507,15,WHITE,true);
            center(p,s.connected[1-seat]?s.names[1-seat]+(s.ready[1-seat]?" está pronto":" está escolhendo"):"Envie endereço, porta e código para seu amigo",640,543,12,MUTED,false);
        }
        if(paused||!status.isEmpty()){
            rect(p,150,568,980,39,UiTheme.OVERLAY);String notice=status.isEmpty()?"CONTROLES PAUSADOS • ESC PARA VOLTAR • FECHE A JANELA PARA SAIR":status;center(p,notice,640,593,UiTheme.fit(p,notice,12,952,true),GOLD,true);
        }
        p.dispose();frameMillis=(System.nanoTime()-start)/1e6;if(g.settings.quality==3)adaptive.sample(frameMillis,g.settings.frameLimit);
    }
}
