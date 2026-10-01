package rift;

import static rift.Game.*;
import static rift.World.*;

/** Purchases and bot equipment; inventory limits are enforced here. */
final class Loadout {
    final Game g;
    Loadout(Game game){g=game;}

    boolean buyWeapon(Weapon w) {
        if(!(g.phase==Phase.BUY||g.phase==Phase.TRAIN||g.flow.mode.respawn)||g.player.dead)return false;
        Gun existing=w.sidearm()?g.player.pistol:g.player.primary;
        if(existing!=null&&existing.kind==w) {
            g.player.gun().burstLeft=0;
            g.player.gun().reload=0;
            g.combat.swing=g.combat.inspect=0;
            if(g.player.slot!=(w.sidearm()?1:2)) {
                g.player.slot=w.sidearm()?1:2;
                g.weaponEquip=.4;
                g.recoil=g.aimLerp=0;
                g.aiming=false;
            }
            g.tell("Arma já equipada",2);
            return false;
        }
        if(!g.training&&!g.flow.mode.respawn&&g.player.credits<w.price) {
            g.tell("Créditos insuficientes",2);
            g.audio.play("deny");
            return false;
        }
        if(!g.training&&!g.flow.mode.respawn)g.player.credits-=w.price;
        g.player.gun().burstLeft=0;
        g.player.gun().reload=0;
        g.combat.swing=g.combat.inspect=0;
        if(w.sidearm()) {
            g.player.pistol=new Gun(w);
            g.player.slot=1;
        }
        else {
            g.player.primary=new Gun(w);
            g.player.slot=2;
        }
        g.recoil=0;
        g.aiming=false;
        g.aimLerp=0;
        g.weaponEquip=.4;
        g.audio.play("equip");
        g.tell(w.label+" equipado",2);
        return true;
    }

    void equipBot(Actor a) {
        if(a.primary==null) {
            Weapon[] preference= {
                Weapon.ECHO,Weapon.SHADE,Weapon.HELIX,Weapon.WISP,Weapon.CIRCUIT,Weapon.RIDGE,Weapon.BASTION,Weapon.MARROW,Weapon.HORIZON
            };
            Weapon wanted=preference[Math.floorMod(a.id+g.round,preference.length)];
            if(a.credits>=wanted.price+400) {
                a.primary=new Gun(wanted);
                a.credits-=wanted.price;
                a.slot=2;
            }
            else if(a.credits>=1600) {
                a.primary=new Gun(Weapon.CIRCUIT);
                a.credits-=1200;
                a.slot=2;
            }
            else if(a.credits>=500&&a.id%3==0&&a.pistol.kind==Weapon.SPARK) {
                a.pistol=new Gun(Weapon.VEIL);
                a.credits-=500;
            }
        }
        if(a.armor<25&&a.credits>=400) {
            a.armor=25;
            a.credits-=400;
        }
    }

    boolean buyArmor(int amount) {
        if(!(g.phase==Phase.BUY||g.phase==Phase.TRAIN||g.flow.mode.respawn)||g.player.dead)return false;
        int price=amount==25?400:1000;
        if(g.player.armor>=amount) {
            g.tell("Proteção já equipada",2);
            return false;
        }
        if(!g.training&&!g.flow.mode.respawn&&g.player.credits<price) {
            g.tell("Créditos insuficientes",2);
            return false;
        }
        if(!g.training&&!g.flow.mode.respawn)g.player.credits-=price;
        g.player.armor=amount;
        g.audio.play("ui");
        return true;
    }

    boolean buyAbility(int slot) {
        if(!(g.phase==Phase.BUY||g.phase==Phase.TRAIN)||g.player.dead||slot==2)return false;
        int max=g.agent.charges(slot);
        if(g.charges(slot)>=max) {
            g.tell("Cargas completas",2);
            return false;
        }
        if(!g.training&&g.player.credits<200) {
            g.tell("Créditos insuficientes",2);
            return false;
        }
        if(!g.training)g.player.credits-=200;
        if(slot==0)g.player.qCharges++;
        else if(slot==1)g.player.cCharges++;
        else g.player.eCharges++;
        g.audio.play("ui");
        return true;
    }

    void quickBuy() {
        if(g.player==null||!(g.phase==Phase.BUY||g.phase==Phase.TRAIN)||g.player.dead)return;
        g.buyWeapon(g.profile.savedPistol);
        g.buyWeapon(g.profile.savedPrimary);
        g.buyArmor(50);
        g.tell("Recompra: itens disponíveis dentro do seu saldo",2);
    }
}
