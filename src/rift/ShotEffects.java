package rift;

import java.util.*;
import static rift.Game.*;
import static rift.World.*;

/** Cosmetic simulation has its own random stream and never changes shot accuracy. */
final class ShotEffects {
    final Game g;final Random rng=new Random(761);final List<Casing> casings=new ArrayList<>();final List<Impact> marks=new ArrayList<>();
    static final class Casing{V p,v;double life=2.4,spin;final boolean shell;Casing(V p,V v,boolean shell){this.p=p;this.v=v;this.shell=shell;}}
    static final class Impact{final V p,n;final int material;double life=9;Impact(V p,V n,int m){this.p=p;this.n=n;material=m;}}
    ShotEffects(Game g){this.g=g;}
    void clear(){casings.clear();marks.clear();}
    void fired(Actor a){if(!g.settings.impactFX)return;V side=new V(Math.cos(a.yaw),0,-Math.sin(a.yaw));if(casings.size()>=32)casings.remove(0);casings.add(new Casing(a.eye().add(a.dir().mul(.48)).add(side.mul(.24)).add(new V(0,-.16,0)),side.mul(1.6+rng.nextDouble()).add(new V(0,1.4+rng.nextDouble(),0)),a.gun().kind.pellets>1));}
    SurfaceHit hit(V origin,V direction,double distance){double start=Math.max(0,distance-.08);SurfaceHit surface=SurfaceHit.cast(g.world,origin.add(direction.mul(start)),direction,distance-start+.04,0);if(surface==null||Math.abs(start+surface.distance()-distance)>.1)return null;if(g.settings.impactFX){if(marks.size()>=80)marks.remove(0);marks.add(new Impact(surface.point().add(surface.normal().mul(.008)),surface.normal(),surface.material()));}return surface;}
    void tick(double dt){marks.removeIf(m->(m.life-=dt)<=0);for(Iterator<Casing> it=casings.iterator();it.hasNext();){Casing c=it.next();c.life-=dt;if(c.life<=0){it.remove();continue;}c.v=c.v.add(new V(0,-10*dt,0));double speed=c.v.length();SurfaceHit hit=SurfaceHit.cast(g.world,c.p,c.v.unit(),speed*dt,.018);if(hit==null)c.p=c.p.add(c.v.mul(dt));else{c.p=hit.point().add(hit.normal().mul(.002));c.v=c.v.sub(hit.normal().mul(c.v.dot(hit.normal())*1.24)).mul(.60);}c.spin+=dt*c.v.length()*8;}
    }
    void draw(Renderer r){
        for(Casing c:casings){WeaponModel m=new WeaponModel(r.dynamic,c.p.x(),c.p.y(),c.p.z(),c.spin,1,0xD0AA62);m.tube(0,0,0,c.shell?.068:.045,c.shell?.014:.008,c.shell?0xB2664E:0xD5B575,5);}
        for(Impact m:marks){V u=Math.abs(m.n.y())>.8?new V(1,0,0):new V(-m.n.z(),0,m.n.x()).unit(),v=new V(m.n.y()*u.z()-m.n.z()*u.y(),m.n.z()*u.x()-m.n.x()*u.z(),m.n.x()*u.y()-m.n.y()*u.x());double size=.027;
            for(int i=0;i<7;i++){double a=i*Math.PI*2/7,b=(i+1)*Math.PI*2/7;r.dynamic.add(new Tri(m.p,m.p.add(u.mul(Math.cos(a)*size)).add(v.mul(Math.sin(a)*size)),m.p.add(u.mul(Math.cos(b)*size)).add(v.mul(Math.sin(b)*size)),i%2==0?0x283033:0x394244));}
            for(int i=0;i<3;i++){double a=i*2.094;V center=m.p.add(u.mul(Math.cos(a)*.044)).add(v.mul(Math.sin(a)*.044));quad(r.dynamic,center,center.add(u.mul(.028)),center.add(u.mul(.017)).add(v.mul(.020)),center.add(v.mul(.012)),0xBEB4A0);}
        }
    }
    static double kick(Gun gun){return Math.exp(-gun.shotAge*25)*Math.sin(Math.min(1,gun.shotAge/.065)*Math.PI*.65)*(.55+gun.kind.kick*1.8);}
    static void muzzle(WeaponModel m,Weapon w,double age){double z=w.sidearm()?(w==Weapon.VEIL?.94:w==Weapon.TALON?.83:.69):w==Weapon.HORIZON?1.34:w.silenced()?1.29:w.category==Category.SMG?.83:1.12;
        double rad=w.silenced()?.022:w.pellets>1?.11:.074;int n=w.silenced()?5:7;
        for(int i=0;i<n;i++){double a=i*Math.PI*2/n+age*18,aa=a+.20;V center=m.p(0,-.023,z+.05),p=m.p(Math.cos(a)*rad,-.023+Math.sin(a)*rad,z+.06),q=m.p(Math.cos(aa)*rad*.32,-.023+Math.sin(aa)*rad*.32,z+.10);m.out.add(new Tri(center,p,q,0xFFD699));}
        m.tube(0,-.023,z,w.silenced()?.035:.11,rad*.25,0xFFF4CE,6);
    }
}
