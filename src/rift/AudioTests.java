package rift;

import java.io.*;
import java.util.*;
import static rift.Tests.*;

/** Audio regression coverage without a sound device or a running mixer thread. */
final class AudioTests {
    static void run()throws Exception{decoding();assets();mixing();events();closing();}

    static void decoding()throws Exception{
        short[] mono={0,32767,-32768,16384,-16384};
        float[] decoded=WavAudio.read(new ByteArrayInputStream(wave(22050,1,16,mono)));
        check(decoded.length==mono.length,"WAV PCM mono preserva a quantidade de amostras");
        boolean exact=true;for(int i=0;i<mono.length;i++)exact&=Math.abs(decoded[i]-mono[i]/32768.)<.00004;
        check(exact,"WAV PCM 16-bit preserva sinal, extremos e amplitude");
        short[] stereo={32767,32767,32767,-32768,0,-16384,-32768,-32768};
        decoded=WavAudio.read(new ByteArrayInputStream(wave(22050,2,16,stereo)));
        check(decoded.length==stereo.length/2,"WAV stereo e convertido para o banco mono");
        check(Math.abs(decoded[0]-32767/32768.)<.00004&&Math.abs(decoded[1])<.00004&&Math.abs(decoded[2]+.25)<.00004&&Math.abs(decoded[3]+1)<.00004,"Conversao stereo considera os dois canais sem overflow");
        decoded=WavAudio.read(new ByteArrayInputStream(waveBytes(22050,1,8,1,new byte[]{0,(byte)128,(byte)255})));
        check(decoded.length==3&&decoded[0]<-.99&&Math.abs(decoded[1])<.00004&&decoded[2]>.98,"WAV 8-bit unsigned e convertido sem deslocamento DC");
        ByteArrayOutputStream encoded=new ByteArrayOutputStream();for(int value:new int[]{-8388608,0,8388607})integer(encoded,value,3);
        decoded=WavAudio.read(new ByteArrayInputStream(waveBytes(22050,1,24,1,encoded.toByteArray())));
        check(decoded.length==3&&decoded[0]<-.99&&Math.abs(decoded[1])<.00004&&decoded[2]>.99,"WAV PCM 24-bit e reduzido para o formato do mixer");
        encoded=new ByteArrayOutputStream();for(int value:new int[]{Integer.MIN_VALUE,0,Integer.MAX_VALUE})integer(encoded,value,4);
        decoded=WavAudio.read(new ByteArrayInputStream(waveBytes(22050,1,32,1,encoded.toByteArray())));
        check(decoded.length==3&&decoded[0]<-.99&&Math.abs(decoded[1])<.00004&&decoded[2]>.99,"WAV PCM 32-bit preserva sinal e amplitude");
        encoded=new ByteArrayOutputStream();for(float value:new float[]{-.5f,0,.5f})integer(encoded,Float.floatToIntBits(value),4);
        decoded=WavAudio.read(new ByteArrayInputStream(waveBytes(22050,1,32,3,encoded.toByteArray())));
        check(decoded.length==3&&Math.abs(decoded[0]+.5)<.0001&&Math.abs(decoded[1])<.0001&&Math.abs(decoded[2]-.5)<.0001,"WAV float32 e convertido para PCM limitado");
        for(int rate:new int[]{8000,11025,44100,48000,96000}){
            short[] tone=new short[rate/10];for(int i=0;i<tone.length;i++)tone[i]=(short)(Math.sin(i*2*Math.PI*440/rate)*16000);
            decoded=WavAudio.read(new ByteArrayInputStream(wave(rate,1,16,tone)));double energy=0;for(float value:decoded)energy+=value*value;
            check(Math.abs(decoded.length-2205)<12&&energy/decoded.length>.01,"Resampling preserva duracao e energia de WAV "+rate+" Hz");
        }
        byte[] valid=wave(22050,1,16,mono),invalid=valid.clone();invalid[0]='X';
        ByteArrayOutputStream metadata=new ByteArrayOutputStream();metadata.write(valid,0,4);integer(metadata,valid.length+4,4);metadata.write(valid,8,4);
        metadata.write(new byte[]{'J','U','N','K'});integer(metadata,3,4);metadata.write(new byte[]{1,2,3,0});metadata.write(valid,12,valid.length-12);
        decoded=WavAudio.read(new ByteArrayInputStream(metadata.toByteArray()));
        check(decoded.length==mono.length&&Math.abs(decoded[3]-.5)<.00004,"WAV aceita metadata adicional com padding de chunks");
        rejected(invalid,"Cabecalho WAV invalido e rejeitado");
        rejected(Arrays.copyOf(valid,valid.length-3),"WAV truncado nao retorna amostras parciais");
        rejected(new byte[0],"WAV vazio e rejeitado");
        rejected(new byte[WavAudio.MAX_FILE_BYTES+1],"WAV acima do limite de bytes e rejeitado antes de decodificar");
        rejected(wave(22050,1,16,new short[0]),"WAV sem frames e rejeitado");
        rejected(wave(22050,2,16,new short[]{1,2,3}),"Dados WAV incompletos para frame stereo sao rejeitados");
        rejected(wave(22050,3,16,new short[]{1,2,3}),"WAV multicanal nao suportado e rejeitado");
        rejected(wave(0,1,16,mono),"Taxa WAV zero e rejeitada");
        rejected(wave(7999,1,16,mono),"Taxa WAV abaixo de 8000 Hz e rejeitada");
        rejected(wave(96001,1,16,mono),"Taxa WAV acima do limite suportado e rejeitada");
        rejected(wave(22050,1,16,new short[WavAudio.MAX_FRAMES+1]),"WAV acima do limite de duracao e rejeitado");
        for(int rate:new int[]{8000,11025,22050,44100,48000,96000}){
            decoded=WavAudio.read(new ByteArrayInputStream(wave(rate,1,16,new short[rate*2])));
            check(decoded.length==WavAudio.MAX_FRAMES,"WAV de exatamente dois segundos respeita o limite apos resampling: "+rate+" Hz");
            rejected(wave(rate,1,16,new short[rate*2+1]),"WAV acima de dois segundos por um frame e rejeitado: "+rate+" Hz");
        }
        for(String key:new String[]{"../shot_ECHO#0","shot_ECHO#0/../../foo","C:\\Windows\\win.ini",""})check(WavAudio.load(key)==null,"Chave WAV insegura ou vazia nao acessa arquivos: "+key);
        check(WavAudio.load("sample_sem_arquivo")==null,"WAV ausente permite alternativa sintetizada");
        check(WavAudio.load(null)==null,"Chave WAV nula e tratada sem falha");
    }

