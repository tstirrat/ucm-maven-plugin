package org.ucmtwine.maven.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Update the component classpath to include the project's dependencies, if they
 * have changed.
 * 
 * @goal classpath
 * @requiresDependencyResolution compile
 */
public class UpdateClasspath extends AbstractComponentMojo {

  /**
   * The project currently being build.
   * 
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * The current Maven session.
   * 
   * @parameter expression="${session}"
   * @required
   * @readonly
   */
  private MavenSession session;

  /**
   * The Maven BuildPluginManager component.
   * 
   * @component
   * @required
   */
  private BuildPluginManager pluginManager;

  /**
   * Overwrite classpath?
   * 
   * If true, the entire classpath will be rewritten with maven dependencies.
   * 
   * If false (default) only new dependencies will be appended if missing.
   * 
   * @parameter expression="${overwriteClasspath}" default-value="true"
   */
  private boolean overwriteClasspath;

  /**
   * The relative path to the folder where your libraries are kept. Defaults to
   * "lib".
   * 
   * @parameter default-value="lib"
   */
  private String libFolder;

  /**
   * Exclude scope when building classpath
   * 
   * @parameter default-value="provided"
   */
  private String excludeScope;

  /**
   * Include this scope when building classpath
   * 
   * @parameter default-value="runtime"
   */
  private String includeScope;

  public void execute() throws MojoExecutionException, MojoFailureException {

    // find componentName
    determineComponentName();

    String classPathRoot = "$COMPONENT_DIR/" + libFolder;

    StringBuilder classpath = new StringBuilder();

    if (classPathRoot.endsWith("/")) {
      classPathRoot = classPathRoot.substring(0, classPathRoot.length() - 2);
    }

    // add this artifact, if its a jar type
    if (project.getPackaging().equalsIgnoreCase("jar")) {
      classpath.append(classPathRoot).append("/").append(project.getArtifactId()).append("-")
          .append(project.getVersion()).append(".jar;");
    }

    // @formatter:off
    executeMojo(
        plugin("org.apache.maven.plugins", "maven-dependency-plugin", "2.8"),
        goal("build-classpath"),
        configuration(
            element(name("prefix"), classPathRoot),
            element(name("fileSeparator"), "/"),
            element(name("pathSeparator"), ";"),
            element(name("includeScope"), includeScope),
            element(name("excludeScope"), excludeScope),
            element(name("outputProperty"), "componentClassPath")
        ), 
        executionEnvironment(project, session, pluginManager));
    // @formatter:on

    String mojoClassPath = project.getProperties().getProperty("componentClassPath");

    if (mojoClassPath != null) {
      classpath.append(mojoClassPath);
    }

    String finalClassPath = classpath.toString();

    // TODO: need only update classpath if it has changed!
    writeClassPath(finalClassPath);
  }

  /**
   * Writes the classpath to the hda file.
   * 
   * @param classPathItems
   * @throws MojoExecutionException
   */
  private void writeClassPath(String classpath) throws MojoExecutionException {

    getLog().info("New classpath: " + classpath);

    File hdaFile = new File(componentName + ".hda");

    if (!hdaFile.exists()) {
      throw new MojoExecutionException("Hda file does not exist: " + hdaFile.toString());
    }

    try {
      replaceClassPath(classpath, hdaFile);

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void replaceClassPath(String newClassPath, File hdaFile) throws IOException, MojoExecutionException {

    File tempFile = new File("temp.hda");

    BufferedReader reader = new BufferedReader(new FileReader(hdaFile));
    PrintWriter writer = new PrintWriter(new FileWriter(tempFile, false));
    String line = null;

    while ((line = reader.readLine()) != null) {
      if (line.startsWith("classpath=")) {
        writer.println("classpath=" + newClassPath);
      } else {
        writer.println(line);
      }
    }

    reader.close();
    writer.flush();
    writer.close();

    File oldFile = new File("old.hda");

    if (oldFile.exists()) {
      oldFile.delete();
    }

    if (!hdaFile.renameTo(oldFile)) {
      throw new MojoExecutionException("Unable to rename " + hdaFile.getName() + " to " + oldFile.getName());
    }

    if (!tempFile.renameTo(hdaFile)) {
      throw new MojoExecutionException("Unable to rename " + tempFile.getName() + " to " + hdaFile.getName());
    }
  }

  @SuppressWarnings("unused")
  private SortedSet<String> getExistingClassPath() throws MojoExecutionException {
    SortedSet<String> items = new TreeSet<String>();

    File componentHda = new File(componentName + ".hda");

    if (!componentHda.exists()) {
      throw new MojoExecutionException("Missing hda: " + componentHda.getName());
    }

    // TODO: get and process current lib path
    if (!overwriteClasspath) {

    }

    return items;
  }
}