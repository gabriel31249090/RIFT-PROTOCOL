package rift;

import java.io.*;
import java.nio.*;
import javax.sound.sampled.*;

/** Bounded Java Sound decoding to the mixer's mono PCM format. */
final class WavAudio {
    static final int RATE=22050,MAX_FRAMES=RATE*2,MAX_FILE_BYTES=2*1024*1024;
    static final AudioFormat FORMAT=new AudioFormat(RATE,16,1,true,false);

    static float[] load(String key){
        if(key==null||key.length()>80||!key.matches("[A-Za-z][A-Za-z0-9_]*(?:#[0-2])?"))return null;
        String path="/assets/audio/"+key.replace('#','-')+".wav";
        try(InputStream in=WavAudio.class.getResourceAsStream(path)){
            return in==null?null:read(in);
        }catch(IOException|RuntimeException ex){return null;}
    }

    static float[] read(InputStream in)throws IOException{
        byte[] file=in.readNBytes(MAX_FILE_BYTES+1);
        if(file.length>MAX_FILE_BYTES)throw new IOException("WAV exceeds file limit");
        int dataLength=dataLength(file);
        try(AudioInputStream source=AudioSystem.getAudioInputStream(new ByteArrayInputStream(file))){
            AudioFormat f=source.getFormat();float rate=f.getSampleRate();int channels=f.getChannels(),bits=f.getSampleSizeInBits(),frameSize=f.getFrameSize();
            boolean pcm=f.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)||f.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)
                ||f.getEncoding().equals(AudioFormat.Encoding.PCM_FLOAT);
            if(!pcm||!Float.isFinite(rate)||rate<8000||rate>96000||channels<1||channels>2
                ||(bits!=8&&bits!=16&&bits!=24&&bits!=32)||frameSize!=channels*(bits/8))throw new IOException("Unsupported WAV format");
            long frames=source.getFrameLength();int limit=(int)(rate*2)*frameSize;
            if(frames>rate*2||dataLength>limit)throw new IOException("WAV exceeds duration limit");
            if(dataLength==0||dataLength%frameSize!=0)throw new IOException("Incomplete WAV frame");
            byte[] original=source.readNBytes(limit+frameSize);
            if(original.length!=dataLength||original.length>limit||original.length%frameSize!=0
                ||frames>=0&&frames!=original.length/frameSize)throw new IOException("Truncated or oversized WAV data");
            if(!AudioSystem.isConversionSupported(FORMAT,f))throw new IOException("WAV conversion unavailable");
            try(AudioInputStream verified=new AudioInputStream(new ByteArrayInputStream(original),f,original.length/frameSize);
                AudioInputStream decoded=AudioSystem.getAudioInputStream(FORMAT,verified)){
                int expected=Math.max(1,(int)Math.round(original.length/(double)frameSize*RATE/rate));
                byte[] pcmBytes=decoded.readNBytes((MAX_FRAMES+16)*2);
                if(pcmBytes.length==0||pcmBytes.length%2!=0||pcmBytes.length/2>expected+16
                    ||pcmBytes.length/2<expected-4)throw new IOException("Invalid decoded WAV length");
                // Resamplers can append filter padding; duration follows the source frames.
                float[] samples=new float[Math.min(expected,pcmBytes.length/2)];
                for(int i=0;i<samples.length;i++)samples[i]=(short)((pcmBytes[i*2]&255)|(pcmBytes[i*2+1]<<8))/32768f;
                return samples;
            }
        }catch(UnsupportedAudioFileException|IllegalArgumentException ex){throw new IOException("Invalid WAV",ex);}
    }

    // Java Sound rounds the data chunk down to whole frames; verify its byte boundary first.
    static int dataLength(byte[] file)throws IOException{
        if(file.length<12)throw new IOException("Missing WAV header");
        ByteBuffer chunks=ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        if(chunks.getInt(0)!=0x46464952||chunks.getInt(8)!=0x45564157)throw new IOException("Expected RIFF WAVE");
        long end=8+Integer.toUnsignedLong(chunks.getInt(4));
        if(end<12||end>file.length)throw new IOException("Truncated WAV container");
        int data=-1;
        for(int at=12;at<end;){
            if(end-at<8)throw new IOException("Truncated WAV chunk header");
            long length=Integer.toUnsignedLong(chunks.getInt(at+4)),next=at+8+length+(length&1);
            if(next>end)throw new IOException("Truncated WAV chunk");
            if(chunks.getInt(at)==0x61746164){if(data>=0)throw new IOException("Duplicate WAV data");data=(int)length;}
            at=(int)next;
        }
        if(data<0)throw new IOException("Missing WAV data");
        return data;
    }
}
