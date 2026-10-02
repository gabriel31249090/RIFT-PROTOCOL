package rift;

import java.util.EnumSet;
import static rift.Game.*;
import static rift.Tests.*;
import static rift.UpdateTests.*;

/** Shared weapon handling must behave identically for players, bots and tick rates. */
final class CombatTests {
    static void run(){profiles();recoil();legalCadence();recovery();damage();accuracy();storedWeapon();}

    static Actor actor(Weapon w){Actor a=new Actor(0,0,"handling-test");a.primary=new Gun(w);a.slot=2;return a;}
    // Clock-free stress state; legalCadence covers the actual trigger and shot intervals.
    static Gun burst(Weapon w,int count){Gun gun=new Gun(w);for(int i=0;i<count;i++)Combat.recoil(gun,false,false);return gun;}
    static boolean close(double a,double b){return Math.abs(a-b)<1e-11;}

    static void profiles(){
        EnumSet<WeaponHandling> found=EnumSet.noneOf(WeaponHandling.class);
        for(Weapon w:Weapon.values()){
            WeaponHandling h=w.handling;found.add(h);
            check(h.name().equals(w.name()),"Perfil exclusivo ligado a "+w.name());
            double[] values={h.rise,h.riseGrowth,h.riseCap,h.sideGrowth,h.sideCap,h.sideStart,h.sideFrequency,
                h.bloomGrowth,h.bloomCap,h.recoveryDelay,h.recoveryRate,h.moveSpread,h.airSpread,h.rangeRetention};
            boolean valid=true;for(double value:values)valid&=Double.isFinite(value)&&value>=0;
            check(valid&&h.rise>0&&h.riseCap>=h.rise&&h.recoveryRate>0&&h.recoveryDelay>0&&h.rangeRetention>0&&h.rangeRetention<1,
                "Parametros de manejo finitos e limitados: "+w.name());
        }
        check(found.size()==15&&Weapon.values().length==15,"As quinze armas possuem perfis independentes");
    }

    static void recoil(){
        for(Weapon w:Weapon.values()){
            Gun first=new Gun(w),second=new Gun(w);WeaponHandling h=w.handling;
            double previousPitch=0,previousBloom=0;boolean deterministic=true,bounded=true,progressive=true;
            for(int i=0;i<300;i++){
                Combat.recoil(first,false,false);Combat.recoil(second,false,false);
                deterministic&=close(first.pitchRecoil,second.pitchRecoil)&&close(first.yawRecoil,second.yawRecoil)&&close(first.bloom,second.bloom);
                bounded&=Double.isFinite(first.pitchRecoil)&&Double.isFinite(first.yawRecoil)&&Double.isFinite(first.bloom)
                    &&first.pitchRecoil<=h.riseCap+1e-11&&Math.abs(first.yawRecoil)<=h.sideCap+1e-11&&first.bloom<=h.bloomCap+1e-11;
                progressive&=first.pitchRecoil>=previousPitch&&first.bloom>=previousBloom;
                previousPitch=first.pitchRecoil;previousBloom=first.bloom;
            }
            check(deterministic,"Padrao de recuo deterministico: "+w.name());
            check(bounded&&progressive,"Recuo e dispersao crescem ate os limites: "+w.name());
            check(first.sprayStep==300&&first.shotAge==0,"Disparo avanca contador e reinicia recuperacao: "+w.name());
            Gun crouched=new Gun(w),focused=new Gun(w);
            for(int i=0;i<12;i++){Combat.recoil(crouched,true,false);Combat.recoil(focused,false,true);}
            Gun normal=burst(w,12);
            check(crouched.pitchRecoil<normal.pitchRecoil&&crouched.bloom<=normal.bloom,
                "Agachar reduz recuo sem ampliar dispersao: "+w.name());
            check(focused.pitchRecoil<crouched.pitchRecoil&&focused.bloom<=crouched.bloom,
                "FOCO reduz recuo e dispersao: "+w.name());
            Gun untouched=new Gun(w);first.tick(.01);
            check(untouched.sprayStep==0&&untouched.pitchRecoil==0&&untouched.yawRecoil==0&&untouched.bloom==0,
                "Instancias da mesma arma nao compartilham estado: "+w.name());
        }
        Gun echo=burst(Weapon.ECHO,9);
        check(echo.pitchRecoil>.05&&Math.abs(echo.yawRecoil)>.002,"ECHO mantem recuo vertical e lateral em rajada longa");
    }

