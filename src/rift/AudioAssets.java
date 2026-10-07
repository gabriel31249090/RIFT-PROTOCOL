package rift;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import static rift.Game.*;

/** Reproducible, original RIFT samples; no recordings or external game assets. */
final class AudioAssets {
    static final int SAMPLE_RATE=22050;
    private static final String[] SURFACES={"concrete","wood","metal"};
    private static final String[] CATEGORIES={"pistol","smg","rifle","shotgun","precision","heavy"};
    private static final List<String> BASES=bases();
    private static final List<String> KEYS=sampleKeys();

    private static List<String> bases(){
        List<String> bases=new ArrayList<>();
        for(Weapon w:Weapon.values())bases.add(w.sound());
        for(String event:List.of("step","land","impact"))for(String surface:SURFACES)bases.add(event+"_"+surface);
        for(String event:List.of("reload","magout","magin","bolt"))for(String category:CATEGORIES)bases.add(event+"_"+category);
        return List.copyOf(bases);
    }
    private static List<String> sampleKeys(){
        List<String> keys=new ArrayList<>();
        for(String base:BASES)for(int variant=0;variant<3;variant++)keys.add(base+"#"+variant);
        return List.copyOf(keys);
    }
    static List<String> keys(){return KEYS;}
    static boolean varied(String base){return BASES.contains(base);}

    static float[] render(String key){
        if(key==null)return null;
        int split=key.lastIndexOf('#'),variant=0;
        String base=split<0?key:key.substring(0,split);
        if(!varied(base))return null;
        if(split>=0){
            if(key.length()!=split+2||key.charAt(split+1)<'0'||key.charAt(split+1)>'2')return null;
            variant=key.charAt(split+1)-'0';
        }
        float[] samples;
        if(base.startsWith("shot_"))samples=ShotAudio.render(Weapon.valueOf(base.substring(5)),variant);
        else if(base.startsWith("step_")||base.startsWith("land_")||base.startsWith("impact_"))samples=surface(base,variant);
        else samples=mechanism(base,variant);
        finish(samples);
        return samples;
    }

    private static float[] surface(String base,int variant){
        int split=base.indexOf('_');String event=base.substring(0,split),material=base.substring(split+1);
        boolean landing=event.equals("land"),impact=event.equals("impact"),metal=material.equals("metal"),wood=material.equals("wood");
        double duration=impact?(metal?.42:.28):landing?(metal?.48:.36):(metal?.30:.23);
        double shift=1+(variant-1)*.045,weight=landing?.86:impact?.68:.58;
        float[] out=new float[(int)(SAMPLE_RATE*duration)];Random random=new Random(0x52494654L+base.hashCode()*37L+variant*101L);
        double low=0,previous=0;
        for(int i=0;i<out.length;i++){
            double t=i/(double)SAMPLE_RATE,noise=random.nextDouble()*2-1;
            low=low*.88+noise*.12;double high=noise-previous;previous=noise;
            double body=tone(t,(metal?112:wood?145:91)*shift,landing?.082:.043)*.56;
            double contact=low*Math.exp(-t/(landing?.051:.024))*(impact?.86:.62);
            double onset=high*Math.exp(-t/(impact?.0045:.009))*(metal?.24:wood?.16:.11);
            double resonance;
            if(metal)resonance=tone(t,617*shift,.067)*.19+tone(t,1237*shift,.055)*.10+tone(t,1979*shift,.040)*.045;
            else if(wood)resonance=tone(t,267*shift,.040)*.22+tone(t,431*shift,.025)*.10;
            else resonance=low*Math.exp(-t/.071)*.20;
            double scrape=envelope(t,landing?.072:.047,.040)*low*(impact?.03:metal?.14:.28);
            double settle=landing?envelope(t,.086,.024)*(low*.36+Math.sin(2*Math.PI*183*shift*t)*.12):0;
            out[i]=(float)Math.tanh((body+contact+onset+resonance+scrape+settle)*weight);
        }
        return out;
    }

