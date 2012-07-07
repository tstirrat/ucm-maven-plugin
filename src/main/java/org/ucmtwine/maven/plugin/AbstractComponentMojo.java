package org.ucmtwine.maven.plugin;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Extend this if you need your goal to be aware of the component name and .hda
 * file.
 */
abstract class AbstractComponentMojo extends AbstractMojo {

  /**
   * Name of the component.
   * 
   * Determines the name of the zip file. Can be specified or will auto detect
   * from first component found in manifest.hda
   * 
   * @parameter
   */
  protected String componentName;

  /**
   * The component zip path, relative to the root of the project.
   * 
   * @parameter
   */
  protected File componentZip;

  /**
   * The project base dir.
   * 
   * @parameter expression="${project.basedir}"
   * @required
   */
  protected File baseDir;

  /**
   * Determines the component name from either:
   * 
   * <ul>
   * <li>Supplied configuration</li>
   * <li>Folder name if the folder name corresponds to .hda file.</li>
   * <li>First non manifest.hda file it finds</li>
   * </ul>
   * 
   * @throws MojoExecutionException
   * 
   */
  protected void determineComponentName() throws MojoExecutionException {
    // if componentName passed, nothing to do
    if (componentName == null || componentName.length() == 0) {

      // 2. If <DirName>.hda exists
      if (baseDir != null && new File(baseDir.getName() + ".hda").exists()) {
        componentName = baseDir.getName();
      }

      // 3. first non manifest.hda file

      // find all .hda files in base folder
      FilenameFilter filter = new FilenameFilter() {
        public boolean accept(File folder, String name) {
          return name.endsWith(".hda") && !name.equals("manifest.hda");
        }
      };

      File files[] = baseDir.listFiles(filter);

      // TODO: iterate through and confirm that the .hda is in fact a component
      // hda.

      if (files.length == 0) {
        throw new MojoExecutionException(
            "Unable to determine component name. No component .hda found in project folder.");
      }

      componentName = files[0].getName().replaceFirst(".hda", "");
    }
  }

  /**
   * Determines the zip file for the component.
   */
  protected void determineComponentZip() {
    if (componentZip == null) {
      if (componentName != null) {
        componentZip = new File(componentName + ".zip");
      }
    }
  }
}
