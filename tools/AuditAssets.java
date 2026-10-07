import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;

/** Run from repository root: java tools/AuditAssets.java. */
class AuditAssets {
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    public static void main(String[] args)throws Exception{
        Path path=Path.of("RiftProtocol.jar");int assets=0,models=0,bitmaps=0,wavs=0;
        try(JarFile jar=new JarFile(path.toFile())){
            String version=jar.getManifest().getMainAttributes().getValue("Implementation-Version");
            if(!"1.9.2".equals(version))throw new AssertionError("Version: "+version);
            Set<String> entries=new HashSet<>();for(JarEntry e:Collections.list(jar.entries()))if(!entries.add(e.getName()))throw new AssertionError("Duplicate entry: "+e.getName());
            try(var files=Files.walk(Path.of("assets"))){for(Path file:files.filter(Files::isRegularFile).toList()){
                String name=file.toString().replace('\\','/');JarEntry entry=jar.getJarEntry(name);if(entry==null)throw new AssertionError("Missing resource: "+name);
                try(var in=jar.getInputStream(entry)){if(!Arrays.equals(Files.readAllBytes(file),in.readAllBytes()))throw new AssertionError("Resource differs: "+name);}
                assets++;if(name.endsWith(".obj"))models++;if(name.startsWith("assets/models/")&&name.endsWith(".jpg"))bitmaps++;if(name.endsWith(".wav"))wavs++;
            }}
            for(String folder:List.of("echo","vertice","cais7"))for(String line:Files.readAllLines(Path.of("assets/models",folder,"SHA256SUMS.txt"))){
                String[] parts=line.split("  ",2);try(var in=jar.getInputStream(jar.getJarEntry("assets/models/"+folder+"/"+parts[1]))){if(!hash(in.readAllBytes()).equals(parts[0]))throw new AssertionError("Checksum: "+line);}
            }
            String license="META-INF/licenses/obj-LICENSE.txt";
            try(var in=jar.getInputStream(jar.getJarEntry(license))){if(!Arrays.equals(Files.readAllBytes(Path.of("lib/obj-LICENSE.txt")),in.readAllBytes()))throw new AssertionError("OBJ license differs");}
            if(jar.getJarEntry("de/javagl/obj/ObjReader.class")==null)throw new AssertionError("OBJ parser not bundled");
            if(models!=7||bitmaps!=5||wavs!=144)throw new AssertionError("Unexpected asset counts");
            System.out.println("Implementation-Version: "+version);
            System.out.println("All source resources packaged byte-identical: "+assets);
            System.out.println("OBJ files: "+models+" (including upstream source); photographic maps: "+bitmaps+"; WAVs: "+wavs);
            System.out.println("Pilot mesh/bitmap checksums, parser classes and third-party notices: OK");
        }
        System.out.println("JAR bytes: "+Files.size(path));System.out.println("JAR SHA256: "+hash(Files.readAllBytes(path)));
    }
}