    static void legalCadence(){
        Game g=arena();
        try{
            for(Weapon w:Weapon.values()){
                Gun gun=new Gun(w);WeaponHandling h=w.handling;g.player.primary=gun;g.player.slot=2;
                g.weaponEquip=0;g.player.crouch=false;g.player.stim=0;g.focus=0;
                boolean legal=true,blocked=true,pattern=true,bounded=true,accumulated=false;int expectedStep=1;
                for(int i=0;i<w.mag;i++){
                    int shots=g.trainingShots,ammo=gun.ammo;
                    g.controlPlayer(0,trigger(true,true));
                    legal&=g.trainingShots==shots+1&&gun.ammo==ammo-1;
                    pattern&=gun.sprayStep==expectedStep;accumulated|=gun.sprayStep>1;
                    bounded&=gun.pitchRecoil<=h.riseCap+1e-11&&Math.abs(gun.yawRecoil)<=h.sideCap+1e-11&&gun.bloom<=h.bloomCap+1e-11;
                    double wait=gun.cooldown;int step=gun.sprayStep;
                    gun.tick(wait/2);shots=g.trainingShots;ammo=gun.ammo;
                    g.controlPlayer(0,trigger(true,true));
                    blocked&=g.trainingShots==shots&&gun.ammo==ammo;
                    gun.tick(wait/2);legal&=gun.cooldown==0;
                    expectedStep=wait>h.recoveryDelay?1:step+1;
                }
                check(legal&&blocked,"Carregador real respeita cadencia e bloqueia tiro antecipado: "+w.name());
                check(pattern&&accumulated==(w.interval<h.recoveryDelay),
                    "Padrao acumula entre tiros rapidos e recupera antes dos tiros lentos: "+w.name());
                check(bounded,"Recuo respeita limites durante cadencia legal: "+w.name());
                double pitch=gun.pitchRecoil,yaw=gun.yawRecoil,bloom=gun.bloom,age=gun.shotAge,pause=h.recoveryDelay+.7;
                double active=Math.max(0,age+pause-h.recoveryDelay)-Math.max(0,age-h.recoveryDelay);
                double decay=Math.exp(-h.recoveryRate*active);gun.tick(pause);
                check(gun.sprayStep==0&&close(gun.pitchRecoil,pitch*decay)&&close(gun.yawRecoil,yaw*decay)&&close(gun.bloom,bloom*decay),
                    "Pausa apos carregador recupera pelo atraso e taxa do perfil: "+w.name());
                gun.ammo=1;Gun fresh=new Gun(w);Combat.recoil(fresh,false,false);g.controlPlayer(0,trigger(true,true));
                check(gun.sprayStep==1&&close(gun.pitchRecoil,fresh.pitchRecoil)&&close(gun.yawRecoil,fresh.yawRecoil)&&close(gun.bloom,fresh.bloom),
                    "Primeiro tiro apos pausa retorna ao padrao inicial: "+w.name());
            }
        }finally{g.close();}
    }

    static void recovery(){
        for(Weapon w:Weapon.values()){
            WeaponHandling h=w.handling;Gun whole=burst(w,12),split=burst(w,12);
            double pitch=whole.pitchRecoil,yaw=whole.yawRecoil,bloom=whole.bloom;
            whole.tick(h.recoveryDelay*.6);
            check(close(whole.pitchRecoil,pitch)&&close(whole.yawRecoil,yaw)&&close(whole.bloom,bloom)&&whole.sprayStep==12,
                "Recuo permanece durante atraso de recuperacao: "+w.name());
            whole.tick(h.recoveryDelay*.4+.037);
            double decay=Math.exp(-h.recoveryRate*.037);
            check(close(whole.pitchRecoil,pitch*decay)&&close(whole.yawRecoil,yaw*decay)&&close(whole.bloom,bloom*decay)&&whole.sprayStep==0,
                "Passo que cruza atraso recupera somente o tempo ativo: "+w.name());
            double duration=h.recoveryDelay+.037;
            for(int i=0;i<61;i++)split.tick(duration/61);
            check(close(whole.pitchRecoil,split.pitchRecoil)&&close(whole.yawRecoil,split.yawRecoil)&&close(whole.bloom,split.bloom),
                "Recuperacao independe da particao temporal: "+w.name());
            Gun once=burst(w,12),sixty=burst(w,12),high=burst(w,12);
            once.tick(.65);for(int i=0;i<39;i++)sixty.tick(1/60.);for(int i=0;i<94;i++)high.tick(.65/94);
            check(close(once.pitchRecoil,sixty.pitchRecoil)&&close(once.bloom,sixty.bloom)&&close(once.pitchRecoil,high.pitchRecoil)&&close(once.bloom,high.bloom),
                "Pausa de 650 ms recupera igual em um passo, 60 e 144 FPS: "+w.name());
            check(once.sprayStep==0&&once.pitchRecoil<pitch&&once.bloom<=bloom,
                "Pausa reduz recuo e reinicia a primeira rajada: "+w.name());
        }
        Gun gun=new Gun(Weapon.ECHO);gun.ammo=5;gun.reserve=10;gun.cooldown=.10;gun.reload=.15;
        gun.tick(.20);
        check(gun.ammo==15&&gun.reserve==0&&gun.cooldown==0&&close(gun.cooldownDebt,.10),
            "Recuperacao conserva recarga e compensacao de cadencia");
    }

