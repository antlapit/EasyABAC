package custis.easyabac.generation;

import custis.easyabac.generation.util.CompleteGenerator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.FileInputStream;
import java.nio.file.Path;

@Mojo( name = "generatetest", requiresDependencyResolution = ResolutionScope.COMPILE)
public class TestGenerationMojo extends EasyAbacBaseMojo {

    // input paramters

    @Parameter( property = "testBasePackage", defaultValue = "easyabac.autogen" )
    private String testBasePackage;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            findAndCreateTests();
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
        }
    }

    private void findAndCreateTests() throws Exception {
        FileInputStream is = new FileInputStream(project.getBasedir() + "/" + policyFile);
        Path rootPath = project.getBasedir().toPath().resolve(testPath);

        CompleteGenerator.generate(is, rootPath, testBasePackage);
    }
}
