package rift;

import java.awt.*;
import java.awt.geom.*;
import static rift.View.*;
import static rift.Game.*;
import static rift.World.*;

/** Lobby, local queue, agent stage and progression screens. */
final class FlowUI {
    final View v;final Game g;final Renderer stage=new Renderer(430,475);
    FlowUI(View view){v=view;g=view.game;}
    void backdrop(Graphics2D p,String title,String step){
        p.drawImage(Assets.HERO,0,0,1280,720,null);rect(p,0,0,1280,720,new Color(18,24,26,242));
        logo(p,38,32,25);text(p,title,82,56,23,WHITE,true);right(p,"LOCAL  /  VOCÊ + BOTS",1238,47,11,MINT,true);text(p,step,40,93,11,MUTED,false);
        line(p,40,110,1238,110,UiTheme.BORDER,1);
    }
    void modes(Graphics2D p){
        backdrop(p,"JOGAR","01  MODO E MAPA     /     02  ACEITAR     /     03  AGENTE     /     04  PARTIDA");
        for(MatchFlow.Mode mode:MatchFlow.Mode.values()){int i=mode.ordinal(),x=40+(i%3)*403,y=128+(i/3)*144;boolean selected=g.flow.mode==mode;
            rect(p,x,y,386,126,selected?UiTheme.RAISED:UiTheme.SURFACE);line(p,x,y,x+386,y,selected?MINT:UiTheme.BORDER,selected?2:1);
            text(p,String.format("0%d",i+1),x+19,y+32,14,selected?MINT:MUTED,true);text(p,mode.label,x+20,y+70,23,WHITE,true);text(p,mode.description,x+20,y+102,11,MUTED,false);
            v.buttons.add(new View.Button(x,y,386,126,()->{g.flow.mode=mode;g.audio.play("select");}));
        }
        text(p,"ESCOLHA O MAPA",40,454,12,MINT,true);
        for(int i=0;i<3;i++){int x=40+i*403;final int index=i;boolean selected=g.flow.mapIndex==i;
            rect(p,x,473,386,117,selected?UiTheme.RAISED:UiTheme.SURFACE);Color color=new Color(new int[]{0xB5AE91,0x9BB9D1,0xC49C75}[i]);
            for(int k=0;k<6;k++){int bx=x+216+k*24,by=497+((k+i)%3)*11;polygon(p,new double[]{bx,562,bx,by,bx+15,by-8,bx+28,by,bx+28,562},new Color(color.getRed(),color.getGreen(),color.getBlue(),65));}
            text(p,World.NAMES[i],x+16,506,20,WHITE,true);wrap(p,World.DETAILS[i],x+17,532,208,11,MUTED);if(selected)line(p,x,590,x+386,590,color,3);v.buttons.add(new View.Button(x,473,386,117,()->g.flow.mapIndex=index));
        }
        text(p,"Classificação e torneio são salvos neste computador. A fila prepara uma partida com bots.",40,624,12,MUTED,false);
        v.buttonIcon(p,"arrow-left","VOLTAR",40,653,150,43,false,()->g.ui="menu");v.buttonIcon(p,"play","CONFIRMAR E ENTRAR NA FILA",855,643,383,51,true,g.flow::queue);
    }
    void queue(Graphics2D p){
        boolean found=g.ui.equals("found");backdrop(p,found?"PARTIDA ENCONTRADA":"PREPARANDO PARTIDA LOCAL","02  /  ACEITE");
        p.setColor(new Color(164,214,191,35));p.setStroke(new BasicStroke(2));p.drawOval(523,168,234,234);p.drawOval(545,190,190,190);
        p.setColor(found?MINT:CORAL);p.setStroke(new BasicStroke(5));p.draw(new Arc2D.Double(523,168,234,234,-g.visualTime*100,found?360:110,Arc2D.OPEN));
        center(p,found?"10 / 10":"•••",640,296,48,WHITE,true);center(p,"VOCÊ + 9 BOTS",640,333,12,MUTED,true);
        center(p,g.flow.mode.label,640,460,32,WHITE,true);center(p,World.NAMES[g.flow.mapIndex]+"  /  "+(found?"ACEITE EM "+Math.max(0,15-(int)g.flow.elapsed)+" s":"MONTANDO EQUIPES"),640,492,13,MUTED,true);
        if(found)v.button(p,"ACEITAR  [ ENTER ]",435,548,410,59,true,g.flow::accept);
        v.button(p,"CANCELAR",535,638,210,38,false,()->g.ui="modes");
    }
    void agents(Graphics2D p){
        backdrop(p,"SELEÇÃO DE AGENTES",g.pendingTraining?"TREINO  /  TODOS OS AGENTES LIVRES":g.flow.accepted?"03  /  ESCOLHA E TRAVE SEU AGENTE":"CONTRATOS  /  300 XP POR AGENTE  /  1.800 XP INICIAIS");
        text(p,"ELENCO  /  11 AGENTES",40,137,12,MINT,true);
        for(int i=0;i<Agent.values().length;i++){final int index=i;Agent a=Agent.values()[i];int x=40+(i%3)*117,y=157+(i/3)*109;boolean selected=g.settings.agent==i,unlocked=g.pendingTraining||g.profile.unlocked(i);
            rect(p,x,y,108,100,selected?UiTheme.RAISED:UiTheme.SURFACE);Shape clip=p.getClip();p.clipRect(x,y,108,65);Assets.portrait(p,i,x,y-15,108,108);p.setClip(clip);
            line(p,x,y,x+108,y,selected?new Color(a.color):UiTheme.BORDER,selected?2:1);text(p,a.name,x+8,y+81,11,WHITE,true);text(p,unlocked?String.format("%02d",i+1):"300 XP",x+8,y+95,8,unlocked?MUTED:GOLD,false);v.buttons.add(new View.Button(x,y,108,100,()->g.selectAgent(index)));
        }
        text(p,"SALDO  "+g.profile.wallet+" XP",41,611,13,GOLD,true);
        Agent a=Agent.values()[g.settings.agent];Assets.portrait(p,a.ordinal(),421,169,430,430);
        rect(p,421,125,430,76,UiTheme.OVERLAY);
        text(p,a.name,437,163,30,new Color(a.color),true);text(p,a.role,439,185,11,MINT,true);text(p,"MAESTRIA "+g.profile.level(a.ordinal()),439,581,11,MUTED,true);
        text(p,"KIT DO AGENTE",897,137,12,MINT,true);int[] slots={0,3,1,2};String[] keys={"Q","E","C","X"};
        for(int i=0;i<4;i++){Ability ability=a.slot(slots[i]);int y=164+i*108;rect(p,889,y,349,97,UiTheme.SURFACE);v.icon(p,ability,915,y+23,11,new Color(a.color));text(p,keys[i]+"  "+abilityName(ability),938,y+28,13,WHITE,true);wrap(p,abilityDescription(ability),902,y+50,318,11,MUTED);}
        if(g.flow.accepted){for(int i=0;i<10;i++){int x=40+i*83;boolean ready=i==0?g.flow.locked:i<=g.flow.botLocks;rect(p,x,650,74,37,new Color(ready?0x365F56:0x213B4A));center(p,i==0?"VOCÊ":"BOT "+i,x+37,665,9,WHITE,true);center(p,ready?"TRAVADO":"ESCOLHENDO",x+37,680,8,ready?MINT:MUTED,false);}}
        else v.button(p,"VOLTAR",40,650,149,41,false,()->{g.pendingStart=false;g.flow.accepted=false;g.flow.locked=false;g.ui=g.agentReturn;});
        boolean unlocked=g.pendingTraining||g.profile.unlocked(a.ordinal());
        String label=g.flow.locked?"AGUARDANDO BOTS…":!unlocked?"DESBLOQUEAR / 300 XP":g.pendingStart?(g.pendingTraining?"ENTRAR NO TREINO":"TRAVAR AGENTE"):"CONFIRMAR AGENTE";
        v.button(p,label,897,644,341,52,!g.flow.locked,()->{if(g.flow.locked)return;if(!unlocked){if(g.profile.unlock(a.ordinal()))g.audio.play("select");else g.tell("Saldo insuficiente",2);return;}g.confirmAgent();});
        if(g.noticeTime>0)center(p,g.notice,643,626,11,GOLD,true);
    }
    void loading(Graphics2D p){backdrop(p,"CARREGANDO MAPA","04  /  PARTIDA LOCAL");center(p,World.NAMES[g.flow.mapIndex],640,286,72,WHITE,true);center(p,World.DETAILS[g.flow.mapIndex],640,333,16,MUTED,false);center(p,g.flow.mode.label+"  •  "+g.agent.name,640,408,21,MINT,true);rect(p,375,465,530,4,new Color(0x2C4858));rect(p,375,465,530*Math.min(1,g.flow.elapsed/1.6),4,MINT);center(p,"DICA  /  Use E para a quarta habilidade e Z para marcar uma posição.",640,570,13,MUTED,false);}
    void tournament(Graphics2D p){backdrop(p,"PREMIER / COPA LOCAL","SEMIFINAL CONCLUÍDA");center(p,"VOCÊ ESTÁ NA FINAL",640,246,49,MINT,true);center(p,"Seu esquadrão venceu a semifinal. A final acontece no próximo mapa.",640,294,15,WHITE,false);for(int i=0;i<4;i++){int x=170+i*250;rect(p,x,363,215,104,new Color(i==0||i==3?0x35584F:0x213644));center(p,new String[]{"SEU ESQUADRÃO","EQUIPE ÔNIX","EQUIPE FLINT","EQUIPE NIMBUS"}[i],x+107,404,13,WHITE,true);center(p,i==0||i==3?"FINALISTA":"ELIMINADA",x+107,433,10,i==0||i==3?MINT:MUTED,true);}v.button(p,"JOGAR A FINAL",447,549,386,57,true,g.flow::nextTournament);v.button(p,"ENCERRAR COPA",520,640,240,38,false,()->g.ui="menu");}
    void profile(Graphics2D p){backdrop(p,"CARREIRA LOCAL","PROGRESSÃO SALVA NESTE COMPUTADOR");Profile r=g.profile;text(p,r.rank(),48,196,57,MINT,true);text(p,r.rating+" PONTOS  /  COMPETITIVO LOCAL",51,230,13,MUTED,true);String[] stats={r.matches+" PARTIDAS",r.wins+" VITÓRIAS",r.kills+" ABATES",r.aces+" ACES"};for(int i=0;i<4;i++){rect(p,48+i*300,272,284,65,new Color(0x234252));text(p,stats[i],66+i*300,314,20,WHITE,true);}text(p,"MAESTRIA DOS AGENTES",48,382,14,GOLD,true);for(int i=0;i<Agent.values().length;i++){int x=48+i%3*305,y=407+i/3*51;text(p,Agent.values()[i].name+" / NÍVEL "+r.level(i),x,y,13,WHITE,true);rect(p,x,y+14,254,4,new Color(0x304A59));rect(p,x,y+14,254*(r.mastery[i]%800)/800.,4,new Color(Agent.values()[i].color));}text(p,"HISTÓRICO DE RANKS",986,382,12,MINT,true);int y=412;for(String h:r.history.subList(Math.max(0,r.history.size()-7),r.history.size())){text(p,h,986,y,11,MUTED,false);y+=27;}text(p,"DESTAQUES",48,631,11,GOLD,true);text(p,(r.kills>0?"✓ PRIMEIRO ABATE   ":"")+ (r.wins>0?"✓ PRIMEIRA VITÓRIA   ":"")+(r.kills>=100?"✓ CENTURIÃO   ":"")+(r.aces>0?"✓ ACE":""),174,631,12,WHITE,true);v.button(p,"VOLTAR",1001,649,237,42,true,()->g.ui="menu");if(!r.saveError.isEmpty())text(p,r.saveError,48,684,12,CORAL,false);}
}
