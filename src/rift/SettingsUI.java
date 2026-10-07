package rift;

import java.awt.*;
import static rift.View.*;

final class SettingsUI {
    final View v;final Game g;int tab;SettingsUI(View v){this.v=v;g=v.game;}
    void draw(Graphics2D p){
        Settings s=g.settings;rect(p,0,0,1280,720,UiTheme.BACKGROUND);logo(p,43,31,23);text(p,"CONFIGURAÇÕES",82,54,26,WHITE,true);line(p,42,105,1231,105,UiTheme.BORDER,1);
        String[] tabs={"CONTROLES","GRÁFICOS","ÁUDIO","GAMEPLAY","ACESSIBILIDADE"};
        for(int i=0;i<tabs.length;i++){final int index=i;v.tab(p,tabs[i],42,140+i*60,210,46,tab==i,()->{tab=index;g.bindCapture=null;});}
        if(tab==0){
            adjust(p,0,"Sensibilidade",String.format(java.util.Locale.ROOT,"%.2f",s.sensitivity/.0021),()->s.sensitivity=Settings.clamp(s.sensitivity-.0002,.0003,.008),()->s.sensitivity=Settings.clamp(s.sensitivity+.0002,.0003,.008));
            adjust(p,1,"Multiplicador ADS",String.format(java.util.Locale.ROOT,"%.2f",s.adsSensitivity),()->s.adsSensitivity=Settings.clamp(s.adsSensitivity-.05,.1,1.5),()->s.adsSensitivity=Settings.clamp(s.adsSensitivity+.05,.1,1.5));
            int i=0;for(Settings.Action a:Settings.Action.values()){int x=292+(i%3)*308,y=255+(i/3)*37;v.button(p,a.label+"  ["+(g.bindCapture==a?"…":s.key(a))+"]",x,y,295,31,g.bindCapture==a,()->g.bindCapture=a);i++;}
            text(p,g.bindCapture==null?"Conflitos trocam as duas teclas. Mouse: esquerdo atira; direito mira.":"Pressione a nova tecla para "+g.bindCapture.label+". Esc cancela.",295,625,12,g.bindCapture==null?MUTED:GOLD,false);
        }else if(tab==1){
            segments(p,0,"Qualidade 3D",new String[]{"BAIXA","MÉDIA","ALTA","AUTO"},s.quality,i->s.quality=i);
            toggle(p,1,"Sombras",s.shadows,()->{s.shadows=!s.shadows;g.world.setShadows(s.shadows);});
            segments(p,2,"Distância de renderização",new String[]{"60 m","100 m","160 m","210 m"},index(new int[]{60,100,160,210},s.renderDistance),i->s.renderDistance=new int[]{60,100,160,210}[i]);
            segments(p,3,"Limite de quadros",new String[]{"60 FPS","120 FPS","144 FPS","240 FPS"},index(new int[]{60,120,144,240},s.frameLimit),i->s.frameLimit=new int[]{60,120,144,240}[i]);
            segments(p,4,"Dificuldade dos bots",new String[]{"TRANQUILO","NORMAL","DIFÍCIL"},s.difficulty,i->s.difficulty=i);
            toggle(p,5,"Texturas de materiais",s.textures,()->s.textures=!s.textures);
            toggle(p,6,"Marcas e cápsulas de disparos",s.impactFX,()->s.impactFX=!s.impactFX);
            segments(p,7,"Ampliação do cenário",new String[]{"NÍTIDA","SUAVE"},s.smoothUpscale?1:0,i->s.smoothUpscale=i==1);
            v.button(p,"MODO DESEMPENHO",42,572,210,43,false,()->{s.performance();v.adaptive.reset(1);g.world.setShadows(false);s.save();});
        }else if(tab==2){
            adjust(p,0,"Volume master",percent(s.volume),()->s.volume=Math.max(0,s.volume-.1),()->s.volume=Math.min(1,s.volume+.1));
            adjust(p,1,"Efeitos",percent(s.effectsVolume),()->s.effectsVolume=Math.max(0,s.effectsVolume-.1),()->s.effectsVolume=Math.min(1,s.effectsVolume+.1));
            adjust(p,2,"Música do menu",percent(s.musicVolume),()->s.musicVolume=Math.max(0,s.musicVolume-.1),()->s.musicVolume=Math.min(1,s.musicVolume+.1));
            toggle(p,3,"Áudio",s.sound,()->s.sound=!s.sound);
            v.buttonIcon(p,"play","TESTAR DISPARO",295,425,310,43,false,()->g.audio.play(Game.Weapon.ECHO.sound()));
        }else if(tab==3){
            adjust(p,0,"Campo de visão horizontal",s.fov+"°",()->s.fov=Math.max(70,s.fov-5),()->s.fov=Math.min(110,s.fov+5));
            adjust(p,1,"Mira: comprimento",s.crossSize+" px",()->s.crossSize=Math.max(2,s.crossSize-1),()->s.crossSize=Math.min(16,s.crossSize+1));
            adjust(p,2,"Mira: abertura",s.crossGap+" px",()->s.crossGap=Math.max(0,s.crossGap-1),()->s.crossGap=Math.min(16,s.crossGap+1));
            segments(p,3,"Mira: espessura",new String[]{"1 px","2 px","3 px","4 px"},s.crossWidth-1,i->s.crossWidth=i+1);
            swatches(p,4,"Cor da mira",s);
            row(p,5,"Mira dinâmica / ponto");switchAt(p,816,448,"Dinâmica",s.dynamicCrosshair,()->s.dynamicCrosshair=!s.dynamicCrosshair);switchAt(p,1031,448,"Ponto",s.crossDot,()->s.crossDot=!s.crossDot);
            segments(p,6,"Mapa de habilidade / câmera",new String[]{"ALTERNAR","SEGURAR"},s.abilityHold?1:0,i->s.abilityHold=i==1);
            cross(p,s,1120,592,0);
        }else{
            toggle(p,0,"Movimento de câmera reduzido",s.lowMotion,()->s.lowMotion=!s.lowMotion);
            toggle(p,1,"Clarão escuro",s.softFlash,()->s.softFlash=!s.softFlash);
            toggle(p,2,"Legendas de efeitos",s.captions,()->s.captions=!s.captions);
            toggle(p,3,"HUD com alto contraste",s.highContrast,()->s.highContrast=!s.highContrast);
            segments(p,4,"Mira das outras armas",new String[]{"SEGURAR","ALTERNAR"},s.aimToggle?1:0,i->s.aimToggle=i==1);
            row(p,5,"Alternar agachar / andar");switchAt(p,816,448,"Agachar",s.crouchToggle,()->s.crouchToggle=!s.crouchToggle);switchAt(p,1031,448,"Andar",s.walkToggle,()->s.walkToggle=!s.walkToggle);
            toggle(p,6,"Inverter eixo vertical",s.invertY,()->s.invertY=!s.invertY);
            segments(p,7,"Mira da sniper",new String[]{"SEGURAR","ALTERNAR"},s.sniperToggle?1:0,i->s.sniperToggle=i==1);
        }
        line(p,42,633,1231,633,UiTheme.BORDER,1);
        v.button(p,"RESTAURAR TECLAS",42,648,210,43,false,()->{s.resetBinds();g.bindCapture=null;});
        v.buttonIcon(p,"save","SALVAR E VOLTAR",935,648,296,43,true,()->{s.save();g.bindCapture=null;g.ui=g.backUi;});
    }
    String percent(double value){return Math.round(value*100)+"%";}String on(boolean b){return b?"ATIVADO":"DESATIVADO";}
    int index(int[] values,int value){for(int i=0;i<values.length;i++)if(values[i]==value)return i;return 0;}
    int row(Graphics2D p,int row,String label){int y=140+row*60;text(p,label,296,y+26,14,WHITE,false);line(p,295,y+48,1220,y+48,UiTheme.BORDER,1);return y;}
    void adjust(Graphics2D p,int row,String label,String value,Runnable minus,Runnable plus){int y=row(p,row,label);v.iconButton(p,"minus","Diminuir "+label,832,y,48,39,minus);center(p,value,1005,y+26,15,MINT,true);v.iconButton(p,"plus","Aumentar "+label,1165,y,48,39,plus);}
    void toggle(Graphics2D p,int row,String label,boolean value,Runnable action){int y=row(p,row,label);switchAt(p,1034,y+8,value?"Ativado":"Desativado",value,action);}
    void switchAt(Graphics2D p,int x,int y,String label,boolean value,Runnable action){
        boolean hover=v.mouseX>=x&&v.mouseX<x+182&&v.mouseY>=y-8&&v.mouseY<y+31;
        text(p,label,x,y+17,12,value?MINT:MUTED,false);p.setColor(value?MINT:hover?UiTheme.MUTED:UiTheme.BORDER);p.fillRoundRect(x+140,y,40,22,22,22);
        p.setColor(value?INK:WHITE);p.fillOval(x+143+(value?18:0),y+3,16,16);v.buttons.add(new View.Button(x,y-8,182,39,action));
    }
    void segments(Graphics2D p,int row,String label,String[] values,int selected,java.util.function.IntConsumer action){int y=row(p,row,label);double w=405./values.length;for(int i=0;i<values.length;i++){final int choice=i;v.tab(p,values[i],810+i*w,y,w-3,39,selected==i,()->action.accept(choice));}}
    void swatches(Graphics2D p,int row,String label,Settings s){int y=row(p,row,label);int[] colors={0xB3FFE0,0x79ECFF,0xFFF56E,0xF79BEF};String[] names={"MENTA","CIANO","AMARELO","MAGENTA"};for(int i=0;i<colors.length;i++){final int choice=i;int x=810+i*102;rect(p,x,y,96,39,s.crossColor==i?UiTheme.RAISED:UiTheme.SURFACE);p.setColor(new Color(colors[i]));p.fillRoundRect(x+9,y+12,14,14,4,4);text(p,names[i],x+30,y+26,9,s.crossColor==i?WHITE:MUTED,true);if(s.crossColor==i)line(p,x,y+38,x+96,y+38,new Color(colors[i]),2);v.buttons.add(new View.Button(x,y,96,39,()->s.crossColor=choice));}}
    static void cross(Graphics2D p,Settings s,int x,int y,int error){int gap=s.crossGap+(s.dynamicCrosshair?error:0),size=s.crossSize;Color c=new Color(new int[]{0xB3FFE0,0x79ECFF,0xFFF56E,0xF79BEF}[s.crossColor]);p.setStroke(new BasicStroke(s.crossWidth+2));p.setColor(new Color(4,16,23,210));for(int pass=0;pass<2;pass++){p.drawLine(x-gap-size,y,x-gap,y);p.drawLine(x+gap,y,x+gap+size,y);p.drawLine(x,y-gap-size,x,y-gap);p.drawLine(x,y+gap,x,y+gap+size);p.setColor(c);p.setStroke(new BasicStroke(s.crossWidth));}if(s.crossDot)p.fillRect(x-1,y-1,3,3);}
}
