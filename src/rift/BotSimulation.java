package rift;

import java.util.*;
import static rift.Game.*;

/** Reproducible full matches; results are a smoke test, not a competitive balance claim. */
final class BotSimulation {
    static void run(int matches) {
        matches=Math.max(1,Math.min(30,matches));
        int rounds=0,attackWins=0,plants=0,defuses=0,timeouts=0;
        long ticks=0,totalNanos=0;double[] samples=new double[24000];int sampled=0;
        System.out.println("RIFT 1.7 / bots × bots / 60 ticks por segundo simulado / sem render");
        System.out.println("Java "+System.getProperty("java.version")+" / "+System.getProperty("os.name"));
        for(int match=0;match<matches;match++) {
            Game g=new Game(new Settings(false),false,17000+match);
            g.flow.mapIndex=match%3;g.startBots();int localTicks=0,localRounds=0,localAttack=0;
            while(g.phase!=Phase.MATCH&&localTicks<150000) {
                if(g.phase==Phase.BUY)g.beginRound();
                Phase before=g.phase;boolean planted=g.planted;
                long start=System.nanoTime();g.tick(1/60.,Input.Frame.empty());long elapsed=System.nanoTime()-start;
                if(localTicks>600)totalNanos+=elapsed;
                if(localTicks>600&&sampled<samples.length)samples[sampled++]=elapsed/1e6;
                if(!planted&&g.planted)plants++;
                if(before==Phase.LIVE&&g.phase==Phase.END) {
                    localRounds++;rounds++;
                    if(g.roundWinner==g.attackTeam){attackWins++;localAttack++;}
                    if(g.endReason.contains("desarmado"))defuses++;
                    if(g.endReason.contains("Tempo"))timeouts++;
                }
                for(Actor a:g.actors)if(!Double.isFinite(a.x+a.y+a.z+a.yaw+a.hp)||a.credits<0||a.gun().ammo<0)
                    throw new AssertionError("Estado inválido em "+a.name);
                localTicks++;
            }
            if(g.phase!=Phase.MATCH)throw new AssertionError("Partida travada: seed "+(17000+match));
            ticks+=Math.max(0,localTicks-601);
            System.out.printf(Locale.ROOT,"seed=%d mapa=%s placar=%d:%d rounds=%d ataque=%d ticks=%d%n",
                17000+match,World.NAMES[g.world.mapIndex],g.scoreBlue,g.scoreRed,localRounds,localAttack,localTicks);
            g.close();
        }
        Arrays.sort(samples,0,sampled);
        System.out.printf(Locale.ROOT,"CONCLUÍDO: %d partidas / %d rounds / ataque %.1f%% / %d plants / %d defuses / %d timeouts%n",
            matches,rounds,100.*attackWins/Math.max(1,rounds),plants,defuses,timeouts);
        System.out.printf(Locale.ROOT,"CPU simulação: média %.3f ms/tick / p95 %.3f ms / p99 %.3f ms (amostra inicial %d)%n",
            totalNanos/1e6/Math.max(1,ticks),samples[(int)(sampled*.95)],samples[(int)(sampled*.99)],sampled);
        System.out.println("Amostra pequena e local; não valida win rate competitivo nem FPS com janela.");
    }
}