    static void assets()throws Exception{
        Settings settings=new Settings(false);
        try(AudioEngine engine=new AudioEngine(settings,false)){
            Set<String> keys=new HashSet<>();Set<Integer> shotHashes=new HashSet<>();
            for(String key:AudioAssets.keys()){
                check(keys.add(key),"Chave do banco WAV e unica: "+key);
                float[] sample=WavAudio.load(key);
                check(sample!=null,"WAV original esta empacotado: "+key);
                if(sample==null)continue;
                boolean finite=true;double sum=0,peak=0;
                for(float value:sample){finite&=Float.isFinite(value);sum+=value*value;peak=Math.max(peak,Math.abs(value));}
                check(sample.length>0&&sample.length<=WavAudio.MAX_FRAMES&&finite&&peak<=1,"WAV possui duracao limitada e amplitudes validas: "+key);
                check(sample[0]==0&&sample[sample.length-1]==0,"WAV inicia e termina no repouso sem clique: "+key);
                check(sum/sample.length>1e-7,"WAV nao e silencioso: "+key);
                check(Arrays.equals(sample,engine.sample(key)),"Motor prefere o WAV empacotado: "+key);
                check(engine.sample(key)==engine.sample(key),"Banco reutiliza a mesma amostra em cache: "+key);
                if(key.startsWith("shot_"))shotHashes.add(Arrays.hashCode(sample));
            }
            check(!keys.isEmpty(),"Banco WAV possui amostras originais");
            check(shotHashes.size()>=Game.Weapon.values().length*3,"Cada arma tem tres variacoes sonoras distintas");
            float[] fallback=engine.sample("sample_sem_arquivo");double energy=0;for(float value:fallback)energy+=value*value;
            check(fallback.length>0&&energy>0,"Efeito sem WAV usa sintese como alternativa");
            check(fallback==engine.sample("sample_sem_arquivo"),"Alternativa sintetizada tambem e reutilizada");
            check(!engine.running&&engine.thread==null&&engine.line==null,"Testes do banco nao abrem placa de som nem thread");
        }
    }

