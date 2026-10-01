package rift;

import static rift.Game.*;
import static rift.World.*;

/** Round endings, side victory conditions and overtime. */
final class Rules {
    final Game g;
    Rules(Game game){g=game;}

    long living(int team) {
        return g.actors.stream().filter(a->a.team==team&&!a.dead).count();
    }

    void checkRules() {
        if(g.phase!=Phase.LIVE)return;
        if(g.living(1-g.attackTeam)==0)g.finishRound(g.attackTeam,"Equipe defensora eliminada");
        else if(g.living(g.attackTeam)==0&&!g.planted)g.finishRound(1-g.attackTeam,"Equipe atacante eliminada");
        else if(g.timer<=0&&!g.planted)g.finishRound(1-g.attackTeam,"Tempo esgotado");
    }

    void finishRound(int winner,String reason) {
        if(g.phase==Phase.END||g.phase==Phase.MATCH)return;
        g.combat.swing=g.combat.inspect=0;
        g.phase=Phase.END;
        g.timer=5;
        g.roundWinner=winner;
        g.endAge=0;
        g.dashTime=0;
        g.tacticalTargets.clear();
        if(winner==0)g.scoreBlue++;
        else g.scoreRed++;
        g.endTitle=winner==0?"RODADA VENCIDA":"RODADA PERDIDA";
        g.endReason=reason;
        g.economy.rewardRound(winner);
        g.ui="play";
        g.audio.play(winner==0?"win":"lose");
    }

    boolean overtime() {
        return g.flow.mode==MatchFlow.Mode.COMPETITIVE&&g.scoreBlue>=g.flow.mode.target-1&&g.scoreRed>=g.flow.mode.target-1;
    }

    boolean matchFinished() {
        int max=Math.max(g.scoreBlue,g.scoreRed);
        if(max<g.flow.mode.target)return false;
        return g.flow.mode!=MatchFlow.Mode.COMPETITIVE||Math.abs(g.scoreBlue-g.scoreRed)>=2||g.round>=19;
    }
}
