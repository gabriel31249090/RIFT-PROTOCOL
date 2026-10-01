package rift;

import java.nio.file.*;
import java.util.*;
import java.awt.event.KeyEvent;
import static java.awt.event.KeyEvent.*;

/** Backward-compatible preferences. Only declared scalar fields are persisted. */
final class Settings {
    double sensitivity=.0021,adsSensitivity=.65,volume=.45,effectsVolume=1,musicVolume=.15;
    int quality=3,frameLimit=120,difficulty=1,agent=0,renderDistance=160,fov=90;
    int crossSize=7,crossGap=4,crossWidth=1,crossColor=0;
    boolean invertY=false,sound=true,shadows=true,dynamicCrosshair=true,crossDot=true;
    boolean sniperToggle=true,textures=true,impactFX=true;
    boolean smoothUpscale=false;
    boolean aimToggle=false,crouchToggle=false,walkToggle=false,abilityHold=false;
    boolean lowMotion=false,softFlash=false,captions=true,highContrast=false;
    final Path path;
    enum Action {
        FORWARD("Avançar",VK_W),BACK("Recuar",VK_S),LEFT("Esquerda",VK_A),RIGHT("Direita",VK_D),
        JUMP("Saltar / bhop",VK_SPACE),WALK("Andar",VK_SHIFT),CROUCH("Agachar",VK_CONTROL),
        RELOAD("Recarregar",VK_R),PISTOL("Pistola",VK_1),PRIMARY("Principal",VK_2),MELEE("Lâmina",VK_3),
        Q("Habilidade Q",VK_Q),E("Assinatura E",VK_E),C("Habilidade C",VK_C),ULT("Suprema",VK_X),
        USE("Objetivo / interagir",VK_F),SHOP("Comprar",VK_B),PING("Ping",VK_Z),INSPECT("Inspecionar",VK_V),
        SCORE("Placar",VK_TAB),AGENT("Trocar agente",VK_G),DAMAGE("Dano no treino",VK_H),
        TRAIN("Treino de bhop",VK_T),QUICKBUY("Recomprar",VK_K),FUNDS("Pedir créditos",VK_L);
        final String label;final int key;Action(String label,int key){this.label=label;this.key=key;}
    }
    final int[] binds=new int[Action.values().length];
    Settings(boolean persistent){this(persistent?Path.of(System.getProperty("user.home"),".rift-protocol","settings.properties"):null);}
    Settings(Path path){this.path=path;resetBinds();if(path==null||!Files.isRegularFile(path))return;
        try(var in=Files.newInputStream(path)){Properties p=new Properties();p.load(in);
            for(var f:Settings.class.getDeclaredFields())try{String value=p.getProperty(f.getName());if(value==null)continue;if(f.getType()==double.class)f.setDouble(this,Double.parseDouble(value));else if(f.getType()==int.class)f.setInt(this,Integer.parseInt(value));else if(f.getType()==boolean.class)f.setBoolean(this,Boolean.parseBoolean(value));}catch(Exception ignored){}
            for(Action a:Action.values())try{bind(a,Integer.parseInt(p.getProperty("bind."+a.name(),""+a.key)));}catch(Exception ignored){}
        }catch(Exception ignored){}validate();
    }
    void validate(){sensitivity=clamp(sensitivity,.0003,.008);adsSensitivity=clamp(adsSensitivity,.1,1.5);volume=clamp(volume,0,1);effectsVolume=clamp(effectsVolume,0,1);musicVolume=clamp(musicVolume,0,1);quality=(int)clamp(quality,0,3);frameLimit=(int)clamp(frameLimit,60,240);difficulty=(int)clamp(difficulty,0,2);agent=(int)clamp(agent,0,Game.Agent.values().length-1);renderDistance=(int)clamp(renderDistance,60,210);fov=(int)clamp(fov,70,110);crossSize=(int)clamp(crossSize,2,16);crossGap=(int)clamp(crossGap,0,16);crossWidth=(int)clamp(crossWidth,1,4);crossColor=Math.floorMod(crossColor,4);}
    void resetBinds(){for(Action a:Action.values())binds[a.ordinal()]=a.key;}
    void performance(){quality=3;frameLimit=120;renderDistance=100;shadows=false;impactFX=false;smoothUpscale=false;textures=true;}
    boolean bind(Action action,int key){if(key<1||key>525||key==VK_ESCAPE||key==VK_ENTER||key==VK_F1||key==VK_F10||key==VK_F11||key==VK_F12)return false;int old=binds[action.ordinal()];for(Action a:Action.values())if(a!=action&&binds[a.ordinal()]==key)binds[a.ordinal()]=old;binds[action.ordinal()]=key;return true;}
    String key(Action a){return KeyEvent.getKeyText(binds[a.ordinal()]);}
    Input.Frame remap(Input.Frame raw){BitSet down=(BitSet)raw.down().clone(),edges=(BitSet)raw.edges().clone();for(Action a:Action.values()){down.clear(a.key);edges.clear(a.key);down.clear(binds[a.ordinal()]);edges.clear(binds[a.ordinal()]);}for(Action a:Action.values()){down.set(a.key,raw.held(binds[a.ordinal()]));edges.set(a.key,raw.pressed(binds[a.ordinal()]));}return new Input.Frame(down,edges,raw.fire(),raw.aim(),raw.click(),raw.dx(),raw.dy(),raw.mx(),raw.my());}
    void save(){if(path==null)return;try{validate();Files.createDirectories(path.getParent());Properties p=new Properties();for(var f:Settings.class.getDeclaredFields())if(f.getType()==double.class||f.getType()==int.class||f.getType()==boolean.class)p.setProperty(f.getName(),String.valueOf(f.get(this)));for(Action a:Action.values())p.setProperty("bind."+a.name(),""+binds[a.ordinal()]);Path tmp=path.resolveSibling("settings.tmp");try(var out=Files.newOutputStream(tmp)){p.store(out,"RIFT Protocol / preferences");}Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}catch(Exception ignored){}}
    static double clamp(double n,double a,double b){return Double.isFinite(n)?Math.max(a,Math.min(b,n)):a;}
}
