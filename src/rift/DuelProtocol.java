package rift;

import java.io.*;
import java.util.*;
import static java.awt.event.KeyEvent.*;
import static rift.Game.*;
import static rift.World.*;

/** Bounded, versioned binary messages. Clients can submit controls and loadouts, never damage or positions. */
final class DuelProtocol {
    static final int MAGIC=0x52465438,VERSION=1,MAX_PACKET=16384,PORT=27960;
    static final int[] KEYS={VK_W,VK_S,VK_A,VK_D,VK_SPACE,VK_SHIFT,VK_CONTROL,VK_R,VK_1,VK_2,VK_3,VK_V};
    static final int KEY_MASK=(1<<KEYS.length)-1;
    record Choice(int primary,int pistol,int agent,int skin,int charm,int blade){
        static Choice defaults(){return new Choice(Weapon.ECHO.ordinal(),Weapon.SPARK.ordinal(),0,0,0,0);}
        boolean valid(){return primary>=0&&primary<Weapon.values().length&&!Weapon.values()[primary].sidearm()&&pistol>=0&&pistol<5&&agent>=0&&agent<Agent.values().length&&skin>=0&&skin<Cosmetics.Skin.values().length&&charm>=0&&charm<Cosmetics.Charm.values().length&&blade>=0&&blade<Cosmetics.Melee.values().length;}
    }
    record Command(long sequence,long sentAt,int down,int edges,int buttons,float yaw,float pitch,Choice choice,boolean ready){
        Input.Frame frame(){return new Input.Frame(bits(down),bits(edges),(buttons&1)!=0,(buttons&2)!=0,(buttons&4)!=0,0,0,0,0);}
        Command held(){return new Command(sequence,sentAt,down,0,buttons&3,yaw,pitch,choice,false);}
        boolean valid(){return sequence>=0&&(down&~KEY_MASK)==0&&(edges&~KEY_MASK)==0&&(buttons&~7)==0&&Float.isFinite(yaw)&&Math.abs(yaw)<1000&&Float.isFinite(pitch)&&Math.abs(pitch)<=1.351&&choice.valid();}
    }
    static BitSet bits(int mask){BitSet b=new BitSet();for(int i=0;i<KEYS.length;i++)if((mask&(1<<i))!=0)b.set(KEYS[i]);return b;}
    static int mask(BitSet b){int m=0;for(int i=0;i<KEYS.length;i++)if(b.get(KEYS[i]))m|=1<<i;return m;}
    interface Writer {void write(DataOutputStream out)throws IOException;}
    static byte[] bytes(Writer writer)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream(2048);try(DataOutputStream d=new DataOutputStream(b)){writer.write(d);}return b.toByteArray();}
    static void send(DataOutputStream out,byte[] packet)throws IOException{if(packet.length>MAX_PACKET)throw new IOException("Pacote excessivo");out.writeInt(packet.length);out.write(packet);out.flush();}
    static DataInputStream receive(DataInputStream in)throws IOException{int size=in.readInt();if(size<1||size>MAX_PACKET)throw new IOException("Tamanho de pacote inválido");byte[] b=in.readNBytes(size);if(b.length!=size)throw new EOFException();return new DataInputStream(new ByteArrayInputStream(b));}
    static void text(DataOutputStream out,String value,int max)throws IOException{byte[] b=value.getBytes(java.nio.charset.StandardCharsets.UTF_8);if(b.length>max)throw new IOException("Texto excessivo");out.writeShort(b.length);out.write(b);}
    static String text(DataInputStream in,int max)throws IOException{int n=in.readUnsignedShort();if(n>max)throw new IOException("Texto excessivo");byte[] b=in.readNBytes(n);if(b.length!=n)throw new EOFException();return new String(b,java.nio.charset.StandardCharsets.UTF_8).replaceAll("[\\p{Cntrl}]","");}
    static void choice(DataOutputStream o,Choice c)throws IOException{o.writeByte(c.primary);o.writeByte(c.pistol);o.writeByte(c.agent);o.writeByte(c.skin);o.writeByte(c.charm);o.writeByte(c.blade);}
    static Choice choice(DataInputStream i)throws IOException{Choice c=new Choice(i.readUnsignedByte(),i.readUnsignedByte(),i.readUnsignedByte(),i.readUnsignedByte(),i.readUnsignedByte(),i.readUnsignedByte());if(!c.valid())throw new IOException("Equipamento inválido");return c;}
    static byte[] command(Command c)throws IOException{return bytes(o->{o.writeLong(c.sequence);o.writeLong(c.sentAt);o.writeInt(c.down);o.writeInt(c.edges);o.writeByte(c.buttons);o.writeFloat(c.yaw);o.writeFloat(c.pitch);choice(o,c.choice);o.writeBoolean(c.ready);});}
    static Command command(DataInputStream i)throws IOException{Command c=new Command(i.readLong(),i.readLong(),i.readInt(),i.readInt(),i.readUnsignedByte(),i.readFloat(),i.readFloat(),choice(i),i.readBoolean());if(!c.valid()||i.available()!=0)throw new IOException("Controles inválidos");return c;}
    static void floats(DataOutputStream o,double... v)throws IOException{for(double n:v)o.writeFloat((float)n);}
    static float f(DataInputStream i)throws IOException{float f=i.readFloat();if(!Float.isFinite(f))throw new IOException("Estado inválido");return f;}
    static void gun(DataOutputStream o,Gun g)throws IOException{o.writeByte(g.kind.ordinal());o.writeShort(g.ammo);o.writeShort(g.reserve);floats(o,g.reload,g.reloadTotal,g.shotAge,g.pitchRecoil,g.yawRecoil,g.bloom);}
    static Gun gun(DataInputStream i,Gun g)throws IOException{int w=i.readUnsignedByte();if(w>=Weapon.values().length)throw new IOException("Arma inválida");if(g==null||g.kind.ordinal()!=w)g=new Gun(Weapon.values()[w]);g.ammo=i.readUnsignedShort();g.reserve=i.readUnsignedShort();double oldReload=g.reload;g.reload=f(i);if(g.reload>oldReload+.05)g.reloadStage=0;g.reloadTotal=f(i);g.shotAge=f(i);g.pitchRecoil=f(i);g.yawRecoil=f(i);g.bloom=f(i);return g;}
    static void actor(DataOutputStream o,Actor a)throws IOException{
        floats(o,a.x,a.y,a.z,a.yaw,a.pitch,a.vx,a.vy,a.vz,a.hp,a.armor,a.eyeHeight,a.moveSpeed,a.walk,a.deathAge,a.bodyYaw,a.animSpeed,a.animCrouch,a.lean,a.animTurn,a.shotGlow,a.damageGlow,a.tagTime,a.landRecovery);
        o.writeBoolean(a.dead);o.writeBoolean(a.crouch);o.writeBoolean(a.grounded);o.writeByte(a.slot);o.writeByte(a.agentIndex);o.writeInt(a.kills);o.writeInt(a.deaths);gun(o,a.pistol);gun(o,a.primary);
    }
    static void actor(DataInputStream i,Actor a)throws IOException{
        a.x=f(i);a.y=f(i);a.z=f(i);a.yaw=f(i);a.pitch=f(i);a.vx=f(i);a.vy=f(i);a.vz=f(i);a.hp=f(i);a.armor=f(i);a.eyeHeight=f(i);a.moveSpeed=f(i);a.walk=f(i);a.deathAge=f(i);a.bodyYaw=f(i);a.animSpeed=f(i);a.animCrouch=f(i);a.lean=f(i);a.animTurn=f(i);a.shotGlow=f(i);a.damageGlow=f(i);a.tagTime=f(i);a.landRecovery=f(i);
        a.dead=i.readBoolean();a.crouch=i.readBoolean();a.grounded=i.readBoolean();a.slot=i.readUnsignedByte();a.agentIndex=i.readUnsignedByte();if(a.slot<1||a.slot>3||a.agentIndex>=Agent.values().length)throw new IOException("Ator inválido");a.kills=i.readInt();a.deaths=i.readInt();a.pistol=gun(i,a.pistol);a.primary=gun(i,a.primary);
    }
    static byte[] snapshot(DuelSimulation s,int seat,long echo)throws IOException{return bytes(o->{
        o.writeLong(s.tick);o.writeLong(echo);o.writeInt(s.epoch);o.writeByte(s.phase);o.writeInt(s.round);o.writeFloat((float)s.timer);o.writeByte(s.winner+1);text(o,s.message,240);
        for(int j=0;j<2;j++){o.writeBoolean(s.connected[j]);o.writeBoolean(s.ready[j]);o.writeInt(s.scores[j]);text(o,s.names[j],64);choice(o,s.choices[j]);actor(o,s.players[j]);}
        Game g=s.seats[seat];floats(o,g.aimLerp,g.weaponEquip,g.viewKick,g.landing,g.swayX,g.swayY,g.hitMarker,g.hitHead,g.killToast,g.combat.swing,g.combat.elapsed,g.combat.inspect,g.combat.charmAngle,g.combat.charmTwist);o.writeBoolean(g.aiming);o.writeBoolean(g.combat.heavy);o.writeInt(g.combat.combo);o.writeInt(g.trainingShots);o.writeInt(g.trainingHits);
        int n=Math.min(40,s.seats[0].traces.size()+s.seats[1].traces.size());o.writeByte(n);int count=0;
        for(Game view:s.seats)for(Trace t:view.traces)if(count++<n){floats(o,t.from.x(),t.from.y(),t.from.z(),t.to.x(),t.to.y(),t.to.z());o.writeInt(t.color);}
    });}
    static final class State {
        long tick,echo;int epoch,phase,round,winner,shots,hits;float timer;String message="Conectando";
        final String[] names={"Jogador 1","Jogador 2"};final int[] scores=new int[2];final boolean[] connected=new boolean[2],ready=new boolean[2];final Choice[] choices={Choice.defaults(),Choice.defaults()};
    }
    static void snapshot(byte[] data,Game g,State s,int seat)throws IOException{
        DataInputStream i=new DataInputStream(new ByteArrayInputStream(data));s.tick=i.readLong();s.echo=i.readLong();s.epoch=i.readInt();s.phase=i.readUnsignedByte();s.round=i.readInt();s.timer=f(i);s.winner=i.readUnsignedByte()-1;s.message=text(i,240);
        for(int j=0;j<2;j++){s.connected[j]=i.readBoolean();s.ready[j]=i.readBoolean();s.scores[j]=i.readInt();s.names[j]=text(i,64);s.choices[j]=choice(i);actor(i,g.actors.get(j));}
        g.aimLerp=f(i);g.weaponEquip=f(i);g.viewKick=f(i);g.landing=f(i);g.swayX=f(i);g.swayY=f(i);g.hitMarker=f(i);g.hitHead=f(i);g.killToast=f(i);g.combat.swing=f(i);g.combat.elapsed=f(i);g.combat.inspect=f(i);g.combat.charmAngle=f(i);g.combat.charmTwist=f(i);g.aiming=i.readBoolean();g.combat.heavy=i.readBoolean();g.combat.combo=i.readInt();s.shots=i.readInt();s.hits=i.readInt();
        g.traces.clear();int n=i.readUnsignedByte();if(n>40)throw new IOException("Rastros excessivos");for(int j=0;j<n;j++)g.traces.add(new Trace(new V(f(i),f(i),f(i)),new V(f(i),f(i),f(i)),i.readInt()));
        if(i.available()!=0||s.phase>4)throw new IOException("Estado incompatível");
        g.agent=Agent.values()[g.player.agentIndex];g.phase=s.phase==DuelSimulation.LIVE?Phase.LIVE:s.phase==DuelSimulation.FINISHED?Phase.MATCH:Phase.BUY;
        Choice c=s.choices[seat];Arrays.fill(g.profile.skins,c.skin);Arrays.fill(g.profile.charms,c.charm);g.profile.melee=c.blade;g.profile.meleeSkin=c.skin;
    }
}
