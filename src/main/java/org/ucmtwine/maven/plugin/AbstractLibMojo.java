package org.ucmtwine.maven.plugin;


import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

/**
 *  Abstract Base for working with Library/ClassPaths
 *  Create the component Library directory
 *  for simplified inclusion in the final component .zip file
 */
abstract class AbstractLibMojo extends AbstractComponentMojo
{

  /**
   * Exclude scope when building classpath and library
   *
   * //@parameter default-value="provided"
   * @required
   */
  @Parameter(defaultValue="provided")
  protected String excludeScope;

  /**
   * Include this scope when building classpath and library
   *
   * //@parameter default-value="runtime"
   * @required
   */
  @Parameter(defaultValue="runtime")
  protected String includeScope;

}
