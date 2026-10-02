package rift;

import static rift.Game.*;
import static rift.Tests.*;
import static rift.UpdateTests.*;

/** Ground friction and projected acceleration contracts for the 1.9 movement update. */
final class MovementTests {
    static void run(){ground();rates();}
    static void ground(){
        Game g=arena();Actor a=g.player;
        a.vx=5;a.vz=0;a.intentX=0;a.intentZ=1;
        g.combat.movement(1/240.,5.4);
        check(a.vx>4.7&&a.vx<5&&a.vz>0,"Troca de direcao conserva impulso ortogonal com atrito separado");
        for(int i=0;i<120;i++)g.combat.movement(1/240.,5.4);
        check(Math.hypot(a.vx,a.vz)<=5.400001&&a.vz>5,"Aceleracao projetada nao excede o limite de corrida no solo");
        a.x=42;a.z=72;a.vx=6.2;a.vz=0;a.intentX=a.intentZ=0;
        g.combat.movement(1/240.,6.2);
        check(a.vx>0&&a.vx<6.2,"Atrito sem entrada inicia frenagem continua");
        g.combat.movement(.09,6.2);
        check(a.vx==0&&a.vz==0,"Atrito interrompe corrida com lamina em menos de 100 ms");
        a.x=42;a.z=72;a.vx=5.4;a.vz=0;a.intentX=-1;
        g.combat.movement(.08,5.4);
        check(a.vx<0,"Contra-strafe usa projecao oposta e cruza o repouso em 80 ms");
        a.x=42;a.z=72;a.vx=a.vz=0;a.intentX=1;a.intentZ=0;
        g.combat.movement(.3,.15);
        check(a.vx>.149&&a.vx<=.150001,"Atrito de controle respeita combinacoes muito lentas de agachar e efeitos");
        a.x=42;a.z=72;a.vx=a.vz=0;a.intentX=a.intentZ=Math.sqrt(.5);
        g.combat.movement(.3,5.4);
        check(Math.abs(Math.hypot(a.vx,a.vz)-5.4)<.000001,"Entrada diagonal normalizada nao aumenta a velocidade no solo");
        a.x=42;a.z=72;a.vx=5.4;a.vz=0;a.intentX=Math.cos(Math.PI/6);a.intentZ=Math.sin(Math.PI/6);
        g.combat.movement(.15,5.4);
        check(a.vx>0&&a.vz>0&&Math.hypot(a.vx,a.vz)<=5.400001,"Giro de trinta graus redireciona impulso sem acelerar alem do limite no solo");
        g.close();
    }
    static void rates(){
        double[] distance=new double[3];int[] rates={60,120,144};
        for(int k=0;k<rates.length;k++){
            Game g=arena();Actor a=g.player;a.vx=a.vz=0;a.intentX=1;a.intentZ=0;
            double travelled=0;
            for(int i=0;i<rates[k];i++){
                if(i>=rates[k]/2){a.intentX=0;a.intentZ=-1;}
                double x=a.x,z=a.z;g.combat.movement(1./rates[k],5.4);
                travelled+=Math.hypot(a.x-x,a.z-z);
            }
            distance[k]=travelled;g.close();
        }
        check(java.util.Arrays.stream(distance).max().orElse(0)-java.util.Arrays.stream(distance).min().orElse(0)<.025,"Atrito e mudanca de direcao sao consistentes em 60, 120 e 144 FPS");
    }
}
