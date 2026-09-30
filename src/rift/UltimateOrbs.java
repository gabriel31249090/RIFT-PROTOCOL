package rift;

import static rift.Game.*;
import static rift.World.*;

/** Two contestable, shared pickups per round. Holding interact commits the player. */
final class UltimateOrbs {
    final Game g;final boolean[] taken=new boolean[2];double progress;int selected=-1;
    UltimateOrbs(Game g){this.g=g;}
    V position(int i){return i==0?new V(43,0,67):new V(98,0,38);}
    void reset(){taken[0]=taken[1]=false;progress=0;selected=-1;}
    int nearest(){if(g.player==null||g.player.dead||g.player.ult>=6)return -1;for(int i=0;i<2;i++){V p=position(i);if(!taken[i]&&Math.hypot(g.player.x-p.x(),g.player.z-p.z())<1.7&&g.world.visible(g.player.eye(),p.add(new V(0,.7,0))))return i;}return -1;}
    void tick(double dt,boolean interact){int i=nearest();if(i!=selected){progress=0;selected=i;}if(i<0||!interact||g.player.moveSpeed>.1||g.player.damageGlow>0||g.sentinels.watching()){progress=0;return;}progress+=dt;if(progress>=.9){taken[i]=true;g.player.ult=Math.min(6,g.player.ult+1);progress=0;g.audio.play("focus");g.tell("ORBE CAPTURADO  •  +1 ponto de suprema",2);}}
    void draw(Renderer r,double time){for(int i=0;i<2;i++)if(!taken[i]){V p=position(i);AbilityArt.gem(r,p.add(new V(0,.75+.08*Math.sin(time*3),0)),.20,.32,0xB9E4DE,time);r.ring(p.x(),.04,p.z(),.48,.04,0xDBE8C0);}}
}
