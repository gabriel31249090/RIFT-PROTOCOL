package rift;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.concurrent.locks.LockSupport;
import javax.swing.*;
import static java.awt.event.KeyEvent.*;
import static rift.Game.*;

/** One camera, one input stream, one independent equipment profile. */
final class DuelWindow extends Canvas implements Runnable {
    final DuelClient client;final RawInputHub raw;final int localSeat;
    final Game game;final DuelView view;final DuelAudio audio;final Input input=new Input();
    final DuelProtocol.State state=new DuelProtocol.State();final Runnable onClose;final String room;
    DuelProtocol.Choice choice;
    JFrame frame;Robot robot;Cursor hidden;
    volatile boolean closed,focused=true,locked,paused;
    boolean equipment=true;int previousPhase=-1,epoch=-1;long sequence;double yaw,pitch;
    DuelWindow(DuelClient client,RawInputHub raw,int localSeat,Settings settings,World world,String room,Runnable onClose){
        this.client=client;this.raw=raw;this.localSeat=localSeat;this.room=room;this.onClose=onClose;
        game=new Game(settings,raw==null||localSeat==0,1800+client.seat,world);game.duel=true;game.ui="play";game.audio.menu=false;
        for(int i=0;i<2;i++){Actor a=new Actor(i,i,"Jogador "+(i+1));World.V spawn=DuelMaps.spawn(i);a.x=spawn.x();a.z=spawn.z();a.yaw=i==0?Math.PI:0;a.primary=new Gun(Weapon.ECHO);a.slot=2;game.actors.add(a);}
        game.player=game.actors.get(client.seat);game.phase=Phase.BUY;game.agent=Agent.values()[settings.agent];
        Profile p=game.profile;choice=new DuelProtocol.Choice(p.savedPrimary.ordinal(),p.savedPistol.ordinal(),settings.agent,p.skins[p.savedPrimary.ordinal()],p.charms[p.savedPrimary.ordinal()],p.melee);
        view=new DuelView(game);audio=new DuelAudio(game);
    }
    void open(Rectangle bounds){
        frame=new JFrame("RIFT 1v1 / J"+(client.seat+1)+(raw!=null?" / dispositivos separados":" / rede"));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);frame.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){stop();onClose.run();}});
        frame.addWindowFocusListener(new WindowAdapter(){public void windowLostFocus(WindowEvent e){focused=false;input.clear();}public void windowGainedFocus(WindowEvent e){focused=true;requestFocusInWindow();}});
        frame.add(this);setBackground(Color.BLACK);setIgnoreRepaint(true);frame.setBounds(bounds);frame.setMinimumSize(new Dimension(480,300));
        try{robot=new Robot(frame.getGraphicsConfiguration().getDevice());hidden=Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB),new Point(0,0),"rift-duel");}catch(AWTException ex){System.err.println("Mouse capture: "+ex);}
        addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){if(raw==null)input.key(e.getKeyCode(),true);}public void keyReleased(KeyEvent e){if(raw==null)input.key(e.getKeyCode(),false);}});
        addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){if(raw==null){requestFocusInWindow();input.mouse(e.getButton(),true);}}public void mouseReleased(MouseEvent e){if(raw==null)input.mouse(e.getButton(),false);}});
        MouseMotionAdapter motion=new MouseMotionAdapter(){int x,y;public void mouseMoved(MouseEvent e){if(raw==null&&locked){int mx=robot==null?e.getX()-x:e.getX()-getWidth()/2,my=robot==null?e.getY()-y:e.getY()-getHeight()/2;if(mx!=0||my!=0){input.motion(mx,my);warp();}}x=e.getX();y=e.getY();}public void mouseDragged(MouseEvent e){mouseMoved(e);}};addMouseMotionListener(motion);
        frame.setVisible(true);createBufferStrategy(2);requestFocusInWindow();
        DuelServer.thread("rift-duel-view-"+client.seat,this);
    }
    void warp(){if(robot!=null&&isShowing())try{Point p=getLocationOnScreen();robot.mouseMove(p.x+getWidth()/2,p.y+getHeight()/2);}catch(IllegalComponentStateException ex){/* Window is closing. */}}
    void capture(){boolean next=raw==null&&focused&&!paused&&!closed;if(next==locked)return;locked=next;EventQueue.invokeLater(()->{setCursor((locked||raw!=null)&&hidden!=null?hidden:Cursor.getDefaultCursor());if(locked)warp();});}
    @Override public void run(){
        long last=System.nanoTime(),fpsAt=last;int frames=0;
        try{
            while(!closed){
                if(raw!=null&&raw.stopped){onClose.run();break;}
                long start=System.nanoTime();double dt=Math.min(.05,(start-last)/1e9);last=start;
                byte[] packet=client.snapshot.getAndSet(null);
                if(packet!=null){
                    int oldShots=state.shots,oldHits=state.hits,oldKills=game.player.kills;double hp=game.player.hp,remoteAge=game.actors.get(1-client.seat).gun().shotAge;
                    DuelProtocol.snapshot(packet,game,state,client.seat);
                    audio.observe(state);
                    if(state.epoch!=epoch){epoch=state.epoch;yaw=game.player.yaw;pitch=game.player.pitch;input.clear();}
                    if(state.phase!=previousPhase){previousPhase=state.phase;equipment=state.phase==DuelSimulation.WAITING||state.phase==DuelSimulation.PREP||state.phase==DuelSimulation.FINISHED;}
                    if(state.shots>oldShots){game.audio.play(game.player.gun().kind.sound());game.shotFX.fired(game.player);}
                    Actor remote=game.actors.get(1-client.seat);if(remote.gun().shotAge+.01<remoteAge&&remote.gun().shotAge<.1){double pan=Math.sin(Math.atan2(remote.x-game.player.x,remote.z-game.player.z)-yaw);game.audio.playAt(remote.gun().kind.sound(),pan,.45);}
                    if(state.hits>oldHits)game.audio.play(game.hitHead>0?"head":"hit");if(game.player.hp<hp)game.audio.play("hurt");if(game.player.kills>oldKills)game.audio.play("multi1");
                    if(state.echo>0)view.latency=Math.max(0,(System.nanoTime()-state.echo)/1_000_000);
                }
                Input.Frame controls=raw==null?input.poll():raw.poll(localSeat);
                if(controls.pressed(VK_ESCAPE))paused=!paused;
                if(controls.pressed(VK_F12)){stop();onClose.run();break;}
                if(raw==null&&!focused||paused)controls=Input.Frame.empty();
                boolean selecting=state.phase==DuelSimulation.WAITING||state.phase==DuelSimulation.PREP||state.phase==DuelSimulation.FINISHED;
                if(selecting){if(controls.pressed(VK_B))equipment=!equipment;select(controls);}else equipment=false;
                Input.Frame mapped=game.settings.remap(controls);
                if(!game.player.dead&&!equipment){
                    double sens=game.settings.sensitivity*(game.aiming?game.settings.adsSensitivity:1);
                    yaw+=mapped.dx()*sens;pitch+=mapped.dy()*sens*(game.settings.invertY?1:-1);
                    if(mapped.held(VK_LEFT))yaw-=dt*1.8;if(mapped.held(VK_RIGHT))yaw+=dt*1.8;
                    if(mapped.held(VK_UP))pitch+=dt;if(mapped.held(VK_DOWN))pitch-=dt;
                }
                yaw=Math.atan2(Math.sin(yaw),Math.cos(yaw));pitch=Settings.clamp(pitch,-1.35,1.35);
                boolean ready=controls.pressed(VK_ENTER)&&selecting;
                int buttons=(mapped.fire()||mapped.click()?1:0)|(mapped.aim()?2:0)|(mapped.click()?4:0);
                client.submit(new DuelProtocol.Command(++sequence,System.nanoTime(),DuelProtocol.mask(mapped.down()),DuelProtocol.mask(mapped.edges()),buttons,(float)yaw,(float)pitch,choice,ready));
                if(!game.player.dead){game.player.yaw=yaw;game.player.pitch=pitch;}
                game.visualTime+=dt;game.shotFX.tick(dt);capture();
                String status=client.error;
                if(status.isEmpty()&&raw!=null)status=raw.ready?raw.labels[localSeat]:raw.status;
                BufferStrategy buffer=getBufferStrategy();if(buffer!=null)try{do{do{Graphics2D p=(Graphics2D)buffer.getDrawGraphics();try{view.render(p,getWidth(),getHeight(),state,client.seat,choice,equipment,paused,room,status);}finally{p.dispose();}}while(buffer.contentsRestored());buffer.show();}while(buffer.contentsLost());}catch(IllegalStateException ex){if(!closed)System.err.println("Janela: "+ex.getMessage());}
                frames++;if(start-fpsAt>=1_000_000_000L){view.fps=frames;frames=0;fpsAt=start;}
                long remaining=1_000_000_000L/game.settings.frameLimit-(System.nanoTime()-start);if(remaining>0)LockSupport.parkNanos(remaining);
            }
        }catch(Exception ex){ex.printStackTrace();EventQueue.invokeLater(()->JOptionPane.showMessageDialog(frame,"Falha no duelo: "+ex.getMessage()));onClose.run();}
        finally{stop();saveProfile();game.close();EventQueue.invokeLater(()->frame.dispose());}
    }
    void select(Input.Frame in){
        int primary=choice.primary(),pistol=choice.pistol(),agent=choice.agent(),skin=choice.skin(),charm=choice.charm(),blade=choice.blade();
        if(in.pressed(VK_F5)){do{primary=(primary+1)%Weapon.values().length;}while(Weapon.values()[primary].sidearm());}
        if(in.pressed(VK_F6))pistol=(pistol+1)%5;if(in.pressed(VK_F7))agent=(agent+1)%Agent.values().length;
        if(in.pressed(VK_F8))skin=(skin+1)%Cosmetics.Skin.values().length;if(in.pressed(VK_HOME))charm=(charm+1)%Cosmetics.Charm.values().length;if(in.pressed(VK_END))blade=(blade+1)%Cosmetics.Melee.values().length;
        choice=new DuelProtocol.Choice(primary,pistol,agent,skin,charm,blade);
    }
    synchronized void stop(){if(closed)return;closed=true;locked=false;client.close();if(frame==null){saveProfile();game.close();}}
    void saveProfile(){game.settings.agent=choice.agent();Profile p=game.profile;p.savedPrimary=Weapon.values()[choice.primary()];p.savedPistol=Weapon.values()[choice.pistol()];java.util.Arrays.fill(p.skins,choice.skin());java.util.Arrays.fill(p.charms,choice.charm());p.melee=choice.blade();p.save();}
}
