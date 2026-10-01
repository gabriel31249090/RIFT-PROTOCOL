package rift;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Private child-process pipe; Windows device events never become a shared desktop cursor. */
final class RawInputHub implements AutoCloseable {
    final Input[] inputs={new Input(),new Input()};
    final long[] sequence={-1,-1};
    volatile boolean ready,closed,stopped;
    volatile long heartbeat=System.nanoTime();
    volatile String status="Cadastre o teclado e mouse de cada jogador na janela RIFT / Dispositivos";
    final String[] labels={"J1: aguardando dispositivos","J2: aguardando dispositivos"};
    Process process;
    RawInputHub(){}
    void launch()throws Exception{
        if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"))throw new IOException("Dois teclados + dois mouses no mesmo PC exigem Windows. A rede funciona também em Linux/macOS.");
        byte[] source;try(InputStream in=RawInputHub.class.getResourceAsStream("/assets/native/RawInputBridge.cs")){if(in==null)throw new IOException("Ponte de dispositivos ausente do JAR");source=in.readAllBytes();}
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source)).substring(0,16);
        Path cache=Path.of(System.getProperty("user.home"),".rift-protocol","native",hash);Files.createDirectories(cache);
        Path cs=cache.resolve("RawInputBridge.cs"),exe=cache.resolve("RiftDevices.exe");try(OutputStream output=Files.newOutputStream(cs)){output.write(new byte[]{(byte)0xef,(byte)0xbb,(byte)0xbf});output.write(source);}
        if(!Files.isRegularFile(exe)){
            String win=System.getenv("WINDIR");if(win==null)throw new IOException("Windows não identificado");
            Path compiler=Path.of(win,"Microsoft.NET","Framework64","v4.0.30319","csc.exe");
            if(!Files.isRegularFile(compiler))compiler=Path.of(win,"Microsoft.NET","Framework","v4.0.30319","csc.exe");
            if(!Files.isRegularFile(compiler))throw new IOException("O compilador do .NET Framework 4.x não foi encontrado. Instale/repare o .NET Framework do Windows para usar dois conjuntos de dispositivos.");
            Path log=cache.resolve("build.log"),temporary=cache.resolve("RiftDevices.build.exe");
            Process compile=new ProcessBuilder(compiler.toString(),"/nologo","/target:exe","/optimize+","/out:"+temporary,"/reference:System.Windows.Forms.dll","/reference:System.Drawing.dll",cs.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if(!compile.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)){compile.destroyForcibly();throw new IOException("Tempo excedido ao preparar dispositivos. Consulte "+log);}
            if(compile.exitValue()!=0)throw new IOException("Falha ao preparar dispositivos. Consulte "+log+"\n"+Files.readString(log));
            Files.move(temporary,exe,StandardCopyOption.REPLACE_EXISTING);
        }
        if(closed)return;
        process=new ProcessBuilder(exe.toString(),Long.toString(ProcessHandle.current().pid())).redirectErrorStream(true).start();
        DuelServer.thread("rift-raw-input",()->{
            try(BufferedReader r=process.inputReader(StandardCharsets.UTF_8)){String line;while(!closed&&(line=r.readLine())!=null)accept(line);}
            catch(IOException ex){if(!closed)status="Dispositivos: "+ex.getMessage();}
            finally{stopped=true;ready=false;clear();if(!closed)status="A janela de dispositivos foi fechada. Feche as duas telas e abra o modo local novamente.";}
        });
    }
    synchronized void accept(String line){
        if(line.startsWith("STATUS ")){status=line.substring(7);heartbeat=System.nanoTime();return;}
        if(line.startsWith("LABEL ")){int seat=line.charAt(6)-'0';if(seat>=0&&seat<2)labels[seat]=line.substring(8);return;}
        if(line.equals("CLEAR")){ready=false;clear();heartbeat=System.nanoTime();return;}
        if(line.equals("READY")){ready=true;heartbeat=System.nanoTime();return;}
        if(line.equals("PING")){heartbeat=System.nanoTime();return;}
        if(!line.startsWith("S ")||line.length()>512)return;
        try{
            String[] p=line.split(" ");if(p.length!=8)return;
            long seq=Long.parseLong(p[1]);int seat=Integer.parseInt(p[2]);
            if(seat<0||seat>1||seq<=sequence[seat]||p[3].length()!=64||p[4].length()!=64)return;
            int buttons=Integer.parseInt(p[5]);double dx=Double.parseDouble(p[6]),dy=Double.parseDouble(p[7]);
            if(!Double.isFinite(dx)||!Double.isFinite(dy)||Math.abs(dx)>100000||Math.abs(dy)>100000||(buttons&~7)!=0)return;
            BitSet down=decode(p[3]),edges=decode(p[4]);sequence[seat]=seq;
            inputs[seat].merge(new Input.Frame(down,edges,(buttons&1)!=0,(buttons&2)!=0,(buttons&4)!=0,dx,dy,0,0));heartbeat=System.nanoTime();
        }catch(IllegalArgumentException ex){status="Pacote de dispositivo inválido; movimento descartado.";}
    }
    static BitSet decode(String hex){BitSet b=new BitSet(256);for(int i=0;i<64;i++){int n=Character.digit(hex.charAt(i),16);if(n<0)throw new IllegalArgumentException();for(int j=0;j<4;j++)if((n&(1<<j))!=0)b.set(javaKey(i*4+j));}return b;}
    static int javaKey(int vk){return switch(vk){
        case 13->10; case 44->154; case 45->155; case 46->127;
        case 91,92->524; case 93->525;
        case 186->59; case 187->61; case 188->44; case 189->45; case 190->46; case 191->47;
        case 219->91; case 220,226->92; case 221->93; default->vk;
    };}
    Input.Frame poll(int seat){if(!ready||System.nanoTime()-heartbeat>500_000_000L){inputs[seat].clear();return Input.Frame.empty();}return inputs[seat].poll();}
    void clear(){for(Input in:inputs)in.clear();}
    @Override public void close(){closed=true;ready=false;clear();if(process!=null){try{process.outputWriter(StandardCharsets.UTF_8).append("STOP\n").flush();}catch(IOException ex){System.err.println("Fechar ponte: "+ex.getMessage());}DuelServer.thread("rift-raw-close",()->{try{if(!process.waitFor(2,java.util.concurrent.TimeUnit.SECONDS))process.destroyForcibly();}catch(InterruptedException ex){Thread.currentThread().interrupt();process.destroyForcibly();}});}}
}
