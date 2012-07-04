package org.ucmtwine.maven.plugin;

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

import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.model.impl.DataFactoryImpl;
import oracle.stellent.ridc.model.serialize.HdaBinderSerializer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Build the component zip
 * 
 * @goal build
 */
public class BuildComponent extends AbstractMojo {

  private static final int BUFFER = 2048;

  /**
   * The component zip path, relative to the root of the project.
   * 
   * @parameter expression="${project.basedir}/manifest.hda" default=""
   */
  private File manifestFile;

  /**
   * A filter of file and directory names to exclude when packaging the
   * component.
   * 
   * @parameter default="\.svn|\.git|\._.*|\.DS_Store|thumbs\.db|lockwait\.dat"
   */
  private String excludeFiles;

  /**
   * Name of the component.
   * 
   * Determines the name of the zip file. Can be specified or will auto detect
   * from first component found in manifest.hda
   * 
   * @parameter
   */
  private String componentName;

  public void execute() throws MojoExecutionException, MojoFailureException {

    DataResultSet manifestRs = getResultSetFromHda(manifestFile, "Manifest");

    ZipOutputStream zipStream;

    Map<String, String> zipListing = new TreeMap<String, String>();

    for (DataObject row : manifestRs.getRows()) {
      addToZipList(zipListing, row);
    }
    zipListing.put("manifest.hda", "manifest.hda");

    if (componentName == null) {
      throw new MojoExecutionException("No component name specified or auto detected");
    }

    File componentZipFile = new File(componentName + ".zip");

    getLog().info("Saving " + componentZipFile.getName() + " with contents:");

    try {
      zipStream = new ZipOutputStream(new FileOutputStream(componentZipFile, false));

    } catch (FileNotFoundException e) {
      throw new MojoExecutionException("Unable to open zip file for output", e);
    }

    for (Iterator<String> i = zipListing.keySet().iterator(); i.hasNext();) {
      String fileSystemPath = i.next();
      String zipPath = zipListing.get(fileSystemPath);
      getLog().info("  " + String.format("%-60s", fileSystemPath) + " -> " + zipPath);

      try {
        addFileToZip(zipStream, new File(fileSystemPath), zipPath);
      } catch (IOException e) {
        throw new MojoExecutionException("Unable to close stream for: " + fileSystemPath, e);
      }
    }

    try {
      zipStream.close();

    } catch (IOException e) {
      throw new MojoExecutionException("Unable to close zip file", e);
    }
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
  private void addFileToZip(ZipOutputStream zipStream, File fileSystemPath, String zipPath)
      throws MojoExecutionException, IOException {

    if (!fileSystemPath.canRead()) {
      throw new MojoExecutionException("file cannot be read: " + fileSystemPath);
    }

    if (fileSystemPath.isDirectory()) {
      addFolderToZip(zipStream, fileSystemPath, zipPath);

    } else {

      InputStream in = null;
      try {
        in = new FileInputStream(fileSystemPath);

        ZipEntry entry = new ZipEntry(zipPath);
        zipStream.putNextEntry(entry);

        byte[] buf = new byte[BUFFER];
        int num = 0;
        while ((num = in.read(buf)) > 0) {
          zipStream.write(buf, 0, num);
        }

      } catch (FileNotFoundException e) {
        throw new MojoExecutionException("file not found: " + fileSystemPath);

      } catch (IOException e) {
        throw new MojoExecutionException("error writing to zip: " + fileSystemPath);

      } finally {
        in.close();
        zipStream.closeEntry();
      }
    }
  }

  private void addFolderToZip(ZipOutputStream zipStream, File fileSystemPath, String zipPath)
      throws MojoExecutionException, IOException {
    // get all items in folder, exclude those in excludeFiles
    if (zipPath.endsWith("/") || zipPath.endsWith("\\")) {
      zipPath = zipPath.substring(0, zipPath.length() - 1);
    }

    // It is also possible to filter the list of returned files.
    // This example does not return any files that start with `.'.
    FilenameFilter filter = getFileFilter();

    for (File entry : fileSystemPath.listFiles(filter)) {
      String newZipPath = zipPath + File.separator + entry.getName();
      if (entry.isDirectory()) {
        addFolderToZip(zipStream, entry, newZipPath);
      } else {
        addFileToZip(zipStream, entry, newZipPath);
      }
    }
  }

  /**
   * Return the filter used to enforce the <code>excludeFiles</code> config
   * parameter.
   * 
   * @return
   */
  private FilenameFilter getFileFilter() {
    if (excludeFiles == null) {
      excludeFiles = ".*\\.svn|.*\\.git|\\._.*|\\.DS_Store|thumbs\\.db|lockwait\\.dat";
    }
    return new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return !name.matches(excludeFiles);
      }
    };
  }

  /**
   * Given a hda, extract a result set from it.
   * 
   * @param manifestFile
   * @param rsName
   * @return
   * @throws MojoExecutionException
   */
  private DataResultSet getResultSetFromHda(File manifestFile, String rsName) throws MojoExecutionException {
    DataBinder manifest = getBinderFromHda(manifestFile);

    DataResultSet manifestRs = manifest.getResultSet(rsName);

    if (manifestRs == null) {
      throw new MojoExecutionException("Resultset " + rsName + " doesn't exist in file " + manifestFile);
    }
    return manifestRs;
  }

  /**
   * Unserialize a hda file into a binder.
   * 
   * @param manifestFile
   * @return
   * @throws MojoExecutionException
   */
  private DataBinder getBinderFromHda(File manifestFile) throws MojoExecutionException {
    if (manifestFile == null || !manifestFile.exists()) {
      throw new MojoExecutionException("File " + manifestFile + " does not exist");
    }

    // TODO: fix hard coded encoding
    HdaBinderSerializer serializer = new HdaBinderSerializer("UTF-8", new DataFactoryImpl());
    DataBinder binder = null;

    try {
      binder = serializer.parseBinder(new FileReader(manifestFile));

    } catch (Exception e) {
      throw new MojoExecutionException("Error opening" + manifestFile, e);
    }

    return binder;
  }

  /**
   * Adds a manifest listing to the zip listing, if the listing is a component,
   * read the component's .hda and add any component specific resources.
   * 
   * @param zipListing
   * @param manifestEntry
   * @throws MojoExecutionException
   */
  private void addToZipList(Map<String, String> zipListing, DataObject manifestEntry) throws MojoExecutionException {
    String entryType = manifestEntry.get("entryType");
    String location = manifestEntry.get("location");

    if (entryType.equals("component")) {

      File componentHdaFile = new File(".." + File.separator + location);

      addResourcesToZipList(zipListing, componentHdaFile);
    }

    zipListing.put(".." + File.separator + location, "component" + File.separator + location);
  }

  /**
   * Adds all files needed within a component to the zip listing.
   * 
   * @param zipListing
   * @param componentHdaFile
   * @throws MojoExecutionException
   */
  private void addResourcesToZipList(Map<String, String> zipListing, File componentHdaFile)
      throws MojoExecutionException {
    String componentName = componentHdaFile.getName().replaceAll(".hda", "");

    // if component name not set yet, set it.
    if (this.componentName == null) {
      this.componentName = componentName;
    }

    String baseFileSystemPath = ".." + File.separator + componentName + File.separator;
    String baseZipPath = "component" + File.separator + componentName + File.separator;

    // read ResourceDefinition from hda file.
    DataResultSet componentResources = getResultSetFromHda(componentHdaFile, "ResourceDefinition");

    for (DataObject resourceRow : componentResources.getRows()) {
      String type = resourceRow.get("type");
      String fileName = resourceRow.get("filename");

      // template entries have multiple files so they need to be included by
      // folder.
      if (type != null && type.equals("template")) {
        String templateFolder = new File(fileName).getParent();
        zipListing.put(baseFileSystemPath + templateFolder, baseZipPath + templateFolder);
      } else {
        zipListing.put(baseFileSystemPath + fileName, baseZipPath + fileName);
      }
    }
  }
}
