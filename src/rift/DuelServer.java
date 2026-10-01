package rift;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/** Two-seat TCP server, fixed 60 Hz simulation, one bounded outgoing snapshot per peer. */
final class DuelServer implements AutoCloseable {
    final ServerSocket listener;
    final DuelSimulation simulation;
    final String code;
    final Peer[] peers=new Peer[2];
    final ConcurrentLinkedQueue<Runnable> events=new ConcurrentLinkedQueue<>();
    volatile boolean closed;
    volatile String error="";
    final Thread simulationThread,acceptThread;
    DuelServer(int port,int map,String code,boolean localOnly)throws IOException{
        this.code=code==null?newCode():code.toUpperCase(java.util.Locale.ROOT).trim();
        if(!this.code.matches("[A-Z0-9-]{4,24}"))throw new IOException("Código da sala: 4–24 letras/números");
        listener=new ServerSocket();listener.setReuseAddress(true);
        listener.bind(new InetSocketAddress(localOnly?InetAddress.getLoopbackAddress():null,port),4);
        simulation=new DuelSimulation(map);
        simulationThread=thread("rift-duel-server",this::run);
        acceptThread=thread("rift-duel-accept",this::accept);
    }
    static String newCode(){return java.util.HexFormat.of().formatHex(new SecureRandom().generateSeed(5)).toUpperCase(java.util.Locale.ROOT);}
    static Thread thread(String name,Runnable body){Thread t=new Thread(body,name);t.setDaemon(true);t.start();return t;}
    int port(){return listener.getLocalPort();}
    void accept(){
        while(!closed)try{
            Socket socket=listener.accept();socket.setSoTimeout(4000);socket.setTcpNoDelay(true);
            try{
                DataInputStream in=new DataInputStream(socket.getInputStream());DataOutputStream out=new DataOutputStream(socket.getOutputStream());
                DataInputStream hello=DuelProtocol.receive(in);
                int magic=hello.readInt(),version=hello.readInt();String room=DuelProtocol.text(hello,24),name=DuelProtocol.text(hello,64).strip();int options=hello.readUnsignedByte();
                String rejection=magic!=DuelProtocol.MAGIC||version!=DuelProtocol.VERSION?"Versão incompatível. Usem a mesma atualização.":!code.equals(room)?"Código da sala incorreto.":name.isBlank()?"Informe seu nome.":"";
                if(hello.available()!=0||options>15)rejection="Identificação inválida.";
                int seat=-1;synchronized(peers){for(int j=0;j<2;j++)if(peers[j]==null){seat=j;break;}}
                if(seat<0)rejection="A sala já tem dois jogadores.";
                if(!rejection.isEmpty()){String reason=rejection;DuelProtocol.send(out,DuelProtocol.bytes(o->{o.writeBoolean(false);DuelProtocol.text(o,reason,240);}));socket.close();continue;}
                int index=seat;
                DuelProtocol.send(out,DuelProtocol.bytes(o->{o.writeBoolean(true);o.writeByte(index);o.writeByte(simulation.world.mapIndex-3);}));
                Peer peer=new Peer(socket,in,out,index);synchronized(peers){peers[index]=peer;}
                events.add(()->simulation.connect(index,name,options));peer.start();
            }catch(IOException|RuntimeException ex){socket.close();}
        }catch(IOException ex){if(!closed){error="Falha ao aceitar conexão: "+ex.getMessage();System.err.println(error);}}
    }
    void run(){
        long next=System.nanoTime();
        try{
            while(!closed){
                Runnable event;while((event=events.poll())!=null)event.run();
                DuelProtocol.Command[] commands=new DuelProtocol.Command[2];Peer[] current;
                synchronized(peers){current=peers.clone();}
                for(int i=0;i<2;i++)if(current[i]!=null)commands[i]=current[i].poll();
                simulation.step(commands);
                for(int i=0;i<2;i++)if(current[i]!=null&&!current[i].ended){long echo=commands[i]==null?0:commands[i].sentAt();byte[] state=DuelProtocol.snapshot(simulation,i,echo);current[i].offer(state);}
                next+=1_000_000_000L/60;long wait=next-System.nanoTime();if(wait>0)LockSupport.parkNanos(wait);else if(wait< -250_000_000L)next=System.nanoTime();
            }
        }catch(Throwable ex){error="Servidor interrompido: "+ex;ex.printStackTrace();close();}
        finally{simulation.close();}
    }
    final class Peer {
        final Socket socket;final DataInputStream in;final DataOutputStream out;final int seat;
        final ArrayBlockingQueue<byte[]> snapshots=new ArrayBlockingQueue<>(1);
        DuelProtocol.Command latest;long receivedAt,lastSequence=-1;volatile boolean ended;
        Peer(Socket s,DataInputStream i,DataOutputStream o,int seat){socket=s;in=i;out=o;this.seat=seat;}
        void start(){thread("rift-duel-read-"+seat,this::read);thread("rift-duel-write-"+seat,this::write);}
        synchronized void accept(DuelProtocol.Command c){
            if(c.sequence()<=lastSequence)return;lastSequence=c.sequence();receivedAt=System.nanoTime();
            if(latest!=null)c=new DuelProtocol.Command(c.sequence(),c.sentAt(),c.down(),c.edges()|latest.edges(),c.buttons()|(latest.buttons()&4),c.yaw(),c.pitch(),c.choice(),c.ready()||latest.ready());
            latest=c;
        }
        synchronized DuelProtocol.Command poll(){
            if(latest==null||ended)return null;
            DuelProtocol.Command c=latest;latest=c.held();
            if(System.nanoTime()-receivedAt>350_000_000L)return new DuelProtocol.Command(c.sequence(),c.sentAt(),0,0,0,c.yaw(),c.pitch(),c.choice(),false);
            return c;
        }
        void read(){try{while(!closed&&!ended)accept(DuelProtocol.command(DuelProtocol.receive(in)));}catch(IOException|RuntimeException ex){end();}}
        void offer(byte[] b){if(!snapshots.offer(b)){snapshots.poll();snapshots.offer(b);}}
        void write(){try{while(!closed&&!ended){byte[] b=snapshots.poll(1,TimeUnit.SECONDS);if(b!=null)DuelProtocol.send(out,b);}}catch(IOException|InterruptedException ex){end();}}
        void end(){
            synchronized(this){if(ended)return;ended=true;}
            try{socket.close();}catch(IOException ex){System.err.println("Fechar conexão: "+ex.getMessage());}
            synchronized(peers){if(peers[seat]==this){peers[seat]=null;events.add(()->simulation.disconnect(seat));}}
        }
    }
    @Override public void close(){if(closed)return;closed=true;try{listener.close();}catch(IOException ex){System.err.println(ex.getMessage());}synchronized(peers){for(Peer peer:peers)if(peer!=null)peer.end();}LockSupport.unpark(simulationThread);}
}
