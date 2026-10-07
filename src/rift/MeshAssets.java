package rift;

import de.javagl.obj.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import static rift.World.*;

/** Bounded, cached OBJ templates. Material files never open arbitrary file paths. */
final class MeshAssets {
    static final int MAX_BYTES=4*1024*1024,MAX_VERTICES=16384,MAX_TRIANGLES=12000;
    private static final Map<String,Mesh> CACHE=new ConcurrentHashMap<>();
    private static final Set<String> MISSING=ConcurrentHashMap.newKeySet();
    static Mesh load(String resource){
        validatePath(resource);
        return CACHE.computeIfAbsent(resource,path->{
            try(InputStream packaged=MeshAssets.class.getResourceAsStream("/assets/"+path)){
                if(packaged!=null)return read(packaged);
                try(InputStream disk=Files.newInputStream(Path.of("assets").resolve(path))){return read(disk);}
            }catch(IOException|RuntimeException e){throw new IllegalStateException("Modelo OBJ invalido ou ausente: "+path,e);}
        });
    }
    static Mesh tryLoad(String resource){
        validatePath(resource);
        if(MISSING.contains(resource))return null;
        try{return load(resource);}catch(IllegalStateException e){MISSING.add(resource);return null;}
    }
    private static void validatePath(String path){
        if(path==null||!path.matches("models/[A-Za-z0-9_./-]+\\.obj")||path.contains(".."))
            throw new IllegalArgumentException("Caminho de modelo invalido");
    }
    static Mesh read(InputStream input)throws IOException{
        byte[] data=input.readNBytes(MAX_BYTES+1);
        if(data.length>MAX_BYTES)throw new IOException("OBJ excede limite de bytes");
        try {
            Obj raw=ObjReader.read(new InputStreamReader(new ByteArrayInputStream(data),StandardCharsets.UTF_8));
            if(raw.getNumVertices()<3||raw.getNumVertices()>MAX_VERTICES||raw.getNumFaces()==0)
                throw new IOException("Quantidade de vertices/faces invalida");
            V[] vertices=new V[raw.getNumVertices()];
            V min=new V(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY),max=min.mul(-1);
            for(int i=0;i<vertices.length;i++){
                FloatTuple t=raw.getVertex(i);V v=new V(t.getX(),t.getY(),t.getZ());
                if(!finite(v.x(),10000)||!finite(v.y(),10000)||!finite(v.z(),10000))throw new IOException("Vertice nao finito ou fora do limite");
                vertices[i]=v;min=new V(Math.min(min.x(),v.x()),Math.min(min.y(),v.y()),Math.min(min.z(),v.z()));
                max=new V(Math.max(max.x(),v.x()),Math.max(max.y(),v.y()),Math.max(max.z(),v.z()));
            }
            for(int i=0;i<raw.getNumTexCoords();i++){
                FloatTuple uv=raw.getTexCoord(i);
                if(uv.getDimensions()<2||!finite(uv.getX(),100000)||!finite(uv.getY(),100000))throw new IOException("UV invalido");
            }
            long count=0;
            for(int i=0;i<raw.getNumFaces();i++){
                ObjFace face=raw.getFace(i);int n=face.getNumVertices();count+=n-2;
                if(n<3||n>64||count>MAX_TRIANGLES)throw new IOException("OBJ excede limite de triangulos");
                for(int j=0;j<n;j++){
                    if(face.getVertexIndex(j)<0||face.getVertexIndex(j)>=vertices.length)throw new IOException("Indice de vertice invalido");
                    if(face.containsTexCoordIndices()&&(face.getTexCoordIndex(j)<0||face.getTexCoordIndex(j)>=raw.getNumTexCoords()))throw new IOException("Indice UV invalido");
                }
            }
            Obj obj=ObjUtils.triangulate(raw);List<Face> faces=new ArrayList<>();
            String currentMaterial="";
            for(int i=0;i<obj.getNumFaces();i++){
                ObjFace face=obj.getFace(i);int a=face.getVertexIndex(0),b=face.getVertexIndex(1),c=face.getVertexIndex(2);
                String activated=obj.getActivatedMaterialGroupName(face);if(activated!=null)currentMaterial=activated;
                V ab=vertices[b].sub(vertices[a]),ac=vertices[c].sub(vertices[a]);
                V n=new V(ab.y()*ac.z()-ab.z()*ac.y(),ab.z()*ac.x()-ab.x()*ac.z(),ab.x()*ac.y()-ab.y()*ac.x());
                if(n.length()<1e-10)continue;
                n=n.unit();double light=.76+.25*Math.max(0,n.dot(new V(-.4,.82,-.4)))+.08*n.y();
                double[] uv=new double[6];
                if(face.containsTexCoordIndices())for(int j=0;j<3;j++){
                    FloatTuple t=obj.getTexCoord(face.getTexCoordIndex(j));uv[j*2]=t.getX();uv[j*2+1]=1-t.getY();
                }else{
                    Tri tex=World.texture(new Tri(vertices[a],vertices[b],vertices[c],0),Assets.STEEL,2);
                    uv=new double[]{tex.ua(),tex.va(),tex.ub(),tex.vb(),tex.uc(),tex.vc()};
                }
                // OBJ uses outward CCW faces; the CPU scene renderer culls inward CW faces.
                faces.add(new Face(a,c,b,uv[0],uv[1],uv[4],uv[5],uv[2],uv[3],currentMaterial,light));
            }
            if(faces.isEmpty())throw new IOException("Modelo sem triangulos validos");
            return new Mesh(vertices,faces.toArray(Face[]::new),min,max);
        }catch(IllegalArgumentException|IndexOutOfBoundsException e){throw new IOException("Dados OBJ invalidos",e);}
    }
    private static boolean finite(double value,double limit){return Double.isFinite(value)&&Math.abs(value)<=limit;}
    private record Face(int a,int b,int c,double ua,double va,double ub,double vb,double uc,double vc,String material,double light){}
    static final class Mesh {
        private final V[] vertices;private final Face[] faces;private final V min,max;
        private Mesh(V[] vertices,Face[] faces,V min,V max){this.vertices=vertices;this.faces=faces;this.min=min;this.max=max;}
        int vertexCount(){return vertices.length;}
        int triangleCount(){return faces.length;}
        V min(){return min;}
        V max(){return max;}
        void add(List<Tri> out,UnaryOperator<V> transform,ToIntFunction<String> colors,ToIntFunction<String> textures){
            V[] transformed=new V[vertices.length];for(int i=0;i<vertices.length;i++)transformed[i]=transform.apply(vertices[i]);
            for(Face f:faces){int material=textures.applyAsInt(f.material);
                out.add(new Tri(transformed[f.a],transformed[f.b],transformed[f.c],shade(colors.applyAsInt(f.material),f.light),material,f.ua,f.va,f.ub,f.vb,f.uc,f.vc));}
        }
    }
}
