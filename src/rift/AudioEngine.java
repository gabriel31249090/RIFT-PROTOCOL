package rift;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.*;

/** Bounded stereo mixer with packaged WAV samples and original synthesis as fallback. */
final class AudioEngine implements AutoCloseable {
    static final int MAX_PENDING=24,MAX_VOICES=20;
    final Map<String,Integer> variants=new HashMap<>();
    final Settings settings;final BlockingQueue<Sound> queue=new ArrayBlockingQueue<>(MAX_PENDING);
    record Sound(String name,double pan,double gain){}
    volatile boolean menu=true;volatile String caption="";volatile long captionAt;
    volatile boolean running;volatile String error="";Thread thread;volatile SourceDataLine line;
    final Map<String,float[]> bank=new ConcurrentHashMap<>();
    AudioEngine(Settings s,boolean enabled){settings=s;if(!enabled)return;running=true;thread=new Thread(this::mix,"rift-audio");thread.setDaemon(true);thread.start();}
    void play(String sound){playAt(sound,0,1);}
    static String caption(String sound){
        if(sound==null)return "";
        if(sound.startsWith("shot_"))return "DISPARO";
        if(sound.startsWith("step_"))return "PASSOS";
        if(sound.startsWith("land_"))return "POUSO";
        if(sound.startsWith("impact_"))return "IMPACTO";
        if(sound.startsWith("reload_")||sound.startsWith("magout_")||sound.startsWith("magin_")||sound.startsWith("bolt_"))return "RECARGA";
        return switch(sound){case "step"->"PASSOS";case "land"->"POUSO";case "reload"->"RECARGA";case "scan"->"PULSO / ALARME";
            case "plant"->"NÚCLEO ARMADO";case "defuse"->"NÚCLEO DESARMADO";case "boom"->"EXPLOSÃO";case "flash"->"CLARÃO";
            case "beep"->"NÚCLEO ATIVO";case "distant","rifle","pistol","sniper","shotgun"->"DISPARO";default->"";};
    }
    synchronized String variant(String sound){
        if(!AudioAssets.varied(sound))return sound;
        int next=variants.getOrDefault(sound,0);variants.put(sound,(next+1)%3);return sound+"#"+next;
    }
    synchronized void playAt(String sound,double pan,double gain){
        if(sound==null||!Double.isFinite(pan)||!Double.isFinite(gain)||gain<=0)return;
        String label=caption(sound);if(!label.isEmpty()){caption=label;captionAt=System.nanoTime();}
        if(running&&settings.sound&&settings.volume>0)queue.offer(new Sound(variant(sound),Settings.clamp(pan,-1,1),Math.min(2,gain)));
    }
    float[] sample(String key){return bank.computeIfAbsent(key,name->{
        float[] wav=WavAudio.load(name);if(wav!=null)return wav;
        float[] original=AudioAssets.render(name);return original!=null?original:synthesize(name);
    });}
    static final class Voice{
        final float[] samples;final double pan,gain;int at;
        Voice(float[] s,double pan,double gain){samples=s;this.pan=Double.isFinite(pan)?Settings.clamp(pan,-1,1):0;this.gain=Double.isFinite(gain)?Settings.clamp(gain,0,2):0;}
    }
    void drain(List<Voice> voices){
        voices.removeIf(v->v.at>=v.samples.length);
        Sound sound;while((sound=queue.poll())!=null)if(voices.size()<MAX_VOICES)voices.add(new Voice(sample(sound.name()),sound.pan(),sound.gain()));
    }
    static long mixBlock(List<Voice> voices,byte[] bytes,long clock,boolean menu,Settings settings){
        if(bytes.length%4!=0)throw new IllegalArgumentException("Stereo buffer must contain complete frames");
        for(int i=0;i<bytes.length/4;i++){
            double left=0,right=0;
            for(Voice voice:voices)if(voice.at<voice.samples.length){
                double sample=voice.samples[voice.at++];double value=(Double.isFinite(sample)?sample:0)*voice.gain*settings.effectsVolume;
                left+=value*Math.sqrt((1-voice.pan)*.5);right+=value*Math.sqrt((1+voice.pan)*.5);
            }
            double t=clock++/(double)WavAudio.RATE;
            double music=menu?(.055*Math.sin(t*Math.PI*2*130.81)+.035*Math.sin(t*Math.PI*2*196)+.027*Math.sin(t*Math.PI*2*261.63))*settings.musicVolume*(.7+.3*Math.sin(t*.18)):0;
            double master=settings.sound?settings.volume:0;
            int l=(int)(Math.tanh(left+music)*master*21000),r=(int)(Math.tanh(right+music)*master*21000);
            bytes[i*4]=(byte)l;bytes[i*4+1]=(byte)(l>>8);bytes[i*4+2]=(byte)r;bytes[i*4+3]=(byte)(r>>8);
        }
        voices.removeIf(v->v.at>=v.samples.length);return clock;
    }
    void mix(){
        SourceDataLine output=null;
        try {
            AudioFormat format=new AudioFormat(WavAudio.RATE,16,2,true,false);
            output=AudioSystem.getSourceDataLine(format);
            synchronized(this){
                if(!running)return;
                output.open(format,4096);line=output;output.start();
            }
            List<Voice> voices=new ArrayList<>();byte[] bytes=new byte[1024];long musicClock=0;
            while(running){
                drain(voices);musicClock=mixBlock(voices,bytes,musicClock,menu,settings);output.write(bytes,0,bytes.length);
            }
        }catch(Exception ex){if(running)error="Audio unavailable: "+ex.getClass().getSimpleName();}finally{
            synchronized(this){running=false;queue.clear();}
            if(output!=null){try{output.stop();}finally{output.close();}}
            line=null;
        }
    }
    float[] synthesize(String kind){
        if(kind.startsWith("shot_")){try{String[] parts=kind.substring(5).split("#");return ShotAudio.render(Game.Weapon.valueOf(parts[0]),parts.length>1?Integer.parseInt(parts[1]):1);}catch(IllegalArgumentException ex){return new float[1];}}
        double duration=switch(kind){case "ace"->1.6;case "ultimate","focus","orbital","surge"->.9;case "win","lose"->.7;case "boom"->.65;case "reload"->.22;case "scan","heal","plant","defuse"->.38;case "rifle","pistol","shotgun","sniper","revolver","heavy"->.20;case "dash","smoke"->.4;case "multi1","multi2","multi3","multi4","multi5"->.3;default->.13;};
        float[] data=new float[(int)(22050*duration)];Random noise=new Random(kind.hashCode());
        for(int i=0;i<data.length;i++){
            double t=i/22050.,env=Math.exp(-t/(duration*.22)),n=noise.nextDouble()*2-1,v;
            v=switch(kind){
                case "rifle"->(n*.9+Math.sin(t*650)*.6)*env;
                case "pistol"->(n*.6+Math.sin(t*1300)*.5)*env;
                case "suppressed"->(n*.28+Math.sin(t*1900)*.31)*env;
                case "revolver"->(n*.8+Math.sin(t*570)*.83)*env;
                case "smg"->(n*.76+Math.sin(t*1700)*.28)*env;
                case "heavy"->(n*.9+Math.sin(t*330)*.9)*env;
                case "shotgun","sniper"->(n+Math.sin(t*430)*.8)*env;
                case "distant"->n*.20*env;
                case "step"->(n*.10+Math.sin(t*480)*.05)*env;
                case "slash","stab"->n*.38*Math.sin(Math.PI*t/duration)*(kind.equals("stab")?.8:1);
                case "bladehit"->(n*.37+Math.sin(t*740)*.22)*env;
                case "wire"->(Math.sin(t*2*Math.PI*(1300-t*2300))*.34+Math.sin(t*6700)*.1)*env;
                case "bounce","magout","magin","bolt"->(n*.35+Math.sin(t*(kind.equals("bolt")?4300:kind.equals("magin")?2200:7100))*.18)*env;
                case "metal"->(Math.sin(t*6400)+Math.sin(t*8300))*.19*env;
                case "land"->(n*.22+Math.sin(t*330)*.17)*env;
                case "equip"->n*.17*env+Math.sin(t*3700)*.09*env;
                case "reload"->n*.25*env*Math.pow(Math.cos(t*75),8);
                case "boom"->(n*.8+Math.sin(t*190))*env;
                case "hurt"->n*.35*env;
                case "smoke","dash","flash"->n*.22*Math.sin(Math.PI*t/duration);
                case "win"->Math.sin(t*Math.PI*2*(t<.23?523:t<.46?659:784))*.3*Math.sin(Math.PI*t/duration);
                case "lose"->Math.sin(t*Math.PI*2*(t<.3?330:247))*.25*Math.sin(Math.PI*t/duration);
                case "head","kill"->(Math.sin(t*8000)+Math.sin(t*10600))*.18*env;
                case "hit"->Math.sin(t*7100)*.28*env;
                case "beep"->Math.sin(t*5300)*.22*env;
                case "scan","heal","plant","defuse"->Math.sin(t*2*Math.PI*(450+t*1100))*.23*Math.sin(Math.PI*t/duration);
                case "deny"->Math.sin(t*1500)*.22*env;
                case "multi1","multi2","multi3","multi4","multi5"->{int k=kind.charAt(5)-'0';yield (Math.sin(t*2*Math.PI*(440+k*115))+Math.sin(t*2*Math.PI*(660+k*120)))*.13*env;}
                case "ace"->{int note=Math.min(4,(int)(t/.19));double f=new double[]{523,659,784,988,1046}[note];yield (Math.sin(t*2*Math.PI*f)*.22+Math.sin(t*Math.PI*f)*.13)*Math.sin(Math.PI*Math.min(.999,t/duration));}
                case "ultimate","focus"->(Math.sin(t*2*Math.PI*(190+t*800))*.19+Math.sin(t*2*Math.PI*760)*.09)*Math.sin(Math.PI*t/duration);
                case "surge"->(Math.sin(t*2*Math.PI*523)+Math.sin(t*2*Math.PI*659)+Math.sin(t*2*Math.PI*784))*.11*Math.sin(Math.PI*t/duration);
                case "orbital"->(Math.sin(t*2*Math.PI*(450-t*330))*.29+n*.11)*Math.sin(Math.PI*t/duration);
                case "mark","map","select"->Math.sin(t*2*Math.PI*(kind.equals("mark")?1250:kind.equals("map")?740:980))*.23*env;
                default->Math.sin(t*4100)*.2*env;
            };
            data[i]=(float)v;
        }
        return data;
    }
    public void close(){
        SourceDataLine output;
        synchronized(this){running=false;queue.clear();output=line;}
        if(output!=null)output.close();
        if(thread!=null&&thread!=Thread.currentThread()){
            thread.interrupt();
            try{thread.join(1000);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
        }
    }
}
