package org.ucmtwine.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;

/**
 * Update the component classpath to include the project's dependencies, if they
 * have changed.
 * 
 * @goal classpath
 */
public class UpdateClasspath extends AbstractComponentMojo {

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
   * @component
   * @required
   * @readonly
   */
  private ArtifactFactory artifactFactory;

  /**
   * @component
   * @required
   * @readonly
   */
  private ArtifactMetadataSource artifactMetadataSource;

  /**
   * @component
   * @required
   * @readonly
   */
  private ArtifactCollector artifactCollector;

  /**
   * @component
   * @required
   * @readonly
   */
  private DependencyTreeBuilder treeBuilder;

  /**
   * @parameter default-value="${localRepository}"
   * @required
   * @readonly
   */
  private ArtifactRepository localRepository;

  /**
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

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

    if (!classPathRoot.endsWith("/")) {
      classPathRoot += "/";
    }

    SortedSet<String> classPathItems = getExistingClassPath();

    try {

      ArtifactFilter artifactFilter = new ScopeArtifactFilter(null);

      DependencyNode rootNode = treeBuilder.buildDependencyTree(project, localRepository, artifactFactory,
          artifactMetadataSource, artifactFilter, artifactCollector);

      CollectingDependencyNodeVisitor visitor = new CollectingDependencyNodeVisitor();

      rootNode.accept(visitor);

      @SuppressWarnings("unchecked")
      List<DependencyNode> nodes = visitor.getNodes();
      for (DependencyNode dependencyNode : nodes) {
        int state = dependencyNode.getState();
        Artifact artifact = dependencyNode.getArtifact();

        String scope = artifact.getScope();

        if (scope == null) {
          scope = "runtime";
        }

        if (artifact != null && state == DependencyNode.INCLUDED && !scope.equalsIgnoreCase(excludeScope)) {
          StringBuilder sb = new StringBuilder(classPathRoot);
          sb.append(artifact.getArtifactId()).append("-").append(artifact.getVersion());
          sb.append(".").append(artifact.getArtifactHandler().getExtension());

          classPathItems.add(sb.toString());
        }
      }

    } catch (DependencyTreeBuilderException e) {
      // TODO handle exception
      e.printStackTrace();
    }

    writeClassPath(classPathItems);
  }

  /**
   * Writes the classpath to the hda file.
   * 
   * @param classPathItems
   * @throws MojoExecutionException
   */
  private void writeClassPath(SortedSet<String> classPathItems) throws MojoExecutionException {
    StringBuilder sb = new StringBuilder();

    for (Iterator<String> i = classPathItems.iterator(); i.hasNext();) {
      sb.append(i.next()).append(";");
    }

    getLog().info("New classpath: " + sb.toString());

    File hdaFile = new File(componentName + ".hda");

    if (!hdaFile.exists()) {
      throw new MojoExecutionException("Hda file does not exist: " + hdaFile.toString());
    }

    try {
      replaceClassPath(sb.toString(), hdaFile);

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