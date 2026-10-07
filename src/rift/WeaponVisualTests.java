package rift;

import java.util.*;
import static rift.Tests.check;
import static rift.World.*;
import static rift.Game.*;

final class WeaponVisualTests {
    static List<Tri> echo(double magazine,double cycle,Cosmetics.Skin skin,Cosmetics.Charm charm){
        List<Tri> out=new ArrayList<>();WeaponModel.add(out,Weapon.ECHO,0,0,0,0,1,magazine,cycle,skin,charm,.1,.1);return out;
    }
    static void run(){
        MeshAssets.Mesh body=MeshAssets.load("models/echo/echo-body.obj"),world=MeshAssets.load("models/echo/echo-world-body.obj");
        check(body.vertexCount()==154&&body.triangleCount()==280,"ECHO Quaternius receiver reduced to 280 triangles");
        for(double yaw:new double[]{0,.6,2.4}){
            WeaponModel model=new WeaponModel(new ArrayList<>(),3,4,5,yaw,.8,Weapon.ECHO.color);V vertex=new V(.1,.2,.3),actual=model.p(vertex.x(),vertex.y(),vertex.z());
            V expected=new V(3+.8*(vertex.x()*Math.cos(yaw)+vertex.z()*Math.sin(yaw)),4+.8*vertex.y(),5+.8*(-vertex.x()*Math.sin(yaw)+vertex.z()*Math.cos(yaw)));
            check(actual.equals(expected)&&model.local(actual).sub(vertex).length()<1e-12,"Weapon cached rotation preserves exact world transform and inverse: "+yaw);
        }
        check(world.vertexCount()==82&&world.triangleCount()==144,"Third-person ECHO imports reduced 144-triangle receiver");
        List<Tri> idle=echo(0,0,Cosmetics.Skin.STANDARD,Cosmetics.Charm.NONE),reload=echo(.15,0,Cosmetics.Skin.STANDARD,Cosmetics.Charm.NONE),fire=echo(0,.05,Cosmetics.Skin.STANDARD,Cosmetics.Charm.NONE);
        check(idle.size()<1500&&idle.size()>body.triangleCount(),"ECHO pilot stays within first-person geometry budget");
        check(!idle.equals(reload)&&idle.size()==reload.size(),"ECHO magazine moves during reload without replacing the receiver");
        check(!idle.equals(fire)&&idle.size()==fire.size(),"ECHO charging mechanism retains firing cycle");
        boolean receiver=true;for(int i=0;i<body.triangleCount();i++)receiver&=idle.get(i).equals(reload.get(i));
        check(receiver,"ECHO fixed imported receiver is stable while magazine animates");
        for(Cosmetics.Skin skin:Cosmetics.Skin.values()){
            List<Tri> colored=echo(0,0,skin,Cosmetics.Charm.NONE);
            check(!colored.isEmpty()&&colored.stream().allMatch(t->Double.isFinite(t.a().x())&&Double.isFinite(t.a().y())&&Double.isFinite(t.a().z())),"ECHO skin renders finite imported and original geometry: "+skin);
        }
        for(Cosmetics.Charm charm:Cosmetics.Charm.values())check(echo(0,0,Cosmetics.Skin.STANDARD,charm).size()>=idle.size(),"ECHO charm remains attached: "+charm);
        Actor actor=new Actor(0,0,"VERTICE");actor.primary=new Gun(Weapon.ECHO);actor.slot=2;
        List<Tri> detailed=new ArrayList<>(),low=new ArrayList<>();CharacterModel.add(detailed,actor,0,true);CharacterModel.add(low,actor,0,false);
        check(detailed.size()>low.size()&&detailed.size()<1700,"Detailed ECHO and VERTICE preserve cheaper distant actor LOD");
        actor.gun().reloadTotal=2.2;actor.gun().reload=1.1;List<Tri> reloading=new ArrayList<>();CharacterModel.add(reloading,actor,0,true);
        check(detailed.size()==reloading.size()&&!detailed.equals(reloading),"Third-person ECHO follows existing reload animation");
        actor.gun().reload=0;actor.shotGlow=.1;List<Tri> firing=new ArrayList<>();CharacterModel.add(firing,actor,0,true);
        double minZ=Double.POSITIVE_INFINITY,maxZ=Double.NEGATIVE_INFINITY;
        for(Tri t:firing.subList(firing.size()-16,firing.size()))for(V v:new V[]{t.a(),t.b(),t.c()}){minZ=Math.min(minZ,v.z());maxZ=Math.max(maxZ,v.z());}
        check(Math.abs(minZ-(.37-.045+(1.075-.19)*.62))<1e-6&&Math.abs(maxZ-minZ-.15)<1e-6,"Third-person ECHO muzzle flash starts at the imported barrel tip");
        System.out.println("WEAPON VISUAL TESTS OK");
    }
}