    private static float[] mechanism(String base,int variant){
        int split=base.indexOf('_');String event=base.substring(0,split),category=base.substring(split+1);
        int group=List.of(CATEGORIES).indexOf(category);
        double shift=(1+(variant-1)*.037)*(group==0?1.20:group==1?1.11:group==4?.87:group==5?.77:1);
        double duration=event.equals("reload")?.12:event.equals("bolt")?.34:.28;
        float[] out=new float[(int)(SAMPLE_RATE*duration)];Random random=new Random(0x4D454348L+base.hashCode()*43L+variant*137L);
        double low=0,previous=0;
        for(int i=0;i<out.length;i++){
            double t=i/(double)SAMPLE_RATE,noise=random.nextDouble()*2-1;low=low*.76+noise*.24;double high=noise-previous;previous=noise;
            double value=switch(event){
                case "magout"->release(t,shift,low,high);
                case "magin"->insert(t,shift,low,high);
                case "bolt"->bolt(t,shift,low,high);
                default->envelope(t,.008,.025)*(low*.24+Math.sin(2*Math.PI*233*shift*t)*.12);
            };
            out[i]=(float)Math.tanh(value*(group==5?.85:group==0?.60:.72));
        }
        return out;
    }
    private static double release(double t,double pitch,double low,double high){
        if(t<0)return 0;
        return click(t,.006,pitch,high,.34)+click(t,.070,pitch*.89,high,.19)+envelope(t,.022,.053)*low*.37;
    }
    private static double insert(double t,double pitch,double low,double high){
        if(t<0)return 0;
        return click(t,.010,pitch*.84,high,.56)+click(t,.055,pitch*1.22,high,.23)+envelope(t,.012,.024)*low*.52;
    }
    private static double bolt(double t,double pitch,double low,double high){
        if(t<0)return 0;
        return click(t,.007,pitch*1.28,high,.28)+click(t,.137,pitch*.82,high,.54)+envelope(t,.027,.062)*(low*.35+Math.sin(2*Math.PI*740*pitch*t)*.032);
    }
    private static double click(double t,double at,double pitch,double noise,double weight){
        double age=t-at;if(age<0)return 0;
        return envelope(t,at,.011)*(noise*.46+Math.sin(2*Math.PI*1620*pitch*age)*.33+Math.sin(2*Math.PI*314*pitch*age)*.42)*weight;
    }
    private static double envelope(double t,double at,double decay){
        double age=t-at;return age<0?0:Math.min(1,age/.0012)*Math.exp(-age/decay);
    }
    private static double tone(double t,double frequency,double decay){return Math.sin(2*Math.PI*frequency*t)*Math.exp(-t/decay);}

    private static void finish(float[] samples){
        double mean=0;for(float sample:samples)mean+=sample;mean/=samples.length;
        int fadeIn=Math.max(1,SAMPLE_RATE/2000),fadeOut=Math.max(1,SAMPLE_RATE/200);double peak=0;
        for(int i=0;i<samples.length;i++){
            double fade=Math.min(1,Math.min(i/(double)fadeIn,(samples.length-1-i)/(double)fadeOut));
            samples[i]=(float)((samples[i]-mean)*fade);peak=Math.max(peak,Math.abs(samples[i]));
        }
        if(peak>.94)for(int i=0;i<samples.length;i++)samples[i]*=(float)(.94/peak);
    }

    static void generate(Path directory)throws IOException{
        Files.createDirectories(directory);
        AudioFormat format=new AudioFormat(SAMPLE_RATE,16,1,true,false);
        List<String> hashes=new ArrayList<>();
        MessageDigest digest;
        try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        for(String key:KEYS){
            float[] samples=render(key);byte[] pcm=new byte[samples.length*2];
            for(int i=0;i<samples.length;i++){int value=Math.round(samples[i]*32767);pcm[i*2]=(byte)value;pcm[i*2+1]=(byte)(value>>>8);}
            Path path=directory.resolve(key.replace('#','-')+".wav");
            try(AudioInputStream stream=new AudioInputStream(new ByteArrayInputStream(pcm),format,samples.length)){
                if(AudioSystem.write(stream,AudioFileFormat.Type.WAVE,path.toFile())<=0)throw new IOException("Cannot write "+path);
            }
            hashes.add(HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)))+"  "+path.getFileName());
        }
        Files.writeString(directory.resolve("SHA256SUMS.txt"),String.join("\n",hashes)+"\n");
    }
    public static void main(String[] args)throws IOException{
        if(args.length>1)throw new IllegalArgumentException("Usage: rift.AudioAssets [assets/audio]");
        Path directory=Path.of(args.length==0?"assets/audio":args[0]);generate(directory);
        System.out.println("Generated "+KEYS.size()+" original WAV samples in "+directory.toAbsolutePath());
    }
}
