package rift;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import static rift.Tests.check;
import static rift.Game.*;
import static rift.World.*;

/** Actual two-client sockets plus deterministic combat, input isolation and arena checks. */
final class DuelTests {
    static DuelProtocol.Command command(long seq,int down,int edges,int buttons,float yaw,DuelProtocol.Choice choice,boolean ready){return new DuelProtocol.Command(seq,System.nanoTime(),down,edges,buttons,yaw,0,choice,ready);}
    static DuelProtocol.Command command(boolean ready){return command(1,0,0,0,0,DuelProtocol.Choice.defaults(),ready);}
    static void run()throws Exception{
        for(int map=0;map<3;map++)try(DuelSimulation s=new DuelSimulation(map)){
            for(Actor a:s.players)check(!s.world.blocked(a.x,a.z,a.y,.31,1.78),"Duelo "+map+": spawn livre J"+(a.id+1));
            check(!s.world.visible(s.players[0].eye(),s.players[1].eye()),"Duelo "+map+": spawn sem linha de tiro direta");
            check(s.world.path(72,84,72,44).size()>20,"Duelo "+map+": passagem entre os lados");
            check(s.world.blocked(47.9,64,0,.31,1.78)&&s.world.blocked(96.1,64,0,.31,1.78),"Duelo "+map+": perímetro fechado");
        }
        try(DuelSimulation s=new DuelSimulation(0)){
            s.connect(0,"Alpha",1);s.connect(1,"Bravo",1);
            DuelProtocol.Choice a=new DuelProtocol.Choice(Weapon.HORIZON.ordinal(),Weapon.TALON.ordinal(),2,1,1,1),b=new DuelProtocol.Choice(Weapon.SHADE.ordinal(),Weapon.VEIL.ordinal(),4,2,2,2);
            s.step(new DuelProtocol.Command[]{command(1,0,0,0,0,a,true),command(1,0,0,0,0,b,true)});
            check(s.phase==DuelSimulation.PREP,"Dois jogadores prontos iniciam preparação");
            check(s.players[0].primary.kind==Weapon.HORIZON&&s.players[1].primary.kind==Weapon.SHADE,"Loadouts diferentes no mesmo servidor");
            check(s.players[0].primary!=s.players[1].primary&&s.players[0].pistol!=s.players[1].pistol,"Munição e recarga sem referências compartilhadas");
            check(s.seats[0].profile.melee==1&&s.seats[1].profile.melee==2,"Cosméticos independentes");
            double z=s.players[0].z;s.step(new DuelProtocol.Command[]{command(2,1,0,0,(float)Math.PI,a,false),command(false)});
            check(s.players[0].z==z,"Movimento bloqueado durante preparação");
            s.timer=.001;s.step(new DuelProtocol.Command[]{null,null});check(s.phase==DuelSimulation.LIVE,"Contagem regressiva libera o duelo");
            for(int t=0;t<60;t++)s.step(new DuelProtocol.Command[]{command(t+3,1,0,0,(float)Math.PI,a,false),null});
            check(s.players[0].z<z-4&&s.players[1].z==44,"W do jogador 1 não movimenta o jogador 2");
            s.step(new DuelProtocol.Command[]{command(90,0,0,0,0,b,false),null});check(s.players[0].primary.kind==Weapon.HORIZON,"Troca de loadout rejeitada em combate");
            s.seats[0].combat.equip(3);check(s.players[0].melee()&&s.players[1].slot==2,"Troca de slot isolada");
            s.seats[0].combat.equip(2);s.seats[0].weaponEquip=0;
            s.players[0].x=60;s.players[0].z=62;s.players[0].yaw=0;s.players[0].pitch=0;s.players[0].moveSpeed=0;s.players[0].primary.pitchRecoil=0;s.players[0].primary.yawRecoil=0;s.players[0].armor=50;s.players[0].hp=100;
            s.players[1].x=60;s.players[1].z=67;s.seats[0].aiming=s.seats[0].aimLatched=true;
            int enemyAmmo=s.players[1].gun().ammo;s.seats[0].firePlayer();
            check(s.players[0].primary.ammo==4&&s.players[1].gun().ammo==enemyAmmo,"Disparo só consome munição do atirador");
            check(s.players[1].dead&&s.players[0].hp==100,"Servidor calcula headshot, armadura e eliminação");
            s.step(new DuelProtocol.Command[]{null,null});check(s.scores[0]==1&&s.phase==DuelSimulation.RESULT,"Abate encerra rodada e soma uma vitória");
            s.timer=.001;s.step(new DuelProtocol.Command[]{null,null});check(s.round==2&&s.players[0].z==44&&s.players[1].z==84,"Rodada seguinte troca lados e repõe equipamentos");
            s.phase=DuelSimulation.LIVE;s.timer=0;s.players[0].hp=80;s.players[1].hp=45;s.step(new DuelProtocol.Command[]{null,null});check(s.winner==0,"Desempate por vida e escudo ao fim do tempo");
            s.scores[0]=6;s.phase=DuelSimulation.LIVE;s.finish(0,"Teste");s.timer=0;s.step(new DuelProtocol.Command[]{null,null});check(s.phase==DuelSimulation.FINISHED&&s.scores[0]==7,"Primeiro a sete encerra a partida");
            s.step(new DuelProtocol.Command[]{command(true),command(true)});check(s.phase==DuelSimulation.PREP&&s.scores[0]==0&&s.players[0].kills==0,"Revanche exige ambos prontos e zera o placar");
            byte[] packet=DuelProtocol.snapshot(s,0,123);Game copy=replica(s.world,0);DuelProtocol.State state=new DuelProtocol.State();DuelProtocol.snapshot(packet,copy,state,0);
            check(copy.player.primary.kind==s.players[0].primary.kind&&state.epoch==s.epoch,"Snapshot conserva inventário e rodada");
            check(packet.length<DuelProtocol.MAX_PACKET,"Snapshot respeita limite de pacote");copy.close();
            s.disconnect(1);check(s.phase==DuelSimulation.FINISHED&&s.winner==0,"Desconexão finaliza duelo em andamento");
        }
        protocol();raw();network();parallelRender();System.out.println("DUEL TESTS OK");
    }
    static Game replica(World world,int seat){Settings settings=new Settings(false);settings.shadows=false;Game g=new Game(settings,false,2,world);g.duel=true;g.ui="play";for(int j=0;j<2;j++){Actor a=new Actor(j,j,"J"+j);a.primary=new Gun(Weapon.ECHO);g.actors.add(a);}g.player=g.actors.get(seat);return g;}
    static void protocol()throws Exception{
        DuelProtocol.Command c=command(9,3,1,5,.5f,DuelProtocol.Choice.defaults(),true);DuelProtocol.Command d=DuelProtocol.command(new DataInputStream(new ByteArrayInputStream(DuelProtocol.command(c))));check(c.equals(d),"Protocolo preserva controles e sequência");
        check(!command(1,0,0,0,Float.NaN,c.choice(),false).valid(),"NaN rejeitado na orientação");
        check(!command(1,1<<29,0,0,0,c.choice(),false).valid(),"Teclas de habilidades e bits desconhecidos rejeitados");
        boolean rejected=false;try{DuelProtocol.receive(new DataInputStream(new ByteArrayInputStream(new byte[]{0x7f,-1,-1,-1})));}catch(IOException ex){rejected=true;}check(rejected,"Pacotes excessivos rejeitados antes de alocar memória");
    }
    static String hex(int... keys){char[] hex=new char[64];Arrays.fill(hex,'0');for(int k:keys)hex[k/4]=Character.forDigit(Character.digit(hex[k/4],16)|(1<<(k%4)),16);return new String(hex);}
    static void raw(){
        check(RawInputHub.decode(hex(186,189,219,46)).get(59)&&RawInputHub.decode(hex(186,189,219,46)).get(45)&&RawInputHub.decode(hex(186,189,219,46)).get(91)&&RawInputHub.decode(hex(186,189,219,46)).get(127),"Raw Input converte pontuação e Delete para binds Java");
        RawInputHub hub=new RawInputHub();hub.accept("READY");hub.accept("S 1 0 "+hex(87)+" "+hex(87)+" 5 31 -9");
        Input.Frame a=hub.poll(0),b=hub.poll(1);check(a.held(87)&&a.pressed(87)&&a.click()&&a.dx()==31&&a.dy()==-9,"Dispositivo J1 conserva teclado, clique e movimento bruto");check(!b.held(87)&&!b.fire()&&b.dx()==0,"Dispositivo J1 não vaza para J2");
        hub.accept("S 2 1 "+hex(65)+" "+hex(65)+" 2 -4 6");check(hub.poll(1).aim()&&hub.poll(0).held(87),"Ambos jogadores podem manter comandos distintos");
        hub.accept("S 1 0 "+hex()+" "+hex()+" 0 99 0");check(hub.poll(0).held(87),"Pacote antigo de dispositivo é descartado");
        hub.accept("CLEAR");check(!hub.poll(0).held(87)&&!hub.poll(1).aim(),"Alt+Tab/recadastro limpa as duas entradas");
        hub.accept("READY");hub.accept("S 4 0 "+hex(87)+" "+hex()+" 0 0 0");hub.heartbeat=System.nanoTime()-600_000_000;check(!hub.poll(0).held(87),"Ponte interrompida não deixa tecla presa");hub.close();
    }
    static void await(BooleanSupplier test,String message)throws Exception{long end=System.nanoTime()+3_000_000_000L;while(!test.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(10);check(test.getAsBoolean(),message);}
    static void network()throws Exception{
        Settings settings=new Settings(false);
        try(DuelServer server=new DuelServer(0,0,"DUELTEST",true)){
            boolean rejected=false;try(DuelClient wrong=new DuelClient("127.0.0.1",server.port(),"WRONG","X",settings)){}catch(IOException ex){rejected=ex.getMessage().contains("incorreto");}check(rejected,"Sala real rejeita código errado");
            try(DuelClient a=new DuelClient("127.0.0.1",server.port(),server.code,"Alpha",settings);DuelClient b=new DuelClient("127.0.0.1",server.port(),server.code,"Bravo",settings)){
                check(a.seat==0&&b.seat==1,"Duas conexões reais recebem assentos diferentes");
                await(()->a.snapshot.get()!=null&&b.snapshot.get()!=null,"Os dois clientes recebem snapshots do servidor");
                boolean full=false;try(DuelClient third=new DuelClient("127.0.0.1",server.port(),server.code,"Third",settings)){}catch(IOException ex){full=ex.getMessage().contains("dois jogadores");}check(full,"Sala 1v1 recusa um terceiro jogador");
                a.submit(command(true));b.submit(command(true));
                Game ga=replica(server.simulation.world,0);DuelProtocol.State sa=new DuelProtocol.State();
                await(()->{try{DuelProtocol.snapshot(a.snapshot.get(),ga,sa,0);return sa.phase==DuelSimulation.PREP;}catch(Exception ex){return false;}},"Ready atravessa a conexão e inicia preparação");
                server.events.add(()->server.simulation.timer=.001);Thread.sleep(50);
                a.submit(command(2,1,0,0,(float)Math.PI,DuelProtocol.Choice.defaults(),false));b.submit(command(2,0,0,0,0,DuelProtocol.Choice.defaults(),false));
                Thread.sleep(230);DuelProtocol.snapshot(a.snapshot.get(),ga,sa,0);double z=ga.player.z;
                check(z<83.5&&ga.actors.get(1).z==44,"Entrada de rede move somente o jogador autenticado");
                Thread.sleep(500);DuelProtocol.snapshot(a.snapshot.get(),ga,sa,0);double stopped=ga.player.z;Thread.sleep(140);DuelProtocol.snapshot(a.snapshot.get(),ga,sa,0);check(Math.abs(ga.player.z-stopped)<.01,"Servidor libera teclas após 350 ms sem comandos");
                server.events.add(()->{DuelSimulation d=server.simulation;d.phase=DuelSimulation.LIVE;d.timer=60;Actor p=d.players[0],q=d.players[1];p.x=60;p.z=62;p.y=p.pitch=p.vx=p.vz=p.moveSpeed=0;p.yaw=0;p.primary=new Gun(Weapon.ECHO);p.slot=2;d.seats[0].weaponEquip=0;q.x=60;q.z=67;q.y=0;q.hp=20;q.armor=0;});
                Thread.sleep(50);a.submit(command(3,0,0,5,0,DuelProtocol.Choice.defaults(),false));
                await(()->{try{DuelProtocol.snapshot(b.snapshot.get(),ga,sa,1);return sa.phase==DuelSimulation.RESULT&&sa.scores[0]==1&&ga.actors.get(1).dead;}catch(Exception ex){return false;}},"Tiro enviado pela rede elimina o oponente e replica o placar");
                a.submit(command(4,0,0,0,Float.NaN,DuelProtocol.Choice.defaults(),false));await(()->a.closed,"Servidor desconecta comando não finito");
                await(()->{try{DuelProtocol.snapshot(b.snapshot.get(),ga,sa,1);return !sa.connected[0]&&sa.phase==DuelSimulation.FINISHED;}catch(Exception ex){return false;}},"Outro cliente recebe saída e resultado");ga.close();
            }
        }
    }
    static void parallelRender()throws Exception{
        try(DuelSimulation s=new DuelSimulation(1)){
            Game[] games={replica(s.world,0),replica(s.world,1)};int[] hashes=new int[2];
            for(int j=0;j<2;j++){s.players[j].yaw=j==0?Math.PI:0;DuelProtocol.snapshot(DuelProtocol.snapshot(s,j,0),games[j],new DuelProtocol.State(),j);games[j].player.yaw=j==0?Math.PI+.2:-.2;Renderer r=new Renderer(480,270);r.render(games[j]);hashes[j]=Arrays.hashCode(r.pixels);}
            var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
            try{java.util.List<java.util.concurrent.Future<Boolean>> tasks=new ArrayList<>();for(int j=0;j<2;j++){int seat=j;tasks.add(pool.submit(()->{Renderer r=new Renderer(480,270);for(int n=0;n<40;n++){r.render(games[seat]);if(Arrays.hashCode(r.pixels)!=hashes[seat])return false;}return true;}));}check(tasks.get(0).get()&&tasks.get(1).get(),"Dois renderizadores paralelos preservam as mesmas cores e pixels");}finally{pool.shutdownNow();for(Game g:games)g.close();}
        }
    }
    static void benchmark()throws Exception{
        try(DuelSimulation s=new DuelSimulation(0)){
            Game[] games={replica(s.world,0),replica(s.world,1)};
            for(int j=0;j<2;j++){DuelProtocol.snapshot(DuelProtocol.snapshot(s,j,0),games[j],new DuelProtocol.State(),j);games[j].player.x=60;games[j].player.z=j==0?78:50;games[j].player.yaw=j==0?Math.PI:0;}
            var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var gate=new java.util.concurrent.CyclicBarrier(2);
            try{java.util.List<java.util.concurrent.Future<Double>> results=new ArrayList<>();for(int j=0;j<2;j++){int seat=j;results.add(pool.submit(()->{Renderer r=new Renderer(640,360);for(int k=0;k<50;k++)r.render(games[seat]);gate.await();long start=System.nanoTime();for(int k=0;k<240;k++){games[seat].visualTime+=1/60.;games[seat].player.yaw+=(seat==0?1:-1)*.002;r.render(games[seat]);}return (System.nanoTime()-start)/1e6/240;}));}for(int j=0;j<2;j++){double ms=results.get(j).get();System.out.printf(Locale.ROOT,"J%d: %.2f ms/quadro, %.1f FPS, renderizadores simultâneos 640x360 (sem janelas, sem limite)%n",j+1,ms,1000/ms);}}finally{pool.shutdownNow();for(Game g:games)g.close();}
        }
    }
    static void capture(Path path)throws Exception{
        Files.createDirectories(path);
        Game menu=new Game(new Settings(false),false,18);Tests.shot(new View(menu),path.resolve("menu-1.8.png"));menu.close();
        for(int map=0;map<3;map++)try(DuelSimulation s=new DuelSimulation(map)){
            s.connect(0,"Alpha",1);s.connect(1,"Bravo",1);s.phase=DuelSimulation.LIVE;s.timer=54;
            s.players[0].x=map==2?54:60;s.players[0].z=map==2?84:79;s.players[0].yaw=map==2?Math.PI:Math.PI+.3;s.players[1].x=map==2?54:60;s.players[1].z=map==2?61:62;
            Game g=replica(s.world,0);DuelProtocol.State state=new DuelProtocol.State();DuelProtocol.snapshot(DuelProtocol.snapshot(s,0,0),g,state,0);
            DuelView view=new DuelView(g);BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();view.render(p,1280,720,state,0,s.choices[0],false,false,"1v1 / CONEXÃO DIRETA","");p.dispose();ImageIO.write(image,"png",path.resolve("duelo-"+map+".png").toFile());
            if(map==0){p=image.createGraphics();state.phase=DuelSimulation.PREP;state.timer=6;view.render(p,1280,720,state,0,s.choices[0],true,false,"LOCAL / DOIS TECLADOS + DOIS MOUSES","");p.dispose();ImageIO.write(image,"png",path.resolve("duelo-equipamento.png").toFile());}g.close();
        }
    }
}
