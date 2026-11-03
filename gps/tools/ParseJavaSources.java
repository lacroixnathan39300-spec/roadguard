import com.sun.source.util.JavacTask;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;

/** JDK parser only: deliberately does not resolve Android types or build an APK. */
public final class ParseJavaSources {
  public static void main(String[] args)throws Exception{
    List<java.io.File> files=new ArrayList<>();
    try(java.util.stream.Stream<Path> paths=Files.walk(Paths.get(args[0]))){paths.filter(p->p.toString().endsWith(".java")).forEach(p->files.add(p.toFile()));}
    JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
    try(StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,null,null)){
      JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,Arrays.asList("-proc:none","-source","8","-Xlint:-options"),null,manager.getJavaFileObjectsFromFiles(files));
      for(Object parsed:task.parse()){}
      int errors=0;for(Diagnostic<?> d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR){System.err.println(d);errors++;}
      if(errors>0)throw new AssertionError(errors+" syntax errors");
    }
    System.out.println("PASS Java syntax: "+files.size()+" files parsed; Android symbols NOT resolved; APK NOT built");
  }
}
