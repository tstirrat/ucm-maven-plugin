package org.ucmtwine.maven.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import oracle.security.jps.internal.core.action.GetSecurityPropertyAction;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *  Build the component zip
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE,
      requiresProject = true)
//@Execute(goal="lib", phase = LifecyclePhase.PREPARE_PACKAGE)
public class BuildComponent extends AbstractComponentMojo
{
  private static final int BUFFER = 2048;
  
  /** project level variable  */
  @Parameter( defaultValue = "${project}", readonly = true, required = true )
  private MavenProject project;

  /**
   * The Directory where compiled classes are found
   */
  @Parameter(property="classes", defaultValue = "${project.build.outputDirectory}")
  private String classes;

  /**
   * A filter of file and directory names to exclude when packaging the
   * component.
   *
   * @parameter default="\.svn|\.git|\._.*|\.DS_Store|thumbs\.db|lockwait\. dat"
   */
  private String excludeFiles;

  public void execute() throws MojoExecutionException, MojoFailureException
  {
    DataResultSet manifestRs = getResultSetFromHda(getManifestFile(), "Manifest");

    ZipOutputStream zipStream;

    Map<String, String> zipListing = new TreeMap<String, String>();

    zipListing.put("manifest.hda", "manifest.hda");
    for (DataObject row : manifestRs.getRows()) { addToZipList(zipListing, row); }

    if (componentName == null)
    {
      throw new MojoExecutionException("No component name specified "
                                      +"or auto detected");
    }

    //TODO: adjust this to build in the project.build.directory and/or configured location
    //File componentZipFile = new File(componentName + ".zip");
    File componentZipFile = new File(componentLocation);

    getLog().info("Saving " + componentZipFile.getName() + " with contents:");

    try
    {
      zipStream = new ZipOutputStream(new FileOutputStream(componentZipFile,
                                                           false));
    }
    catch (FileNotFoundException e)
    { throw new MojoExecutionException("Unable to open zip file for output", e); }

    for (Iterator<String> i = zipListing.keySet().iterator(); i.hasNext();)
    {
      //TODO: Add fix for Component Classes to use /target/classes
      String fileSystemPath = i.next();
      String zipPath = zipListing.get(fileSystemPath);
      getLog().info("  " + zipPath);

      try { addFileToZip(zipStream, new File(fileSystemPath), zipPath); }
      catch (IOException e)
      {
        throw new MojoExecutionException("Unable to close stream for: "
                                        + fileSystemPath, e);
      }
    }

    try { zipStream.close(); }
    catch (IOException e)
    { throw new MojoExecutionException("Unable to close zip file", e); }
    
    //we've built the component now add it to the project for upstream
    Artifact artifact = project.getArtifact();
    artifact.setFile(componentZipFile);
  }

  /**
   * Add the file to the zip output stream
   *
   * @param zipStream
   * @param fileSystemPath
   * @param zipPath
   * @throws MojoExecutionException
   * @throws IOException
   */
  private void addFileToZip(ZipOutputStream zipStream, File fileSystemPath,
                            String zipPath)
          throws MojoExecutionException, IOException
  {
    if (!fileSystemPath.canRead())
    { throw new MojoExecutionException("file cannot be read: " + fileSystemPath); }

    if (fileSystemPath.isDirectory())
    { addFolderToZip(zipStream, fileSystemPath, zipPath); }
    else
    {
      InputStream in = null;
      try
      {
        in = new FileInputStream(fileSystemPath);

        ZipEntry entry = new ZipEntry(zipPath);
        zipStream.putNextEntry(entry);

        byte[] buf = new byte[BUFFER];
        int num = 0;
        while ((num = in.read(buf)) > 0) { zipStream.write(buf, 0, num);  }
      }
      catch (FileNotFoundException e)
      { throw new MojoExecutionException("file not found: " + fileSystemPath); }
      catch (IOException e)
      {
        throw new MojoExecutionException("error writing to zip: "
                                        + fileSystemPath);
      }
      finally
      {
        in.close();
        zipStream.closeEntry();
      }
    }
  }

