package rift;

import java.nio.file.*;
import java.util.*;

/** Local-only progression. Atomic saves keep an interrupted game from erasing a profile. */
final class Profile {
    static final String[] RANKS={"FERRO","BRONZE","PRATA","OURO","PLATINA","DIAMANTE","ASCENDENTE","IMORTAL","RADIANTE"};
    Game.Weapon savedPrimary=Game.Weapon.ECHO,savedPistol=Game.Weapon.SPARK;
    final Path path;int wallet=1800,xp,matches,wins,kills,aces,rating=250,unlocked=7,lastXp,lastRating;
    final int[] skins=new int[Game.Weapon.values().length],charms=new int[Game.Weapon.values().length];int melee,meleeSkin;
    final int[] mastery=new int[Game.Agent.values().length];final List<String> history=new ArrayList<>();String saveError="";
    Profile(Path path){this.path=path;if(path==null||!Files.isRegularFile(path))return;Properties p=new Properties();try(var input=Files.newInputStream(path)){p.load(input);try{savedPrimary=Game.Weapon.valueOf(p.getProperty("loadout.primary","ECHO"));savedPistol=Game.Weapon.valueOf(p.getProperty("loadout.pistol","SPARK"));if(savedPrimary.sidearm())savedPrimary=Game.Weapon.ECHO;if(!savedPistol.sidearm())savedPistol=Game.Weapon.SPARK;}catch(Exception ignored){}for(Game.Weapon w:Game.Weapon.values()){skins[w.ordinal()]=Math.min(Cosmetics.Skin.values().length-1,number(p,"skin."+w.name(),0));charms[w.ordinal()]=Math.min(Cosmetics.Charm.values().length-1,number(p,"charm."+w.name(),0));}melee=Math.min(Cosmetics.Melee.values().length-1,number(p,"melee",0));meleeSkin=Math.min(Cosmetics.Skin.values().length-1,number(p,"meleeSkin",0));wallet=number(p,"wallet",1800);xp=number(p,"xp",0);matches=number(p,"matches",0);wins=number(p,"wins",0);kills=number(p,"kills",0);aces=number(p,"aces",0);rating=Math.min(2699,number(p,"rating",250));unlocked=number(p,"unlocked",7)|7;for(int i=0;i<mastery.length;i++)mastery[i]=number(p,"mastery."+i,0);String h=p.getProperty("history","");if(!h.isBlank())history.addAll(Arrays.asList(h.split("\\|")));}catch(Exception ex){saveError="Não foi possível ler a progressão local.";}}
    static int number(Properties p,String key,int fallback){try{return Math.max(0,Integer.parseInt(p.getProperty(key,""+fallback)));}catch(Exception e){return fallback;}}
    boolean unlocked(int index){return index>=9 || (unlocked&(1<<index))!=0;}
    boolean unlock(int index){if(unlocked(index))return true;if(wallet<300)return false;wallet-=300;unlocked|=1<<index;save();return true;}
    String rank(){return RANKS[Math.min(8,rating/300)];}
    int level(int agent){return 1+mastery[agent]/800;}
    void reward(Game game,boolean win){
        lastXp=300+game.player.kills*20+(win?200:0);lastRating=0;xp+=lastXp;wallet+=lastXp;matches++;if(win)wins++;kills+=game.player.kills;aces+=game.player.aces;mastery[game.player.agentIndex]+=lastXp;
        if(game.flow.mode==MatchFlow.Mode.COMPETITIVE){int old=rating;rating=Math.max(0,Math.min(2699,rating+(win?28:-20)));lastRating=rating-old;history.add(rank()+" "+rating+" / "+(win?"V":"D"));while(history.size()>12)history.remove(0);}save();
    }
    Cosmetics.Skin skin(Game.Weapon w){return Cosmetics.Skin.values()[skins[w.ordinal()]];}
    Cosmetics.Charm charm(Game.Weapon w){return Cosmetics.Charm.values()[charms[w.ordinal()]];}
    Cosmetics.Melee blade(){return Cosmetics.Melee.values()[melee];}
    void save(){if(path==null)return;try{Files.createDirectories(path.getParent());Properties p=new Properties();p.setProperty("loadout.primary",savedPrimary.name());p.setProperty("loadout.pistol",savedPistol.name());for(Game.Weapon w:Game.Weapon.values()){p.setProperty("skin."+w.name(),""+skins[w.ordinal()]);p.setProperty("charm."+w.name(),""+charms[w.ordinal()]);}p.setProperty("melee",""+melee);p.setProperty("meleeSkin",""+meleeSkin);p.setProperty("wallet",""+wallet);p.setProperty("xp",""+xp);p.setProperty("matches",""+matches);p.setProperty("wins",""+wins);p.setProperty("kills",""+kills);p.setProperty("aces",""+aces);p.setProperty("rating",""+rating);p.setProperty("unlocked",""+unlocked);for(int i=0;i<mastery.length;i++)p.setProperty("mastery."+i,""+mastery[i]);p.setProperty("history",String.join("|",history));Path tmp=path.resolveSibling("profile.tmp");try(var out=Files.newOutputStream(tmp)){p.store(out,"RIFT / local progression");}try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}saveError="";}catch(Exception e){saveError="Não foi possível salvar a progressão neste computador.";}}
}
