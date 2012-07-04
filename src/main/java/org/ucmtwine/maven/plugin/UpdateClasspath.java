package org.ucmtwine.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Update the component classpath to include the project's dependencies, if they
 * have changed.
 * 
 * @goal classpath
 */
public class UpdateClasspath extends AbstractMojo {

  /**
   * Overwrite classpath?
   * 
   * If true, the entire classpath will be rewritten with maven dependencies.
   * 
   * If false (default) only new dependencies will be appended if missing.
   * 
   * @parameter expression="${overwriteClasspath}" default="false"
   */
  private boolean overwriteClasspath;

  public void execute() throws MojoExecutionException, MojoFailureException {
  }
}
