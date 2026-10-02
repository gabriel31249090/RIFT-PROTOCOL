package rift;

import java.awt.*;
import static rift.View.*;

final class SettingsUI {
    final View v;final Game g;int tab;SettingsUI(View v){this.v=v;g=v.game;}
    void draw(Graphics2D p){
        Settings s=g.settings;rect(p,0,0,1280,720,new Color(0x0E2230));text(p,"AJUSTE AO SEU RITMO",44,64,31,WHITE,true);text(p,"F10 abre durante a partida • Esc volta • Preferências salvas neste computador",45,95,12,MUTED,false);
        String[] tabs={"CONTROLES","GRÁFICOS","ÁUDIO","GAMEPLAY","ACESSIBILIDADE"};
        for(int i=0;i<tabs.length;i++){final int index=i;v.button(p,tabs[i],42,140+i*60,210,46,tab==i,()->{tab=index;g.bindCapture=null;});}
        if(tab==0){
            adjust(p,0,"Sensibilidade",String.format(java.util.Locale.ROOT,"%.2f",s.sensitivity/.0021),()->s.sensitivity=Settings.clamp(s.sensitivity-.0002,.0003,.008),()->s.sensitivity=Settings.clamp(s.sensitivity+.0002,.0003,.008));
            adjust(p,1,"Multiplicador ADS",String.format(java.util.Locale.ROOT,"%.2f",s.adsSensitivity),()->s.adsSensitivity=Settings.clamp(s.adsSensitivity-.05,.1,1.5),()->s.adsSensitivity=Settings.clamp(s.adsSensitivity+.05,.1,1.5));
            int i=0;for(Settings.Action a:Settings.Action.values()){int x=292+(i%3)*308,y=255+(i/3)*37;v.button(p,a.label+"  ["+(g.bindCapture==a?"…":s.key(a))+"]",x,y,295,31,g.bindCapture==a,()->g.bindCapture=a);i++;}
            text(p,g.bindCapture==null?"Conflitos trocam as duas teclas. Mouse: esquerdo atira; direito mira.":"Pressione a nova tecla para "+g.bindCapture.label+". Esc cancela.",295,625,12,g.bindCapture==null?MUTED:GOLD,false);
        }else if(tab==1){
            cycle(p,0,"Qualidade 3D",new String[]{"BAIXA","MÉDIA","ALTA","AUTOMÁTICA"}[s.quality],()->s.quality=(s.quality+1)%4);
            cycle(p,1,"Sombras",s.shadows?"ATIVADAS":"DESATIVADAS",()->{s.shadows=!s.shadows;g.world.setShadows(s.shadows);});
            cycle(p,2,"Distância de renderização",s.renderDistance+" m",()->s.renderDistance=s.renderDistance==60?100:s.renderDistance==100?160:s.renderDistance==160?210:60);
            cycle(p,3,"Limite de quadros",s.frameLimit+" FPS",()->s.frameLimit=s.frameLimit==60?120:s.frameLimit==120?144:s.frameLimit==144?240:60);
            cycle(p,4,"Dificuldade dos bots",new String[]{"TRANQUILO","NORMAL","DIFÍCIL"}[s.difficulty],()->s.difficulty=(s.difficulty+1)%3);
            cycle(p,5,"Texturas de materiais",on(s.textures),()->s.textures=!s.textures);
            cycle(p,6,"Marcas e cápsulas de disparos",on(s.impactFX),()->s.impactFX=!s.impactFX);
            cycle(p,7,"Ampliação do cenário",s.smoothUpscale?"SUAVE / MAIS CUSTOSA":"RÁPIDA / MAIS NÍTIDA",()->s.smoothUpscale=!s.smoothUpscale);
            v.button(p,"MODO DESEMPENHO",42,572,210,43,false,()->{s.performance();v.adaptive.reset(1);g.world.setShadows(false);s.save();});
            text(p,"AUTO reage ao tempo do quadro completo. F11: tela cheia.",295,629,11,MUTED,false);
        }else if(tab==2){
            adjust(p,0,"Volume master",percent(s.volume),()->s.volume=Math.max(0,s.volume-.1),()->s.volume=Math.min(1,s.volume+.1));
            adjust(p,1,"Efeitos",percent(s.effectsVolume),()->s.effectsVolume=Math.max(0,s.effectsVolume-.1),()->s.effectsVolume=Math.min(1,s.effectsVolume+.1));
            adjust(p,2,"Música do menu",percent(s.musicVolume),()->s.musicVolume=Math.max(0,s.musicVolume-.1),()->s.musicVolume=Math.min(1,s.musicVolume+.1));
            cycle(p,3,"Áudio",s.sound?"ATIVADO":"DESATIVADO",()->s.sound=!s.sound);
            v.button(p,"TESTAR DISPARO",295,425,310,43,false,()->g.audio.play(Game.Weapon.ECHO.sound()));
            text(p,"Efeitos estéreo. A trilha ambiente toca somente nos menus.",295,506,14,MUTED,false);
        }else if(tab==3){
            adjust(p,0,"Campo de visão horizontal",s.fov+"°",()->s.fov=Math.max(70,s.fov-5),()->s.fov=Math.min(110,s.fov+5));
            adjust(p,1,"Mira: comprimento",s.crossSize+" px",()->s.crossSize=Math.max(2,s.crossSize-1),()->s.crossSize=Math.min(16,s.crossSize+1));
            adjust(p,2,"Mira: abertura",s.crossGap+" px",()->s.crossGap=Math.max(0,s.crossGap-1),()->s.crossGap=Math.min(16,s.crossGap+1));
            cycle(p,3,"Mira: espessura",s.crossWidth+" px",()->s.crossWidth=s.crossWidth%4+1);
            cycle(p,4,"Cor da mira",new String[]{"MENTA","CIANO","AMARELO","MAGENTA"}[s.crossColor],()->s.crossColor=(s.crossColor+1)%4);
            cycle(p,5,"Mira dinâmica / ponto",(s.dynamicCrosshair?"DINÂMICA":"FIXA")+" / "+(s.crossDot?"PONTO":"SEM PONTO"),()->{if(s.crossDot)s.crossDot=false;else{s.crossDot=true;s.dynamicCrosshair=!s.dynamicCrosshair;}});
            cycle(p,6,"Mapa de habilidade / câmera",s.abilityHold?"SEGURAR A TECLA":"ALTERNAR",()->s.abilityHold=!s.abilityHold);
            cross(p,s,1120,592,0);
        }else{
            cycle(p,0,"Movimento de câmera reduzido",on(s.lowMotion),()->s.lowMotion=!s.lowMotion);
            cycle(p,1,"Clarão escuro (mesma cegueira)",on(s.softFlash),()->s.softFlash=!s.softFlash);
            cycle(p,2,"Legendas de efeitos",on(s.captions),()->s.captions=!s.captions);
            cycle(p,3,"HUD com alto contraste",on(s.highContrast),()->s.highContrast=!s.highContrast);
            cycle(p,4,"Mira das outras armas",s.aimToggle?"ALTERNAR":"SEGURAR",()->s.aimToggle=!s.aimToggle);
            cycle(p,5,"Agachar / andar",(s.crouchToggle?"ALTERNAR":"SEGURAR")+" / "+(s.walkToggle?"ALTERNAR":"SEGURAR"),()->{if(s.walkToggle)s.walkToggle=false;else{s.walkToggle=true;s.crouchToggle=!s.crouchToggle;}});
            cycle(p,6,"Inverter eixo vertical",on(s.invertY),()->s.invertY=!s.invertY);
            cycle(p,7,"Mira da sniper",s.sniperToggle?"ALTERNAR POR CLIQUE":"SEGURAR",()->s.sniperToggle=!s.sniperToggle);
        }
        v.button(p,"RESTAURAR TECLAS",42,648,210,43,false,()->{s.resetBinds();g.bindCapture=null;});
        v.button(p,"SALVAR E VOLTAR",935,648,296,43,true,()->{s.save();g.bindCapture=null;g.ui=g.backUi;});
    }
    String percent(double value){return Math.round(value*100)+"%";}String on(boolean b){return b?"ATIVADO":"DESATIVADO";}
    void adjust(Graphics2D p,int row,String label,String value,Runnable minus,Runnable plus){int y=140+row*60;text(p,label,296,y+27,14,WHITE,true);v.button(p,"−",832,y,48,39,false,minus);center(p,value,1005,y+26,15,MINT,true);v.button(p,"+",1165,y,48,39,false,plus);line(p,295,y+47,1220,y+47,new Color(0x294452),1);}
    void cycle(Graphics2D p,int row,String label,String value,Runnable action){int y=140+row*60;text(p,label,296,y+27,14,WHITE,true);v.button(p,value,810,y,405,39,false,action);line(p,295,y+47,1220,y+47,new Color(0x294452),1);}
    static void cross(Graphics2D p,Settings s,int x,int y,int error){int gap=s.crossGap+(s.dynamicCrosshair?error:0),size=s.crossSize;Color c=new Color(new int[]{0xB3FFE0,0x79ECFF,0xFFF56E,0xF79BEF}[s.crossColor]);p.setStroke(new BasicStroke(s.crossWidth+2));p.setColor(new Color(4,16,23,210));for(int pass=0;pass<2;pass++){p.drawLine(x-gap-size,y,x-gap,y);p.drawLine(x+gap,y,x+gap+size,y);p.drawLine(x,y-gap-size,x,y-gap);p.drawLine(x,y+gap,x,y+gap+size);p.setColor(c);p.setStroke(new BasicStroke(s.crossWidth));}if(s.crossDot)p.fillRect(x-1,y-1,3,3);}
}
