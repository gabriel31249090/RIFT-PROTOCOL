package rift;

import java.util.Random;
import static rift.Game.*;

/** Original layered shot samples: transient, pressure body, mechanism and room tail. */
final class ShotAudio {
    static float[] render(Weapon w,int variant){
        boolean suppress=w.silenced();double duration=w==Weapon.HORIZON?.72:w.pellets>1?.56:w.sidearm()?.35:.43;
        int n=(int)(22050*duration);float[] out=new float[n];double[] dry=new double[n];Random random=new Random(w.ordinal()*771L+variant*29+811);double low=0,old=0;
        double pitch=1+(variant-1)*.025,base=(w==Weapon.HORIZON?66:w.pellets>1?78:w==Weapon.TALON?95:w.category==Category.HEAVY?74:w.category==Category.SMG?165:w.sidearm()?190:113)*pitch;
        for(int i=0;i<n;i++){
            double t=i/22050.,noise=random.nextDouble()*2-1;low=low*.81+noise*.19;double high=noise-old;old=noise;
            double attack=Math.min(1,t/.0007),crack=high*Math.exp(-t/(suppress?.004:.008))*(suppress?.15:.53);
            double body=Math.sin(2*Math.PI*(base*t+29*(1-Math.exp(-t*40))/40))*Math.exp(-t/(suppress?.024:.048))*(suppress?.32:.51);
            double powder=low*Math.exp(-t/.055)*(w.pellets>1?.78:.43),metal=Math.sin(t*2*Math.PI*(1600+w.ordinal()*57))*Math.exp(-Math.max(0,t-.021)*140)*(t>=.021&&t<.065?.085:0);
            dry[i]=(crack+body+powder+metal)*attack;
        }
        for(int i=0;i<n;i++){double value=dry[i];for(int j=1;j<=3;j++){int back=i-(int)(22050*(.029*j+.005*(w.ordinal()%3)));if(back>=0)value+=dry[back]*(suppress?.025:.095)/j;}out[i]=(float)Math.tanh(value*1.15);}
        return out;
    }
}
