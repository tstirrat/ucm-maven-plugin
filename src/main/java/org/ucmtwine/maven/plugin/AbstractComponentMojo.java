package org.ucmtwine.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.model.impl.DataFactoryImpl;
import oracle.stellent.ridc.model.serialize.HdaBinderSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Extend this if you need your goal to be aware
 * of the component name and .hda file.
 */
abstract class AbstractComponentMojo extends AbstractMojo
{
   private static final Log log = LogFactory.getLog(AbstractComponentMojo.class);
   
   /** 
    * Field in the 
    *    Glue File (ComponentName.hda) 
    *    or the
    *    Manifest File (manifest.hda)
    * for the name of the component 
    */
   protected static final String componentNameKey = "ComponentName";

   /** Name of the Manifest File used for installing/updating the component */
   protected static final String manifestFileName = "manifest.hda";

  /**
   * Name of the component.
   *
   * Determined name of the zip file. Can be specified or will auto detect
   * from first component found in manifest.hda
   *
   */
   @Parameter(property = "componentName")
  protected String componentName;

  /**
   *  Location where to place the component zip file.
   *  defaults to target/componentName.zip
   */
  @Parameter(property = "componentLocation",
             defaultValue = "${project.build.Directory}/${componentFileName}")
  protected String componentLocation;

  @Parameter(property = "componentFileName", defaultValue = "${componentName}.zip")
  protected String componentFileName;

  /** The component zip path, relative to the root of the project. */
  @Parameter(property = "componentZip", defaultValue = "${componentLocation}")
  protected String componentZip;
  
  protected File getComponentZipAsFile()
  { return null != componentZip ? new File(componentZip) : null; }

  /** The project currently being built. */
  @Parameter( defaultValue = "${project}", readonly = true, required = true )
  protected MavenProject project;

  /** The current Maven session. */
  @Parameter( defaultValue = "${session}", readonly = true, required = true )
  protected MavenSession session;

  /**
   * The Maven BuildPluginManager component.
   *
   * @component
   * @required
   */
  @Component
  protected BuildPluginManager pluginManager;

  /** The project base dir. */
  @Parameter(defaultValue="${project.basedir}")
  protected File baseDir;

  /**
   * The relative path to the folder where your libraries are kept.
   * Defaults to "target/lib".
   *
   * //@parameter
   * @required
   */
  @Parameter(defaultValue = "${project.build.directory}/lib")
  protected String libFolder;

  /**
   * The component path to the folder where your libraries are kept.
   * Defaults to "/lib". <br />
   *
   * <strong>NOTE: </strong> must match the .hda file currently no support
   * for automagic grabbing from the hda file
   *
   * //@parameter
   * @required
   */
  @Parameter(property="componentLibFolder", defaultValue = "/lib")
  protected String componentLibFolder;

  /**
   * The component zip path, relative to the root of the project.
   *
   * @parameter property="project.basedir"/manifest.hda default=""
   */
   @Parameter(property = "manifestFile",
              defaultValue = "${project.basedir}/manifest.hda")
   protected String manifestHda;
   
   protected File getManifestFile()
   { return null != manifestHda ? new File(manifestHda) : null; }

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
   */
  protected void determineComponentName() throws MojoExecutionException
  {
    getLog().debug("determining component Name.");

    // if componentName passed, nothing to do
    if ( null == componentName || "".equals(componentName.trim()) )
    {
      // 2. manifest.hda ((Optionally) -> ComponentName.hda) -> ComponentName=XXXX
      File file = new File(manifestFileName);
      
      componentName = processHDAFile(file);
      getLog().debug("ComponentName is: " + componentName);
      if ( null != componentName && !"".equals(componentName.trim()) ) 
      { return; }
      
      // 2. If <DirName>.hda exists
      if (baseDir != null && new File(baseDir.getName() + ".hda").exists())
      { 
         componentName = baseDir.getName();
         if ( null != componentName && !"".equals(componentName.trim()) ) 
         { return; }
      }
      
      // 3. first non manifest.hda file

      // find all .hda files in base folder
      FilenameFilter filter =
              new FilenameFilter()
                  {
                     public boolean accept(File folder, String name)
                     {
                        return name.endsWith(".hda")
                            && !name.equals(manifestFileName);
                     }
                  };

      File files[] = baseDir.listFiles(filter);

      //TODO: iterate through and confirm that the .hda is in fact a component hda.

      if (files.length == 0)
      {
        throw new MojoExecutionException("Unable to determine component name. "
                                        +"No component .hda found in project folder.");
      }
      componentName = files[0].getName().replaceFirst(".hda", "");
    }
  }
  