  private void addFolderToZip(ZipOutputStream zipStream, File fileSystemPath,
                              String zipPath)
          throws MojoExecutionException, IOException
  {
    // get all items in folder, exclude those in excludeFiles
    if (zipPath.endsWith("/") || zipPath.endsWith("\\"))
    { zipPath = zipPath.substring(0, zipPath.length() - 1); }

    // It is also possible to filter the list of returned files.
    // This example does not return any files that start with `.'.
    FilenameFilter filter = getFileFilter();

    for (File entry : fileSystemPath.listFiles(filter))
    {
      String newZipPath = zipPath + "/" + entry.getName();
      if (entry.isDirectory()) { addFolderToZip(zipStream, entry, newZipPath); }
      else { addFileToZip(zipStream, entry, newZipPath); }
    }
  }

  /**
   * Return the filter used to enforce the <code>excludeFiles</code> config
   * parameter.
   *
   * @return
   */
  private FilenameFilter getFileFilter()
  {
    if (excludeFiles == null)
    {
       excludeFiles = ".*\\.svn|.*\\.git|\\._.*|\\.DS_Store|thumbs\\.db|lockwait\\.dat";
    }
    return new FilenameFilter()
    {
      public boolean accept(File dir, String name)
      { return !name.matches(excludeFiles); }
    };
  }

  /**
   * Adds a manifest listing to the zip listing, if the listing is a component,
   * read the component's .hda and add any component specific resources.
   *
   * @param zipListing
   * @param manifestEntry
   * @throws MojoExecutionException
   */
  private void addToZipList(Map<String, String> zipListing, DataObject manifestEntry)
          throws MojoExecutionException
  {
    String entryType = manifestEntry.get("entryType");
    String location  = manifestEntry.get("location");

    // remove component dir prefix
    if (location.startsWith(componentName))
    { location = location.replaceFirst(componentName+"/", ""); }

    String zipPrefix = "component/"+componentName+"/";
    String fileSystemLocation = location;

    if (entryType.equals("componentClasses"))
    { 
      getLog().debug("Classes Dir : " + classes);

      if ( null == classes ) { classes = "target/classes/"; }

      //classes = "component/"+componentName+"/"+classes;
      fileSystemLocation = classes;
    }
    
    if (entryType.equals("componentLib"))
    { fileSystemLocation = libFolder; }
    
    zipListing.put(fileSystemLocation, zipPrefix + location);

    if (entryType.equals("component"))
    {
      File componentHdaFile = new File(location);

      addComponentResourcesToZipList(zipListing, componentHdaFile);
    }

  }

  /**
   * Adds all files needed within a component to the zip listing.
   *
   * @param zipListing
   * @param componentHdaFile
   * @throws MojoExecutionException
   */
  private void addComponentResourcesToZipList(Map<String, String> zipListing,
                                              File componentHdaFile)
          throws MojoExecutionException
  {
    String componentName = componentHdaFile.getName().replaceAll(".hda", "");

    // if component name not set yet, set it.
    if (this.componentName == null) { this.componentName = componentName; }

    String baseZipPath = "component/" + componentName + "/";

    // read ResourceDefinition from hda file.
    DataResultSet componentResources = getResultSetFromHda(componentHdaFile,
                                                           "ResourceDefinition");

    for (DataObject resourceRow : componentResources.getRows())
    {
      String type = resourceRow.get("type");
      String fileName = resourceRow.get("filename");

      // template entries have multiple files
      // so they need to be included by folder.
      if (type != null && type.equals("template"))
      {
        String templateFolder = new File(fileName).getParent();
        zipListing.put(templateFolder, baseZipPath + templateFolder);
      }
      else { zipListing.put(fileName, baseZipPath + fileName); }
    }
  }
}