    static void mixing(){
        Settings settings=new Settings(false);settings.volume=1;settings.effectsVolume=1;settings.musicVolume=0;
        float[] constant=new float[4];Arrays.fill(constant,.5f);
        byte[] left=mix(settings,constant,-1,1,false),right=mix(settings,constant,1,1,false),center=mix(settings,constant,0,1,false);
        check(channel(left,0,0)>0&&channel(left,0,1)==0,"Pan esquerda nao vaza para o canal direito");
        check(channel(right,0,0)==0&&channel(right,0,1)>0,"Pan direita nao vaza para o canal esquerdo");
        check(channel(center,0,0)==channel(center,0,1)&&channel(center,0,0)>0,"Pan central usa ganhos iguais nos dois canais");
        settings.volume=.5;byte[] quieter=mix(settings,constant,0,1,false);
        check(Math.abs(channel(quieter,0,0)-channel(center,0,0)*.5)<=1,"Volume geral escala os efeitos");
        settings.volume=0;check(silent(mix(settings,constant,0,1,false)),"Volume zero silencia ambos os canais");
        settings.volume=1;settings.sound=false;check(silent(mix(settings,constant,0,1,true)),"Audio desativado silencia efeitos e musica");
        List<AudioEngine.Voice> muted=new ArrayList<>();muted.add(new AudioEngine.Voice(constant,0,1));byte[] mutedBytes=new byte[constant.length*4];
        AudioEngine.mixBlock(muted,mutedBytes,0,false,settings);check(muted.isEmpty(),"Vozes silenciadas sao consumidas sem retomar som antigo");
        settings.sound=true;settings.effectsVolume=0;check(silent(mix(settings,constant,0,1,false)),"Volume de efeitos zero preserva silencio fora do menu");
        settings.effectsVolume=1;settings.musicVolume=1;
        byte[] music=new byte[64];AudioEngine.mixBlock(new ArrayList<>(),music,100,true,settings);
        check(!silent(music),"Musica do menu independe de vozes de efeitos");
        Arrays.fill(music,(byte)0);AudioEngine.mixBlock(new ArrayList<>(),music,100,false,settings);
        check(silent(music),"Musica do menu nao toca durante a partida");
        settings.musicVolume=0;
        List<AudioEngine.Voice> voices=new ArrayList<>();for(int i=0;i<20;i++)voices.add(new AudioEngine.Voice(constant,0,100));
        byte[] saturated=new byte[16];AudioEngine.mixBlock(voices,saturated,0,false,settings);boolean bounded=true;
        for(int i=0;i<saturated.length/4;i++)for(int c=0;c<2;c++)bounded&=Math.abs(channel(saturated,i,c))<=21000;
        check(bounded&&!silent(saturated),"Mistura de vinte vozes satura sem overflow PCM");
        voices=new ArrayList<>();AudioEngine.Voice voice=new AudioEngine.Voice(new float[]{.5f,.25f},0,1);voices.add(voice);
        byte[] finished=new byte[16];long clock=AudioEngine.mixBlock(voices,finished,73,false,settings);
        check(clock==77&&voice.at==2,"Relogio avanca por frame sem reler amostra encerrada");
        check(channel(finished,2,0)==0&&channel(finished,3,1)==0,"Cauda do bloco fica silenciosa apos a voz terminar");
        check(voices.isEmpty(),"Mixer remove vozes encerradas apos o bloco");
        AudioEngine.Voice invalid=new AudioEngine.Voice(constant,Double.NaN,Double.NaN);
        check(Double.isFinite(invalid.pan)&&Double.isFinite(invalid.gain)&&invalid.pan>=-1&&invalid.pan<=1&&invalid.gain>=0&&invalid.gain<=2,"Voz limita parametros nao finitos antes da mistura");
        voices=new ArrayList<>();voices.add(new AudioEngine.Voice(new float[]{Float.NaN,Float.POSITIVE_INFINITY,.2f},0,1));voices.add(new AudioEngine.Voice(new float[]{.25f,.25f,.25f},0,1));
        byte[] repaired=new byte[12];AudioEngine.mixBlock(voices,repaired,0,false,settings);
        check(channel(repaired,0,0)>0&&channel(repaired,1,0)>0,"Amostra nao finita nao silencia as outras vozes");
        try{AudioEngine.mixBlock(new ArrayList<>(),new byte[3],0,false,settings);check(false,"Mixer rejeita buffer stereo com frame incompleto");}catch(IllegalArgumentException expected){check(true,"Mixer rejeita buffer stereo com frame incompleto");}
    }

