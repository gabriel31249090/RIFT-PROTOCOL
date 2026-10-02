package rift;

import static rift.Game.*;
import static rift.World.*;
import static rift.Tests.check;

/** Shared ray rules include partially crossed cover, nested solids and overlap. */
final class BallisticsTests {
    static final V ORIGIN=new V(50,1,60),FORWARD=new V(0,0,1);
    static void clear(World world){world.solids.clear();world.penetrable.clear();world.coverMaterials.clear();world.temporary.clear();world.ramps.clear();}
    static Box panel(World world,double z,double depth,Ballistics.Material material){
        Box box=new Box(48,0,z,52,3,z+depth,0);
        world.solids.add(box);world.penetrable.add(box);world.coverMaterials.put(box,material);return box;
    }
    static Ballistics.Shot shot(World world,Weapon weapon){return Ballistics.trace(world,ORIGIN,FORWARD,weapon);}
    static boolean near(double a,double b){return Math.abs(a-b)<1e-8;}
    static void run(){
        World world=new World();weapons(world);materials(world);overlap(world);obstructions(world);internalTarget();
    }
    static void weapons(World world){
        for(Weapon weapon:Weapon.values()){
            clear(world);panel(world,65,.05,Ballistics.Material.WOOD);
            Ballistics.Shot first=shot(world,weapon);
            if(weapon.pellets>1){
                check(near(first.distance(),5)&&first.passages().isEmpty(),weapon.name()+": pellets param na primeira cobertura");
            }else{
                check(first.distance()>20&&first.passages().size()==1,weapon.name()+": atravessa madeira de cinco centimetros");
                check(first.factorAt(4)==1&&first.factorAt(7)>0&&first.factorAt(7)<1,weapon.name()+": retencao de dano respeita a cobertura");
                panel(world,68,.05,Ballistics.Material.WOOD);Ballistics.Shot second=shot(world,weapon);
                boolean two=weapon.category==Category.RIFLE||weapon.scoped()||weapon.category==Category.HEAVY;
                check(two?second.distance()>20&&second.passages().size()==2:near(second.distance(),8)&&second.passages().size()==1,
                    weapon.name()+": limite individual de superficies");
            }
        }
        clear(world);panel(world,65,.13,Ballistics.Material.WOOD);
        check(shot(world,Weapon.SPARK).distance()<5.13&&shot(world,Weapon.VEIL).distance()>20,"Pistolas leves possuem potencias distintas");
        clear(world);panel(world,65,.27,Ballistics.Material.WOOD);
        check(shot(world,Weapon.CIRCUIT).distance()<5.27&&shot(world,Weapon.WISP).distance()>20,"SMGs possuem potencias distintas");
        clear(world);panel(world,65,.58,Ballistics.Material.WOOD);
        check(shot(world,Weapon.SHADE).distance()<5.58&&shot(world,Weapon.HELIX).distance()<5.58&&shot(world,Weapon.ECHO).distance()>20,
            "Fuzis possuem potencias distintas");
        clear(world);panel(world,65,.9,Ballistics.Material.WOOD);
        check(shot(world,Weapon.RIDGE).distance()<5.9&&shot(world,Weapon.HORIZON).distance()>20,"Armas de precisao possuem potencias distintas");
        clear(world);for(int i=0;i<3;i++)panel(world,65+i,.05,Ballistics.Material.WOOD);
        check(near(shot(world,Weapon.BASTION).distance(),7),"Arma pesada para na terceira superficie mesmo com potencia restante");
    }
    static void materials(World world){
        clear(world);panel(world,65,.2,Ballistics.Material.WOOD);
        Ballistics.Shot wood=shot(world,Weapon.HORIZON);
        clear(world);panel(world,65,.2,Ballistics.Material.METAL);
        Ballistics.Shot metal=shot(world,Weapon.HORIZON);
        check(metal.distance()>20&&metal.factorAt(10)<wood.factorAt(10),"Metal custa mais potencia e retencao que madeira");
        check(near(shot(world,Weapon.ECHO).distance(),5+.6/3.5),"Bala sem potencia suficiente para dentro do metal");
        clear(world);panel(world,65,.2,Ballistics.Material.CONCRETE);
        check(near(shot(world,Weapon.BASTION).distance(),5)&&shot(world,Weapon.BASTION).factorAt(10)==1,
            "Concreto bloqueia sem criar fator de dano NaN");
        clear(world);panel(world,65,.4,Ballistics.Material.WOOD);panel(world,66,.4,Ballistics.Material.WOOD);
        check(near(shot(world,Weapon.ECHO).distance(),6.2),"Espessuras consomem um unico orcamento ao longo do trajeto");
        clear(world);Box wide=new Box(0,0,65,144,3,65.2,0);world.solids.add(wide);world.penetrable.add(wide);
        Ballistics.Shot normal=shot(world,Weapon.ECHO),angled=Ballistics.trace(world,ORIGIN,new V(.97,0,.243).unit(),Weapon.ECHO);
        check(normal.passages().size()==1&&angled.passages().isEmpty(),"Angulo obliquo aumenta espessura fisica e pode impedir saida");
    }
    static void overlap(World world){
        clear(world);panel(world,65,.4,Ballistics.Material.WOOD);panel(world,65.1,.1,Ballistics.Material.WOOD);
        Ballistics.Shot nested=shot(world,Weapon.ECHO);
        check(nested.distance()>20&&nested.passages().size()==2,"Dois paineis sobrepostos compartilham potencia sem serem ignorados");
        check(near(nested.passages().get(0).exit(),5.2)&&near(nested.passages().get(1).exit(),5.4),"Saidas aninhadas ficam em ordem espacial");
        check(near(nested.factorAt(5.05),.82-.05*.55)&&near(nested.factorAt(5.3),(.82-.3*.55)*(.82-.1*.55)),
            "Dano dentro da cobertura usa apenas espessura realmente percorrida");
        check(near(nested.factorAt(6),(.82-.4*.55)*(.82-.1*.55)),"Coberturas sobrepostas acumulam retencao depois das duas saidas");
        clear(world);panel(world,65,.4,Ballistics.Material.WOOD);panel(world,65.1,.3,Ballistics.Material.WOOD);
        Ballistics.Shot stopped=shot(world,Weapon.ECHO);
        check(near(stopped.distance(),5.35)&&stopped.passages().isEmpty(),"Orcamento acaba dentro de duas coberturas sobrepostas");
        clear(world);panel(world,65.1,.3,Ballistics.Material.WOOD);panel(world,65,.4,Ballistics.Material.WOOD);
        Ballistics.Shot reversed=shot(world,Weapon.ECHO);
        check(near(reversed.distance(),stopped.distance())&&near(reversed.factorAt(5.2),stopped.factorAt(5.2)),
            "Resultado de sobreposicao independe da ordem de cadastro");
        clear(world);panel(world,65,.2,Ballistics.Material.WOOD);panel(world,65.02,.01,Ballistics.Material.WOOD);panel(world,65.04,.01,Ballistics.Material.WOOD);
        check(near(shot(world,Weapon.ECHO).distance(),5.04),"Terceiro painel interno ainda respeita limite de superficies");
    }
    static void obstructions(World world){
        clear(world);panel(world,65,.2,Ballistics.Material.WOOD);world.solids.add(new Box(48,0,65.1,52,3,65.15,1));
        check(near(shot(world,Weapon.ECHO).distance(),5.1),"Solido opaco dentro de painel interrompe a bala");
        clear(world);panel(world,65,.2,Ballistics.Material.WOOD);world.temporary.add(new Box(48,0,65.08,52,3,65.09,1));
        check(near(shot(world,Weapon.ECHO).distance(),5.08),"Barreira temporaria dentro de painel interrompe a bala");
        clear(world);Box box=panel(world,65,.2,Ballistics.Material.WOOD);world.temporary.add(box);
        check(near(shot(world,Weapon.ECHO).distance(),5)&&shot(world,Weapon.ECHO).passages().isEmpty(),
            "Barreira temporaria coincidente nao herda penetracao do painel");
        clear(world);panel(world,65,.2,Ballistics.Material.WOOD);world.solids.add(new Box(48,0,65,52,3,65.2,1));
        check(near(shot(world,Weapon.ECHO).distance(),5),"Solido opaco coincidente tem precedencia sobre painel");
        clear(world);panel(world,65,.2,Ballistics.Material.WOOD);world.solids.add(new Box(48,0,65.2005,52,3,65.21,1));
        check(near(shot(world,Weapon.ECHO).distance(),5.2005),"Obstaculo a meio milimetro da saida nao e pulado");
        clear(world);panel(world,65,.2,Ballistics.Material.WOOD);world.ramps.add(new Ramp(48,65.05,52,65.15,2,false));
        check(near(shot(world,Weapon.ECHO).distance(),5.1),"Rampa dentro de painel mantem sua obstrucao geometrica");
        clear(world);panel(world,65,.2,Ballistics.Material.WOOD);V direction=new V(0,-.03,1).unit();
        Ballistics.Shot floor=Ballistics.trace(world,new V(50,.153,60),direction,Weapon.ECHO);
        check(near(floor.distance(),-.153/direction.y()),"Chao dentro de painel interrompe tiro descendente");
        clear(world);
        check(near(shot(world,Weapon.ECHO).distance(),210),"Raio sem geometria conserva alcance maximo de 210 metros");
    }
    static void internalTarget(){
        Game game=BotTests.arena();
        try{
            Actor shooter=game.actors.get(1),target=game.actors.get(5);target.z=65.4;
            panel(game.world,65,1,Ballistics.Material.WOOD);
            game.shoot(shooter,target.center().sub(shooter.eye()).unit(),Weapon.ECHO);
            check(target.hp<100&&target.hp>65,"Tiro real acerta alvo parcialmente interno sem cobrar espessura posterior nem dar dano integral");
            target.hp=100;target.z=67;game.shoot(shooter,target.center().sub(shooter.eye()).unit(),Weapon.ECHO);
            check(target.hp==100,"Alvo alem do orcamento percorrivel permanece protegido");
            target.z=65.4;game.world.temporary.add(new Box(48,0,65.1,52,3,65.11,1));
            game.shoot(shooter,target.center().sub(shooter.eye()).unit(),Weapon.ECHO);
            check(target.hp==100,"Tiro real nao atinge alvo apos barreira temporaria interna");
        }finally{game.close();}
    }
}
