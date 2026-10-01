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
        rect(p,26,22,290,76,new Color(11,28,40,220));text(p,"RIFT / DUELO",42,46,14,MINT,true);text(p,DuelMaps.NAMES[g.world.mapIndex-3]+" • PRIMEIRO A 7",42,70,12,WHITE,true);text(p,"J"+(seat+1)+" / "+s.names[seat],42,89,11,MUTED,false);
        rect(p,447,20,386,80,new Color(11,28,40,231));center(p,s.scores[seat]+"     :     "+s.scores[1-seat],640,66,35,WHITE,true);center(p,"RODADA "+s.round+"  /  "+(int)Math.ceil(Math.max(0,s.timer))+" s",640,89,11,MINT,true);
        rect(p,891,22,362,56,new Color(11,28,40,220));
        right(p,(fps>0?""+fps:"—")+" FPS  •  "+Math.round(frameMillis)+" ms  •  REDE "+latency+" ms",1248,42,11,WHITE,true);
        right(p,room,1248,64,10,MUTED,false);
        if(s.phase==DuelSimulation.LIVE){center(p,"Contra "+s.names[1-seat],640,124,12,WHITE,true);}
        if(!equipment&&(s.phase==DuelSimulation.RESULT||s.phase==DuelSimulation.FINISHED)){
            rect(p,220,140,840,90,new Color(10,25,38,230));center(p,s.message,640,181,20,WHITE,true);
            center(p,s.phase==DuelSimulation.FINISHED?"ENTER  REVANCHE  •  ESC  LIBERAR CURSOR":"TROCA DE LADOS NA PRÓXIMA RODADA",640,211,12,MINT,true);
        }
        rect(p,26,617,295,76,new Color(11,28,40,220));text(p,""+(int)Math.ceil(a.hp),43,665,37,WHITE,true);text(p,"VIDA",45,685,9,MUTED,true);
        text(p,""+(int)Math.ceil(a.armor),167,665,29,MINT,true);text(p,"ESCUDO",167,685,9,MUTED,true);text(p,Agent.values()[a.agentIndex].name,42,638,11,MINT,true);
        rect(p,910,617,342,76,new Color(11,28,40,220));right(p,a.melee()?g.profile.blade().label:gun.kind.label,1235,639,14,MINT,true);
        right(p,a.melee()?"CORPO A CORPO":gun.ammo+" / "+gun.reserve,1235,674,32,WHITE,true);
        center(p,gun.reload>0?"RECARREGANDO":"1 PISTOLA   2 PRINCIPAL   3 LÂMINA   R RECARGA   V INSPECIONAR",640,685,10,WHITE,true);
        if(equipment){
            rect(p,338,155,604,423,new Color(10,25,39,245));line(p,338,155,942,155,MINT,3);
            center(p,s.phase==DuelSimulation.FINISHED?(s.winner==seat?"VITÓRIA / REVANCHE":"DERROTA / REVANCHE"):"SEU EQUIPAMENTO",640,194,24,WHITE,true);
            center(p,s.phase==DuelSimulation.FINISHED?s.message:"Gratuito a cada rodada • habilidades desativadas no duelo",640,222,12,MUTED,false);
            Weapon primary=Weapon.values()[choice.primary()],pistol=Weapon.values()[choice.pistol()];
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
            rect(p,150,568,980,39,new Color(9,24,37,245));center(p,status.isEmpty()?"CONTROLES PAUSADOS • ESC PARA VOLTAR • FECHE A JANELA PARA SAIR":status,640,593,12,GOLD,true);
        }
        p.dispose();frameMillis=(System.nanoTime()-start)/1e6;if(g.settings.quality==3)adaptive.sample(frameMillis,g.settings.frameLimit);
    }
}
