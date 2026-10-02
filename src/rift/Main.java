package rift;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.nio.file.*;
import java.util.concurrent.locks.LockSupport;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Desktop entry point and native mouse capture. */
public final class Main extends Canvas implements Runnable,KeyListener,MouseListener,MouseMotionListener,FocusListener {
    final Input input=new Input();final Game game=new Game(new Settings(true),true,System.nanoTime());final View view=new View(game);
    JFrame frame;Robot robot;Cursor hidden;volatile boolean focused=true,pauseRequested;
    volatile boolean locked;boolean fullscreen;Rectangle windowBounds;int previousX,previousY;
    public static void main(String[] args)throws Exception {
        if(args.length>0){
            switch(args[0]){
                case "--duel-benchmark"->{DuelTests.benchmark();return;}
                case "--duel-test"->{DuelTests.run();System.out.println(Tests.passed+" checks passed.");return;}
                case "--duel-capture"->{DuelTests.capture(Path.of(args.length>1?args[1]:"screenshots"));return;}
                case "--duel-server"->{
                    int port=args.length>1?Integer.parseInt(args[1]):DuelProtocol.PORT;
                    int map=args.length>2?Integer.parseInt(args[2]):0;
                    DuelServer server=new DuelServer(port,map,args.length>3?args[3]:null,false);
                    Runtime.getRuntime().addShutdownHook(new Thread(server::close));
                    System.out.println("RIFT 1v1 | TCP "+server.port()+" | código "+server.code+" | "+DuelMaps.NAMES[Math.floorMod(map,3)]);
                    while(!server.closed)LockSupport.parkNanos(500_000_000L);return;
                }
                case "--multiplayer"->{ }
                case "--performance"->{ }
                case "--performance-test"->{PerformanceTests.run();System.out.println(Tests.passed+" checks passed.");return;}
                case "--benchmark-full"->{PerformanceTests.benchmark();return;}
                case "--impact-capture"->{ImpactTests.capture(Path.of(args.length>1?args[1]:"screenshots"));return;}
                case "--impact-test"->{ImpactTests.run();ImpactTests.throwables();System.out.println(Tests.passed+" checks passed.");return;}
                case "--tactical-capture"->{TacticalTests.capture(Path.of(args.length>1?args[1]:"screenshots"));return;}
                case "--tactical-test"->{TacticalTests.run();System.out.println(Tests.passed+" checks passed.");return;}
                case "--loadout-frames"->{LoadoutPreview.frames(Path.of(args.length>1?args[1]:"loadout-frames"));return;}
                case "--feel-capture"->{FeelTests.capture(Path.of(args.length>1?args[1]:"screenshots"));return;}
                case "--feel-test"->{FeelTests.run();System.out.println(Tests.passed+" checks passed.");return;}
                case "--benchmark-bots"->{BotBenchmark.main(new String[0]);return;}
                case "--simulate-bots"->{BotSimulation.run(args.length>1?Integer.parseInt(args[1]):6);return;}
                case "--bot-test"->{BotTests.run();System.out.println(Tests.passed+" checks passed.");return;}
                case "--bot-capture"->{BotTests.capture(Path.of(args.length>1?args[1]:"screenshots"));return;}
                case "--self-test"->{Tests.run();return;}
                case "--capture"->{Tests.capture(args.length>1?args[1]:"screenshots");return;}
                case "--benchmark"->{Tests.benchmark();return;}
                case "--animation-frames"->{AnimationPreview.frames(Path.of(args.length>1?args[1]:"animation-frames"));return;}
                default->{System.out.println("RIFT Protocol | java -jar RiftProtocol.jar [--self-test | --capture pasta | --benchmark]");return;}
            }
        }
        if(GraphicsEnvironment.isHeadless()){System.err.println("O jogo precisa de um ambiente gráfico. Para os testes: java -jar RiftProtocol.jar --self-test");return;}
        Main app=new Main();if(args.length>0&&args[0].equals("--performance")){app.game.settings.performance();app.game.world.setShadows(false);app.view.adaptive.reset(1);}SwingUtilities.invokeAndWait(app::open);if(args.length>0&&args[0].equals("--multiplayer"))app.game.multiplayerRequested=true;new Thread(app,"rift-game").start();
    }
    void open(){
        frame=new JFrame("RIFT Protocol — Tactical FPS / Java");frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){game.quit=true;}});
        frame.addWindowFocusListener(new WindowAdapter(){@Override public void windowLostFocus(WindowEvent e){focused=false;pauseRequested=true;input.clear();}@Override public void windowGainedFocus(WindowEvent e){focused=true;}});
        Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();double size=Math.min(1,Math.min((screen.width-80)/1280.,(screen.height-100)/720.));
        setPreferredSize(new Dimension((int)(1280*size),(int)(720*size)));setMinimumSize(new Dimension(800,450));
        setBackground(Color.BLACK);setIgnoreRepaint(true);frame.add(this);frame.pack();frame.setLocationRelativeTo(null);frame.setMinimumSize(new Dimension(800,490));
        BufferedImage icon=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);Graphics2D ig=icon.createGraphics();ig.setColor(View.INK);ig.fillRect(0,0,64,64);View.logo(ig,10,12,43);ig.dispose();frame.setIconImage(icon);
        addKeyListener(this);addMouseListener(this);addMouseMotionListener(this);addFocusListener(this);
        try{robot=new Robot();hidden=Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB),new Point(0,0),"rift");}catch(Exception ignored){robot=null;}
        frame.setVisible(true);createBufferStrategy(2);requestFocusInWindow();
    }
    @Override public void run(){
        long previous=System.nanoTime(),fpsStart=previous;int frames=0;
        try{
            while(!game.quit){
                if(!frame.isVisible()){locked=false;game.audio.menu=false;input.clear();previous=System.nanoTime();LockSupport.parkNanos(50_000_000L);continue;}
                if(game.multiplayerRequested){game.multiplayerRequested=false;EventQueue.invokeLater(()->DuelLobby.show(frame,game.settings));}
                long start=System.nanoTime();double dt=Math.min(.05,(start-previous)/1e9);previous=start;
                Input.Frame in=input.poll();view.pointer(in.mx(),in.my());
                if(pauseRequested){pauseRequested=false;if(game.ui.equals("play")||game.ui.equals("shop")||game.ui.equals("tactical")){if(game.ui.equals("tactical"))game.cancelTactical();game.ui="pause";}}
                if(in.pressed(KeyEvent.VK_F11)||in.pressed(KeyEvent.VK_ENTER)&&in.held(KeyEvent.VK_ALT)){toggleFullscreen();input.clear();in=Input.Frame.empty();}
                if(in.click()&&(!game.ui.equals("play")||game.phase==Game.Phase.MATCH)){
                    if(view.click(in.mx(),in.my())){input.clear();in=Input.Frame.empty();}
                }
                long simStart=System.nanoTime();game.tick(dt,in);game.simulationMillis=(System.nanoTime()-simStart)/1e6;captureMouse();
                BufferStrategy strategy=getBufferStrategy();
                if(strategy==null){createBufferStrategy(2);continue;}
                try {
                    do{do{Graphics2D g=(Graphics2D)strategy.getDrawGraphics();try{view.render(g,getWidth(),getHeight());}finally{g.dispose();}}while(strategy.contentsRestored());strategy.show();}while(strategy.contentsLost());
                    Toolkit.getDefaultToolkit().sync();
                }catch(IllegalStateException ignored){ }
                if(in.pressed(KeyEvent.VK_F12))saveScreenshot();
                frames++;if(start-fpsStart>=1_000_000_000L){game.fps=frames;frames=0;fpsStart=start;}
                long remain=(1_000_000_000L/game.settings.frameLimit)-(System.nanoTime()-start);if(remain>0)LockSupport.parkNanos(remain);
            }
        }catch(Throwable ex){
            ex.printStackTrace();try{Files.writeString(Path.of("rift-error.log"),stack(ex));}catch(Exception ignored){}
            SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(frame,"Ocorreu um erro. Consulte rift-error.log.\n"+ex,"RIFT Protocol",JOptionPane.ERROR_MESSAGE));
        }finally{locked=false;game.close();SwingUtilities.invokeLater(()->frame.dispose());}
    }
    static String stack(Throwable t){java.io.StringWriter s=new java.io.StringWriter();t.printStackTrace(new java.io.PrintWriter(s));return s.toString();}
    void captureMouse(){
        boolean should=focused&&game.ui.equals("play")&&game.phase!=Game.Phase.MATCH;
        if(should==locked)return;locked=should;
        EventQueue.invokeLater(()->{setCursor(locked&&hidden!=null?hidden:Cursor.getDefaultCursor());if(locked){requestFocusInWindow();warp();}});
    }
    void warp(){if(robot==null||!isShowing())return;try{Point p=getLocationOnScreen();robot.mouseMove(p.x+getWidth()/2,p.y+getHeight()/2);}catch(IllegalComponentStateException ignored){}}
    void toggleFullscreen(){
        try{SwingUtilities.invokeAndWait(()->{
            locked=false;frame.dispose();fullscreen=!fullscreen;frame.setUndecorated(fullscreen);
            if(fullscreen){windowBounds=frame.getBounds();Rectangle bounds=frame.getGraphicsConfiguration().getBounds();frame.setBounds(bounds);}
            else{frame.setBounds(windowBounds!=null?windowBounds:new Rectangle(100,80,1280,760));}
            frame.setVisible(true);createBufferStrategy(2);requestFocusInWindow();
        });}catch(Exception e){game.tell("Não foi possível mudar o modo de tela",3);}
    }
    void saveScreenshot(){try{Path dir=Path.of("captures");Files.createDirectories(dir);BufferedImage shot=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=shot.createGraphics();view.render(g,1280,720);g.dispose();ImageIO.write(shot,"png",dir.resolve("rift-"+System.currentTimeMillis()+".png").toFile());game.tell("Captura salva na pasta captures",2);}catch(Exception e){game.tell("Não foi possível salvar a captura",3);}}
    @Override public void keyPressed(KeyEvent e){input.key(e.getKeyCode(),true);}
    @Override public void keyReleased(KeyEvent e){input.key(e.getKeyCode(),false);}
    @Override public void keyTyped(KeyEvent e){}
    @Override public void mousePressed(MouseEvent e){requestFocusInWindow();input.position(e.getX(),e.getY());input.mouse(e.getButton(),true);}
    @Override public void mouseReleased(MouseEvent e){input.mouse(e.getButton(),false);}
    @Override public void mouseMoved(MouseEvent e){
        input.position(e.getX(),e.getY());
        if(locked){
            if(robot!=null){double dx=e.getX()-getWidth()/2,dy=e.getY()-getHeight()/2;if(dx!=0||dy!=0){input.motion(dx,dy);warp();}}
            else input.motion(e.getX()-previousX,e.getY()-previousY);
        }
        previousX=e.getX();previousY=e.getY();
    }
    @Override public void mouseDragged(MouseEvent e){mouseMoved(e);}
    @Override public void mouseClicked(MouseEvent e){}
    @Override public void mouseEntered(MouseEvent e){}
    @Override public void mouseExited(MouseEvent e){}
    @Override public void focusGained(FocusEvent e){focused=true;}
    @Override public void focusLost(FocusEvent e){focused=false;pauseRequested=true;input.clear();}
}
