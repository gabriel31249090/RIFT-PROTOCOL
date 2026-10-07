package rift;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static rift.Tests.check;
import static rift.World.*;

final class MeshTests {
    static final String TRI="v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n";
    static MeshAssets.Mesh parse(String text)throws IOException{return MeshAssets.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));}
    static List<Tri> triangles(MeshAssets.Mesh mesh){List<Tri> out=new ArrayList<>();mesh.add(out,v->v,name->0xAABBBB,name->Assets.STEEL);return out;}
    static void rejected(String text,String message){
        boolean rejected=false;try{parse(text);}catch(IOException e){rejected=true;}check(rejected,message);
    }
    static void run()throws Exception{
        MeshAssets.Mesh mesh=parse(TRI);List<Tri> faces=triangles(mesh);Tri t=faces.get(0);
        check(mesh.vertexCount()==3&&mesh.triangleCount()==1,"OBJ parser imports a real triangle");
        check(t.a().equals(new V(0,0,0))&&t.b().equals(new V(0,1,0))&&t.c().equals(new V(1,0,0)),"OBJ outward winding converts to engine inward winding");
        check(mesh.min().equals(new V(0,0,0))&&mesh.max().equals(new V(1,1,0)),"OBJ cached template reports finite local bounds");
        MeshAssets.Mesh uv=parse("v 0 0 0\nv 1 0 0\nv 1 1 0\nv 0 1 0\nvt 0 .1\nvt .8 .1\nvt .8 .9\nvt 0 .9\nusemtl steel\nf 1/1 2/2 3/3 4/4\n");
        List<String> groups=new ArrayList<>();uv.add(new ArrayList<>(),v->v,name->{groups.add(name);return 0xAAFFFF;},name->Assets.STEEL);
        check(uv.triangleCount()==2&&groups.stream().allMatch("steel"::equals),"OBJ triangulated quad keeps material on every generated face");
        Tri u=triangles(uv).get(0);check(Math.abs(u.va()-.9)<1e-6&&Math.abs(u.vb()-.1)<1e-6,"OBJ UV vertical origin flips and follows reversed vertices");
        mesh=parse("mtllib ../../ignored.mtl\nv 0 0 0\nv 1 0 0\nv 0 1 0\nusemtl a\nf -3 -2 -1\nf 1 2 3\nusemtl b\nf 3 2 1\n");
        groups.clear();mesh.add(new ArrayList<>(),v->v,name->{groups.add(name);return 0xAABBCC;},name->-1);
        check(groups.equals(List.of("a","a","b")),"OBJ materials persist across faces and switch explicitly");
        check(mesh.triangleCount()==3,"Negative OBJ indices resolve without opening MTL paths");
        List<Tri> translated=new ArrayList<>();mesh.add(translated,v->v.add(new V(2,3,4)),name->0xAABBCC,name->-1);
        check(translated.get(0).a().equals(new V(2,3,4)),"Cached OBJ transforms operate on copies, not source vertices");
        check(triangles(mesh).get(0).a().equals(new V(0,0,0)),"Repeated OBJ instance retains original local geometry");
        rejected("", "Empty OBJ is rejected");
        rejected(TRI.replace("1 2 3","1 2 99"),"Out of range OBJ index is rejected");
        rejected(TRI.replace("v 0 0 0","v NaN 0 0"),"Nonfinite OBJ vertex is rejected");
        rejected(TRI.replace("v 0 0 0","v 10001 0 0"),"Excessive OBJ coordinate is rejected");
        rejected("v 0 0 0\nv 1 0 0\nv 2 0 0\nf 1 2 3\n","Degenerate-only OBJ is rejected");
        rejected("v 0 0 0\nv 1 0 0\nv 0 1 0\nvt NaN 0\nf 1/1 2/1 3/1\n","Invalid OBJ UV is rejected");
        rejected(TRI+"f 1 2 3\n".repeat(MeshAssets.MAX_TRIANGLES),"OBJ triangle budget is enforced before triangulation");
        rejected("#"+"a".repeat(MeshAssets.MAX_BYTES),"OBJ byte budget is enforced before parsing");
        boolean path=false;try{MeshAssets.load("models/../../secret.obj");}catch(IllegalArgumentException e){path=true;}
        check(path,"OBJ resource path cannot escape asset directory");
        check(MeshAssets.tryLoad("models/missing-pilot.obj")==null&&MeshAssets.tryLoad("models/missing-pilot.obj")==null,"Optional model fallback caches absent resources");
        MeshAssets.Mesh unit=MeshAssets.load("models/cais7/service-unit.obj");
        check(unit==MeshAssets.load("models/cais7/service-unit.obj")&&unit.triangleCount()==32,"CAIS service unit uses one cached 32-triangle template");
        for(int material:new int[]{CaisArt.FLOOR,CaisArt.WALL,CaisArt.METAL}){
            check(material>=16&&Assets.tiles[material][0].length==65536&&Assets.tiles[material][4].length==256,"Imported material retains bounded five-level mip pyramid "+material);
            check(Assets.tinted(material,0,0xAABBCC).length==256,"Imported material uses bounded palette tint cache "+material);
        }
        check(CaisArt.FLOOR==Assets.registerMaterial("models/cais7/concrete_floor_worn_001_diff_1k.jpg"),"Repeated material registration reuses the same ID");
        ByteArrayOutputStream oversized=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4097,1,BufferedImage.TYPE_INT_RGB),"png",oversized);
        boolean dimensions=false;try{Assets.readMaterial(new ByteArrayInputStream(oversized.toByteArray()));}catch(IOException e){dimensions=true;}
        check(dimensions,"Imported texture dimensions are rejected before pixel decoding");
        World w=new World(0);int solids=w.solids.size(),ramps=w.ramps.size();boolean[] nav=w.nav.clone();
        check(w.triangles.stream().anyMatch(f->f.material()==CaisArt.WALL),"CAIS concrete uses imported photographic texture");
        CaisArt.decorate(w);check(w.solids.size()==solids&&w.ramps.size()==ramps&&Arrays.equals(nav,w.nav),"CAIS decoration never changes collision or navigation");
        check(w.triangles.stream().filter(f->f.material()==CaisArt.METAL&&f.a().y()>6&&f.a().z()<0).count()>=12*32,"CAIS imported service props stay outside playable boundary");
        WeaponVisualTests.run();
        System.out.println("MESH TESTS OK");
    }
}
