package rift;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Real room creation and address-based joining. No simulated queue or bot impersonating a peer. */
final class DuelLobby {
    static JDialog current;
    static void show(JFrame owner,Settings settings){
        if(current!=null&&current.isDisplayable()){current.toFront();return;}
        new DuelLobby(owner,settings);
    }
    final JFrame owner;final Settings base;final JDialog dialog;
    final JTextField name=new JTextField("Jogador",18),host=new JTextField("192.168.1.10",18),code=new JTextField(18);
    final JSpinner port=new JSpinner(new SpinnerNumberModel(DuelProtocol.PORT,1024,65535,1));
    final JComboBox<String> map=new JComboBox<>(DuelMaps.NAMES);
    final JLabel status=new JLabel("Escolha como jogar.");
    final java.util.List<JButton> buttons=new ArrayList<>();
    DuelLobby(JFrame owner,Settings base){
        this.owner=owner;this.base=base;dialog=new JDialog(owner,"RIFT / Salas 1v1",false);current=dialog;dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(16,16));root.setBorder(BorderFactory.createEmptyBorder(24,28,24,28));root.setBackground(View.INK);
        JPanel header=new JPanel(){protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.drawImage(Assets.HERO,0,-60,getWidth(),getWidth()*9/16,null);g.setPaint(new GradientPaint(0,0,new Color(10,24,35,245),getWidth(),0,new Color(10,24,35,60)));g.fillRect(0,0,getWidth(),getHeight());View.text(g,"RIFT / DUELO",22,52,30,View.WHITE,true);View.text(g,"DUAS PESSOAS. PRIMEIRO A SETE.",24,85,12,View.MINT,true);g.dispose();}};header.setPreferredSize(new Dimension(720,115));root.add(header,BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane();
        JPanel create=panel();row(create,"Seu nome",name);row(create,"Arena",map);row(create,"Porta TCP",port);
        note(create,"<html>Crie a sala e envie endereço, porta e código ao amigo.<br>LAN: use o IP da sua rede. Internet: o anfitrião precisa de uma porta<br>acessível ou de uma VPN entre os PCs. Não há matchmaking automático.</html>");
        action(create,"CRIAR SALA",()->launch(0));tabs.addTab("Criar sala",create);
        JPanel join=panel();row(join,"Endereço do anfitrião",host);JSpinner joinPort=new JSpinner(port.getModel());row(join,"Porta TCP",joinPort);row(join,"Código da sala",code);
        JTextField guestName=new JTextField("Convidado",18);row(join,"Seu nome",guestName);
        action(join,"ENTRAR",()->{name.setText(guestName.getText());launch(1);});tabs.addTab("Entrar por endereço",join);
        JPanel local=panel();note(local,"<html><b>2 teclados + 2 mouses físicos, no mesmo Windows (experimental).</b><br><br>1. Abre uma janela para cada jogador.<br>2. Em dois monitores, distribui uma janela em cada um.<br>3. Cada pessoa aperta Enter no próprio teclado e clica no próprio mouse.<br>4. Os perfis, armas, munição e cosméticos ficam separados.<br><br>F9 recadastra dispositivos. F12 sai. Alt+Tab libera o cursor.<br>O som usa a saída padrão do Windows, compartilhada entre os jogadores.</html>");
        JComboBox<String> localMap=new JComboBox<>(map.getModel());row(local,"Arena",localMap);action(local,"ABRIR DUAS JANELAS",()->launch(2));tabs.addTab("Local / dois conjuntos",local);
        root.add(tabs,BorderLayout.CENTER);status.setForeground(View.MINT);root.add(status,BorderLayout.SOUTH);dialog.setContentPane(root);dialog.pack();dialog.setMinimumSize(new Dimension(760,590));dialog.setLocationRelativeTo(owner);dialog.setVisible(true);
    }
    static JPanel panel(){JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));p.setBackground(new Color(22,43,57));return p;}
    static void row(JPanel p,String title,JComponent control){JPanel row=new JPanel(new BorderLayout(18,4));row.setOpaque(false);JLabel label=new JLabel(title);label.setForeground(View.WHITE);label.setPreferredSize(new Dimension(180,30));row.add(label,BorderLayout.WEST);row.add(control,BorderLayout.CENTER);row.setMaximumSize(new Dimension(720,35));p.add(row);p.add(Box.createVerticalStrut(10));}
    static void note(JPanel p,String text){JLabel label=new JLabel(text);label.setForeground(View.MUTED);label.setFont(new Font("SansSerif",Font.PLAIN,13));label.setBorder(BorderFactory.createEmptyBorder(8,0,18,0));p.add(label);}
    void action(JPanel p,String label,Runnable r){JButton b=new JButton(label);b.setBackground(View.MINT);b.setForeground(View.INK);b.setFont(new Font("SansSerif",Font.BOLD,14));b.addActionListener(e->r.run());buttons.add(b);p.add(b);}
    static Settings settings(Settings base,String seat){
        Path path=Path.of(System.getProperty("user.home"),".rift-protocol","duel",seat,"settings.properties");Settings s=new Settings(path);
        if(!Files.exists(path))try{for(var f:Settings.class.getDeclaredFields()){if(f.getType()==int.class)f.setInt(s,f.getInt(base));else if(f.getType()==double.class)f.setDouble(s,f.getDouble(base));else if(f.getType()==boolean.class)f.setBoolean(s,f.getBoolean(base));}System.arraycopy(base.binds,0,s.binds,0,s.binds.length);}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        s.frameLimit=60;s.quality=3;s.shadows=false;s.renderDistance=100;s.validate();return s;
    }
    void launch(int mode){
        String playerName=name.getText().strip(),address=host.getText().strip(),roomCode=code.getText().strip();int selected=map.getSelectedIndex(),number=(Integer)port.getValue();
        if(mode!=2&&(playerName.isEmpty()||playerName.codePointCount(0,playerName.length())>24||playerName.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>64)){status.setText("Informe um nome de até 24 letras.");return;}
        if(mode==2&&!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")){status.setText("Dois conjuntos no mesmo PC exigem Windows. A conexão em rede está disponível.");return;}
        for(JButton b:buttons)b.setEnabled(false);status.setText("Preparando arena e conexão...");
        new SwingWorker<Session,Void>(){
            protected Session doInBackground()throws Exception{
                Session session=new Session(owner);try{
                    if(mode!=1)session.server=new DuelServer(mode==2?0:number,selected,null,mode==2);
                    int count=mode==2?2:1;World world=session.server==null?null:session.server.simulation.world;
                    if(mode==2)session.raw=new RawInputHub();
                    for(int i=0;i<count;i++){
                        Settings s=settings(base,mode==2?"player-"+(i+1):"online");
                        DuelClient client=new DuelClient(mode==1?address:"127.0.0.1",mode==1?number:session.server.port(),mode==1?roomCode:session.server.code,mode==2?"Jogador "+(i+1):playerName,s);
                        session.clients.add(client);if(world==null){world=new World(3+client.map);world.setShadows(false);}
                        String info=mode==2?"LOCAL / DOIS DISPOSITIVOS POR PESSOA":mode==0?"LAN "+lanAddress()+":"+session.server.port()+"  •  CÓDIGO "+session.server.code:address+":"+number;
                        session.windows.add(new DuelWindow(client,session.raw,i,s,world,info,session::close));
                    }
                    return session;
                }catch(Exception ex){session.close();throw ex;}
            }
            protected void done(){try{Session session=get();if(!dialog.isDisplayable()){session.close();return;}dialog.dispose();owner.setVisible(false);session.open();}catch(Exception ex){for(JButton b:buttons)b.setEnabled(true);Throwable cause=ex.getCause()==null?ex:ex.getCause();status.setText("Não foi possível abrir a sala.");JOptionPane.showMessageDialog(dialog,cause.getMessage(),"RIFT / Conexão",JOptionPane.ERROR_MESSAGE);}}
        }.execute();
    }
    static String lanAddress(){try{for(NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces()))if(ni.isUp()&&!ni.isLoopback())for(InetAddress ip:Collections.list(ni.getInetAddresses()))if(ip instanceof Inet4Address&&ip.isSiteLocalAddress())return ip.getHostAddress();}catch(SocketException ex){System.err.println("IP local: "+ex.getMessage());}return "127.0.0.1";}
    static final class Session {
        final JFrame owner;DuelServer server;RawInputHub raw;volatile boolean closed;
        final java.util.List<DuelClient> clients=new ArrayList<>();final java.util.List<DuelWindow> windows=new ArrayList<>();
        Session(JFrame owner){this.owner=owner;}
        void open(){
            GraphicsDevice[] monitors=GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for(int i=0;i<windows.size();i++){
                Rectangle bounds=new Rectangle(monitors[Math.min(i,monitors.length-1)].getDefaultConfiguration().getBounds());Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(monitors[Math.min(i,monitors.length-1)].getDefaultConfiguration());bounds.x+=insets.left;bounds.y+=insets.top;bounds.width-=insets.left+insets.right;bounds.height-=insets.top+insets.bottom;
                if(windows.size()==2&&monitors.length==1){bounds.width/=2;bounds.x+=i*bounds.width;}else if(windows.size()==1){bounds.width=Math.min(1280,bounds.width);bounds.height=Math.min(760,bounds.height);}
                windows.get(i).open(bounds);
            }
            if(raw!=null)DuelServer.thread("rift-raw-start",()->{try{raw.launch();}catch(Exception ex){EventQueue.invokeLater(()->{JOptionPane.showMessageDialog(owner,ex.getMessage(),"RIFT / Dispositivos",JOptionPane.ERROR_MESSAGE);close();});}});
        }
        synchronized void close(){if(closed)return;closed=true;for(DuelWindow w:windows)w.stop();for(DuelClient c:clients)c.close();if(raw!=null)raw.close();if(server!=null)server.close();EventQueue.invokeLater(()->{owner.setVisible(true);owner.toFront();});}
    }
}