    static void damage(){
        for(Weapon w:Weapon.values()){
            check(w.damageAt(0,false)==w.body&&w.damageAt(w.range,false)==w.body&&w.damageAt(w.range,true)==w.head,
                "Dano base preservado ate alcance nominal: "+w.name());
            double at=w.damageAt(w.range,false),after=w.damageAt(w.range+1e-6,false);
            check(after<=at&&at-after<1e-5,"Dano continuo no antigo degrau de alcance: "+w.name());
            double previous=w.body;boolean monotonic=true,ratio=true;
            for(int i=0;i<=400;i++){
                double distance=i*2.5,body=w.damageAt(distance,false),head=w.damageAt(distance,true);
                monotonic&=Double.isFinite(body)&&body>0&&body<=previous+1e-11;
                ratio&=close(body/w.body,head/w.head);previous=body;
            }
            check(monotonic&&ratio,"Queda gradual preserva fator comum de corpo e cabeca: "+w.name());
            double expected=Math.max(w.pellets>1?.18:.45,w.handling.rangeRetention);
            check(close(w.damageAt(w.range+25,false)/w.body,expected),"Retencao calibrada por arma a cada 25 metros: "+w.name());
        }
        check(Weapon.DART.damageAt(35,false)<Weapon.DART.damageAt(5,false)*.3,"DART perde eficacia fora de combate proximo");
        check(Weapon.HORIZON.damageAt(120,false)/Weapon.HORIZON.body>Weapon.CIRCUIT.damageAt(45,false)/Weapon.CIRCUIT.body,
            "Precisao pesada retem mais dano que submetralhadora apos alcance nominal");
    }

    static void accuracy(){
        for(Weapon w:Weapon.values()){
            Actor a=actor(w);double idle=Combat.spread(a,false,false);a.moveSpeed=5.4*w.mobility;
            double running=Combat.spread(a,false,false);a.moveSpeed=0;a.grounded=false;
            double airborne=Combat.spread(a,false,false);a.grounded=true;a.landRecovery=.1;
            double landing=Combat.spread(a,false,false);a.landRecovery=0;a.crouch=true;
            check(idle>0&&running>idle&&airborne>idle&&landing>idle&&Combat.spread(a,false,false)<idle,
                "Precisao responde a corrida, salto, pouso e agachamento: "+w.name());
            a.crouch=false;double aimed=Combat.spread(a,true,false),focused=Combat.spread(a,false,true);
            check(aimed<idle&&focused<idle,"Mira e FOCO melhoram precisao: "+w.name());
            for(int i=0;i<9;i++)Combat.recoil(a.gun(),false,false);
            check(Combat.spread(a,false,false)>idle,"Rajada aumenta dispersao comum: "+w.name());
        }
        Actor horizon=actor(Weapon.HORIZON);
        check(Combat.spread(horizon,true,false)<Combat.spread(horizon,false,false)*.1,"HORIZON exige luneta para precisao");
        Actor echo=actor(Weapon.ECHO);double idle=Combat.spread(echo,false,false);echo.grounded=false;
        check(Combat.spread(echo,false,false)>idle*20,"Penalidade aerea forte permanece no fuzil");
        Game g=arena();
        try{
            for(Weapon w:Weapon.values()){
                g.player.primary=new Gun(w);g.player.slot=2;g.player.moveSpeed=3.5;g.player.grounded=false;g.player.crouch=false;
                g.player.landRecovery=.08;g.aiming=w.scoped();g.focus=0;for(int i=0;i<9;i++)Combat.recoil(g.player.gun(),false,false);
                double shared=Combat.spread(g.player,g.aiming,false);
                check(close(g.playerSpread(),shared),"Jogador usa precisao compartilhada: "+w.name());
                for(int difficulty=0;difficulty<3;difficulty++){
                    g.settings.difficulty=difficulty;double error=.004+(2-difficulty)*.005;
                    check(close(g.bots.shotSpread(g.player),shared+error),"Bot adiciona apenas erro de dificuldade "+difficulty+": "+w.name());
                }
            }
        }finally{g.close();}
    }

    static void storedWeapon(){
        Game g=arena();
        try{
            g.player.primary=burst(Weapon.ECHO,9);g.player.slot=1;Gun stored=g.player.primary;
            for(int i=0;i<78;i++)g.tick(1/120.,Input.Frame.empty());
            check(stored.sprayStep==0&&stored.bloom<1e-5&&stored.pitchRecoil<1e-5,"Arma guardada tambem recupera recuo no tempo de jogo");
            g.player.slot=2;Actor fresh=actor(Weapon.ECHO);
            check(g.playerSpread()<Combat.spread(fresh,false,false)*1.01,"Reequipar arma recuperada restaura precisao sem estado congelado");
        }finally{g.close();}
    }
}
