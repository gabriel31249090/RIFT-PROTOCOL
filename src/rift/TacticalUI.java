package rift;

import java.awt.*;
import java.awt.geom.*;
import static rift.World.*;
import static rift.Game.*;
import static rift.View.*;

/** Distinct instruments share only their world/click projection, never an invented map. */
final class TacticalUI {
    static boolean chemical(Game g){return g.agent==Agent.CAUSTICA;}
    static boolean astral(Game g){return g.agent==Agent.ESPECTRO;}
    static Shape shape(Game g){return chemical(g)?new RoundRectangle2D.Double(400,102,480,480,26,26):new Ellipse2D.Double(400,102,480,480);}
    static void draw(View v,Graphics2D p){
        Game g=v.game;boolean chem=chemical(g),star=astral(g);Color accent=new Color(chem?0xBBE56E:star?0xC5B0FF:0xF0BB78);Color shade=new Color(chem?0x173629:star?0x201C3A:0x352D27);
        rect(p,0,0,1280,720,new Color(4,11,20,star?175:115));MapProjection m=v.tacticalMap();Shape clip=p.getClip(),surface=shape(g);
        if(star){for(int i=0;i<34;i++){double x=310+(i*137)%650,y=48+(i*79)%569;double alpha=.5+.5*Math.sin(g.visualTime+i);p.setColor(new Color(194,172,255,(int)(50+alpha*100)));p.fill(new Ellipse2D.Double(x,y,2+i%3,2+i%3));}for(int i=0;i<3;i++){p.setColor(new Color(154,126,229,80));p.draw(new Ellipse2D.Double(366-i*7,68-i*7,548+i*14,548+i*14));}}
        else if(chem){rect(p,378,85,524,514,new Color(17,34,31,245));for(int i=0;i<8;i++){rect(p,382,125+i*55,10,29,new Color(chem?0xA2BD54:0x9E805A));rect(p,888,125+i*55,8,29,new Color(0x58753C));}}
        else{p.setStroke(new BasicStroke(13));p.setColor(new Color(32,43,47));p.draw(new Ellipse2D.Double(391,93,498,498));for(int i=0;i<60;i++){double a=i*Math.PI/30;line(p,640+Math.sin(a)*252,342+Math.cos(a)*252,640+Math.sin(a)*(i%5==0?265:258),342+Math.cos(a)*(i%5==0?265:258),accent,1);}}
        p.setColor(shade);p.fill(surface);p.clip(surface);v.mapTerrain(p,m,true);
        rect(p,400,102,480,480,new Color(shade.getRed(),shade.getGreen(),shade.getBlue(),100));
        if(chem){for(int i=0;i<12;i++)line(p,400,110+i*42,880,110+i*42,new Color(183,224,130,35),1);}
        else if(!star){for(int i=-6;i<=6;i++){line(p,400,342+i*40,880,342+i*40,new Color(240,184,104,28),1);line(p,640+i*40,102,640+i*40,582,new Color(240,184,104,28),1);}p.setColor(new Color(240,185,114,28));p.fill(new Arc2D.Double(400,102,480,480,-g.visualTime*28,16,Arc2D.PIE));}
        double x=m.x(g.player.x,g.player.z),y=m.y(g.player.x,g.player.z),reach=g.tacticalRange()*m.scale;p.setColor(accent);p.setStroke(new BasicStroke(1));p.draw(new Ellipse2D.Double(x-reach,y-reach,reach*2,reach*2));
        for(Smoke smoke:g.smokes){double r=smoke.radius()*m.scale;p.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),70));p.fill(new Ellipse2D.Double(m.x(smoke.x,smoke.z)-r,m.y(smoke.x,smoke.z)-r,r*2,r*2));}
        for(Actor a:g.actors)if(!a.dead&&(a.team==g.player.team||a.revealed>0))v.mapArrow(p,m,a.x,a.z,a.yaw,a==g.player?10:6,a==g.player?WHITE:a.team==g.player.team?MINT:CORAL);
        int count=0;for(V target:g.tacticalTargets){double tx=m.x(target.x(),target.z()),ty=m.y(target.x(),target.z()),r=g.tacticalRadius()*m.scale;p.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),65));p.fill(new Ellipse2D.Double(tx-r,ty-r,r*2,r*2));p.setColor(accent);p.setStroke(new BasicStroke(2));p.draw(new Ellipse2D.Double(tx-r,ty-r,r*2,r*2));center(p,""+(++count),tx,ty+5,15,WHITE,true);if(star){line(p,tx-r,ty,tx+r,ty,accent,1);line(p,tx,ty-r,tx,ty+r,accent,1);}}
        if(surface.contains(v.mouseX,v.mouseY)){V target=m.world(v.mouseX,v.mouseY);double r=g.tacticalRadius()*m.scale;p.setColor(g.validTarget(target.x(),target.z())?accent:CORAL);p.draw(new Ellipse2D.Double(v.mouseX-r,v.mouseY-r,r*2,r*2));}
        p.setClip(clip);p.setColor(accent);p.setStroke(new BasicStroke(2));p.draw(surface);
        center(p,chem?"NORTE FIXO / DISPERSÃO QUÍMICA":star?"NORTE FIXO / VISÃO ASTRAL":"FRENTE DA CÂMERA / TERMINAL TÁTICO",640,84,11,accent,true);
        rect(p,40,235,283,311,new Color(7,19,29,225));Assets.portrait(p,g.agent.ordinal(),61,255,74,74);text(p,g.agent.name,148,286,20,WHITE,true);text(p,g.agent.role,149,308,10,accent,true);
        text(p,chem?"CONTENÇÃO":star?"ENTRE PLANOS":"CONTROLE DE ÁREA",61,367,15,accent,true);
        wrap(p,chem?"Uma nuvem. Escolha a entrada que quer negar e acompanhe a área de dispersão.":star?"Projete uma estrela sobre o terreno. As passagens e alturas continuam reais.":"Marque múltiplos pontos e envie a névoa em uma única confirmação.",61,400,240,13,MUTED);
        text(p,"POSICIONAMENTO / "+Game.abilityName(g.tacticalAbility),61,521,9,accent,true);
        rect(p,935,236,306,284,new Color(7,19,29,235));Assets.icon(p,g.tacticalAbility,973,280,20);text(p,abilityName(g.tacticalAbility),1006,285,18,WHITE,true);text(p,World.NAMES[g.world.mapIndex],956,322,11,accent,true);
        text(p,g.tacticalTargets.size()+" / "+g.tacticalCapacity()+" MARCAÇÕES",956,363,17,WHITE,true);text(p,"ALCANCE  "+(int)g.tacticalRange()+" m",956,393,13,accent,true);text(p,"Clique: marcar / remover",956,440,12,MUTED,false);text(p,"Direito: desfazer",956,466,12,MUTED,false);text(p,"A partida continua em tempo real.",956,497,10,MUTED,false);
        v.buttons.add(new View.Button(400,102,480,480,()->{if(surface.contains(v.mouseX,v.mouseY)){V at=m.world(v.mouseX,v.mouseY);g.markTarget(at.x(),at.z());}}));
        v.button(p,chem?"LIBERAR NUVEM  [ ENTER ]":star?"MANIFESTAR  [ ENTER ]":"CONFIRMAR  [ ENTER ]",461,616,358,44,!g.tacticalTargets.isEmpty(),g::deployTactical);
        v.button(p,"CANCELAR  [ ESC ]",956,546,266,39,false,g::cancelTactical);
    }
}
