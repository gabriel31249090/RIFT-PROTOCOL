import javax.tools.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.security.*;

/** Build with: java Build.java. No Maven, Gradle or downloads required. */
public class Build {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Para compilar, instale um JDK 17 ou superior.");
        Path out = Path.of("build", "classes");
        Files.createDirectories(out);
        if(Files.isDirectory(Path.of("assets")))try(var assets=Files.walk(Path.of("assets"))){for(Path asset:assets.filter(Files::isRegularFile).toList()){Path dest=out.resolve(asset);Files.createDirectories(dest.getParent());Files.copy(asset,dest,StandardCopyOption.REPLACE_EXISTING);}}
        // Removed or renamed classes must not leak into the next executable.
        try (var previous = Files.walk(out)) {
            for (Path file : previous.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".class")).toList()) Files.delete(file);
        }
        List<File> sources;
        try (var files = Files.walk(Path.of("src"))) {
            sources = files.filter(p -> p.toString().endsWith(".java")).sorted().map(Path::toFile).toList();
        }
        Path objLibrary=Path.of("lib","obj-0.4.0.jar");
        String objHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(objLibrary)));
        if(!objHash.equals("abd01a80fe99bac083c367d93f0ae26de76abf71d05990410620f371add15f4b"))
            throw new IllegalStateException("Dependencia OBJ alterada: restaure lib/obj-0.4.0.jar.");
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            boolean ok = compiler.getTask(null, fm, null,
                List.of("--release", "17", "-encoding", "UTF-8", "-classpath", objLibrary.toString(), "-d", out.toString()), null,
                fm.getJavaFileObjectsFromFiles(sources)).call();
            if (!ok) System.exit(1);
        }
        // Bundle the small parser so the executable stays fully offline and standalone.
        try(JarFile dependency=new JarFile(objLibrary.toFile())) {
            for(JarEntry entry:Collections.list(dependency.entries())) {
                if(!entry.isDirectory()&&entry.getName().startsWith("de/javagl/obj/")&&entry.getName().endsWith(".class")) {
                    Path destination=out.resolve(entry.getName());Files.createDirectories(destination.getParent());
                    try(InputStream stream=dependency.getInputStream(entry)){Files.copy(stream,destination,StandardCopyOption.REPLACE_EXISTING);}
                }
            }
        }
        Path notice=out.resolve("META-INF/licenses/obj-LICENSE.txt");Files.createDirectories(notice.getParent());
        Files.copy(Path.of("lib","obj-LICENSE.txt"),notice,StandardCopyOption.REPLACE_EXISTING);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "rift.Main");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "1.9.2");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(Path.of("RiftProtocol.jar.tmp")), manifest);
             var files = Files.walk(out)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                jar.putNextEntry(new JarEntry(out.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, jar);
                jar.closeEntry();
            }
        }
        Files.move(Path.of("RiftProtocol.jar.tmp"), Path.of("RiftProtocol.jar"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Build OK: RiftProtocol.jar (Java 17+)");
    }
}
