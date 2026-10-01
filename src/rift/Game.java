package rift;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import static rift.World.*;
import static java.awt.event.KeyEvent.*;

final class Game {
    double simulationMillis;
    enum Phase { MENU, BUY, LIVE, END, MATCH, TRAIN }
    enum Ability { DASH, SMOKE, HEAL, SCAN, FLASH, FOCUS, ORBITAL, SURGE,
        QUICK_SMOKE, UPDRAFT, STIM, INCENDIARY, SUPPRESS, TOXIC_WALL, POISON_CLOUD, ACID, TOXIC_DOME,
        BLINK, BLIND_WAVE, GLOBAL_TELEPORT, CURVEFLASH, HEAL_FIRE, FIRE_WALL, RETURN,
        SATCHEL, FRAG, HUNTER_BOT, ROCKET, BARRIER, SLOW, REVIVE, TURRET, TRAP, SENSOR, LOCKDOWN, ECLIPSE, VERDICT, BULWARK, ANCHOR, RAIL, TRIPWIRE, CAGE, SPYCAM, NETWORK }
    enum Category { PISTOL("PISTOLAS"),SMG("SUBMETRALHADORAS"),RIFLE("FUZIS"),SHOTGUN("ESCOPETAS"),PRECISION("PRECISÃO"),HEAVY("PESADAS");final String label;Category(String label){this.label=label;} }
    enum FireMode { SEMI("SEMIAUTOMÁTICA"),AUTO("AUTOMÁTICA"),BURST("RAJADA DE 3"),PUMP("AÇÃO POR BOMBA");final String label;FireMode(String label){this.label=label;} }
    enum Weapon {
        SPARK("SPARK",Category.PISTOL,FireMode.SEMI,0,12,48,26,82,.23,1.45,.010,1,0xB5C6C8,30,.16,1,19),
        VEIL("VEIL",Category.PISTOL,FireMode.SEMI,500,15,60,30,105,.20,1.65,.007,1,0x88BEB4,30,.13,1,19),
        TALON("TALON",Category.PISTOL,FireMode.SEMI,800,6,30,55,159,.43,2.25,.009,1,0xD9B17E,30,.38,.96,20),
        RUSH("RUSH",Category.PISTOL,FireMode.AUTO,450,20,80,22,70,.071,1.70,.022,1,0xE1907D,18,.18,1,18),
        DART("DART",Category.PISTOL,FireMode.SEMI,350,2,20,11,18,.36,1.85,.092,12,0xAC9C81,6,.42,1,15),
        WISP("WISP",Category.SMG,FireMode.AUTO,1600,24,96,23,69,.082,1.9,.018,1,0x9DD9C2,24,.16,1,21),
        CIRCUIT("CIRCUIT",Category.SMG,FireMode.AUTO,1200,32,96,21,63,.066,2.1,.022,1,0x85C6D2,20,.18,1.02,20),
        ECHO("ECHO",Category.RIFLE,FireMode.AUTO,2700,25,75,35,156,.115,2.15,.010,1,0xCAD2D0,30,.16,.96,21),
        SHADE("SHADE",Category.RIFLE,FireMode.AUTO,2900,30,90,31,124,.094,2.25,.009,1,0x9EB2CE,25,.13,.95,23),
        HELIX("HELIX",Category.RIFLE,FireMode.BURST,2100,24,72,34,132,.075,2.20,.008,1,0xCDA782,30,.12,.96,25),
        MARROW("MARROW",Category.SHOTGUN,FireMode.PUMP,1850,8,24,13,24,.72,2.5,.072,9,0xDCB48A,10,.38,.95,17),
        BREACH("BREACH",Category.SHOTGUN,FireMode.AUTO,2200,7,28,10,19,.34,2.8,.078,10,0xB2BB8B,8,.29,.90,17),
        RIDGE("RIDGE",Category.PRECISION,FireMode.SEMI,2250,12,36,65,195,.32,2.3,.004,1,0xB4BE9F,45,.31,.92,39),
        HORIZON("HORIZON",Category.PRECISION,FireMode.SEMI,4200,5,20,100,170,1.12,2.6,.005,1,0xABA3CE,95,.48,.85,57),
        BASTION("BASTION",Category.HEAVY,FireMode.AUTO,3200,75,150,32,104,.105,3.7,.024,1,0xB7C3AB,35,.20,.80,24);
        final String label,type;final Category category;final FireMode mode;
        final int price,mag,reserve,body,head,pellets,color;final double interval,reload,spread,range,kick,mobility,zoom;
        Weapon(String label,Category category,FireMode mode,int price,int mag,int reserve,int body,int head,double interval,double reload,double spread,int pellets,int color,double range,double kick,double mobility,double zoom) {
            this.label=label;this.category=category;this.type=category.label;this.mode=mode;this.price=price;this.mag=mag;this.reserve=reserve;this.body=body;this.head=head;
            this.interval=interval;this.reload=reload;this.spread=spread;this.pellets=pellets;this.color=color;this.range=range;this.kick=kick;this.mobility=mobility;this.zoom=zoom;
        }
        boolean sidearm(){return category==Category.PISTOL;}
        boolean scoped(){return zoom>=35;}
        boolean silenced(){return this==VEIL||this==SHADE;}
        double damageAt(double distance,boolean headshot){double fall=pellets>1?Settings.clamp(1-(distance-range)/25,.18,1):distance>range?.82:1;return (headshot?head:body)*fall;}
        String sound(){return "shot_"+name();}
        String detail(){return switch(this){case SPARK->"Pistola leve, um disparo por clique.";case VEIL->"Silenciador, precisão e pouca dispersão.";case TALON->"Revólver de seis tiros e alto impacto.";case RUSH->"Pistola automática para combate próximo.";case DART->"Dois canos, 12 projéteis por disparo.";case WISP->"Controle e mobilidade em distâncias curtas.";case CIRCUIT->"Alta cadência e carregador de 32 tiros.";case ECHO->"Fuzil de alto dano e recuo progressivo.";case SHADE->"Fuzil silenciado, estável e de tiro rápido.";case HELIX->"Três disparos por clique, com pausa entre rajadas.";case MARROW->"Escopeta de bombeamento e nove projéteis.";case BREACH->"Escopeta automática com carregador destacável.";case RIDGE->"Fuzil semiautomático com luneta curta.";case HORIZON->"Precisão pesada, luneta e ação por ferrolho.";case BASTION->"75 tiros, cadência sustentada e movimento pesado.";};}
    }
    enum Agent {
        VERTICE("VÉRTICE","DUELISTA","Abra espaço com vento e velocidade.",Ability.DASH,2,Ability.QUICK_SMOKE,2,Ability.UPDRAFT,2,Ability.FOCUS,0xC8E7E3),
        BRUMA("BRUMA","CONTROLADORA","Trace o campo de batalha do alto.",Ability.SMOKE,3,Ability.INCENDIARY,1,Ability.STIM,1,Ability.ORBITAL,0x91BAAC),
        ION("ÍON","INICIADOR","Informação, clarões e supressão.",Ability.SCAN,2,Ability.FLASH,2,Ability.SUPPRESS,1,Ability.SURGE,0xAAADE7),
        CAUSTICA("CÁUSTICA","CONTROLADORA","Veneno corta rotas e enfraquece invasores.",Ability.TOXIC_WALL,1,Ability.ACID,2,Ability.POISON_CLOUD,1,Ability.TOXIC_DOME,0xADCE69),
        ESPECTRO("ESPECTRO","CONTROLADOR","Mude de ângulo sem ser visto.",Ability.BLINK,2,Ability.BLIND_WAVE,2,Ability.ECLIPSE,2,Ability.GLOBAL_TELEPORT,0x9497CE),
        SOLAR("SOLAR","DUELISTA","Fogo, curva e uma segunda chance.",Ability.CURVEFLASH,2,Ability.FIRE_WALL,1,Ability.HEAL_FIRE,1,Ability.RETURN,0xE8A56A),
        ESTOPIM("ESTOPIM","DUELISTA","Abra caminho com explosões e impulso.",Ability.SATCHEL,2,Ability.HUNTER_BOT,1,Ability.FRAG,2,Ability.ROCKET,0xDD9178),
        BASTILHA("BASTILHA","SENTINELA","Proteja, repare e traga a equipe de volta.",Ability.BARRIER,1,Ability.SLOW,2,Ability.HEAL,2,Ability.REVIVE,0x8FD5C7),
        VIGIA("VIGIA","SENTINELA","Defesa automatizada e controle de área.",Ability.TURRET,1,Ability.SENSOR,2,Ability.TRAP,2,Ability.LOCKDOWN,0xE2C374),
        AUREO("ÁUREO","SENTINELA","Precisão, proteção frontal e retirada calculada.",Ability.VERDICT,1,Ability.BULWARK,1,Ability.ANCHOR,1,Ability.RAIL,0xE9C879),
        TRAMA("TRAMA","SENTINELA","Vigie passagens e transforme pistas em informação.",Ability.TRIPWIRE,2,Ability.CAGE,2,Ability.SPYCAM,1,Ability.NETWORK,0x83CFE6);
        final String name,role,description;final Ability q,c,e,x;final int qs,cs,es,color;
        Agent(String n,String r,String d,Ability q,int qs,Ability c,int cs,Ability e,int es,Ability x,int color){name=n;role=r;description=d;this.q=q;this.qs=qs;this.c=c;this.cs=cs;this.e=e;this.es=es;this.x=x;this.color=color;}
        Ability slot(int s){return s==0?q:s==1?c:s==2?x:e;}
        int charges(int s){return s==0?qs:s==1?cs:s==3?es:6;}
    }
    static String abilityName(Ability a) {return switch(a){
        case DASH->"IMPULSO";case SMOKE->"NÉVOA";case HEAL->"REPARO";case SCAN->"PULSO";case FLASH->"CLARÃO";case FOCUS->"FOCO";case ORBITAL->"RUPTURA";case SURGE->"RESSONÂNCIA";
        case QUICK_SMOKE->"VÉU RÁPIDO";case UPDRAFT->"ASCENSÃO";case STIM->"ACELERADOR";case INCENDIARY->"BRASA";case SUPPRESS->"SILÊNCIO";
        case TOXIC_WALL->"CORTINA ÁCIDA";case POISON_CLOUD->"MANTO TÓXICO";case ACID->"CORROSÃO";case TOXIC_DOME->"BIOSFERA";
        case BLINK->"PASSO SOMBRIO";case BLIND_WAVE->"PESADELO";case GLOBAL_TELEPORT->"TRAVESSIA";case CURVEFLASH->"CENTELHA";case HEAL_FIRE->"REACENDER";case FIRE_WALL->"LABAREDA";case RETURN->"RENASCER";
        case SATCHEL->"PROPULSOR";case FRAG->"ESTILHAÇO";case HUNTER_BOT->"RASTREADOR";case ROCKET->"DEMOLIÇÃO";case BARRIER->"BASTIÃO";case SLOW->"GEADA";case REVIVE->"REANIMAR";
        case ECLIPSE->"ECLIPSE";case VERDICT->"VEREDITO";case BULWARK->"APÓLICE";case ANCHOR->"ÂNCORA";case RAIL->"EXECUÇÃO";case TRIPWIRE->"FIO DE ALARME";case CAGE->"REDE SURDA";case SPYCAM->"OLHO REMOTO";case NETWORK->"RASTREAMENTO";
        case TURRET->"TORRETA";case TRAP->"ARMADILHA";case SENSOR->"SONAR";case LOCKDOWN->"CONFINAMENTO";
    };}
    static String abilityDescription(Ability a) {return switch(a){
        case DASH->"Avança até 7 m em 0,40 s, respeitando paredes.";case SMOKE->"Mapa circular: marque até 26 m. Esferas ocas de 12 s.";case HEAL->"Mire em aliado para curar; sem alvo, cura você. 55 PV em 5 s.";case SCAN->"Revela inimigos até 30 m por 5 s.";case FLASH->"Granada com ricochete e perda de energia ao colidir. Detona em 1,1 s; mirando, arremesso curto de 0,65 s. Pode cegar você.";case FOCUS->"12 s: +30% cadência, -82% dispersão, recarga 40% mais rápida.";case ORBITAL->"Mapa: aviso de 1,2 s e 70 dano/s por 4 s, raio 5,5 m.";case SURGE->"Aliados até 24 m ganham 35 PV, 25 de proteção e cura gradual.";
        case QUICK_SMOKE->"Lança direto na mira. Abre em até 0,42 s; dura 4,5 s. Sem mapa.";case UPDRAFT->"Impulso vertical para chegar a plataformas e novos ângulos.";case STIM->"Área de 6 m por 10 s: aliados recebem +25% de cadência e velocidade.";case INCENDIARY->"Granada cria fogo por 6 s; causa 30 dano/s aos inimigos.";case SUPPRESS->"Disco arremessável suprime habilidades inimigas em 9 m por 5 s.";
        case TOXIC_WALL->"Cortina de 18 m bloqueia visão por 10 s; inimigos sofrem dano e lentidão.";case POISON_CLOUD->"Painel químico: marque piso a 22 m. Esfera tóxica oca por 10 s corrói invasores.";case ACID->"Poça de 5 s: 22 dano/s e vulnerabilidade de 50%.";case TOXIC_DOME->"Grande esfera tóxica de 8 m por 16 s. Marca invasores próximos.";
        case BLINK->"Passo de até 10 m após canalizar 0,45 s. Não atravessa paredes.";case BLIND_WAVE->"Onda atravessa paredes e reduz a visão inimiga por 3 s.";case GLOBAL_TELEPORT->"Escolha piso no mapa inteiro. Teleporta após 1,5 s; dano interrompe.";
        case CURVEFLASH->"Orbe faz uma curva para a direita; mirando, para a esquerda. Explode em 0,65 s.";case HEAL_FIRE->"Chama de 5 s cura você e causa dano aos inimigos.";case FIRE_WALL->"Parede de 16 m bloqueia visão e queima inimigos por 8 s.";case RETURN->"Marca sua posição por 10 s. Ao morrer ou acabar o tempo, retorna com vida.";
        case SATCHEL->"Explosão aos pés impulsiona você para frente e para cima.";case FRAG->"Granada com três explosões sucessivas em uma área de 5 m.";case HUNTER_BOT->"Robô segue rotas até um inimigo e explode ao aproximar.";case ROCKET->"Foguete direto com dano de até 180 em um raio de 6 m.";
        case BARRIER->"Ergue parede física de 7 m por 18 s; pode ser destruída a tiros.";case SLOW->"Granada cria campo de 5 m que reduz movimento em 60% por 7 s.";case REVIVE->"Mire perto de um aliado caído até 18 m para reanimá-lo com 100 PV.";
        case ECLIPSE->"Visão astral: posicione uma estrela a 30 m. Vórtice de 9 s reduz visão e apaga revelações.";
        case VERDICT->"Invoca pistola de 6 tiros: 70 no corpo / 160 na cabeça. Q reequipa enquanto houver munição.";
        case BULWARK->"Escudo frontal de 250 PV por 12 s. Intercepta tiros inimigos; jogadores atravessam.";
        case ANCHOR->"Posiciona âncora destrutível. E retorna até 22 m após 0,35 s; dano interrompe.";
        case RAIL->"Invoca fuzil de 5 tiros: 150 no corpo / 300 na cabeça, sem queda de dano.";
        case TRIPWIRE->"Fio de até 12 m. Prévia na parede e clique para instalar. Puxa, revela e atordoa; destrua as âncoras ou evite a altura do fio.";
        case CAGE->"Posiciona emissor. C ativa gaiolas de 7 s: bloqueiam visão na borda e alertam ao cruzar.";
        case SPYCAM->"Instala câmera na parede até 18 m. E entra/sai. Clique marca um alvo visível; seu corpo fica exposto.";
        case NETWORK->"Exige corpo inimigo a 12 m. Duas varreduras globais, separadas por 3 s, revelam por 1,2 s.";
        case TURRET->"Torreta destrutível de 100 PV. Atira com linha de visão por 24 s.";case TRAP->"Dispositivo ativa com inimigo a 3 m: lentidão e revelação.";case SENSOR->"Sensor acústico por 14 s: detecta corrida e tiros até 12 m. Andar devagar evita o alarme.";case LOCKDOWN->"Dispositivo destrutível: após 7 s, desarma e desacelera inimigos a 25 m por 6 s.";
    };}
    static final class Gun {
        final Weapon kind;int ammo,reserve,burstLeft;double reload,cooldown,cooldownDebt,reloadTotal,shotAge=9,pitchRecoil,yawRecoil,bloom;int sprayStep,reloadStage;
        Gun(Weapon w){kind=w;ammo=w.mag;reserve=w.reserve;reloadTotal=w.reload;}
        void tick(double dt){shotAge+=dt;if(shotAge>Combat.recovery(kind)){sprayStep=0;double decay=Math.exp(-dt*27);pitchRecoil*=decay;yawRecoil*=decay;bloom*=decay;}cooldownDebt=0;if(cooldown>0){cooldown-=dt;if(cooldown<0){cooldownDebt=-cooldown;cooldown=0;}}if(reload>0){reload-=dt;if(reload<=0){int take=Math.min(kind.mag-ammo,reserve);ammo+=take;reserve-=take;}}}
    }
    static final class Actor {
        final int id;int team;final String name;
        final Bots.Mind mind = new Bots.Mind();
        double x,y,z,yaw,pitch,vy,hp=100,armor,walk,flash,react,repath,respawn,healing,revealed,perception;
        double vx,vz,tagTime,landRecovery,nearSight,eRegen;double moveSpeed,objective,shotGlow,damageGlow,intentX,intentZ;int kills,deaths,credits=800,slot=1;
        int qCharges,cCharges,eCharges,ult=0,losses,agentIndex,aces; boolean crouch,dead,grounded=true,carrier;
        final Set<Integer> roundVictims=new HashSet<>();
        double eyeHeight=1.63,deathAge,healRate=11,emp,energy,slow,stim,vulnerable,detained,invulnerable,animSpeed,bodyYaw,animCrouch,lean,animTurn,skillCooldown;
        double returnTime,teleportTime;V returnPoint,teleportPoint;double returnYaw;
        V motionStart;
        Gun pistol=new Gun(Weapon.SPARK),primary,specialPistol,specialRifle; Actor target;List<V> path=List.of();int pathIndex;
        double destX=-1,destZ=-1; int lane;
        Actor(int id,int team,String name){this.id=id;this.team=team;this.name=name;lane=id%3;agentIndex=id%Agent.values().length;}
        boolean melee(){return slot==3;}
        Gun gun(){if(slot==4&&specialPistol!=null)return specialPistol;if(slot==5&&specialRifle!=null)return specialRifle;return slot==2 && primary!=null?primary:pistol;}
        V eye(){return new V(x,y+eyeHeight,z);}
        V center(){return new V(x,y+1,z);}
        V dir(){return new V(Math.sin(yaw)*Math.cos(pitch),Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));}
        double distance(Actor b){return Math.hypot(x-b.x,z-b.z);}
    }
    static final class Smoke {
        Ability style=Ability.SMOKE;final double x,z;double y=1.6,life=12,age,maxRadius=3.3,grow=.6;int color=0xC9DACD;
        Smoke(double x,double z){this.x=x;this.z=z;age=1;}
        Smoke(double x,double z,double delay){this.x=x;this.z=z;age=-delay;}
        double radius(){return maxRadius*smooth(age/grow)*smooth(life/.85);}
        static double smooth(double t){t=Settings.clamp(t,0,1);return t*t*(3-2*t);}
        boolean tick(double dt){double old=age;age+=dt;if(age>0)life-=old<0?age:dt;return life<=0;}
    }
    static final class Particle {
        double x,y,z,vx,vy,vz,life;final double duration,size;final int color;
        Particle(V p,V velocity,double life,double size,int color){x=p.x();y=p.y();z=p.z();vx=velocity.x();vy=velocity.y();vz=velocity.z();this.life=duration=life;this.size=size;this.color=color;}
        boolean tick(double dt){life-=dt;vy-=7*dt;x+=vx*dt;y+=vy*dt;z+=vz*dt;if(y<.035){y=.035;vy=Math.abs(vy)*.25;vx*=.7;vz*=.7;}return life<=0;}
    }
    static final class Orbital {final double x,z;final Actor owner;double age;Orbital(double x,double z,Actor owner){this.x=x;this.z=z;this.owner=owner;}double remaining(){return Math.max(0,5.2-age);}boolean active(){return age>=1.2&&age<5.2;}}
    static final class Pulse {final V at;final int color;final double maxRadius,duration;double age;Pulse(V at,int color,double max,double duration){this.at=at;this.color=color;maxRadius=max;this.duration=duration;}double radius(){return maxRadius*Settings.clamp(age/duration,0,1);}}
    static final class Trace {final V from,to;final int color;double life=.085;Trace(V a,V b,int col){from=a;to=b;color=col;} }
    record Feed(String killer,String victim,boolean head,int team,double expires) { }
    World world;
    boolean duel;
    volatile boolean multiplayerRequested;
    final Abilities abilities=new Abilities(this);
    final Combat combat=new Combat(this);
    final ShotEffects shotFX=new ShotEffects(this);
    final UltimateOrbs orbs=new UltimateOrbs(this);
    final Sentinels sentinels=new Sentinels(this);
    final MatchFlow flow=new MatchFlow(this);
    final Loadout loadout=new Loadout(this);
    final Spike spike=new Spike(this);
    final Rules rules=new Rules(this);
    final Economy economy=new Economy(this);
    final Bots bots;
    final Profile profile;
    final Settings settings;final Random rng;final AudioEngine audio;
    final List<Actor> actors=new ArrayList<>(); final List<Smoke> smokes=new ArrayList<>();
    final List<Trace> traces=new ArrayList<>();final List<Feed> feed=new ArrayList<>();
    final List<Particle> particles=new ArrayList<>();final List<Orbital> orbitals=new ArrayList<>();final List<Pulse> pulses=new ArrayList<>();
    final List<V> tacticalTargets=new ArrayList<>();
    Actor player;Agent agent; Phase phase=Phase.MENU;
    Settings.Action bindCapture;boolean aimLatched,walkLatched,crouchLatched,lastAim,bhopTrainer,fundsAsked;
    String helpReturn="menu";
    String ui="menu",backUi="menu",notice="",endTitle="",endReason="";
    int round=1,attackTeam=0,scoreBlue,scoreRed,roundWinner=-1,spectate=1,trainingHits,trainingShots;
    double timer=20,time,visualTime,noticeTime,plantProgress,defuseProgress,damageFlash,hitMarker,hitHead,recoil,focus,scan,footstep;
    double damageYaw;double aimLerp; boolean planted,training,scoreboard,aiming,observing;volatile boolean quit;
    double spikeX,spikeZ,spikeTime,spikeBeep; Actor defuser;
    int fps;long matchSeed;
    boolean pendingStart,pendingTraining,smokeRightHeld;String agentReturn="menu";
    Ability tacticalAbility=Ability.SMOKE;int tacticalSlot; // 0=Q, 1=C, 2=X
    double dashTime,dashX,dashZ,dashRecovery,weaponEquip,viewKick,kickVelocity,swayX,swayY,landing,abilityAnim,tacticalYaw;
    double aceTime,killToast,ultimateBurst,endAge;int lastKillCount,scanCount,flashCount,surgeCount;
    String aceName="",abilityToast="";int aceTeam;

    Game(Settings settings,boolean sound,long seed){this(settings,sound,seed,new World());}
    Game(Settings settings,boolean sound,long seed,World world){this.world=world;this.settings=settings;this.profile=new Profile(settings.path==null?null:settings.path.resolveSibling("profile.properties"));this.rng=new Random(seed);matchSeed=seed;agent=Agent.values()[settings.agent];audio=new AudioEngine(settings,sound);bots=new Bots(this,seed);world.setShadows(settings.shadows);}
    void openAgentSelect(boolean train,boolean launch){pendingStart=launch;pendingTraining=train;agentReturn=ui.equals("menu")?"menu":"play";tacticalTargets.clear();ui="agents";audio.play("ui");}
    boolean selectAgent(int index){
        if(index<0||index>=Agent.values().length)return false;
        if(!pendingStart&&!agentReturn.equals("menu")&&phase!=Phase.BUY&&phase!=Phase.TRAIN)return false;
        if(flow.locked)return false;
        if(!pendingStart&&agentReturn.equals("play")&&phase==Phase.BUY&&!profile.unlocked(index)){tell("Desbloqueie o contrato no menu de agentes",3);return false;}
        settings.agent=index;agent=Agent.values()[index];
        
        if(!pendingStart&&player!=null&&(phase==Phase.BUY||phase==Phase.TRAIN)){
            if(player.agentIndex!=index){abilities.removeOwned(player);player.specialPistol=player.specialRifle=null;if(player.slot>=4)player.slot=player.primary==null?1:2;}player.agentIndex=index;player.qCharges=agent.qs;player.cCharges=agent.cs;player.eCharges=agent.es;focus=scan=0;dashTime=dashRecovery=0;weaponEquip=.45;
        }
        settings.save();audio.play("select");return true;
    }
    void confirmAgent(){if(flow.accepted&&!pendingTraining){flow.lockAgent();return;}if(pendingStart){boolean train=pendingTraining;pendingStart=false;start(train);}else{ui=agentReturn;tell(agent.name+" selecionado",2);}}
    void start(boolean train) { start(train,false); }
    void startBots() { flow.mode=MatchFlow.Mode.UNRANKED;start(false,true);beginRound(); }
    void start(boolean train,boolean watch) {
        observing=watch;spectate=0;
        training=train;flow.rewarded=false;flow.accepted=false;flow.locked=false;abilities.clear();if(world.mapIndex!=flow.mapIndex)world=new World(flow.mapIndex);world.setShadows(settings.shadows);scoreBlue=scoreRed=0;round=1;time=0;actors.clear();smokes.clear();feed.clear();traces.clear();particles.clear();orbitals.clear();pulses.clear();
        agent=Agent.values()[settings.agent];
        actors.add(new Actor(0,0,observing?"VECTOR":"VOCÊ"));
        String[] names={"SABLE","KITE","ORBIT","EMBER","VEX","RUNE","FLINT","QUILL","ONYX"};
        for(int i=1;i<10;i++)actors.add(new Actor(i,!train&&flow.mode==MatchFlow.Mode.DEATHMATCH?i:i<5?0:1,names[i-1]));
        player=actors.get(0);player.agentIndex=settings.agent;trainingHits=trainingShots=0;ui="play";pendingStart=false;spawn(false);
        if(training) {
            phase=Phase.TRAIN;timer=0;player.credits=9999;player.primary=new Gun(Weapon.ECHO);player.slot=2;
            player.x=20;player.z=30;player.yaw=Math.PI;player.armor=50;player.carrier=false;
            for(Actor a:actors)if(a!=player) {
                if(a.team==0){a.dead=true;continue;}
                placeTarget(a);
            }
            tell("TREINO  •  G: agente  •  H: simular dano  •  B: arsenal  •  X: suprema livre",6);
        } else if(flow.mode.respawn){flow.startRespawn();} else tell("FASE DE COMPRA  •  B: arsenal  •  Enter: começar",5);
    }
    void placeTarget(Actor a) {
        int i=a.id-5;
        double[][] p={{5,17},{18,13},{20,19},{17,27},{5,27}};
        a.x=p[i][0];a.z=p[i][1];a.y=world.surfaceAt(a.x,a.z);a.hp=100;a.armor=0;a.dead=false;a.yaw=0;a.respawn=0;a.primary=null;a.carrier=false;a.deathAge=0;a.eyeHeight=1.63;a.flash=a.revealed=0;
    }
    void spawn(boolean carry) {
        abilities.clear();shotFX.clear();orbs.reset();fundsAsked=false;aimLatched=walkLatched=crouchLatched=lastAim=false;attackTeam=overtime()?Math.floorMod(round-(flow.mode.target*2-1),2):((round-1)/flow.mode.half)%2; planted=false;spikeTime=0;plantProgress=defuseProgress=0;defuser=null;smokes.clear();traces.clear();particles.clear();orbitals.clear();pulses.clear();tacticalTargets.clear();
        focus=scan=recoil=damageFlash=hitMarker=aimLerp=0; aiming=false;roundWinner=-1;timer=flow.mode==MatchFlow.Mode.SPIKE_RUSH?8:20;phase=Phase.BUY;
        combat.reset();dashTime=dashRecovery=aceTime=killToast=ultimateBurst=landing=viewKick=kickVelocity=0;weaponEquip=.45;abilityAnim=0;
        boolean halftime=round==flow.mode.half+1;
        for(Actor a:actors) {
            boolean survive=carry&&!a.dead&&!halftime;
            a.dead=false;a.hp=100;a.y=0;a.vy=0;a.grounded=true;a.crouch=false;a.flash=a.healing=a.revealed=0;
            a.path=List.of();a.pathIndex=0;a.repath=0;a.target=null;a.objective=0;a.react=0;a.destX=-1;a.destZ=-1;
            a.specialPistol=a.specialRifle=null;a.nearSight=a.eRegen=0;a.vx=a.vz=a.tagTime=a.landRecovery=0;a.shotGlow=a.damageGlow=0;a.moveSpeed=0;a.deathAge=0;a.eyeHeight=1.63;a.roundVictims.clear();a.energy=0;
            if(halftime||overtime()){a.credits=overtime()?5000:800;a.primary=null;a.armor=0;a.pistol=new Gun(Weapon.SPARK);a.slot=1;}
            if(!survive){a.armor=0;a.primary=null;a.slot=1;}
            a.pistol=new Gun(survive?a.pistol.kind:Weapon.SPARK);if(a.primary!=null)a.primary=new Gun(a.primary.kind);
            Agent kit=Agent.values()[a.agentIndex];if(!carry||training||flow.mode==MatchFlow.Mode.SPIKE_RUSH){a.qCharges=kit.qs;a.cCharges=kit.cs;}a.eCharges=kit.es;a.ult=Math.min(6,a.ult+(carry?1:0));
            int index=a.id==0?2:(a.id<5?(a.id<3?a.id-1:a.id):a.id-5);
            boolean atk=a.team==attackTeam;
            a.x=64+index*2;a.z=atk?120+(index%2)*1.3:5+(index%2)*1.3;a.yaw=atk?Math.PI:0;a.pitch=0;
            a.carrier=atk&&(a.id==0||a.id==5);
            if(a.carrier){spikeX=a.x;spikeZ=a.z;}
            a.skillCooldown=2+a.id*.8;a.slow=a.emp=a.stim=a.vulnerable=a.detained=a.invulnerable=a.returnTime=a.teleportTime=0;a.bodyYaw=a.yaw;a.animSpeed=a.animCrouch=0;
            if(overtime()){a.credits=5000;a.primary=null;a.armor=0;a.slot=1;}if(a.id!=0||observing){equipBot(a);if(carry){a.qCharges=kit.qs;a.cCharges=kit.cs;}}if(flow.mode==MatchFlow.Mode.SPIKE_RUSH){a.primary=new Gun(new Weapon[]{Weapon.WISP,Weapon.HELIX,Weapon.MARROW,Weapon.ECHO}[Math.floorMod(round-1,4)]);a.slot=2;a.armor=50;a.ult=6;}
        }
        bots.beginRound();
        ui="play";
        if(overtime())tell(round>=19?"MORTE SÚBITA  •  Esta rodada decide a partida":"PRORROGAÇÃO  •  Vença por duas  •  5.000 créditos",5);else if(halftime)tell("TROCA DE LADOS  •  Economia reiniciada",5);
    }
    void beginRound(){if(phase==Phase.BUY){phase=Phase.LIVE;timer=flow.mode==MatchFlow.Mode.SPIKE_RUSH?100:140;ui="play";tell(attackTeam==0?"ATAQUE  •  Leve o núcleo ao ponto A ou B":"DEFESA  •  Proteja A e B",3);audio.play("start");}}
    void tick(double dt,Input.Frame in) {
        visualTime+=dt;audio.menu=player==null||ui.equals("menu")||ui.equals("modes")||ui.equals("agents");
        if(bindCapture!=null){if(in.pressed(VK_ESCAPE)){bindCapture=null;return;}int key=in.edges().nextSetBit(0);if(key>=0){if(settings.bind(bindCapture,key)){bindCapture=null;settings.save();}else tell("Tecla reservada. Escolha outra.",2);}return;}
        if(in.pressed(VK_F10)){if(ui.equals("settings")){settings.save();ui=backUi;}else{backUi=ui.equals("play")?"play":"menu";ui="settings";}return;}
        if(in.pressed(VK_F1)){if(ui.equals("help"))ui=helpReturn;else{helpReturn=ui;ui="help";}return;}
        if(!ui.equals("agents")&&!ui.equals("settings")&&!ui.equals("menu"))in=settings.remap(in);
        if(sentinels.placingWire&&in.pressed(VK_ESCAPE)){sentinels.cancelWire();return;}
        if(sentinels.watching()&&in.pressed(VK_ESCAPE)){sentinels.leave();return;}if(flow.tick(dt,in))return;
        if(in.pressed(VK_ESCAPE)) {
            if(ui.equals("play")){ui="pause";audio.play("ui");}
            else if(ui.equals("pause")||ui.equals("shop")){ui="play";}
            else if(ui.equals("tactical")){cancelTactical();}
            else if(ui.equals("collection")){ui=backUi;}
            else if(ui.equals("settings")){settings.save();ui=backUi;}
            else if(ui.equals("agents")){pendingStart=false;flow.accepted=false;flow.locked=false;ui=agentReturn;}
            else if(ui.equals("help"))ui=helpReturn;
            return;
        }
        if(ui.equals("agents")){
            for(int i=0;i<9;i++)if(in.pressed(VK_1+i))selectAgent(i);
            if(in.pressed(VK_ENTER))confirmAgent();return;
        }
        if(ui.equals("collection")||ui.equals("menu")||ui.equals("settings")||ui.equals("agents")||ui.equals("help")||ui.equals("pause"))return;
        if(!observing&&in.pressed(VK_G)&&(phase==Phase.BUY||phase==Phase.TRAIN)&&!player.dead&&dashTime<=0){openAgentSelect(training,false);return;}
        if(training&&in.pressed(VK_H)&&!player.dead){player.hp=Math.max(5,player.hp-35);tell("Treino: -35 de vida para testar reparo e ressonância",3);}
        if(ui.equals("tactical")){
            if(in.aim()&&!smokeRightHeld&&!tacticalTargets.isEmpty()){tacticalTargets.remove(tacticalTargets.size()-1);audio.play("ui");}
            smokeRightHeld=in.aim();
            if(in.pressed(VK_ENTER)||settings.abilityHold&&!in.held(tacticalSlot==0?VK_Q:tacticalSlot==1?VK_C:tacticalSlot==3?VK_E:VK_X)){if(tacticalTargets.isEmpty())cancelTactical();else deployTactical();}
            if(in.pressed(tacticalSlot==0?VK_Q:tacticalSlot==1?VK_C:tacticalSlot==3?VK_E:VK_X)){cancelTactical();in=Input.Frame.empty();}
        }
        if(!observing&&in.pressed(VK_B) && (phase==Phase.BUY||phase==Phase.TRAIN||flow.mode.respawn)&&dashTime<=0)ui=ui.equals("shop")?"play":"shop";
        if(in.pressed(VK_ENTER)) {if(phase==Phase.BUY)beginRound();else if(phase==Phase.MATCH){flow.play();return;}}
        if(!observing){if(in.pressed(VK_K))quickBuy();if(in.pressed(VK_L))requestFunds();}
        if(training&&in.pressed(VK_T)){bhopTrainer=!bhopTrainer;combat.hops=0;combat.bestSpeed=0;tell(bhopTrainer?"BHOP  •  Espaço no pouso + A/D e giro do mouse":"TREINO DE MIRA",4);}
        time+=dt;noticeTime=Math.max(0,noticeTime-dt);damageFlash=Math.max(0,damageFlash-dt*2);hitMarker=Math.max(0,hitMarker-dt);hitHead=Math.max(0,hitHead-dt);
        focus=Math.max(0,focus-dt);scan=Math.max(0,scan-dt);recoil=player.melee()?0:player.gun().bloom*30;combat.tick(dt);
        dashRecovery=Math.max(0,dashRecovery-dt);weaponEquip=Math.max(0,weaponEquip-dt);landing=Math.max(0,landing-dt*4);abilityAnim=Math.max(0,abilityAnim-dt);ultimateBurst=Math.max(0,ultimateBurst-dt);
        aceTime=Math.max(0,aceTime-dt);killToast=Math.max(0,killToast-dt);kickVelocity+=(-95*viewKick-18*kickVelocity)*dt;viewKick+=kickVelocity*dt;
        traces.removeIf(t->(t.life-=dt)<=0);smokes.removeIf(s->s.tick(dt));feed.removeIf(f->f.expires<time);particles.removeIf(p->p.tick(dt));pulses.removeIf(p->(p.age+=dt)>=p.duration);
        shotFX.tick(dt);scoreboard=in.held(VK_TAB);
        for(Actor a:actors){
            a.motionStart=new V(a.x,a.y,a.z);
            if(!a.dead&&a.eCharges==0&&sentinels.own(a,Agent.values()[a.agentIndex].e)==null){a.eRegen+=dt;if(a.eRegen>=30){a.eRegen=0;a.eCharges=1;if(a==player)tell("Assinatura recarregada",2);}}else a.eRegen=0;
            a.nearSight=Math.max(0,a.nearSight-dt);if(a.specialPistol!=null)a.specialPistol.tick(dt);if(a.specialRifle!=null)a.specialRifle.tick(dt);a.tagTime=Math.max(0,a.tagTime-dt);a.landRecovery=Math.max(0,a.landRecovery-dt);a.pistol.tick(dt);if(a.primary!=null)a.primary.tick(dt);a.shotGlow=Math.max(0,a.shotGlow-dt);a.damageGlow=Math.max(0,a.damageGlow-dt);a.flash=Math.max(0,a.flash-dt);a.revealed=Math.max(0,a.revealed-dt);a.energy=Math.max(0,a.energy-dt);a.emp=Math.max(0,a.emp-dt);a.slow=Math.max(0,a.slow-dt);a.stim=Math.max(0,a.stim-dt);a.vulnerable=Math.max(0,a.vulnerable-dt);a.detained=Math.max(0,a.detained-dt);a.invulnerable=Math.max(0,a.invulnerable-dt);
            CharacterModel.animate(a,dt);abilities.tickActor(a,dt);
            if(a.dead)a.deathAge+=dt;
            if(a.healing>0&&!a.dead){double healTime=Math.min(a.healing,dt);a.healing-=healTime;a.hp=Math.min(100,a.hp+a.healRate*healTime);}
        }
        if(phase==Phase.MATCH)return;
        if(phase==Phase.END){endAge+=dt;for(Orbital o:orbitals)o.age+=dt;orbitals.removeIf(o->o.age>=5.2);timer-=dt;if(timer<=0){if(matchFinished()){flow.complete(scoreBlue>scoreRed);}else{round++;spawn(true);}}return;}
        if(phase==Phase.BUY){timer-=dt;if(timer<=0)beginRound();}
        boolean playing=ui.equals("play");
        if(playing&&!player.dead&&!observing){if(sentinels.watching())sentinels.control(dt,in);else controlPlayer(dt,in);}
        else {aiming=false;aimLerp=Math.max(0,aimLerp-dt*8);plantProgress=0;if(!observing)player.gun().burstLeft=0;}
        if((player.dead||observing) && in.pressed(VK_SPACE))spectate++;
        if(phase==Phase.LIVE) {
            timer-=dt;
            bots.beginFrame();
            for(Actor a:actors)if((a.id!=0||observing)&&!a.dead){bot(a,dt);actorGravity(a,dt);}
            tickOrbitals(dt);abilities.tick(dt);if(!flow.mode.respawn)orbs.tick(dt,!observing&&playing&&in.held(VK_F));if(flow.mode.respawn)flow.tickRespawn(dt);else{updateSpike(dt,!observing&&playing&&in.held(VK_F)&&dashTime<=0);checkRules();}
        }
        if(phase==Phase.TRAIN){tickOrbitals(dt);abilities.tick(dt);orbs.tick(dt,!observing&&playing&&in.held(VK_F));for(Actor a:actors)if(a.team==1&&a.dead){a.respawn-=dt;if(a.respawn<=0)placeTarget(a);}}
    }
    void controlPlayer(double dt,Input.Frame in) {
        player.yaw+=in.dx()*settings.sensitivity*(aiming?settings.adsSensitivity:1);
        player.pitch=Settings.clamp(player.pitch+in.dy()*settings.sensitivity*(settings.invertY?1:-1)*(aiming?settings.adsSensitivity:1),-1.35,1.35);
        if(in.held(VK_LEFT))player.yaw-=dt*1.8;if(in.held(VK_RIGHT))player.yaw+=dt*1.8;
        if(in.held(VK_UP))player.pitch=Math.min(1.35,player.pitch+dt);if(in.held(VK_DOWN))player.pitch=Math.max(-1.35,player.pitch-dt);
        boolean toggle=player.gun().kind.scoped()?settings.sniperToggle:settings.aimToggle;
        if(in.aim()&&!lastAim&&toggle&&!sentinels.placingWire&&!player.melee()&&weaponEquip<=0)aimLatched=!aimLatched;lastAim=in.aim();
        aiming=!sentinels.placingWire&&!player.melee()&&(toggle?aimLatched:in.aim())&&player.gun().reload<=0&&weaponEquip<=0&&dashTime<=0&&dashRecovery<=0;if(aiming)combat.inspect=0;aimLerp+=((aiming?1:0)-aimLerp)*Math.min(1,dt*12);
        swayX+=(Settings.clamp(-in.dx()*.0012,-.035,.035)-swayX)*Math.min(1,dt*10);swayY+=(Settings.clamp(in.dy()*.001,-.025,.025)-swayY)*Math.min(1,dt*10);
        if(in.pressed(VK_CONTROL))crouchLatched=!crouchLatched;if(in.pressed(VK_SHIFT))walkLatched=!walkLatched;
        player.crouch=settings.crouchToggle?crouchLatched:in.held(VK_CONTROL);double forward=(in.held(VK_W)?1:0)-(in.held(VK_S)?1:0),side=(in.held(VK_D)?1:0)-(in.held(VK_A)?1:0);
        player.eyeHeight+=((player.crouch?1.12:1.63)-player.eyeHeight)*Math.min(1,dt*13);
        double l=Math.hypot(forward,side);if(l>0){forward/=l;side/=l;}
        player.intentX=Math.sin(player.yaw)*forward+Math.cos(player.yaw)*side;player.intentZ=Math.cos(player.yaw)*forward-Math.sin(player.yaw)*side;
        double speed=(player.teleportTime>0?0:1)*(player.crouch?1.8:(settings.walkToggle?walkLatched:in.held(VK_SHIFT))?2.45:player.melee()?6.2:5.4)*(aiming?.72:1)*(focus>0?1.20:1)*(player.stim>0?1.25:1)*(player.slow>0?.4:1)*(player.detained>0?.35:1)*(player.tagTime>0?.60:1)*(player.landRecovery>0?.80:1)*(player.melee()?1:player.gun().kind.mobility);
        if(phase==Phase.LIVE && in.held(VK_F) && (planted || player.carrier&&world.site(player.x,player.z)!=null))speed=0;
        combat.jump(dt,in);double ox=player.x,oz=player.z;if(dashTime>0){player.vx=player.vz=0;advanceDash(dt);}else combat.movement(dt,speed);
        if(phase==Phase.BUY && (attackTeam==0?player.z<113:player.z>10)){player.x=ox;player.z=oz;}
        player.moveSpeed=Math.hypot(player.x-ox,player.z-oz)/dt;player.walk+=player.moveSpeed*dt;
        footstep-=dt;if(footstep<=0&&player.moveSpeed>3.3&&player.grounded){audio.play("step");bots.noise(player,Bots.Noise.STEP,19);footstep=.36;}
        
        double oldY=player.y;player.vy-=16*dt;player.y+=player.vy*dt;double floor=world.groundAt(player.x,player.z,oldY);
        if(player.y<=floor){if(!player.grounded&&player.vy<-3){combat.landed();player.landRecovery=.11;landing=Math.min(1,Math.abs(player.vy)*.08);audio.play("land");bots.noise(player,Bots.Noise.LAND,Math.min(28,8+Math.abs(player.vy)*1.3));}player.y=floor;player.vy=0;player.grounded=true;}else player.grounded=false;
        if(in.pressed(VK_1))combat.equip(1);if(in.pressed(VK_2))combat.equip(2);if(in.pressed(VK_3))combat.equip(3);
        if(in.pressed(VK_V)&&weaponEquip<=0&&combat.swing<=0&&player.gun().reload<=0)combat.inspect=2.4;
        if(in.pressed(VK_R)&&dashTime<=0)reload(player);
        if(phase==Phase.LIVE||phase==Phase.TRAIN) {
            if(in.pressed(VK_Q))castSlot(0);
            if(in.pressed(VK_C))castSlot(1);
            if(in.pressed(VK_X))castSlot(2);
            if(in.pressed(VK_E))castSlot(3);
            if(in.pressed(VK_Z))abilities.ping();
            if(sentinels.watching())return;
            if(sentinels.placingWire){sentinels.preview=sentinels.wire(player);if(in.aim())sentinels.cancelWire();else if(in.click())sentinels.confirmWire();return;}
            if(!ui.equals("play"))return;
            if(player.melee()){if(in.fire())combat.attack(false);if(in.aim()&&!combat.rightHeld)combat.attack(true);combat.rightHeld=in.aim();return;}
            Gun gun=player.gun();boolean trigger=gun.burstLeft>0||in.fire()&&(gun.kind.mode==FireMode.AUTO||gun.kind.mode==FireMode.PUMP||in.click());
            if(trigger&&ui.equals("play")&&dashTime<=0&&dashRecovery<=0&&player.detained<=0&&player.teleportTime<=0&&weaponEquip<=0&&gun.reload<=0&&gun.cooldown<=0&&!(plantProgress>0||defuser==player))firePlayer();
        }
    }
    void move(Actor a,double dx,double dz) {
        int steps=Math.max(1,(int)(Math.hypot(dx,dz)/.15)+1);
        for(int i=0;i<steps;i++){
            double nx=a.x+dx/steps,nz=a.z+dz/steps;double h=world.groundAt(nx,a.z,a.y);
            if(!world.blocked(nx,a.z,Math.max(a.y,h),.31,a.crouch?1.2:1.78))a.x=nx;
            h=world.groundAt(a.x,nz,a.y);if(!world.blocked(a.x,nz,Math.max(a.y,h),.31,a.crouch?1.2:1.78))a.z=nz;
            double floor=world.groundAt(a.x,a.z,a.y);if(a.grounded&&floor>=a.y-.18&&floor<=a.y+.35)a.y=floor;else if(floor<a.y-.18)a.grounded=false;
        }
    }
    void actorGravity(Actor a,double dt){double old=a.y;a.vy-=16*dt;a.y+=a.vy*dt;double floor=world.groundAt(a.x,a.z,old);if(a.y<=floor){a.y=floor;a.vy=0;a.grounded=true;}else a.grounded=false;}
    void castSlot(int slot){
        if(player.emp>0||player.detained>0||player.teleportTime>0){tell("Habilidades indisponíveis durante este efeito",1.5);return;}
        if(dashTime>0){tell("Aguarde o fim do impulso",1);return;}
        if(sentinels.recast(agent.slot(slot)))return;
        if(slot==2&&player.ult<6&&!training){tell("Suprema: "+player.ult+" / 6 pontos",2);audio.play("deny");return;}
        if(slot!=2&&charges(slot)<=0){tell("Sem cargas nesta rodada",2);audio.play("deny");return;}
        tacticalSlot=slot;Ability type=agent.slot(slot);
        if(type==Ability.TRIPWIRE){if(sentinels.placingWire)sentinels.confirmWire();else sentinels.beginWire();return;}
        if(ability(type)){
            if(!training){if(slot==0)player.qCharges--;else if(slot==1)player.cCharges--;else if(slot==3)player.eCharges--;else player.ult=0;}
            if(slot==2){ultimateBurst=1.2;audio.play("ultimate");}
        }
    }
    void advanceDash(double dt){
        double before=dashTime,after=Math.max(0,before-dt),t0=1-before/.40,t1=1-after/.40;
        double distance=7*(Smoke.smooth(t1)-Smoke.smooth(t0)),ox=player.x,oz=player.z;
        move(player,dashX*distance,dashZ*distance);dashTime=after;
        particle(new V(player.x,player.y+.4,player.z),new V(-dashX*1.5,.8,-dashZ*1.5),.45,.05,0xB8EEDB);
        if(Math.hypot(player.x-ox,player.z-oz)<distance*.25&&distance>.01)dashTime=0;
        if(dashTime<=0){dashRecovery=.16;landing=.26;}
    }
    boolean ability(Ability ability) {
        switch(ability) {
            case DASH -> {
                if(dashTime>0)return false;double ix=player.intentX,iz=player.intentZ;if(Math.hypot(ix,iz)<.1){ix=Math.sin(player.yaw);iz=Math.cos(player.yaw);}
                if(world.blocked(player.x+ix*.45,player.z+iz*.45,player.y,.31,player.crouch?1.2:1.78)){tell("Sem espaço para o impulso",2);return false;}
                dashX=ix;dashZ=iz;dashTime=.40;aiming=false;player.gun().burstLeft=0;audio.play("dash");
            }
            case SMOKE, ORBITAL, GLOBAL_TELEPORT, POISON_CLOUD, ECLIPSE -> {openTactical(ability);return false;}
            case HEAL -> {Actor target=abilities.healTarget();if(target.hp>=100){tell(training?"Vida cheia  •  H simula dano no treino":"Sua vida já está cheia",2);return false;}target.healing=5;target.healRate=11;target.energy=5;pulses.add(new Pulse(target.center(),0x8DDFC3,4,1.1));audio.play("heal");}
            case SCAN -> {scan=5;scanCount=0;for(Actor a:actors)if(!a.dead&&a.team!=player.team&&player.distance(a)<30){a.revealed=5;scanCount++;}pulses.add(new Pulse(player.center(),0xA4B9FF,30,1.3));audio.play("scan");}
            case FLASH -> {abilities.launch(player,ability);}
            case FOCUS -> {focus=12;recoil=0;weaponEquip=.22;player.energy=12;pulses.add(new Pulse(player.center(),0xFFDE8D,5,.7));audio.play("focus");}
            case SURGE -> {
                surgeCount=0;for(Actor a:actors)if(!a.dead&&a.team==player.team&&player.distance(a)<=24){a.hp=Math.min(100,a.hp+35);a.armor=Math.min(50,a.armor+25);a.healing=4;a.healRate=10;a.energy=4;surgeCount++;}
                pulses.add(new Pulse(player.center(),0xC4BEFF,24,1.5));audio.play("surge");
            }
            default -> {if(!abilities.cast(ability))return false;}
        }
        abilities.effects.add(new Abilities.Effect(ability,player.center(),player.yaw,1.2));abilityAnim=.65;abilityToast=abilityName(ability);
        tell(switch(ability){case SCAN->"PULSO  •  "+scanCount+" inimigo(s) detectado(s)";case FLASH->"CLARÃO  •  Granada lançada: desvie o olhar!";case FOCUS->"FOCO  •  +30% cadência  •  -82% dispersão  •  recarga acelerada";case SURGE->"RESSONÂNCIA  •  "+surgeCount+" aliado(s) energizado(s)";default->abilityName(ability)+" ativado";},2.8);
        return true;
    }
    void openTactical(Ability type){tacticalAbility=type;tacticalTargets.clear();smokeRightHeld=false;aiming=aimLatched=false;sentinels.cancelWire();tacticalYaw=player.yaw;player.gun().burstLeft=0;ui="tactical";audio.play("map");}
    double tacticalRange(){return tacticalAbility==Ability.GLOBAL_TELEPORT?Math.hypot(World.WIDTH,World.LENGTH):tacticalAbility==Ability.ORBITAL?36:tacticalAbility==Ability.POISON_CLOUD?22:tacticalAbility==Ability.ECLIPSE?30:26;}
    double tacticalRadius(){return tacticalAbility==Ability.GLOBAL_TELEPORT?1:tacticalAbility==Ability.ORBITAL?5.5:tacticalAbility==Ability.POISON_CLOUD?3.8:tacticalAbility==Ability.ECLIPSE?4:3.3;}
    int charges(int slot){return slot==0?player.qCharges:slot==1?player.cCharges:slot==2?player.ult:player.eCharges;}
    int tacticalCapacity(){return tacticalSlot==2||tacticalAbility==Ability.POISON_CLOUD||tacticalAbility==Ability.ECLIPSE?1:training?agent.charges(tacticalSlot):charges(tacticalSlot);}
    boolean validTarget(double x,double z){return player!=null&&!player.dead&&Math.hypot(x-player.x,z-player.z)<=tacticalRange()&&x>1&&z>1&&x<World.WIDTH-1&&z<World.LENGTH-1&&!world.blocked(x,z,world.surfaceAt(x,z),.3,1.8);}
    boolean markTarget(double x,double z){
        if(!ui.equals("tactical")||!validTarget(x,z)){tell("Selecione piso livre dentro do alcance",2);audio.play("deny");return false;}
        for(int i=0;i<tacticalTargets.size();i++)if(Math.hypot(tacticalTargets.get(i).x()-x,tacticalTargets.get(i).z()-z)<1.8){tacticalTargets.remove(i);audio.play("ui");return true;}
        if(tacticalTargets.size()>=tacticalCapacity()){tell("Limite de marcações  •  botão direito desfaz a última",2);return false;}
        tacticalTargets.add(new V(x,world.surfaceAt(x,z),z));audio.play("mark");return true;
    }
    boolean deployTactical(){
        if(!ui.equals("tactical")||player==null||player.dead||tacticalTargets.isEmpty()||!(phase==Phase.LIVE||phase==Phase.TRAIN))return false;
        if(tacticalTargets.size()>tacticalCapacity())return false;
        for(V v:tacticalTargets)if(!validTarget(v.x(),v.z()))return false;
        int count=tacticalTargets.size();
        if(tacticalAbility==Ability.GLOBAL_TELEPORT){if(player.ult<6&&!training)return false;abilities.teleport(player,tacticalTargets.get(0),1.5);if(!training)player.ult=0;
        }else if(tacticalAbility==Ability.ORBITAL){
            if(player.ult<6&&!training)return false;V v=tacticalTargets.get(0);orbitals.add(new Orbital(v.x(),v.z(),player));if(!training)player.ult=0;ultimateBurst=1.2;audio.play("orbital");
        }else{
            for(int i=0;i<count;i++){V p=tacticalTargets.get(i);
                if(tacticalAbility==Ability.POISON_CLOUD){abilities.smoke(p,3.8,10,0xB4CD85);abilities.zones.add(new Abilities.Zone(Ability.POISON_CLOUD,player,p,3.8,10));}
                else if(tacticalAbility==Ability.ECLIPSE){abilities.zones.add(new Abilities.Zone(Ability.ECLIPSE,player,p,4,9));abilities.effects.add(new Abilities.Effect(Ability.ECLIPSE,p,0,1));}
                else{Smoke s=new Smoke(p.x(),p.z(),.35+i*.15);s.y=p.y()+1.6;smokes.add(s);}}
            if(!training){if(tacticalSlot==0)player.qCharges-=count;else if(tacticalSlot==1)player.cCharges-=count;else player.eCharges-=count;}audio.play("smoke");
        }
        abilityAnim=.7;abilityToast=abilityName(tacticalAbility);tell(tacticalAbility==Ability.GLOBAL_TELEPORT?"TRAVESSIA  •  Canalizando por 1,5 s":tacticalAbility==Ability.ORBITAL?"RUPTURA  •  Impacto em 1,2 segundo":abilityName(tacticalAbility)+"  •  "+count+" ponto(s) confirmado(s)",3);tacticalTargets.clear();ui="play";return true;
    }
    void cancelTactical(){tacticalTargets.clear();ui="play";smokeRightHeld=false;}
    void tickOrbitals(double dt){
        for(Orbital o:orbitals){double old=o.age;o.age+=dt;double activeDt=Math.max(0,Math.min(o.age,5.2)-Math.max(old,1.2));
            if(activeDt>0){for(Actor a:actors)if(!a.dead&&a.team!=o.owner.team&&Math.hypot(a.x-o.x,a.z-o.z)<=5.5)damage(a,70*activeDt,o.owner,false);}
            if(old<1.2&&o.age>=1.2){audio.play("boom");pulses.add(new Pulse(new V(o.x,.1,o.z),0xFFAE67,5.5,.7));}
        }
        orbitals.removeIf(o->o.age>=5.2);
    }
    void particle(V at,V velocity,double life,double size,int color){if(particles.size()<170)particles.add(new Particle(at,velocity,life,size,color));}
    void reload(Actor a) {
        if(a.melee())return;Gun gun=a.gun();if(gun.reload>0||gun.ammo==gun.kind.mag||gun.reserve<=0)return;
        gun.burstLeft=0;gun.reloadTotal=gun.kind.reload*(a==player&&focus>0?.6:1);gun.reload=gun.reloadTotal;gun.reloadStage=0;bots.noise(a,Bots.Noise.RELOAD,13);if(a==player){combat.inspect=0;audio.play("reload");}
    }
    double playerSpread(){
        Gun gun=player.gun();Weapon w=gun.kind;
        double base=w.spread*(w.pellets>1?1:.30)*(aiming?.35:1)*(player.crouch?.8:1);
        if(w==Weapon.HORIZON&&!aiming)base+=.035;
        base+=Combat.movementError(player)*.018+(player.grounded?0:.10)+(player.landRecovery>0?.018:0)+gun.bloom;
        return base*(focus>0?.18:1);
    }
    void firePlayer() {
        if(player.melee())return;combat.inspect=0;Gun gun=player.gun();if(gun.ammo<=0){reload(player);return;}
        player.invulnerable=0;gun.ammo--;double interval=gun.kind.interval;
        if(gun.kind.mode==FireMode.BURST){if(gun.burstLeft==0)gun.burstLeft=3;gun.burstLeft--;if(gun.burstLeft==0)interval=.38;}if(gun.ammo==0)gun.burstLeft=0;
        gun.cooldown=Math.max(.001,interval/((focus>0?1.3:1)*(player.stim>0?1.25:1))-gun.cooldownDebt);gun.cooldownDebt=0;player.shotGlow=.065;trainingShots++;
        double spread=playerSpread();kickVelocity+=(.35+gun.kind.kick*3)*(focus>0?.3:1);

        audio.play(gun.kind.sound());shotFX.fired(player);bots.noise(player,Bots.Noise.SHOT,gun.kind.silenced()?34:64);
        for(int i=0;i<gun.kind.pellets;i++) {
            double yaw=player.yaw+gun.yawRecoil+(rng.nextDouble()-.5)*spread*2;
            double pitch=player.pitch+gun.pitchRecoil+(rng.nextDouble()-.5)*spread*2;
            V dir=new V(Math.sin(yaw)*Math.cos(pitch),Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));
            shoot(player,dir,gun.kind);
        }
        Combat.recoil(gun,player.crouch,focus>0);recoil=gun.bloom*30;combat.charmVelocity+=.9;
        if(training&&player.slot<4&&gun.reserve<gun.kind.reserve)gun.reserve=gun.kind.reserve;
    }
    void shoot(Actor shooter,V dir,Weapon gun) {
        V origin=shooter.eye();Ballistics.Shot shot=Ballistics.trace(world,origin,dir,gun);double closest=shot.distance();Actor victim=null;boolean head=false;
        for(Actor a:actors)if(!a.dead&&a.team!=shooter.team) {
            double height=a.crouch?1.18:1.78;
            double t=rayBox(origin,dir,new Box(a.x-.29,a.y+.12,a.z-.26,a.x+.29,a.y+height-.30,a.z+.26,0));
            double ht=raySphere(origin,dir,new V(a.x,a.y+height-.16,a.z),.235);
            if(ht>=0&&ht<closest&&(t<0||ht<=t)){closest=ht;victim=a;head=true;}
            else if(t>=0&&t<closest){closest=t;victim=a;head=false;}
        }
        double device=abilities.hitDevice(shooter,origin,dir,closest,shooter.slot==4?70:shooter.slot==5?150:gun.body);if(device>=0){closest=device;victim=null;head=false;}
        V endpoint=origin.add(dir.mul(closest));if(victim==null&&device<0)shotFX.hit(origin,dir,closest);
        traces.add(new Trace(origin.add(new V(Math.cos(shooter.yaw)*.15,-.19,-Math.sin(shooter.yaw)*.15)),endpoint,shooter.team==0?0xFFF2BE:0xFF997B));
        if(closest<209){for(int i=0;i<3;i++)particle(endpoint,new V((rng.nextDouble()-.5)*2,rng.nextDouble()*2,(rng.nextDouble()-.5)*2),.3+rng.nextDouble()*.25,.025,victim==null?0xE8D9B6:0xFFD19B);}
        if(victim!=null) {
            double amount=shooter.slot==4?(head?160:70):shooter.slot==5?(head?300:150):gun.damageAt(closest,head);
            if(!head&&endpoint.y()<victim.y+.65)amount*=.75;damage(victim,amount*shot.factorAt(closest),shooter,head);
            if(shooter==player){trainingHits++;hitMarker=.17;hitHead=head?.25:0;audio.play(head?"head":"hit");}
        }
    }
    static double raySphere(V o,V d,V center,double r){V oc=o.sub(center);double b=oc.dot(d),c=oc.dot(oc)-r*r,det=b*b-c;if(det<0)return -1;double t=-b-Math.sqrt(det);return t>=0?t:-1;}
    void damage(Actor a,double amount,Actor from,boolean head) {
        if(a.dead||a.invulnerable>0||from!=null&&a.team==from.team)return;
        if(a.vulnerable>0)amount*=1.5;
        if(a.teleportTime>0){a.teleportTime=0;if(a==player)tell("Travessia interrompida pelo dano",2);}
        double absorb=Math.min(a.armor,amount*.66);a.armor-=absorb;a.hp-=amount-absorb;a.damageGlow=.17;a.healing=0;a.tagTime=.55;
        bots.hurt(a,from);
        if(a==player){if(from!=null)damageYaw=Math.atan2(from.x-a.x,from.z-a.z);damageFlash=.65;audio.play("hurt");}
        if(a.hp<=0){
            if(a.returnTime>0){abilities.returnActor(a);return;}
            a.hp=0;a.dead=true;a.deaths++;a.respawn=flow.mode.respawn?3:2;a.objective=0;a.deathAge=0;
            if(a.carrier){a.carrier=false;spikeX=a.x;spikeZ=a.z;}
            if(from!=null){
                from.kills++;from.credits=Math.min(9000,from.credits+200);from.ult=Math.min(6,from.ult+1);feed.add(new Feed(from.name,a.name,head,from.team,time+5));
                boolean first=from.roundVictims.add(a.id);
                if(from==player){lastKillCount=from.roundVictims.size();killToast=2.4;audio.play("multi"+Math.min(5,lastKillCount));if(focus>0)focus=Math.min(12,focus+2);}
                if(first&&!training&&!flow.mode.respawn&&phase==Phase.LIVE&&from.roundVictims.size()==5){from.aces++;aceTime=6;aceName=from.name;aceTeam=from.team;audio.play("ace");}
            }
            if(a==player){aimLatched=false;aiming=false;aimLerp=0;plantProgress=0;dashTime=0;focus=0;if(ui.equals("tactical"))cancelTactical();tell(flow.mode.respawn?"Você caiu  •  Respawn em 3 segundos":"Você caiu  •  Espaço: trocar aliado  •  Tab: placar",5);}
        }
    }
    boolean obscured(V from,V to) {
        if(sentinels.cageObscures(from,to))return true;
        V line=to.sub(from);double length=line.dot(line);
        for(Smoke s:smokes){V c=new V(s.x,s.y,s.z);if(from.sub(c).length()<s.radius()-.15&&to.sub(c).length()<s.radius()-.15)continue;double t=Settings.clamp(c.sub(from).dot(line)/Math.max(.001,length),0,1);if(s.radius()>.35&&from.add(line.mul(t)).sub(c).length()<s.radius()*.94)return true;}
        return abilities.wallObscures(from,to);
    }
    boolean canSee(Actor from,Actor to){return (from.nearSight<=0||from.distance(to)<7)&&world.visible(from.eye(),to.center())&&!obscured(from.eye(),to.center());}
    void bot(Actor a,double dt) { bots.tick(a,dt); }
    void updateSpike(double dt,boolean interact){spike.updateSpike(dt,interact);}
    void plant(Actor a){spike.plant(a);}
    long living(int team){return rules.living(team);}
    void checkRules(){rules.checkRules();}
    void finishRound(int winner,String reason){rules.finishRound(winner,reason);}
    boolean buyWeapon(Weapon w){return loadout.buyWeapon(w);}
    void equipBot(Actor a){loadout.equipBot(a);}
    boolean buyArmor(int amount){return loadout.buyArmor(amount);}
    Actor cameraActor() {
        if(player==null)return null;
        if(duel)return player;
        if(observing){int live=0;for(Actor a:actors)if(!a.dead)live++;if(live==0)return player;int index=Math.floorMod(spectate,live);for(Actor a:actors)if(!a.dead&&index--==0)return a;}
        if(sentinels.watching())return sentinels.eye;
        if(!player.dead)return player;
        if(player.deathAge<.85)return player;
        List<Actor> live=actors.stream().filter(a->a.team==0&&!a.dead).toList();
        return live.isEmpty()?player:live.get(Math.floorMod(spectate,live.size()));
    }
    boolean buyAbility(int slot){return loadout.buyAbility(slot);}
    boolean overtime(){return rules.overtime();}
    boolean matchFinished(){return rules.matchFinished();}
    void quickBuy(){loadout.quickBuy();}
    void requestFunds(){economy.requestFunds();}
    void tell(String text,double seconds){notice=text;noticeTime=seconds;}
    void close(){settings.save();audio.close();}
}
