package rift;

import java.awt.*;
import static rift.View.*;
import static rift.Game.*;
import static rift.Cosmetics.*;

/** Loadout editor using the same meshes/materials as the first-person renderer. */
final class CollectionUI {
    final View view;final Game game;final Renderer stage=new Renderer(700,370);
    Weapon weapon=Weapon.ECHO;boolean blades;int blade,skin,charm;String status="";
    CollectionUI(View v){view=v;game=v.game;load();}
    void open(){game.backUi=game.ui;game.ui="collection";load();}
    void load(){skin=blades?game.profile.meleeSkin:game.profile.skins[weapon.ordinal()];charm=game.profile.charms[weapon.ordinal()];blade=game.profile.melee;status="";}
    void equip(boolean all){
        if(blades){game.profile.melee=blade;game.profile.meleeSkin=skin;}
        else if(all){java.util.Arrays.fill(game.profile.skins,skin);java.util.Arrays.fill(game.profile.charms,charm);}
        else {game.profile.skins[weapon.ordinal()]=skin;game.profile.charms[weapon.ordinal()]=charm;}
        game.profile.save();status=game.profile.saveError.isEmpty()?(all?"VISUAL APLICADO ÀS 15 ARMAS":"EQUIPADO / SALVO NO PERFIL"):game.profile.saveError;game.audio.play("select");
    }
    void render(Graphics2D g){
        rect(g,0,0,1280,720,UiTheme.BACKGROUND);logo(g,32,27,25);tracked(g,"RIFT / OFICINA 01",72,47,12,0,MUTED);
        text(g,"COLEÇÃO",31,109,34,WHITE,true);line(g,32,138,1246,138,UiTheme.BORDER,1);
        view.buttonIcon(g,"arrow-left","VOLTAR",1050,32,196,36,false,()->game.ui=game.backUi);
        text(g,"TODOS OS VISUAIS GRATUITOS",861,108,11,MINT,true);
        view.tab(g,"ARMAS",32,159,101,34,!blades,()->{blades=false;load();});view.tab(g,"LÂMINAS",141,159,101,34,blades,()->{blades=true;load();});
        if(!blades){int i=0;for(Weapon w:Weapon.values()){int y=210+i++*28;boolean chosen=w==weapon;rect(g,38,y,198,26,chosen?UiTheme.RAISED:UiTheme.BACKGROUND);if(chosen)rect(g,38,y,2,26,MINT);text(g,w.label,49,y+18,12,chosen?WHITE:MUTED,true);right(g,w.category==Category.PISTOL?"P":"•",222,y+18,10,chosen?MINT:MUTED,true);view.buttons.add(new View.Button(38,y,198,26,()->{weapon=w;load();}));}}
        else {int i=0;for(Melee m:Melee.values()){int y=218+i++*91;rect(g,42,y,190,78,blade==m.ordinal()?UiTheme.RAISED:UiTheme.SURFACE);text(g,m.label,54,y+30,20,WHITE,true);View.wrap(g,m.detail,54,y+49,168,10,MUTED);view.buttons.add(new View.Button(42,y,190,78,()->{blade=m.ordinal();status="";}));}}
        Skin finish=Skin.values()[skin];Charm trinket=Charm.values()[charm];
        rect(g,260,159,690,386,UiTheme.SURFACE);g.drawImage(stage.collectionPreview(blades?null:weapon,blades?Melee.values()[blade]:null,finish,trinket,game.visualTime),260,175,690,365,null);
        tracked(g,blades?"CORPO A CORPO":weapon.type,281,185,10,2,MUTED);
        text(g,blades?Melee.values()[blade].label:weapon.label,281,510,28,WHITE,true);right(g,finish.label,926,511,13,new Color(finish.accent),true);
        text(g,"ACABAMENTO",970,176,11,MINT,true);
        for(Skin s:Skin.values()){int y=191+s.ordinal()*48;boolean chosen=s.ordinal()==skin;rect(g,970,y,276,42,chosen?UiTheme.RAISED:UiTheme.SURFACE);rect(g,981,y+10,23,23,new Color(s.trim));rect(g,996,y+10,8,23,new Color(s.accent));text(g,s.label,1018,y+27,13,WHITE,true);if(chosen)UiIcons.draw(g,"check",1218,y+12,17,MINT);view.buttons.add(new View.Button(970,y,276,42,()->{skin=s.ordinal();status="";}));}
        View.wrap(g,blades?"Corte: 50 / forte: 75. Pelas costas: dano dobrado. As três lâminas têm o mesmo alcance e velocidade.":"Acabamentos e pingentes mudam o visual. Dano, precisão e cadência são os mesmos.",971,508,265,12,MUTED);
        if(!blades){text(g,"PINGENTE",262,570,11,MINT,true);for(Charm c:Charm.values()){int i=c.ordinal(),x=260+(i%3)*234,y=581+(i/3)*37;view.tab(g,c.label,x,y,222,31,charm==i,()->{charm=i;status="";});}}
        else {text(g,"LÂMINA LIVRE EM TODAS AS PARTIDAS",280,584,12,MINT,true);View.wrap(g,"Equipe com 3, aproxime-se e ataque. Volte ao fuzil com 2. Inspecionar é apenas uma animação e pode ser interrompido.",280,611,646,13,MUTED);}
        line(g,32,658,1246,658,new Color(0x2E4855),1);
        text(g,status.isBlank()?"V em partida / inspecionar equipamento":status,34,690,11,status.isBlank()?MUTED:MINT,true);
        if(!blades)view.button(g,"APLICAR ÀS 15 ARMAS",712,669,254,35,false,()->equip(true));
        view.buttonIcon(g,"check","EQUIPAR",982,669,264,35,true,()->equip(false));
    }
}