    static void events()throws Exception{
        Settings settings=new Settings(false);settings.volume=1;
        try(AudioEngine engine=new AudioEngine(settings,false)){
            engine.play("shot_ECHO");check(engine.queue.isEmpty(),"Audio sem dispositivo nao acumula eventos");
            check(engine.caption.contains("DISPARO")&&engine.captionAt>0,"Legenda de disparo funciona sem hardware");
            engine.running=true;
            engine.playAt(null,0,1);engine.playAt("step_concrete",Double.NaN,1);engine.playAt("step_concrete",Double.POSITIVE_INFINITY,1);engine.playAt("step_concrete",0,Double.NaN);engine.playAt("step_concrete",0,Double.NEGATIVE_INFINITY);engine.playAt("step_concrete",0,0);
            check(engine.queue.isEmpty(),"Eventos invalidos nao entram na fila nem no mixer");
            for(int i=0;i<120;i++)engine.playAt("shot_ECHO",i%2==0?-5:5,1);
            check(engine.queue.size()==AudioEngine.MAX_PENDING,"Fila de efeitos permanece limitada em rajadas");
            boolean safe=true;Set<String> variants=new HashSet<>();AudioEngine.Sound sound;
            while((sound=engine.queue.poll())!=null){safe&=Double.isFinite(sound.pan())&&sound.pan()>=-1&&sound.pan()<=1&&Double.isFinite(sound.gain())&&sound.gain()>=0&&sound.gain()<=2;variants.add(sound.name());}
            check(safe,"Eventos limitam pan ao intervalo stereo");
            check(variants.size()==3,"Disparos alternam tres variacoes sem criar chaves ilimitadas");
            List<AudioEngine.Voice> voices=new ArrayList<>();float[] sample={.1f};AudioEngine.Voice expired=new AudioEngine.Voice(sample,0,1);expired.at=sample.length;voices.add(expired);
            for(int i=0;i<AudioEngine.MAX_PENDING;i++)engine.play("shot_ECHO");engine.drain(voices);
            check(voices.size()==AudioEngine.MAX_VOICES&&!voices.contains(expired),"Fila substitui vozes encerradas e limita a polifonia");
            check(engine.queue.isEmpty(),"Efeitos descartados por saturacao nao viram backlog de sons antigos");
            Thread[] producers=new Thread[4];for(int i=0;i<producers.length;i++){producers[i]=new Thread(()->{for(int j=0;j<250;j++)engine.play("shot_ECHO");});producers[i].start();}for(Thread producer:producers)producer.join();
            check(engine.queue.size()==AudioEngine.MAX_PENDING,"Fila concorrente mantem o limite durante eventos simultaneos");engine.queue.clear();
            settings.sound=false;engine.play("reload");check(engine.queue.isEmpty(),"Audio desligado nao enfileira efeitos");
            check(!engine.caption.isEmpty(),"Legendas continuam disponiveis com audio desligado");
            settings.sound=true;settings.volume=0;engine.play("shot_ECHO");check(engine.queue.isEmpty(),"Volume zero nao enfileira efeitos");
            for(String key:AudioAssets.keys())if(key.startsWith("step_")||key.startsWith("land_")||key.startsWith("impact_"))check(!AudioEngine.caption(key).isEmpty(),"Efeito de superficie oferece legenda: "+key);
            settings.volume=1;engine.play("shot_ECHO");engine.close();check(engine.queue.isEmpty()&&!engine.running&&engine.thread==null&&engine.line==null,"Fechar motor descarta eventos pendentes sem abrir hardware");
        }
    }

