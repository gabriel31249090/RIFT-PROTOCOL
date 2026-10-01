package rift;

import static rift.Game.*;
import static rift.World.*;

/** Credits, defeat bonuses and teammate transfers. */
final class Economy {
    final Game g;
    Economy(Game game){g=game;}

    void requestFunds() {
        if(g.player==null||g.phase!=Phase.BUY||g.fundsAsked)return;
        Actor donor=null;
        for(Actor a:g.actors)if(a!=g.player&&a.team==g.player.team&&a.credits>1400&&(donor==null||a.credits>donor.credits))donor=a;
        if(donor==null) {
            g.tell("Aliados sem créditos disponíveis",2);
            return;
        }
        int amount=Math.min(600,9000-g.player.credits);
        donor.credits-=amount;
        g.player.credits+=amount;
        g.fundsAsked=true;
        g.tell(donor.name+" transferiu "+amount+" créditos",3);
    }
    static final int MAX_CREDITS=9000, WIN_REWARD=3000;
    static int lossBonus(int consecutiveLosses){return Math.min(2900,1900+Math.max(0,consecutiveLosses-1)*500);}
    void rewardRound(int winner){
        for(Actor a:g.actors){
            if(a.team==winner){a.credits=Math.min(MAX_CREDITS,a.credits+WIN_REWARD);a.losses=0;}
            else{a.losses++;a.credits=Math.min(MAX_CREDITS,a.credits+lossBonus(a.losses));}
        }
    }
}
