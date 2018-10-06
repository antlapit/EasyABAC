package custis.easyabac.generation;

import com.github.javaparser.utils.CodeGenerationUtils;
import custis.easyabac.generation.util.CompleteGenerator;
import custis.easyabac.generation.util.ModelGenerator;

import java.io.InputStream;
import java.nio.file.Path;

public class Main {

    private static InputStream getResourceAsStream(String s) {
        return Main.class
                .getClassLoader()
                .getResourceAsStream(s);
    }

    public static void main(String[] args) throws Exception {
        InputStream is = getResourceAsStream("test.yaml");
        Path rootPath = CodeGenerationUtils.mavenModuleRoot(ModelGenerator.class).resolve("src/test/java");
        String bPackage = "generation";

        CompleteGenerator.generate(is, rootPath, bPackage);

    }
}
