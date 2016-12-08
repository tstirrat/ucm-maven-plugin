package org.ucmtwine.maven.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import oracle.security.jps.internal.core.action.GetSecurityPropertyAction;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.model.impl.DataFactoryImpl;
import oracle.stellent.ridc.model.serialize.HdaBinderSerializer;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *  Create the component Library directory
 *  for simplified inclusion in the final component .zip file
 */
@Mojo(name = "lib", defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
      requiresProject = true)
//@Execute(goal="compile", phase = LifecyclePhase.COMPILE)
public class LibComponent extends AbstractLibMojo
{

   @Parameter(defaultValue="${project.build.directory}/lib")
   private String outputDirectory;

   public void execute() throws MojoExecutionException, MojoFailureException
   {
     //String libFolder = getLibFolder();
     getLog().debug("Lib Folder: " + libFolder);

     project.getProperties().setProperty("outputDirectory", libFolder);
     getLog().debug( "Lib Folder Property: "
                   + project.getProperties().getProperty("outputDirectory"));
     getLog().debug( "Output Folder: " + outputDirectory);

     executeMojo(
            plugin("org.apache.maven.plugins", "maven-dependency-plugin", "2.8"),
            goal("copy-dependencies"),
            configuration(element(name("outputDirectory"), outputDirectory),
                          element(name("includeScope"),    includeScope),
                          element(name("excludeScope"),    excludeScope) //,
                          //element(name("outputDirectory"), libFolder)
                          ),
            executionEnvironment(project, session, pluginManager));
   }

}