    static void closing()throws Exception{
        Settings settings=new Settings(false);settings.volume=1;
        try(AudioEngine engine=new AudioEngine(settings,false)){
            Thread producer=new Thread(()->engine.play("shot_ECHO"),"audio-test-producer");boolean blocked;
            synchronized(engine){
                engine.running=true;producer.start();long deadline=System.nanoTime()+1_000_000_000L;
                while(producer.getState()!=Thread.State.BLOCKED&&System.nanoTime()<deadline)Thread.sleep(1);
                blocked=producer.getState()==Thread.State.BLOCKED;engine.close();
            }
            producer.join(1000);
            check(blocked,"Produtor de audio aguarda monitor durante teste concorrente de fechamento");
            check(!producer.isAlive()&&engine.queue.isEmpty(),"Fechamento concorrente impede evento posterior a limpeza da fila");
        }
    }

    static byte[] mix(Settings settings,float[] samples,double pan,double gain,boolean menu){
        byte[] bytes=new byte[samples.length*4];List<AudioEngine.Voice> voices=new ArrayList<>();voices.add(new AudioEngine.Voice(samples,pan,gain));AudioEngine.mixBlock(voices,bytes,0,menu,settings);return bytes;
    }
    static int channel(byte[] pcm,int frame,int channel){int at=frame*4+channel*2;return (short)((pcm[at]&255)|(pcm[at+1]&255)<<8);}
    static boolean silent(byte[] pcm){for(byte value:pcm)if(value!=0)return false;return true;}
    static void rejected(byte[] data,String label){try{WavAudio.read(new ByteArrayInputStream(data));check(false,label);}catch(IOException expected){check(true,label);}}

    static byte[] wave(int rate,int channels,int bits,short[] samples)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();for(short value:samples)integer(out,value,2);return waveBytes(rate,channels,bits,1,out.toByteArray());
    }
    static byte[] waveBytes(int rate,int channels,int bits,int format,byte[] data)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(new byte[]{'R','I','F','F'});integer(out,36+data.length+(data.length&1),4);out.write(new byte[]{'W','A','V','E','f','m','t',' '});
        integer(out,16,4);integer(out,format,2);integer(out,channels,2);integer(out,rate,4);integer(out,rate*channels*bits/8,4);integer(out,channels*bits/8,2);integer(out,bits,2);
        out.write(new byte[]{'d','a','t','a'});integer(out,data.length,4);out.write(data);if((data.length&1)!=0)out.write(0);return out.toByteArray();
    }
    static void integer(OutputStream out,int value,int bytes)throws IOException{for(int i=0;i<bytes;i++)out.write(value>>>(i*8)&255);}
}
