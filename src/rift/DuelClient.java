package rift;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/** Network transport independent of rendering; used by loopback tests and both local seats. */
final class DuelClient implements AutoCloseable {
    final Socket socket;
    final DataInputStream in;final DataOutputStream out;
    final int seat,map;
    final AtomicReference<byte[]> snapshot=new AtomicReference<>();
    volatile boolean closed;
    volatile String error="";
    volatile long lastReceived;
    private DuelProtocol.Command pending;
    Thread writer;
    DuelClient(String host,int port,String code,String name,Settings settings)throws IOException{
        socket=new Socket();
        try{
            socket.connect(new InetSocketAddress(host,port),5000);socket.setTcpNoDelay(true);socket.setSoTimeout(5000);
            in=new DataInputStream(socket.getInputStream());out=new DataOutputStream(socket.getOutputStream());
            int options=(settings.sniperToggle?1:0)|(settings.aimToggle?2:0)|(settings.crouchToggle?4:0)|(settings.walkToggle?8:0);
            String room=code.strip().toUpperCase(java.util.Locale.ROOT);
            DuelProtocol.send(out,DuelProtocol.bytes(o->{o.writeInt(DuelProtocol.MAGIC);o.writeInt(DuelProtocol.VERSION);DuelProtocol.text(o,room,24);DuelProtocol.text(o,name.strip(),64);o.writeByte(options);}));
            DataInputStream hello=DuelProtocol.receive(in);if(!hello.readBoolean())throw new IOException(DuelProtocol.text(hello,240));seat=hello.readUnsignedByte();map=hello.readUnsignedByte();if(seat>1||map>2)throw new IOException("Sala inválida");
            lastReceived=System.nanoTime();writer=DuelServer.thread("rift-duel-client-write-"+seat,this::write);DuelServer.thread("rift-duel-client-read-"+seat,this::read);
        }catch(IOException|RuntimeException ex){socket.close();throw ex;}
    }
    synchronized void submit(DuelProtocol.Command c){
        if(pending!=null)c=new DuelProtocol.Command(c.sequence(),c.sentAt(),c.down(),c.edges()|pending.edges(),c.buttons()|(pending.buttons()&4),c.yaw(),c.pitch(),c.choice(),c.ready()||pending.ready());pending=c;LockSupport.unpark(writer);
    }
    private synchronized DuelProtocol.Command take(){DuelProtocol.Command c=pending;pending=null;return c;}
    private void write(){try{while(!closed){DuelProtocol.Command c=take();if(c==null){LockSupport.parkNanos(10_000_000);continue;}DuelProtocol.send(out,DuelProtocol.command(c));}}catch(IOException ex){fail(ex);}}
    private void read(){try{while(!closed){DataInputStream packet=DuelProtocol.receive(in);byte[] b=packet.readAllBytes();snapshot.set(b);lastReceived=System.nanoTime();}}catch(IOException ex){fail(ex);}}
    private void fail(Exception ex){if(!closed){error=ex instanceof SocketTimeoutException?"A conexão parou de responder.":"Conexão encerrada: "+(ex.getMessage()==null?"o outro lado saiu":ex.getMessage());close();}}
    @Override public void close(){closed=true;try{socket.close();}catch(IOException ex){System.err.println(ex.getMessage());}if(writer!=null)LockSupport.unpark(writer);}
}