  private String processHDAFile(File file)
  {
     getLog().debug("processing hda file: " + file.getAbsolutePath());

     BufferedReader br = null;
     
     try
     {
       br = new BufferedReader(new FileReader(file));
       String line;
       while ((line = br.readLine()) != null)
       {
         getLog().debug("reading line: " + line);
         if ( line.startsWith(componentNameKey) )
         {
           getLog().debug("Found component name row: " + line);
           Pattern pattern = Pattern.compile(componentNameKey+"=(\\w+)");
           Matcher matcher = pattern.matcher(line);
           if (matcher.find())
           {
             componentName = matcher.group(1);
             getLog().debug("Found component name in manifest: " + componentName);
             return componentName; 
           }
         }
         
         if ( line.equals("component") ) //next line is the path to the glue file 
         {
            String componentHDAPath = br.readLine();
            getLog().debug("glue file path:" + componentHDAPath); 
            File glueFile = new File(componentHDAPath);
            
            if ( !glueFile.exists() )
            {  
               File prefixedGlueFile = 
                            new File("component"+File.separator+glueFile.getPath());
               
               if ( !prefixedGlueFile.exists() )
               {  //walk down the file path searching for the file
                  while ( !glueFile.exists() )
                  {
                     String path = glueFile.getPath();
                     int index = path.lastIndexOf(File.separatorChar);
                     // if separator @ end of string remove it and try again
                     if ( index == (path.length()-1) ) 
                     {
                        path = path.substring(index-1);
                        index = path.lastIndexOf(File.separatorChar);
                     }
                     if ( -1 == index ) { break; }
                     if ( File.separatorChar == path.charAt(0) )
                     { path = path.substring(1, path.length()); }
                     path = path.substring(index); 
                     glueFile = new File(path);
                     getLog().debug("glue file path:" + glueFile.getAbsolutePath()); 
                  }
               }
               else { glueFile = prefixedGlueFile; }
            }
            // if glueFile found, restart the process
            if ( glueFile.exists() ) { return processHDAFile(glueFile); }
         }
       }
     }
     catch (FileNotFoundException fne)
     { getLog().error("Unable to find the " + file.getPath(), fne); }
     catch (IOException ioe)
     { getLog().error("Error opening the " + file.getPath() + " file.", ioe); }
     finally
     { if (br != null) { try { br.close(); } catch (IOException ingored) {} } }
     
     return null; //not found
  }

  /**
   * Given a hda, extract a result set from it.
   *
   * @param manifestFile
   * @param rsName
   * @return
   * @throws MojoExecutionException
   */
   protected DataResultSet getResultSetFromHda(File manifestFile, String rsName)
             throws MojoExecutionException
   {
      DataBinder manifest = getBinderFromHda(manifestFile);
   
      DataResultSet manifestRs = manifest.getResultSet(rsName);
   
      if (manifestRs == null)
      {
         throw new MojoExecutionException("Resultset " + rsName 
                                         + " doesn't exist in file " 
                                         + manifestFile);
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
  private DataBinder getBinderFromHda(File manifestFile) throws MojoExecutionException
  {
    if (manifestFile == null || !manifestFile.exists())
    { throw new MojoExecutionException("File "+manifestFile+" does not exist"); }

    // TODO: fix hard coded encoding
    HdaBinderSerializer serializer = new HdaBinderSerializer("UTF-8", new DataFactoryImpl());
    DataBinder binder = null;

    try { binder = serializer.parseBinder(new FileReader(manifestFile)); }
    catch (Exception e)
    { throw new MojoExecutionException("Error opening" + manifestFile, e); }

    return binder;
  }

}
