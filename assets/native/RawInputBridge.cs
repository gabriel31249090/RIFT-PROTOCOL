// RIFT Protocol 1.8 — Windows Raw Input bridge. .NET Framework 4.x; no drivers or administrator rights.
// Captures only while the RIFT process or this pairing window is foreground. Output is a private pipe.
using System;
using System.Text;
using System.IO;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Runtime.InteropServices;
using System.Threading;
using System.Windows.Forms;

class RiftDevices : Form {
    [StructLayout(LayoutKind.Sequential)] struct Device { public ushort page,usage; public uint flags; public IntPtr target; }
    [StructLayout(LayoutKind.Sequential)] struct Header { public uint type,size; public IntPtr device,wparam; }
    [StructLayout(LayoutKind.Sequential)] struct Rect { public int left,top,right,bottom; }
    [StructLayout(LayoutKind.Sequential)] struct Pt { public int x,y; }
    [DllImport("user32.dll")] static extern bool SetProcessDPIAware();
    [DllImport("user32.dll",SetLastError=true)] static extern bool RegisterRawInputDevices(Device[] devices,uint count,uint size);
    [DllImport("user32.dll")] static extern uint GetRawInputData(IntPtr input,uint command,IntPtr data,ref uint size,uint headerSize);
    [DllImport("user32.dll",CharSet=CharSet.Unicode)] static extern uint GetRawInputDeviceInfo(IntPtr device,uint command,StringBuilder data,ref uint size);
    [DllImport("user32.dll")] static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr window,out uint pid);
    [DllImport("user32.dll")] static extern bool ClipCursor(ref Rect rect);
    [DllImport("user32.dll",EntryPoint="ClipCursor")] static extern bool ReleaseCursor(IntPtr unused);
    [DllImport("user32.dll")] static extern bool GetClientRect(IntPtr window,out Rect rect);
    [DllImport("user32.dll")] static extern bool ClientToScreen(IntPtr window,ref Pt point);
    [DllImport("user32.dll")] static extern bool SetForegroundWindow(IntPtr window);
    [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr window);
    delegate bool EnumWindow(IntPtr window,IntPtr parameter);
    [DllImport("user32.dll")] static extern bool EnumWindows(EnumWindow callback,IntPtr parameter);
    readonly IntPtr[] keyboards=new IntPtr[2],mice=new IntPtr[2];
    readonly bool[][] down={new bool[256],new bool[256]},edges={new bool[256],new bool[256]};
    readonly int[] buttons=new int[2],dx=new int[2],dy=new int[2];
    readonly Dictionary<IntPtr,int> numbers=new Dictionary<IntPtr,int>();
    readonly string[] keyboardNames=new string[2],mouseNames=new string[2];
    int stage;long sequence;uint parent,self;Process parentProcess;bool active,lastActive;string warning="";
    Label title=new Label(),body=new Label(),devices=new Label();
    System.Windows.Forms.Timer timer=new System.Windows.Forms.Timer();
    RiftDevices(uint process){
        parent=process;self=(uint)Process.GetCurrentProcess().Id;parentProcess=Process.GetProcessById((int)parent);Text="RIFT / Identificar teclados e mouses";Size=new Size(660,440);StartPosition=FormStartPosition.CenterScreen;
        BackColor=Color.FromArgb(15,30,43);ForeColor=Color.FromArgb(228,239,231);Font=new Font("Segoe UI",12);
        title.SetBounds(30,30,590,65);title.Font=new Font("Segoe UI",21,FontStyle.Bold);
        body.SetBounds(30,107,590,115);devices.SetBounds(30,227,590,90);devices.Font=new Font("Segoe UI",10);
        Label help=new Label();help.SetBounds(30,328,590,60);help.Text="F9: cadastrar novamente    F12: sair dos dispositivos\nAlt+Tab libera o cursor. Cada pessoa precisa de um conjunto físico distinto.";help.Font=new Font("Segoe UI",10);
        Controls.AddRange(new Control[]{title,body,devices,help});
        Shown+=delegate {Device[] d={new Device {page=1,usage=6,flags=0x100|0x2000,target=Handle},new Device {page=1,usage=2,flags=0x100|0x2000,target=Handle}};
            if(!RegisterRawInputDevices(d,2,(uint)Marshal.SizeOf(typeof(Device)))){MessageBox.Show("Raw Input indisponível: "+Marshal.GetLastWin32Error());Close();return;}RefreshStep();};
        FormClosed+=delegate {timer.Stop();ReleaseCursor(IntPtr.Zero);parentProcess.Dispose();};
        timer.Interval=16;timer.Tick+=Tick;timer.Start();
        Thread stop=new Thread(delegate(){try{while(Console.ReadLine()!=null){if(IsHandleCreated)BeginInvoke(new Action(Close));break;}}catch(Exception){} });stop.IsBackground=true;stop.Start();
    }
    static void Emit(string line){try{Console.WriteLine(line);Console.Out.Flush();}catch(IOException){ReleaseCursor(IntPtr.Zero);Application.Exit();}}
    string Name(IntPtr handle,string type){
        int number;if(!numbers.TryGetValue(handle,out number)){number=numbers.Count+1;numbers.Add(handle,number);}
        uint n=0;GetRawInputDeviceInfo(handle,0x20000007,null,ref n);string shortName="";
        if(n>0&&n<4096){StringBuilder b=new StringBuilder((int)n+1);GetRawInputDeviceInfo(handle,0x20000007,b,ref n);string s=b.ToString();int vid=s.IndexOf("VID_",StringComparison.OrdinalIgnoreCase);if(vid>=0)shortName=" • "+s.Substring(vid,Math.Min(17,s.Length-vid));}
        return type+" #"+number+shortName;
    }
    void RefreshStep(){
        string[] titles={"01 / Teclado do jogador 1","02 / Mouse do jogador 1","03 / Teclado do jogador 2","04 / Mouse do jogador 2","Dispositivos separados"};
        title.Text=titles[stage];body.Text=stage==4?"Pronto. Volte para uma das janelas do jogo. Os dois jogadores podem agir ao mesmo tempo.":stage%2==0?"Aperte ENTER no teclado que este jogador vai usar.":"Clique com o botão esquerdo do mouse que este jogador vai usar.";
        if(warning.Length>0)body.Text+="\n"+warning;
        devices.Text="J1: "+(keyboardNames[0]??"teclado pendente")+" / "+(mouseNames[0]??"mouse pendente")+"\nJ2: "+(keyboardNames[1]??"teclado pendente")+" / "+(mouseNames[1]??"mouse pendente");
        Emit("STATUS "+titles[stage]);for(int j=0;j<2;j++)Emit("LABEL "+j+" J"+(j+1)+": "+(keyboardNames[j]??"?")+" / "+(mouseNames[j]??"?"));
    }
    void ResetState(){for(int j=0;j<2;j++){Array.Clear(down[j],0,256);Array.Clear(edges[j],0,256);buttons[j]=dx[j]=dy[j]=0;}Emit("CLEAR");ReleaseCursor(IntPtr.Zero);}
    void PairAgain(){stage=0;keyboards[0]=keyboards[1]=mice[0]=mice[1]=IntPtr.Zero;keyboardNames[0]=keyboardNames[1]=mouseNames[0]=mouseNames[1]=null;ResetState();WindowState=FormWindowState.Normal;Show();Activate();RefreshStep();}
    void Paired(){ResetState();RefreshStep();Emit("READY");WindowState=FormWindowState.Minimized;
        EnumWindows(delegate(IntPtr h,IntPtr p){uint pid;GetWindowThreadProcessId(h,out pid);if(pid==parent&&IsWindowVisible(h)){SetForegroundWindow(h);return false;}return true;},IntPtr.Zero);
    }
    protected override void WndProc(ref Message message){
        if(message.Msg==0x00FE&&stage==4){IntPtr removed=message.LParam;if(message.WParam.ToInt32()==2&&(Array.IndexOf(keyboards,removed)>=0||Array.IndexOf(mice,removed)>=0)){warning="Dispositivo removido. Cadastre novamente.";PairAgain();}}
        if(message.Msg==0x00FF){
            uint pid;GetWindowThreadProcessId(GetForegroundWindow(),out pid);bool allowed=pid==parent||pid==self;
            if(allowed){uint size=0,hs=(uint)Marshal.SizeOf(typeof(Header));GetRawInputData(message.LParam,0x10000003,IntPtr.Zero,ref size,hs);
                if(size>=hs&&size<4096){IntPtr data=Marshal.AllocHGlobal((int)size);try{uint got=GetRawInputData(message.LParam,0x10000003,data,ref size,hs);if(got==size){Header h=(Header)Marshal.PtrToStructure(data,typeof(Header));IntPtr p=IntPtr.Add(data,(int)hs);if(h.device!=IntPtr.Zero){if(h.type==1&&size>=hs+16)Keyboard(h.device,p);if(h.type==0&&size>=hs+24)Mouse(h.device,p);}}}finally{Marshal.FreeHGlobal(data);}}
            }
        }
        base.WndProc(ref message);
    }
    void Keyboard(IntPtr device,IntPtr p){
        int flags=(ushort)Marshal.ReadInt16(p,2),key=(ushort)Marshal.ReadInt16(p,6);bool pressed=(flags&1)==0;
        if(key==123&&pressed){Close();return;}if(key==120&&pressed){warning="";PairAgain();return;}
        if(stage<4){if(stage%2==0&&key==13&&pressed){int seat=stage/2;if(seat==1&&device==keyboards[0]){warning="Este teclado já pertence ao jogador 1. Use o outro.";RefreshStep();return;}keyboards[seat]=device;keyboardNames[seat]=Name(device,"teclado");stage++;warning="";RefreshStep();}return;}
        if(!active)return;int player=Array.IndexOf(keyboards,device);if(player<0)return;
        if(key==13)key=10; // Windows VK_RETURN -> Java VK_ENTER
        if(key==160||key==161)key=16;if(key==162||key==163)key=17;if(key==164||key==165)key=18;
        if(key<0||key>=256)return;if(pressed&&!down[player][key])edges[player][key]=true;down[player][key]=pressed;
    }
    void Mouse(IntPtr device,IntPtr p){
        int flags=(ushort)Marshal.ReadInt16(p,4);
        if(stage<4){if(stage%2==1&&(flags&1)!=0){int seat=stage/2;if(seat==1&&device==mice[0]){warning="Este mouse já pertence ao jogador 1. Use o outro.";RefreshStep();return;}mice[seat]=device;mouseNames[seat]=Name(device,"mouse");stage++;warning="";if(stage==4)Paired();else RefreshStep();}return;}
        if(!active)return;int player=Array.IndexOf(mice,device);if(player<0)return;
        if((Marshal.ReadInt16(p,0)&1)!=0){warning="Use mouses com movimento relativo. Touchscreens/tablets absolutos não são compatíveis.";return;}
        dx[player]+=Marshal.ReadInt32(p,12);dy[player]+=Marshal.ReadInt32(p,16);
        if((flags&1)!=0)buttons[player]|=5;if((flags&2)!=0)buttons[player]&=~1;
        if((flags&4)!=0)buttons[player]|=2;if((flags&8)!=0)buttons[player]&=~2;
    }
    string Hex(bool[] bits){StringBuilder s=new StringBuilder(64);for(int i=0;i<256;i+=4){int n=0;for(int j=0;j<4;j++)if(bits[i+j])n|=1<<j;s.Append("0123456789abcdef"[n]);}return s.ToString();}
    void Tick(object sender,EventArgs args){
        try{if(parentProcess.HasExited){Close();return;}}catch(InvalidOperationException){Close();return;}
        uint pid;IntPtr foreground=GetForegroundWindow();GetWindowThreadProcessId(foreground,out pid);active=stage==4&&pid==parent;
        if(active!=lastActive){ResetState();if(active)Emit("READY");lastActive=active;}
        if(active){Rect rect;Pt point=new Pt();if(GetClientRect(foreground,out rect)){point.x=(rect.right-rect.left)/2;point.y=(rect.bottom-rect.top)/2;ClientToScreen(foreground,ref point);Rect clip=new Rect{left=point.x,top=point.y,right=point.x+1,bottom=point.y+1};ClipCursor(ref clip);}
            for(int j=0;j<2;j++){Emit("S "+(++sequence)+" "+j+" "+Hex(down[j])+" "+Hex(edges[j])+" "+buttons[j]+" "+dx[j]+" "+dy[j]);Array.Clear(edges[j],0,256);buttons[j]&=3;dx[j]=dy[j]=0;}
        }else ReleaseCursor(IntPtr.Zero);
        if(sequence%60==0)Emit("PING");
    }
    [STAThread] static void Main(string[] args){Console.OutputEncoding=new UTF8Encoding(false);try{SetProcessDPIAware();Application.SetUnhandledExceptionMode(UnhandledExceptionMode.ThrowException);Application.EnableVisualStyles();Application.Run(new RiftDevices(uint.Parse(args[0])));}catch(Exception ex){Emit("STATUS Falha nos dispositivos: "+ex.Message);}finally{ReleaseCursor(IntPtr.Zero);}}
}
